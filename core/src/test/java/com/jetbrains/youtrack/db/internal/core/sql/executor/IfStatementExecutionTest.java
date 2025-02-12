package com.jetbrains.youtrack.db.internal.core.sql.executor;

import static org.assertj.core.api.Assertions.assertThat;

import com.jetbrains.youtrack.db.internal.DbTestBase;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class IfStatementExecutionTest extends DbTestBase {

  @Test
  public void testPositive() {
    var results = session.command("if(1=1){ select 1 as a; }");
    Assert.assertTrue(results.hasNext());
    var result = results.next();
    assertThat((Integer) result.getProperty("a")).isEqualTo(1);
    Assert.assertFalse(results.hasNext());
    results.close();
  }

  @Test
  public void testNegative() {
    var results = session.command("if(1=2){ select 1 as a; }");
    Assert.assertFalse(results.hasNext());
    results.close();
  }

  @Test
  public void testIfReturn() {
    var results = session.command("if(1=1){ return 'yes'; }");
    Assert.assertTrue(results.hasNext());
    Assert.assertEquals("yes", results.next().getProperty("value"));
    Assert.assertFalse(results.hasNext());
    results.close();
  }
}
