package com.jetbrains.youtrack.db.internal.core.sql.executor;

import com.jetbrains.youtrack.db.api.exception.CommandSQLParsingException;
import com.jetbrains.youtrack.db.api.exception.ConcurrentModificationException;
import com.jetbrains.youtrack.db.internal.DbTestBase;
import java.math.BigDecimal;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class ScriptExecutionTest extends DbTestBase {

  @Test
  public void testTwoInserts() {
    var className = "testTwoInserts";
    session.createClass(className);
    session.execute(
        "SQL",
        "begin;INSERT INTO "
            + className
            + " SET name = 'foo';INSERT INTO "
            + className
            + " SET name = 'bar';commit;");
    var rs = session.query("SELECT count(*) as count from " + className);
    Assert.assertEquals((Object) 2L, rs.next().getProperty("count"));
  }

  @Test
  public void testIf() {
    var className = "testIf";
    session.createClass(className);
    var script = "begin;";
    script += "INSERT INTO " + className + " SET name = 'foo';";
    script += "LET $1 = SELECT count(*) as count FROM " + className + " WHERE name ='bar';";
    script += "IF($1.size() = 0 OR $1[0].count = 0){";
    script += "   INSERT INTO " + className + " SET name = 'bar';";
    script += "}";
    script += "LET $2 = SELECT count(*) as count FROM " + className + " WHERE name ='bar';";
    script += "IF($2.size() = 0 OR $2[0].count = 0){";
    script += "   INSERT INTO " + className + " SET name = 'bar';";
    script += "};";
    script += "commit;";
    session.execute("SQL", script);
    var rs = session.query("SELECT count(*) as count from " + className);
    Assert.assertEquals((Object) 2L, rs.next().getProperty("count"));
  }

  @Test
  public void testReturnInIf() {
    var className = "testReturnInIf";
    session.createClass(className);
    var script = "";
    script += "begin;";
    script += "INSERT INTO " + className + " SET name = 'foo';";
    script += "LET $1 = SELECT count(*) as count FROM " + className + " WHERE name ='foo';";
    script += "IF($1.size() = 0 OR $1[0].count = 0){";
    script += "   INSERT INTO " + className + " SET name = 'bar';";
    script += "   commit;";
    script += "   RETURN;";
    script += "}";
    script += "INSERT INTO " + className + " SET name = 'baz';";
    script += "commit;";

    session.execute("SQL", script);
    var rs = session.query("SELECT count(*) as count from " + className);
    Assert.assertEquals((Object) 2L, rs.next().getProperty("count"));
  }

  @Test
  public void testReturnInIf2() {
    var className = "testReturnInIf2";
    session.createClass(className);
    var script = "begin;";
    script += "INSERT INTO " + className + " SET name = 'foo';";
    script += "commit;";
    script += "LET $1 = SELECT count(*) as count FROM " + className + " WHERE name ='foo';";
    script += "IF($1.size() > 0 ){";
    script += "   RETURN 'OK';";
    script += "}";
    script += "RETURN 'FAIL';";
    var result = session.execute("SQL", script);

    var item = result.next();

    Assert.assertEquals("OK", item.getProperty("value"));
    result.close();
  }

  @Test
  public void testReturnInIf3() {
    var className = "testReturnInIf3";
    session.createClass(className);
    var script = "";
    script += "BEGIN;";
    script += "INSERT INTO " + className + " SET name = 'foo';";
    script += "LET $1 = SELECT count(*) as count FROM " + className + " WHERE name ='foo';";
    script += "IF($1.size() = 0 ){";
    script += "   ROLLBACK;";
    script += "   RETURN 'FAIL';";
    script += "}";
    script += "COMMIT;";
    script += "RETURN 'OK';";
    var result = session.execute("SQL", script);

    var item = result.next();

    Assert.assertEquals("OK", item.getProperty("value"));
    result.close();
  }

  @Test
  public void testLazyExecutionPlanning() {
    var script = "";
    script +=
        "LET $1 = SELECT FROM (select expand(classes) from metadata:schema) where name ="
            + " 'nonExistingClass';";
    script += "IF($1.size() > 0) {";
    script += "   SELECT FROM nonExistingClass;";
    script += "   RETURN 'FAIL';";
    script += "}";
    script += "RETURN 'OK';";
    var result = session.execute("SQL", script);

    var item = result.next();

    Assert.assertEquals("OK", item.getProperty("value"));
    result.close();
  }

  @Test
  public void testCommitRetry() {
    var className = "testCommitRetry";
    session.createClass(className);
    var script = "";
    script += "LET $retries = 0;";
    script += "BEGIN;";
    script += "INSERT INTO " + className + " set attempt = $retries;";
    script += "LET $retries = $retries + 1;";
    script += "IF($retries < 5) {";
    script += "  SELECT throwCME(#-1:-1, 1, 1, 1);";
    script += "}";
    script += "COMMIT RETRY 10;";
    session.execute("SQL", script);

    var result = session.query("select from " + className);
    Assert.assertTrue(result.hasNext());
    var item = result.next();
    Assert.assertEquals(4, (int) item.getProperty("attempt"));
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testCommitRetryWithFailure() {
    var className = "testCommitRetryWithFailure";
    session.createClass(className);
    var script = "";
    script += "LET $retries = 0;";
    script += "BEGIN;";
    script += "INSERT INTO " + className + " set attempt = $retries;";
    script += "LET $retries = $retries + 1;";
    script += "SELECT throwCME(#-1:-1, 1, 1, 1);";
    script += "COMMIT RETRY 10;";
    try {
      session.execute("SQL", script);
    } catch (ConcurrentModificationException x) {
    }

    var result = session.query("select from " + className);
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testCommitRetryWithFailureAndContinue() {
    var className = "testCommitRetryWithFailureAndContinue";
    session.createClass(className);
    var script = "";
    script += "LET $retries = 0;";
    script += "BEGIN;";
    script += "INSERT INTO " + className + " set attempt = $retries;";
    script += "LET $retries = $retries + 1;";
    script += "SELECT throwCME(#-1:-1, 1, 1, 1);";
    script += "COMMIT RETRY 10 ELSE CONTINUE;";
    script += "BEGIN;";
    script += "INSERT INTO " + className + " set name = 'foo';";
    script += "COMMIT;";

    session.execute("SQL", script);

    var result = session.query("select from " + className);
    Assert.assertTrue(result.hasNext());
    var item = result.next();
    Assert.assertEquals("foo", item.getProperty("name"));
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testCommitRetryWithFailureScriptAndContinue() {
    var className = "testCommitRetryWithFailureScriptAndContinue";
    session.createClass(className);
    var script = "";
    script += "LET $retries = 0;";
    script += "BEGIN;";
    script += "INSERT INTO " + className + " set attempt = $retries;";
    script += "LET $retries = $retries + 1;";
    script += "SELECT throwCME(#-1:-1, 1, 1, 1);";
    script += "COMMIT RETRY 10 ELSE {";
    script += "BEGIN;";
    script += "INSERT INTO " + className + " set name = 'foo';";
    script += "COMMIT;";
    script += "} AND CONTINUE;";

    session.execute("SQL", script);

    var result = session.query("select from " + className);
    Assert.assertTrue(result.hasNext());
    var item = result.next();
    Assert.assertEquals("foo", item.getProperty("name"));
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testCommitRetryWithFailureScriptAndFail() {
    var className = "testCommitRetryWithFailureScriptAndFail";
    session.createClass(className);
    var script = "";
    script += "LET $retries = 0;";
    script += "BEGIN;";
    script += "INSERT INTO " + className + " set attempt = $retries;";
    script += "LET $retries = $retries + 1;";
    script += "SELECT throwCME(#-1:-1, 1, 1, 1);";
    script += "COMMIT RETRY 10 ELSE {";
    script += "begin;INSERT INTO " + className + " set name = 'foo';commit;";
    script += "} AND FAIL;";

    try {
      session.execute("SQL", script);
      Assert.fail();
    } catch (ConcurrentModificationException e) {

    }

    var result = session.query("select from " + className);
    Assert.assertTrue(result.hasNext());
    var item = result.next();
    Assert.assertEquals("foo", item.getProperty("name"));
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testCommitRetryWithFailureScriptAndFail2() {
    var className = "testCommitRetryWithFailureScriptAndFail2";
    session.createClass(className);
    var script = "";
    script += "LET $retries = 0;";
    script += "BEGIN;";
    script += "INSERT INTO " + className + " set attempt = $retries;";
    script += "LET $retries = $retries + 1;";
    script += "SELECT throwCME(#-1:-1, 1, 1, 1);";
    script += "COMMIT RETRY 10 ELSE {";
    script += "begin;INSERT INTO " + className + " set name = 'foo';commit;";
    script += "}";

    try {
      session.execute("SQL", script);
      Assert.fail();
    } catch (ConcurrentModificationException e) {

    }

    var result = session.query("select from " + className);
    Assert.assertTrue(result.hasNext());
    var item = result.next();
    Assert.assertEquals("foo", item.getProperty("name"));
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testFunctionAsStatement() {
    var script = "";
    script += "decimal('10');";

    try {
      session.command(script);
      Assert.fail();
    } catch (CommandSQLParsingException e) {

    }

    var rs = session.execute("SQL", script);
    Assert.assertTrue(rs.hasNext());
    var item = rs.next();
    Assert.assertTrue(item.getProperty("result") instanceof BigDecimal);
    Assert.assertFalse(rs.hasNext());

    rs.close();
  }

  @Test
  public void testAssignOnEdgeCreate() {
    var script = "";
    script += "create class IndirectEdge if not exists extends E;\n";

    session.execute("sql", script).close();

    script = " begin;\n";
    script += "insert into V set name = 'a', PrimaryName = 'foo1';\n";
    script += "insert into V set name = 'b', PrimaryName = 'foo2';\n";
    script += "insert into V set name = 'c', PrimaryName = 'foo3';\n";
    script += "insert into V set name = 'd', PrimaryName = 'foo4';\n";
    script +=
        "create edge E from (select from V where name = 'a') to (select from V where name ="
            + " 'b');\n";
    script +=
        "create edge E from (select from V where name = 'c') to (select from V where name ="
            + " 'd');\n";
    script += "LET SourceDataset = SELECT expand(out()) from V where name = 'a';\n";
    script += "LET TarDataset = SELECT expand(out()) from V where name = 'c';\n";
    script += "IF ($SourceDataset[0] != $TarDataset[0])\n";
    script += "{\n";
    script +=
        "CREATE EDGE IndirectEdge FROM $SourceDataset To $TarDataset SET Source ="
            + " $SourceDataset[0].PrimaryName;\n";
    script += "};\n";
    script += "commit retry 10;\n";

    session.execute("sql", script).close();

    try (var rs = session.query("select from IndirectEdge")) {
      Assert.assertEquals("foo2", rs.next().getProperty("Source"));
      Assert.assertFalse(rs.hasNext());
    }
  }
}
