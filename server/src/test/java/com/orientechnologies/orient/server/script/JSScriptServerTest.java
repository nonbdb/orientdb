package com.orientechnologies.orient.server.script;

import com.orientechnologies.orient.core.db.YouTrackDB;
import com.orientechnologies.orient.core.db.YouTrackDBConfig;
import com.orientechnologies.orient.core.sql.executor.OResultSet;
import com.orientechnologies.orient.server.OServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

public class JSScriptServerTest {

  @Rule
  public TestName name = new TestName();

  private OServer server;

  @Before
  public void before() throws Exception {

    server =
        OServer.startFromStreamConfig(
            getClass().getResourceAsStream("orientdb-server-javascript-config.xml"));
  }

  @Test
  public void jsPackagesFromConfigTest() {

    YouTrackDB youTrackDB =
        new YouTrackDB("remote:localhost", "root", "root", YouTrackDBConfig.defaultConfig());
    youTrackDB.execute(
        "create database ? memory users (admin identified by 'admin' role admin)",
        name.getMethodName());
    try (var db = youTrackDB.open(name.getMethodName(), "admin", "admin")) {
      try (OResultSet resultSet = db.execute("javascript", "new java.math.BigDecimal(1.0);")) {
        Assert.assertEquals(1, resultSet.stream().count());
      }

      try (OResultSet resultSet = db.execute("javascript", "new java.util.ArrayList();")) {
        Assert.assertEquals(1, resultSet.stream().count());
      }

    } finally {

      youTrackDB.drop(name.getMethodName());
      youTrackDB.close();
    }
  }

  @After
  public void after() {
    server.shutdown();
  }
}
