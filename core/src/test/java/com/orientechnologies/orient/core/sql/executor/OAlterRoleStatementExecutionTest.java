package com.orientechnologies.orient.core.sql.executor;

import com.orientechnologies.BaseMemoryDatabase;
import com.orientechnologies.orient.core.metadata.security.OSecurityInternal;
import com.orientechnologies.orient.core.metadata.security.OSecurityPolicyImpl;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class OAlterRoleStatementExecutionTest extends BaseMemoryDatabase {

  @Test
  public void testAddPolicy() {
    OSecurityInternal security = db.getSharedContext().getSecurity();

    db.createClass("Person");

    db.begin();
    OSecurityPolicyImpl policy = security.createSecurityPolicy(db, "testPolicy");
    policy.setActive(db, true);
    policy.setReadRule(db, "name = 'foo'");
    security.saveSecurityPolicy(db, policy);
    db.command("ALTER ROLE reader SET POLICY testPolicy ON database.class.Person").close();
    db.commit();

    Assert.assertEquals(
        "testPolicy",
        security
            .getSecurityPolicies(db, security.getRole(db, "reader"))
            .get("database.class.Person")
            .getName(db));
  }

  @Test
  public void testRemovePolicy() {
    OSecurityInternal security = db.getSharedContext().getSecurity();

    db.createClass("Person");

    db.begin();
    OSecurityPolicyImpl policy = security.createSecurityPolicy(db, "testPolicy");
    policy.setActive(db, true);
    policy.setReadRule(db, "name = 'foo'");
    security.saveSecurityPolicy(db, policy);
    security.setSecurityPolicy(db, security.getRole(db, "reader"), "database.class.Person", policy);
    db.commit();

    Assert.assertEquals(
        "testPolicy",
        security
            .getSecurityPolicies(db, security.getRole(db, "reader"))
            .get("database.class.Person")
            .getName(db));

    db.begin();
    db.command("ALTER ROLE reader REMOVE POLICY ON database.class.Person").close();
    db.commit();

    Assert.assertNull(
        security
            .getSecurityPolicies(db, security.getRole(db, "reader"))
            .get("database.class.Person"));
  }
}
