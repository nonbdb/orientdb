package com.jetbrains.youtrack.db.internal.core.sql.executor;

import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.api.query.ResultSet;
import com.jetbrains.youtrack.db.internal.DbTestBase;
import com.jetbrains.youtrack.db.api.record.Entity;
import com.jetbrains.youtrack.db.api.record.Vertex;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class UpdateEdgeStatementExecutionTest extends DbTestBase {

  @Test
  public void testUpdateEdge() {

    db.command("create class V1 extends V");

    db.command("create class E1 extends E");

    // VERTEXES

    db.begin();
    Entity v1;
    try (ResultSet res1 = db.command("create vertex")) {
      Result r = res1.next();
      Assert.assertEquals(r.getProperty("@class"), "V");
      v1 = r.toEntity();
    }
    db.commit();

    db.begin();
    Entity v2;
    try (ResultSet res2 = db.command("create vertex V1")) {
      Result r = res2.next();
      Assert.assertEquals(r.getProperty("@class"), "V1");
      v2 = r.toEntity();
    }
    db.commit();

    db.begin();
    Entity v3;
    try (ResultSet res3 = db.command("create vertex set vid = 'v3', brand = 'fiat'")) {
      Result r = res3.next();
      Assert.assertEquals(r.getProperty("@class"), "V");
      Assert.assertEquals(r.getProperty("brand"), "fiat");
      v3 = r.toEntity();
    }
    db.commit();

    db.begin();
    Entity v4;
    try (ResultSet res4 =
        db.command("create vertex V1 set vid = 'v4',  brand = 'fiat',name = 'wow'")) {
      Result r = res4.next();
      Assert.assertEquals(r.getProperty("@class"), "V1");
      Assert.assertEquals(r.getProperty("brand"), "fiat");
      Assert.assertEquals(r.getProperty("name"), "wow");
      v4 = r.toEntity();
    }
    db.commit();

    db.begin();
    ResultSet edges =
        db.command("create edge E1 from " + v1.getIdentity() + " to " + v2.getIdentity());
    db.commit();

    Assert.assertTrue(edges.hasNext());
    Result edge = edges.next();
    Assert.assertFalse(edges.hasNext());
    Assert.assertEquals(((EntityImpl) edge.toEntity().getRecord()).getClassName(), "E1");
    edges.close();

    db.begin();
    db.command(
        "update edge E1 set out = "
            + v3.getIdentity()
            + ", in = "
            + v4.getIdentity()
            + " where @rid = "
            + edge.toEntity().getIdentity());
    db.commit();

    ResultSet result = db.query("select expand(out('E1')) from " + v3.getIdentity());
    Assert.assertTrue(result.hasNext());
    Result vertex4 = result.next();
    Assert.assertEquals(vertex4.getProperty("vid"), "v4");
    Assert.assertFalse(result.hasNext());
    result.close();

    result = db.query("select expand(in('E1')) from " + v4.getIdentity());
    Assert.assertTrue(result.hasNext());
    Result vertex3 = result.next();
    Assert.assertEquals(vertex3.getProperty("vid"), "v3");
    Assert.assertFalse(result.hasNext());
    result.close();

    result = db.query("select expand(out('E1')) from " + v1.getIdentity());
    Assert.assertFalse(result.hasNext());
    result.close();

    result = db.query("select expand(in('E1')) from " + v2.getIdentity());
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testUpdateEdgeOfTypeE() {
    // issue #6378

    db.begin();
    Vertex v1 = db.newVertex();
    db.save(v1);
    Vertex v2 = db.newVertex();
    db.save(v2);
    Vertex v3 = db.newVertex();
    db.save(v3);
    db.commit();

    db.begin();
    ResultSet edges =
        db.command("create edge E from " + v1.getIdentity() + " to " + v2.getIdentity());
    db.commit();
    Result edge = edges.next();

    db.begin();
    db.command("UPDATE EDGE " + edge.toEntity().getIdentity() + " SET in = " + v3.getIdentity())
        .close();
    db.commit();
    edges.close();

    ResultSet result = db.query("select expand(out()) from " + v1.getIdentity());

    Assert.assertEquals(result.next().getRecordId(), v3.getIdentity());
    result.close();

    result = db.query("select expand(in()) from " + v3.getIdentity());
    Assert.assertEquals(result.next().getRecordId(), v1.getIdentity());
    result.close();

    result = db.command("select expand(in()) from " + v2.getIdentity());
    Assert.assertFalse(result.hasNext());
    result.close();
  }
}
