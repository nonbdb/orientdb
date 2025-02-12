package com.jetbrains.youtrack.db.internal.core.sql.executor;

import com.jetbrains.youtrack.db.internal.DbTestBase;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class DropUserStatementExecutionTest extends DbTestBase {

  @Test
  public void testPlain() {
    session.begin();
    var result = session.command("CREATE USER test IDENTIFIED BY foo ROLE admin");
    session.commit();
    result.close();

    result = session.query("SELECT name, roles.name as roles FROM OUser WHERE name = 'test'");
    Assert.assertTrue(result.hasNext());
    var user = result.next();
    Assert.assertEquals("test", user.getProperty("name"));
    List<String> roles = user.getProperty("roles");
    Assert.assertEquals(1, roles.size());
    Assert.assertEquals("admin", roles.get(0));
    result.close();

    session.begin();
    result = session.command("DROP USER test");
    session.commit();
    result.close();

    result = session.query("SELECT name, roles.name as roles FROM OUser WHERE name = 'test'");
    Assert.assertFalse(result.hasNext());
    result.close();
  }
}
