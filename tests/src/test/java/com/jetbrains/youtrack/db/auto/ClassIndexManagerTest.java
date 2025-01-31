package com.jetbrains.youtrack.db.auto;

import com.jetbrains.youtrack.db.api.exception.RecordDuplicatedException;
import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.api.schema.Schema;
import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import com.jetbrains.youtrack.db.internal.core.index.CompositeKey;
import com.jetbrains.youtrack.db.internal.core.index.Index;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.SchemaClassInternal;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

@Test
public class ClassIndexManagerTest extends BaseDBTest {

  @Parameters(value = "remote")
  public ClassIndexManagerTest(@Optional Boolean remote) {
    super(remote != null ? remote : false);
  }

  @BeforeClass
  public void beforeClass() throws Exception {
    super.beforeClass();

    final Schema schema = db.getMetadata().getSchema();

    if (schema.existsClass("classIndexManagerTestClass")) {
      schema.dropClass("classIndexManagerTestClass");
    }

    if (schema.existsClass("classIndexManagerTestClassTwo")) {
      schema.dropClass("classIndexManagerTestClassTwo");
    }

    if (schema.existsClass("classIndexManagerTestSuperClass")) {
      schema.dropClass("classIndexManagerTestSuperClass");
    }

    if (schema.existsClass("classIndexManagerTestCompositeCollectionClass")) {
      schema.dropClass("classIndexManagerTestCompositeCollectionClass");
    }

    final var superClass = schema.createClass("classIndexManagerTestSuperClass");
    superClass.createProperty(db, "prop0", PropertyType.STRING);
    superClass.createIndex(db,
        "classIndexManagerTestSuperClass.prop0",
        SchemaClass.INDEX_TYPE.UNIQUE.toString(),
        null,
        Map.of("ignoreNullValues", true),
        new String[]{"prop0"});

    final var oClass = schema.createClass("classIndexManagerTestClass", superClass);
    oClass.createProperty(db, "prop1", PropertyType.STRING);
    oClass.createIndex(db,
        "classIndexManagerTestClass.prop1",
        SchemaClass.INDEX_TYPE.UNIQUE.toString(),
        null,
        Map.of("ignoreNullValues", true),
        new String[]{"prop1"});

    final var propTwo = oClass.createProperty(db, "prop2", PropertyType.INTEGER);
    propTwo.createIndex(db, SchemaClass.INDEX_TYPE.NOTUNIQUE);

    oClass.createProperty(db, "prop3", PropertyType.BOOLEAN);

    final var propFour = oClass.createProperty(db, "prop4", PropertyType.EMBEDDEDLIST,
        PropertyType.STRING);
    propFour.createIndex(db, SchemaClass.INDEX_TYPE.NOTUNIQUE);

    oClass.createProperty(db, "prop5", PropertyType.EMBEDDEDMAP, PropertyType.STRING);
    oClass.createIndex(db, "classIndexManagerTestIndexByKey",
        SchemaClass.INDEX_TYPE.NOTUNIQUE,
        "prop5");
    oClass.createIndex(db,
        "classIndexManagerTestIndexByValue", SchemaClass.INDEX_TYPE.NOTUNIQUE, "prop5 by value");

    final var propSix = oClass.createProperty(db, "prop6", PropertyType.EMBEDDEDSET,
        PropertyType.STRING);
    propSix.createIndex(db, SchemaClass.INDEX_TYPE.NOTUNIQUE);

    oClass.createIndex(db,
        "classIndexManagerComposite",
        SchemaClass.INDEX_TYPE.UNIQUE.toString(),
        null,
        Map.of("ignoreNullValues", true), new String[]{"prop1", "prop2"});

    final var oClassTwo = schema.createClass("classIndexManagerTestClassTwo");
    oClassTwo.createProperty(db, "prop1", PropertyType.STRING);
    oClassTwo.createProperty(db, "prop2", PropertyType.INTEGER);

    final var compositeCollectionClass =
        schema.createClass("classIndexManagerTestCompositeCollectionClass");
    compositeCollectionClass.createProperty(db, "prop1", PropertyType.STRING);
    compositeCollectionClass.createProperty(db, "prop2", PropertyType.EMBEDDEDLIST,
        PropertyType.INTEGER);

    compositeCollectionClass.createIndex(db,
        "classIndexManagerTestIndexValueAndCollection",
        SchemaClass.INDEX_TYPE.UNIQUE.toString(),
        null,
        Map.of("ignoreNullValues", true),
        new String[]{"prop1", "prop2"});

    oClass.createIndex(db,
        "classIndexManagerTestIndexOnPropertiesFromClassAndSuperclass",
        SchemaClass.INDEX_TYPE.UNIQUE.toString(),
        null,
        Map.of("ignoreNullValues", true),
        new String[]{"prop0", "prop1"});

    db.close();
  }

  @AfterMethod
  public void afterMethod() throws Exception {
    db.begin();
    db.command("delete from classIndexManagerTestClass").close();
    db.commit();

    db.begin();
    db.command("delete from classIndexManagerTestClassTwo").close();
    db.commit();

    db.begin();
    db.command("delete from classIndexManagerTestSuperClass").close();
    db.commit();

    if (!db.getStorage().isRemote()) {
      Assert.assertEquals(
          db
              .getMetadata()
              .getIndexManagerInternal()
              .getIndex(db, "classIndexManagerTestClass.prop1")
              .getInternal()
              .size(db),
          0);
      Assert.assertEquals(
          db
              .getMetadata()
              .getIndexManagerInternal()
              .getIndex(db, "classIndexManagerTestClass.prop2")
              .getInternal()
              .size(db),
          0);
    }

    super.afterMethod();
  }

  public void testPropertiesCheckUniqueIndexDubKeysCreate() {
    final var docOne = ((EntityImpl) db.newEntity("classIndexManagerTestClass"));
    final var docTwo = ((EntityImpl) db.newEntity("classIndexManagerTestClass"));

    docOne.field("prop1", "a");
    db.begin();
    docOne.save();
    db.commit();

    var exceptionThrown = false;
    try {
      docTwo.field("prop1", "a");
      db.begin();
      docTwo.save();
      db.commit();

    } catch (RecordDuplicatedException e) {
      exceptionThrown = true;
    }
    Assert.assertTrue(exceptionThrown);
  }

  public void testPropertiesCheckUniqueIndexDubKeyIsNullCreate() {
    final var docOne = ((EntityImpl) db.newEntity("classIndexManagerTestClass"));
    final var docTwo = ((EntityImpl) db.newEntity("classIndexManagerTestClass"));

    docOne.field("prop1", "a");
    db.begin();
    docOne.save();
    db.commit();

    docTwo.field("prop1", (String) null);
    db.begin();
    docTwo.save();
    db.commit();
  }

  public void testPropertiesCheckUniqueIndexDubKeyIsNullCreateInTx() {
    final var docOne = ((EntityImpl) db.newEntity("classIndexManagerTestClass"));
    final var docTwo = ((EntityImpl) db.newEntity("classIndexManagerTestClass"));

    db.begin();
    docOne.field("prop1", "a");
    docOne.save();

    docTwo.field("prop1", (String) null);
    docTwo.save();
    db.commit();
  }

  public void testPropertiesCheckUniqueIndexInParentDubKeysCreate() {
    final var docOne = ((EntityImpl) db.newEntity("classIndexManagerTestClass"));
    final var docTwo = ((EntityImpl) db.newEntity("classIndexManagerTestClass"));

    docOne.field("prop0", "a");
    db.begin();
    docOne.save();
    db.commit();

    var exceptionThrown = false;
    try {
      docTwo.field("prop0", "a");
      db.begin();
      docTwo.save();
      db.commit();
    } catch (RecordDuplicatedException e) {
      exceptionThrown = true;
    }
    Assert.assertTrue(exceptionThrown);
  }

  public void testPropertiesCheckUniqueIndexDubKeysUpdate() {
    db.begin();
    var docOne = ((EntityImpl) db.newEntity("classIndexManagerTestClass"));
    var docTwo = ((EntityImpl) db.newEntity("classIndexManagerTestClass"));

    var exceptionThrown = false;
    docOne.field("prop1", "a");

    docOne.save();
    db.commit();

    db.begin();
    docTwo.field("prop1", "b");

    docTwo.save();
    db.commit();

    try {
      db.begin();
      docTwo = db.bindToSession(docTwo);
      docTwo.field("prop1", "a");

      docTwo.save();
      db.commit();
    } catch (RecordDuplicatedException e) {
      exceptionThrown = true;
    }
    Assert.assertTrue(exceptionThrown);
  }

  public void testPropertiesCheckUniqueIndexDubKeyIsNullUpdate() {
    db.begin();
    var docOne = ((EntityImpl) db.newEntity("classIndexManagerTestClass"));
    var docTwo = ((EntityImpl) db.newEntity("classIndexManagerTestClass"));

    docOne.field("prop1", "a");

    docOne.save();
    db.commit();

    db.begin();
    docTwo.field("prop1", "b");

    docTwo.save();
    db.commit();

    db.begin();
    docTwo = db.bindToSession(docTwo);
    docTwo.field("prop1", (String) null);

    docTwo.save();
    db.commit();
  }

  public void testPropertiesCheckUniqueIndexDubKeyIsNullUpdateInTX() {
    final var docOne = ((EntityImpl) db.newEntity("classIndexManagerTestClass"));
    final var docTwo = ((EntityImpl) db.newEntity("classIndexManagerTestClass"));

    db.begin();
    docOne.field("prop1", "a");
    docOne.save();

    docTwo.field("prop1", "b");
    docTwo.save();

    docTwo.field("prop1", (String) null);
    docTwo.save();
    db.commit();
  }

  public void testPropertiesCheckNonUniqueIndexDubKeys() {
    final var docOne = ((EntityImpl) db.newEntity("classIndexManagerTestClass"));
    docOne.field("prop2", 1);
    db.begin();
    docOne.save();
    db.commit();

    final var docTwo = ((EntityImpl) db.newEntity("classIndexManagerTestClass"));
    docTwo.field("prop2", 1);
    db.begin();
    docTwo.save();
    db.commit();
  }

  public void testPropertiesCheckUniqueNullKeys() {
    final var docOne = ((EntityImpl) db.newEntity("classIndexManagerTestClass"));
    db.begin();
    docOne.save();
    db.commit();

    final var docTwo = ((EntityImpl) db.newEntity("classIndexManagerTestClass"));
    db.begin();
    docTwo.save();
    db.commit();
  }

  public void testCreateDocumentWithoutClass() {
    checkEmbeddedDB();

    final var beforeIndexes =
        db.getMetadata().getIndexManagerInternal().getIndexes(db);
    final Map<String, Long> indexSizeMap = new HashMap<>();

    for (final var index : beforeIndexes) {
      indexSizeMap.put(index.getName(), index.getInternal().size(db));
    }

    db.begin();
    final var docOne = ((EntityImpl) db.newEntity());
    docOne.field("prop1", "a");
    docOne.save();

    final var docTwo = ((EntityImpl) db.newEntity());
    docTwo.field("prop1", "a");
    docTwo.save();
    db.commit();

    final var afterIndexes =
        db.getMetadata().getIndexManagerInternal().getIndexes(db);
    for (final var index : afterIndexes) {
      Assert.assertEquals(
          index.getInternal().size(db), indexSizeMap.get(index.getName()).longValue());
    }
  }

  public void testUpdateDocumentWithoutClass() {
    checkEmbeddedDB();

    final var beforeIndexes =
        db.getMetadata().getIndexManagerInternal().getIndexes(db);
    final Map<String, Long> indexSizeMap = new HashMap<>();

    for (final var index : beforeIndexes) {
      indexSizeMap.put(index.getName(), index.getInternal().size(db));
    }

    db.begin();
    final var docOne = ((EntityImpl) db.newEntity());
    docOne.field("prop1", "a");
    docOne.save();

    final var docTwo = ((EntityImpl) db.newEntity());
    docTwo.field("prop1", "b");
    docTwo.save();

    docOne.field("prop1", "a");
    docOne.save();
    db.commit();

    final var afterIndexes =
        db.getMetadata().getIndexManagerInternal().getIndexes(db);
    for (final var index : afterIndexes) {
      Assert.assertEquals(
          index.getInternal().size(db), indexSizeMap.get(index.getName()).longValue());
    }
  }

  public void testDeleteDocumentWithoutClass() {
    final var docOne = ((EntityImpl) db.newEntity());
    docOne.field("prop1", "a");

    db.begin();
    docOne.save();
    db.commit();

    db.begin();
    db.bindToSession(docOne).delete();
    db.commit();
  }

  public void testDeleteModifiedDocumentWithoutClass() {
    var docOne = ((EntityImpl) db.newEntity());
    docOne.field("prop1", "a");

    db.begin();
    docOne.save();
    db.commit();

    db.begin();
    docOne = db.bindToSession(docOne);
    docOne.field("prop1", "b");
    docOne.delete();
    db.commit();
  }

  public void testDocumentUpdateWithoutDirtyFields() {
    db.begin();
    var docOne = ((EntityImpl) db.newEntity("classIndexManagerTestClass"));
    docOne.field("prop1", "a");

    docOne.save();
    db.commit();

    db.begin();
    docOne = db.bindToSession(docOne);
    docOne.setDirty();
    docOne.save();
    db.commit();
  }

  public void testCreateDocumentIndexRecordAdded() {
    checkEmbeddedDB();

    final var doc = ((EntityImpl) db.newEntity("classIndexManagerTestClass"));
    doc.field("prop0", "x");
    doc.field("prop1", "a");
    doc.field("prop2", 1);

    db.begin();
    doc.save();
    db.commit();

    final Schema schema = db.getMetadata().getSchema();
    final var oClass = schema.getClass("classIndexManagerTestClass");
    final var oSuperClass = schema.getClass("classIndexManagerTestSuperClass");

    final var propOneIndex = db.getMetadata().getIndexManagerInternal()
        .getIndex(db, "classIndexManagerTestClass.prop1");
    try (var stream = propOneIndex.getInternal().getRids(db, "a")) {
      Assert.assertTrue(stream.findFirst().isPresent());
    }
    Assert.assertEquals(propOneIndex.getInternal().size(db), 1);

    final var compositeIndex = db.getMetadata().getIndexManagerInternal()
        .getIndex(db, "classIndexManagerComposite");

    final var compositeIndexDefinition = compositeIndex.getDefinition();
    try (var rids =
        compositeIndex
            .getInternal()
            .getRids(db, compositeIndexDefinition.createValue(db, "a", 1))) {
      Assert.assertTrue(rids.findFirst().isPresent());
    }
    Assert.assertEquals(compositeIndex.getInternal().size(db), 1);

    final var propZeroIndex = db.getMetadata().getIndexManagerInternal().getIndex(db,
        "classIndexManagerTestSuperClass.prop0");
    try (var stream = propZeroIndex.getInternal().getRids(db, "x")) {
      Assert.assertTrue(stream.findFirst().isPresent());
    }
    Assert.assertEquals(propZeroIndex.getInternal().size(db), 1);
  }

  public void testUpdateDocumentIndexRecordRemoved() {
    checkEmbeddedDB();

    db.begin();
    var doc = ((EntityImpl) db.newEntity("classIndexManagerTestClass"));
    doc.field("prop0", "x");
    doc.field("prop1", "a");
    doc.field("prop2", 1);

    doc.save();
    db.commit();

    final Schema schema = db.getMetadata().getSchema();
    final var oSuperClass = schema.getClass("classIndexManagerTestSuperClass");
    final var oClass = schema.getClass("classIndexManagerTestClass");

    final var propOneIndex = db.getMetadata().getIndexManagerInternal()
        .getIndex(db, "classIndexManagerTestClass.prop1");
    final var compositeIndex = db.getMetadata().getIndexManagerInternal()
        .getIndex(db, "classIndexManagerComposite");
    final var propZeroIndex = db.getMetadata().getIndexManagerInternal().getIndex(db,
        "classIndexManagerTestSuperClass.prop0");

    Assert.assertEquals(propOneIndex.getInternal().size(db), 1);
    Assert.assertEquals(compositeIndex.getInternal().size(db), 1);
    Assert.assertEquals(propZeroIndex.getInternal().size(db), 1);

    db.begin();
    doc = db.bindToSession(doc);
    doc.removeField("prop2");
    doc.removeField("prop0");

    doc.save();
    db.commit();

    Assert.assertEquals(propOneIndex.getInternal().size(db), 1);
    Assert.assertEquals(compositeIndex.getInternal().size(db), 0);
    Assert.assertEquals(propZeroIndex.getInternal().size(db), 0);
  }

  public void testUpdateDocumentNullKeyIndexRecordRemoved() {
    checkEmbeddedDB();

    db.begin();
    var doc = ((EntityImpl) db.newEntity("classIndexManagerTestClass"));

    doc.field("prop0", "x");
    doc.field("prop1", "a");
    doc.field("prop2", 1);

    doc.save();
    db.commit();

    final Schema schema = db.getMetadata().getSchema();
    final var oSuperClass = schema.getClass("classIndexManagerTestSuperClass");
    final var oClass = schema.getClass("classIndexManagerTestClass");

    final var propOneIndex = db.getMetadata().getIndexManagerInternal().getIndex(db,
        "classIndexManagerTestClass.prop1");
    final var compositeIndex = db.getMetadata().getIndexManagerInternal()
        .getIndex(db, "classIndexManagerComposite");
    final var propZeroIndex = db.getMetadata().getIndexManagerInternal().getIndex(db,
        "classIndexManagerTestSuperClass.prop0");

    Assert.assertEquals(propOneIndex.getInternal().size(db), 1);
    Assert.assertEquals(compositeIndex.getInternal().size(db), 1);
    Assert.assertEquals(propZeroIndex.getInternal().size(db), 1);

    db.begin();
    doc = db.bindToSession(doc);
    doc.field("prop2", (Object) null);
    doc.field("prop0", (Object) null);
    doc.save();
    db.commit();

    Assert.assertEquals(propOneIndex.getInternal().size(db), 1);
    Assert.assertEquals(compositeIndex.getInternal().size(db), 0);
    Assert.assertEquals(propZeroIndex.getInternal().size(db), 0);
  }

  public void testUpdateDocumentIndexRecordUpdated() {
    checkEmbeddedDB();

    db.begin();
    var doc = ((EntityImpl) db.newEntity("classIndexManagerTestClass"));
    doc.field("prop0", "x");
    doc.field("prop1", "a");
    doc.field("prop2", 1);

    doc.save();
    db.commit();

    final Schema schema = db.getMetadata().getSchema();
    final var oSuperClass = schema.getClass("classIndexManagerTestSuperClass");
    final var oClass = schema.getClass("classIndexManagerTestClass");

    final var propZeroIndex = db.getMetadata().getIndexManagerInternal().getIndex(db,
        "classIndexManagerTestSuperClass.prop0");
    final var propOneIndex = db.getMetadata().getIndexManagerInternal()
        .getIndex(db, "classIndexManagerTestClass.prop1");
    final var compositeIndex = db.getMetadata().getIndexManagerInternal()
        .getIndex(db, "classIndexManagerComposite");
    final var compositeIndexDefinition = compositeIndex.getDefinition();

    Assert.assertEquals(propOneIndex.getInternal().size(db), 1);
    Assert.assertEquals(compositeIndex.getInternal().size(db), 1);
    Assert.assertEquals(propZeroIndex.getInternal().size(db), 1);

    db.begin();
    doc = db.bindToSession(doc);
    doc.field("prop2", 2);
    doc.field("prop0", "y");
    doc.save();
    db.commit();

    Assert.assertEquals(propOneIndex.getInternal().size(db), 1);
    Assert.assertEquals(compositeIndex.getInternal().size(db), 1);
    Assert.assertEquals(propZeroIndex.getInternal().size(db), 1);

    try (var stream = propZeroIndex.getInternal().getRids(db, "y")) {
      Assert.assertTrue(stream.findFirst().isPresent());
    }
    try (var stream = propOneIndex.getInternal().getRids(db, "a")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
    try (var stream =
        compositeIndex
            .getInternal()
            .getRids(db, compositeIndexDefinition.createValue(db, "a", 2))) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
  }

  public void testUpdateDocumentIndexRecordUpdatedFromNullField() {
    checkEmbeddedDB();

    db.begin();
    var doc = ((EntityImpl) db.newEntity("classIndexManagerTestClass"));
    doc.field("prop1", "a");
    doc.field("prop2", (Object) null);

    doc.save();
    db.commit();

    final Schema schema = db.getMetadata().getSchema();
    final var oClass = schema.getClass("classIndexManagerTestClass");

    final var propOneIndex = db.getMetadata().getIndexManagerInternal()
        .getIndex(db, "classIndexManagerTestClass.prop1");
    final var compositeIndex = db.getMetadata().getIndexManagerInternal()
        .getIndex(db, "classIndexManagerComposite");
    final var compositeIndexDefinition = compositeIndex.getDefinition();

    Assert.assertEquals(propOneIndex.getInternal().size(db), 1);
    Assert.assertEquals(compositeIndex.getInternal().size(db), 0);

    db.begin();
    doc = db.bindToSession(doc);
    doc.field("prop2", 2);
    doc.save();
    db.commit();

    Assert.assertEquals(propOneIndex.getInternal().size(db), 1);
    Assert.assertEquals(compositeIndex.getInternal().size(db), 1);

    try (var stream = propOneIndex.getInternal().getRids(db, "a")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
    try (var stream =
        compositeIndex
            .getInternal()
            .getRids(db, compositeIndexDefinition.createValue(db, "a", 2))) {
      Assert.assertTrue(stream.findFirst().isPresent());
    }
  }

  public void testListUpdate() {
    checkEmbeddedDB();

    final Schema schema = db.getMetadata().getSchema();
    final var oClass = schema.getClass("classIndexManagerTestClass");

    final var propFourIndex = db.getMetadata().getIndexManagerInternal()
        .getIndex(db, "classIndexManagerTestClass.prop4");

    Assert.assertEquals(propFourIndex.getInternal().size(db), 0);

    db.begin();
    var doc = ((EntityImpl) db.newEntity("classIndexManagerTestClass"));

    final List<String> listProperty = new ArrayList<>();
    listProperty.add("value1");
    listProperty.add("value2");

    doc.field("prop4", listProperty);
    doc.save();
    db.commit();

    Assert.assertEquals(propFourIndex.getInternal().size(db), 2);
    try (var stream = propFourIndex.getInternal().getRids(db, "value1")) {
      Assert.assertTrue(stream.findFirst().isPresent());
    }
    try (var stream = propFourIndex.getInternal().getRids(db, "value2")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }

    db.begin();
    doc = db.bindToSession(doc);
    List<String> trackedList = doc.field("prop4");
    trackedList.set(0, "value3");

    trackedList.add("value4");
    trackedList.add("value4");
    trackedList.add("value4");
    trackedList.remove("value4");
    trackedList.remove("value2");
    trackedList.add("value5");

    doc.save();
    db.commit();

    Assert.assertEquals(propFourIndex.getInternal().size(db), 3);
    try (var stream = propFourIndex.getInternal().getRids(db, "value3")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
    try (var stream = propFourIndex.getInternal().getRids(db, "value4")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }

    try (var stream = propFourIndex.getInternal().getRids(db, "value5")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
  }

  public void testMapUpdate() {
    checkEmbeddedDB();

    final var propFiveIndexKey = db.getMetadata().getIndexManagerInternal()
        .getIndex(db,
            "classIndexManagerTestIndexByKey");
    final var propFiveIndexValue = db.getMetadata().getIndexManagerInternal()
        .getIndex(db,
            "classIndexManagerTestIndexByValue");

    Assert.assertEquals(propFiveIndexKey.getInternal().size(db), 0);

    db.begin();
    var doc = ((EntityImpl) db.newEntity("classIndexManagerTestClass"));

    final Map<String, String> mapProperty = new HashMap<>();
    mapProperty.put("key1", "value1");
    mapProperty.put("key2", "value2");

    doc.field("prop5", mapProperty);
    doc.save();
    db.commit();

    Assert.assertEquals(propFiveIndexKey.getInternal().size(db), 2);
    try (var stream = propFiveIndexKey.getInternal().getRids(db, "key1")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
    try (var stream = propFiveIndexKey.getInternal().getRids(db, "key2")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }

    db.begin();
    doc = db.bindToSession(doc);
    Map<String, String> trackedMap = doc.field("prop5");
    trackedMap.put("key3", "value3");
    trackedMap.put("key4", "value4");
    trackedMap.remove("key1");
    trackedMap.put("key1", "value5");
    trackedMap.remove("key2");
    trackedMap.put("key6", "value6");
    trackedMap.put("key7", "value6");
    trackedMap.put("key8", "value6");
    trackedMap.put("key4", "value7");

    trackedMap.remove("key8");

    doc.save();
    db.commit();

    Assert.assertEquals(propFiveIndexKey.getInternal().size(db), 5);
    try (var stream = propFiveIndexKey.getInternal().getRids(db, "key1")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
    try (var stream = propFiveIndexKey.getInternal().getRids(db, "key3")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
    try (var stream = propFiveIndexKey.getInternal().getRids(db, "key4")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
    try (var stream = propFiveIndexKey.getInternal().getRids(db, "key6")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
    try (var stream = propFiveIndexKey.getInternal().getRids(db, "key7")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }

    Assert.assertEquals(propFiveIndexValue.getInternal().size(db), 4);
    try (var stream = propFiveIndexValue.getInternal().getRids(db, "value5")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
    try (var stream = propFiveIndexValue.getInternal().getRids(db, "value3")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
    try (var stream = propFiveIndexValue.getInternal().getRids(db, "value7")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
    try (var stream = propFiveIndexValue.getInternal().getRids(db, "value6")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
  }

  public void testSetUpdate() {
    checkEmbeddedDB();

    final var propSixIndex = db.getMetadata().getIndexManagerInternal()
        .getIndex(db, "classIndexManagerTestClass.prop6");

    Assert.assertEquals(propSixIndex.getInternal().size(db), 0);

    db.begin();
    var doc = ((EntityImpl) db.newEntity("classIndexManagerTestClass"));

    final Set<String> setProperty = new HashSet<>();
    setProperty.add("value1");
    setProperty.add("value2");

    doc.field("prop6", setProperty);
    doc.save();
    db.commit();

    Assert.assertEquals(propSixIndex.getInternal().size(db), 2);
    try (var stream = propSixIndex.getInternal().getRids(db, "value1")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
    try (var stream = propSixIndex.getInternal().getRids(db, "value2")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }

    db.begin();
    doc = db.bindToSession(doc);
    Set<String> trackedSet = doc.field("prop6");

    //noinspection OverwrittenKey
    trackedSet.add("value4");
    //noinspection OverwrittenKey
    trackedSet.add("value4");
    //noinspection OverwrittenKey
    trackedSet.add("value4");
    trackedSet.remove("value4");
    trackedSet.remove("value2");
    trackedSet.add("value5");

    doc.save();
    db.commit();

    Assert.assertEquals(propSixIndex.getInternal().size(db), 2);
    try (var stream = propSixIndex.getInternal().getRids(db, "value1")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
    try (var stream = propSixIndex.getInternal().getRids(db, "value5")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
  }

  public void testListDelete() {
    checkEmbeddedDB();

    final var propFourIndex = db.getMetadata().getIndexManagerInternal()
        .getIndex(db, "classIndexManagerTestClass.prop4");

    Assert.assertEquals(propFourIndex.getInternal().size(db), 0);

    db.begin();
    var doc = ((EntityImpl) db.newEntity("classIndexManagerTestClass"));

    final List<String> listProperty = new ArrayList<>();
    listProperty.add("value1");
    listProperty.add("value2");

    doc.field("prop4", listProperty);
    doc.save();
    db.commit();

    Assert.assertEquals(propFourIndex.getInternal().size(db), 2);
    try (var stream = propFourIndex.getInternal().getRids(db, "value1")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
    try (var stream = propFourIndex.getInternal().getRids(db, "value2")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }

    db.begin();
    doc = db.bindToSession(doc);
    List<String> trackedList = doc.field("prop4");
    trackedList.set(0, "value3");

    trackedList.add("value4");
    trackedList.add("value4");
    trackedList.add("value4");
    trackedList.remove("value4");
    trackedList.remove("value2");
    trackedList.add("value5");

    doc.save();
    db.commit();

    Assert.assertEquals(propFourIndex.getInternal().size(db), 3);
    try (var stream = propFourIndex.getInternal().getRids(db, "value3")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
    try (var stream = propFourIndex.getInternal().getRids(db, "value4")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
    try (var stream = propFourIndex.getInternal().getRids(db, "value5")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }

    db.begin();
    doc = db.bindToSession(doc);
    trackedList = doc.field("prop4");
    trackedList.remove("value3");
    trackedList.remove("value4");
    trackedList.add("value8");

    doc.delete();
    db.commit();

    Assert.assertEquals(propFourIndex.getInternal().size(db), 0);
  }

  public void testMapDelete() {
    checkEmbeddedDB();

    final var propFiveIndexKey = db.getMetadata().getIndexManagerInternal()
        .getIndex(db,
            "classIndexManagerTestIndexByKey");
    final var propFiveIndexValue = db.getMetadata().getIndexManagerInternal()
        .getIndex(db,
            "classIndexManagerTestIndexByValue");

    Assert.assertEquals(propFiveIndexKey.getInternal().size(db), 0);

    db.begin();
    var doc = ((EntityImpl) db.newEntity("classIndexManagerTestClass"));

    final Map<String, String> mapProperty = new HashMap<>();
    mapProperty.put("key1", "value1");
    mapProperty.put("key2", "value2");

    doc.field("prop5", mapProperty);
    doc.save();
    db.commit();

    Assert.assertEquals(propFiveIndexKey.getInternal().size(db), 2);
    try (var stream = propFiveIndexKey.getInternal().getRids(db, "key1")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
    try (var stream = propFiveIndexKey.getInternal().getRids(db, "key2")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }

    db.begin();
    doc = db.bindToSession(doc);
    Map<String, String> trackedMap = doc.field("prop5");
    trackedMap.put("key3", "value3");
    trackedMap.put("key4", "value4");
    trackedMap.remove("key1");
    trackedMap.put("key1", "value5");
    trackedMap.remove("key2");
    trackedMap.put("key6", "value6");
    trackedMap.put("key7", "value6");
    trackedMap.put("key8", "value6");
    trackedMap.put("key4", "value7");

    trackedMap.remove("key8");

    doc.save();
    db.commit();

    Assert.assertEquals(propFiveIndexKey.getInternal().size(db), 5);
    try (var stream = propFiveIndexKey.getInternal().getRids(db, "key1")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
    try (var stream = propFiveIndexKey.getInternal().getRids(db, "key3")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
    try (var stream = propFiveIndexKey.getInternal().getRids(db, "key4")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
    try (var stream = propFiveIndexKey.getInternal().getRids(db, "key6")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
    try (var stream = propFiveIndexKey.getInternal().getRids(db, "key7")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }

    Assert.assertEquals(propFiveIndexValue.getInternal().size(db), 4);
    try (var stream = propFiveIndexValue.getInternal().getRids(db, "value5")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
    try (var stream = propFiveIndexValue.getInternal().getRids(db, "value3")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
    try (var stream = propFiveIndexValue.getInternal().getRids(db, "value7")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
    try (var stream = propFiveIndexValue.getInternal().getRids(db, "value6")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }

    db.begin();
    doc = db.bindToSession(doc);
    trackedMap = doc.field("prop5");

    trackedMap.remove("key1");
    trackedMap.remove("key3");
    trackedMap.remove("key4");
    trackedMap.put("key6", "value10");
    trackedMap.put("key11", "value11");

    doc.delete();
    db.commit();

    Assert.assertEquals(propFiveIndexKey.getInternal().size(db), 0);
    Assert.assertEquals(propFiveIndexValue.getInternal().size(db), 0);
  }

  public void testSetDelete() {
    checkEmbeddedDB();
    final var propSixIndex = db.getMetadata().getIndexManagerInternal()
        .getIndex(db, "classIndexManagerTestClass.prop6");

    Assert.assertEquals(propSixIndex.getInternal().size(db), 0);

    db.begin();
    var doc = ((EntityImpl) db.newEntity("classIndexManagerTestClass"));

    final Set<String> setProperty = new HashSet<>();
    setProperty.add("value1");
    setProperty.add("value2");

    doc.field("prop6", setProperty);
    doc.save();
    db.commit();

    Assert.assertEquals(propSixIndex.getInternal().size(db), 2);
    try (var stream = propSixIndex.getInternal().getRids(db, "value1")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
    try (var stream = propSixIndex.getInternal().getRids(db, "value2")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }

    db.begin();
    doc = db.bindToSession(doc);
    Set<String> trackedSet = doc.field("prop6");

    //noinspection OverwrittenKey
    trackedSet.add("value4");
    //noinspection OverwrittenKey
    trackedSet.add("value4");
    //noinspection OverwrittenKey
    trackedSet.add("value4");
    trackedSet.remove("value4");
    trackedSet.remove("value2");
    trackedSet.add("value5");

    doc.save();
    db.commit();

    Assert.assertEquals(propSixIndex.getInternal().size(db), 2);
    try (var stream = propSixIndex.getInternal().getRids(db, "value1")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }
    try (var stream = propSixIndex.getInternal().getRids(db, "value5")) {
      Assert.assertTrue(stream.findAny().isPresent());
    }

    db.begin();
    doc = db.bindToSession(doc);
    trackedSet = doc.field("prop6");
    trackedSet.remove("value1");
    trackedSet.add("value6");

    doc.delete();
    db.commit();

    Assert.assertEquals(propSixIndex.getInternal().size(db), 0);
  }

  public void testDeleteDocumentIndexRecordDeleted() {
    checkEmbeddedDB();

    final var doc = ((EntityImpl) db.newEntity("classIndexManagerTestClass"));
    doc.field("prop0", "x");
    doc.field("prop1", "a");
    doc.field("prop2", 1);

    db.begin();
    doc.save();
    db.commit();

    final var propZeroIndex = db.getMetadata().getIndexManagerInternal().getIndex(db,
        "classIndexManagerTestSuperClass.prop0");
    final var propOneIndex = db.getMetadata().getIndexManagerInternal()
        .getIndex(db, "classIndexManagerTestClass.prop1");
    final var compositeIndex = db.getMetadata().getIndexManagerInternal()
        .getIndex(db, "classIndexManagerComposite");

    Assert.assertEquals(propZeroIndex.getInternal().size(db), 1);
    Assert.assertEquals(propOneIndex.getInternal().size(db), 1);
    Assert.assertEquals(compositeIndex.getInternal().size(db), 1);

    db.begin();
    db.bindToSession(doc).delete();
    db.commit();

    Assert.assertEquals(propZeroIndex.getInternal().size(db), 0);
    Assert.assertEquals(propOneIndex.getInternal().size(db), 0);
    Assert.assertEquals(compositeIndex.getInternal().size(db), 0);
  }

  public void testDeleteUpdatedDocumentIndexRecordDeleted() {
    checkEmbeddedDB();

    db.begin();
    var doc = ((EntityImpl) db.newEntity("classIndexManagerTestClass"));
    doc.field("prop0", "x");
    doc.field("prop1", "a");
    doc.field("prop2", 1);

    doc.save();
    db.commit();

    final var propOneIndex = db.getMetadata().getIndexManagerInternal()
        .getIndex(db, "classIndexManagerTestClass.prop1");
    final var compositeIndex = db.getMetadata().getIndexManagerInternal()
        .getIndex(db, "classIndexManagerComposite");

    final var propZeroIndex = db.getMetadata().getIndexManagerInternal().getIndex(db,
        "classIndexManagerTestSuperClass.prop0");
    Assert.assertEquals(propZeroIndex.getInternal().size(db), 1);
    Assert.assertEquals(propOneIndex.getInternal().size(db), 1);
    Assert.assertEquals(compositeIndex.getInternal().size(db), 1);

    db.begin();
    doc = db.bindToSession(doc);
    doc.field("prop2", 2);
    doc.field("prop0", "y");

    doc.delete();
    db.commit();

    Assert.assertEquals(propZeroIndex.getInternal().size(db), 0);
    Assert.assertEquals(propOneIndex.getInternal().size(db), 0);
    Assert.assertEquals(compositeIndex.getInternal().size(db), 0);
  }

  public void testDeleteUpdatedDocumentNullFieldIndexRecordDeleted() {
    checkEmbeddedDB();

    db.begin();
    final var doc = ((EntityImpl) db.newEntity("classIndexManagerTestClass"));
    doc.field("prop1", "a");
    doc.field("prop2", (Object) null);

    doc.save();
    db.commit();

    final var propOneIndex = db.getMetadata().getIndexManagerInternal()
        .getIndex(db, "classIndexManagerTestClass.prop1");
    final var compositeIndex = db.getMetadata().getIndexManagerInternal()
        .getIndex(db, "classIndexManagerComposite");

    Assert.assertEquals(propOneIndex.getInternal().size(db), 1);
    Assert.assertEquals(compositeIndex.getInternal().size(db), 0);

    db.begin();
    db.bindToSession(doc).delete();
    db.commit();

    Assert.assertEquals(propOneIndex.getInternal().size(db), 0);
    Assert.assertEquals(compositeIndex.getInternal().size(db), 0);
  }

  public void testDeleteUpdatedDocumentOrigNullFieldIndexRecordDeleted() {
    checkEmbeddedDB();

    db.begin();
    var doc = ((EntityImpl) db.newEntity("classIndexManagerTestClass"));
    doc.field("prop1", "a");
    doc.field("prop2", (Object) null);

    doc.save();
    db.commit();

    final var propOneIndex = db.getMetadata().getIndexManagerInternal()
        .getIndex(db, "classIndexManagerTestClass.prop1");
    final var compositeIndex = db.getMetadata().getIndexManagerInternal()
        .getIndex(db, "classIndexManagerComposite");

    Assert.assertEquals(propOneIndex.getInternal().size(db), 1);
    Assert.assertEquals(compositeIndex.getInternal().size(db), 0);

    db.begin();
    doc = db.bindToSession(doc);
    doc.field("prop2", 2);

    doc.delete();
    db.commit();

    Assert.assertEquals(propOneIndex.getInternal().size(db), 0);
    Assert.assertEquals(compositeIndex.getInternal().size(db), 0);
  }

  public void testNoClassIndexesUpdate() {
    checkEmbeddedDB();

    db.begin();
    var doc = ((EntityImpl) db.newEntity("classIndexManagerTestClassTwo"));
    doc.field("prop1", "a");

    doc.save();
    db.commit();
    db.begin();
    doc = db.bindToSession(doc);
    doc.field("prop1", "b");

    doc.save();
    db.commit();

    final Schema schema = db.getMetadata().getSchema();
    final var oClass = (SchemaClassInternal) schema.getClass(
        "classIndexManagerTestClass");

    final Collection<Index> indexes = oClass.getIndexesInternal(db);
    for (final var index : indexes) {
      Assert.assertEquals(index.getInternal().size(db), 0);
    }
  }

  public void testNoClassIndexesDelete() {
    db.begin();
    var doc = ((EntityImpl) db.newEntity("classIndexManagerTestClassTwo"));
    doc.field("prop1", "a");

    doc.save();
    db.commit();

    db.begin();
    db.bindToSession(doc).delete();
    db.commit();
  }

  public void testCollectionCompositeCreation() {
    checkEmbeddedDB();

    final var doc = ((EntityImpl) db.newEntity(
        "classIndexManagerTestCompositeCollectionClass"));

    doc.field("prop1", "test1");
    doc.field("prop2", Arrays.asList(1, 2));

    db.begin();
    doc.save();
    db.commit();

    final var index =
        db
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(db, "classIndexManagerTestIndexValueAndCollection");
    Assert.assertEquals(index.getInternal().size(db), 2);

    try (var stream = index.getInternal()
        .getRids(db, new CompositeKey("test1", 1))) {
      Assert.assertEquals(stream.findAny().orElse(null), doc.getIdentity());
    }
    try (var stream = index.getInternal()
        .getRids(db, new CompositeKey("test1", 2))) {
      Assert.assertEquals(stream.findAny().orElse(null), doc.getIdentity());
    }

    db.begin();
    db.bindToSession(doc).delete();
    db.commit();

    Assert.assertEquals(index.getInternal().size(db), 0);
  }

  public void testCollectionCompositeNullSimpleFieldCreation() {
    checkEmbeddedDB();

    db.begin();
    final var doc = ((EntityImpl) db.newEntity(
        "classIndexManagerTestCompositeCollectionClass"));

    doc.field("prop1", (Object) null);
    doc.field("prop2", Arrays.asList(1, 2));

    doc.save();
    db.commit();

    final var index =
        db
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(db, "classIndexManagerTestIndexValueAndCollection");
    Assert.assertEquals(index.getInternal().size(db), 0);

    db.begin();
    db.bindToSession(doc).delete();
    db.commit();
  }

  public void testCollectionCompositeNullCollectionFieldCreation() {
    checkEmbeddedDB();

    final var doc = ((EntityImpl) db.newEntity(
        "classIndexManagerTestCompositeCollectionClass"));

    doc.field("prop1", "test1");
    doc.field("prop2", (Object) null);

    db.begin();
    doc.save();
    db.commit();

    final var index =
        db
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(db, "classIndexManagerTestIndexValueAndCollection");
    Assert.assertEquals(index.getInternal().size(db), 0);

    db.begin();
    db.bindToSession(doc).delete();
    db.commit();
  }

  public void testCollectionCompositeUpdateSimpleField() {
    checkEmbeddedDB();

    db.begin();
    var doc = ((EntityImpl) db.newEntity("classIndexManagerTestCompositeCollectionClass"));

    doc.field("prop1", "test1");
    doc.field("prop2", Arrays.asList(1, 2));

    doc.save();
    db.commit();

    db.begin();
    final var index =
        db
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(db, "classIndexManagerTestIndexValueAndCollection");
    Assert.assertEquals(index.getInternal().size(db), 2);

    doc = db.bindToSession(doc);
    doc.field("prop1", "test2");

    doc.save();
    db.commit();

    try (var stream = index.getInternal()
        .getRids(db, new CompositeKey("test2", 1))) {
      Assert.assertEquals(stream.findAny().orElse(null), doc.getIdentity());
    }
    try (var stream = index.getInternal()
        .getRids(db, new CompositeKey("test2", 2))) {
      Assert.assertEquals(stream.findAny().orElse(null), doc.getIdentity());
    }

    Assert.assertEquals(index.getInternal().size(db), 2);

    db.begin();
    db.bindToSession(doc).delete();
    db.commit();

    Assert.assertEquals(index.getInternal().size(db), 0);
  }

  public void testCollectionCompositeUpdateCollectionWasAssigned() {
    checkEmbeddedDB();

    db.begin();
    var doc = ((EntityImpl) db.newEntity("classIndexManagerTestCompositeCollectionClass"));

    doc.field("prop1", "test1");
    doc.field("prop2", Arrays.asList(1, 2));

    doc.save();
    db.commit();

    db.begin();
    final var index =
        db
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(db, "classIndexManagerTestIndexValueAndCollection");
    Assert.assertEquals(index.getInternal().size(db), 2);

    doc = db.bindToSession(doc);
    doc.field("prop2", Arrays.asList(1, 3));

    doc.save();
    db.commit();

    try (var stream = index.getInternal()
        .getRids(db, new CompositeKey("test1", 1))) {
      Assert.assertEquals(stream.findAny().orElse(null), doc.getIdentity());
    }
    try (var stream = index.getInternal()
        .getRids(db, new CompositeKey("test1", 3))) {
      Assert.assertEquals(stream.findAny().orElse(null), doc.getIdentity());
    }

    Assert.assertEquals(index.getInternal().size(db), 2);

    db.begin();
    db.bindToSession(doc).delete();
    db.commit();

    Assert.assertEquals(index.getInternal().size(db), 0);
  }

  public void testCollectionCompositeUpdateCollectionWasChanged() {
    checkEmbeddedDB();

    db.begin();
    var doc = ((EntityImpl) db.newEntity("classIndexManagerTestCompositeCollectionClass"));

    doc.field("prop1", "test1");
    doc.field("prop2", Arrays.asList(1, 2));

    doc.save();
    db.commit();

    db.begin();
    final var index =
        db
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(db, "classIndexManagerTestIndexValueAndCollection");
    Assert.assertEquals(index.getInternal().size(db), 2);

    doc = db.bindToSession(doc);
    List<Integer> docList = doc.field("prop2");
    docList.add(3);
    docList.add(4);
    docList.add(5);

    docList.remove(0);

    doc.save();
    db.commit();

    try (var stream = index.getInternal()
        .getRids(db, new CompositeKey("test1", 2))) {
      Assert.assertEquals(stream.findAny().orElse(null), doc.getIdentity());
    }
    try (var stream = index.getInternal()
        .getRids(db, new CompositeKey("test1", 3))) {
      Assert.assertEquals(stream.findAny().orElse(null), doc.getIdentity());
    }
    try (var stream = index.getInternal()
        .getRids(db, new CompositeKey("test1", 4))) {
      Assert.assertEquals(stream.findAny().orElse(null), doc.getIdentity());
    }
    try (var stream = index.getInternal()
        .getRids(db, new CompositeKey("test1", 5))) {
      Assert.assertEquals(stream.findAny().orElse(null), doc.getIdentity());
    }

    Assert.assertEquals(index.getInternal().size(db), 4);

    db.begin();
    db.bindToSession(doc).delete();
    db.commit();

    Assert.assertEquals(index.getInternal().size(db), 0);
  }

  public void testCollectionCompositeUpdateCollectionWasChangedSimpleFieldWasAssigned() {
    checkEmbeddedDB();

    db.begin();
    var doc = ((EntityImpl) db.newEntity("classIndexManagerTestCompositeCollectionClass"));

    doc.field("prop1", "test1");
    doc.field("prop2", Arrays.asList(1, 2));

    doc.save();
    db.commit();

    db.begin();
    final var index =
        db
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(db, "classIndexManagerTestIndexValueAndCollection");
    Assert.assertEquals(index.getInternal().size(db), 2);

    doc = db.bindToSession(doc);
    List<Integer> docList = doc.field("prop2");
    docList.add(3);
    docList.add(4);
    docList.add(5);

    docList.remove(0);

    doc.field("prop1", "test2");

    doc.save();
    db.commit();

    Assert.assertEquals(index.getInternal().size(db), 4);

    try (var stream = index.getInternal()
        .getRids(db, new CompositeKey("test2", 2))) {
      Assert.assertEquals(stream.findAny().orElse(null), doc.getIdentity());
    }
    try (var stream = index.getInternal()
        .getRids(db, new CompositeKey("test2", 3))) {
      Assert.assertEquals(stream.findAny().orElse(null), doc.getIdentity());
    }
    try (var stream = index.getInternal()
        .getRids(db, new CompositeKey("test2", 4))) {
      Assert.assertEquals(stream.findAny().orElse(null), doc.getIdentity());
    }
    try (var stream = index.getInternal()
        .getRids(db, new CompositeKey("test2", 5))) {
      Assert.assertEquals(stream.findAny().orElse(null), doc.getIdentity());
    }

    db.begin();
    db.bindToSession(doc).delete();
    db.commit();

    Assert.assertEquals(index.getInternal().size(db), 0);
  }

  public void testCollectionCompositeUpdateSimpleFieldNull() {
    checkEmbeddedDB();

    db.begin();
    var doc = ((EntityImpl) db.newEntity("classIndexManagerTestCompositeCollectionClass"));

    doc.field("prop1", "test1");
    doc.field("prop2", Arrays.asList(1, 2));

    doc.save();
    db.commit();

    db.begin();
    final var index =
        db
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(db, "classIndexManagerTestIndexValueAndCollection");
    Assert.assertEquals(index.getInternal().size(db), 2);

    doc = db.bindToSession(doc);
    doc.field("prop1", (Object) null);

    doc.save();
    db.commit();

    Assert.assertEquals(index.getInternal().size(db), 0);

    db.begin();
    db.bindToSession(doc).delete();
    db.commit();

    Assert.assertEquals(index.getInternal().size(db), 0);
  }

  public void testCollectionCompositeUpdateCollectionWasAssignedNull() {
    checkEmbeddedDB();

    db.begin();
    var doc = ((EntityImpl) db.newEntity("classIndexManagerTestCompositeCollectionClass"));

    doc.field("prop1", "test1");
    doc.field("prop2", Arrays.asList(1, 2));

    doc.save();
    db.commit();

    db.begin();
    final var index =
        db
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(db, "classIndexManagerTestIndexValueAndCollection");
    Assert.assertEquals(index.getInternal().size(db), 2);

    doc = db.bindToSession(doc);
    doc.field("prop2", (Object) null);

    doc.save();
    db.commit();

    Assert.assertEquals(index.getInternal().size(db), 0);

    db.begin();
    db.bindToSession(doc).delete();
    db.commit();

    Assert.assertEquals(index.getInternal().size(db), 0);
  }

  public void testCollectionCompositeUpdateBothAssignedNull() {
    checkEmbeddedDB();

    db.begin();
    var doc = ((EntityImpl) db.newEntity("classIndexManagerTestCompositeCollectionClass"));

    doc.field("prop1", "test1");
    doc.field("prop2", Arrays.asList(1, 2));

    doc.save();
    db.commit();

    db.begin();
    doc = db.bindToSession(doc);
    final var index =
        db
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(db, "classIndexManagerTestIndexValueAndCollection");
    Assert.assertEquals(index.getInternal().size(db), 2);

    doc.field("prop2", (Object) null);
    doc.field("prop1", (Object) null);

    doc.save();
    db.commit();

    Assert.assertEquals(index.getInternal().size(db), 0);

    db.begin();
    db.bindToSession(doc).delete();
    db.commit();

    Assert.assertEquals(index.getInternal().size(db), 0);
  }

  public void testCollectionCompositeUpdateCollectionWasChangedSimpleFieldWasAssignedNull() {
    checkEmbeddedDB();

    db.begin();
    var doc = ((EntityImpl) db.newEntity("classIndexManagerTestCompositeCollectionClass"));

    doc.field("prop1", "test1");
    doc.field("prop2", Arrays.asList(1, 2));

    doc.save();
    db.commit();

    db.begin();
    final var index =
        db
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(db, "classIndexManagerTestIndexValueAndCollection");
    Assert.assertEquals(index.getInternal().size(db), 2);

    doc = db.bindToSession(doc);
    List<Integer> docList = doc.field("prop2");
    docList.add(3);
    docList.add(4);
    docList.add(5);

    docList.remove(0);

    doc.field("prop1", (Object) null);

    doc.save();
    db.commit();

    Assert.assertEquals(index.getInternal().size(db), 0);

    db.begin();
    db.bindToSession(doc).delete();
    db.commit();

    Assert.assertEquals(index.getInternal().size(db), 0);
  }

  public void testCollectionCompositeDeleteSimpleFieldAssigend() {
    checkEmbeddedDB();

    db.begin();
    var doc = ((EntityImpl) db.newEntity("classIndexManagerTestCompositeCollectionClass"));

    doc.field("prop1", "test1");
    doc.field("prop2", Arrays.asList(1, 2));

    doc.save();
    db.commit();

    db.begin();
    final var index =
        db
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(db, "classIndexManagerTestIndexValueAndCollection");
    Assert.assertEquals(index.getInternal().size(db), 2);

    doc = db.bindToSession(doc);
    doc.field("prop1", "test2");

    doc.delete();
    db.commit();

    Assert.assertEquals(index.getInternal().size(db), 0);
  }

  public void testCollectionCompositeDeleteCollectionFieldAssigend() {
    checkEmbeddedDB();

    db.begin();
    var doc = ((EntityImpl) db.newEntity("classIndexManagerTestCompositeCollectionClass"));

    doc.field("prop1", "test1");
    doc.field("prop2", Arrays.asList(1, 2));

    doc.save();
    db.commit();

    db.begin();
    final var index =
        db
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(db, "classIndexManagerTestIndexValueAndCollection");
    Assert.assertEquals(index.getInternal().size(db), 2);

    doc = db.bindToSession(doc);
    doc.field("prop2", Arrays.asList(1, 3));

    doc.delete();
    db.commit();

    Assert.assertEquals(index.getInternal().size(db), 0);
  }

  public void testCollectionCompositeDeleteCollectionFieldChanged() {
    checkEmbeddedDB();

    db.begin();
    var doc = ((EntityImpl) db.newEntity("classIndexManagerTestCompositeCollectionClass"));

    doc.field("prop1", "test1");
    doc.field("prop2", Arrays.asList(1, 2));

    doc.save();
    db.commit();

    db.begin();
    final var index =
        db
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(db, "classIndexManagerTestIndexValueAndCollection");
    Assert.assertEquals(index.getInternal().size(db), 2);

    doc = db.bindToSession(doc);
    List<Integer> docList = doc.field("prop2");
    docList.add(3);
    docList.add(4);

    docList.remove(1);

    doc.delete();
    db.commit();

    Assert.assertEquals(index.getInternal().size(db), 0);
  }

  public void testCollectionCompositeDeleteBothCollectionSimpleFieldChanged() {
    checkEmbeddedDB();

    db.begin();
    var doc = ((EntityImpl) db.newEntity("classIndexManagerTestCompositeCollectionClass"));

    doc.field("prop1", "test1");
    doc.field("prop2", Arrays.asList(1, 2));

    doc.save();
    db.commit();

    db.begin();
    doc = db.bindToSession(doc);
    final var index =
        db
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(db, "classIndexManagerTestIndexValueAndCollection");
    Assert.assertEquals(index.getInternal().size(db), 2);

    List<Integer> docList = doc.field("prop2");
    docList.add(3);
    docList.add(4);

    docList.remove(1);

    doc.field("prop1", "test2");

    doc.delete();
    db.commit();

    Assert.assertEquals(index.getInternal().size(db), 0);
  }

  public void testCollectionCompositeDeleteBothCollectionSimpleFieldAssigend() {
    checkEmbeddedDB();

    db.begin();
    var doc = ((EntityImpl) db.newEntity("classIndexManagerTestCompositeCollectionClass"));

    doc.field("prop1", "test1");
    doc.field("prop2", Arrays.asList(1, 2));

    doc.save();
    db.commit();

    db.begin();
    final var index =
        db
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(db, "classIndexManagerTestIndexValueAndCollection");
    Assert.assertEquals(index.getInternal().size(db), 2);

    doc = db.bindToSession(doc);
    doc.field("prop2", Arrays.asList(1, 3));
    doc.field("prop1", "test2");

    doc.delete();
    db.commit();

    Assert.assertEquals(index.getInternal().size(db), 0);
  }

  public void testCollectionCompositeDeleteSimpleFieldNull() {
    checkEmbeddedDB();

    db.begin();
    var doc = ((EntityImpl) db.newEntity("classIndexManagerTestCompositeCollectionClass"));

    doc.field("prop1", "test1");
    doc.field("prop2", Arrays.asList(1, 2));

    doc.save();
    db.commit();

    db.begin();
    doc = db.bindToSession(doc);
    final var index =
        db
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(db, "classIndexManagerTestIndexValueAndCollection");
    Assert.assertEquals(index.getInternal().size(db), 2);

    doc.field("prop1", (Object) null);

    doc.delete();
    db.commit();

    Assert.assertEquals(index.getInternal().size(db), 0);
  }

  public void testCollectionCompositeDeleteCollectionFieldNull() {
    checkEmbeddedDB();

    db.begin();
    var doc = ((EntityImpl) db.newEntity("classIndexManagerTestCompositeCollectionClass"));

    doc.field("prop1", "test1");
    doc.field("prop2", Arrays.asList(1, 2));

    doc.save();
    db.commit();

    db.begin();
    final var index =
        db
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(db, "classIndexManagerTestIndexValueAndCollection");
    Assert.assertEquals(index.getInternal().size(db), 2);

    doc = db.bindToSession(doc);
    doc.field("prop2", (Object) null);

    doc.delete();
    db.commit();

    Assert.assertEquals(index.getInternal().size(db), 0);
  }

  public void testCollectionCompositeDeleteBothSimpleCollectionFieldNull() {
    checkEmbeddedDB();

    db.begin();
    var doc = ((EntityImpl) db.newEntity("classIndexManagerTestCompositeCollectionClass"));

    doc.field("prop1", "test1");
    doc.field("prop2", Arrays.asList(1, 2));

    doc.save();
    db.commit();

    db.begin();
    doc = db.bindToSession(doc);
    final var index =
        db
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(db, "classIndexManagerTestIndexValueAndCollection");
    Assert.assertEquals(index.getInternal().size(db), 2);

    doc.field("prop2", (Object) null);
    doc.field("prop1", (Object) null);

    doc.delete();
    db.commit();

    Assert.assertEquals(index.getInternal().size(db), 0);
  }

  public void testCollectionCompositeDeleteCollectionFieldChangedSimpleFieldNull() {
    checkEmbeddedDB();

    db.begin();
    var doc = ((EntityImpl) db.newEntity("classIndexManagerTestCompositeCollectionClass"));

    doc.field("prop1", "test1");
    doc.field("prop2", Arrays.asList(1, 2));

    doc.save();
    db.commit();

    db.begin();
    doc = db.bindToSession(doc);
    final var index =
        db
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(db, "classIndexManagerTestIndexValueAndCollection");
    Assert.assertEquals(index.getInternal().size(db), 2);

    List<Integer> docList = doc.field("prop2");
    docList.add(3);
    docList.add(4);

    docList.remove(1);

    doc.field("prop1", (Object) null);

    doc.delete();
    db.commit();

    Assert.assertEquals(index.getInternal().size(db), 0);
  }

  public void testIndexOnPropertiesFromClassAndSuperclass() {
    checkEmbeddedDB();

    final var docOne = ((EntityImpl) db.newEntity("classIndexManagerTestClass"));
    docOne.field("prop0", "doc1-prop0");
    docOne.field("prop1", "doc1-prop1");

    db.begin();
    docOne.save();
    db.commit();

    final var docTwo = ((EntityImpl) db.newEntity("classIndexManagerTestClass"));
    docTwo.field("prop0", "doc2-prop0");
    docTwo.field("prop1", "doc2-prop1");

    db.begin();
    docTwo.save();
    db.commit();

    final Schema schema = db.getMetadata().getSchema();
    final var index =
        db.getMetadata().getIndexManagerInternal()
            .getIndex(db, "classIndexManagerTestIndexOnPropertiesFromClassAndSuperclass");

    Assert.assertEquals(index.getInternal().size(db), 2);
  }
}
