package com.jetbrains.youtrack.db.internal.core.index;

import com.jetbrains.youtrack.db.api.exception.BaseException;
import com.jetbrains.youtrack.db.api.exception.DatabaseException;
import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.internal.DbTestBase;
import com.jetbrains.youtrack.db.internal.core.db.record.MultiValueChangeEvent;
import com.jetbrains.youtrack.db.internal.core.db.record.TrackedList;
import com.jetbrains.youtrack.db.internal.core.db.record.TrackedMap;
import com.jetbrains.youtrack.db.internal.core.db.record.TrackedSet;
import com.jetbrains.youtrack.db.internal.core.db.record.ridbag.RidBag;
import com.jetbrains.youtrack.db.internal.core.id.RecordId;
import com.jetbrains.youtrack.db.internal.core.record.RecordInternal;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("unchecked")
public class CompositeIndexDefinitionTest extends DbTestBase {

  private CompositeIndexDefinition compositeIndex;

  @Before
  public void beforeMethod() {
    compositeIndex = new CompositeIndexDefinition("testClass");

    compositeIndex.addIndex(new PropertyIndexDefinition("testClass", "fOne", PropertyType.INTEGER));
    compositeIndex.addIndex(new PropertyIndexDefinition("testClass", "fTwo", PropertyType.STRING));
  }

  @Test
  public void testGetFields() {
    final var fields = compositeIndex.getFields();

    Assert.assertEquals(2, fields.size());
    Assert.assertEquals("fOne", fields.get(0));
    Assert.assertEquals("fTwo", fields.get(1));
  }

  @Test
  public void testCreateValueSuccessful() {
    final var result = compositeIndex.createValue(db, Arrays.asList("12", "test"));

    Assert.assertEquals(result, new CompositeKey(Arrays.asList(12, "test")));
  }

  @Test
  public void testCreateMapValueSuccessful() {
    final var compositeIndexDefinition =
        new CompositeIndexDefinition("testCollectionClass");

    compositeIndexDefinition.addIndex(
        new PropertyIndexDefinition("testCollectionClass", "fOne", PropertyType.INTEGER));
    compositeIndexDefinition.addIndex(
        new PropertyMapIndexDefinition(
            "testCollectionClass", "fTwo", PropertyType.STRING,
            PropertyMapIndexDefinition.INDEX_BY.KEY));

    final Map<String, String> stringMap = new HashMap<String, String>();
    stringMap.put("key1", "val1");
    stringMap.put("key2", "val2");

    final var result = compositeIndexDefinition.createValue(db, 12, stringMap);

    final var collectionResult = (Collection<CompositeKey>) result;

    Assert.assertEquals(2, collectionResult.size());
    Assert.assertTrue(collectionResult.contains(new CompositeKey(12, "key1")));
    Assert.assertTrue(collectionResult.contains(new CompositeKey(12, "key2")));
  }

  @Test
  public void testCreateCollectionValueSuccessfulOne() {
    final var compositeIndexDefinition =
        new CompositeIndexDefinition("testCollectionClass");

    compositeIndexDefinition.addIndex(
        new PropertyIndexDefinition("testCollectionClass", "fOne", PropertyType.INTEGER));
    compositeIndexDefinition.addIndex(
        new PropertyListIndexDefinition("testCollectionClass", "fTwo", PropertyType.INTEGER));

    final var result = compositeIndexDefinition.createValue(db, 12, Arrays.asList(1, 2));

    final var expectedResult = new ArrayList<CompositeKey>();

    expectedResult.add(new CompositeKey(12, 1));
    expectedResult.add(new CompositeKey(12, 2));

    Assert.assertEquals(result, expectedResult);
  }

  @Test
  public void testCreateRidBagValueSuccessfulOne() {
    final var compositeIndexDefinition =
        new CompositeIndexDefinition("testCollectionClass");

    compositeIndexDefinition.addIndex(
        new PropertyIndexDefinition("testCollectionClass", "fOne", PropertyType.INTEGER));
    compositeIndexDefinition.addIndex(
        new PropertyRidBagIndexDefinition("testCollectionClass", "fTwo"));

    var ridBag = new RidBag(db);
    ridBag.add(new RecordId("#1:10"));
    ridBag.add(new RecordId("#1:11"));
    ridBag.add(new RecordId("#1:11"));

    final var result = compositeIndexDefinition.createValue(db, 12, ridBag);

    final var expectedResult = new ArrayList<CompositeKey>();

    expectedResult.add(new CompositeKey(12, new RecordId("#1:10")));
    expectedResult.add(new CompositeKey(12, new RecordId("#1:11")));
    expectedResult.add(new CompositeKey(12, new RecordId("#1:11")));

    Assert.assertEquals(result, expectedResult);
  }

  @Test
  public void testCreateCollectionValueSuccessfulTwo() {
    final var compositeIndexDefinition =
        new CompositeIndexDefinition("testCollectionClass");

    compositeIndexDefinition.addIndex(
        new PropertyListIndexDefinition("testCollectionClass", "fTwo", PropertyType.INTEGER));
    compositeIndexDefinition.addIndex(
        new PropertyIndexDefinition("testCollectionClass", "fOne", PropertyType.INTEGER));

    final var result =
        compositeIndexDefinition.createValue(db, Arrays.asList(Arrays.asList(1, 2), 12));

    final var expectedResult = new ArrayList<CompositeKey>();

    expectedResult.add(new CompositeKey(1, 12));
    expectedResult.add(new CompositeKey(2, 12));

    Assert.assertEquals(result, expectedResult);
  }

  @Test
  public void testCreateCollectionValueEmptyListOne() {
    final var compositeIndexDefinition =
        new CompositeIndexDefinition("testCollectionClass");

    compositeIndexDefinition.addIndex(
        new PropertyListIndexDefinition("testCollectionClass", "fTwo", PropertyType.INTEGER));
    compositeIndexDefinition.addIndex(
        new PropertyIndexDefinition("testCollectionClass", "fOne", PropertyType.INTEGER));

    final var result = compositeIndexDefinition.createValue(db, Collections.emptyList(), 12);
    Assert.assertNull(result);
  }

  @Test
  public void testCreateCollectionValueEmptyListTwo() {
    final var compositeIndexDefinition =
        new CompositeIndexDefinition("testCollectionClass");

    compositeIndexDefinition.addIndex(
        new PropertyIndexDefinition("testCollectionClass", "fOne", PropertyType.INTEGER));
    compositeIndexDefinition.addIndex(
        new PropertyListIndexDefinition("testCollectionClass", "fTwo", PropertyType.INTEGER));

    final var result = compositeIndexDefinition.createValue(db, 12, Collections.emptyList());
    Assert.assertNull(result);
  }

  @Test
  public void testCreateCollectionValueEmptyListOneNullSupport() {
    final var compositeIndexDefinition =
        new CompositeIndexDefinition("testCollectionClass");
    compositeIndexDefinition.setNullValuesIgnored(false);

    compositeIndexDefinition.addIndex(
        new PropertyListIndexDefinition("testCollectionClass", "fTwo", PropertyType.INTEGER));
    compositeIndexDefinition.addIndex(
        new PropertyIndexDefinition("testCollectionClass", "fOne", PropertyType.INTEGER));

    final var result = compositeIndexDefinition.createValue(db, Collections.emptyList(), 12);
    Assert.assertEquals(result, List.of(new CompositeKey(null, 12)));
  }

  @Test
  public void testCreateCollectionValueEmptyListTwoNullSupport() {
    final var compositeIndexDefinition =
        new CompositeIndexDefinition("testCollectionClass");
    compositeIndexDefinition.setNullValuesIgnored(false);

    compositeIndexDefinition.addIndex(
        new PropertyIndexDefinition("testCollectionClass", "fOne", PropertyType.INTEGER));
    compositeIndexDefinition.addIndex(
        new PropertyListIndexDefinition("testCollectionClass", "fTwo", PropertyType.INTEGER));

    final var result = compositeIndexDefinition.createValue(db, 12, Collections.emptyList());
    Assert.assertEquals(result, List.of(new CompositeKey(12, null)));
  }

  @Test
  public void testCreateRidBagValueSuccessfulTwo() {
    final var compositeIndexDefinition =
        new CompositeIndexDefinition("testCollectionClass");

    compositeIndexDefinition.addIndex(
        new PropertyRidBagIndexDefinition("testCollectionClass", "fTwo"));
    compositeIndexDefinition.addIndex(
        new PropertyIndexDefinition("testCollectionClass", "fOne", PropertyType.INTEGER));

    var ridBag = new RidBag(db);
    ridBag.add(new RecordId("#1:10"));
    ridBag.add(new RecordId("#1:11"));
    ridBag.add(new RecordId("#1:11"));

    final var result = compositeIndexDefinition.createValue(db, Arrays.asList(ridBag, 12));

    final var expectedResult = new ArrayList<CompositeKey>();

    expectedResult.add(new CompositeKey(new RecordId("#1:10"), 12));
    expectedResult.add(new CompositeKey(new RecordId("#1:11"), 12));
    expectedResult.add(new CompositeKey(new RecordId("#1:11"), 12));

    Assert.assertEquals(result, expectedResult);
  }

  @Test
  public void testCreateCollectionValueSuccessfulThree() {
    final var compositeIndexDefinition =
        new CompositeIndexDefinition("testCollectionClass");

    compositeIndexDefinition.addIndex(
        new PropertyIndexDefinition("testCollectionClass", "fOne", PropertyType.INTEGER));
    compositeIndexDefinition.addIndex(
        new PropertyListIndexDefinition("testCollectionClass", "fTwo", PropertyType.INTEGER));
    compositeIndexDefinition.addIndex(
        new PropertyIndexDefinition("testCollectionClass", "fThree", PropertyType.STRING));

    final var result = compositeIndexDefinition.createValue(db, 12, Arrays.asList(1, 2), "test");

    final var expectedResult = new ArrayList<CompositeKey>();

    expectedResult.add(new CompositeKey(12, 1, "test"));
    expectedResult.add(new CompositeKey(12, 2, "test"));

    Assert.assertEquals(result, expectedResult);
  }

  @Test
  public void testCreateRidBagValueSuccessfulThree() {
    final var compositeIndexDefinition =
        new CompositeIndexDefinition("testCollectionClass");

    compositeIndexDefinition.addIndex(
        new PropertyIndexDefinition("testCollectionClass", "fOne", PropertyType.INTEGER));
    compositeIndexDefinition.addIndex(
        new PropertyRidBagIndexDefinition("testCollectionClass", "fTwo"));
    compositeIndexDefinition.addIndex(
        new PropertyIndexDefinition("testCollectionClass", "fThree", PropertyType.STRING));

    var ridBag = new RidBag(db);
    ridBag.add(new RecordId("#1:10"));
    ridBag.add(new RecordId("#1:11"));
    ridBag.add(new RecordId("#1:11"));

    final var result = compositeIndexDefinition.createValue(db, 12, ridBag, "test");

    final var expectedResult = new ArrayList<CompositeKey>();

    expectedResult.add(new CompositeKey(12, new RecordId("#1:10"), "test"));
    expectedResult.add(new CompositeKey(12, new RecordId("#1:11"), "test"));
    expectedResult.add(new CompositeKey(12, new RecordId("#1:11"), "test"));

    Assert.assertEquals(result, expectedResult);
  }

  @Test(expected = IndexException.class)
  public void testCreateCollectionValueTwoCollections() {
    final var compositeIndexDefinition =
        new CompositeIndexDefinition("testCollectionClass");

    compositeIndexDefinition.addIndex(
        new PropertyListIndexDefinition("testCollectionClass", "fTwo", PropertyType.INTEGER));
    compositeIndexDefinition.addIndex(
        new PropertyListIndexDefinition("testCollectionClass", "fOne", PropertyType.INTEGER));

    compositeIndexDefinition.createValue(db, Arrays.asList(1, 2), List.of(12));
  }

  @Test(expected = DatabaseException.class)
  public void testCreateValueWrongParam() {
    compositeIndex.createValue(db, Arrays.asList("1t2", "test"));
  }

  @Test
  public void testCreateValueSuccessfulArrayParams() {
    final var result = compositeIndex.createValue(db, "12", "test");

    Assert.assertEquals(result, new CompositeKey(Arrays.asList(12, "test")));
  }

  @Test(expected = DatabaseException.class)
  public void testCreateValueWrongParamArrayParams() {
    compositeIndex.createValue(db, "1t2", "test");
  }

  @Test
  public void testCreateValueDefinitionsMoreThanParams() {
    compositeIndex.addIndex(
        new PropertyIndexDefinition("testClass", "fThree", PropertyType.STRING));

    final var result = compositeIndex.createValue(db, "12", "test");
    Assert.assertEquals(result, new CompositeKey(Arrays.asList(12, "test")));
  }

  @Test
  public void testCreateValueIndexItemWithTwoParams() {
    final var anotherCompositeIndex =
        new CompositeIndexDefinition("testClass");

    anotherCompositeIndex.addIndex(
        new PropertyIndexDefinition("testClass", "f11", PropertyType.STRING));
    anotherCompositeIndex.addIndex(
        new PropertyIndexDefinition("testClass", "f22", PropertyType.STRING));

    compositeIndex.addIndex(anotherCompositeIndex);

    final var result = compositeIndex.createValue(db, "12", "test", "tset");
    Assert.assertEquals(result, new CompositeKey(Arrays.asList(12, "test", "tset")));
  }

  @Test
  public void testDocumentToIndexSuccessful() {
    final var document = (EntityImpl) db.newEntity();

    document.field("fOne", 12);
    document.field("fTwo", "test");

    final var result = compositeIndex.getDocumentValueToIndex(db, document);
    Assert.assertEquals(result, new CompositeKey(Arrays.asList(12, "test")));
  }

  @Test
  public void testDocumentToIndexMapValueSuccessful() {
    final var document = (EntityImpl) db.newEntity();

    final Map<String, String> stringMap = new HashMap<String, String>();
    stringMap.put("key1", "val1");
    stringMap.put("key2", "val2");

    document.field("fOne", 12);
    document.field("fTwo", stringMap);

    final var compositeIndexDefinition =
        new CompositeIndexDefinition("testCollectionClass");

    compositeIndexDefinition.addIndex(
        new PropertyIndexDefinition("testCollectionClass", "fOne", PropertyType.INTEGER));
    compositeIndexDefinition.addIndex(
        new PropertyMapIndexDefinition(
            "testCollectionClass", "fTwo", PropertyType.STRING,
            PropertyMapIndexDefinition.INDEX_BY.KEY));

    final var result = compositeIndexDefinition.getDocumentValueToIndex(db, document);
    final var collectionResult = (Collection<CompositeKey>) result;

    Assert.assertEquals(2, collectionResult.size());
    Assert.assertTrue(collectionResult.contains(new CompositeKey(12, "key1")));
    Assert.assertTrue(collectionResult.contains(new CompositeKey(12, "key2")));
  }

  @Test
  public void testDocumentToIndexCollectionValueSuccessfulOne() {
    final var document = (EntityImpl) db.newEntity();

    document.field("fOne", 12);
    document.field("fTwo", Arrays.asList(1, 2));

    final var compositeIndexDefinition =
        new CompositeIndexDefinition("testCollectionClass");

    compositeIndexDefinition.addIndex(
        new PropertyIndexDefinition("testCollectionClass", "fOne", PropertyType.INTEGER));
    compositeIndexDefinition.addIndex(
        new PropertyListIndexDefinition("testCollectionClass", "fTwo", PropertyType.INTEGER));

    final var result = compositeIndexDefinition.getDocumentValueToIndex(db, document);

    final var expectedResult = new ArrayList<CompositeKey>();

    expectedResult.add(new CompositeKey(12, 1));
    expectedResult.add(new CompositeKey(12, 2));

    Assert.assertEquals(result, expectedResult);
  }

  @Test
  public void testDocumentToIndexCollectionValueEmptyOne() {
    final var document = (EntityImpl) db.newEntity();

    document.field("fOne", 12);
    document.field("fTwo", Collections.emptyList());

    final var compositeIndexDefinition =
        new CompositeIndexDefinition("testCollectionClass");

    compositeIndexDefinition.addIndex(
        new PropertyIndexDefinition("testCollectionClass", "fOne", PropertyType.INTEGER));
    compositeIndexDefinition.addIndex(
        new PropertyListIndexDefinition("testCollectionClass", "fTwo", PropertyType.INTEGER));

    final var result = compositeIndexDefinition.getDocumentValueToIndex(db, document);
    Assert.assertNull(result);
  }

  @Test
  public void testDocumentToIndexCollectionValueEmptyTwo() {
    final var document = (EntityImpl) db.newEntity();

    document.field("fOne", Collections.emptyList());
    document.field("fTwo", 12);

    final var compositeIndexDefinition =
        new CompositeIndexDefinition("testCollectionClass");

    compositeIndexDefinition.addIndex(
        new PropertyListIndexDefinition("testCollectionClass", "fOne", PropertyType.INTEGER));
    compositeIndexDefinition.addIndex(
        new PropertyIndexDefinition("testCollectionClass", "fTwo", PropertyType.INTEGER));

    final var result = compositeIndexDefinition.getDocumentValueToIndex(db, document);
    Assert.assertNull(result);
  }

  @Test
  public void testDocumentToIndexCollectionValueEmptyOneNullValuesSupport() {
    final var document = (EntityImpl) db.newEntity();

    document.field("fOne", 12);
    document.field("fTwo", Collections.emptyList());

    final var compositeIndexDefinition =
        new CompositeIndexDefinition("testCollectionClass");

    compositeIndexDefinition.addIndex(
        new PropertyIndexDefinition("testCollectionClass", "fOne", PropertyType.INTEGER));
    compositeIndexDefinition.addIndex(
        new PropertyListIndexDefinition("testCollectionClass", "fTwo", PropertyType.INTEGER));
    compositeIndexDefinition.setNullValuesIgnored(false);

    final var result = compositeIndexDefinition.getDocumentValueToIndex(db, document);
    Assert.assertEquals(result, List.of(new CompositeKey(12, null)));
  }

  @Test
  public void testDocumentToIndexCollectionValueEmptyTwoNullValuesSupport() {
    final var document = (EntityImpl) db.newEntity();

    document.field("fOne", Collections.emptyList());
    document.field("fTwo", 12);

    final var compositeIndexDefinition =
        new CompositeIndexDefinition("testCollectionClass");

    compositeIndexDefinition.addIndex(
        new PropertyListIndexDefinition("testCollectionClass", "fOne", PropertyType.INTEGER));
    compositeIndexDefinition.addIndex(
        new PropertyIndexDefinition("testCollectionClass", "fTwo", PropertyType.INTEGER));
    compositeIndexDefinition.setNullValuesIgnored(false);

    final var result = compositeIndexDefinition.getDocumentValueToIndex(db, document);
    Assert.assertEquals(result, List.of(new CompositeKey(null, 12)));
  }

  @Test
  public void testDocumentToIndexRidBagValueSuccessfulOne() {
    final var document = (EntityImpl) db.newEntity();

    final var ridBag = new RidBag(db);
    ridBag.add(new RecordId("#1:10"));
    ridBag.add(new RecordId("#1:11"));
    ridBag.add(new RecordId("#1:11"));

    document.field("fOne", 12);
    document.field("fTwo", ridBag);

    final var compositeIndexDefinition =
        new CompositeIndexDefinition("testCollectionClass");

    compositeIndexDefinition.addIndex(
        new PropertyIndexDefinition("testCollectionClass", "fOne", PropertyType.INTEGER));
    compositeIndexDefinition.addIndex(
        new PropertyRidBagIndexDefinition("testCollectionClass", "fTwo"));

    final var result = compositeIndexDefinition.getDocumentValueToIndex(db, document);

    final var expectedResult = new ArrayList<CompositeKey>();

    expectedResult.add(new CompositeKey(12, new RecordId("#1:10")));
    expectedResult.add(new CompositeKey(12, new RecordId("#1:11")));
    expectedResult.add(new CompositeKey(12, new RecordId("#1:11")));

    Assert.assertEquals(result, expectedResult);
  }

  @Test
  public void testDocumentToIndexCollectionValueSuccessfulTwo() {
    final var document = (EntityImpl) db.newEntity();

    document.field("fOne", 12);
    document.field("fTwo", Arrays.asList(1, 2));

    final var compositeIndexDefinition =
        new CompositeIndexDefinition("testCollectionClass");

    compositeIndexDefinition.addIndex(
        new PropertyListIndexDefinition("testCollectionClass", "fTwo", PropertyType.INTEGER));
    compositeIndexDefinition.addIndex(
        new PropertyIndexDefinition("testCollectionClass", "fOne", PropertyType.INTEGER));

    final var result = compositeIndexDefinition.getDocumentValueToIndex(db, document);

    final var expectedResult = new ArrayList<CompositeKey>();

    expectedResult.add(new CompositeKey(1, 12));
    expectedResult.add(new CompositeKey(2, 12));

    Assert.assertEquals(result, expectedResult);
  }

  @Test
  public void testDocumentToIndexRidBagValueSuccessfulTwo() {
    final var ridBag = new RidBag(db);
    ridBag.add(new RecordId("#1:10"));
    ridBag.add(new RecordId("#1:11"));
    ridBag.add(new RecordId("#1:11"));

    final var document = (EntityImpl) db.newEntity();

    document.field("fOne", 12);
    document.field("fTwo", ridBag);

    final var compositeIndexDefinition =
        new CompositeIndexDefinition("testCollectionClass");

    compositeIndexDefinition.addIndex(
        new PropertyRidBagIndexDefinition("testCollectionClass", "fTwo"));
    compositeIndexDefinition.addIndex(
        new PropertyIndexDefinition("testCollectionClass", "fOne", PropertyType.INTEGER));

    final var result = compositeIndexDefinition.getDocumentValueToIndex(db, document);

    final var expectedResult = new ArrayList<CompositeKey>();

    expectedResult.add(new CompositeKey(new RecordId("#1:10"), 12));
    expectedResult.add(new CompositeKey(new RecordId("#1:11"), 12));
    expectedResult.add(new CompositeKey(new RecordId("#1:11"), 12));

    Assert.assertEquals(result, expectedResult);
  }

  @Test
  public void testDocumentToIndexCollectionValueSuccessfulThree() {
    final var document = (EntityImpl) db.newEntity();

    document.field("fOne", 12);
    document.field("fTwo", Arrays.asList(1, 2));
    document.field("fThree", "test");

    final var compositeIndexDefinition =
        new CompositeIndexDefinition("testCollectionClass");

    compositeIndexDefinition.addIndex(
        new PropertyIndexDefinition("testCollectionClass", "fOne", PropertyType.INTEGER));
    compositeIndexDefinition.addIndex(
        new PropertyListIndexDefinition("testCollectionClass", "fTwo", PropertyType.INTEGER));
    compositeIndexDefinition.addIndex(
        new PropertyIndexDefinition("testCollectionClass", "fThree", PropertyType.STRING));

    final var result = compositeIndexDefinition.getDocumentValueToIndex(db, document);

    final var expectedResult = new ArrayList<CompositeKey>();

    expectedResult.add(new CompositeKey(12, 1, "test"));
    expectedResult.add(new CompositeKey(12, 2, "test"));

    Assert.assertEquals(result, expectedResult);
  }

  @Test
  public void testDocumentToIndexRidBagValueSuccessfulThree() {
    final var document = (EntityImpl) db.newEntity();

    final var ridBag = new RidBag(db);
    ridBag.add(new RecordId("#1:10"));
    ridBag.add(new RecordId("#1:11"));
    ridBag.add(new RecordId("#1:11"));

    document.field("fOne", 12);
    document.field("fTwo", ridBag);
    document.field("fThree", "test");

    final var compositeIndexDefinition =
        new CompositeIndexDefinition("testCollectionClass");

    compositeIndexDefinition.addIndex(
        new PropertyIndexDefinition("testCollectionClass", "fOne", PropertyType.INTEGER));
    compositeIndexDefinition.addIndex(
        new PropertyRidBagIndexDefinition("testCollectionClass", "fTwo"));
    compositeIndexDefinition.addIndex(
        new PropertyIndexDefinition("testCollectionClass", "fThree", PropertyType.STRING));

    final var result = compositeIndexDefinition.getDocumentValueToIndex(db, document);

    final var expectedResult = new ArrayList<CompositeKey>();

    expectedResult.add(new CompositeKey(12, new RecordId("#1:10"), "test"));
    expectedResult.add(new CompositeKey(12, new RecordId("#1:11"), "test"));
    expectedResult.add(new CompositeKey(12, new RecordId("#1:11"), "test"));

    Assert.assertEquals(result, expectedResult);
  }

  @Test(expected = BaseException.class)
  public void testDocumentToIndexCollectionValueTwoCollections() {
    final var document = (EntityImpl) db.newEntity();

    document.field("fOne", List.of(12));
    document.field("fTwo", Arrays.asList(1, 2));

    final var compositeIndexDefinition =
        new CompositeIndexDefinition("testCollectionClass");

    compositeIndexDefinition.addIndex(
        new PropertyListIndexDefinition("testCollectionClass", "fOne", PropertyType.INTEGER));
    compositeIndexDefinition.addIndex(
        new PropertyListIndexDefinition("testCollectionClass", "fTwo", PropertyType.INTEGER));
    compositeIndexDefinition.getDocumentValueToIndex(db, document);
  }

  @Test(expected = DatabaseException.class)
  public void testDocumentToIndexWrongField() {
    final var document = (EntityImpl) db.newEntity();

    document.field("fOne", "1t2");
    document.field("fTwo", "test");

    compositeIndex.getDocumentValueToIndex(db, document);
  }

  @Test
  public void testGetParamCount() {
    final var result = compositeIndex.getParamCount();

    Assert.assertEquals(2, result);
  }

  @Test
  public void testGetTypes() {
    final var result = compositeIndex.getTypes();

    Assert.assertEquals(2, result.length);
    Assert.assertEquals(PropertyType.INTEGER, result[0]);
    Assert.assertEquals(PropertyType.STRING, result[1]);
  }

  @Test
  public void testEmptyIndexReload() {
    final var emptyCompositeIndex =
        new CompositeIndexDefinition("testClass");

    emptyCompositeIndex.addIndex(
        new PropertyIndexDefinition("testClass", "fOne", PropertyType.INTEGER));
    emptyCompositeIndex.addIndex(
        new PropertyIndexDefinition("testClass", "fTwo", PropertyType.STRING));

    db.begin();
    final var docToStore = emptyCompositeIndex.toStream(db, (EntityImpl) db.newEntity());
    db.save(docToStore);
    db.commit();

    final EntityImpl docToLoad = db.load(docToStore.getIdentity());

    final var result = new CompositeIndexDefinition();
    result.fromStream(docToLoad);

    Assert.assertEquals(result, emptyCompositeIndex);
  }

  @Test
  public void testIndexReload() {
    final var docToStore = compositeIndex.toStream(db, (EntityImpl) db.newEntity());

    final var result = new CompositeIndexDefinition();
    result.fromStream(docToStore);

    Assert.assertEquals(result, compositeIndex);
  }

  @Test
  public void testClassOnlyConstructor() {

    final var emptyCompositeIndex =
        new CompositeIndexDefinition(
            "testClass",
            Arrays.asList(
                new PropertyIndexDefinition("testClass", "fOne", PropertyType.INTEGER),
                new PropertyIndexDefinition("testClass", "fTwo", PropertyType.STRING)));

    final var emptyCompositeIndexTwo =
        new CompositeIndexDefinition("testClass");

    emptyCompositeIndexTwo.addIndex(
        new PropertyIndexDefinition("testClass", "fOne", PropertyType.INTEGER));
    emptyCompositeIndexTwo.addIndex(
        new PropertyIndexDefinition("testClass", "fTwo", PropertyType.STRING));

    Assert.assertEquals(emptyCompositeIndex, emptyCompositeIndexTwo);

    db.begin();
    final var entityToStore = emptyCompositeIndex.toStream(db, (EntityImpl) db.newEntity());
    db.save(entityToStore);
    db.commit();

    final EntityImpl docToLoad = db.load(entityToStore.getIdentity());

    final var result = new CompositeIndexDefinition();
    result.fromStream(docToLoad);

    Assert.assertEquals(result, emptyCompositeIndexTwo);
  }

  @Test
  public void testProcessChangeListEventsOne() {
    final var compositeIndexDefinition = new CompositeIndexDefinition();

    compositeIndexDefinition.addIndex(
        new PropertyIndexDefinition("testCollectionClass", "fOne", PropertyType.INTEGER));
    compositeIndexDefinition.addIndex(
        new PropertyListIndexDefinition("testCollectionClass", "fTwo", PropertyType.STRING));
    compositeIndexDefinition.addIndex(
        new PropertyIndexDefinition("testCollectionClass", "fThree", PropertyType.INTEGER));

    final var doc = (EntityImpl) db.newEntity();
    RecordInternal.unsetDirty(doc);
    Assert.assertFalse(doc.isDirty());

    final var trackedList = new TrackedList<String>(doc);
    trackedList.enableTracking(doc);
    trackedList.add("l1");
    trackedList.add("l2");
    trackedList.add("l3");
    trackedList.remove("l2");

    var keysToAdd = new Object2IntOpenHashMap<CompositeKey>();
    keysToAdd.defaultReturnValue(-1);

    var keysToRemove = new Object2IntOpenHashMap<CompositeKey>();
    keysToRemove.defaultReturnValue(-1);

    for (var multiValueChangeEvent :
        trackedList.getTimeLine().getMultiValueChangeEvents()) {
      compositeIndexDefinition.processChangeEvent(
          db, multiValueChangeEvent, keysToAdd, keysToRemove, 2, 3);
    }

    Assert.assertEquals(0, keysToRemove.size());
    Assert.assertEquals(2, keysToAdd.size());

    Assert.assertTrue(keysToAdd.containsKey(new CompositeKey(2, "l1", 3)));
    Assert.assertTrue(keysToAdd.containsKey(new CompositeKey(2, "l3", 3)));
  }

  @Test
  public void testProcessChangeRidBagEventsOne() {
    final var compositeIndexDefinition = new CompositeIndexDefinition();

    compositeIndexDefinition.addIndex(
        new PropertyIndexDefinition("testCollectionClass", "fOne", PropertyType.INTEGER));
    compositeIndexDefinition.addIndex(
        new PropertyRidBagIndexDefinition("testCollectionClass", "fTwo"));
    compositeIndexDefinition.addIndex(
        new PropertyIndexDefinition("testCollectionClass", "fThree", PropertyType.INTEGER));

    final var ridBag = new RidBag(db);
    ridBag.enableTracking(null);
    ridBag.add(new RecordId("#10:0"));
    ridBag.add(new RecordId("#10:1"));
    ridBag.add(new RecordId("#10:0"));
    ridBag.add(new RecordId("#10:2"));
    ridBag.remove(new RecordId("#10:0"));
    ridBag.remove(new RecordId("#10:1"));

    var keysToAdd = new Object2IntOpenHashMap<CompositeKey>();
    keysToAdd.defaultReturnValue(-1);

    var keysToRemove = new Object2IntOpenHashMap<CompositeKey>();
    keysToRemove.defaultReturnValue(-1);

    for (var multiValueChangeEvent :
        ridBag.getTimeLine().getMultiValueChangeEvents()) {
      compositeIndexDefinition.processChangeEvent(
          db, multiValueChangeEvent, keysToAdd, keysToRemove, 2, 3);
    }

    Assert.assertEquals(0, keysToRemove.size());
    Assert.assertEquals(2, keysToAdd.size());

    Assert.assertTrue(keysToAdd.containsKey(new CompositeKey(2, new RecordId("#10:0"), 3)));
    Assert.assertTrue(keysToAdd.containsKey(new CompositeKey(2, new RecordId("#10:2"), 3)));
  }

  @Test
  public void testProcessChangeListEventsTwo() {
    final var compositeIndexDefinition = new CompositeIndexDefinition();

    compositeIndexDefinition.addIndex(
        new PropertyIndexDefinition("testCollectionClass", "fOne", PropertyType.INTEGER));
    compositeIndexDefinition.addIndex(
        new PropertyListIndexDefinition("testCollectionClass", "fTwo", PropertyType.STRING));
    compositeIndexDefinition.addIndex(
        new PropertyIndexDefinition("testCollectionClass", "fThree", PropertyType.INTEGER));

    final var doc = (EntityImpl) db.newEntity();
    RecordInternal.unsetDirty(doc);
    Assert.assertFalse(doc.isDirty());

    final var trackedList = new TrackedList<String>(doc);

    trackedList.add("l1");
    trackedList.add("l2");
    trackedList.add("l3");
    trackedList.remove("l2");

    trackedList.enableTracking(doc);
    trackedList.add("l4");
    trackedList.remove("l1");

    var keysToAdd = new Object2IntOpenHashMap<CompositeKey>();
    keysToAdd.defaultReturnValue(-1);

    var keysToRemove = new Object2IntOpenHashMap<CompositeKey>();
    keysToRemove.defaultReturnValue(-1);

    for (var multiValueChangeEvent :
        trackedList.getTimeLine().getMultiValueChangeEvents()) {
      compositeIndexDefinition.processChangeEvent(
          db, multiValueChangeEvent, keysToAdd, keysToRemove, 2, 3);
    }

    Assert.assertEquals(1, keysToRemove.size());
    Assert.assertEquals(1, keysToAdd.size());

    Assert.assertTrue(keysToAdd.containsKey(new CompositeKey(2, "l4", 3)));
    Assert.assertTrue(keysToRemove.containsKey(new CompositeKey(2, "l1", 3)));
  }

  @Test
  public void testProcessChangeRidBagEventsTwo() {
    final var compositeIndexDefinition = new CompositeIndexDefinition();

    compositeIndexDefinition.addIndex(
        new PropertyIndexDefinition("testCollectionClass", "fOne", PropertyType.INTEGER));
    compositeIndexDefinition.addIndex(
        new PropertyRidBagIndexDefinition("testCollectionClass", "fTwo"));
    compositeIndexDefinition.addIndex(
        new PropertyIndexDefinition("testCollectionClass", "fThree", PropertyType.INTEGER));

    final var ridBag = new RidBag(db);

    ridBag.add(new RecordId("#10:1"));
    ridBag.add(new RecordId("#10:2"));
    ridBag.add(new RecordId("#10:3"));
    ridBag.remove(new RecordId("#10:2"));
    ridBag.disableTracking(null);
    ridBag.enableTracking(null);

    ridBag.add(new RecordId("#10:4"));
    ridBag.remove(new RecordId("#10:1"));

    var keysToAdd = new Object2IntOpenHashMap<CompositeKey>();
    keysToAdd.defaultReturnValue(-1);

    var keysToRemove = new Object2IntOpenHashMap<CompositeKey>();
    keysToRemove.defaultReturnValue(-1);

    for (var multiValueChangeEvent :
        ridBag.getTimeLine().getMultiValueChangeEvents()) {
      compositeIndexDefinition.processChangeEvent(
          db, multiValueChangeEvent, keysToAdd, keysToRemove, 2, 3);
    }

    Assert.assertEquals(1, keysToRemove.size());
    Assert.assertEquals(1, keysToAdd.size());

    Assert.assertTrue(keysToAdd.containsKey(new CompositeKey(2, new RecordId("#10:4"), 3)));
    Assert.assertTrue(keysToRemove.containsKey(new CompositeKey(2, new RecordId("#10:1"), 3)));
  }

  @Test
  public void testProcessChangeSetEventsOne() {
    final var compositeIndexDefinition = new CompositeIndexDefinition();

    compositeIndexDefinition.addIndex(
        new PropertyIndexDefinition("testCollectionClass", "fOne", PropertyType.INTEGER));
    compositeIndexDefinition.addIndex(
        new PropertyListIndexDefinition("testCollectionClass", "fTwo", PropertyType.STRING));
    compositeIndexDefinition.addIndex(
        new PropertyIndexDefinition("testCollectionClass", "fThree", PropertyType.INTEGER));

    final var doc = (EntityImpl) db.newEntity();
    RecordInternal.unsetDirty(doc);
    Assert.assertFalse(doc.isDirty());

    final var trackedSet = new TrackedSet<String>(doc);

    trackedSet.enableTracking(doc);
    trackedSet.add("l1");
    trackedSet.add("l2");
    trackedSet.add("l3");
    trackedSet.remove("l2");

    var keysToAdd = new Object2IntOpenHashMap<CompositeKey>();
    keysToAdd.defaultReturnValue(-1);

    var keysToRemove = new Object2IntOpenHashMap<CompositeKey>();
    keysToRemove.defaultReturnValue(-1);

    for (var multiValueChangeEvent :
        trackedSet.getTimeLine().getMultiValueChangeEvents()) {
      compositeIndexDefinition.processChangeEvent(
          db, multiValueChangeEvent, keysToAdd, keysToRemove, 2, 3);
    }

    Assert.assertEquals(0, keysToRemove.size());
    Assert.assertEquals(2, keysToAdd.size());

    Assert.assertTrue(keysToAdd.containsKey(new CompositeKey(2, "l1", 3)));
    Assert.assertTrue(keysToAdd.containsKey(new CompositeKey(2, "l3", 3)));
  }

  @Test
  public void testProcessChangeSetEventsTwo() {
    final var compositeIndexDefinition = new CompositeIndexDefinition();

    compositeIndexDefinition.addIndex(
        new PropertyIndexDefinition("testCollectionClass", "fOne", PropertyType.INTEGER));
    compositeIndexDefinition.addIndex(
        new PropertyListIndexDefinition("testCollectionClass", "fTwo", PropertyType.STRING));
    compositeIndexDefinition.addIndex(
        new PropertyIndexDefinition("testCollectionClass", "fThree", PropertyType.INTEGER));

    final var doc = (EntityImpl) db.newEntity();
    RecordInternal.unsetDirty(doc);
    Assert.assertFalse(doc.isDirty());

    final var trackedMap = new TrackedSet<String>(doc);

    trackedMap.add("l1");
    trackedMap.add("l2");
    trackedMap.add("l3");
    trackedMap.remove("l2");

    trackedMap.enableTracking(doc);
    trackedMap.add("l4");
    trackedMap.remove("l1");

    var keysToAdd = new Object2IntOpenHashMap<CompositeKey>();
    keysToAdd.defaultReturnValue(-1);

    var keysToRemove = new Object2IntOpenHashMap<CompositeKey>();
    keysToRemove.defaultReturnValue(-1);

    for (var multiValueChangeEvent :
        trackedMap.getTimeLine().getMultiValueChangeEvents()) {
      compositeIndexDefinition.processChangeEvent(
          db, multiValueChangeEvent, keysToAdd, keysToRemove, 2, 3);
    }

    Assert.assertEquals(1, keysToRemove.size());
    Assert.assertEquals(1, keysToAdd.size());

    Assert.assertTrue(keysToAdd.containsKey(new CompositeKey(2, "l4", 3)));
    Assert.assertTrue(keysToRemove.containsKey(new CompositeKey(2, "l1", 3)));
  }

  @Test
  public void testProcessChangeKeyMapEventsOne() {
    final var compositeIndexDefinition = new CompositeIndexDefinition();

    compositeIndexDefinition.addIndex(
        new PropertyIndexDefinition("testCollectionClass", "fOne", PropertyType.INTEGER));
    compositeIndexDefinition.addIndex(
        new PropertyMapIndexDefinition(
            "testCollectionClass", "fTwo", PropertyType.STRING,
            PropertyMapIndexDefinition.INDEX_BY.KEY));
    compositeIndexDefinition.addIndex(
        new PropertyIndexDefinition("testCollectionClass", "fThree", PropertyType.INTEGER));

    final var doc = (EntityImpl) db.newEntity();
    RecordInternal.unsetDirty(doc);
    Assert.assertFalse(doc.isDirty());

    final var trackedMap = new TrackedMap<String>(doc);
    trackedMap.enableTracking(doc);
    trackedMap.put("k1", "v1");
    trackedMap.put("k2", "v2");
    trackedMap.put("k3", "v3");
    trackedMap.remove("k2");

    var keysToAdd = new Object2IntOpenHashMap<CompositeKey>();
    keysToAdd.defaultReturnValue(-1);

    var keysToRemove = new Object2IntOpenHashMap<CompositeKey>();
    keysToRemove.defaultReturnValue(-1);

    for (var multiValueChangeEvent :
        trackedMap.getTimeLine().getMultiValueChangeEvents()) {
      compositeIndexDefinition.processChangeEvent(
          db, multiValueChangeEvent, keysToAdd, keysToRemove, 2, 3);
    }

    Assert.assertEquals(0, keysToRemove.size());
    Assert.assertEquals(2, keysToAdd.size());

    Assert.assertTrue(keysToAdd.containsKey(new CompositeKey(2, "k1", 3)));
    Assert.assertTrue(keysToAdd.containsKey(new CompositeKey(2, "k3", 3)));
  }

  @Test
  public void testProcessChangeKeyMapEventsTwo() {
    final var compositeIndexDefinition = new CompositeIndexDefinition();

    compositeIndexDefinition.addIndex(
        new PropertyIndexDefinition("testCollectionClass", "fOne", PropertyType.INTEGER));
    compositeIndexDefinition.addIndex(
        new PropertyMapIndexDefinition(
            "testCollectionClass", "fTwo", PropertyType.STRING,
            PropertyMapIndexDefinition.INDEX_BY.KEY));
    compositeIndexDefinition.addIndex(
        new PropertyIndexDefinition("testCollectionClass", "fThree", PropertyType.INTEGER));

    final var doc = (EntityImpl) db.newEntity();
    RecordInternal.unsetDirty(doc);
    Assert.assertFalse(doc.isDirty());

    final var trackedMap = new TrackedMap<String>(doc);

    trackedMap.put("k1", "v1");
    trackedMap.put("k2", "v2");
    trackedMap.put("k3", "v3");
    trackedMap.remove("k2");
    trackedMap.enableTracking(doc);

    trackedMap.put("k4", "v4");
    trackedMap.remove("k1");

    var keysToAdd = new Object2IntOpenHashMap<CompositeKey>();
    keysToAdd.defaultReturnValue(-1);

    var keysToRemove = new Object2IntOpenHashMap<CompositeKey>();
    keysToRemove.defaultReturnValue(-1);

    for (var multiValueChangeEvent :
        trackedMap.getTimeLine().getMultiValueChangeEvents()) {
      compositeIndexDefinition.processChangeEvent(
          db, multiValueChangeEvent, keysToAdd, keysToRemove, 2, 3);
    }

    Assert.assertEquals(1, keysToRemove.size());
    Assert.assertEquals(1, keysToAdd.size());

    Assert.assertTrue(keysToAdd.containsKey(new CompositeKey(2, "k4", 3)));
    Assert.assertTrue(keysToRemove.containsKey(new CompositeKey(2, "k1", 3)));
  }

  @Test
  public void testClassName() {
    Assert.assertEquals("testClass", compositeIndex.getClassName());
  }
}
