package com.jetbrains.youtrack.db.internal.core.sql.executor;

import com.jetbrains.youtrack.db.api.query.ResultSet;
import com.jetbrains.youtrack.db.internal.DbTestBase;
import com.jetbrains.youtrack.db.internal.core.metadata.security.SecurityInternal;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class CreateSecurityPolicyStatementExecutionTest extends DbTestBase {

  @Test
  public void testPlain() {
    db.begin();
    var result = db.command("CREATE SECURITY POLICY foo");
    result.close();
    db.commit();

    db.begin();
    var security = db.getSharedContext().getSecurity();
    Assert.assertNotNull(security.getSecurityPolicy(db, "foo"));
    db.commit();
  }
}
