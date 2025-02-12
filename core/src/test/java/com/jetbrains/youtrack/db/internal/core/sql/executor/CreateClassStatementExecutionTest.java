package com.jetbrains.youtrack.db.internal.core.sql.executor;

import com.jetbrains.youtrack.db.api.schema.Schema;
import com.jetbrains.youtrack.db.internal.DbTestBase;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class CreateClassStatementExecutionTest extends DbTestBase {

  @Test
  public void testPlain() {
    var className = "testPlain";
    var result = session.command("create class " + className);
    Schema schema = session.getMetadata().getSchema();
    var clazz = schema.getClass(className);
    Assert.assertNotNull(clazz);
    Assert.assertFalse(clazz.isAbstract(session));
    result.close();
  }

  @Test
  public void testAbstract() {
    var className = "testAbstract";
    var result = session.command("create class " + className + " abstract ");
    Schema schema = session.getMetadata().getSchema();
    var clazz = schema.getClass(className);
    Assert.assertNotNull(clazz);
    Assert.assertTrue(clazz.isAbstract(session));
    result.close();
  }

  @Test
  public void testCluster() {
    var className = "testCluster";
    var result = session.command("create class " + className + " cluster 1235, 1236, 1255");
    Schema schema = session.getMetadata().getSchema();
    var clazz = schema.getClass(className);
    Assert.assertNotNull(clazz);
    Assert.assertFalse(clazz.isAbstract(session));
    Assert.assertEquals(3, clazz.getClusterIds(session).length);
    result.close();
  }

  @Test
  public void testClusters() {
    var className = "testClusters";
    var result = session.command("create class " + className + " clusters 32");
    Schema schema = session.getMetadata().getSchema();
    var clazz = schema.getClass(className);
    Assert.assertNotNull(clazz);
    Assert.assertFalse(clazz.isAbstract(session));
    Assert.assertEquals(32, clazz.getClusterIds(session).length);
    result.close();
  }

  @Test
  public void testIfNotExists() {
    var className = "testIfNotExists";
    var result = session.command("create class " + className + " if not exists");
    Schema schema = session.getMetadata().getSchema();
    var clazz = schema.getClass(className);
    Assert.assertNotNull(clazz);
    result.close();

    result = session.command("create class " + className + " if not exists");
    clazz = schema.getClass(className);
    Assert.assertNotNull(clazz);
    result.close();
  }
}
