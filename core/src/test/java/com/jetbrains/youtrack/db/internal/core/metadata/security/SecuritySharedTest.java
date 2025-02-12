package com.jetbrains.youtrack.db.internal.core.metadata.security;

import com.jetbrains.youtrack.db.internal.DbTestBase;
import org.junit.Assert;
import org.junit.Test;

public class SecuritySharedTest extends DbTestBase {

  @Test
  public void testCreateSecurityPolicy() {
    var security = session.getSharedContext().getSecurity();
    session.begin();
    security.createSecurityPolicy(session, "testPolicy");
    session.commit();
    Assert.assertNotNull(security.getSecurityPolicy(session, "testPolicy"));
  }

  @Test
  public void testDeleteSecurityPolicy() {
    var security = session.getSharedContext().getSecurity();
    session.begin();
    security.createSecurityPolicy(session, "testPolicy");
    session.commit();

    session.begin();
    security.deleteSecurityPolicy(session, "testPolicy");
    session.commit();

    Assert.assertNull(security.getSecurityPolicy(session, "testPolicy"));
  }

  @Test
  public void testUpdateSecurityPolicy() {
    var security = session.getSharedContext().getSecurity();
    session.begin();
    var policy = security.createSecurityPolicy(session, "testPolicy");
    policy.setActive(session, true);
    policy.setReadRule(session, "name = 'foo'");
    security.saveSecurityPolicy(session, policy);
    session.commit();

    Assert.assertTrue(security.getSecurityPolicy(session, "testPolicy").isActive(session));
    Assert.assertEquals("name = 'foo'",
        security.getSecurityPolicy(session, "testPolicy").getReadRule(session));
  }

  @Test
  public void testBindPolicyToRole() {
    var security = session.getSharedContext().getSecurity();

    session.createClass("Person");

    session.begin();
    var policy = security.createSecurityPolicy(session, "testPolicy");
    policy.setActive(session, true);
    policy.setReadRule(session, "name = 'foo'");
    security.saveSecurityPolicy(session, policy);
    security.setSecurityPolicy(session, security.getRole(session, "reader"),
        "database.class.Person", policy);
    session.commit();

    Assert.assertEquals(
        "testPolicy",
        security
            .getSecurityPolicies(session, security.getRole(session, "reader"))
            .get("database.class.Person")
            .getName(session));
  }

  @Test
  public void testUnbindPolicyFromRole() {
    var security = session.getSharedContext().getSecurity();

    session.createClass("Person");

    session.begin();
    var policy = security.createSecurityPolicy(session, "testPolicy");
    policy.setActive(session, true);
    policy.setReadRule(session, "name = 'foo'");
    security.saveSecurityPolicy(session, policy);
    security.setSecurityPolicy(session, security.getRole(session, "reader"),
        "database.class.Person", policy);
    session.commit();

    session.begin();
    security.removeSecurityPolicy(session, security.getRole(session, "reader"),
        "database.class.Person");
    session.commit();

    Assert.assertNull(
        security
            .getSecurityPolicies(session, security.getRole(session, "reader"))
            .get("database.class.Person"));
  }
}
