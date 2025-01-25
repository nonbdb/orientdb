package com.jetbrains.youtrack.db.internal.core.record.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.jetbrains.youtrack.db.api.YouTrackDB;
import com.jetbrains.youtrack.db.api.config.GlobalConfiguration;
import com.jetbrains.youtrack.db.api.config.YouTrackDBConfig;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.internal.DbTestBase;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.YouTrackDBImpl;
import com.jetbrains.youtrack.db.internal.core.db.record.ridbag.RidBag;
import com.jetbrains.youtrack.db.internal.core.exception.SerializationException;
import com.jetbrains.youtrack.db.internal.core.id.RecordId;
import com.jetbrains.youtrack.db.internal.core.record.RecordInternal;
import com.jetbrains.youtrack.db.internal.core.serialization.EntitySerializable;
import com.jetbrains.youtrack.db.internal.core.serialization.SerializableStream;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.RecordSerializer;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.binary.RecordSerializerBinary;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.binary.RecordSerializerNetworkBase;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.binary.RecordSerializerNetworkDistributed;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.binary.RecordSerializerNetworkV37;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class DocumentSchemalessBinarySerializationTest extends DbTestBase {

  @Parameters
  public static Collection<Object[]> generateParams() {
    List<Object[]> params = new ArrayList<Object[]>();
    // first we want to run tests for all registreted serializers, and then for two network
    // serializers
    // testig for each serializer type has its own index
    for (byte i = 0; i < RecordSerializerBinary.INSTANCE.getNumberOfSupportedVersions() + 3; i++) {
      params.add(new Object[]{i});
    }
    return params;
  }

  protected RecordSerializer serializer;
  private final byte serializerVersion;

  // first to test for all registreted serializers , then for network serializers
  public DocumentSchemalessBinarySerializationTest(byte serializerVersion) {
    int numOfRegistretedSerializers =
        RecordSerializerBinary.INSTANCE.getNumberOfSupportedVersions();
    if (serializerVersion < numOfRegistretedSerializers) {
      serializer = new RecordSerializerBinary(serializerVersion);
    } else if (serializerVersion == numOfRegistretedSerializers) {
      serializer = new RecordSerializerNetworkBase();
    } else if (serializerVersion == numOfRegistretedSerializers + 1) {
      serializer = new RecordSerializerNetworkV37();
    } else if (serializerVersion == numOfRegistretedSerializers + 2) {
      serializer = new RecordSerializerNetworkDistributed();
    }

    this.serializerVersion = serializerVersion;
  }

  @Before
  public void createSerializer() {
    // we want new instance before method only for network serializers
    if (serializerVersion == RecordSerializerBinary.INSTANCE.getNumberOfSupportedVersions()) {
      serializer = new RecordSerializerNetworkBase();
    } else if (serializerVersion
        == RecordSerializerBinary.INSTANCE.getNumberOfSupportedVersions() + 1) {
      serializer = new RecordSerializerNetworkV37();
    }
  }

  @Test
  public void testSimpleSerialization() {
    EntityImpl document = (EntityImpl) db.newEntity();

    document.field("name", "name");
    document.field("age", 20);
    document.field("youngAge", (short) 20);
    document.field("oldAge", (long) 20);
    document.field("heigth", 12.5f);
    document.field("bitHeigth", 12.5d);
    document.field("class", (byte) 'C');
    document.field("nullField", (Object) null);
    document.field("character", 'C');
    document.field("alive", true);
    document.field("dateTime", new Date());
    document.field(
        "bigNumber", new BigDecimal("43989872423376487952454365232141525434.32146432321442534"));
    RidBag bag = new RidBag(db);
    bag.add(new RecordId(1, 1));
    bag.add(new RecordId(2, 2));
    // document.field("ridBag", bag);
    Calendar c = Calendar.getInstance();
    document.field("date", c.getTime(), PropertyType.DATE);
    Calendar c1 = Calendar.getInstance();
    c1.set(Calendar.MILLISECOND, 0);
    c1.set(Calendar.SECOND, 0);
    c1.set(Calendar.MINUTE, 0);
    c1.set(Calendar.HOUR_OF_DAY, 0);
    document.field("date1", c1.getTime(), PropertyType.DATE);

    byte[] byteValue = new byte[10];
    Arrays.fill(byteValue, (byte) 10);
    document.field("bytes", byteValue);

    document.field("utf8String", "A" + "\u00ea" + "\u00f1" + "\u00fc" + "C");
    document.field("recordId", new RecordId(10, 10));

    byte[] res = serializer.toStream(db, document);
    EntityImpl extr = (EntityImpl) serializer.fromStream(db, res, (EntityImpl) db.newEntity(),
        new String[]{});

    c.set(Calendar.MILLISECOND, 0);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.HOUR_OF_DAY, 0);

    assertEquals(extr.fields(), document.fields());
    assertEquals(extr.<Object>field("name"), document.field("name"));
    assertEquals(extr.<Object>field("age"), document.field("age"));
    assertEquals(extr.<Object>field("youngAge"), document.field("youngAge"));
    assertEquals(extr.<Object>field("oldAge"), document.field("oldAge"));
    assertEquals(extr.<Object>field("heigth"), document.field("heigth"));
    assertEquals(extr.<Object>field("bitHeigth"), document.field("bitHeigth"));
    assertEquals(extr.<Object>field("class"), document.field("class"));
    // TODO fix char management issue:#2427
    // assertEquals(document.field("character"), extr.field("character"));
    assertEquals(extr.<Object>field("alive"), document.field("alive"));
    assertEquals(extr.<Object>field("dateTime"), document.field("dateTime"));
    assertEquals(extr.field("date"), c.getTime());
    assertEquals(extr.field("date1"), c1.getTime());
    //    assertEquals(extr.<String>field("bytes"), document.field("bytes"));
    Assertions.assertThat(extr.<Object>field("bytes")).isEqualTo(document.field("bytes"));
    assertEquals(extr.<String>field("utf8String"), document.field("utf8String"));
    assertEquals(extr.<Object>field("recordId"), document.field("recordId"));
    assertEquals(extr.<Object>field("bigNumber"), document.field("bigNumber"));
    assertNull(extr.field("nullField"));
    // assertEquals(extr.field("ridBag"), document.field("ridBag"));

  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Test
  public void testSimpleLiteralArray() {
    EntityImpl document = (EntityImpl) db.newEntity();
    String[] strings = new String[3];
    strings[0] = "a";
    strings[1] = "b";
    strings[2] = "c";
    document.field("listStrings", strings);

    Short[] shorts = new Short[3];
    shorts[0] = (short) 1;
    shorts[1] = (short) 2;
    shorts[2] = (short) 3;
    document.field("shorts", shorts);

    Long[] longs = new Long[3];
    longs[0] = (long) 1;
    longs[1] = (long) 2;
    longs[2] = (long) 3;
    document.field("longs", longs);

    Integer[] ints = new Integer[3];
    ints[0] = 1;
    ints[1] = 2;
    ints[2] = 3;
    document.field("integers", ints);

    Float[] floats = new Float[3];
    floats[0] = 1.1f;
    floats[1] = 2.2f;
    floats[2] = 3.3f;
    document.field("floats", floats);

    Double[] doubles = new Double[3];
    doubles[0] = 1.1d;
    doubles[1] = 2.2d;
    doubles[2] = 3.3d;
    document.field("doubles", doubles);

    Date[] dates = new Date[3];
    dates[0] = new Date();
    dates[1] = new Date();
    dates[2] = new Date();
    document.field("dates", dates);

    Byte[] bytes = new Byte[3];
    bytes[0] = (byte) 0;
    bytes[1] = (byte) 1;
    bytes[2] = (byte) 3;
    document.field("bytes", bytes);

    // TODO: char not currently supported
    Character[] chars = new Character[3];
    chars[0] = 'A';
    chars[1] = 'B';
    chars[2] = 'C';
    // document.field("chars", chars);

    Boolean[] booleans = new Boolean[3];
    booleans[0] = true;
    booleans[1] = false;
    booleans[2] = false;
    document.field("booleans", booleans);

    Object[] arrayNulls = new Object[3];
    // document.field("arrayNulls", arrayNulls);

    // Object[] listMixed = new ArrayList[9];
    // listMixed[0] = new Boolean(true);
    // listMixed[1] = 1;
    // listMixed[2] = (long) 5;
    // listMixed[3] = (short) 2;
    // listMixed[4] = 4.0f;
    // listMixed[5] = 7.0D;
    // listMixed[6] = "hello";
    // listMixed[7] = new Date();
    // listMixed[8] = (byte) 10;
    // document.field("listMixed", listMixed);

    byte[] res = serializer.toStream(db, document);
    EntityImpl extr = (EntityImpl) serializer.fromStream(db, res, (EntityImpl) db.newEntity(),
        new String[]{});

    assertEquals(extr.fields(), document.fields());
    assertEquals(((List) extr.field("listStrings")).toArray(), document.field("listStrings"));
    assertEquals(((List) extr.field("integers")).toArray(), document.field("integers"));
    assertEquals(((List) extr.field("doubles")).toArray(), document.field("doubles"));
    assertEquals(((List) extr.field("dates")).toArray(), document.field("dates"));
    assertEquals(((List) extr.field("bytes")).toArray(), document.field("bytes"));
    assertEquals(((List) extr.field("booleans")).toArray(), document.field("booleans"));
    // assertEquals(((List) extr.field("arrayNulls")).toArray(), document.field("arrayNulls"));
    // assertEquals(extr.field("listMixed"), document.field("listMixed"));
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Test
  public void testSimpleLiteralList() {
    EntityImpl document = (EntityImpl) db.newEntity();
    List<String> strings = new ArrayList<String>();
    strings.add("a");
    strings.add("b");
    strings.add("c");
    document.field("listStrings", strings);

    List<Short> shorts = new ArrayList<Short>();
    shorts.add((short) 1);
    shorts.add((short) 2);
    shorts.add((short) 3);
    document.field("shorts", shorts);

    List<Long> longs = new ArrayList<Long>();
    longs.add((long) 1);
    longs.add((long) 2);
    longs.add((long) 3);
    document.field("longs", longs);

    List<Integer> ints = new ArrayList<Integer>();
    ints.add(1);
    ints.add(2);
    ints.add(3);
    document.field("integers", ints);

    List<Float> floats = new ArrayList<Float>();
    floats.add(1.1f);
    floats.add(2.2f);
    floats.add(3.3f);
    document.field("floats", floats);

    List<Double> doubles = new ArrayList<Double>();
    doubles.add(1.1);
    doubles.add(2.2);
    doubles.add(3.3);
    document.field("doubles", doubles);

    List<Date> dates = new ArrayList<Date>();
    dates.add(new Date());
    dates.add(new Date());
    dates.add(new Date());
    document.field("dates", dates);

    List<Byte> bytes = new ArrayList<Byte>();
    bytes.add((byte) 0);
    bytes.add((byte) 1);
    bytes.add((byte) 3);
    document.field("bytes", bytes);

    // TODO: char not currently supported
    List<Character> chars = new ArrayList<Character>();
    chars.add('A');
    chars.add('B');
    chars.add('C');
    // document.field("chars", chars);

    List<Boolean> booleans = new ArrayList<Boolean>();
    booleans.add(true);
    booleans.add(false);
    booleans.add(false);
    document.field("booleans", booleans);

    List listMixed = new ArrayList();
    listMixed.add(true);
    listMixed.add(1);
    listMixed.add((long) 5);
    listMixed.add((short) 2);
    listMixed.add(4.0f);
    listMixed.add(7.0D);
    listMixed.add("hello");
    listMixed.add(new Date());
    listMixed.add((byte) 10);
    document.field("listMixed", listMixed);

    byte[] res = serializer.toStream(db, document);
    EntityImpl extr = (EntityImpl) serializer.fromStream(db, res, (EntityImpl) db.newEntity(),
        new String[]{});

    assertEquals(extr.fields(), document.fields());
    assertEquals(extr.<Object>field("listStrings"), document.field("listStrings"));
    assertEquals(extr.<Object>field("integers"), document.field("integers"));
    assertEquals(extr.<Object>field("doubles"), document.field("doubles"));
    assertEquals(extr.<Object>field("dates"), document.field("dates"));
    assertEquals(extr.<Object>field("bytes"), document.field("bytes"));
    assertEquals(extr.<Object>field("booleans"), document.field("booleans"));
    assertEquals(extr.<Object>field("listMixed"), document.field("listMixed"));
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Test
  public void testSimpleLiteralSet() throws InterruptedException {
    try (YouTrackDB ctx = new YouTrackDBImpl(DbTestBase.embeddedDBUrl(getClass()),
        YouTrackDBConfig.defaultConfig())) {
      ctx.execute(
          "create database testSimpleLiteralSet memory users(admin identified by 'adminpwd' role"
              + " admin)");
      try (var db = (DatabaseSessionInternal) ctx.open("testSimpleLiteralSet", "admin",
          "adminpwd")) {
        EntityImpl document = (EntityImpl) db.newEntity();
        Set<String> strings = new HashSet<String>();
        strings.add("a");
        strings.add("b");
        strings.add("c");
        document.field("listStrings", strings);

        Set<Short> shorts = new HashSet<Short>();
        shorts.add((short) 1);
        shorts.add((short) 2);
        shorts.add((short) 3);
        document.field("shorts", shorts);

        Set<Long> longs = new HashSet<Long>();
        longs.add((long) 1);
        longs.add((long) 2);
        longs.add((long) 3);
        document.field("longs", longs);

        Set<Integer> ints = new HashSet<Integer>();
        ints.add(1);
        ints.add(2);
        ints.add(3);
        document.field("integers", ints);

        Set<Float> floats = new HashSet<Float>();
        floats.add(1.1f);
        floats.add(2.2f);
        floats.add(3.3f);
        document.field("floats", floats);

        Set<Double> doubles = new HashSet<Double>();
        doubles.add(1.1);
        doubles.add(2.2);
        doubles.add(3.3);
        document.field("doubles", doubles);

        Set<Date> dates = new HashSet<Date>();
        dates.add(new Date());
        Thread.sleep(1);
        dates.add(new Date());
        Thread.sleep(1);
        dates.add(new Date());
        document.field("dates", dates);

        Set<Byte> bytes = new HashSet<Byte>();
        bytes.add((byte) 0);
        bytes.add((byte) 1);
        bytes.add((byte) 3);
        document.field("bytes", bytes);

        // TODO: char not currently supported
        Set<Character> chars = new HashSet<Character>();
        chars.add('A');
        chars.add('B');
        chars.add('C');
        // document.field("chars", chars);

        Set<Boolean> booleans = new HashSet<Boolean>();
        booleans.add(true);
        booleans.add(false);
        booleans.add(false);
        document.field("booleans", booleans);

        Set listMixed = new HashSet();
        listMixed.add(true);
        listMixed.add(1);
        listMixed.add((long) 5);
        listMixed.add((short) 2);
        listMixed.add(4.0f);
        listMixed.add(7.0D);
        listMixed.add("hello");
        listMixed.add(new Date());
        listMixed.add((byte) 10);
        listMixed.add(new RecordId(10, 20));
        document.field("listMixed", listMixed);

        byte[] res = serializer.toStream(db, document);
        EntityImpl extr = (EntityImpl) serializer.fromStream(db, res, (EntityImpl) db.newEntity(),
            new String[]{});

        assertEquals(extr.fields(), document.fields());
        assertEquals(extr.<Object>field("listStrings"), document.field("listStrings"));
        assertEquals(extr.<Object>field("integers"), document.field("integers"));
        assertEquals(extr.<Object>field("doubles"), document.field("doubles"));
        assertEquals(extr.<Object>field("dates"), document.field("dates"));
        assertEquals(extr.<Object>field("bytes"), document.field("bytes"));
        assertEquals(extr.<Object>field("booleans"), document.field("booleans"));
        assertEquals(extr.<Object>field("listMixed"), document.field("listMixed"));
      }
    }
  }

  @Test
  public void testLinkCollections() {
    try (YouTrackDB ctx = new YouTrackDBImpl(DbTestBase.embeddedDBUrl(getClass()),
        YouTrackDBConfig.defaultConfig())) {
      ctx.execute("create database test memory users(admin identified by 'adminpwd' role admin)");
      try (var db = (DatabaseSessionInternal) ctx.open("test", "admin", "adminpwd")) {
        EntityImpl document = (EntityImpl) db.newEntity();
        Set<RecordId> linkSet = new HashSet<RecordId>();
        linkSet.add(new RecordId(10, 20));
        linkSet.add(new RecordId(10, 21));
        linkSet.add(new RecordId(10, 22));
        linkSet.add(new RecordId(11, 22));
        document.field("linkSet", linkSet, PropertyType.LINKSET);

        List<RecordId> linkList = new ArrayList<RecordId>();
        linkList.add(new RecordId(10, 20));
        linkList.add(new RecordId(10, 21));
        linkList.add(new RecordId(10, 22));
        linkList.add(new RecordId(11, 22));
        document.field("linkList", linkList, PropertyType.LINKLIST);
        byte[] res = serializer.toStream(db, document);
        EntityImpl extr = (EntityImpl) serializer.fromStream(db, res, (EntityImpl) db.newEntity(),
            new String[]{});

        assertEquals(extr.fields(), document.fields());
        assertEquals(
            ((Set<?>) extr.field("linkSet")).size(), ((Set<?>) document.field("linkSet")).size());
        assertTrue(((Set<?>) extr.field("linkSet")).containsAll(document.field("linkSet")));
        assertEquals(extr.<Object>field("linkList"), document.field("linkList"));
      }
      ctx.drop("test");
    }
  }

  @Test
  public void testSimpleEmbeddedDoc() {
    EntityImpl document = (EntityImpl) db.newEntity();
    EntityImpl embedded = (EntityImpl) db.newEntity();
    embedded.field("name", "test");
    embedded.field("surname", "something");
    document.field("embed", embedded, PropertyType.EMBEDDED);

    byte[] res = serializer.toStream(db, document);
    EntityImpl extr = (EntityImpl) serializer.fromStream(db, res, (EntityImpl) db.newEntity(),
        new String[]{});
    assertEquals(document.fields(), extr.fields());
    EntityImpl emb = extr.field("embed");
    assertNotNull(emb);
    assertEquals(emb.<Object>field("name"), embedded.field("name"));
    assertEquals(emb.<Object>field("surname"), embedded.field("surname"));
  }

  @Test
  public void testSimpleMapStringLiteral() {
    EntityImpl document = (EntityImpl) db.newEntity();

    Map<String, String> mapString = new HashMap<String, String>();
    mapString.put("key", "value");
    mapString.put("key1", "value1");
    document.field("mapString", mapString);

    Map<String, Integer> mapInt = new HashMap<String, Integer>();
    mapInt.put("key", 2);
    mapInt.put("key1", 3);
    document.field("mapInt", mapInt);

    Map<String, Long> mapLong = new HashMap<String, Long>();
    mapLong.put("key", 2L);
    mapLong.put("key1", 3L);
    document.field("mapLong", mapLong);

    Map<String, Short> shortMap = new HashMap<String, Short>();
    shortMap.put("key", (short) 2);
    shortMap.put("key1", (short) 3);
    document.field("shortMap", shortMap);

    Map<String, Date> dateMap = new HashMap<String, Date>();
    dateMap.put("key", new Date());
    dateMap.put("key1", new Date());
    document.field("dateMap", dateMap);

    Map<String, Float> floatMap = new HashMap<String, Float>();
    floatMap.put("key", 10f);
    floatMap.put("key1", 11f);
    document.field("floatMap", floatMap);

    Map<String, Double> doubleMap = new HashMap<String, Double>();
    doubleMap.put("key", 10d);
    doubleMap.put("key1", 11d);
    document.field("doubleMap", doubleMap);

    Map<String, Byte> bytesMap = new HashMap<String, Byte>();
    bytesMap.put("key", (byte) 10);
    bytesMap.put("key1", (byte) 11);
    document.field("bytesMap", bytesMap);

    Map<String, String> mapWithNulls = new HashMap<String, String>();
    mapWithNulls.put("key", "dddd");
    mapWithNulls.put("key1", null);
    document.field("bytesMap", mapWithNulls);

    byte[] res = serializer.toStream(db, document);
    EntityImpl extr = (EntityImpl) serializer.fromStream(db, res, (EntityImpl) db.newEntity(),
        new String[]{});
    assertEquals(extr.fields(), document.fields());
    assertEquals(extr.<Object>field("mapString"), document.field("mapString"));
    assertEquals(extr.<Object>field("mapLong"), document.field("mapLong"));
    assertEquals(extr.<Object>field("shortMap"), document.field("shortMap"));
    assertEquals(extr.<Object>field("dateMap"), document.field("dateMap"));
    assertEquals(extr.<Object>field("doubleMap"), document.field("doubleMap"));
    assertEquals(extr.<Object>field("bytesMap"), document.field("bytesMap"));
  }

  @Test
  public void testlistOfList() {
    EntityImpl document = (EntityImpl) db.newEntity();
    List<List<String>> list = new ArrayList<List<String>>();
    List<String> ls = new ArrayList<String>();
    ls.add("test1");
    ls.add("test2");
    list.add(ls);
    document.field("complexList", list);

    byte[] res = serializer.toStream(db, document);
    EntityImpl extr = (EntityImpl) serializer.fromStream(db, res, (EntityImpl) db.newEntity(),
        new String[]{});
    assertEquals(extr.fields(), document.fields());
    assertEquals(extr.<Object>field("complexList"), document.field("complexList"));
  }

  @Test
  public void testArrayOfArray() {
    EntityImpl document = (EntityImpl) db.newEntity();
    String[][] array = new String[1][];
    String[] ls = new String[2];
    ls[0] = "test1";
    ls[1] = "test2";
    array[0] = ls;
    document.field("complexArray", array);

    byte[] res = serializer.toStream(db, document);
    EntityImpl extr = (EntityImpl) serializer.fromStream(db, res, (EntityImpl) db.newEntity(),
        new String[]{});
    assertEquals(extr.fields(), document.fields());
    List<List<String>> savedValue = extr.field("complexArray");
    assertEquals(savedValue.size(), array.length);
    assertEquals(savedValue.get(0).size(), array[0].length);
    assertEquals(savedValue.get(0).get(0), array[0][0]);
    assertEquals(savedValue.get(0).get(1), array[0][1]);
  }

  @Test
  public void testEmbeddedListOfEmbeddedMap() {
    EntityImpl document = (EntityImpl) db.newEntity();
    List<Map<String, String>> coll = new ArrayList<Map<String, String>>();
    Map<String, String> map = new HashMap<String, String>();
    map.put("first", "something");
    map.put("second", "somethingElse");
    Map<String, String> map2 = new HashMap<String, String>();
    map2.put("first", "something");
    map2.put("second", "somethingElse");
    coll.add(map);
    coll.add(map2);
    document.field("list", coll);
    byte[] res = serializer.toStream(db, document);
    EntityImpl extr = (EntityImpl) serializer.fromStream(db, res, (EntityImpl) db.newEntity(),
        new String[]{});
    assertEquals(extr.fields(), document.fields());
    assertEquals(extr.<Object>field("list"), document.field("list"));
  }

  @Test
  public void testMapOfEmbeddedDocument() {
    EntityImpl document = (EntityImpl) db.newEntity();

    EntityImpl embeddedInMap = (EntityImpl) db.newEntity();
    embeddedInMap.field("name", "test");
    embeddedInMap.field("surname", "something");
    Map<String, EntityImpl> map = new HashMap<String, EntityImpl>();
    map.put("embedded", embeddedInMap);
    document.field("map", map, PropertyType.EMBEDDEDMAP);

    byte[] res = serializer.toStream(db, document);
    EntityImpl extr = (EntityImpl) serializer.fromStream(db, res, (EntityImpl) db.newEntity(),
        new String[]{});
    Map<String, EntityImpl> mapS = extr.field("map");
    assertEquals(1, mapS.size());
    EntityImpl emb = mapS.get("embedded");
    assertNotNull(emb);
    assertEquals(emb.<Object>field("name"), embeddedInMap.field("name"));
    assertEquals(emb.<Object>field("surname"), embeddedInMap.field("surname"));
  }

  @Test
  public void testMapOfLink() {
    // needs a database because of the lazy loading
    try (YouTrackDB ctx = new YouTrackDBImpl(DbTestBase.embeddedDBUrl(getClass()),
        YouTrackDBConfig.defaultConfig())) {
      ctx.execute("create database test memory users(admin identified by 'adminpwd' role admin)");
      try (var db = (DatabaseSessionInternal) ctx.open("test", "admin", "adminpwd")) {
        EntityImpl document = (EntityImpl) db.newEntity();

        Map<String, Identifiable> map = new HashMap<String, Identifiable>();
        map.put("link", new RecordId(0, 0));
        document.field("map", map, PropertyType.LINKMAP);

        byte[] res = serializer.toStream(db, document);
        EntityImpl extr = (EntityImpl) serializer.fromStream(db, res, (EntityImpl) db.newEntity(),
            new String[]{});
        assertEquals(extr.fields(), document.fields());
        assertEquals(extr.<Object>field("map"), document.field("map"));
      }
      ctx.drop("test");
    }
  }

  @Test
  public void testDocumentSimple() {
    try (YouTrackDB ctx = new YouTrackDBImpl(DbTestBase.embeddedDBUrl(getClass()),
        YouTrackDBConfig.defaultConfig())) {
      ctx.execute("create database test memory users(admin identified by 'adminpwd' role admin)");
      try (var db = (DatabaseSessionInternal) ctx.open("test", "admin", "adminpwd")) {
        EntityImpl document = (EntityImpl) db.newEntity("TestClass");
        document.field("test", "test");
        byte[] res = serializer.toStream(db, document);
        EntityImpl extr = (EntityImpl) serializer.fromStream(db, res, (EntityImpl) db.newEntity(),
            new String[]{});
        //      assertEquals(extr.getClassName(), document.getClassName());
        assertEquals(extr.fields(), document.fields());
        assertEquals(extr.<Object>field("test"), document.field("test"));
      }
      ctx.drop("test");
    }
  }

  @Test
  public void testDocumentWithCostum() {
    boolean old = GlobalConfiguration.DB_CUSTOM_SUPPORT.getValueAsBoolean();
    GlobalConfiguration.DB_CUSTOM_SUPPORT.setValue(true);

    EntityImpl document = (EntityImpl) db.newEntity();
    document.field("test", "test");
    document.field("custom", new Custom());
    byte[] res = serializer.toStream(db, document);
    EntityImpl extr = (EntityImpl) serializer.fromStream(db, res, (EntityImpl) db.newEntity(),
        new String[]{});
    assertEquals(extr.getClassName(), document.getClassName());
    assertEquals(extr.fields(), document.fields());
    assertEquals(extr.<Object>field("test"), document.field("test"));
    assertEquals(extr.<Object>field("custom"), document.field("custom"));
    GlobalConfiguration.DB_CUSTOM_SUPPORT.setValue(old);
  }

  @Test
  public void testDocumentWithCostumDocument() {
    EntityImpl document = (EntityImpl) db.newEntity();
    document.field("test", "test");
    document.field("custom", new CustomDocument());
    byte[] res = serializer.toStream(db, document);
    EntityImpl extr = (EntityImpl) serializer.fromStream(db, res, (EntityImpl) db.newEntity(),
        new String[]{});
    assertEquals(extr.getClassName(), document.getClassName());
    assertEquals(extr.fields(), document.fields());
    assertEquals(extr.<Object>field("test"), document.field("test"));
    assertEquals(extr.<Object>field("custom"), document.field("custom"));
  }

  @Test(expected = SerializationException.class)
  public void testSetOfWrongData() {
    EntityImpl document = (EntityImpl) db.newEntity();

    Set<Object> embeddedSet = new HashSet<Object>();
    embeddedSet.add(new WrongData());
    document.field("embeddedSet", embeddedSet, PropertyType.EMBEDDEDSET);

    serializer.toStream(db, document);
  }

  @Test(expected = SerializationException.class)
  public void testListOfWrongData() {
    EntityImpl document = (EntityImpl) db.newEntity();

    List<Object> embeddedList = new ArrayList<Object>();
    embeddedList.add(new WrongData());
    document.field("embeddedList", embeddedList, PropertyType.EMBEDDEDLIST);

    serializer.toStream(db, document);
  }

  @Test(expected = SerializationException.class)
  public void testMapOfWrongData() {
    EntityImpl document = (EntityImpl) db.newEntity();

    Map<String, Object> embeddedMap = new HashMap<String, Object>();
    embeddedMap.put("name", new WrongData());
    document.field("embeddedMap", embeddedMap, PropertyType.EMBEDDEDMAP);

    serializer.toStream(db, document);
  }

  @Test(expected = ClassCastException.class)
  public void testLinkSetOfWrongData() {
    EntityImpl document = (EntityImpl) db.newEntity();

    Set<Object> linkSet = new HashSet<Object>();
    linkSet.add(new WrongData());
    document.field("linkSet", linkSet, PropertyType.LINKSET);

    serializer.toStream(db, document);
  }

  @Test(expected = ClassCastException.class)
  public void testLinkListOfWrongData() {
    EntityImpl document = (EntityImpl) db.newEntity();

    List<Object> linkList = new ArrayList<Object>();
    linkList.add(new WrongData());
    document.field("linkList", linkList, PropertyType.LINKLIST);

    serializer.toStream(db, document);
  }

  @Test(expected = ClassCastException.class)
  public void testLinkMapOfWrongData() {
    EntityImpl document = (EntityImpl) db.newEntity();

    Map<String, Object> linkMap = new HashMap<String, Object>();
    linkMap.put("name", new WrongData());
    document.field("linkMap", linkMap, PropertyType.LINKMAP);

    serializer.toStream(db, document);
  }

  @Test(expected = SerializationException.class)
  public void testFieldWrongData() {
    EntityImpl document = (EntityImpl) db.newEntity();

    document.field("wrongData", new WrongData());

    serializer.toStream(db, document);
  }

  @Test
  public void testCollectionOfEmbeddedDocument() {
    EntityImpl document = (EntityImpl) db.newEntity();

    EntityImpl embeddedInList = (EntityImpl) db.newEntity();
    embeddedInList.field("name", "test");
    embeddedInList.field("surname", "something");

    EntityImpl embeddedInList2 = (EntityImpl) db.newEntity();
    embeddedInList2.field("name", "test1");
    embeddedInList2.field("surname", "something2");

    List<EntityImpl> embeddedList = new ArrayList<EntityImpl>();
    embeddedList.add(embeddedInList);
    embeddedList.add(embeddedInList2);
    embeddedList.add(null);
    embeddedList.add((EntityImpl) db.newEntity());
    document.field("embeddedList", embeddedList, PropertyType.EMBEDDEDLIST);

    EntityImpl embeddedInSet = (EntityImpl) db.newEntity();
    embeddedInSet.field("name", "test2");
    embeddedInSet.field("surname", "something3");

    EntityImpl embeddedInSet2 = (EntityImpl) db.newEntity();
    embeddedInSet2.field("name", "test5");
    embeddedInSet2.field("surname", "something6");

    Set<EntityImpl> embeddedSet = new HashSet<EntityImpl>();
    embeddedSet.add(embeddedInSet);
    embeddedSet.add(embeddedInSet2);
    embeddedSet.add((EntityImpl) db.newEntity());
    document.field("embeddedSet", embeddedSet, PropertyType.EMBEDDEDSET);

    byte[] res = serializer.toStream(db, document);
    EntityImpl extr = (EntityImpl) serializer.fromStream(db, res, (EntityImpl) db.newEntity(),
        new String[]{});

    List<EntityImpl> ser = extr.field("embeddedList");
    assertEquals(ser.size(), 4);
    assertNotNull(ser.get(0));
    assertNotNull(ser.get(1));
    assertNull(ser.get(2));
    assertNotNull(ser.get(3));
    EntityImpl inList = ser.get(0);
    assertNotNull(inList);
    assertEquals(inList.<Object>field("name"), embeddedInList.field("name"));
    assertEquals(inList.<Object>field("surname"), embeddedInList.field("surname"));

    Set<EntityImpl> setEmb = extr.field("embeddedSet");
    assertEquals(setEmb.size(), 3);
    boolean ok = false;
    for (EntityImpl inSet : setEmb) {
      assertNotNull(inSet);
      if (embeddedInSet.field("name").equals(inSet.field("name"))
          && embeddedInSet.field("surname").equals(inSet.field("surname"))) {
        ok = true;
      }
    }
    assertTrue("not found record in the set after serilize", ok);
  }

  @Test
  public void testSerializableValue() {
    boolean old = GlobalConfiguration.DB_CUSTOM_SUPPORT.getValueAsBoolean();
    GlobalConfiguration.DB_CUSTOM_SUPPORT.setValue(true);

    EntityImpl document = (EntityImpl) db.newEntity();
    SimpleSerializableClass ser = new SimpleSerializableClass();
    ser.name = "testName";
    document.field("seri", ser);
    byte[] res = serializer.toStream(db, document);
    EntityImpl extr = (EntityImpl) serializer.fromStream(db, res, (EntityImpl) db.newEntity(),
        new String[]{});

    assertNotNull(extr.field("seri"));
    assertEquals(extr.getPropertyType("seri"), PropertyType.CUSTOM);
    SimpleSerializableClass newser = extr.field("seri");
    assertEquals(newser.name, ser.name);
    GlobalConfiguration.DB_CUSTOM_SUPPORT.setValue(old);
  }

  @Test
  public void testFieldNames() {
    EntityImpl document = (EntityImpl) db.newEntity();
    document.fields("a", 1, "b", 2, "c", 3);
    byte[] res = serializer.toStream(db, document);
    EntityImpl extr = (EntityImpl) serializer.fromStream(db, res, (EntityImpl) db.newEntity(),
        new String[]{});

    final String[] fields = extr.fieldNames();

    assertNotNull(fields);
    assertEquals(fields.length, 3);
    assertEquals(fields[0], "a");
    assertEquals(fields[1], "b");
    assertEquals(fields[2], "c");
  }

  @Test
  public void testFieldNamesRaw() {
    EntityImpl document = (EntityImpl) db.newEntity();
    document.fields("a", 1, "b", 2, "c", 3);
    byte[] res = serializer.toStream(db, document);
    final String[] fields = serializer.getFieldNames(db, document, res);

    assertNotNull(fields);
    assertEquals(fields.length, 3);
    assertEquals(fields[0], "a");
    assertEquals(fields[1], "b");
    assertEquals(fields[2], "c");
  }

  @Test
  public void testPartial() {
    EntityImpl document = (EntityImpl) db.newEntity();
    document.field("name", "name");
    document.field("age", 20);
    document.field("youngAge", (short) 20);
    document.field("oldAge", (long) 20);

    byte[] res = serializer.toStream(db, document);
    EntityImpl extr =
        (EntityImpl) serializer.fromStream(db, res, (EntityImpl) db.newEntity(),
            new String[]{"name", "age"});

    assertEquals(document.field("name"), extr.<Object>field("name"));
    assertEquals(document.<Object>field("age"), extr.field("age"));
    assertNull(extr.field("youngAge"));
    assertNull(extr.field("oldAge"));
  }

  @Test
  public void testWithRemove() {
    EntityImpl document = (EntityImpl) db.newEntity();
    document.field("name", "name");
    document.field("age", 20);
    document.field("youngAge", (short) 20);
    document.field("oldAge", (long) 20);
    document.removeField("oldAge");

    byte[] res = serializer.toStream(db, document);
    EntityImpl extr = (EntityImpl) serializer.fromStream(db, res, (EntityImpl) db.newEntity(),
        new String[]{});

    assertEquals(document.field("name"), extr.<Object>field("name"));
    assertEquals(document.<Object>field("age"), extr.field("age"));
    assertEquals(document.<Object>field("youngAge"), extr.field("youngAge"));
    assertNull(extr.field("oldAge"));
  }

  @Test
  public void testPartialCustom() {
    EntityImpl document = (EntityImpl) db.newEntity();
    document.field("name", "name");
    document.field("age", 20);
    document.field("youngAge", (short) 20);
    document.field("oldAge", (long) 20);

    byte[] res = serializer.toStream(db, document);

    EntityImpl extr = new EntityImpl(db, res);

    RecordInternal.setRecordSerializer(extr, serializer);

    assertEquals(document.field("name"), extr.<Object>field("name"));
    assertEquals(document.<Object>field("age"), extr.field("age"));
    assertEquals(document.<Object>field("youngAge"), extr.field("youngAge"));
    assertEquals(document.<Object>field("oldAge"), extr.field("oldAge"));

    assertEquals(document.fieldNames().length, extr.fieldNames().length);
  }

  @Test
  public void testPartialNotFound() {
    // this test want to do only for RecordSerializerNetworkV37
    if (serializer instanceof RecordSerializerNetworkV37) {
      EntityImpl document = (EntityImpl) db.newEntity();
      document.field("name", "name");
      document.field("age", 20);
      document.field("youngAge", (short) 20);
      document.field("oldAge", (long) 20);

      byte[] res = serializer.toStream(db, document);
      EntityImpl extr =
          (EntityImpl) serializer.fromStream(db, res, (EntityImpl) db.newEntity(),
              new String[]{"foo"});

      assertEquals(document.field("name"), extr.<Object>field("name"));
      assertEquals(document.<Object>field("age"), extr.field("age"));
      assertEquals(document.<Object>field("youngAge"), extr.field("youngAge"));
      assertEquals(document.<Object>field("oldAge"), extr.field("oldAge"));
    }
  }

  @Test
  public void testListOfMapsWithNull() {
    EntityImpl document = (EntityImpl) db.newEntity();

    List lista = new ArrayList<>();
    Map mappa = new LinkedHashMap<>();
    mappa.put("prop1", "val1");
    mappa.put("prop2", null);
    lista.add(mappa);

    mappa = new HashMap();
    mappa.put("prop", "val");
    lista.add(mappa);
    document.setProperty("list", lista);

    byte[] res = serializer.toStream(db, document);
    EntityImpl extr = (EntityImpl) serializer.fromStream(db, res, (EntityImpl) db.newEntity(),
        new String[]{});
    assertEquals(extr.fields(), document.fields());
    assertEquals(extr.<Object>field("list"), document.field("list"));
  }

  public static class Custom implements SerializableStream {

    byte[] bytes = new byte[10];

    @Override
    public SerializableStream fromStream(byte[] iStream) throws SerializationException {
      bytes = iStream;
      return this;
    }

    @Override
    public byte[] toStream() throws SerializationException {
      for (int i = 0; i < bytes.length; i++) {
        bytes[i] = (byte) i;
      }
      return bytes;
    }

    @Override
    public boolean equals(Object obj) {
      return obj != null && obj instanceof Custom && Arrays.equals(bytes, ((Custom) obj).bytes);
    }
  }

  public static class CustomDocument implements EntitySerializable {

    private EntityImpl document;

    @Override
    public void fromDocument(EntityImpl document) {
      this.document = document;
    }

    @Override
    public EntityImpl toEntity(DatabaseSessionInternal db) {
      document = (EntityImpl) db.newEntity();
      document.field("test", "some strange content");
      return document;
    }

    @Override
    public boolean equals(Object obj) {
      return obj != null
          && document.field("test").equals(((CustomDocument) obj).document.field("test"));
    }
  }

  private class WrongData {

  }
}
