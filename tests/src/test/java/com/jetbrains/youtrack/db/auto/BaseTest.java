package com.jetbrains.youtrack.db.auto;

import com.jetbrains.youtrack.db.api.DatabaseType;
import com.jetbrains.youtrack.db.api.YouTrackDB;
import com.jetbrains.youtrack.db.api.YourTracks;
import com.jetbrains.youtrack.db.api.config.GlobalConfiguration;
import com.jetbrains.youtrack.db.api.config.YouTrackDBConfig;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.YouTrackDBConfigBuilderImpl;
import com.jetbrains.youtrack.db.internal.core.db.YouTrackDBImpl;
import com.jetbrains.youtrack.db.internal.core.index.Index;
import com.jetbrains.youtrack.db.internal.server.YouTrackDBServer;
import java.util.Locale;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

@Test
public abstract class BaseTest<T extends DatabaseSessionInternal> {

  public static final String SERVER_PASSWORD =
      "D2AFD02F20640EC8B7A5140F34FCA49D2289DB1F0D0598BB9DE8AAA75A0792F3";
  private YouTrackDBServer server;

  public static final String DEFAULT_DB_NAME = "demo";

  protected T session;
  protected String dbName;

  protected boolean remoteDB = false;
  protected DatabaseType databaseType;

  public static YouTrackDB youTrackDB;

  protected BaseTest() {
  }

  @Parameters(value = "remote")
  public BaseTest(boolean remote) {
    var config = System.getProperty("youtrackdb.test.env");

    if ("ci".equals(config) || "release".equals(config)) {
      databaseType = DatabaseType.PLOCAL;
    }

    if (databaseType == null) {
      databaseType = DatabaseType.MEMORY;
    }

    this.remoteDB = remote;
    this.dbName = DEFAULT_DB_NAME;
  }

  @Parameters(value = "remote")
  public BaseTest(boolean remote, String prefix) {
    this(remote);
    this.dbName = prefix + DEFAULT_DB_NAME;
  }

  @BeforeSuite
  public void beforeSuite() {
    try {
      if (remoteDB && server == null) {
        server = new YouTrackDBServer(false);
        server.startup(
            BaseTest.class.getClassLoader().getResourceAsStream("youtrackdb-server-config.xml"));
        server.activate();
      }

      if (youTrackDB == null) {
        var builder = new YouTrackDBConfigBuilderImpl();
        if (remoteDB) {
          youTrackDB =
              new YouTrackDBImpl("remote:localhost", "root", SERVER_PASSWORD,
                  createConfig(builder));
        } else {
          final var buildDirectory = System.getProperty("buildDirectory", ".");
          youTrackDB = YourTracks.embedded(buildDirectory + "/test-db", createConfig(builder));
        }
      }

      createDatabase();
    } catch (Exception e) {
      throw new IllegalStateException(
          "Cannot create database in test " + this.getClass().getSimpleName(), e);
    }
  }

  protected void createDatabase(String dbName) {
    youTrackDB.createIfNotExists(
        dbName,
        databaseType,
        "admin",
        "admin",
        "admin",
        "writer",
        "writer",
        "writer",
        "reader",
        "reader",
        "reader");
  }

  protected void createDatabase() {
    createDatabase(dbName);
  }

  protected void dropDatabase(String dbName) {
    if (youTrackDB.exists(dbName)) {
      youTrackDB.drop(dbName);
    }
  }

  @AfterSuite
  public void afterSuite() {
    try {
      if (youTrackDB != null) {
        youTrackDB.close();
        youTrackDB = null;
      }

      if (remoteDB && server != null) {
        server.shutdown();
        server = null;
      }
    } catch (Exception e) {
      throw new IllegalStateException(
          "Cannot close database instance in test " + this.getClass().getSimpleName(), e);
    }
  }

  @BeforeClass
  public void beforeClass() throws Exception {
    createDatabase();
    newSession();
  }

  @BeforeMethod
  public void beforeMethod() throws Exception {
    newSession();
  }

  private void newSession() {
    try {
      if (session == null) {
        session = createSessionInstance();
      }

      session.activateOnCurrentThread();
      if (session.isClosed()) {
        session = createSessionInstance();
      }
    } catch (Exception e) {
      throw new IllegalStateException(
          "Cannot open database session in test " + this.getClass().getSimpleName(), e);
    }
  }

  @AfterClass
  public void afterClass() throws Exception {
    closeSession();
  }

  @AfterMethod
  public void afterMethod() throws Exception {
    closeSession();
  }

  private void closeSession() {
    try {
      if (!session.isClosed()) {
        session.activateOnCurrentThread();
        session.close();
      }
    } catch (Exception e) {
      throw new IllegalStateException(
          "Cannot close database session in test " + this.getClass().getSimpleName(), e);
    }
  }

  protected abstract T createSessionInstance(
      YouTrackDB youTrackDB, String dbName, String user, String password);

  protected final T createSessionInstance() {
    return createSessionInstance("admin", "admin");
  }

  protected final T createSessionInstance(String dbName) {
    return createSessionInstance(dbName, "admin", "admin");
  }

  protected final T createSessionInstance(String dbName, String user, String password) {
    return createSessionInstance(youTrackDB, dbName, user, password);
  }

  protected final T createSessionInstance(String user, String password) {
    return createSessionInstance(dbName, user, password);
  }

  protected DatabaseSessionInternal acquireSession() {
    return acquireSession(dbName);
  }

  protected DatabaseSessionInternal acquireSession(String dbName) {
    return (DatabaseSessionInternal) youTrackDB.open(dbName, "admin", "admin");
  }

  protected YouTrackDBConfig createConfig(YouTrackDBConfigBuilderImpl builder) {
    builder.addGlobalConfigurationParameter(GlobalConfiguration.NON_TX_READS_WARNING_MODE,
        "SILENT");
    return builder.build();
  }

  protected static String getTestEnv() {
    return System.getProperty("youtrackdb.test.env");
  }

  protected final String getStorageType() {
    return databaseType.toString().toLowerCase(Locale.ROOT);
  }

  protected void checkEmbeddedDB() {
    if (session.getStorage().isRemote()) {
      throw new SkipException("Test is running only in embedded database");
    }
  }

  protected Index getIndex(final String indexName) {
    final DatabaseSessionInternal db = this.session;

    return (db.getMetadata()).getIndexManagerInternal().getIndex(db, indexName);
  }
}
