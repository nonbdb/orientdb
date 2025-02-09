package com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated;

import com.jetbrains.youtrack.db.api.YouTrackDB;
import com.jetbrains.youtrack.db.api.YourTracks;
import com.jetbrains.youtrack.db.api.config.GlobalConfiguration;
import com.jetbrains.youtrack.db.api.config.YouTrackDBConfig;
import com.jetbrains.youtrack.db.api.exception.ModificationOperationProhibitedException;
import com.jetbrains.youtrack.db.api.record.RID;
import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.api.schema.Schema;
import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import com.jetbrains.youtrack.db.internal.DbTestBase;
import com.jetbrains.youtrack.db.internal.common.io.FileUtils;
import com.jetbrains.youtrack.db.internal.common.log.LogManager;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.YouTrackDBConfigImpl;
import com.jetbrains.youtrack.db.internal.core.db.tool.DatabaseCompare;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.Assert;
import org.junit.Test;

public class StorageBackupMTTest {

  private final CountDownLatch started = new CountDownLatch(1);
  private final Stack<CountDownLatch> backupIterationRecordCount = new Stack<>();
  private final CountDownLatch finished = new CountDownLatch(1);

  private YouTrackDB youTrackDB;
  private String dbName;

  @Test
  public void testParallelBackup() throws Exception {
    backupIterationRecordCount.clear();
    for (int i = 0; i < 100; i++) {
      CountDownLatch latch = new CountDownLatch(4);
      backupIterationRecordCount.add(latch);
    }

    dbName = StorageBackupMTTest.class.getSimpleName();

    final String buildDirectory =
        System.getProperty("buildDirectory", ".") + File.separator + getClass().getSimpleName();
    final File backupDir = new File(buildDirectory, "backupDir");

    FileUtils.deleteRecursively(backupDir);
    FileUtils.deleteRecursively(new File(DbTestBase.getBaseDirectoryPath(getClass())));

    final String backupDbName = StorageBackupMTTest.class.getSimpleName() + "BackUp";
    try {
      youTrackDB = YourTracks.embedded(DbTestBase.getBaseDirectoryPath(getClass()),
          YouTrackDBConfig.defaultConfig());
      youTrackDB.execute(
          "create database `" + dbName + "` plocal users(admin identified by 'admin' role admin)");

      var db = (DatabaseSessionInternal) youTrackDB.open(dbName, "admin", "admin");

      final Schema schema = db.getMetadata().getSchema();
      final SchemaClass backupClass = schema.createClass("BackupClass");
      backupClass.createProperty(db, "num", PropertyType.INTEGER);
      backupClass.createProperty(db, "data", PropertyType.BINARY);

      backupClass.createIndex(db, "backupIndex", SchemaClass.INDEX_TYPE.NOTUNIQUE, "num");
      if (!backupDir.exists()) {
        Assert.assertTrue(backupDir.mkdirs());
      }

      try (final ExecutorService executor = Executors.newCachedThreadPool()) {
        final List<Future<Void>> futures = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
          Stack<CountDownLatch> producerIterationRecordCount = new Stack<>();
          producerIterationRecordCount.addAll(backupIterationRecordCount);
          futures.add(executor.submit(new DataWriterCallable(producerIterationRecordCount, 1000)));
        }

        futures.add(executor.submit(new DBBackupCallable(backupDir.getAbsolutePath())));

        started.countDown();

        finished.await();

        for (Future<Void> future : futures) {
          future.get();
        }
      }

      System.out.println("do inc backup last time");
      db.incrementalBackup(backupDir.toPath());

      youTrackDB.close();

      System.out.println("create and restore");

      youTrackDB = YourTracks.embedded(DbTestBase.getBaseDirectoryPath(getClass()),
          YouTrackDBConfig.defaultConfig());
      youTrackDB.restore(backupDbName, null, null, backupDir.getAbsolutePath(),
          YouTrackDBConfig.defaultConfig());

      final DatabaseCompare compare =
          new DatabaseCompare(
              (DatabaseSessionInternal) youTrackDB.open(dbName, "admin", "admin"),
              (DatabaseSessionInternal) youTrackDB.open(backupDbName, "admin", "admin"),
              System.out::println);

      System.out.println("compare");

      boolean areSame = compare.compare();
      Assert.assertTrue(areSame);
    } finally {
      if (youTrackDB != null && youTrackDB.isOpen()) {
        try {
          youTrackDB.close();
        } catch (Exception ex) {
          LogManager.instance().error(this, "", ex);
        }
      }
      try {
        youTrackDB = YourTracks.embedded(DbTestBase.getBaseDirectoryPath(getClass()),
            YouTrackDBConfig.defaultConfig());

        if (youTrackDB.exists(dbName)) {
          youTrackDB.drop(dbName);
        }
        if (youTrackDB.exists(backupDbName)) {
          youTrackDB.drop(backupDbName);
        }

        youTrackDB.close();

        FileUtils.deleteRecursively(backupDir);
      } catch (Exception ex) {
        LogManager.instance().error(this, "", ex);
      }
    }
  }

  @Test
  public void testParallelBackupEncryption() throws Exception {
    backupIterationRecordCount.clear();
    for (int i = 0; i < 100; i++) {
      CountDownLatch latch = new CountDownLatch(4);
      backupIterationRecordCount.add(latch);
    }

    String testDirectory = DbTestBase.getBaseDirectoryPath(getClass());
    FileUtils.deleteRecursively(new File(testDirectory));

    FileUtils.createDirectoryTree(testDirectory);
    final String backupDbName = StorageBackupMTTest.class.getSimpleName() + "BackUp";
    final File backupDir = new File(testDirectory, "backupDir");
    FileUtils.deleteRecursively(backupDir);

    dbName = StorageBackupMTTest.class.getSimpleName();
    final YouTrackDBConfigImpl config =
        (YouTrackDBConfigImpl) YouTrackDBConfig.builder()
            .addGlobalConfigurationParameter(GlobalConfiguration.STORAGE_ENCRYPTION_KEY,
                "T1JJRU5UREJfSVNfQ09PTA==")
            .build();

    try {
      youTrackDB = YourTracks.embedded(testDirectory, config);
      youTrackDB.execute(
          "create database `" + dbName + "` plocal users(admin identified by 'admin' role admin)");
      var db = (DatabaseSessionInternal) youTrackDB.open(dbName, "admin", "admin");

      final Schema schema = db.getMetadata().getSchema();
      final SchemaClass backupClass = schema.createClass("BackupClass");
      backupClass.createProperty(db, "num", PropertyType.INTEGER);
      backupClass.createProperty(db, "data", PropertyType.BINARY);

      backupClass.createIndex(db, "backupIndex", SchemaClass.INDEX_TYPE.NOTUNIQUE, "num");

      if (!backupDir.exists()) {
        Assert.assertTrue(backupDir.mkdirs());
      }

      try (final ExecutorService executor = Executors.newCachedThreadPool()) {
        final List<Future<Void>> futures = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
          Stack<CountDownLatch> producerIterationRecordCount = new Stack<>();
          producerIterationRecordCount.addAll(backupIterationRecordCount);
          futures.add(executor.submit(new DataWriterCallable(producerIterationRecordCount, 1000)));
        }

        futures.add(executor.submit(new DBBackupCallable(backupDir.getAbsolutePath())));

        started.countDown();

        finished.await();

        for (Future<Void> future : futures) {
          future.get();
        }
      }

      System.out.println("do inc backup last time");
      db.incrementalBackup(backupDir.toPath());

      youTrackDB.close();

      System.out.println("create and restore");

      youTrackDB = YourTracks.embedded(testDirectory, config);
      youTrackDB.restore(backupDbName, null, null,
          backupDir.getAbsolutePath(), config);

      final DatabaseCompare compare =
          new DatabaseCompare(
              (DatabaseSessionInternal) youTrackDB.open(dbName, "admin", "admin"),
              (DatabaseSessionInternal) youTrackDB.open(backupDbName, "admin", "admin"),
              System.out::println);

      System.out.println("compare");

      boolean areSame = compare.compare();
      Assert.assertTrue(areSame);
    } finally {
      if (youTrackDB.isOpen()) {
        try {
          youTrackDB.close();
        } catch (Exception ex) {
          LogManager.instance().error(this, "", ex);
        }
      }
      try {
        youTrackDB = YourTracks.embedded(testDirectory, config);
        if (youTrackDB.exists(dbName)) {
          youTrackDB.drop(dbName);
        }
        if (youTrackDB.exists(backupDbName)) {
          youTrackDB.drop(backupDbName);
        }

        youTrackDB.close();

        FileUtils.deleteRecursively(backupDir);
      } catch (Exception ex) {
        LogManager.instance().error(this, "", ex);
      }
    }
  }

  private final class DataWriterCallable implements Callable<Void> {

    private final Stack<CountDownLatch> producerIterationRecordCount;
    private final int count;

    public DataWriterCallable(Stack<CountDownLatch> producerIterationRecordCount, int count) {
      this.producerIterationRecordCount = producerIterationRecordCount;
      this.count = count;
    }

    @Override
    public Void call() throws Exception {
      started.await();

      System.out.println(Thread.currentThread() + " - start writing");

      try (var db = youTrackDB.open(dbName, "admin", "admin")) {

        Random random = new Random();
        List<RID> ids = new ArrayList<>();
        while (!producerIterationRecordCount.isEmpty()) {

          for (int i = 0; i < count; i++) {
            try {
              db.begin();
              final byte[] data = new byte[random.nextInt(1024)];
              random.nextBytes(data);

              final int num = random.nextInt();
              if (!ids.isEmpty() && i % 8 == 0) {
                RID id = ids.removeFirst();
                db.delete(id);
              } else if (!ids.isEmpty() && i % 4 == 0) {
                RID id = ids.removeFirst();
                final EntityImpl document = db.load(id);
                document.field("data", data);
              } else {
                final EntityImpl document = new EntityImpl("BackupClass");
                document.field("num", num);
                document.field("data", data);

                document.save();
                RID id = document.getIdentity();
                if (ids.size() < 100) {
                  ids.add(id);
                }
              }
              db.commit();

            } catch (ModificationOperationProhibitedException e) {
              System.out.println("Modification prohibited ... wait ...");
              //noinspection BusyWait
              Thread.sleep(1000);
            }
          }
          producerIterationRecordCount.pop().countDown();
          System.out.println(Thread.currentThread() + " writing of a batch done");
        }

        System.out.println(Thread.currentThread() + " - done writing");
        finished.countDown();
        return null;
      }
    }
  }

  public final class DBBackupCallable implements Callable<Void> {

    private final String backupPath;

    public DBBackupCallable(String backupPath) {
      this.backupPath = backupPath;
    }

    @Override
    public Void call() throws Exception {
      started.await();

      try (var db = youTrackDB.open(dbName, "admin", "admin")) {
        System.out.println(Thread.currentThread() + " - start backup");
        while (!backupIterationRecordCount.isEmpty()) {
          CountDownLatch latch = backupIterationRecordCount.pop();
          latch.await();

          System.out.println(Thread.currentThread() + " do inc backup");
          db.incrementalBackup(Path.of(backupPath));
          System.out.println(Thread.currentThread() + " done inc backup");
        }
      } catch (Exception | Error e) {
        LogManager.instance().error(this, "", e);
        throw e;
      }
      finished.countDown();

      System.out.println(Thread.currentThread() + " - stop backup");

      return null;
    }
  }
}
