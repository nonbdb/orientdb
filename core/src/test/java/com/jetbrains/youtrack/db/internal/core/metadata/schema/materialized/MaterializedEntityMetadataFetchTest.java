package com.jetbrains.youtrack.db.internal.core.metadata.schema.materialized;

import static org.junit.Assert.assertEquals;

import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import com.jetbrains.youtrack.db.internal.DbTestBase;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.materialized.entities.EmptyEntity;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.materialized.entities.EntityWithEmbeddedCollections;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.materialized.entities.EntityWithLinkProperties;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.materialized.entities.EntityWithPrimitiveProperties;
import org.junit.Test;

public class MaterializedEntityMetadataFetchTest extends DbTestBase {

  @Test
  public void registerEmptyEntity() {
    var schema = db.getSchema();
    var result = schema.registerMaterializedEntity(EmptyEntity.class);

    validateEmptyEntity(result);
  }

  private void validateEmptyEntity(SchemaClass result) {
    assertEquals(0, result.properties(db).size());
    assertEquals(0, result.getAllSuperClasses().size());
    assertEquals(EmptyEntity.class.getSimpleName(), result.getName());
    assertEquals(EmptyEntity.class, result.getMaterializedEntity());
  }

  @Test
  public void registerEntityWithPrimitiveProperties() {
    var schema = db.getSchema();

    var result = schema.registerMaterializedEntity(EntityWithPrimitiveProperties.class);
    validateEntityWithPrimitiveCollections(result);
  }

  private void validateEntityWithPrimitiveCollections(SchemaClass result) {
    assertEquals(0, result.getAllSuperClasses().size());
    assertEquals(EntityWithPrimitiveProperties.class.getSimpleName(), result.getName());
    assertEquals(EntityWithPrimitiveProperties.class, result.getMaterializedEntity());

    var properties = result.propertiesMap(db);

    assertEquals(7, properties.size());
    assertEquals(PropertyType.INTEGER, properties.get("intProperty").getType());
    assertEquals(PropertyType.LONG, properties.get("longProperty").getType());
    assertEquals(PropertyType.DOUBLE, properties.get("doubleProperty").getType());
    assertEquals(PropertyType.FLOAT, properties.get("floatProperty").getType());
    assertEquals(PropertyType.BOOLEAN, properties.get("booleanProperty").getType());
    assertEquals(PropertyType.BYTE, properties.get("byteProperty").getType());
    assertEquals(PropertyType.SHORT, properties.get("shortProperty").getType());
  }

  @Test
  public void registerEntityWithEmbeddedCollections() {
    var schema = db.getSchema();

    var result = schema.registerMaterializedEntity(EntityWithEmbeddedCollections.class);

    validateEntityWithEmbeddedCollections(result);
  }

  private void validateEntityWithEmbeddedCollections(SchemaClass result) {
    assertEquals(0, result.getAllSuperClasses().size());
    assertEquals(EntityWithEmbeddedCollections.class.getSimpleName(), result.getName());
    assertEquals(EntityWithEmbeddedCollections.class, result.getMaterializedEntity());

    var properties = result.propertiesMap(db);

    assertEquals(3, properties.size());
    assertEquals(PropertyType.EMBEDDEDLIST, properties.get("stringList").getType());
    assertEquals(PropertyType.STRING, properties.get("stringList").getLinkedType());

    assertEquals(PropertyType.EMBEDDEDSET, properties.get("stringSet").getType());
    assertEquals(PropertyType.STRING, properties.get("stringSet").getLinkedType());

    assertEquals(PropertyType.EMBEDDEDMAP, properties.get("integerMap").getType());
    assertEquals(PropertyType.INTEGER, properties.get("integerMap").getLinkedType());
  }

  @Test
  public void registerEntityWithLinkProperties() {
    var schema = db.getSchema();

    var result = schema.registerMaterializedEntity(EntityWithLinkProperties.class);

    validateEntityWithLinkedProperties(result);
  }

  private void validateEntityWithLinkedProperties(SchemaClass result) {
    assertEquals(0, result.getAllSuperClasses().size());
    assertEquals(EntityWithLinkProperties.class.getSimpleName(), result.getName());
    assertEquals(EntityWithLinkProperties.class, result.getMaterializedEntity());

    var properties = result.propertiesMap(db);

    assertEquals(4, properties.size());
    assertEquals(PropertyType.LINK, properties.get("entityWithEmbeddedCollections").getType());

    var linkedType = properties.get("entityWithEmbeddedCollections").getLinkedClass();
    validateEntityWithEmbeddedCollections(linkedType);

    assertEquals(PropertyType.LINKSET,
        properties.get("entityWithPrimitivePropertiesSet").getType());
    linkedType = properties.get("entityWithPrimitivePropertiesSet").getLinkedClass();
    validateEntityWithPrimitiveCollections(linkedType);

    assertEquals(PropertyType.LINKLIST, properties.get("emptyEntityList").getType());
    linkedType = properties.get("emptyEntityList").getLinkedClass();
    validateEmptyEntity(linkedType);

    assertEquals(PropertyType.LINKMAP,
        properties.get("entityWithEmbeddedCollectionsMap").getType());
    linkedType = properties.get("entityWithEmbeddedCollectionsMap").getLinkedClass();
    validateEntityWithEmbeddedCollections(linkedType);
  }
}
