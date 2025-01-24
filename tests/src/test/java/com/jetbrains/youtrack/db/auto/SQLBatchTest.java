/*
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.youtrack.db.auto;

import com.jetbrains.youtrack.db.api.exception.CommandExecutionException;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

@Test
public class SQLBatchTest extends BaseDBTest {

  @Parameters(value = "remote")
  public SQLBatchTest(@Optional Boolean remote) {
    super(remote != null && remote);
  }

  @Test(enabled = false)
  public void createEdgeFailIfNoSourceOrTargetVertices() {
    try {
      db.execute("sql",
          """
              BEGIN;
              LET credential = INSERT INTO V SET email = '123', password = '123';
              LET order = SELECT FROM V WHERE cannotFindThisAttribute = true;
              LET edge = CREATE EDGE E FROM $credential TO $order set crazyName = 'yes';
              COMMIT;
              RETURN $credential;""");

      Assert.fail("Tx has been committed while a rollback was expected");
    } catch (CommandExecutionException e) {

      var result = executeQuery("select from V where email = '123'");
      Assert.assertTrue(result.isEmpty());

      result = executeQuery("select from E where crazyName = 'yes'");
      Assert.assertTrue(result.isEmpty());

    }
  }

  public void testInlineArray() {
    String className1 = "SQLBatchTest_testInlineArray1";
    String className2 = "SQLBatchTest_testInlineArray2";
    db.command("CREATE CLASS " + className1 + " EXTENDS V").close();
    db.command("CREATE CLASS " + className2 + " EXTENDS V").close();
    db.command("CREATE PROPERTY " + className2 + ".foos LinkList " + className1).close();

    String script =
        "BEGIN;"
            + "LET a = CREATE VERTEX "
            + className1
            + ";"
            + "LET b = CREATE VERTEX "
            + className1
            + ";"
            + "LET c = CREATE VERTEX "
            + className1
            + ";"
            + "CREATE VERTEX "
            + className2
            + " SET foos=[$a,$b,$c];"
            + "COMMIT";

    db.execute("sql", script);

    var result = executeQuery("select from " + className2);
    Assert.assertEquals(result.size(), 1);
    List foos = result.getFirst().getProperty("foos");
    Assert.assertEquals(foos.size(), 3);
    Assert.assertTrue(foos.get(0) instanceof Identifiable);
    Assert.assertTrue(foos.get(1) instanceof Identifiable);
    Assert.assertTrue(foos.get(2) instanceof Identifiable);
  }

  public void testInlineArray2() {
    String className1 = "SQLBatchTest_testInlineArray21";
    String className2 = "SQLBatchTest_testInlineArray22";
    db.command("CREATE CLASS " + className1 + " EXTENDS V").close();
    db.command("CREATE CLASS " + className2 + " EXTENDS V").close();
    db.command("CREATE PROPERTY " + className2 + ".foos LinkList " + className1).close();

    String script =
        "BEGIN;\n"
            + "LET a = CREATE VERTEX "
            + className1
            + ";\n"
            + "LET b = CREATE VERTEX "
            + className1
            + ";\n"
            + "LET c = CREATE VERTEX "
            + className1
            + ";\n"
            + "LET foos = [$a,$b,$c];"
            + "CREATE VERTEX "
            + className2
            + " SET foos= $foos;\n"
            + "COMMIT;";

    db.execute("sql", script);

    var result = executeQuery("select from " + className2);
    Assert.assertEquals(result.size(), 1);
    List foos = result.getFirst().getProperty("foos");
    Assert.assertEquals(foos.size(), 3);
    Assert.assertTrue(foos.get(0) instanceof Identifiable);
    Assert.assertTrue(foos.get(1) instanceof Identifiable);
    Assert.assertTrue(foos.get(2) instanceof Identifiable);
  }
}
