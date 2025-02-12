package com.jetbrains.youtrack.db.internal.core.sql.executor;

import com.jetbrains.youtrack.db.internal.DbTestBase;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class WhileBlockExecutionTest extends DbTestBase {

  @Test
  public void testPlain() {

    var className = "testPlain";

    session.createClass(className);

    var script = "";
    script += "LET $i = 0;";
    script += "WHILE ($i < 3){\n";
    script += "  begin;\n";
    script += "  insert into " + className + " set value = $i;\n";
    script += "  LET $i = $i + 1;";
    script += "  commit;";
    script += "}";
    script += "SELECT FROM " + className;
    var results = session.execute("sql", script);

    var tot = 0;
    var sum = 0;
    while (results.hasNext()) {
      var item = results.next();
      sum += item.<Integer>getProperty("value");
      tot++;
    }
    Assert.assertEquals(3, tot);
    Assert.assertEquals(3, sum);
    results.close();
  }

  @Test
  public void testReturn() {
    var className = "testReturn";

    session.createClass(className);

    var script = "";
    script += "LET $i = 0;";
    script += "WHILE ($i < 3){\n";
    script += "  begin;\n";
    script += "  insert into " + className + " set value = $i;\n";
    script += "   commit;\n";
    script += "  IF ($i = 1) {";
    script += "    RETURN;";
    script += "  }";
    script += "  LET $i = $i + 1;";
    script += "}";

    var results = session.execute("sql", script);
    results.close();
    results = session.query("SELECT FROM " + className);

    var tot = 0;
    var sum = 0;
    while (results.hasNext()) {
      var item = results.next();
      sum += item.<Integer>getProperty("value");
      tot++;
    }
    Assert.assertEquals(2, tot);
    Assert.assertEquals(1, sum);
    results.close();
  }
}
