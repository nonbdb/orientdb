package com.jetbrains.youtrack.db.internal.core.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import com.jetbrains.youtrack.db.api.DatabaseSession;
import com.jetbrains.youtrack.db.api.YouTrackDB;
import com.jetbrains.youtrack.db.api.config.GlobalConfiguration;
import com.jetbrains.youtrack.db.api.config.YouTrackDBConfig;
import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.api.query.ResultSet;
import com.jetbrains.youtrack.db.api.session.SessionPool;
import com.jetbrains.youtrack.db.internal.DbTestBase;
import com.jetbrains.youtrack.db.internal.common.concur.NeedRetryException;
import com.jetbrains.youtrack.db.internal.core.CreateDatabaseUtil;
import com.jetbrains.youtrack.db.internal.core.YouTrackDBEnginesManager;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseRecordThreadLocal;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseThreadLocalFactory;
import com.jetbrains.youtrack.db.internal.core.db.YouTrackDBImpl;
import com.jetbrains.youtrack.db.internal.core.metadata.function.Function;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests cases for the Scheduler component.
 */
public class SchedulerTest {

  @Test
  public void scheduleSQLFunction() throws Exception {
    try (YouTrackDB context = createContext()) {
      var db =
          (DatabaseSessionInternal) context.cachedPool("test", "admin",
              CreateDatabaseUtil.NEW_ADMIN_PASSWORD).acquire();
      createLogEvent(db);

      DbTestBase.assertWithTimeout(
          db,
          () -> {
            Long count = getLogCounter(db);
            Assert.assertTrue(count >= 2 && count <= 3);
          });
    }
  }

  @Test
  public void scheduleWithDbClosed() throws Exception {
    YouTrackDB context = createContext();
    {
      var db = (DatabaseSessionInternal) context.open("test", "admin",
          CreateDatabaseUtil.NEW_ADMIN_PASSWORD);
      createLogEvent(db);
      db.close();
    }

    var db = context.open("test", "admin", CreateDatabaseUtil.NEW_ADMIN_PASSWORD);
    DbTestBase.assertWithTimeout(
        db,
        () -> {
          Long count = getLogCounter(db);
          Assert.assertTrue(count >= 2);
        });

    db.close();
    context.close();
  }

  @Test
  public void eventLifecycle() throws Exception {
    try (YouTrackDB context = createContext()) {
      var db =
          (DatabaseSessionInternal) context.cachedPool("test", "admin",
              CreateDatabaseUtil.NEW_ADMIN_PASSWORD).acquire();
      createLogEvent(db);

      Thread.sleep(2000);

      db.getMetadata().getScheduler().removeEvent(db, "test");

      assertThat(db.getMetadata().getScheduler().getEvents()).isEmpty();

      assertThat(db.getMetadata().getScheduler().getEvent("test")).isNull();

      // remove again
      db.getMetadata().getScheduler().removeEvent(db, "test");

      Thread.sleep(3000);

      Long count = getLogCounter(db);

      Assert.assertTrue(count >= 1 && count <= 3);
    }
  }

  @Test
  public void eventSavedAndLoaded() throws Exception {
    YouTrackDB context = createContext();
    var db =
        (DatabaseSessionInternal) context.open("test", "admin",
            CreateDatabaseUtil.NEW_ADMIN_PASSWORD);
    createLogEvent(db);
    db.close();

    Thread.sleep(1000);

    final DatabaseSession db2 =
        context.open("test", "admin", CreateDatabaseUtil.NEW_ADMIN_PASSWORD);
    try {
      Thread.sleep(4000);
      Long count = getLogCounter(db2);
      Assert.assertTrue(count >= 2);

    } finally {
      db2.close();
      context.close();
    }
  }

  @Test
  public void testScheduleEventWithMultipleActiveDatabaseConnections() {
    final YouTrackDB youTrackDb =
        new YouTrackDBImpl(
            DbTestBase.embeddedDBUrl(getClass()),
            YouTrackDBConfig.builder()
                .addGlobalConfigurationParameter(GlobalConfiguration.DB_POOL_MAX, 1)
                .addGlobalConfigurationParameter(GlobalConfiguration.CREATE_DEFAULT_USERS, false)
                .build());
    if (!youTrackDb.exists("test")) {
      youTrackDb.execute(
          "create database "
              + "test"
              + " "
              + "memory"
              + " users ( admin identified by '"
              + CreateDatabaseUtil.NEW_ADMIN_PASSWORD
              + "' role admin)");
    }
    final SessionPool pool =
        youTrackDb.cachedPool("test", "admin", CreateDatabaseUtil.NEW_ADMIN_PASSWORD);
    var db = (DatabaseSessionInternal) pool.acquire();

    assertEquals(db, DatabaseRecordThreadLocal.instance().getIfDefined());
    createLogEvent(db);
    assertEquals(db, DatabaseRecordThreadLocal.instance().getIfDefined());

    youTrackDb.close();
  }

  @Test
  public void eventBySQL() throws Exception {
    YouTrackDB context = createContext();
    try (context;
        var db =
            (DatabaseSessionInternal) context.open("test", "admin",
                CreateDatabaseUtil.NEW_ADMIN_PASSWORD)) {
      Function func = createFunction(db);
      db.begin();
      db.command(
              "insert into oschedule set name = 'test',"
                  + " function = ?, rule = \"0/1 * * * * ?\", arguments = {\"note\": \"test\"}",
              func.getId(db))
          .close();
      db.commit();

      DbTestBase.assertWithTimeout(
          db,
          () -> {
            long count = getLogCounter(db);
            Assert.assertTrue(count >= 2);
          });

      final long count = getLogCounter(db);
      Assert.assertTrue(count >= 2);

      int retryCount = 10;
      while (true) {
        try {
          db.begin();
          db.command(
                  "update oschedule set rule = \"0/2 * * * * ?\", function = ? where name = 'test'",
                  func.getId(db))
              .close();
          db.commit();
          break;
        } catch (NeedRetryException e) {
          retryCount--;
          //noinspection BusyWait
          Thread.sleep(10);
        }
        Assert.assertTrue(retryCount >= 0);
      }

      DbTestBase.assertWithTimeout(
          db,
          () -> {
            long newCount = getLogCounter(db);
            Assert.assertTrue(newCount - count > 1);
          });

      long newCount = getLogCounter(db);
      Assert.assertTrue(newCount - count > 1);

      retryCount = 10;
      while (true) {
        try {
          // DELETE
          db.begin();
          db.command("delete from oschedule where name = 'test'", func.getId(db)).close();
          db.commit();
          break;
        } catch (NeedRetryException e) {
          retryCount--;
          //noinspection BusyWait
          Thread.sleep(10);
        }
        Assert.assertTrue(retryCount >= 0);
      }

      DbTestBase.assertWithTimeout(
          db,
          () -> {
            var counter = getLogCounter(db);
            Assert.assertTrue(counter - newCount <= 1);
          });
    }
  }

  private YouTrackDB createContext() {
    final YouTrackDB youTrackDB =
        CreateDatabaseUtil.createDatabase("test", DbTestBase.embeddedDBUrl(getClass()),
            CreateDatabaseUtil.TYPE_MEMORY);
    YouTrackDBEnginesManager.instance()
        .registerThreadDatabaseFactory(
            new TestScheduleDatabaseFactory(
                youTrackDB, "test", "admin", CreateDatabaseUtil.NEW_ADMIN_PASSWORD));
    return youTrackDB;
  }

  private void createLogEvent(DatabaseSessionInternal db) {
    Function func = createFunction(db);

    db.executeInTx(() -> {
      Map<Object, Object> args = new HashMap<>();
      args.put("note", "test");

      new ScheduledEventBuilder()
          .setName(db, "test")
          .setRule(db, "0/1 * * * * ?")
          .setFunction(db, func)
          .setArguments(db, args)
          .build(db);
    });
  }

  private Function createFunction(DatabaseSessionInternal db) {
    db.getMetadata().getSchema().createClass("scheduler_log");

    return db.computeInTx(
        () -> {
          Function func = db.getMetadata().getFunctionLibrary().createFunction("logEvent");
          func.setLanguage(db, "SQL");
          func.setCode(db, "insert into scheduler_log set timestamp = sysdate(), note = :note");
          final List<String> pars = new ArrayList<>();
          pars.add("note");
          func.setParameters(db, pars);
          func.save(db);
          return func;
        });
  }

  private Long getLogCounter(final DatabaseSession db) {
    ResultSet resultSet =
        db.query("select count(*) as count from scheduler_log where note = 'test'");
    Result result = resultSet.stream().findFirst().orElseThrow();
    var count = result.<Long>getProperty("count");
    resultSet.close();
    return count;
  }

  private static class TestScheduleDatabaseFactory implements DatabaseThreadLocalFactory {

    private final YouTrackDB context;
    private final String database;
    private final String username;
    private final String password;

    public TestScheduleDatabaseFactory(
        YouTrackDB context, String database, String username, String password) {
      this.context = context;
      this.database = database;
      this.username = username;
      this.password = password;
    }

    @Override
    public DatabaseSessionInternal getThreadDatabase() {
      return (DatabaseSessionInternal) context.cachedPool(database, username, password).acquire();
    }
  }
}
