package com.jetbrains.youtrack.db.internal.server.http;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests HTTP errors command.
 */
public class HttpErrorsTest extends BaseHttpTest {

  @Test
  public void testCommandNotFound() throws Exception {
    Assert.assertEquals(
        setUserName("root").setUserPassword("root").get("commandNotfound").getResponse().getCode(),
        405);
  }

  @Test
  public void testCommandWrongMethod() throws Exception {
    Assert.assertEquals(
        setUserName("root").setUserPassword("root").post("listDatabases").getResponse().getCode(),
        405);
  }

  @Override
  public String getDatabaseName() {
    return "httperrors";
  }

  @Before
  public void startServer() throws Exception {
    super.startServer();
  }

  @After
  public void stopServer() throws Exception {
    super.stopServer();
  }
}
