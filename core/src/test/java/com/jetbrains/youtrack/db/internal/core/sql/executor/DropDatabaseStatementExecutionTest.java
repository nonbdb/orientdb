package com.jetbrains.youtrack.db.internal.core.sql.executor;

import com.jetbrains.youtrack.db.api.YouTrackDB;
import com.jetbrains.youtrack.db.api.config.YouTrackDBConfig;
import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.api.query.ResultSet;
import com.jetbrains.youtrack.db.internal.DbTestBase;
import com.jetbrains.youtrack.db.internal.core.CreateDatabaseUtil;
import com.jetbrains.youtrack.db.api.config.GlobalConfiguration;
import com.jetbrains.youtrack.db.api.DatabaseSession;
import com.jetbrains.youtrack.db.internal.core.db.YouTrackDBImpl;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class DropDatabaseStatementExecutionTest {

  @Test
  public void testPlain() {
    String dbName = "ODropDatabaseStatementExecutionTest_testPlain";
    YouTrackDB youTrackDb =
        new YouTrackDBImpl(
            DbTestBase.embeddedDBUrl(getClass()),
            YouTrackDBConfig.builder()
                .addGlobalConfigurationParameter(GlobalConfiguration.CREATE_DEFAULT_USERS, false)
                .build());
    try {
      try (ResultSet result =
          youTrackDb.execute(
              "create database "
                  + dbName
                  + " plocal"
                  + " users ( admin identified by '"
                  + CreateDatabaseUtil.NEW_ADMIN_PASSWORD
                  + "' role admin)")) {
        Assert.assertTrue(result.hasNext());
        Result item = result.next();
        Assert.assertEquals(true, item.getProperty("created"));
      }
      Assert.assertTrue(youTrackDb.exists(dbName));

      DatabaseSession session =
          youTrackDb.open(dbName, "admin", CreateDatabaseUtil.NEW_ADMIN_PASSWORD);
      session.close();

      youTrackDb.execute("drop database " + dbName);
      Assert.assertFalse(youTrackDb.exists(dbName));
    } finally {
      youTrackDb.close();
    }
  }

  @Test
  public void testIfExists1() {
    String dbName = "ODropDatabaseStatementExecutionTest_testIfExists1";
    final YouTrackDB youTrackDb =
        new YouTrackDBImpl(
            DbTestBase.embeddedDBUrl(getClass()),
            YouTrackDBConfig.builder()
                .addGlobalConfigurationParameter(GlobalConfiguration.CREATE_DEFAULT_USERS, false)
                .build());
    try {
      try (ResultSet result =
          youTrackDb.execute(
              "create database "
                  + dbName
                  + " plocal"
                  + " users ( admin identified by '"
                  + CreateDatabaseUtil.NEW_ADMIN_PASSWORD
                  + "' role admin)")) {
        Assert.assertTrue(result.hasNext());
        Result item = result.next();
        Assert.assertEquals(true, item.getProperty("created"));
      }
      Assert.assertTrue(youTrackDb.exists(dbName));

      DatabaseSession session =
          youTrackDb.open(dbName, "admin", CreateDatabaseUtil.NEW_ADMIN_PASSWORD);
      session.close();

      youTrackDb.execute("drop database " + dbName + " if exists");
      Assert.assertFalse(youTrackDb.exists(dbName));
    } finally {
      youTrackDb.close();
    }
  }

  @Test
  public void testIfExists2() {
    String dbName = "ODropDatabaseStatementExecutionTest_testIfExists2";
    try (YouTrackDB youTrackDb = new YouTrackDBImpl(
        DbTestBase.embeddedDBUrl(getClass()) + getClass().getSimpleName(),
        YouTrackDBConfig.builder()
            .addGlobalConfigurationParameter(GlobalConfiguration.CREATE_DEFAULT_USERS, false)
            .build())) {
      youTrackDb.execute("drop database " + dbName + " if exists");
      Assert.assertFalse(youTrackDb.exists(dbName));
    }
  }
}
