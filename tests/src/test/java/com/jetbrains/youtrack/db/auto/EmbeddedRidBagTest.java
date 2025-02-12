package com.jetbrains.youtrack.db.auto;

import com.jetbrains.youtrack.db.api.config.GlobalConfiguration;
import com.jetbrains.youtrack.db.internal.client.remote.ServerAdmin;
import java.io.IOException;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

@Test
public class EmbeddedRidBagTest extends RidBagTest {

  private int topThreshold;
  private int bottomThreshold;

  @Parameters(value = "remote")
  public EmbeddedRidBagTest(@Optional Boolean remote) {
    super(remote != null && remote);
  }

  @BeforeMethod
  public void beforeMethod() throws IOException {
    topThreshold =
        GlobalConfiguration.RID_BAG_EMBEDDED_TO_SBTREEBONSAI_THRESHOLD.getValueAsInteger();
    bottomThreshold =
        GlobalConfiguration.RID_BAG_SBTREEBONSAI_TO_EMBEDDED_THRESHOLD.getValueAsInteger();

    GlobalConfiguration.RID_BAG_EMBEDDED_TO_SBTREEBONSAI_THRESHOLD.setValue(Integer.MAX_VALUE);
    GlobalConfiguration.RID_BAG_SBTREEBONSAI_TO_EMBEDDED_THRESHOLD.setValue(Integer.MAX_VALUE);

    if (session.isRemote()) {
      var server = new ServerAdmin(session.getURL()).connect("root", SERVER_PASSWORD);
      server.setGlobalConfiguration(
          GlobalConfiguration.RID_BAG_EMBEDDED_TO_SBTREEBONSAI_THRESHOLD, Integer.MAX_VALUE);
      server.setGlobalConfiguration(
          GlobalConfiguration.RID_BAG_SBTREEBONSAI_TO_EMBEDDED_THRESHOLD, Integer.MAX_VALUE);
      server.close();
    }
  }

  @AfterMethod
  public void afterMethod() throws IOException {
    GlobalConfiguration.RID_BAG_EMBEDDED_TO_SBTREEBONSAI_THRESHOLD.setValue(topThreshold);
    GlobalConfiguration.RID_BAG_SBTREEBONSAI_TO_EMBEDDED_THRESHOLD.setValue(bottomThreshold);

    if (session.isRemote()) {
      var server = new ServerAdmin(session.getURL()).connect("root", SERVER_PASSWORD);
      server.setGlobalConfiguration(
          GlobalConfiguration.RID_BAG_EMBEDDED_TO_SBTREEBONSAI_THRESHOLD, topThreshold);
      server.setGlobalConfiguration(
          GlobalConfiguration.RID_BAG_SBTREEBONSAI_TO_EMBEDDED_THRESHOLD, bottomThreshold);
      server.close();
    }
  }

  @Override
  protected void assertEmbedded(boolean isEmbedded) {
    Assert.assertTrue(isEmbedded);
  }
}
