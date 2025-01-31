package com.jetbrains.youtrack.db.internal.core.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.jetbrains.youtrack.db.api.query.ResultSet;
import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.api.schema.Schema;
import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import com.jetbrains.youtrack.db.api.schema.SchemaProperty;
import com.jetbrains.youtrack.db.internal.BaseMemoryInternalDatabase;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.ImmutableSchema;
import com.jetbrains.youtrack.db.internal.core.storage.Storage;
import com.jetbrains.youtrack.db.internal.core.storage.cache.WriteCache;
import com.jetbrains.youtrack.db.internal.core.storage.cluster.PaginatedCluster;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.AbstractPaginatedStorage;
import java.util.Locale;
import org.junit.Assert;
import org.junit.Test;

public class ClassTest extends BaseMemoryInternalDatabase {

  public static final String SHORTNAME_CLASS_NAME = "TestShortName";

  @Test
  public void testShortName() {
    Schema schema = db.getMetadata().getSchema();
    SchemaClass oClass = schema.createClass(SHORTNAME_CLASS_NAME);
    Assert.assertNull(oClass.getShortName());
    Assert.assertNull(queryShortName());

    final Storage storage = db.getStorage();

    if (storage instanceof AbstractPaginatedStorage paginatedStorage) {
      final WriteCache writeCache = paginatedStorage.getWriteCache();
      Assert.assertTrue(
          writeCache.exists(
              SHORTNAME_CLASS_NAME.toLowerCase(Locale.ENGLISH) + PaginatedCluster.DEF_EXTENSION));
    }

    String shortName = "shortname";
    oClass.setShortName(db, shortName);
    assertEquals(shortName, oClass.getShortName());
    assertEquals(shortName, queryShortName());

    // FAILS, saves null value and stores "null" string (not null value) internally
    shortName = "null";
    oClass.setShortName(db, shortName);
    assertEquals(shortName, oClass.getShortName());
    assertEquals(shortName, queryShortName());

    oClass.setShortName(db, null);
    Assert.assertNull(oClass.getShortName());
    Assert.assertNull(queryShortName());

    oClass.setShortName(db, "");
    Assert.assertNull(oClass.getShortName());
    Assert.assertNull(queryShortName());
  }

  @Test
  public void testShortNameSnapshot() {
    Schema schema = db.getMetadata().getSchema();
    SchemaClass oClass = schema.createClass(SHORTNAME_CLASS_NAME);
    Assert.assertNull(oClass.getShortName());

    String shortName = "shortName";
    oClass.setShortName(db, shortName);
    assertEquals(shortName, oClass.getShortName());
    SchemaClass shorted = schema.getClass(shortName);
    Assert.assertNotNull(shorted);
    assertEquals(shortName, shorted.getShortName());
    MetadataInternal intern = db.getMetadata();
    ImmutableSchema immSchema = intern.getImmutableSchemaSnapshot();
    shorted = immSchema.getClass(shortName);
    Assert.assertNotNull(shorted);
    assertEquals(shortName, shorted.getShortName());
  }

  @Test
  public void testRename() {
    Schema schema = db.getMetadata().getSchema();
    SchemaClass oClass = schema.createClass("ClassName");

    final Storage storage = db.getStorage();
    final AbstractPaginatedStorage paginatedStorage = (AbstractPaginatedStorage) storage;
    final WriteCache writeCache = paginatedStorage.getWriteCache();
    Assert.assertTrue(writeCache.exists("classname" + PaginatedCluster.DEF_EXTENSION));

    oClass.setName(db, "ClassNameNew");

    assertFalse(writeCache.exists("classname" + PaginatedCluster.DEF_EXTENSION));
    Assert.assertTrue(writeCache.exists("classnamenew" + PaginatedCluster.DEF_EXTENSION));

    oClass.setName(db, "ClassName");

    assertFalse(writeCache.exists("classnamenew" + PaginatedCluster.DEF_EXTENSION));
    Assert.assertTrue(writeCache.exists("classname" + PaginatedCluster.DEF_EXTENSION));
  }

  @Test
  public void testOClassAndOPropertyDescription() {
    final Schema oSchema = db.getMetadata().getSchema();
    SchemaClass oClass = oSchema.createClass("DescriptionTest");
    SchemaProperty property = oClass.createProperty(db, "property", PropertyType.STRING);
    oClass.setDescription(db, "DescriptionTest-class-description");
    property.setDescription(db, "DescriptionTest-property-description");
    assertEquals(oClass.getDescription(), "DescriptionTest-class-description");
    assertEquals(property.getDescription(), "DescriptionTest-property-description");
    oClass = oSchema.getClass("DescriptionTest");
    property = oClass.getProperty("property");
    assertEquals(oClass.getDescription(), "DescriptionTest-class-description");
    assertEquals(property.getDescription(), "DescriptionTest-property-description");

    oClass = db.getMetadata().getImmutableSchemaSnapshot().getClass("DescriptionTest");
    property = oClass.getProperty("property");
    assertEquals(oClass.getDescription(), "DescriptionTest-class-description");
    assertEquals(property.getDescription(), "DescriptionTest-property-description");
  }

  private String queryShortName() {
    String selectShortNameSQL =
        "select shortName from ( select expand(classes) from metadata:schema )"
            + " where name = \""
            + SHORTNAME_CLASS_NAME
            + "\"";
    try (ResultSet result = db.query(selectShortNameSQL)) {
      String name = result.next().getProperty("shortName");
      assertFalse(result.hasNext());
      return name;
    }
  }
}
