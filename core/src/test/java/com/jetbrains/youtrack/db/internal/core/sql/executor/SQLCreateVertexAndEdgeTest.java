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
package com.jetbrains.youtrack.db.internal.core.sql.executor;

import static org.junit.Assert.assertEquals;

import com.jetbrains.youtrack.db.api.query.ResultSet;
import com.jetbrains.youtrack.db.internal.DbTestBase;
import com.jetbrains.youtrack.db.api.exception.CommandExecutionException;
import com.jetbrains.youtrack.db.api.record.RID;
import com.jetbrains.youtrack.db.api.record.Vertex;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SQLCreateVertexAndEdgeTest extends DbTestBase {

  @Test
  public void testCreateEdgeDefaultClass() {
    var vclusterId = db.addCluster("vdefault");
    var eclusterId = db.addCluster("edefault");

    db.command("create class V1 extends V").close();
    db.command("alter class V1 add_cluster vdefault").close();

    db.command("create class E1 extends E").close();
    db.command("alter class E1 add_cluster edefault").close();

    // VERTEXES
    db.begin();
    var v1 = db.command("create vertex").next().getVertex().get();
    db.commit();

    v1 = db.bindToSession(v1);
    Assert.assertEquals("V", v1.getSchemaType().get().getName());

    db.begin();
    var v2 = db.command("create vertex V1").next().getVertex().get();
    db.commit();

    v2 = db.bindToSession(v2);
    Assert.assertEquals("V1", v2.getSchemaType().get().getName());

    db.begin();
    var v3 = db.command("create vertex set brand = 'fiat'").next().getVertex().get();
    db.commit();

    v3 = db.bindToSession(v3);
    Assert.assertEquals("V", v3.getSchemaType().get().getName());
    Assert.assertEquals("fiat", v3.getProperty("brand"));

    db.begin();
    var v4 =
        db.command("create vertex V1 set brand = 'fiat',name = 'wow'").next().getVertex().get();
    db.commit();

    v4 = db.bindToSession(v4);
    Assert.assertEquals("V1", v4.getSchemaType().get().getName());
    Assert.assertEquals("fiat", v4.getProperty("brand"));
    Assert.assertEquals("wow", v4.getProperty("name"));

    db.begin();
    var v5 = db.command("create vertex V1 cluster vdefault").next().getVertex().get();
    db.commit();

    v5 = db.bindToSession(v5);
    Assert.assertEquals("V1", v5.getSchemaType().get().getName());
    Assert.assertEquals(v5.getIdentity().getClusterId(), vclusterId);

    // EDGES
    db.begin();
    var edges =
        db.command("create edge from " + v1.getIdentity() + " to " + v2.getIdentity());
    db.commit();
    assertEquals(1, edges.stream().count());

    db.begin();
    edges = db.command("create edge E1 from " + v1.getIdentity() + " to " + v3.getIdentity());
    db.commit();
    assertEquals(1, edges.stream().count());

    db.begin();
    edges =
        db.command(
            "create edge from " + v1.getIdentity() + " to " + v4.getIdentity() + " set weight = 3");
    db.commit();

    EntityImpl e3 = edges.next().getIdentity().get().getRecord(db);
    Assert.assertEquals("E", e3.getClassName());
    Assert.assertEquals(e3.field("out"), v1);
    Assert.assertEquals(e3.field("in"), v4);
    Assert.assertEquals(3, e3.<Object>field("weight"));

    db.begin();
    edges =
        db.command(
            "create edge E1 from "
                + v2.getIdentity()
                + " to "
                + v3.getIdentity()
                + " set weight = 10");
    db.commit();
    EntityImpl e4 = edges.next().getIdentity().get().getRecord(db);
    Assert.assertEquals("E1", e4.getClassName());
    Assert.assertEquals(e4.field("out"), v2);
    Assert.assertEquals(e4.field("in"), v3);
    Assert.assertEquals(10, e4.<Object>field("weight"));

    db.begin();
    edges =
        db.command(
            "create edge e1 cluster edefault from "
                + v3.getIdentity()
                + " to "
                + v5.getIdentity()
                + " set weight = 17");
    db.commit();
    EntityImpl e5 = edges.next().getIdentity().get().getRecord(db);
    Assert.assertEquals("E1", e5.getClassName());
    Assert.assertEquals(e5.getIdentity().getClusterId(), eclusterId);
  }

  /**
   * from issue #2925
   */
  @Test
  public void testSqlScriptThatCreatesEdge() {
    var start = System.currentTimeMillis();

    try {
      var cmd = "begin\n";
      cmd += "let a = create vertex set script = true\n";
      cmd += "let b = select from v limit 1\n";
      cmd += "let e = create edge from $a to $b\n";
      cmd += "commit retry 100\n";
      cmd += "return $e";

      var result = db.query("select from V");

      var before = result.stream().count();

      db.execute("sql", cmd).close();

      result = db.query("select from V");

      Assert.assertEquals(result.stream().count(), before + 1);
    } catch (Exception ex) {
      System.err.println("commit exception! " + ex);
      ex.printStackTrace(System.err);
    }

    System.out.println("done in " + (System.currentTimeMillis() - start) + "ms");
  }

  @Test
  public void testNewParser() {
    db.begin();
    var v1 = db.command("create vertex").next().getVertex().get();
    db.commit();

    v1 = db.bindToSession(v1);
    Assert.assertEquals("V", v1.getSchemaType().get().getName());

    var vid = v1.getIdentity();

    db.begin();
    db.command("create edge from " + vid + " to " + vid).close();

    db.command("create edge E from " + vid + " to " + vid).close();

    db.command("create edge from " + vid + " to " + vid + " set foo = 'bar'").close();

    db.command("create edge E from " + vid + " to " + vid + " set bar = 'foo'").close();
    db.commit();
  }

  @Test
  public void testCannotAlterEClassname() {
    db.command("create class ETest extends E").close();

    try {
      db.command("alter class ETest name ETest2").close();
      Assert.fail();
    } catch (CommandExecutionException e) {
      Assert.assertTrue(true);
    }

    try {
      db.command("alter class ETest name ETest2 unsafe").close();
      Assert.assertTrue(true);
    } catch (CommandExecutionException e) {
      Assert.fail();
    }
  }

  public void testSqlScriptThatDeletesEdge() {
    var start = System.currentTimeMillis();

    db.command("create vertex V set name = 'testSqlScriptThatDeletesEdge1'").close();
    db.command("create vertex V set name = 'testSqlScriptThatDeletesEdge2'").close();
    db.command(
            "create edge E from (select from V where name = 'testSqlScriptThatDeletesEdge1') to"
                + " (select from V where name = 'testSqlScriptThatDeletesEdge2') set name ="
                + " 'testSqlScriptThatDeletesEdge'")
        .close();

    try {
      var cmd = "BEGIN\n";
      cmd += "LET $groupVertices = SELECT FROM V WHERE name = 'testSqlScriptThatDeletesEdge1'\n";
      cmd += "LET $removeRoleEdge = DELETE edge E WHERE out IN $groupVertices\n";
      cmd += "COMMIT\n";
      cmd += "RETURN $groupVertices\n";

      db.execute("sql", cmd);

      var edges = db.query("select from E where name = 'testSqlScriptThatDeletesEdge'");

      Assert.assertEquals(0, edges.stream().count());
    } catch (Exception ex) {
      System.err.println("commit exception! " + ex);
      ex.printStackTrace(System.err);
    }

    System.out.println("done in " + (System.currentTimeMillis() - start) + "ms");
  }
}
