package com.jetbrains.youtrack.db.internal.jdbc;

import java.sql.Statement;
import java.util.HashMap;
import org.assertj.core.api.Assertions;
import org.junit.Test;

/**
 *
 */
public class YouTrackDbJdbcIssuesTest extends YouTrackDbJdbcDbPerMethodTemplateTest {

  @Test
  public void shouldMapNullValues_ph8555() throws Exception {

    var commands =
        "CREATE CLASS Demo;\n"
            //        + "CREATE PROPERTY Demo.firstName STRING\n"
            //        + "CREATE PROPERTY Demo.lastName STRING\n"
            //        + "CREATE PROPERTY Demo.address STRING\n"
            //        + "CREATE PROPERTY Demo.amount INTEGER\n"
            + "INSERT INTO Demo(firstName, lastName, address, amount) VALUES (\"John\", \"John\","
            + " \"Street1\", 1234);\n"
            + "INSERT INTO Demo(firstName, lastName, amount) VALUES (\"Lars\", \"Lar\", 2232);\n"
            + "INSERT INTO Demo(firstName, amount) VALUES (\"Lars\", 2232);";

    var stmt = conn.createStatement();
    stmt.addBatch("CREATE CLASS Demo;");
    stmt.addBatch("begin;");
    stmt.addBatch(
        "INSERT INTO Demo(firstName, lastName, address, amount) VALUES (\"John\", \"John\","
            + " \"Street1\", 1234);");
    stmt.addBatch(
        "INSERT INTO Demo(firstName, lastName, amount) VALUES (\"Lars\", \"Lar\", 2232);");
    stmt.addBatch("INSERT INTO Demo(firstName, amount) VALUES (\"Lars\", 2232);");
    stmt.addBatch("commit;");
    stmt.executeBatch();
    stmt.close();

    stmt = conn.createStatement();
    var resSet =
        (YouTrackDbJdbcResultSet)
            stmt.executeQuery("select firstName , lastName , address, amount from Demo");

    while (resSet.next()) {
      var item = new HashMap<String, Object>();

      var numCols = resSet.getMetaData().getColumnCount();

      for (var i = 1; i <= numCols; i++) {
        var colName = resSet.getMetaData().getColumnName(i);
        var value = resSet.getObject(colName);
        item.put(colName, value);
      }

      Assertions.assertThat(item).containsKeys("firstName", "lastName", "address", "amount");
    }

    /**
     * Expected: [{firstName=John, lastName=John, amount=1234, address=Street1}, {firstName=Lars,
     * lastName=Lar, amount=2232}, {firstName=Lars, amount=2232}] Result: [{firstName=John,
     * lastName=John, amount=1234, address=Street1}, {firstName=Lars, lastName=Lar, address=null},
     * {firstName=Lars, lastName=null}]
     */
  }
}
