package com.jetbrains.youtrack.db.internal.core.sql.executor;

import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.api.query.ResultSet;
import com.jetbrains.youtrack.db.internal.DbTestBase;
import java.util.Collection;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class TraverseStatementExecutionTest extends DbTestBase {

  @Test
  public void testPlainTraverse() {
    var classPrefix = "testPlainTraverse_";
    db.createVertexClass(classPrefix + "V");
    db.createEdgeClass(classPrefix + "E");

    db.begin();
    db.command("create vertex " + classPrefix + "V set name = 'a'").close();
    db.command("create vertex " + classPrefix + "V set name = 'b'").close();
    db.command("create vertex " + classPrefix + "V set name = 'c'").close();
    db.command("create vertex " + classPrefix + "V set name = 'd'").close();

    db.command(
            "create edge "
                + classPrefix
                + "E from (select from "
                + classPrefix
                + "V where name = 'a') to (select from "
                + classPrefix
                + "V where name = 'b')")
        .close();
    db.command(
            "create edge "
                + classPrefix
                + "E from (select from "
                + classPrefix
                + "V where name = 'b') to (select from "
                + classPrefix
                + "V where name = 'c')")
        .close();
    db.command(
            "create edge "
                + classPrefix
                + "E from (select from "
                + classPrefix
                + "V where name = 'c') to (select from "
                + classPrefix
                + "V where name = 'd')")
        .close();
    db.commit();

    var result =
        db.query("traverse out() from (select from " + classPrefix + "V where name = 'a')");

    for (var i = 0; i < 4; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertEquals(i, item.getMetadata("$depth"));
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testWithDepth() {
    var classPrefix = "testWithDepth_";
    db.createVertexClass(classPrefix + "V");
    db.createEdgeClass(classPrefix + "E");

    db.begin();
    db.command("create vertex " + classPrefix + "V set name = 'a'").close();
    db.command("create vertex " + classPrefix + "V set name = 'b'").close();
    db.command("create vertex " + classPrefix + "V set name = 'c'").close();
    db.command("create vertex " + classPrefix + "V set name = 'd'").close();

    db.command(
            "create edge "
                + classPrefix
                + "E from (select from "
                + classPrefix
                + "V where name = 'a') to (select from "
                + classPrefix
                + "V where name = 'b')")
        .close();
    db.command(
            "create edge "
                + classPrefix
                + "E from (select from "
                + classPrefix
                + "V where name = 'b') to (select from "
                + classPrefix
                + "V where name = 'c')")
        .close();
    db.command(
            "create edge "
                + classPrefix
                + "E from (select from "
                + classPrefix
                + "V where name = 'c') to (select from "
                + classPrefix
                + "V where name = 'd')")
        .close();
    db.commit();

    var result =
        db.query(
            "traverse out() from (select from "
                + classPrefix
                + "V where name = 'a') WHILE $depth < 2");

    for (var i = 0; i < 2; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertEquals(i, item.getMetadata("$depth"));
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testMaxDepth() {
    var classPrefix = "testMaxDepth";
    db.createVertexClass(classPrefix + "V");
    db.createEdgeClass(classPrefix + "E");

    db.begin();
    db.command("create vertex " + classPrefix + "V set name = 'a'").close();
    db.command("create vertex " + classPrefix + "V set name = 'b'").close();
    db.command("create vertex " + classPrefix + "V set name = 'c'").close();
    db.command("create vertex " + classPrefix + "V set name = 'd'").close();

    db.command(
            "create edge "
                + classPrefix
                + "E from (select from "
                + classPrefix
                + "V where name = 'a') to (select from "
                + classPrefix
                + "V where name = 'b')")
        .close();
    db.command(
            "create edge "
                + classPrefix
                + "E from (select from "
                + classPrefix
                + "V where name = 'b') to (select from "
                + classPrefix
                + "V where name = 'c')")
        .close();
    db.command(
            "create edge "
                + classPrefix
                + "E from (select from "
                + classPrefix
                + "V where name = 'c') to (select from "
                + classPrefix
                + "V where name = 'd')")
        .close();
    db.commit();

    var result =
        db.query(
            "traverse out() from (select from " + classPrefix + "V where name = 'a') MAXDEPTH 1");

    for (var i = 0; i < 2; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertEquals(i, item.getMetadata("$depth"));
    }
    Assert.assertFalse(result.hasNext());
    result.close();

    result =
        db.query(
            "traverse out() from (select from " + classPrefix + "V where name = 'a') MAXDEPTH 2");

    for (var i = 0; i < 3; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertEquals(i, item.getMetadata("$depth"));
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testBreadthFirst() {
    var classPrefix = "testBreadthFirst_";
    db.createVertexClass(classPrefix + "V");
    db.createEdgeClass(classPrefix + "E");

    db.begin();
    db.command("create vertex " + classPrefix + "V set name = 'a'").close();
    db.command("create vertex " + classPrefix + "V set name = 'b'").close();
    db.command("create vertex " + classPrefix + "V set name = 'c'").close();
    db.command("create vertex " + classPrefix + "V set name = 'd'").close();

    db.command(
            "create edge "
                + classPrefix
                + "E from (select from "
                + classPrefix
                + "V where name = 'a') to (select from "
                + classPrefix
                + "V where name = 'b')")
        .close();
    db.command(
            "create edge "
                + classPrefix
                + "E from (select from "
                + classPrefix
                + "V where name = 'b') to (select from "
                + classPrefix
                + "V where name = 'c')")
        .close();
    db.command(
            "create edge "
                + classPrefix
                + "E from (select from "
                + classPrefix
                + "V where name = 'c') to (select from "
                + classPrefix
                + "V where name = 'd')")
        .close();
    db.commit();

    var result =
        db.query(
            "traverse out() from (select from "
                + classPrefix
                + "V where name = 'a') STRATEGY BREADTH_FIRST");

    for (var i = 0; i < 4; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertEquals(i, item.getMetadata("$depth"));
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testTraverseInBatchTx() {
    var script = "";
    script += "";

    script += "drop class testTraverseInBatchTx_V if exists unsafe;";
    script += "create class testTraverseInBatchTx_V extends V;";
    script += "create property testTraverseInBatchTx_V.name STRING;";
    script += "drop class testTraverseInBatchTx_E if exists unsafe;";
    script += "create class testTraverseInBatchTx_E extends E;";

    script += "begin;";
    script += "insert into testTraverseInBatchTx_V(name) values ('a'), ('b'), ('c');";
    script +=
        "create edge testTraverseInBatchTx_E from (select from testTraverseInBatchTx_V where name ="
            + " 'a') to (select from testTraverseInBatchTx_V where name = 'b');";
    script +=
        "create edge testTraverseInBatchTx_E from (select from testTraverseInBatchTx_V where name ="
            + " 'b') to (select from testTraverseInBatchTx_V where name = 'c');";
    script +=
        "let top = (select * from (traverse in('testTraverseInBatchTx_E') from (select from"
            + " testTraverseInBatchTx_V where name='c')) where in('testTraverseInBatchTx_E').size()"
            + " == 0);";
    script += "commit;";
    script += "return $top";

    var result = db.execute("sql", script);
    Assert.assertTrue(result.hasNext());
    var item = result.next();
    var val = item.getProperty("value");
    Assert.assertTrue(val instanceof Collection);
    Assert.assertEquals(1, ((Collection) val).size());
    result.close();
  }
}
