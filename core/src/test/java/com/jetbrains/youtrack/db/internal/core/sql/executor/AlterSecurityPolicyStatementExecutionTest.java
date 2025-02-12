package com.jetbrains.youtrack.db.internal.core.sql.executor;

import com.jetbrains.youtrack.db.internal.DbTestBase;
import com.jetbrains.youtrack.db.internal.core.metadata.security.SecurityPolicy;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class AlterSecurityPolicyStatementExecutionTest extends DbTestBase {

  @Test
  public void testPlain() {
    session.begin();
    session.command("CREATE SECURITY POLICY foo").close();
    session.command("ALTER SECURITY POLICY foo SET READ = (name = 'foo')").close();
    session.commit();

    var security = session.getSharedContext().getSecurity();
    SecurityPolicy policy = security.getSecurityPolicy(session, "foo");
    Assert.assertNotNull(policy);
    Assert.assertNotNull("foo", policy.getName(session));
    Assert.assertEquals("name = 'foo'", policy.getReadRule(session));
    Assert.assertNull(policy.getCreateRule(session));
    Assert.assertNull(policy.getBeforeUpdateRule(session));
    Assert.assertNull(policy.getAfterUpdateRule(session));
    Assert.assertNull(policy.getDeleteRule(session));
    Assert.assertNull(policy.getExecuteRule(session));

    session.begin();
    session.command("ALTER SECURITY POLICY foo REMOVE READ").close();
    session.commit();

    policy = security.getSecurityPolicy(session, "foo");
    Assert.assertNotNull(policy);
    Assert.assertNull(policy.getReadRule(session));
  }
}
