package com.jetbrains.youtrack.db.internal.core.sql.executor;

import static org.junit.Assert.assertEquals;

import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.api.query.ResultSet;
import com.jetbrains.youtrack.db.api.record.Entity;
import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.api.schema.Schema;
import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import com.jetbrains.youtrack.db.internal.DbTestBase;
import org.junit.Assert;
import org.junit.Test;

public class RebuildIndexStatementExecutionTest extends DbTestBase {
  @Test
  public void indexAfterRebuildShouldIncludeAllClusters() {
    // given
    Schema schema = db.getMetadata().getSchema();
    var className = "IndexClusterTest";

    var oclass = schema.createClass(className);
    oclass.createProperty(db, "key", PropertyType.STRING);
    oclass.createProperty(db, "value", PropertyType.INTEGER);
    oclass.createIndex(db, className + "index1", SchemaClass.INDEX_TYPE.NOTUNIQUE, "key");

    db.begin();
    var ele = db.newInstance(className);
    ele.setProperty("key", "a");
    ele.setProperty("value", 1);
    db.save(ele);
    db.commit();

    var clId = db.addCluster(className + "secondCluster");
    oclass.addClusterId(db, clId);

    db.begin();
    var ele1 = db.newInstance(className);
    ele1.setProperty("key", "a");
    ele1.setProperty("value", 2);
    db.save(ele1, className + "secondCluster");
    db.commit();

    // when
    var result = db.command("rebuild index " + className + "index1");
    Assert.assertTrue(result.hasNext());
    var resultRecord = result.next();
    Assert.assertEquals(2L, resultRecord.<Object>getProperty("totalIndexed"));
    Assert.assertFalse(result.hasNext());
    assertEquals(
        db.query("select from " + className + " where key = 'a'").stream().toList().size(), 2);
  }
}
