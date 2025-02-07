package com.jetbrains.youtrack.db.auto;

import com.jetbrains.youtrack.db.api.exception.SchemaException;
import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.api.schema.Schema;
import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.api.query.ResultSet;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

@Test(groups = {"index"})
public class SchemaIndexTest extends BaseDBTest {

  @Parameters(value = "remote")
  public SchemaIndexTest(boolean remote) {
    super(remote);
  }

  @BeforeMethod
  public void beforeMethod() throws Exception {
    super.beforeMethod();

    final Schema schema = database.getMetadata().getSchema();
    final SchemaClass superTest = schema.createClass("SchemaSharedIndexSuperTest");
    final SchemaClass test = schema.createClass("SchemaIndexTest", superTest);
    test.createProperty(database, "prop1", PropertyType.DOUBLE);
    test.createProperty(database, "prop2", PropertyType.DOUBLE);
  }

  @AfterMethod
  public void tearDown() throws Exception {
    if (database.getMetadata().getSchema().existsClass("SchemaIndexTest")) {
      database.command("drop class SchemaIndexTest").close();
    }
    database.command("drop class SchemaSharedIndexSuperTest").close();
  }

  @Test
  public void testDropClass() throws Exception {
    database
        .command(
            "CREATE INDEX SchemaSharedIndexCompositeIndex ON SchemaIndexTest (prop1, prop2) UNIQUE")
        .close();
    database.getMetadata().getIndexManagerInternal().reload(database);
    Assert.assertNotNull(
        database
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(database, "SchemaSharedIndexCompositeIndex"));

    database.getMetadata().getSchema().dropClass("SchemaIndexTest");
    database.getMetadata().getIndexManagerInternal().reload(database);

    Assert.assertNull(database.getMetadata().getSchema().getClass("SchemaIndexTest"));
    Assert.assertNotNull(database.getMetadata().getSchema().getClass("SchemaSharedIndexSuperTest"));

    Assert.assertNull(
        database
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(database, "SchemaSharedIndexCompositeIndex"));
  }

  @Test
  public void testDropSuperClass() throws Exception {
    database
        .command(
            "CREATE INDEX SchemaSharedIndexCompositeIndex ON SchemaIndexTest (prop1, prop2) UNIQUE")
        .close();

    try {
      database.getMetadata().getSchema().dropClass("SchemaSharedIndexSuperTest");
      Assert.fail();
    } catch (SchemaException e) {
      Assert.assertTrue(
          e.getMessage()
              .startsWith(
                  "Class 'SchemaSharedIndexSuperTest' cannot be dropped because it has sub"
                      + " classes"));
    }

    Assert.assertNotNull(database.getMetadata().getSchema().getClass("SchemaIndexTest"));
    Assert.assertNotNull(database.getMetadata().getSchema().getClass("SchemaSharedIndexSuperTest"));

    Assert.assertNotNull(
        database
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(database, "SchemaSharedIndexCompositeIndex"));
  }

  public void testPolymorphicIdsPropagationAfterClusterAddRemove() {
    final Schema schema = database.getMetadata().getSchema();

    SchemaClass polymorpicIdsPropagationSuperSuper =
        schema.getClass("polymorpicIdsPropagationSuperSuper");

    if (polymorpicIdsPropagationSuperSuper == null) {
      polymorpicIdsPropagationSuperSuper = schema.createClass("polymorpicIdsPropagationSuperSuper");
    }

    SchemaClass polymorpicIdsPropagationSuper = schema.getClass("polymorpicIdsPropagationSuper");
    if (polymorpicIdsPropagationSuper == null) {
      polymorpicIdsPropagationSuper = schema.createClass("polymorpicIdsPropagationSuper");
    }

    SchemaClass polymorpicIdsPropagation = schema.getClass("polymorpicIdsPropagation");
    if (polymorpicIdsPropagation == null) {
      polymorpicIdsPropagation = schema.createClass("polymorpicIdsPropagation");
    }

    polymorpicIdsPropagation.setSuperClass(database, polymorpicIdsPropagationSuper);
    polymorpicIdsPropagationSuper.setSuperClass(database, polymorpicIdsPropagationSuperSuper);

    polymorpicIdsPropagationSuperSuper.createProperty(database, "value", PropertyType.STRING);
    polymorpicIdsPropagationSuperSuper.createIndex(database,
        "PolymorpicIdsPropagationSuperSuperIndex", SchemaClass.INDEX_TYPE.UNIQUE, "value");

    int counter = 0;

    for (int i = 0; i < 10; i++) {
      database.begin();
      EntityImpl document = new EntityImpl("polymorpicIdsPropagation");
      document.field("value", "val" + counter);
      document.save();
      database.commit();

      counter++;
    }

    final int clusterId2 = database.addCluster("polymorpicIdsPropagationSuperSuper2");

    for (int i = 0; i < 10; i++) {
      EntityImpl document = new EntityImpl();
      document.field("value", "val" + counter);

      database.begin();
      document.save("polymorpicIdsPropagationSuperSuper2");
      database.commit();

      counter++;
    }

    polymorpicIdsPropagation.addCluster(database, "polymorpicIdsPropagationSuperSuper2");

    assertContains(polymorpicIdsPropagationSuperSuper.getPolymorphicClusterIds(), clusterId2);
    assertContains(polymorpicIdsPropagationSuper.getPolymorphicClusterIds(), clusterId2);

    try (ResultSet result =
        database.query("select from polymorpicIdsPropagationSuperSuper where value = 'val12'")) {

      Assert.assertTrue(result.hasNext());

      Result doc = result.next();
      Assert.assertFalse(result.hasNext());
      Assert.assertEquals(doc.getProperty("@class"), "polymorpicIdsPropagation");
    }
    polymorpicIdsPropagation.removeClusterId(database, clusterId2);

    assertDoesNotContain(polymorpicIdsPropagationSuperSuper.getPolymorphicClusterIds(), clusterId2);
    assertDoesNotContain(polymorpicIdsPropagationSuper.getPolymorphicClusterIds(), clusterId2);

    try (ResultSet result =
        database.query("select from polymorpicIdsPropagationSuperSuper  where value = 'val12'")) {

      Assert.assertFalse(result.hasNext());
    }
  }

  public void testIndexWithNumberProperties() {
    SchemaClass oclass = database.getMetadata().getSchema()
        .createClass("SchemaIndexTest_numberclass");
    oclass.createProperty(database, "1", PropertyType.STRING).setMandatory(database, false);
    oclass.createProperty(database, "2", PropertyType.STRING).setMandatory(database, false);
    oclass.createIndex(database, "SchemaIndexTest_numberclass_1_2", SchemaClass.INDEX_TYPE.UNIQUE,
        "1",
        "2");

    database.getMetadata().getSchema().dropClass(oclass.getName());
  }

  private void assertContains(int[] clusterIds, int clusterId) {
    boolean contains = false;
    for (int cluster : clusterIds) {
      if (cluster == clusterId) {
        contains = true;
        break;
      }
    }

    Assert.assertTrue(contains);
  }

  private void assertDoesNotContain(int[] clusterIds, int clusterId) {
    boolean contains = false;
    for (int cluster : clusterIds) {
      if (cluster == clusterId) {
        contains = true;
        break;
      }
    }

    Assert.assertFalse(contains);
  }
}
