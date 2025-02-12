package com.jetbrains.youtrack.db.auto;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import com.jetbrains.youtrack.db.api.exception.CommandSQLParsingException;
import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.api.schema.Schema;
import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import com.jetbrains.youtrack.db.internal.common.listener.ProgressListener;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.index.CompositeIndexDefinition;
import com.jetbrains.youtrack.db.internal.core.index.Index;
import com.jetbrains.youtrack.db.internal.core.index.IndexDefinition;
import com.jetbrains.youtrack.db.internal.core.index.IndexException;
import com.jetbrains.youtrack.db.internal.core.index.PropertyIndexDefinition;
import com.jetbrains.youtrack.db.internal.core.index.PropertyListIndexDefinition;
import com.jetbrains.youtrack.db.internal.core.index.PropertyMapIndexDefinition;
import com.jetbrains.youtrack.db.internal.core.index.PropertyRidBagIndexDefinition;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.SchemaClassInternal;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

@Test
public class ClassIndexTest extends BaseDBTest {

  private SchemaClassInternal oClass;
  private SchemaClassInternal oSuperClass;

  @Parameters(value = "remote")
  public ClassIndexTest(@Optional Boolean remote) {
    super(remote != null && remote);
  }

  @BeforeClass
  public void beforeClass() throws Exception {
    super.beforeClass();

    final Schema schema = session.getMetadata().getSchemaInternal();

    oClass = (SchemaClassInternal) schema.createClass("ClassIndexTestClass");
    oSuperClass = (SchemaClassInternal) schema.createClass("ClassIndexTestSuperClass");

    oClass.createProperty(session, "fOne", PropertyType.INTEGER);
    oClass.createProperty(session, "fTwo", PropertyType.STRING);
    oClass.createProperty(session, "fThree", PropertyType.BOOLEAN);
    oClass.createProperty(session, "fFour", PropertyType.INTEGER);

    oClass.createProperty(session, "fSix", PropertyType.STRING);
    oClass.createProperty(session, "fSeven", PropertyType.STRING);

    oClass.createProperty(session, "fEight", PropertyType.INTEGER);
    oClass.createProperty(session, "fTen", PropertyType.INTEGER);
    oClass.createProperty(session, "fEleven", PropertyType.INTEGER);
    oClass.createProperty(session, "fTwelve", PropertyType.INTEGER);
    oClass.createProperty(session, "fThirteen", PropertyType.INTEGER);
    oClass.createProperty(session, "fFourteen", PropertyType.INTEGER);
    oClass.createProperty(session, "fFifteen", PropertyType.INTEGER);

    oClass.createProperty(session, "fEmbeddedMap", PropertyType.EMBEDDEDMAP, PropertyType.INTEGER);
    oClass.createProperty(session, "fEmbeddedMapWithoutLinkedType", PropertyType.EMBEDDEDMAP);
    oClass.createProperty(session, "fLinkMap", PropertyType.LINKMAP);

    oClass.createProperty(session, "fLinkList", PropertyType.LINKLIST);
    oClass.createProperty(session, "fEmbeddedList", PropertyType.EMBEDDEDLIST,
        PropertyType.INTEGER);

    oClass.createProperty(session, "fEmbeddedSet", PropertyType.EMBEDDEDSET, PropertyType.INTEGER);
    oClass.createProperty(session, "fLinkSet", PropertyType.LINKSET);

    oClass.createProperty(session, "fRidBag", PropertyType.LINKBAG);

    oSuperClass.createProperty(session, "fNine", PropertyType.INTEGER);
    oClass.setSuperClass(session, oSuperClass);

    session.close();
  }

  @Test
  public void testCreateOnePropertyIndexTest() {
    oClass.createIndex(session,
        "ClassIndexTestPropertyOne",
        SchemaClass.INDEX_TYPE.UNIQUE.toString(),
        null,
        Map.of("ignoreNullValues", true), new String[]{"fOne"});
    assertEquals(
        session
            .getMetadata()
            .getIndexManagerInternal()
            .getClassIndex(session, "ClassIndexTestClass", "ClassIndexTestPropertyOne")
            .getName(),
        "ClassIndexTestPropertyOne");
  }

  @Test
  public void testCreateOnePropertyIndexInvalidName() {
    try {
      oClass.createIndex(session,
          "ClassIndex:TestPropertyOne",
          SchemaClass.INDEX_TYPE.UNIQUE.toString(),
          null,
          Map.of("ignoreNullValues", true), new String[]{"fOne"});
      fail();
    } catch (Exception e) {

      Throwable cause = e;
      while (cause.getCause() != null) {
        cause = cause.getCause();
      }

      assertTrue(
          (cause instanceof IllegalArgumentException)
              || (cause instanceof CommandSQLParsingException));
    }
  }

  @Test
  public void createCompositeIndexTestWithoutListener() {
    oClass.createIndex(session,
        "ClassIndexTestCompositeOne",
        SchemaClass.INDEX_TYPE.UNIQUE.toString(),
        null,
        Map.of("ignoreNullValues", true), new String[]{"fOne", "fTwo"});

    assertEquals(
        session
            .getMetadata()
            .getIndexManagerInternal()
            .getClassIndex(session, "ClassIndexTestClass", "ClassIndexTestCompositeOne")
            .getName(),
        "ClassIndexTestCompositeOne");
  }

  @Test
  public void createCompositeIndexTestWithListener() {
    final var atomicInteger = new AtomicInteger(0);
    final var progressListener =
        new ProgressListener() {
          @Override
          public void onBegin(final Object iTask, final long iTotal, Object metadata) {
            atomicInteger.incrementAndGet();
          }

          @Override
          public boolean onProgress(final Object iTask, final long iCounter, final float iPercent) {
            return true;
          }

          @Override
          public void onCompletition(DatabaseSessionInternal session, final Object iTask,
              final boolean iSucceed) {
            atomicInteger.incrementAndGet();
          }
        };

    oClass.createIndex(session,
        "ClassIndexTestCompositeTwo",
        SchemaClass.INDEX_TYPE.UNIQUE.toString(),
        progressListener,
        Map.of("ignoreNullValues", true),
        new String[]{"fOne", "fTwo", "fThree"});
    assertEquals(
        session
            .getMetadata()
            .getIndexManagerInternal()
            .getClassIndex(session, "ClassIndexTestClass", "ClassIndexTestCompositeTwo")
            .getName(),
        "ClassIndexTestCompositeTwo");
    assertEquals(atomicInteger.get(), 2);
  }

  @Test
  public void testCreateOnePropertyEmbeddedMapIndex() {
    oClass.createIndex(session,
        "ClassIndexTestPropertyEmbeddedMap",
        SchemaClass.INDEX_TYPE.UNIQUE.toString(),
        null,
        Map.of("ignoreNullValues", true), new String[]{"fEmbeddedMap"});

    assertEquals(
        oClass.getClassIndex(session, "ClassIndexTestPropertyEmbeddedMap").getName(),
        "ClassIndexTestPropertyEmbeddedMap");
    assertEquals(
        session
            .getMetadata()
            .getIndexManagerInternal()
            .getClassIndex(session, "ClassIndexTestClass", "ClassIndexTestPropertyEmbeddedMap")
            .getName(),
        "ClassIndexTestPropertyEmbeddedMap");

    final var indexDefinition = session.getIndex("ClassIndexTestPropertyEmbeddedMap")
        .getDefinition();

    assertTrue(indexDefinition instanceof PropertyMapIndexDefinition);
    assertEquals(indexDefinition.getFields().getFirst(), "fEmbeddedMap");
    assertEquals(indexDefinition.getTypes()[0], PropertyType.STRING);
    assertEquals(
        ((PropertyMapIndexDefinition) indexDefinition).getIndexBy(),
        PropertyMapIndexDefinition.INDEX_BY.KEY);
  }

  @Test
  public void testCreateCompositeEmbeddedMapIndex() {
    oClass.createIndex(session,
        "ClassIndexTestCompositeEmbeddedMap",
        SchemaClass.INDEX_TYPE.UNIQUE.toString(),
        null,
        Map.of("ignoreNullValues", true),
        new String[]{"fFifteen", "fEmbeddedMap"});

    assertEquals(
        session
            .getMetadata()
            .getIndexManagerInternal()
            .getClassIndex(session, "ClassIndexTestClass", "ClassIndexTestCompositeEmbeddedMap")
            .getName(),
        "ClassIndexTestCompositeEmbeddedMap");

    final var indexDefinition = session.getIndex("ClassIndexTestCompositeEmbeddedMap")
        .getDefinition();

    assertTrue(indexDefinition instanceof CompositeIndexDefinition);
    assertEquals(indexDefinition.getFields().toArray(), new String[]{"fFifteen", "fEmbeddedMap"});

    assertEquals(indexDefinition.getTypes(),
        new PropertyType[]{PropertyType.INTEGER, PropertyType.STRING});
    assertEquals(indexDefinition.getParamCount(), 2);
  }

  @Test
  public void testCreateCompositeEmbeddedMapByKeyIndex() {
    oClass.createIndex(session,
        "ClassIndexTestCompositeEmbeddedMapByKey",
        SchemaClass.INDEX_TYPE.UNIQUE.toString(),
        null,
        Map.of("ignoreNullValues", true),
        new String[]{"fEight", "fEmbeddedMap"});

    assertEquals(
        oClass.getClassIndex(session, "ClassIndexTestCompositeEmbeddedMapByKey").getName(),
        "ClassIndexTestCompositeEmbeddedMapByKey");
    assertEquals(
        session
            .getMetadata()
            .getIndexManagerInternal()
            .getClassIndex(
                session, "ClassIndexTestClass", "ClassIndexTestCompositeEmbeddedMapByKey")
            .getName(),
        "ClassIndexTestCompositeEmbeddedMapByKey");

    final var indexDefinition = session.getIndex(
        "ClassIndexTestCompositeEmbeddedMapByKey").getDefinition();

    assertTrue(indexDefinition instanceof CompositeIndexDefinition);
    assertEquals(indexDefinition.getFields().toArray(), new String[]{"fEight", "fEmbeddedMap"});

    assertEquals(indexDefinition.getTypes(),
        new PropertyType[]{PropertyType.INTEGER, PropertyType.STRING});
    assertEquals(indexDefinition.getParamCount(), 2);
  }

  @Test
  public void testCreateCompositeEmbeddedMapByValueIndex() {
    oClass.createIndex(session,
        "ClassIndexTestCompositeEmbeddedMapByValue",
        SchemaClass.INDEX_TYPE.UNIQUE.toString(),
        null,
        Map.of("ignoreNullValues", true),
        new String[]{"fTen", "fEmbeddedMap by value"});

    assertEquals(
        oClass.getClassIndex(session, "ClassIndexTestCompositeEmbeddedMapByValue").getName(),
        "ClassIndexTestCompositeEmbeddedMapByValue");
    assertEquals(
        session
            .getMetadata()
            .getIndexManagerInternal()
            .getClassIndex(
                session, "ClassIndexTestClass", "ClassIndexTestCompositeEmbeddedMapByValue")
            .getName(),
        "ClassIndexTestCompositeEmbeddedMapByValue");

    final var indexDefinition = session.getIndex(
        "ClassIndexTestCompositeEmbeddedMapByValue").getDefinition();

    assertTrue(indexDefinition instanceof CompositeIndexDefinition);
    assertEquals(indexDefinition.getFields().toArray(), new String[]{"fTen", "fEmbeddedMap"});

    assertEquals(indexDefinition.getTypes(),
        new PropertyType[]{PropertyType.INTEGER, PropertyType.INTEGER});
    assertEquals(indexDefinition.getParamCount(), 2);
  }

  @Test
  public void testCreateCompositeLinkMapByValueIndex() {
    oClass.createIndex(session,
        "ClassIndexTestCompositeLinkMapByValue",
        SchemaClass.INDEX_TYPE.UNIQUE.toString(),
        null,
        Map.of("ignoreNullValues", true),
        new String[]{"fEleven", "fLinkMap by value"});

    assertEquals(
        oClass.getClassIndex(session, "ClassIndexTestCompositeLinkMapByValue").getName(),
        "ClassIndexTestCompositeLinkMapByValue");
    assertEquals(
        session
            .getMetadata()
            .getIndexManagerInternal()
            .getClassIndex(session, "ClassIndexTestClass", "ClassIndexTestCompositeLinkMapByValue")
            .getName(),
        "ClassIndexTestCompositeLinkMapByValue");

    final var indexDefinition = session.getIndex(
        "ClassIndexTestCompositeLinkMapByValue").getDefinition();

    assertTrue(indexDefinition instanceof CompositeIndexDefinition);
    assertEquals(indexDefinition.getFields().toArray(), new String[]{"fEleven", "fLinkMap"});

    assertEquals(indexDefinition.getTypes(),
        new PropertyType[]{PropertyType.INTEGER, PropertyType.LINK});
    assertEquals(indexDefinition.getParamCount(), 2);
  }

  @Test
  public void testCreateCompositeEmbeddedSetIndex() {
    oClass.createIndex(session,
        "ClassIndexTestCompositeEmbeddedSet",
        SchemaClass.INDEX_TYPE.UNIQUE.toString(),
        null,
        Map.of("ignoreNullValues", true),
        new String[]{"fTwelve", "fEmbeddedSet"});

    assertEquals(
        oClass.getClassIndex(session, "ClassIndexTestCompositeEmbeddedSet").getName(),
        "ClassIndexTestCompositeEmbeddedSet");
    assertEquals(
        session
            .getMetadata()
            .getIndexManagerInternal()
            .getClassIndex(session, "ClassIndexTestClass", "ClassIndexTestCompositeEmbeddedSet")
            .getName(),
        "ClassIndexTestCompositeEmbeddedSet");

    final var indexDefinition = session.getIndex("ClassIndexTestCompositeEmbeddedSet")
        .getDefinition();

    assertTrue(indexDefinition instanceof CompositeIndexDefinition);
    assertEquals(indexDefinition.getFields().toArray(), new String[]{"fTwelve", "fEmbeddedSet"});

    assertEquals(indexDefinition.getTypes(),
        new PropertyType[]{PropertyType.INTEGER, PropertyType.INTEGER});
    assertEquals(indexDefinition.getParamCount(), 2);
  }

  @Test(dependsOnMethods = "testGetIndexes")
  public void testCreateCompositeLinkSetIndex() {
    var indexName = "ClassIndexTestCompositeLinkSet";
    oClass.createIndex(session,
        "ClassIndexTestCompositeLinkSet",
        SchemaClass.INDEX_TYPE.UNIQUE.toString(),
        null,
        Map.of("ignoreNullValues", true),
        new String[]{"fTwelve", "fLinkSet"});

    assertEquals(
        oClass.getClassIndex(session, "ClassIndexTestCompositeLinkSet").getName(),
        indexName);
    assertEquals(
        session
            .getMetadata()
            .getIndexManagerInternal()
            .getClassIndex(session, "ClassIndexTestClass", "ClassIndexTestCompositeLinkSet")
            .getName(),
        indexName);

    final var indexDefinition = session.getIndex(indexName)
        .getDefinition();

    assertTrue(indexDefinition instanceof CompositeIndexDefinition);
    assertEquals(indexDefinition.getFields().toArray(), new String[]{"fTwelve", "fLinkSet"});

    assertEquals(indexDefinition.getTypes(),
        new PropertyType[]{PropertyType.INTEGER, PropertyType.LINK});
    assertEquals(indexDefinition.getParamCount(), 2);
  }

  @Test
  public void testCreateCompositeEmbeddedListIndex() {
    var indexName = "ClassIndexTestCompositeEmbeddedList";
    oClass.createIndex(session,
        indexName,
        SchemaClass.INDEX_TYPE.UNIQUE.toString(),
        null,
        Map.of("ignoreNullValues", true),
        new String[]{"fThirteen", "fEmbeddedList"});

    assertEquals(
        oClass.getClassIndex(session, indexName).getName(),
        indexName);
    assertEquals(
        session
            .getMetadata()
            .getIndexManagerInternal()
            .getClassIndex(session, "ClassIndexTestClass", indexName)
            .getName(),
        indexName);

    final var indexDefinition = session.getIndex(indexName)
        .getDefinition();

    assertTrue(indexDefinition instanceof CompositeIndexDefinition);
    assertEquals(
        indexDefinition.getFields().toArray(), new String[]{"fThirteen", "fEmbeddedList"});

    assertEquals(indexDefinition.getTypes(),
        new PropertyType[]{PropertyType.INTEGER, PropertyType.INTEGER});
    assertEquals(indexDefinition.getParamCount(), 2);
  }

  public void testCreateCompositeLinkListIndex() {
    var indexName = "ClassIndexTestCompositeLinkList";
    oClass.createIndex(session,
        indexName,
        SchemaClass.INDEX_TYPE.UNIQUE.toString(),
        null,
        Map.of("ignoreNullValues", true),
        new String[]{"fFourteen", "fLinkList"});

    assertEquals(
        oClass.getClassIndex(session, indexName).getName(),
        indexName);
    assertEquals(
        session
            .getMetadata()
            .getIndexManagerInternal()
            .getClassIndex(session, "ClassIndexTestClass", indexName)
            .getName(),
        indexName);

    final var indexDefinition = session.getIndex(indexName).getDefinition();

    assertTrue(indexDefinition instanceof CompositeIndexDefinition);
    assertEquals(indexDefinition.getFields().toArray(), new String[]{"fFourteen", "fLinkList"});

    assertEquals(indexDefinition.getTypes(),
        new PropertyType[]{PropertyType.INTEGER, PropertyType.LINK});
    assertEquals(indexDefinition.getParamCount(), 2);
  }

  public void testCreateCompositeRidBagIndex() {
    var indexName = "ClassIndexTestCompositeRidBag";
    oClass.createIndex(session,
        indexName,
        SchemaClass.INDEX_TYPE.UNIQUE.toString(),
        null,
        Map.of("ignoreNullValues", true),
        new String[]{"fFourteen", "fRidBag"});

    assertEquals(oClass.getClassIndex(session, indexName).getName(),
        indexName);
    assertEquals(
        session
            .getMetadata()
            .getIndexManagerInternal()
            .getClassIndex(session, "ClassIndexTestClass", indexName)
            .getName(),
        indexName);

    final var indexDefinition = session.getIndex(indexName).getDefinition();

    assertTrue(indexDefinition instanceof CompositeIndexDefinition);
    assertEquals(indexDefinition.getFields().toArray(), new String[]{"fFourteen", "fRidBag"});

    assertEquals(indexDefinition.getTypes(),
        new PropertyType[]{PropertyType.INTEGER, PropertyType.LINK});
    assertEquals(indexDefinition.getParamCount(), 2);
  }

  @Test
  public void testCreateOnePropertyLinkedMapIndex() {
    var indexName = "ClassIndexTestPropertyLinkedMap";
    oClass.createIndex(session,
        indexName,
        SchemaClass.INDEX_TYPE.UNIQUE.toString(),
        null,
        Map.of("ignoreNullValues", true), new String[]{"fLinkMap"});

    assertEquals(
        oClass.getClassIndex(session, indexName).getName(),
        indexName);
    assertEquals(
        session
            .getMetadata()
            .getIndexManagerInternal()
            .getClassIndex(session, "ClassIndexTestClass", indexName)
            .getName(),
        indexName);

    final var indexDefinition = session.getIndex(indexName).getDefinition();

    assertTrue(indexDefinition instanceof PropertyMapIndexDefinition);
    assertEquals(indexDefinition.getFields().getFirst(), "fLinkMap");
    assertEquals(indexDefinition.getTypes()[0], PropertyType.STRING);
    assertEquals(
        ((PropertyMapIndexDefinition) indexDefinition).getIndexBy(),
        PropertyMapIndexDefinition.INDEX_BY.KEY);
  }

  @Test
  public void testCreateOnePropertyLinkMapByKeyIndex() {
    var indexName = "ClassIndexTestPropertyLinkedMapByKey";
    oClass.createIndex(session,
        indexName,
        SchemaClass.INDEX_TYPE.UNIQUE.toString(),
        null,
        Map.of("ignoreNullValues", true), new String[]{"fLinkMap by key"});

    assertEquals(
        oClass.getClassIndex(session, indexName).getName(),
        indexName);
    assertEquals(
        session
            .getMetadata()
            .getIndexManagerInternal()
            .getClassIndex(session, "ClassIndexTestClass", indexName)
            .getName(),
        indexName);

    final var indexDefinition = session.getIndex(indexName).getDefinition();

    assertTrue(indexDefinition instanceof PropertyMapIndexDefinition);
    assertEquals(indexDefinition.getFields().getFirst(), "fLinkMap");
    assertEquals(indexDefinition.getTypes()[0], PropertyType.STRING);
    assertEquals(
        ((PropertyMapIndexDefinition) indexDefinition).getIndexBy(),
        PropertyMapIndexDefinition.INDEX_BY.KEY);
  }

  @Test
  public void testCreateOnePropertyLinkMapByValueIndex() {
    var indexName = "ClassIndexTestPropertyLinkedMapByValue";
    oClass.createIndex(session,
        indexName,
        SchemaClass.INDEX_TYPE.UNIQUE.toString(),
        null,
        Map.of("ignoreNullValues", true), new String[]{"fLinkMap by value"});

    assertEquals(
        oClass.getClassIndex(session, indexName).getName(),
        indexName);
    assertEquals(
        session
            .getMetadata()
            .getIndexManagerInternal()
            .getClassIndex(
                session, "ClassIndexTestClass", indexName)
            .getName(),
        indexName);

    final var indexDefinition = session.getIndex(indexName).getDefinition();

    assertTrue(indexDefinition instanceof PropertyMapIndexDefinition);
    assertEquals(indexDefinition.getFields().getFirst(), "fLinkMap");
    assertEquals(indexDefinition.getTypes()[0], PropertyType.LINK);
    assertEquals(
        ((PropertyMapIndexDefinition) indexDefinition).getIndexBy(),
        PropertyMapIndexDefinition.INDEX_BY.VALUE);
  }

  @Test
  public void testCreateOnePropertyByKeyEmbeddedMapIndex() {
    var indexName = "ClassIndexTestPropertyByKeyEmbeddedMap";
    oClass.createIndex(session,
        indexName,
        SchemaClass.INDEX_TYPE.UNIQUE.toString(),
        null,
        Map.of("ignoreNullValues", true),
        new String[]{"fEmbeddedMap by key"});

    assertEquals(
        oClass.getClassIndex(session, indexName).getName(),
        indexName);
    assertEquals(
        session
            .getMetadata()
            .getIndexManagerInternal()
            .getClassIndex(
                session, "ClassIndexTestClass", indexName)
            .getName(),
        indexName);

    final var indexDefinition = session.getIndex(indexName).getDefinition();

    assertTrue(indexDefinition instanceof PropertyMapIndexDefinition);
    assertEquals(indexDefinition.getFields().getFirst(), "fEmbeddedMap");
    assertEquals(indexDefinition.getTypes()[0], PropertyType.STRING);
    assertEquals(
        ((PropertyMapIndexDefinition) indexDefinition).getIndexBy(),
        PropertyMapIndexDefinition.INDEX_BY.KEY);
  }

  @Test
  public void testCreateOnePropertyByValueEmbeddedMapIndex() {
    var indexName = "ClassIndexTestPropertyByValueEmbeddedMap";
    oClass.createIndex(session,
        indexName,
        SchemaClass.INDEX_TYPE.UNIQUE.toString(),
        null,
        Map.of("ignoreNullValues", true),
        new String[]{"fEmbeddedMap by value"});

    assertEquals(
        oClass.getClassIndex(session, indexName).getName(),
        indexName);
    assertEquals(
        session
            .getMetadata()
            .getIndexManagerInternal()
            .getClassIndex(
                session, "ClassIndexTestClass", indexName)
            .getName(),
        indexName);

    final var indexDefinition = session.getIndex(indexName).getDefinition();

    assertTrue(indexDefinition instanceof PropertyMapIndexDefinition);
    assertEquals(indexDefinition.getFields().getFirst(), "fEmbeddedMap");
    assertEquals(indexDefinition.getTypes()[0], PropertyType.INTEGER);
    assertEquals(
        ((PropertyMapIndexDefinition) indexDefinition).getIndexBy(),
        PropertyMapIndexDefinition.INDEX_BY.VALUE);
  }

  @Test
  public void testCreateOnePropertyWrongSpecifierEmbeddedMapIndexOne() {
    var exceptionIsThrown = false;
    try {
      oClass.createIndex(session,
          "ClassIndexTestPropertyWrongSpecifierEmbeddedMap",
          SchemaClass.INDEX_TYPE.UNIQUE.toString(),
          null,
          Map.of("ignoreNullValues", true), new String[]{"fEmbeddedMap by ttt"});
    } catch (Exception e) {
      Assert.assertTrue(e instanceof IllegalArgumentException);
      exceptionIsThrown = true;
      assertEquals(
          e.getMessage(),
          "Illegal field name format, should be '<property> [by key|value]' but was 'fEmbeddedMap"
              + " by ttt'");
    }

    assertTrue(exceptionIsThrown);
    assertNull(oClass.getClassIndex(session, "ClassIndexTestPropertyWrongSpecifierEmbeddedMap"));
  }

  @Test
  public void testCreateOnePropertyWrongSpecifierEmbeddedMapIndexTwo() {
    var exceptionIsThrown = false;
    try {
      oClass.createIndex(session,
          "ClassIndexTestPropertyWrongSpecifierEmbeddedMap",
          SchemaClass.INDEX_TYPE.UNIQUE.toString(),
          null,
          Map.of("ignoreNullValues", true),
          new String[]{"fEmbeddedMap b value"});
    } catch (IndexException e) {
      exceptionIsThrown = true;
    }

    assertTrue(exceptionIsThrown);
    assertNull(oClass.getClassIndex(session, "ClassIndexTestPropertyWrongSpecifierEmbeddedMap"));
  }

  @Test
  public void testCreateOnePropertyWrongSpecifierEmbeddedMapIndexThree() {
    var exceptionIsThrown = false;
    try {
      oClass.createIndex(session,
          "ClassIndexTestPropertyWrongSpecifierEmbeddedMap",
          SchemaClass.INDEX_TYPE.UNIQUE.toString(),
          null,
          Map.of("ignoreNullValues", true),
          new String[]{"fEmbeddedMap by value t"});
    } catch (IndexException e) {
      exceptionIsThrown = true;
    }

    assertTrue(exceptionIsThrown);
    assertNull(oClass.getClassIndex(session, "ClassIndexTestPropertyWrongSpecifierEmbeddedMap"));
  }

  @Test(
      dependsOnMethods = {
          "createCompositeIndexTestWithListener",
          "createCompositeIndexTestWithoutListener",
          "testCreateOnePropertyIndexTest",
          "testCreateOnePropertyEmbeddedMapIndex",
          "testCreateOnePropertyByKeyEmbeddedMapIndex",
          "testCreateOnePropertyByValueEmbeddedMapIndex",
          "testCreateOnePropertyLinkedMapIndex",
          "testCreateOnePropertyLinkMapByKeyIndex",
          "testCreateOnePropertyLinkMapByValueIndex",
          "testCreateCompositeEmbeddedMapIndex",
          "testCreateCompositeEmbeddedMapByKeyIndex",
          "testCreateCompositeEmbeddedMapByValueIndex",
          "testCreateCompositeLinkMapByValueIndex",
          "testCreateCompositeEmbeddedSetIndex",
          "testCreateCompositeEmbeddedListIndex"
      })
  public void testAreIndexedOneProperty() {
    final var result = oClass.areIndexed(session, List.of("fOne"));

    assertTrue(result);
  }

  @Test(
      dependsOnMethods = {
          "createCompositeIndexTestWithListener",
          "createCompositeIndexTestWithoutListener",
          "testCreateOnePropertyIndexTest",
          "testCreateOnePropertyEmbeddedMapIndex",
          "testCreateOnePropertyByKeyEmbeddedMapIndex",
          "testCreateOnePropertyByValueEmbeddedMapIndex",
          "testCreateOnePropertyLinkedMapIndex",
          "testCreateOnePropertyLinkMapByKeyIndex",
          "testCreateOnePropertyLinkMapByValueIndex",
          "testCreateCompositeEmbeddedMapIndex",
          "testCreateCompositeEmbeddedMapIndex",
          "testCreateCompositeEmbeddedMapByKeyIndex",
          "testCreateCompositeEmbeddedMapByValueIndex",
          "testCreateCompositeLinkMapByValueIndex",
          "testCreateCompositeEmbeddedSetIndex",
          "testCreateCompositeEmbeddedListIndex"
      })
  public void testAreIndexedEightProperty() {
    final var result = oClass.areIndexed(session, List.of("fEight"));
    assertTrue(result);
  }

  @Test(
      dependsOnMethods = {
          "createCompositeIndexTestWithListener",
          "createCompositeIndexTestWithoutListener",
          "testCreateOnePropertyIndexTest",
          "testCreateOnePropertyEmbeddedMapIndex",
          "testCreateOnePropertyByKeyEmbeddedMapIndex",
          "testCreateOnePropertyByValueEmbeddedMapIndex",
          "testCreateOnePropertyLinkedMapIndex",
          "testCreateOnePropertyLinkMapByKeyIndex",
          "testCreateOnePropertyLinkMapByValueIndex",
          "testCreateCompositeEmbeddedMapIndex",
          "testCreateCompositeEmbeddedMapByKeyIndex",
          "testCreateCompositeEmbeddedMapByKeyIndex",
          "testCreateCompositeEmbeddedMapByValueIndex",
          "testCreateCompositeLinkMapByValueIndex",
          "testCreateCompositeEmbeddedSetIndex",
          "testCreateCompositeEmbeddedListIndex"
      })
  public void testAreIndexedEightPropertyEmbeddedMap() {
    final var result = oClass.areIndexed(session, Arrays.asList("fEmbeddedMap", "fEight"));
    assertTrue(result);
  }

  @Test(
      dependsOnMethods = {
          "createCompositeIndexTestWithListener",
          "createCompositeIndexTestWithoutListener",
          "testCreateOnePropertyIndexTest",
          "testCreateOnePropertyEmbeddedMapIndex",
          "testCreateOnePropertyByKeyEmbeddedMapIndex",
          "testCreateOnePropertyByValueEmbeddedMapIndex",
          "testCreateOnePropertyLinkedMapIndex",
          "testCreateOnePropertyLinkMapByKeyIndex",
          "testCreateOnePropertyLinkMapByValueIndex",
          "testCreateCompositeEmbeddedMapIndex",
          "testCreateCompositeEmbeddedMapByKeyIndex",
          "testCreateCompositeEmbeddedMapByValueIndex",
          "testCreateCompositeLinkMapByValueIndex",
          "testCreateCompositeEmbeddedSetIndex",
          "testCreateCompositeEmbeddedListIndex"
      })
  public void testAreIndexedDoesNotContainProperty() {
    final var result = oClass.areIndexed(session, List.of("fSix"));

    assertFalse(result);
  }

  @Test(
      dependsOnMethods = {
          "createCompositeIndexTestWithListener",
          "createCompositeIndexTestWithoutListener",
          "testCreateOnePropertyIndexTest",
          "testCreateOnePropertyEmbeddedMapIndex",
          "testCreateOnePropertyByKeyEmbeddedMapIndex",
          "testCreateOnePropertyByValueEmbeddedMapIndex",
          "testCreateOnePropertyLinkedMapIndex",
          "testCreateOnePropertyLinkMapByKeyIndex",
          "testCreateOnePropertyLinkMapByValueIndex",
          "testCreateCompositeEmbeddedMapIndex",
          "testCreateCompositeEmbeddedMapByKeyIndex",
          "testCreateCompositeEmbeddedMapByValueIndex",
          "testCreateCompositeLinkMapByValueIndex",
          "testCreateCompositeEmbeddedSetIndex",
          "testCreateCompositeEmbeddedListIndex"
      })
  public void testAreIndexedTwoProperties() {
    final var result = oClass.areIndexed(session, Arrays.asList("fTwo", "fOne"));

    assertTrue(result);
  }

  @Test(
      dependsOnMethods = {
          "createCompositeIndexTestWithListener",
          "createCompositeIndexTestWithoutListener",
          "testCreateOnePropertyIndexTest",
          "testCreateOnePropertyEmbeddedMapIndex",
          "testCreateOnePropertyByKeyEmbeddedMapIndex",
          "testCreateOnePropertyByValueEmbeddedMapIndex",
          "testCreateOnePropertyLinkedMapIndex",
          "testCreateOnePropertyLinkMapByKeyIndex",
          "testCreateOnePropertyLinkMapByValueIndex",
          "testCreateCompositeEmbeddedMapIndex",
          "testCreateCompositeEmbeddedMapByKeyIndex",
          "testCreateCompositeEmbeddedMapByValueIndex",
          "testCreateCompositeLinkMapByValueIndex",
          "testCreateCompositeEmbeddedSetIndex",
          "testCreateCompositeEmbeddedListIndex"
      })
  public void testAreIndexedThreeProperties() {
    final var result = oClass.areIndexed(session, Arrays.asList("fTwo", "fOne", "fThree"));

    assertTrue(result);
  }

  @Test(
      dependsOnMethods = {
          "createCompositeIndexTestWithListener",
          "createCompositeIndexTestWithoutListener",
          "testCreateOnePropertyIndexTest",
          "testCreateOnePropertyEmbeddedMapIndex",
          "testCreateOnePropertyByKeyEmbeddedMapIndex",
          "testCreateOnePropertyByValueEmbeddedMapIndex",
          "testCreateOnePropertyLinkedMapIndex",
          "testCreateOnePropertyLinkMapByKeyIndex",
          "testCreateOnePropertyLinkMapByValueIndex",
          "testCreateCompositeEmbeddedMapIndex",
          "testCreateCompositeEmbeddedMapByKeyIndex",
          "testCreateCompositeEmbeddedMapByValueIndex",
          "testCreateCompositeLinkMapByValueIndex",
          "testCreateCompositeEmbeddedSetIndex",
          "testCreateCompositeEmbeddedListIndex"
      })
  public void testAreIndexedPropertiesNotFirst() {
    final var result = oClass.areIndexed(session, Arrays.asList("fTwo", "fTree"));

    assertFalse(result);
  }

  @Test(
      dependsOnMethods = {
          "createCompositeIndexTestWithListener",
          "createCompositeIndexTestWithoutListener",
          "testCreateOnePropertyIndexTest",
          "testCreateOnePropertyEmbeddedMapIndex",
          "testCreateOnePropertyByKeyEmbeddedMapIndex",
          "testCreateOnePropertyByValueEmbeddedMapIndex",
          "testCreateOnePropertyLinkedMapIndex",
          "testCreateOnePropertyLinkMapByKeyIndex",
          "testCreateOnePropertyLinkMapByValueIndex",
          "testCreateCompositeEmbeddedMapIndex",
          "testCreateCompositeEmbeddedMapByKeyIndex",
          "testCreateCompositeEmbeddedMapByValueIndex",
          "testCreateCompositeLinkMapByValueIndex",
          "testCreateCompositeEmbeddedSetIndex",
          "testCreateCompositeEmbeddedListIndex"
      })
  public void testAreIndexedPropertiesMoreThanNeeded() {
    final var result = oClass.areIndexed(session,
        Arrays.asList("fTwo", "fOne", "fThee", "fFour"));

    assertFalse(result);
  }

  @Test(
      dependsOnMethods = {
          "createCompositeIndexTestWithListener",
          "createCompositeIndexTestWithoutListener",
          "testCreateOnePropertyIndexTest",
          "createParentPropertyIndex",
          "testCreateOnePropertyEmbeddedMapIndex",
          "testCreateOnePropertyByKeyEmbeddedMapIndex",
          "testCreateOnePropertyByValueEmbeddedMapIndex",
          "testCreateOnePropertyLinkedMapIndex",
          "testCreateOnePropertyLinkMapByKeyIndex",
          "testCreateOnePropertyLinkMapByValueIndex",
          "testCreateCompositeEmbeddedMapIndex",
          "testCreateCompositeEmbeddedMapByKeyIndex",
          "testCreateCompositeEmbeddedMapByValueIndex",
          "testCreateCompositeLinkMapByValueIndex",
          "testCreateCompositeEmbeddedSetIndex",
          "testCreateCompositeEmbeddedListIndex"
      })
  public void testAreIndexedParentProperty() {
    final var result = oClass.areIndexed(session, List.of("fNine"));

    assertTrue(result);
  }

  @Test(
      dependsOnMethods = {
          "createCompositeIndexTestWithListener",
          "createCompositeIndexTestWithoutListener",
          "testCreateOnePropertyIndexTest",
          "testCreateOnePropertyEmbeddedMapIndex",
          "testCreateOnePropertyByKeyEmbeddedMapIndex",
          "testCreateOnePropertyByValueEmbeddedMapIndex",
          "testCreateOnePropertyLinkedMapIndex",
          "testCreateOnePropertyLinkMapByKeyIndex",
          "testCreateOnePropertyLinkMapByValueIndex",
          "testCreateCompositeEmbeddedMapIndex",
          "testCreateCompositeEmbeddedMapByKeyIndex",
          "testCreateCompositeEmbeddedMapByValueIndex",
          "testCreateCompositeLinkMapByValueIndex",
          "testCreateCompositeEmbeddedSetIndex",
          "testCreateCompositeEmbeddedListIndex"
      })
  public void testAreIndexedParentChildProperty() {
    final var result = oClass.areIndexed(session, List.of("fOne, fNine"));

    assertFalse(result);
  }

  @Test(
      dependsOnMethods = {
          "createCompositeIndexTestWithListener",
          "createCompositeIndexTestWithoutListener",
          "testCreateOnePropertyIndexTest",
          "testCreateOnePropertyEmbeddedMapIndex",
          "testCreateOnePropertyByKeyEmbeddedMapIndex",
          "testCreateOnePropertyByValueEmbeddedMapIndex",
          "testCreateOnePropertyLinkedMapIndex",
          "testCreateOnePropertyLinkMapByKeyIndex",
          "testCreateOnePropertyLinkMapByValueIndex",
          "testCreateCompositeEmbeddedMapIndex",
          "testCreateCompositeEmbeddedMapByKeyIndex",
          "testCreateCompositeEmbeddedMapByValueIndex",
          "testCreateCompositeLinkMapByValueIndex",
          "testCreateCompositeEmbeddedSetIndex",
          "testCreateCompositeEmbeddedListIndex"
      })
  public void testAreIndexedOnePropertyArrayParams() {
    final var result = oClass.areIndexed(session, "fOne");

    assertTrue(result);
  }

  @Test(
      dependsOnMethods = {
          "createCompositeIndexTestWithListener",
          "createCompositeIndexTestWithoutListener",
          "testCreateOnePropertyIndexTest",
          "testCreateOnePropertyEmbeddedMapIndex",
          "testCreateOnePropertyByKeyEmbeddedMapIndex",
          "testCreateOnePropertyByValueEmbeddedMapIndex",
          "testCreateOnePropertyLinkedMapIndex",
          "testCreateOnePropertyLinkMapByKeyIndex",
          "testCreateOnePropertyLinkMapByValueIndex",
          "testCreateCompositeEmbeddedMapIndex",
          "testCreateCompositeEmbeddedMapByKeyIndex",
          "testCreateCompositeEmbeddedMapByValueIndex",
          "testCreateCompositeLinkMapByValueIndex",
          "testCreateCompositeEmbeddedSetIndex",
          "testCreateCompositeEmbeddedListIndex"
      })
  public void testAreIndexedDoesNotContainPropertyArrayParams() {
    final var result = oClass.areIndexed(session, "fSix");

    assertFalse(result);
  }

  @Test(
      dependsOnMethods = {
          "createCompositeIndexTestWithListener",
          "createCompositeIndexTestWithoutListener",
          "testCreateOnePropertyIndexTest",
          "testCreateOnePropertyEmbeddedMapIndex",
          "testCreateOnePropertyByKeyEmbeddedMapIndex",
          "testCreateOnePropertyByValueEmbeddedMapIndex",
          "testCreateOnePropertyLinkedMapIndex",
          "testCreateOnePropertyLinkMapByKeyIndex",
          "testCreateOnePropertyLinkMapByValueIndex",
          "testCreateCompositeEmbeddedMapIndex",
          "testCreateCompositeEmbeddedMapByKeyIndex",
          "testCreateCompositeEmbeddedMapByValueIndex",
          "testCreateCompositeLinkMapByValueIndex",
          "testCreateCompositeEmbeddedSetIndex",
          "testCreateCompositeEmbeddedListIndex"
      })
  public void testAreIndexedTwoPropertiesArrayParams() {
    final var result = oClass.areIndexed(session, "fTwo", "fOne");

    assertTrue(result);
  }

  @Test(
      dependsOnMethods = {
          "createCompositeIndexTestWithListener",
          "createCompositeIndexTestWithoutListener",
          "testCreateOnePropertyIndexTest",
          "testCreateOnePropertyEmbeddedMapIndex",
          "testCreateOnePropertyByKeyEmbeddedMapIndex",
          "testCreateOnePropertyByValueEmbeddedMapIndex",
          "testCreateOnePropertyLinkedMapIndex",
          "testCreateOnePropertyLinkMapByKeyIndex",
          "testCreateOnePropertyLinkMapByValueIndex",
          "testCreateCompositeEmbeddedMapIndex",
          "testCreateCompositeEmbeddedMapByKeyIndex",
          "testCreateCompositeEmbeddedMapByValueIndex",
          "testCreateCompositeLinkMapByValueIndex",
          "testCreateCompositeEmbeddedSetIndex",
          "testCreateCompositeEmbeddedListIndex"
      })
  public void testAreIndexedThreePropertiesArrayParams() {
    final var result = oClass.areIndexed(session, "fTwo", "fOne", "fThree");

    assertTrue(result);
  }

  @Test(
      dependsOnMethods = {
          "createCompositeIndexTestWithListener",
          "createCompositeIndexTestWithoutListener",
          "testCreateOnePropertyIndexTest",
          "testCreateOnePropertyEmbeddedMapIndex",
          "testCreateOnePropertyByKeyEmbeddedMapIndex",
          "testCreateOnePropertyByValueEmbeddedMapIndex",
          "testCreateOnePropertyLinkedMapIndex",
          "testCreateOnePropertyLinkMapByKeyIndex",
          "testCreateOnePropertyLinkMapByValueIndex",
          "testCreateCompositeEmbeddedMapIndex",
          "testCreateCompositeEmbeddedMapByKeyIndex",
          "testCreateCompositeEmbeddedMapByValueIndex",
          "testCreateCompositeLinkMapByValueIndex",
          "testCreateCompositeEmbeddedSetIndex",
          "testCreateCompositeEmbeddedListIndex"
      })
  public void testAreIndexedPropertiesNotFirstArrayParams() {
    final var result = oClass.areIndexed(session, "fTwo", "fTree");

    assertFalse(result);
  }

  @Test(
      dependsOnMethods = {
          "createCompositeIndexTestWithListener",
          "createCompositeIndexTestWithoutListener",
          "testCreateOnePropertyIndexTest",
          "testCreateOnePropertyEmbeddedMapIndex",
          "testCreateOnePropertyByKeyEmbeddedMapIndex",
          "testCreateOnePropertyByValueEmbeddedMapIndex",
          "testCreateOnePropertyLinkedMapIndex",
          "testCreateOnePropertyLinkMapByKeyIndex",
          "testCreateOnePropertyLinkMapByValueIndex",
          "testCreateCompositeEmbeddedMapIndex",
          "testCreateCompositeEmbeddedMapByKeyIndex",
          "testCreateCompositeEmbeddedMapByValueIndex",
          "testCreateCompositeLinkMapByValueIndex",
          "testCreateCompositeEmbeddedSetIndex",
          "testCreateCompositeEmbeddedListIndex"
      })
  public void testAreIndexedPropertiesMoreThanNeededArrayParams() {
    final var result = oClass.areIndexed(session, "fTwo", "fOne", "fThee", "fFour");

    assertFalse(result);
  }

  @Test(
      dependsOnMethods = {
          "createCompositeIndexTestWithListener",
          "createCompositeIndexTestWithoutListener",
          "testCreateOnePropertyIndexTest",
          "createParentPropertyIndex",
          "testCreateOnePropertyEmbeddedMapIndex",
          "testCreateOnePropertyByKeyEmbeddedMapIndex",
          "testCreateOnePropertyByValueEmbeddedMapIndex",
          "testCreateOnePropertyLinkedMapIndex",
          "testCreateOnePropertyLinkMapByKeyIndex",
          "testCreateOnePropertyLinkMapByValueIndex",
          "testCreateCompositeEmbeddedMapIndex",
          "testCreateCompositeEmbeddedMapByKeyIndex",
          "testCreateCompositeEmbeddedMapByValueIndex",
          "testCreateCompositeLinkMapByValueIndex",
          "testCreateCompositeEmbeddedSetIndex",
          "testCreateCompositeEmbeddedListIndex"
      })
  public void testAreIndexedParentPropertyArrayParams() {
    final var result = oClass.areIndexed(session, "fNine");

    assertTrue(result);
  }

  @Test(
      dependsOnMethods = {
          "createCompositeIndexTestWithListener",
          "createCompositeIndexTestWithoutListener",
          "testCreateOnePropertyIndexTest",
          "testCreateOnePropertyEmbeddedMapIndex",
          "testCreateOnePropertyByKeyEmbeddedMapIndex",
          "testCreateOnePropertyByValueEmbeddedMapIndex",
          "testCreateOnePropertyLinkedMapIndex",
          "testCreateOnePropertyLinkMapByKeyIndex",
          "testCreateOnePropertyLinkMapByValueIndex",
          "testCreateCompositeEmbeddedMapIndex",
          "testCreateCompositeEmbeddedMapByKeyIndex",
          "testCreateCompositeEmbeddedMapByValueIndex",
          "testCreateCompositeLinkMapByValueIndex",
          "testCreateCompositeEmbeddedSetIndex",
          "testCreateCompositeEmbeddedListIndex"
      })
  public void testAreIndexedParentChildPropertyArrayParams() {
    final var result = oClass.areIndexed(session, "fOne, fNine");

    assertFalse(result);
  }

  @Test(
      dependsOnMethods = {
          "createCompositeIndexTestWithListener",
          "createCompositeIndexTestWithoutListener",
          "testCreateOnePropertyIndexTest",
          "testCreateOnePropertyEmbeddedMapIndex",
          "testCreateOnePropertyByKeyEmbeddedMapIndex",
          "testCreateOnePropertyByValueEmbeddedMapIndex",
          "testCreateOnePropertyLinkedMapIndex",
          "testCreateOnePropertyLinkMapByKeyIndex",
          "testCreateOnePropertyLinkMapByValueIndex",
          "testCreateCompositeEmbeddedMapIndex",
          "testCreateCompositeEmbeddedMapByKeyIndex",
          "testCreateCompositeEmbeddedMapByValueIndex",
          "testCreateCompositeLinkMapByValueIndex",
          "testCreateCompositeEmbeddedSetIndex",
          "testCreateCompositeEmbeddedListIndex"
      })
  public void testGetClassInvolvedIndexesOnePropertyArrayParams() {
    final var result = oClass.getClassInvolvedIndexesInternal(session, "fOne");

    assertEquals(result.size(), 1);

    assertTrue(containsIndex(result, "ClassIndexTestPropertyOne"));
  }

  @Test(
      dependsOnMethods = {
          "createCompositeIndexTestWithListener",
          "createCompositeIndexTestWithoutListener",
          "testCreateOnePropertyIndexTest",
          "testCreateOnePropertyEmbeddedMapIndex",
          "testCreateOnePropertyByKeyEmbeddedMapIndex",
          "testCreateOnePropertyByValueEmbeddedMapIndex",
          "testCreateOnePropertyLinkedMapIndex",
          "testCreateOnePropertyLinkMapByKeyIndex",
          "testCreateOnePropertyLinkMapByValueIndex",
          "testCreateCompositeEmbeddedMapIndex",
          "testCreateCompositeEmbeddedMapByKeyIndex",
          "testCreateCompositeEmbeddedMapByValueIndex",
          "testCreateCompositeLinkMapByValueIndex",
          "testCreateCompositeEmbeddedSetIndex",
          "testCreateCompositeEmbeddedListIndex"
      })
  public void testGetClassInvolvedIndexesTwoPropertiesArrayParams() {
    final var result = oClass.getClassInvolvedIndexesInternal(session, "fTwo", "fOne");
    assertEquals(result.size(), 1);

    assertTrue(containsIndex(result, "ClassIndexTestCompositeOne"));
  }

  @Test(
      dependsOnMethods = {
          "createCompositeIndexTestWithListener",
          "createCompositeIndexTestWithoutListener",
          "testCreateOnePropertyIndexTest",
          "testCreateOnePropertyEmbeddedMapIndex",
          "testCreateOnePropertyByKeyEmbeddedMapIndex",
          "testCreateOnePropertyByValueEmbeddedMapIndex",
          "testCreateOnePropertyLinkedMapIndex",
          "testCreateOnePropertyLinkMapByKeyIndex",
          "testCreateOnePropertyLinkMapByValueIndex",
          "testCreateCompositeEmbeddedMapIndex",
          "testCreateCompositeEmbeddedMapByKeyIndex",
          "testCreateCompositeEmbeddedMapByValueIndex",
          "testCreateCompositeLinkMapByValueIndex",
          "testCreateCompositeEmbeddedSetIndex",
          "testCreateCompositeEmbeddedListIndex"
      })
  public void testGetClassInvolvedIndexesThreePropertiesArrayParams() {
    final var result = oClass.getClassInvolvedIndexesInternal(session, "fTwo", "fOne",
        "fThree");

    assertEquals(result.size(), 1);
    assertEquals(result.iterator().next().getName(), "ClassIndexTestCompositeTwo");
  }

  @Test(
      dependsOnMethods = {
          "createCompositeIndexTestWithListener",
          "createCompositeIndexTestWithoutListener",
          "testCreateOnePropertyIndexTest",
          "testCreateOnePropertyEmbeddedMapIndex",
          "testCreateOnePropertyByKeyEmbeddedMapIndex",
          "testCreateOnePropertyByValueEmbeddedMapIndex",
          "testCreateOnePropertyLinkedMapIndex",
          "testCreateOnePropertyLinkMapByKeyIndex",
          "testCreateOnePropertyLinkMapByValueIndex",
          "testCreateCompositeEmbeddedMapIndex",
          "testCreateCompositeEmbeddedMapByKeyIndex",
          "testCreateCompositeEmbeddedMapByValueIndex",
          "testCreateCompositeLinkMapByValueIndex",
          "testCreateCompositeEmbeddedSetIndex",
          "testCreateCompositeEmbeddedListIndex"
      })
  public void testGetClassInvolvedIndexesNotInvolvedPropertiesArrayParams() {
    final var result = oClass.getClassInvolvedIndexesInternal(session, "fTwo", "fFour");

    assertEquals(result.size(), 0);
  }

  @Test(
      dependsOnMethods = {
          "createCompositeIndexTestWithListener",
          "createCompositeIndexTestWithoutListener",
          "testCreateOnePropertyIndexTest",
          "testCreateOnePropertyEmbeddedMapIndex",
          "testCreateOnePropertyByKeyEmbeddedMapIndex",
          "testCreateOnePropertyByValueEmbeddedMapIndex",
          "testCreateOnePropertyLinkedMapIndex",
          "testCreateOnePropertyLinkMapByKeyIndex",
          "testCreateOnePropertyLinkMapByValueIndex",
          "testCreateCompositeEmbeddedMapIndex",
          "testCreateCompositeEmbeddedMapByKeyIndex",
          "testCreateCompositeEmbeddedMapByValueIndex",
          "testCreateCompositeLinkMapByValueIndex",
          "testCreateCompositeEmbeddedSetIndex",
          "testCreateCompositeEmbeddedListIndex"
      })
  public void testGetClassInvolvedIndexesPropertiesMorThanNeededArrayParams() {
    final var result = oClass.getClassInvolvedIndexesInternal(session, "fTwo", "fOne",
        "fThee",
        "fFour");

    assertEquals(result.size(), 0);
  }

  @Test(
      dependsOnMethods = {
          "createCompositeIndexTestWithListener",
          "createCompositeIndexTestWithoutListener",
          "testCreateOnePropertyIndexTest",
          "testCreateOnePropertyEmbeddedMapIndex",
          "testCreateOnePropertyByKeyEmbeddedMapIndex",
          "testCreateOnePropertyByValueEmbeddedMapIndex",
          "testCreateOnePropertyLinkedMapIndex",
          "testCreateOnePropertyLinkMapByKeyIndex",
          "testCreateOnePropertyLinkMapByValueIndex",
          "testCreateCompositeEmbeddedMapIndex",
          "testCreateCompositeEmbeddedMapByKeyIndex",
          "testCreateCompositeEmbeddedMapByValueIndex",
          "testCreateCompositeLinkMapByValueIndex",
          "testCreateCompositeEmbeddedSetIndex",
          "testCreateCompositeEmbeddedListIndex"
      })
  public void testGetInvolvedIndexesPropertiesMorThanNeeded() {
    final var result =
        oClass.getClassInvolvedIndexesInternal(session,
            Arrays.asList("fTwo", "fOne", "fThee", "fFour"));

    assertEquals(result.size(), 0);
  }

  @Test(
      dependsOnMethods = {
          "createCompositeIndexTestWithListener",
          "createCompositeIndexTestWithoutListener",
          "testCreateOnePropertyIndexTest",
          "testCreateOnePropertyEmbeddedMapIndex",
          "testCreateOnePropertyByKeyEmbeddedMapIndex",
          "testCreateOnePropertyByValueEmbeddedMapIndex",
          "testCreateOnePropertyLinkedMapIndex",
          "testCreateOnePropertyLinkMapByKeyIndex",
          "testCreateOnePropertyLinkMapByValueIndex",
          "testCreateCompositeEmbeddedMapIndex",
          "testCreateCompositeEmbeddedMapByKeyIndex",
          "testCreateCompositeEmbeddedMapByValueIndex",
          "testCreateCompositeLinkMapByValueIndex",
          "testCreateCompositeEmbeddedSetIndex",
          "testCreateCompositeEmbeddedListIndex"
      })
  public void testGetClassInvolvedIndexesOneProperty() {
    final var result = oClass.getClassInvolvedIndexesInternal(session, List.of("fOne"));

    assertEquals(result.size(), 1);

    assertTrue(containsIndex(result, "ClassIndexTestPropertyOne"));
  }

  @Test(
      dependsOnMethods = {
          "createCompositeIndexTestWithListener",
          "createCompositeIndexTestWithoutListener",
          "testCreateOnePropertyIndexTest",
          "testCreateOnePropertyEmbeddedMapIndex",
          "testCreateOnePropertyByKeyEmbeddedMapIndex",
          "testCreateOnePropertyByValueEmbeddedMapIndex",
          "testCreateOnePropertyLinkedMapIndex",
          "testCreateOnePropertyLinkMapByKeyIndex",
          "testCreateOnePropertyLinkMapByValueIndex",
          "testCreateCompositeEmbeddedMapIndex",
          "testCreateCompositeEmbeddedMapByKeyIndex",
          "testCreateCompositeEmbeddedMapByValueIndex",
          "testCreateCompositeLinkMapByValueIndex",
          "testCreateCompositeEmbeddedSetIndex",
          "testCreateCompositeEmbeddedListIndex"
      })
  public void testGetClassInvolvedIndexesTwoProperties() {
    final var result = oClass.getClassInvolvedIndexesInternal(session,
        Arrays.asList("fTwo", "fOne"));
    assertEquals(result.size(), 1);

    assertTrue(containsIndex(result, "ClassIndexTestCompositeOne"));
  }

  @Test(
      dependsOnMethods = {
          "createCompositeIndexTestWithListener",
          "createCompositeIndexTestWithoutListener",
          "testCreateOnePropertyIndexTest",
          "testCreateOnePropertyEmbeddedMapIndex",
          "testCreateOnePropertyByKeyEmbeddedMapIndex",
          "testCreateOnePropertyByValueEmbeddedMapIndex",
          "testCreateOnePropertyLinkedMapIndex",
          "testCreateOnePropertyLinkMapByKeyIndex",
          "testCreateOnePropertyLinkMapByValueIndex",
          "testCreateCompositeEmbeddedMapIndex",
          "testCreateCompositeEmbeddedMapByKeyIndex",
          "testCreateCompositeEmbeddedMapByValueIndex",
          "testCreateCompositeLinkMapByValueIndex",
          "testCreateCompositeEmbeddedSetIndex",
          "testCreateCompositeEmbeddedListIndex"
      })
  public void testGetClassInvolvedIndexesThreeProperties() {
    final var result =
        oClass.getClassInvolvedIndexesInternal(session, Arrays.asList("fTwo", "fOne", "fThree"));

    assertEquals(result.size(), 1);
    assertEquals(result.iterator().next().getName(), "ClassIndexTestCompositeTwo");
  }

  @Test(
      dependsOnMethods = {
          "createCompositeIndexTestWithListener",
          "createCompositeIndexTestWithoutListener",
          "testCreateOnePropertyIndexTest",
          "testCreateOnePropertyEmbeddedMapIndex",
          "testCreateOnePropertyByKeyEmbeddedMapIndex",
          "testCreateOnePropertyByValueEmbeddedMapIndex",
          "testCreateOnePropertyLinkedMapIndex",
          "testCreateOnePropertyLinkMapByKeyIndex",
          "testCreateOnePropertyLinkMapByValueIndex",
          "testCreateCompositeEmbeddedMapIndex",
          "testCreateCompositeEmbeddedMapByKeyIndex",
          "testCreateCompositeEmbeddedMapByValueIndex",
          "testCreateCompositeLinkMapByValueIndex",
          "testCreateCompositeEmbeddedSetIndex",
          "testCreateCompositeEmbeddedListIndex"
      })
  public void testGetClassInvolvedIndexesNotInvolvedProperties() {
    final var result = oClass.getClassInvolvedIndexesInternal(session,
        Arrays.asList("fTwo", "fFour"));

    assertEquals(result.size(), 0);
  }

  @Test(
      dependsOnMethods = {
          "createCompositeIndexTestWithListener",
          "createCompositeIndexTestWithoutListener",
          "testCreateOnePropertyIndexTest",
          "testCreateOnePropertyEmbeddedMapIndex",
          "testCreateOnePropertyByKeyEmbeddedMapIndex",
          "testCreateOnePropertyByValueEmbeddedMapIndex",
          "testCreateOnePropertyLinkedMapIndex",
          "testCreateOnePropertyLinkMapByKeyIndex",
          "testCreateOnePropertyLinkMapByValueIndex",
          "testCreateCompositeEmbeddedMapIndex",
          "testCreateCompositeEmbeddedMapByKeyIndex",
          "testCreateCompositeEmbeddedMapByValueIndex",
          "testCreateCompositeLinkMapByValueIndex",
          "testCreateCompositeEmbeddedSetIndex",
          "testCreateCompositeEmbeddedListIndex"
      })
  public void testGetClassInvolvedIndexesPropertiesMorThanNeeded() {
    final var result =
        oClass.getClassInvolvedIndexesInternal(session,
            Arrays.asList("fTwo", "fOne", "fThee", "fFour"));

    assertEquals(result.size(), 0);
  }

  @Test(
      dependsOnMethods = {
          "createCompositeIndexTestWithListener",
          "createCompositeIndexTestWithoutListener",
          "testCreateOnePropertyIndexTest",
          "testCreateOnePropertyEmbeddedMapIndex",
          "testCreateOnePropertyByKeyEmbeddedMapIndex",
          "testCreateOnePropertyByValueEmbeddedMapIndex",
          "testCreateOnePropertyLinkedMapIndex",
          "testCreateOnePropertyLinkMapByKeyIndex",
          "testCreateOnePropertyLinkMapByValueIndex",
          "testCreateCompositeEmbeddedMapIndex",
          "testCreateCompositeEmbeddedMapByKeyIndex",
          "testCreateCompositeEmbeddedMapByValueIndex",
          "testCreateCompositeLinkMapByValueIndex",
          "testCreateCompositeEmbeddedSetIndex",
          "testCreateCompositeEmbeddedListIndex"
      })
  public void testGetInvolvedIndexesOnePropertyArrayParams() {
    final var result = oClass.getInvolvedIndexesInternal(session, "fOne");

    assertEquals(result.size(), 1);

    assertTrue(containsIndex(result, "ClassIndexTestPropertyOne"));
  }

  @Test(
      dependsOnMethods = {
          "createCompositeIndexTestWithListener",
          "createCompositeIndexTestWithoutListener",
          "testCreateOnePropertyIndexTest",
          "testCreateOnePropertyEmbeddedMapIndex",
          "testCreateOnePropertyByKeyEmbeddedMapIndex",
          "testCreateOnePropertyByValueEmbeddedMapIndex",
          "testCreateOnePropertyLinkedMapIndex",
          "testCreateOnePropertyLinkMapByKeyIndex",
          "testCreateOnePropertyLinkMapByValueIndex",
          "testCreateCompositeEmbeddedMapIndex",
          "testCreateCompositeEmbeddedMapByKeyIndex",
          "testCreateCompositeEmbeddedMapByValueIndex",
          "testCreateCompositeLinkMapByValueIndex",
          "testCreateCompositeEmbeddedSetIndex",
          "testCreateCompositeEmbeddedListIndex"
      })
  public void testGetInvolvedIndexesTwoPropertiesArrayParams() {
    final var result = oClass.getInvolvedIndexesInternal(session, "fTwo", "fOne");
    assertEquals(result.size(), 1);

    assertTrue(containsIndex(result, "ClassIndexTestCompositeOne"));
  }

  @Test(
      dependsOnMethods = {
          "createCompositeIndexTestWithListener",
          "createCompositeIndexTestWithoutListener",
          "testCreateOnePropertyIndexTest",
          "testCreateOnePropertyEmbeddedMapIndex",
          "testCreateOnePropertyByKeyEmbeddedMapIndex",
          "testCreateOnePropertyByValueEmbeddedMapIndex",
          "testCreateOnePropertyLinkedMapIndex",
          "testCreateOnePropertyLinkMapByKeyIndex",
          "testCreateOnePropertyLinkMapByValueIndex",
          "testCreateCompositeEmbeddedMapIndex",
          "testCreateCompositeEmbeddedMapByKeyIndex",
          "testCreateCompositeEmbeddedMapByValueIndex",
          "testCreateCompositeLinkMapByValueIndex",
          "testCreateCompositeEmbeddedSetIndex",
          "testCreateCompositeEmbeddedListIndex"
      })
  public void testGetInvolvedIndexesThreePropertiesArrayParams() {
    final var result = oClass.getInvolvedIndexesInternal(session, "fTwo", "fOne", "fThree");

    assertEquals(result.size(), 1);
    assertEquals(result.iterator().next().getName(), "ClassIndexTestCompositeTwo");
  }

  @Test(
      dependsOnMethods = {
          "createCompositeIndexTestWithListener",
          "createCompositeIndexTestWithoutListener",
          "testCreateOnePropertyIndexTest",
          "testCreateOnePropertyEmbeddedMapIndex",
          "testCreateOnePropertyByKeyEmbeddedMapIndex",
          "testCreateOnePropertyByValueEmbeddedMapIndex",
          "testCreateOnePropertyLinkedMapIndex",
          "testCreateOnePropertyLinkMapByKeyIndex",
          "testCreateOnePropertyLinkMapByValueIndex",
          "testCreateCompositeEmbeddedMapIndex",
          "testCreateCompositeEmbeddedMapByKeyIndex",
          "testCreateCompositeEmbeddedMapByValueIndex",
          "testCreateCompositeLinkMapByValueIndex",
          "testCreateCompositeEmbeddedSetIndex",
          "testCreateCompositeEmbeddedListIndex"
      })
  public void testGetInvolvedIndexesNotInvolvedPropertiesArrayParams() {
    final var result = oClass.getInvolvedIndexesInternal(session, "fTwo", "fFour");

    assertEquals(result.size(), 0);
  }

  @Test(
      dependsOnMethods = {
          "createCompositeIndexTestWithListener",
          "createCompositeIndexTestWithoutListener",
          "testCreateOnePropertyIndexTest",
          "testCreateOnePropertyEmbeddedMapIndex",
          "testCreateOnePropertyByKeyEmbeddedMapIndex",
          "testCreateOnePropertyByValueEmbeddedMapIndex",
          "testCreateOnePropertyLinkedMapIndex",
          "testCreateOnePropertyLinkMapByKeyIndex",
          "testCreateOnePropertyLinkMapByValueIndex",
          "testCreateCompositeEmbeddedMapIndex",
          "testCreateCompositeEmbeddedMapByKeyIndex",
          "testCreateCompositeEmbeddedMapByValueIndex",
          "testCreateCompositeLinkMapByValueIndex",
          "testCreateCompositeEmbeddedSetIndex",
          "testCreateCompositeEmbeddedListIndex"
      })
  public void testGetParentInvolvedIndexesArrayParams() {
    final var result = oClass.getInvolvedIndexesInternal(session, "fNine");

    assertEquals(result.size(), 1);
    assertEquals(result.iterator().next().getName(), "ClassIndexTestParentPropertyNine");
  }

  @Test(
      dependsOnMethods = {
          "createCompositeIndexTestWithListener",
          "createCompositeIndexTestWithoutListener",
          "testCreateOnePropertyIndexTest",
          "testCreateOnePropertyEmbeddedMapIndex",
          "testCreateOnePropertyByKeyEmbeddedMapIndex",
          "testCreateOnePropertyByValueEmbeddedMapIndex",
          "testCreateOnePropertyLinkedMapIndex",
          "testCreateOnePropertyLinkMapByKeyIndex",
          "testCreateOnePropertyLinkMapByValueIndex",
          "testCreateCompositeEmbeddedMapIndex",
          "testCreateCompositeEmbeddedMapByKeyIndex",
          "testCreateCompositeEmbeddedMapByValueIndex",
          "testCreateCompositeLinkMapByValueIndex",
          "testCreateCompositeEmbeddedSetIndex",
          "testCreateCompositeEmbeddedListIndex"
      })
  public void testGetParentChildInvolvedIndexesArrayParams() {
    final var result = oClass.getInvolvedIndexesInternal(session, "fOne", "fNine");

    assertEquals(result.size(), 0);
  }

  @Test(
      dependsOnMethods = {
          "createCompositeIndexTestWithListener",
          "createCompositeIndexTestWithoutListener",
          "testCreateOnePropertyIndexTest",
          "testCreateOnePropertyEmbeddedMapIndex",
          "testCreateOnePropertyByKeyEmbeddedMapIndex",
          "testCreateOnePropertyByValueEmbeddedMapIndex",
          "testCreateOnePropertyLinkedMapIndex",
          "testCreateOnePropertyLinkMapByKeyIndex",
          "testCreateOnePropertyLinkMapByValueIndex",
          "testCreateCompositeEmbeddedMapIndex",
          "testCreateCompositeEmbeddedMapByKeyIndex",
          "testCreateCompositeEmbeddedMapByValueIndex",
          "testCreateCompositeLinkMapByValueIndex",
          "testCreateCompositeEmbeddedSetIndex",
          "testCreateCompositeEmbeddedListIndex"
      })
  public void testGetInvolvedIndexesOneProperty() {
    final var result = oClass.getInvolvedIndexesInternal(session, List.of("fOne"));

    assertEquals(result.size(), 1);

    assertTrue(containsIndex(result, "ClassIndexTestPropertyOne"));
  }

  @Test(
      dependsOnMethods = {
          "createCompositeIndexTestWithListener",
          "createCompositeIndexTestWithoutListener",
          "testCreateOnePropertyIndexTest",
          "testCreateOnePropertyEmbeddedMapIndex",
          "testCreateOnePropertyByKeyEmbeddedMapIndex",
          "testCreateOnePropertyByValueEmbeddedMapIndex",
          "testCreateOnePropertyLinkedMapIndex",
          "testCreateOnePropertyLinkMapByKeyIndex",
          "testCreateOnePropertyLinkMapByValueIndex",
          "testCreateCompositeEmbeddedMapIndex",
          "testCreateCompositeEmbeddedMapByKeyIndex",
          "testCreateCompositeEmbeddedMapByValueIndex",
          "testCreateCompositeLinkMapByValueIndex",
          "testCreateCompositeEmbeddedSetIndex",
          "testCreateCompositeEmbeddedListIndex"
      })
  public void testGetInvolvedIndexesTwoProperties() {
    final var result = oClass.getInvolvedIndexesInternal(session,
        Arrays.asList("fTwo", "fOne"));
    assertEquals(result.size(), 1);

    assertTrue(containsIndex(result, "ClassIndexTestCompositeOne"));
  }

  @Test(
      dependsOnMethods = {
          "createCompositeIndexTestWithListener",
          "createCompositeIndexTestWithoutListener",
          "testCreateOnePropertyIndexTest",
          "testCreateOnePropertyEmbeddedMapIndex",
          "testCreateOnePropertyByKeyEmbeddedMapIndex",
          "testCreateOnePropertyByValueEmbeddedMapIndex",
          "testCreateOnePropertyLinkedMapIndex",
          "testCreateOnePropertyLinkMapByKeyIndex",
          "testCreateOnePropertyLinkMapByValueIndex",
          "testCreateCompositeEmbeddedMapIndex",
          "testCreateCompositeEmbeddedMapByKeyIndex",
          "testCreateCompositeEmbeddedMapByValueIndex",
          "testCreateCompositeLinkMapByValueIndex",
          "testCreateCompositeEmbeddedSetIndex",
          "testCreateCompositeEmbeddedListIndex"
      })
  public void testGetInvolvedIndexesThreeProperties() {
    final var result = oClass.getInvolvedIndexesInternal(session,
        Arrays.asList("fTwo", "fOne", "fThree"));

    assertEquals(result.size(), 1);
    assertEquals(result.iterator().next().getName(), "ClassIndexTestCompositeTwo");
  }

  @Test(
      dependsOnMethods = {
          "createCompositeIndexTestWithListener",
          "createCompositeIndexTestWithoutListener",
          "testCreateOnePropertyIndexTest",
          "testCreateOnePropertyEmbeddedMapIndex",
          "testCreateOnePropertyByKeyEmbeddedMapIndex",
          "testCreateOnePropertyByValueEmbeddedMapIndex",
          "testCreateOnePropertyLinkedMapIndex",
          "testCreateOnePropertyLinkMapByKeyIndex",
          "testCreateOnePropertyLinkMapByValueIndex",
          "testCreateCompositeEmbeddedMapIndex",
          "testCreateCompositeEmbeddedMapByKeyIndex",
          "testCreateCompositeEmbeddedMapByValueIndex",
          "testCreateCompositeLinkMapByValueIndex",
          "testCreateCompositeEmbeddedSetIndex",
          "testCreateCompositeEmbeddedListIndex"
      })
  public void testGetInvolvedIndexesNotInvolvedProperties() {
    final var result = oClass.getInvolvedIndexesInternal(session,
        Arrays.asList("fTwo", "fFour"));

    assertEquals(result.size(), 0);
  }

  @Test(
      dependsOnMethods = {
          "createCompositeIndexTestWithListener",
          "createCompositeIndexTestWithoutListener",
          "testCreateOnePropertyIndexTest",
          "testCreateOnePropertyEmbeddedMapIndex",
          "testCreateOnePropertyByKeyEmbeddedMapIndex",
          "testCreateOnePropertyByValueEmbeddedMapIndex",
          "testCreateOnePropertyLinkedMapIndex",
          "testCreateOnePropertyLinkMapByKeyIndex",
          "testCreateOnePropertyLinkMapByValueIndex",
          "testCreateCompositeEmbeddedMapIndex",
          "testCreateCompositeEmbeddedMapByKeyIndex",
          "testCreateCompositeEmbeddedMapByValueIndex",
          "testCreateCompositeLinkMapByValueIndex",
          "testCreateCompositeEmbeddedSetIndex",
          "testCreateCompositeEmbeddedListIndex"
      })
  public void testGetParentInvolvedIndexes() {
    final var result = oClass.getInvolvedIndexesInternal(session, List.of("fNine"));

    assertEquals(result.size(), 1);
    assertEquals(result.iterator().next().getName(), "ClassIndexTestParentPropertyNine");
  }

  @Test(
      dependsOnMethods = {
          "createCompositeIndexTestWithListener",
          "createCompositeIndexTestWithoutListener",
          "testCreateOnePropertyIndexTest",
          "testCreateOnePropertyEmbeddedMapIndex",
          "testCreateOnePropertyByKeyEmbeddedMapIndex",
          "testCreateOnePropertyByValueEmbeddedMapIndex",
          "testCreateOnePropertyLinkedMapIndex",
          "testCreateOnePropertyLinkMapByKeyIndex",
          "testCreateOnePropertyLinkMapByValueIndex",
          "testCreateCompositeEmbeddedMapIndex",
          "testCreateCompositeEmbeddedMapByKeyIndex",
          "testCreateCompositeEmbeddedMapByValueIndex",
          "testCreateCompositeLinkMapByValueIndex",
          "testCreateCompositeEmbeddedSetIndex",
          "testCreateCompositeEmbeddedListIndex"
      })
  public void testGetParentChildInvolvedIndexes() {
    final var result = oClass.getInvolvedIndexesInternal(session,
        Arrays.asList("fOne", "fNine"));

    assertEquals(result.size(), 0);
  }

  @Test(
      dependsOnMethods = {
          "createCompositeIndexTestWithListener",
          "createCompositeIndexTestWithoutListener",
          "testCreateOnePropertyIndexTest",
          "testCreateOnePropertyEmbeddedMapIndex",
          "testCreateOnePropertyByKeyEmbeddedMapIndex",
          "testCreateOnePropertyByValueEmbeddedMapIndex",
          "testCreateOnePropertyLinkedMapIndex",
          "testCreateOnePropertyLinkMapByKeyIndex",
          "testCreateOnePropertyLinkMapByValueIndex",
          "testCreateCompositeEmbeddedMapIndex",
          "testCreateCompositeEmbeddedMapByKeyIndex",
          "testCreateCompositeEmbeddedMapByValueIndex",
          "testCreateCompositeLinkMapByValueIndex",
          "testCreateCompositeEmbeddedSetIndex",
          "testCreateCompositeEmbeddedListIndex",
          "testCreateCompositeLinkListIndex",
          "testCreateCompositeRidBagIndex"
      })
  public void testGetClassIndexes() {
    final var indexes = oClass.getClassIndexesInternal(session);
    final Set<IndexDefinition> expectedIndexDefinitions = new HashSet<>();

    final var compositeIndexOne =
        new CompositeIndexDefinition("ClassIndexTestClass");

    compositeIndexOne.addIndex(
        new PropertyIndexDefinition("ClassIndexTestClass", "fOne", PropertyType.INTEGER));
    compositeIndexOne.addIndex(
        new PropertyIndexDefinition("ClassIndexTestClass", "fTwo", PropertyType.STRING));
    expectedIndexDefinitions.add(compositeIndexOne);

    final var compositeIndexTwo =
        new CompositeIndexDefinition("ClassIndexTestClass");

    compositeIndexTwo.addIndex(
        new PropertyIndexDefinition("ClassIndexTestClass", "fOne", PropertyType.INTEGER));
    compositeIndexTwo.addIndex(
        new PropertyIndexDefinition("ClassIndexTestClass", "fTwo", PropertyType.STRING));
    compositeIndexTwo.addIndex(
        new PropertyIndexDefinition("ClassIndexTestClass", "fThree", PropertyType.BOOLEAN));
    expectedIndexDefinitions.add(compositeIndexTwo);

    final var compositeIndexThree =
        new CompositeIndexDefinition("ClassIndexTestClass");
    compositeIndexThree.addIndex(
        new PropertyIndexDefinition("ClassIndexTestClass", "fEight", PropertyType.INTEGER));
    compositeIndexThree.addIndex(
        new PropertyMapIndexDefinition(
            "ClassIndexTestClass",
            "fEmbeddedMap",
            PropertyType.STRING,
            PropertyMapIndexDefinition.INDEX_BY.KEY));
    expectedIndexDefinitions.add(compositeIndexThree);

    final var compositeIndexFour =
        new CompositeIndexDefinition("ClassIndexTestClass");
    compositeIndexFour.addIndex(
        new PropertyIndexDefinition("ClassIndexTestClass", "fTen", PropertyType.INTEGER));
    compositeIndexFour.addIndex(
        new PropertyMapIndexDefinition(
            "ClassIndexTestClass",
            "fEmbeddedMap",
            PropertyType.INTEGER,
            PropertyMapIndexDefinition.INDEX_BY.VALUE));
    expectedIndexDefinitions.add(compositeIndexFour);

    final var compositeIndexFive =
        new CompositeIndexDefinition("ClassIndexTestClass");
    compositeIndexFive.addIndex(
        new PropertyIndexDefinition("ClassIndexTestClass", "fEleven", PropertyType.INTEGER));
    compositeIndexFive.addIndex(
        new PropertyMapIndexDefinition(
            "ClassIndexTestClass",
            "fLinkMap",
            PropertyType.LINK,
            PropertyMapIndexDefinition.INDEX_BY.VALUE));
    expectedIndexDefinitions.add(compositeIndexFive);

    final var compositeIndexSix =
        new CompositeIndexDefinition("ClassIndexTestClass");
    compositeIndexSix.addIndex(
        new PropertyIndexDefinition("ClassIndexTestClass", "fTwelve", PropertyType.INTEGER));
    compositeIndexSix.addIndex(
        new PropertyListIndexDefinition("ClassIndexTestClass", "fEmbeddedSet",
            PropertyType.INTEGER));
    expectedIndexDefinitions.add(compositeIndexSix);

    final var compositeIndexSeven =
        new CompositeIndexDefinition("ClassIndexTestClass");
    compositeIndexSeven.addIndex(
        new PropertyIndexDefinition("ClassIndexTestClass", "fThirteen", PropertyType.INTEGER));
    compositeIndexSeven.addIndex(
        new PropertyListIndexDefinition("ClassIndexTestClass", "fEmbeddedList",
            PropertyType.INTEGER));
    expectedIndexDefinitions.add(compositeIndexSeven);

    final var compositeIndexEight =
        new CompositeIndexDefinition("ClassIndexTestClass");
    compositeIndexEight.addIndex(
        new PropertyIndexDefinition("ClassIndexTestClass", "fFourteen", PropertyType.INTEGER));
    compositeIndexEight.addIndex(
        new PropertyListIndexDefinition("ClassIndexTestClass", "fEmbeddedList", PropertyType.LINK));
    expectedIndexDefinitions.add(compositeIndexEight);

    final var compositeIndexNine =
        new CompositeIndexDefinition("ClassIndexTestClass");
    compositeIndexNine.addIndex(
        new PropertyIndexDefinition("ClassIndexTestClass", "fFifteen", PropertyType.INTEGER));
    compositeIndexNine.addIndex(
        new PropertyMapIndexDefinition(
            "ClassIndexTestClass",
            "fEmbeddedMap",
            PropertyType.STRING,
            PropertyMapIndexDefinition.INDEX_BY.KEY));
    expectedIndexDefinitions.add(compositeIndexNine);

    final var compositeIndexTen =
        new CompositeIndexDefinition("ClassIndexTestClass");
    compositeIndexTen.addIndex(
        new PropertyIndexDefinition("ClassIndexTestClass", "fFourteen", PropertyType.INTEGER));
    compositeIndexTen.addIndex(
        new PropertyListIndexDefinition("ClassIndexTestClass", "fLinkList", PropertyType.LINK));
    expectedIndexDefinitions.add(compositeIndexTen);

    final var compositeIndexEleven =
        new CompositeIndexDefinition("ClassIndexTestClass");
    compositeIndexEleven.addIndex(
        new PropertyIndexDefinition("ClassIndexTestClass", "fFourteen", PropertyType.INTEGER));
    compositeIndexEleven.addIndex(
        new PropertyRidBagIndexDefinition("ClassIndexTestClass", "fRidBag"));
    expectedIndexDefinitions.add(compositeIndexEleven);

    final var propertyIndex =
        new PropertyIndexDefinition("ClassIndexTestClass", "fOne", PropertyType.INTEGER);
    expectedIndexDefinitions.add(propertyIndex);

    final var propertyMapIndexDefinition =
        new PropertyMapIndexDefinition(
            "ClassIndexTestClass",
            "fEmbeddedMap",
            PropertyType.STRING,
            PropertyMapIndexDefinition.INDEX_BY.KEY);
    expectedIndexDefinitions.add(propertyMapIndexDefinition);

    final var propertyMapByValueIndexDefinition =
        new PropertyMapIndexDefinition(
            "ClassIndexTestClass",
            "fEmbeddedMap",
            PropertyType.INTEGER,
            PropertyMapIndexDefinition.INDEX_BY.VALUE);
    expectedIndexDefinitions.add(propertyMapByValueIndexDefinition);

    final var propertyLinkMapByKeyIndexDefinition =
        new PropertyMapIndexDefinition(
            "ClassIndexTestClass",
            "fLinkMap",
            PropertyType.STRING,
            PropertyMapIndexDefinition.INDEX_BY.KEY);
    expectedIndexDefinitions.add(propertyLinkMapByKeyIndexDefinition);

    final var propertyLinkMapByValueIndexDefinition =
        new PropertyMapIndexDefinition(
            "ClassIndexTestClass",
            "fLinkMap",
            PropertyType.LINK,
            PropertyMapIndexDefinition.INDEX_BY.VALUE);
    expectedIndexDefinitions.add(propertyLinkMapByValueIndexDefinition);

    assertEquals(indexes.size(), 17);

    for (final var index : indexes) {
      assertTrue(expectedIndexDefinitions.contains(index.getDefinition()));
    }
  }

  @Test(
      dependsOnMethods = {
          "createCompositeIndexTestWithListener",
          "createCompositeIndexTestWithoutListener",
          "testCreateOnePropertyIndexTest",
          "createParentPropertyIndex",
          "testCreateOnePropertyEmbeddedMapIndex",
          "testCreateOnePropertyByKeyEmbeddedMapIndex",
          "testCreateOnePropertyByValueEmbeddedMapIndex",
          "testCreateOnePropertyLinkedMapIndex",
          "testCreateOnePropertyLinkMapByKeyIndex",
          "testCreateOnePropertyLinkMapByValueIndex",
          "testCreateCompositeEmbeddedMapIndex",
          "testCreateCompositeEmbeddedMapByKeyIndex",
          "testCreateCompositeEmbeddedMapByValueIndex",
          "testCreateCompositeLinkMapByValueIndex",
          "testCreateCompositeEmbeddedSetIndex",
          "testCreateCompositeEmbeddedListIndex",
          "testCreateCompositeLinkListIndex",
          "testCreateCompositeRidBagIndex"
      })
  public void testGetIndexes() {
    final var indexes = oClass.getIndexesInternal(session);
    final Set<IndexDefinition> expectedIndexDefinitions = new HashSet<>();

    final var compositeIndexOne =
        new CompositeIndexDefinition("ClassIndexTestClass");

    compositeIndexOne.addIndex(
        new PropertyIndexDefinition("ClassIndexTestClass", "fOne", PropertyType.INTEGER));
    compositeIndexOne.addIndex(
        new PropertyIndexDefinition("ClassIndexTestClass", "fTwo", PropertyType.STRING));
    expectedIndexDefinitions.add(compositeIndexOne);

    final var compositeIndexTwo =
        new CompositeIndexDefinition("ClassIndexTestClass");

    compositeIndexTwo.addIndex(
        new PropertyIndexDefinition("ClassIndexTestClass", "fOne", PropertyType.INTEGER));
    compositeIndexTwo.addIndex(
        new PropertyIndexDefinition("ClassIndexTestClass", "fTwo", PropertyType.STRING));
    compositeIndexTwo.addIndex(
        new PropertyIndexDefinition("ClassIndexTestClass", "fThree", PropertyType.BOOLEAN));
    expectedIndexDefinitions.add(compositeIndexTwo);

    final var compositeIndexThree =
        new CompositeIndexDefinition("ClassIndexTestClass");
    compositeIndexThree.addIndex(
        new PropertyIndexDefinition("ClassIndexTestClass", "fEight", PropertyType.INTEGER));
    compositeIndexThree.addIndex(
        new PropertyMapIndexDefinition(
            "ClassIndexTestClass",
            "fEmbeddedMap",
            PropertyType.STRING,
            PropertyMapIndexDefinition.INDEX_BY.KEY));
    expectedIndexDefinitions.add(compositeIndexThree);

    final var compositeIndexFour =
        new CompositeIndexDefinition("ClassIndexTestClass");
    compositeIndexFour.addIndex(
        new PropertyIndexDefinition("ClassIndexTestClass", "fTen", PropertyType.INTEGER));
    compositeIndexFour.addIndex(
        new PropertyMapIndexDefinition(
            "ClassIndexTestClass",
            "fEmbeddedMap",
            PropertyType.INTEGER,
            PropertyMapIndexDefinition.INDEX_BY.VALUE));
    expectedIndexDefinitions.add(compositeIndexFour);

    final var compositeIndexFive =
        new CompositeIndexDefinition("ClassIndexTestClass");
    compositeIndexFive.addIndex(
        new PropertyIndexDefinition("ClassIndexTestClass", "fEleven", PropertyType.INTEGER));
    compositeIndexFive.addIndex(
        new PropertyMapIndexDefinition(
            "ClassIndexTestClass",
            "fLinkMap",
            PropertyType.LINK,
            PropertyMapIndexDefinition.INDEX_BY.VALUE));
    expectedIndexDefinitions.add(compositeIndexFive);

    final var compositeIndexSix =
        new CompositeIndexDefinition("ClassIndexTestClass");
    compositeIndexSix.addIndex(
        new PropertyIndexDefinition("ClassIndexTestClass", "fTwelve", PropertyType.INTEGER));
    compositeIndexSix.addIndex(
        new PropertyListIndexDefinition("ClassIndexTestClass", "fEmbeddedSet",
            PropertyType.INTEGER));
    expectedIndexDefinitions.add(compositeIndexSix);

    final var compositeIndexSeven =
        new CompositeIndexDefinition("ClassIndexTestClass");
    compositeIndexSeven.addIndex(
        new PropertyIndexDefinition("ClassIndexTestClass", "fThirteen", PropertyType.INTEGER));
    compositeIndexSeven.addIndex(
        new PropertyListIndexDefinition("ClassIndexTestClass", "fEmbeddedList",
            PropertyType.INTEGER));
    expectedIndexDefinitions.add(compositeIndexSeven);

    final var compositeIndexEight =
        new CompositeIndexDefinition("ClassIndexTestClass");
    compositeIndexEight.addIndex(
        new PropertyIndexDefinition("ClassIndexTestClass", "fFourteen", PropertyType.INTEGER));
    compositeIndexEight.addIndex(
        new PropertyListIndexDefinition("ClassIndexTestClass", "fEmbeddedList", PropertyType.LINK));
    expectedIndexDefinitions.add(compositeIndexEight);

    final var compositeIndexNine =
        new CompositeIndexDefinition("ClassIndexTestClass");
    compositeIndexNine.addIndex(
        new PropertyIndexDefinition("ClassIndexTestClass", "fFifteen", PropertyType.INTEGER));
    compositeIndexNine.addIndex(
        new PropertyMapIndexDefinition(
            "ClassIndexTestClass",
            "fEmbeddedMap",
            PropertyType.STRING,
            PropertyMapIndexDefinition.INDEX_BY.KEY));
    expectedIndexDefinitions.add(compositeIndexNine);

    final var compositeIndexTen =
        new CompositeIndexDefinition("ClassIndexTestClass");
    compositeIndexTen.addIndex(
        new PropertyIndexDefinition("ClassIndexTestClass", "fFourteen", PropertyType.INTEGER));
    compositeIndexTen.addIndex(
        new PropertyListIndexDefinition("ClassIndexTestClass", "fLinkList", PropertyType.LINK));
    expectedIndexDefinitions.add(compositeIndexTen);

    final var compositeIndexEleven =
        new CompositeIndexDefinition("ClassIndexTestClass");
    compositeIndexEleven.addIndex(
        new PropertyIndexDefinition("ClassIndexTestClass", "fFourteen", PropertyType.INTEGER));
    compositeIndexEleven.addIndex(
        new PropertyRidBagIndexDefinition("ClassIndexTestClass", "fRidBag"));
    expectedIndexDefinitions.add(compositeIndexEleven);

    final var propertyIndex =
        new PropertyIndexDefinition("ClassIndexTestClass", "fOne", PropertyType.INTEGER);
    expectedIndexDefinitions.add(propertyIndex);

    final var parentPropertyIndex =
        new PropertyIndexDefinition("ClassIndexTestSuperClass", "fNine", PropertyType.INTEGER);
    expectedIndexDefinitions.add(parentPropertyIndex);

    final var propertyMapIndexDefinition =
        new PropertyMapIndexDefinition(
            "ClassIndexTestClass",
            "fEmbeddedMap",
            PropertyType.STRING,
            PropertyMapIndexDefinition.INDEX_BY.KEY);
    expectedIndexDefinitions.add(propertyMapIndexDefinition);

    final var propertyMapByValueIndexDefinition =
        new PropertyMapIndexDefinition(
            "ClassIndexTestClass",
            "fEmbeddedMap",
            PropertyType.INTEGER,
            PropertyMapIndexDefinition.INDEX_BY.VALUE);
    expectedIndexDefinitions.add(propertyMapByValueIndexDefinition);

    final var propertyLinkMapByKeyIndexDefinition =
        new PropertyMapIndexDefinition(
            "ClassIndexTestClass",
            "fLinkMap",
            PropertyType.STRING,
            PropertyMapIndexDefinition.INDEX_BY.KEY);
    expectedIndexDefinitions.add(propertyLinkMapByKeyIndexDefinition);

    final var propertyLinkMapByValueIndexDefinition =
        new PropertyMapIndexDefinition(
            "ClassIndexTestClass",
            "fLinkMap",
            PropertyType.LINK,
            PropertyMapIndexDefinition.INDEX_BY.VALUE);
    expectedIndexDefinitions.add(propertyLinkMapByValueIndexDefinition);

    assertEquals(indexes.size(), 18);

    for (final var index : indexes) {
      assertTrue(expectedIndexDefinitions.contains(index.getDefinition()));
    }
  }

  @Test
  public void testGetIndexesWithoutParent() {
    final var inClass = (SchemaClassInternal) session.getMetadata().getSchema()
        .createClass("ClassIndexInTest");
    inClass.createProperty(session, "fOne", PropertyType.INTEGER);

    var indexName = "ClassIndexInTestPropertyOne";
    inClass.createIndex(session,
        indexName,
        SchemaClass.INDEX_TYPE.UNIQUE.toString(),
        null,
        Map.of("ignoreNullValues", true), new String[]{"fOne"});

    assertEquals(inClass.getClassIndex(session, indexName).getName(),
        indexName);

    final var indexes = inClass.getIndexesInternal(session);
    final var propertyIndexDefinition =
        new PropertyIndexDefinition("ClassIndexInTest", "fOne", PropertyType.INTEGER);

    assertEquals(indexes.size(), 1);

    assertEquals(propertyIndexDefinition, indexes.iterator().next().getDefinition());
  }

  @Test(expectedExceptions = IndexException.class)
  public void testCreateIndexEmptyFields() {
    oClass.createIndex(session, "ClassIndexTestCompositeEmpty", SchemaClass.INDEX_TYPE.UNIQUE);
  }

  @Test(expectedExceptions = IndexException.class)
  public void testCreateIndexAbsentFields() {
    oClass.createIndex(session,
        "ClassIndexTestCompositeFieldAbsent",
        SchemaClass.INDEX_TYPE.UNIQUE.toString(),
        null,
        Map.of("ignoreNullValues", true), new String[]{"fFive"});
  }

  @Test(dependsOnMethods = "testGetInvolvedIndexesOnePropertyArrayParams")
  public void testCreateNotUniqueIndex() {
    oClass.createIndex(session, "ClassIndexTestNotUniqueIndex",
        SchemaClass.INDEX_TYPE.NOTUNIQUE,
        "fOne");

    assertEquals(oClass.getClassIndex(session, "ClassIndexTestNotUniqueIndex").getName(),
        "ClassIndexTestNotUniqueIndex");
    var index = session.getIndex("ClassIndexTestNotUniqueIndex");
    assertEquals(index.getType(), SchemaClass.INDEX_TYPE.NOTUNIQUE.toString());
  }

  @Test
  public void testCreateMapWithoutLinkedType() {
    try {
      oClass.createIndex(session,
          "ClassIndexMapWithoutLinkedTypeIndex",
          SchemaClass.INDEX_TYPE.NOTUNIQUE, "fEmbeddedMapWithoutLinkedType by value");
      fail();
    } catch (IndexException e) {
      assertTrue(
          e.getMessage()
              .contains(
                  "Linked type was not provided. You should provide linked type for embedded"
                      + " collections that are going to be indexed."));
    }
  }

  public void createParentPropertyIndex() {
    oSuperClass.createIndex(session,
        "ClassIndexTestParentPropertyNine",
        SchemaClass.INDEX_TYPE.UNIQUE.toString(),
        null,
        Map.of("ignoreNullValues", true), new String[]{"fNine"});

    assertEquals(
        oSuperClass.getClassIndex(session, "ClassIndexTestParentPropertyNine").getName(),
        "ClassIndexTestParentPropertyNine");
  }

  private static boolean containsIndex(
      final Collection<? extends Index> classIndexes, final String indexName) {
    for (final var index : classIndexes) {
      if (index.getName().equals(indexName)) {
        return true;
      }
    }
    return false;
  }

  @Test
  public void testDropProperty() throws Exception {
    oClass.createProperty(session, "fFive", PropertyType.INTEGER);

    oClass.dropProperty(session, "fFive");

    assertNull(oClass.getProperty(session, "fFive"));
  }
}
