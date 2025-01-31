package com.jetbrains.youtrack.db.internal.test.server.network.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jetbrains.youtrack.db.internal.core.YouTrackDBConstants;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.apache.hc.core5.http.HttpResponse;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests HTTP "database" command.
 */
public class HttpDatabaseTest extends BaseHttpTest {

  @Test
  public void testCreateDatabaseNoType() throws Exception {
    Assert.assertEquals(
        500,
        setUserName("root")
            .setUserPassword("root")
            .post("database/" + getDatabaseName())
            .getResponse()
            .getCode());
  }

  @Test
  public void testCreateDatabaseWrongPassword() throws Exception {
    Assert.assertEquals(
        401,
        setUserName("root")
            .setUserPassword("wrongPasswod")
            .post("database/wrongpasswd")
            .getResponse()
            .getCode());
  }

  @Test
  public void testCreateAndGetDatabase() throws IOException {

    var pass = new EntityImpl(null);
    pass.setProperty("adminPassword", "admin");
    Assert.assertEquals(
        200,
        setUserName("root")
            .setUserPassword("root")
            .post("database/" + getDatabaseName() + "/memory")
            .payload(pass.toJSON(), CONTENT.JSON)
            .getResponse()
            .getCode());

    HttpResponse response =
        setUserName("admin")
            .setUserPassword("admin")
            .get("database/" + getDatabaseName())
            .getResponse();

    Assert.assertEquals(200, response.getCode());

    var objectMapper = new ObjectMapper();
    var result = objectMapper.readTree(getResponse().getEntity().getContent());

    var server = result.findValue("server");
    Assert.assertEquals(YouTrackDBConstants.getRawVersion(), server.get("version").asText());
  }

  @Test
  public void testCreateQueryAndDropDatabase() throws Exception {
    var pass = new EntityImpl(null);
    pass.setProperty("adminPassword", "admin");
    Assert.assertEquals(
        200,
        setUserName("root")
            .setUserPassword("root")
            .post("database/" + getDatabaseName() + "/memory")
            .payload(pass.toJSON(), CONTENT.JSON)
            .getResponse()
            .getCode());

    Assert.assertEquals(
        200,
        setUserName("admin")
            .setUserPassword("admin")
            .get(
                "query/"
                    + getDatabaseName()
                    + "/sql/"
                    + URLEncoder.encode("select from OUSer", StandardCharsets.UTF_8)
                    + "/10")
            .getResponse()
            .getCode());

    Assert.assertEquals(
        204,
        setUserName("root")
            .setUserPassword("root")
            .delete("database/" + getDatabaseName())
            .getResponse()
            .getCode());
  }

  @Test
  public void testDropUnknownDatabase() throws Exception {
    Assert.assertEquals(
        500,
        setUserName("root")
            .setUserPassword("root")
            .delete("database/whateverdbname")
            .getResponse()
            .getCode());
  }

  @Override
  public String getDatabaseName() {
    return "httpdb";
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
