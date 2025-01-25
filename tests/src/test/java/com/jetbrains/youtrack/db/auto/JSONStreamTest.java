/*
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.youtrack.db.auto;

import com.jetbrains.youtrack.db.api.DatabaseSession;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.api.record.RID;
import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.internal.core.config.StorageConfiguration;
import com.jetbrains.youtrack.db.internal.core.db.record.TrackedList;
import com.jetbrains.youtrack.db.internal.core.db.record.ridbag.RidBag;
import com.jetbrains.youtrack.db.internal.core.exception.SerializationException;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.JSONWriter;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.string.RecordSerializerSchemaAware2CSV;
import com.jetbrains.youtrack.db.internal.core.sql.query.SQLSynchQuery;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

@SuppressWarnings("unchecked")
@Test
public class JSONStreamTest extends BaseDBTest {

  @Parameters(value = "remote")
  public JSONStreamTest(boolean remote) {
    super(remote);
  }

  @Test
  public void testAlmostLink() throws IOException {
    final EntityImpl doc = ((EntityImpl) db.newEntity());
    doc.updateFromJSON(
        new ByteArrayInputStream(
            "{'title': '#330: Dollar Coins Are Done'}".getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  public void testNullList() throws Exception {
    final EntityImpl documentSource = ((EntityImpl) db.newEntity());
    documentSource.updateFromJSON(
        new ByteArrayInputStream(
            "{\"list\" : [\"string\", null]}".getBytes(StandardCharsets.UTF_8)));

    final EntityImpl documentTarget = ((EntityImpl) db.newEntity());
    documentTarget.fromStream(documentSource.toStream());

    final TrackedList<Object> list = documentTarget.field("list", PropertyType.EMBEDDEDLIST);
    Assert.assertEquals(list.get(0), "string");
    Assert.assertNull(list.get(1));
  }

  @Test
  public void testBooleanList() throws IOException {
    final EntityImpl documentSource = ((EntityImpl) db.newEntity());
    documentSource.updateFromJSON(
        new ByteArrayInputStream("{\"list\" : [true, false]}".getBytes(StandardCharsets.UTF_8)));

    final EntityImpl documentTarget = ((EntityImpl) db.newEntity());
    documentTarget.fromStream(documentSource.toStream());

    final TrackedList<Object> list = documentTarget.field("list", PropertyType.EMBEDDEDLIST);
    Assert.assertEquals(list.get(0), true);
    Assert.assertEquals(list.get(1), false);
  }

  @Test
  public void testNumericIntegerList() throws IOException {
    final EntityImpl documentSource = ((EntityImpl) db.newEntity());
    documentSource.updateFromJSON(
        new ByteArrayInputStream("{\"list\" : [17,42]}".getBytes(StandardCharsets.UTF_8)));

    final EntityImpl documentTarget = ((EntityImpl) db.newEntity());
    documentTarget.fromStream(documentSource.toStream());

    final TrackedList<Object> list = documentTarget.field("list", PropertyType.EMBEDDEDLIST);
    Assert.assertEquals(list.get(0), 17);
    Assert.assertEquals(list.get(1), 42);
  }

  @Test
  public void testNumericLongList() throws IOException {
    final EntityImpl documentSource = ((EntityImpl) db.newEntity());
    documentSource.updateFromJSON(
        new ByteArrayInputStream(
            "{\"list\" : [100000000000,100000000001]}".getBytes(StandardCharsets.UTF_8)));

    final EntityImpl documentTarget = ((EntityImpl) db.newEntity());
    documentTarget.fromStream(documentSource.toStream());

    final TrackedList<Object> list = documentTarget.field("list", PropertyType.EMBEDDEDLIST);
    Assert.assertEquals(list.get(0), 100000000000L);
    Assert.assertEquals(list.get(1), 100000000001L);
  }

  @Test
  public void testNumericFloatList() throws IOException {
    final EntityImpl documentSource = ((EntityImpl) db.newEntity());
    documentSource.updateFromJSON(
        new ByteArrayInputStream("{\"list\" : [17.3,42.7]}".getBytes(StandardCharsets.UTF_8)));

    final EntityImpl documentTarget = ((EntityImpl) db.newEntity());
    documentTarget.fromStream(documentSource.toStream());

    final TrackedList<Object> list = documentTarget.field("list", PropertyType.EMBEDDEDLIST);
    Assert.assertEquals(list.get(0), 17.3);
    Assert.assertEquals(list.get(1), 42.7);
  }

  @Test
  public void testNullity() throws IOException {
    final EntityImpl doc = ((EntityImpl) db.newEntity());
    final String docString =
        "{\"gender\":{\"name\":\"Male\"},\"firstName\":\"Jack\",\"lastName\":\"Williams\",\"phone\":\"561-401-3348\",\"email\":\"0586548571@example.com\",\"address\":{\"street1\":\"Smith"
            + " Ave\","
            + "\"street2\":null,\"city\":\"GORDONSVILLE\",\"state\":\"VA\",\"code\":\"22942\"},\"dob\":\"2011-11-17"
            + " 03:17:04\"}";
    doc.updateFromJSON(new ByteArrayInputStream(docString.getBytes(StandardCharsets.UTF_8)));
    final String json = doc.toJSON();
    final EntityImpl loadedDoc = ((EntityImpl) db.newEntity());
    loadedDoc.updateFromJSON(json);
    Assert.assertTrue(doc.hasSameContentOf(loadedDoc));
  }

  // no benchmark
  @Test
  public void testEmbeddedList() throws IOException {
    final EntityImpl doc = ((EntityImpl) db.newEntity());
    final List<EntityImpl> list = new ArrayList<EntityImpl>();
    doc.field("embeddedList", list, PropertyType.EMBEDDEDLIST);
    list.add(((EntityImpl) db.newEntity()).field("name", "Luca"));
    list.add(((EntityImpl) db.newEntity()).field("name", "Marcus"));

    final String json = doc.toJSON();
    final EntityImpl loadedDoc =
        ((EntityImpl) db.newEntity()).updateFromJSON(
            new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
    Assert.assertTrue(doc.hasSameContentOf(loadedDoc));
    Assert.assertTrue(loadedDoc.containsField("embeddedList"));
    Assert.assertTrue(loadedDoc.field("embeddedList") instanceof List<?>);
    Assert.assertTrue(
        ((List<EntityImpl>) loadedDoc.field("embeddedList")).get(0) instanceof EntityImpl);

    EntityImpl newDoc = ((List<EntityImpl>) loadedDoc.field("embeddedList")).get(0);
    Assert.assertEquals(newDoc.field("name"), "Luca");
    newDoc = ((List<EntityImpl>) loadedDoc.field("embeddedList")).get(1);
    Assert.assertEquals(newDoc.field("name"), "Marcus");
  }

  // no benchmark
  @Test
  public void testEmbeddedMap() throws IOException {
    final EntityImpl doc = ((EntityImpl) db.newEntity());

    final Map<String, EntityImpl> map = new HashMap<String, EntityImpl>();
    doc.field("map", map);
    map.put("Luca", ((EntityImpl) db.newEntity()).field("name", "Luca"));
    map.put("Marcus", ((EntityImpl) db.newEntity()).field("name", "Marcus"));
    map.put("Cesare", ((EntityImpl) db.newEntity()).field("name", "Cesare"));

    final String json = doc.toJSON();
    final EntityImpl loadedDoc =
        ((EntityImpl) db.newEntity()).updateFromJSON(
            new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

    Assert.assertTrue(doc.hasSameContentOf(loadedDoc));

    Assert.assertTrue(loadedDoc.containsField("map"));
    Assert.assertTrue(loadedDoc.field("map") instanceof Map<?, ?>);
    Assert.assertTrue(
        ((Map<String, EntityImpl>) loadedDoc.field("map")).values().iterator().next()
            instanceof EntityImpl);

    EntityImpl newDoc = ((Map<String, EntityImpl>) loadedDoc.field("map")).get("Luca");
    Assert.assertEquals(newDoc.field("name"), "Luca");

    newDoc = ((Map<String, EntityImpl>) loadedDoc.field("map")).get("Marcus");
    Assert.assertEquals(newDoc.field("name"), "Marcus");

    newDoc = ((Map<String, EntityImpl>) loadedDoc.field("map")).get("Cesare");
    Assert.assertEquals(newDoc.field("name"), "Cesare");
  }

  // no benchmark
  @Test
  public void testListToJSON() throws IOException {
    final List<EntityImpl> list = new ArrayList<EntityImpl>();
    final EntityImpl first = ((EntityImpl) db.newEntity()).field("name", "Luca");
    final EntityImpl second = ((EntityImpl) db.newEntity()).field("name", "Marcus");
    list.add(first);
    list.add(second);

    final String jsonResult = JSONWriter.listToJSON(db, list, null);
    final EntityImpl doc = ((EntityImpl) db.newEntity());
    final String docString = "{\"result\": " + jsonResult + "}";
    doc.updateFromJSON(new ByteArrayInputStream(docString.getBytes(StandardCharsets.UTF_8)));
    final Collection<EntityImpl> result = doc.field("result");
    Assert.assertTrue(result instanceof Collection);
    Assert.assertEquals(result.size(), 2);
    for (final EntityImpl resultDoc : result) {
      Assert.assertTrue(first.hasSameContentOf(resultDoc) || second.hasSameContentOf(resultDoc));
    }
  }

  // no benchmark
  @Test
  public void testEmptyEmbeddedMap() throws IOException {
    final EntityImpl doc = ((EntityImpl) db.newEntity());

    final Map<String, EntityImpl> map = new HashMap<String, EntityImpl>();
    doc.field("embeddedMap", map, PropertyType.EMBEDDEDMAP);

    final String json = doc.toJSON();
    final EntityImpl loadedDoc =
        ((EntityImpl) db.newEntity()).updateFromJSON(
            new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
    Assert.assertTrue(doc.hasSameContentOf(loadedDoc));
    Assert.assertTrue(loadedDoc.containsField("embeddedMap"));
    Assert.assertTrue(loadedDoc.field("embeddedMap") instanceof Map<?, ?>);

    final Map<String, EntityImpl> loadedMap = loadedDoc.field("embeddedMap");
    Assert.assertEquals(loadedMap.size(), 0);
  }

  // TODO: "@fieldTypes":"date=t,byte=b,long=l"
  @Test
  public void testMultiLevelTypes() throws IOException {
    final String oldDataTimeFormat =
        db.get(DatabaseSession.ATTRIBUTES.DATE_TIME_FORMAT).toString();
    db.set(
        DatabaseSession.ATTRIBUTES.DATE_TIME_FORMAT, StorageConfiguration.DEFAULT_DATETIME_FORMAT);
    try {
      final EntityImpl doc = ((EntityImpl) db.newEntity());
      doc.field("long", 100000000000L);
      doc.field("date", new Date());
      doc.field("byte", (byte) 12);

      final EntityImpl firstLevelDoc = ((EntityImpl) db.newEntity());
      firstLevelDoc.field("long", 200000000000L);
      firstLevelDoc.field("date", new Date());
      firstLevelDoc.field("byte", (byte) 13);

      final EntityImpl secondLevelDoc = ((EntityImpl) db.newEntity());
      secondLevelDoc.field("long", 300000000000L);
      secondLevelDoc.field("date", new Date());
      secondLevelDoc.field("byte", (byte) 14);

      final EntityImpl thirdLevelDoc = ((EntityImpl) db.newEntity());
      thirdLevelDoc.field("long", 400000000000L);
      thirdLevelDoc.field("date", new Date());
      thirdLevelDoc.field("byte", (byte) 15);
      doc.field("doc", firstLevelDoc);
      firstLevelDoc.field("doc", secondLevelDoc);
      secondLevelDoc.field("doc", thirdLevelDoc);

      // TODO: WIP move @fileTypes from end to signature:
      // final String json =
      //
      // "{\"@type\":\"d\",\"@version\":0,\"@fieldTypes\":\"date=t,byte=b,long=l\",\"date\":\"2021-03-11 14:26:13:141\",\"byte\":12,\"doc\":{\"@type\":\"d\",\"@version\":0,\"@fieldTypes\":\"date=t,byte=b,long=l\",\"date\":\"2021-03-11 14:26:13:141\",\"byte\":13,\"doc\":{\"@type\":\"d\",\"@version\":0,\"@fieldTypes\":\"date=t,byte=b,long=l\",\"date\":\"2021-03-11 14:26:13:141\",\"byte\":14,\"doc\":{\"@type\":\"d\",\"@version\":0,\"@fieldTypes\":\"date=t,byte=b,long=l\",\"date\":\"2021-03-11 14:26:13:141\",\"byte\":15,\"long\":400000000000},\"long\":300000000000},\"long\":200000000000},\"long\":100000000000}";
      final String json = doc.toJSON();

      final EntityImpl loadedDoc =
          ((EntityImpl) db.newEntity()).updateFromJSON(
              new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
      Assert.assertTrue(doc.hasSameContentOf(loadedDoc));
      Assert.assertTrue(loadedDoc.field("long") instanceof Long);
      Assert.assertEquals(
          ((Long) doc.field("long")).longValue(), ((Long) loadedDoc.field("long")).longValue());
      Assert.assertTrue(loadedDoc.field("date") instanceof Date);
      Assert.assertTrue(loadedDoc.field("byte") instanceof Byte);
      Assert.assertEquals(
          ((Byte) doc.field("byte")).byteValue(), ((Byte) loadedDoc.field("byte")).byteValue());
      Assert.assertTrue(loadedDoc.field("doc") instanceof EntityImpl);

      final EntityImpl firstDoc = loadedDoc.field("doc");
      Assert.assertTrue(firstLevelDoc.hasSameContentOf(firstDoc));
      Assert.assertTrue(firstDoc.field("long") instanceof Long);
      Assert.assertEquals(
          ((Long) firstLevelDoc.field("long")).longValue(),
          ((Long) firstDoc.field("long")).longValue());
      Assert.assertTrue(firstDoc.field("date") instanceof Date);
      Assert.assertTrue(firstDoc.field("byte") instanceof Byte);
      Assert.assertEquals(
          ((Byte) firstLevelDoc.field("byte")).byteValue(),
          ((Byte) firstDoc.field("byte")).byteValue());
      Assert.assertTrue(firstDoc.field("doc") instanceof EntityImpl);

      final EntityImpl secondDoc = firstDoc.field("doc");
      Assert.assertTrue(secondLevelDoc.hasSameContentOf(secondDoc));
      Assert.assertTrue(secondDoc.field("long") instanceof Long);
      Assert.assertEquals(
          ((Long) secondLevelDoc.field("long")).longValue(),
          ((Long) secondDoc.field("long")).longValue());
      Assert.assertTrue(secondDoc.field("date") instanceof Date);
      Assert.assertTrue(secondDoc.field("byte") instanceof Byte);
      Assert.assertEquals(
          ((Byte) secondLevelDoc.field("byte")).byteValue(),
          ((Byte) secondDoc.field("byte")).byteValue());
      Assert.assertTrue(secondDoc.field("doc") instanceof EntityImpl);

      final EntityImpl thirdDoc = secondDoc.field("doc");
      Assert.assertTrue(thirdLevelDoc.hasSameContentOf(thirdDoc));
      Assert.assertTrue(thirdDoc.field("long") instanceof Long);
      Assert.assertEquals(
          ((Long) thirdLevelDoc.field("long")).longValue(),
          ((Long) thirdDoc.field("long")).longValue());
      Assert.assertTrue(thirdDoc.field("date") instanceof Date);
      Assert.assertTrue(thirdDoc.field("byte") instanceof Byte);
      Assert.assertEquals(
          ((Byte) thirdLevelDoc.field("byte")).byteValue(),
          ((Byte) thirdDoc.field("byte")).byteValue());
    } finally {
      db.set(DatabaseSession.ATTRIBUTES.DATE_TIME_FORMAT, oldDataTimeFormat);
    }
  }

  @Test
  public void testNestedEmbeddedMap() throws IOException {
    final EntityImpl newDoc = ((EntityImpl) db.newEntity());

    final Map<String, HashMap<?, ?>> map1 = new HashMap<>();
    newDoc.field("map1", map1, PropertyType.EMBEDDEDMAP);

    final Map<String, HashMap<?, ?>> map2 = new HashMap<>();
    map1.put("map2", (HashMap<?, ?>) map2);

    final Map<String, HashMap<?, ?>> map3 = new HashMap<>();
    map2.put("map3", (HashMap<?, ?>) map3);

    String json = newDoc.toJSON();
    final EntityImpl loadedDoc =
        ((EntityImpl) db.newEntity()).updateFromJSON(
            new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

    Assert.assertTrue(newDoc.hasSameContentOf(loadedDoc));

    Assert.assertTrue(loadedDoc.containsField("map1"));
    Assert.assertTrue(loadedDoc.field("map1") instanceof Map<?, ?>);
    final Map<String, EntityImpl> loadedMap1 = loadedDoc.field("map1");
    Assert.assertEquals(loadedMap1.size(), 1);

    Assert.assertTrue(loadedMap1.containsKey("map2"));
    Assert.assertTrue(loadedMap1.get("map2") instanceof Map<?, ?>);
    final Map<String, EntityImpl> loadedMap2 = (Map<String, EntityImpl>) loadedMap1.get("map2");
    Assert.assertEquals(loadedMap2.size(), 1);

    Assert.assertTrue(loadedMap2.containsKey("map3"));
    Assert.assertTrue(loadedMap2.get("map3") instanceof Map<?, ?>);
    final Map<String, EntityImpl> loadedMap3 = (Map<String, EntityImpl>) loadedMap2.get("map3");
    Assert.assertEquals(loadedMap3.size(), 0);
  }

  @Test
  public void testFetchedJson() throws IOException {
    final List<EntityImpl> result =
        db
            .command(
                new SQLSynchQuery<EntityImpl>(
                    "select * from Profile where name = 'Barack' and surname = 'Obama'"))
            .execute(db);

    for (final EntityImpl doc : result) {
      final String jsonFull =
          doc.toJSON("type,rid,version,class,keepTypes,attribSameRow,indent:0,fetchPlan:*:-1");
      final EntityImpl loadedDoc =
          ((EntityImpl) db.newEntity())
              .updateFromJSON(new ByteArrayInputStream(jsonFull.getBytes(StandardCharsets.UTF_8)));
      Assert.assertTrue(doc.hasSameContentOf(loadedDoc));
    }
  }

  // Requires JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES
  public void testSpecialChar() throws IOException {
    final EntityImpl doc =
        ((EntityImpl) db.newEntity())
            .updateFromJSON(
                new ByteArrayInputStream(
                    "{name:{\"%Field\":[\"value1\",\"value2\"],\"%Field2\":{},\"%Field3\":\"value3\"}}"
                        .getBytes(StandardCharsets.UTF_8)));
    db.begin();
    doc.save(db.getClusterNameById(db.getDefaultClusterId()));
    db.commit();

    final EntityImpl loadedDoc = db.load(doc.getIdentity());
    Assert.assertEquals(doc, loadedDoc);
  }

  // Required loading @class
  public void testArrayOfArray() throws IOException {
    final EntityImpl doc = ((EntityImpl) db.newEntity());
    doc.updateFromJSON(
        new ByteArrayInputStream(
            "{\"@type\": \"d\",\"@class\": \"Track\",\"type\": \"LineString\",\"coordinates\": [ [ 100,  0 ],  [ 101, 1 ] ]}"
                .getBytes(StandardCharsets.UTF_8)));

    db.begin();
    doc.save();
    db.commit();

    final EntityImpl loadedDoc = db.load(doc.getIdentity());
    Assert.assertTrue(doc.hasSameContentOf(loadedDoc));
  }

  // Required loading @class
  public void testLongTypes() throws IOException {
    final EntityImpl doc = ((EntityImpl) db.newEntity());
    doc.updateFromJSON(
        new ByteArrayInputStream(
            "{\"@type\": \"d\",\"@class\": \"Track\",\"type\": \"LineString\",\"coordinates\": [ [ 32874387347347,  0 ],  [ -23736753287327, 1 ] ]}"
                .getBytes(StandardCharsets.UTF_8)));
    db.begin();
    doc.save();
    db.commit();

    final EntityImpl loadedDoc = db.load(doc.getIdentity());
    Assert.assertTrue(doc.hasSameContentOf(loadedDoc));
  }

  // Requires JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES
  public void testSpecialChars() throws IOException {
    final EntityImpl doc =
        ((EntityImpl) db.newEntity())
            .updateFromJSON(
                new ByteArrayInputStream(
                    "{Field:{\"Key1\":[\"Value1\",\"Value2\"],\"Key2\":{\"%%dummy%%\":null},\"Key3\":\"Value3\"}}"
                        .getBytes(StandardCharsets.UTF_8)));
    db.begin();
    doc.save(db.getClusterNameById(db.getDefaultClusterId()));
    db.commit();

    final EntityImpl loadedDoc = db.load(doc.getIdentity());
    Assert.assertEquals(doc, loadedDoc);
  }

  // TODO
  public void testJsonToStream() throws IOException {
    final String doc1Json =
        "{Key1:{\"%Field1\":[{},{},{},{},{}],\"%Field2\":false,\"%Field3\":\"Value1\"}}";
    final EntityImpl doc1 =
        ((EntityImpl) db.newEntity())
            .updateFromJSON(new ByteArrayInputStream(doc1Json.getBytes(StandardCharsets.UTF_8)));
    final String doc1String = new String(
        RecordSerializerSchemaAware2CSV.INSTANCE.toStream(db, doc1));
    Assert.assertEquals(doc1Json, "{" + doc1String + "}");

    final String doc2Json =
        "{Key1:{\"%Field1\":[{},{},{},{},{}],\"%Field2\":false,\"%Field3\":\"Value1\"}}";
    final EntityImpl doc2 =
        ((EntityImpl) db.newEntity())
            .updateFromJSON(new ByteArrayInputStream(doc2Json.getBytes(StandardCharsets.UTF_8)));
    final String doc2String = new String(
        RecordSerializerSchemaAware2CSV.INSTANCE.toStream(db, doc2));
    Assert.assertEquals(doc2Json, "{" + doc2String + "}");
  }

  // "@fieldTypes":"out=z"
  /*  public void testSameNameCollectionsAndMap() throws IOException {
    EntityImpl doc = ((EntityImpl)db.newEntity());
    doc.field("string", "STRING_VALUE");
    List<EntityImpl> list = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      EntityImpl doc1 = ((EntityImpl)db.newEntity());
      doc.field("number", i);
      list.add(doc1);
      final Map<String, EntityImpl> docMap = new HashMap<>();
      for (int j = 0; j < 5; j++) {
        final EntityImpl doc2 = ((EntityImpl)db.newEntity());
        doc2.field("blabla", j);
        docMap.put(String.valueOf(j), doc2);
        final EntityImpl doc3 = ((EntityImpl)db.newEntity());
        doc3.field("blubli", String.valueOf(i + j));
        doc2.field("out", doc3);
      }
      doc1.field("out", docMap);
      list.add(doc1);
    }
    doc.field("out", list);

    // TODO: WIP move @fileTypes from end to signature:
    String json =
        "{\"@type\":\"d\",\"@version\":0,\"@fieldTypes\":\"out=z\",\"number\":9,\"string\":\"STRING_VALUE\",\"out\":[{\"@type\":\"d\",\"@version\":0,\"out\":{\"0\":{\"@type\":\"d\",\"@version\":0,\"blabla\":0,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"0\"}},\"1\":{\"@type\":\"d\",\"@version\":0,\"blabla\":1,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"1\"}},\"2\":{\"@type\":\"d\",\"@version\":0,\"blabla\":2,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"2\"}},\"3\":{\"@type\":\"d\",\"@version\":0,\"blabla\":3,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"3\"}},\"4\":{\"@type\":\"d\",\"@version\":0,\"blabla\":4,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"4\"}}}},{\"@type\":\"d\",\"@version\":0,\"out\":{\"0\":{\"@type\":\"d\",\"@version\":0,\"blabla\":0,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"0\"}},\"1\":{\"@type\":\"d\",\"@version\":0,\"blabla\":1,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"1\"}},\"2\":{\"@type\":\"d\",\"@version\":0,\"blabla\":2,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"2\"}},\"3\":{\"@type\":\"d\",\"@version\":0,\"blabla\":3,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"3\"}},\"4\":{\"@type\":\"d\",\"@version\":0,\"blabla\":4,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"4\"}}}},{\"@type\":\"d\",\"@version\":0,\"out\":{\"0\":{\"@type\":\"d\",\"@version\":0,\"blabla\":0,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"1\"}},\"1\":{\"@type\":\"d\",\"@version\":0,\"blabla\":1,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"2\"}},\"2\":{\"@type\":\"d\",\"@version\":0,\"blabla\":2,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"3\"}},\"3\":{\"@type\":\"d\",\"@version\":0,\"blabla\":3,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"4\"}},\"4\":{\"@type\":\"d\",\"@version\":0,\"blabla\":4,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"5\"}}}},{\"@type\":\"d\",\"@version\":0,\"out\":{\"0\":{\"@type\":\"d\",\"@version\":0,\"blabla\":0,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"1\"}},\"1\":{\"@type\":\"d\",\"@version\":0,\"blabla\":1,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"2\"}},\"2\":{\"@type\":\"d\",\"@version\":0,\"blabla\":2,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"3\"}},\"3\":{\"@type\":\"d\",\"@version\":0,\"blabla\":3,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"4\"}},\"4\":{\"@type\":\"d\",\"@version\":0,\"blabla\":4,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"5\"}}}},{\"@type\":\"d\",\"@version\":0,\"out\":{\"0\":{\"@type\":\"d\",\"@version\":0,\"blabla\":0,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"2\"}},\"1\":{\"@type\":\"d\",\"@version\":0,\"blabla\":1,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"3\"}},\"2\":{\"@type\":\"d\",\"@version\":0,\"blabla\":2,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"4\"}},\"3\":{\"@type\":\"d\",\"@version\":0,\"blabla\":3,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"5\"}},\"4\":{\"@type\":\"d\",\"@version\":0,\"blabla\":4,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"6\"}}}},{\"@type\":\"d\",\"@version\":0,\"out\":{\"0\":{\"@type\":\"d\",\"@version\":0,\"blabla\":0,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"2\"}},\"1\":{\"@type\":\"d\",\"@version\":0,\"blabla\":1,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"3\"}},\"2\":{\"@type\":\"d\",\"@version\":0,\"blabla\":2,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"4\"}},\"3\":{\"@type\":\"d\",\"@version\":0,\"blabla\":3,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"5\"}},\"4\":{\"@type\":\"d\",\"@version\":0,\"blabla\":4,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"6\"}}}},{\"@type\":\"d\",\"@version\":0,\"out\":{\"0\":{\"@type\":\"d\",\"@version\":0,\"blabla\":0,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"3\"}},\"1\":{\"@type\":\"d\",\"@version\":0,\"blabla\":1,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"4\"}},\"2\":{\"@type\":\"d\",\"@version\":0,\"blabla\":2,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"5\"}},\"3\":{\"@type\":\"d\",\"@version\":0,\"blabla\":3,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"6\"}},\"4\":{\"@type\":\"d\",\"@version\":0,\"blabla\":4,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"7\"}}}},{\"@type\":\"d\",\"@version\":0,\"out\":{\"0\":{\"@type\":\"d\",\"@version\":0,\"blabla\":0,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"3\"}},\"1\":{\"@type\":\"d\",\"@version\":0,\"blabla\":1,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"4\"}},\"2\":{\"@type\":\"d\",\"@version\":0,\"blabla\":2,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"5\"}},\"3\":{\"@type\":\"d\",\"@version\":0,\"blabla\":3,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"6\"}},\"4\":{\"@type\":\"d\",\"@version\":0,\"blabla\":4,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"7\"}}}},{\"@type\":\"d\",\"@version\":0,\"out\":{\"0\":{\"@type\":\"d\",\"@version\":0,\"blabla\":0,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"4\"}},\"1\":{\"@type\":\"d\",\"@version\":0,\"blabla\":1,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"5\"}},\"2\":{\"@type\":\"d\",\"@version\":0,\"blabla\":2,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"6\"}},\"3\":{\"@type\":\"d\",\"@version\":0,\"blabla\":3,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"7\"}},\"4\":{\"@type\":\"d\",\"@version\":0,\"blabla\":4,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"8\"}}}},{\"@type\":\"d\",\"@version\":0,\"out\":{\"0\":{\"@type\":\"d\",\"@version\":0,\"blabla\":0,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"4\"}},\"1\":{\"@type\":\"d\",\"@version\":0,\"blabla\":1,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"5\"}},\"2\":{\"@type\":\"d\",\"@version\":0,\"blabla\":2,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"6\"}},\"3\":{\"@type\":\"d\",\"@version\":0,\"blabla\":3,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"7\"}},\"4\":{\"@type\":\"d\",\"@version\":0,\"blabla\":4,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"8\"}}}},{\"@type\":\"d\",\"@version\":0,\"out\":{\"0\":{\"@type\":\"d\",\"@version\":0,\"blabla\":0,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"5\"}},\"1\":{\"@type\":\"d\",\"@version\":0,\"blabla\":1,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"6\"}},\"2\":{\"@type\":\"d\",\"@version\":0,\"blabla\":2,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"7\"}},\"3\":{\"@type\":\"d\",\"@version\":0,\"blabla\":3,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"8\"}},\"4\":{\"@type\":\"d\",\"@version\":0,\"blabla\":4,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"9\"}}}},{\"@type\":\"d\",\"@version\":0,\"out\":{\"0\":{\"@type\":\"d\",\"@version\":0,\"blabla\":0,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"5\"}},\"1\":{\"@type\":\"d\",\"@version\":0,\"blabla\":1,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"6\"}},\"2\":{\"@type\":\"d\",\"@version\":0,\"blabla\":2,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"7\"}},\"3\":{\"@type\":\"d\",\"@version\":0,\"blabla\":3,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"8\"}},\"4\":{\"@type\":\"d\",\"@version\":0,\"blabla\":4,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"9\"}}}},{\"@type\":\"d\",\"@version\":0,\"out\":{\"0\":{\"@type\":\"d\",\"@version\":0,\"blabla\":0,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"6\"}},\"1\":{\"@type\":\"d\",\"@version\":0,\"blabla\":1,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"7\"}},\"2\":{\"@type\":\"d\",\"@version\":0,\"blabla\":2,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"8\"}},\"3\":{\"@type\":\"d\",\"@version\":0,\"blabla\":3,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"9\"}},\"4\":{\"@type\":\"d\",\"@version\":0,\"blabla\":4,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"10\"}}}},{\"@type\":\"d\",\"@version\":0,\"out\":{\"0\":{\"@type\":\"d\",\"@version\":0,\"blabla\":0,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"6\"}},\"1\":{\"@type\":\"d\",\"@version\":0,\"blabla\":1,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"7\"}},\"2\":{\"@type\":\"d\",\"@version\":0,\"blabla\":2,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"8\"}},\"3\":{\"@type\":\"d\",\"@version\":0,\"blabla\":3,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"9\"}},\"4\":{\"@type\":\"d\",\"@version\":0,\"blabla\":4,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"10\"}}}},{\"@type\":\"d\",\"@version\":0,\"out\":{\"0\":{\"@type\":\"d\",\"@version\":0,\"blabla\":0,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"7\"}},\"1\":{\"@type\":\"d\",\"@version\":0,\"blabla\":1,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"8\"}},\"2\":{\"@type\":\"d\",\"@version\":0,\"blabla\":2,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"9\"}},\"3\":{\"@type\":\"d\",\"@version\":0,\"blabla\":3,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"10\"}},\"4\":{\"@type\":\"d\",\"@version\":0,\"blabla\":4,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"11\"}}}},{\"@type\":\"d\",\"@version\":0,\"out\":{\"0\":{\"@type\":\"d\",\"@version\":0,\"blabla\":0,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"7\"}},\"1\":{\"@type\":\"d\",\"@version\":0,\"blabla\":1,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"8\"}},\"2\":{\"@type\":\"d\",\"@version\":0,\"blabla\":2,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"9\"}},\"3\":{\"@type\":\"d\",\"@version\":0,\"blabla\":3,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"10\"}},\"4\":{\"@type\":\"d\",\"@version\":0,\"blabla\":4,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"11\"}}}},{\"@type\":\"d\",\"@version\":0,\"out\":{\"0\":{\"@type\":\"d\",\"@version\":0,\"blabla\":0,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"8\"}},\"1\":{\"@type\":\"d\",\"@version\":0,\"blabla\":1,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"9\"}},\"2\":{\"@type\":\"d\",\"@version\":0,\"blabla\":2,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"10\"}},\"3\":{\"@type\":\"d\",\"@version\":0,\"blabla\":3,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"11\"}},\"4\":{\"@type\":\"d\",\"@version\":0,\"blabla\":4,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"12\"}}}},{\"@type\":\"d\",\"@version\":0,\"out\":{\"0\":{\"@type\":\"d\",\"@version\":0,\"blabla\":0,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"8\"}},\"1\":{\"@type\":\"d\",\"@version\":0,\"blabla\":1,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"9\"}},\"2\":{\"@type\":\"d\",\"@version\":0,\"blabla\":2,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"10\"}},\"3\":{\"@type\":\"d\",\"@version\":0,\"blabla\":3,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"11\"}},\"4\":{\"@type\":\"d\",\"@version\":0,\"blabla\":4,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"12\"}}}},{\"@type\":\"d\",\"@version\":0,\"out\":{\"0\":{\"@type\":\"d\",\"@version\":0,\"blabla\":0,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"9\"}},\"1\":{\"@type\":\"d\",\"@version\":0,\"blabla\":1,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"10\"}},\"2\":{\"@type\":\"d\",\"@version\":0,\"blabla\":2,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"11\"}},\"3\":{\"@type\":\"d\",\"@version\":0,\"blabla\":3,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"12\"}},\"4\":{\"@type\":\"d\",\"@version\":0,\"blabla\":4,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"13\"}}}},{\"@type\":\"d\",\"@version\":0,\"out\":{\"0\":{\"@type\":\"d\",\"@version\":0,\"blabla\":0,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"9\"}},\"1\":{\"@type\":\"d\",\"@version\":0,\"blabla\":1,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"10\"}},\"2\":{\"@type\":\"d\",\"@version\":0,\"blabla\":2,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"11\"}},\"3\":{\"@type\":\"d\",\"@version\":0,\"blabla\":3,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"12\"}},\"4\":{\"@type\":\"d\",\"@version\":0,\"blabla\":4,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"13\"}}}}]}";
    // String json = doc.toJSON();

    EntityImpl newDoc =
        ((EntityImpl)db.newEntity()).fromJSON(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
    Assert.assertEquals(
        "{\"@type\":\"d\",\"@version\":0,\"number\":9,\"string\":\"STRING_VALUE\",\"out\":[{\"@type\":\"d\",\"@version\":0,\"out\":{\"0\":{\"@type\":\"d\",\"@version\":0,\"blabla\":0,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"0\"}},\"1\":{\"@type\":\"d\",\"@version\":0,\"blabla\":1,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"1\"}},\"2\":{\"@type\":\"d\",\"@version\":0,\"blabla\":2,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"2\"}},\"3\":{\"@type\":\"d\",\"@version\":0,\"blabla\":3,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"3\"}},\"4\":{\"@type\":\"d\",\"@version\":0,\"blabla\":4,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"4\"}}}},{\"@type\":\"d\",\"@version\":0,\"out\":{\"0\":{\"@type\":\"d\",\"@version\":0,\"blabla\":0,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"0\"}},\"1\":{\"@type\":\"d\",\"@version\":0,\"blabla\":1,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"1\"}},\"2\":{\"@type\":\"d\",\"@version\":0,\"blabla\":2,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"2\"}},\"3\":{\"@type\":\"d\",\"@version\":0,\"blabla\":3,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"3\"}},\"4\":{\"@type\":\"d\",\"@version\":0,\"blabla\":4,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"4\"}}}},{\"@type\":\"d\",\"@version\":0,\"out\":{\"0\":{\"@type\":\"d\",\"@version\":0,\"blabla\":0,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"1\"}},\"1\":{\"@type\":\"d\",\"@version\":0,\"blabla\":1,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"2\"}},\"2\":{\"@type\":\"d\",\"@version\":0,\"blabla\":2,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"3\"}},\"3\":{\"@type\":\"d\",\"@version\":0,\"blabla\":3,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"4\"}},\"4\":{\"@type\":\"d\",\"@version\":0,\"blabla\":4,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"5\"}}}},{\"@type\":\"d\",\"@version\":0,\"out\":{\"0\":{\"@type\":\"d\",\"@version\":0,\"blabla\":0,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"1\"}},\"1\":{\"@type\":\"d\",\"@version\":0,\"blabla\":1,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"2\"}},\"2\":{\"@type\":\"d\",\"@version\":0,\"blabla\":2,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"3\"}},\"3\":{\"@type\":\"d\",\"@version\":0,\"blabla\":3,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"4\"}},\"4\":{\"@type\":\"d\",\"@version\":0,\"blabla\":4,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"5\"}}}},{\"@type\":\"d\",\"@version\":0,\"out\":{\"0\":{\"@type\":\"d\",\"@version\":0,\"blabla\":0,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"2\"}},\"1\":{\"@type\":\"d\",\"@version\":0,\"blabla\":1,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"3\"}},\"2\":{\"@type\":\"d\",\"@version\":0,\"blabla\":2,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"4\"}},\"3\":{\"@type\":\"d\",\"@version\":0,\"blabla\":3,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"5\"}},\"4\":{\"@type\":\"d\",\"@version\":0,\"blabla\":4,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"6\"}}}},{\"@type\":\"d\",\"@version\":0,\"out\":{\"0\":{\"@type\":\"d\",\"@version\":0,\"blabla\":0,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"2\"}},\"1\":{\"@type\":\"d\",\"@version\":0,\"blabla\":1,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"3\"}},\"2\":{\"@type\":\"d\",\"@version\":0,\"blabla\":2,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"4\"}},\"3\":{\"@type\":\"d\",\"@version\":0,\"blabla\":3,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"5\"}},\"4\":{\"@type\":\"d\",\"@version\":0,\"blabla\":4,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"6\"}}}},{\"@type\":\"d\",\"@version\":0,\"out\":{\"0\":{\"@type\":\"d\",\"@version\":0,\"blabla\":0,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"3\"}},\"1\":{\"@type\":\"d\",\"@version\":0,\"blabla\":1,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"4\"}},\"2\":{\"@type\":\"d\",\"@version\":0,\"blabla\":2,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"5\"}},\"3\":{\"@type\":\"d\",\"@version\":0,\"blabla\":3,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"6\"}},\"4\":{\"@type\":\"d\",\"@version\":0,\"blabla\":4,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"7\"}}}},{\"@type\":\"d\",\"@version\":0,\"out\":{\"0\":{\"@type\":\"d\",\"@version\":0,\"blabla\":0,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"3\"}},\"1\":{\"@type\":\"d\",\"@version\":0,\"blabla\":1,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"4\"}},\"2\":{\"@type\":\"d\",\"@version\":0,\"blabla\":2,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"5\"}},\"3\":{\"@type\":\"d\",\"@version\":0,\"blabla\":3,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"6\"}},\"4\":{\"@type\":\"d\",\"@version\":0,\"blabla\":4,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"7\"}}}},{\"@type\":\"d\",\"@version\":0,\"out\":{\"0\":{\"@type\":\"d\",\"@version\":0,\"blabla\":0,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"4\"}},\"1\":{\"@type\":\"d\",\"@version\":0,\"blabla\":1,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"5\"}},\"2\":{\"@type\":\"d\",\"@version\":0,\"blabla\":2,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"6\"}},\"3\":{\"@type\":\"d\",\"@version\":0,\"blabla\":3,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"7\"}},\"4\":{\"@type\":\"d\",\"@version\":0,\"blabla\":4,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"8\"}}}},{\"@type\":\"d\",\"@version\":0,\"out\":{\"0\":{\"@type\":\"d\",\"@version\":0,\"blabla\":0,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"4\"}},\"1\":{\"@type\":\"d\",\"@version\":0,\"blabla\":1,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"5\"}},\"2\":{\"@type\":\"d\",\"@version\":0,\"blabla\":2,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"6\"}},\"3\":{\"@type\":\"d\",\"@version\":0,\"blabla\":3,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"7\"}},\"4\":{\"@type\":\"d\",\"@version\":0,\"blabla\":4,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"8\"}}}},{\"@type\":\"d\",\"@version\":0,\"out\":{\"0\":{\"@type\":\"d\",\"@version\":0,\"blabla\":0,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"5\"}},\"1\":{\"@type\":\"d\",\"@version\":0,\"blabla\":1,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"6\"}},\"2\":{\"@type\":\"d\",\"@version\":0,\"blabla\":2,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"7\"}},\"3\":{\"@type\":\"d\",\"@version\":0,\"blabla\":3,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"8\"}},\"4\":{\"@type\":\"d\",\"@version\":0,\"blabla\":4,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"9\"}}}},{\"@type\":\"d\",\"@version\":0,\"out\":{\"0\":{\"@type\":\"d\",\"@version\":0,\"blabla\":0,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"5\"}},\"1\":{\"@type\":\"d\",\"@version\":0,\"blabla\":1,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"6\"}},\"2\":{\"@type\":\"d\",\"@version\":0,\"blabla\":2,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"7\"}},\"3\":{\"@type\":\"d\",\"@version\":0,\"blabla\":3,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"8\"}},\"4\":{\"@type\":\"d\",\"@version\":0,\"blabla\":4,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"9\"}}}},{\"@type\":\"d\",\"@version\":0,\"out\":{\"0\":{\"@type\":\"d\",\"@version\":0,\"blabla\":0,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"6\"}},\"1\":{\"@type\":\"d\",\"@version\":0,\"blabla\":1,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"7\"}},\"2\":{\"@type\":\"d\",\"@version\":0,\"blabla\":2,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"8\"}},\"3\":{\"@type\":\"d\",\"@version\":0,\"blabla\":3,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"9\"}},\"4\":{\"@type\":\"d\",\"@version\":0,\"blabla\":4,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"10\"}}}},{\"@type\":\"d\",\"@version\":0,\"out\":{\"0\":{\"@type\":\"d\",\"@version\":0,\"blabla\":0,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"6\"}},\"1\":{\"@type\":\"d\",\"@version\":0,\"blabla\":1,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"7\"}},\"2\":{\"@type\":\"d\",\"@version\":0,\"blabla\":2,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"8\"}},\"3\":{\"@type\":\"d\",\"@version\":0,\"blabla\":3,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"9\"}},\"4\":{\"@type\":\"d\",\"@version\":0,\"blabla\":4,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"10\"}}}},{\"@type\":\"d\",\"@version\":0,\"out\":{\"0\":{\"@type\":\"d\",\"@version\":0,\"blabla\":0,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"7\"}},\"1\":{\"@type\":\"d\",\"@version\":0,\"blabla\":1,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"8\"}},\"2\":{\"@type\":\"d\",\"@version\":0,\"blabla\":2,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"9\"}},\"3\":{\"@type\":\"d\",\"@version\":0,\"blabla\":3,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"10\"}},\"4\":{\"@type\":\"d\",\"@version\":0,\"blabla\":4,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"11\"}}}},{\"@type\":\"d\",\"@version\":0,\"out\":{\"0\":{\"@type\":\"d\",\"@version\":0,\"blabla\":0,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"7\"}},\"1\":{\"@type\":\"d\",\"@version\":0,\"blabla\":1,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"8\"}},\"2\":{\"@type\":\"d\",\"@version\":0,\"blabla\":2,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"9\"}},\"3\":{\"@type\":\"d\",\"@version\":0,\"blabla\":3,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"10\"}},\"4\":{\"@type\":\"d\",\"@version\":0,\"blabla\":4,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"11\"}}}},{\"@type\":\"d\",\"@version\":0,\"out\":{\"0\":{\"@type\":\"d\",\"@version\":0,\"blabla\":0,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"8\"}},\"1\":{\"@type\":\"d\",\"@version\":0,\"blabla\":1,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"9\"}},\"2\":{\"@type\":\"d\",\"@version\":0,\"blabla\":2,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"10\"}},\"3\":{\"@type\":\"d\",\"@version\":0,\"blabla\":3,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"11\"}},\"4\":{\"@type\":\"d\",\"@version\":0,\"blabla\":4,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"12\"}}}},{\"@type\":\"d\",\"@version\":0,\"out\":{\"0\":{\"@type\":\"d\",\"@version\":0,\"blabla\":0,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"8\"}},\"1\":{\"@type\":\"d\",\"@version\":0,\"blabla\":1,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"9\"}},\"2\":{\"@type\":\"d\",\"@version\":0,\"blabla\":2,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"10\"}},\"3\":{\"@type\":\"d\",\"@version\":0,\"blabla\":3,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"11\"}},\"4\":{\"@type\":\"d\",\"@version\":0,\"blabla\":4,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"12\"}}}},{\"@type\":\"d\",\"@version\":0,\"out\":{\"0\":{\"@type\":\"d\",\"@version\":0,\"blabla\":0,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"9\"}},\"1\":{\"@type\":\"d\",\"@version\":0,\"blabla\":1,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"10\"}},\"2\":{\"@type\":\"d\",\"@version\":0,\"blabla\":2,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"11\"}},\"3\":{\"@type\":\"d\",\"@version\":0,\"blabla\":3,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"12\"}},\"4\":{\"@type\":\"d\",\"@version\":0,\"blabla\":4,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"13\"}}}},{\"@type\":\"d\",\"@version\":0,\"out\":{\"0\":{\"@type\":\"d\",\"@version\":0,\"blabla\":0,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"9\"}},\"1\":{\"@type\":\"d\",\"@version\":0,\"blabla\":1,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"10\"}},\"2\":{\"@type\":\"d\",\"@version\":0,\"blabla\":2,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"11\"}},\"3\":{\"@type\":\"d\",\"@version\":0,\"blabla\":3,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"12\"}},\"4\":{\"@type\":\"d\",\"@version\":0,\"blabla\":4,\"out\":{\"@type\":\"d\",\"@version\":0,\"blubli\":\"13\"}}}}],\"@fieldTypes\":\"out=z\"}",
        // json,
        newDoc.toJSON());
    Assert.assertTrue(newDoc.hasSameContentOf(doc));

    doc = ((EntityImpl)db.newEntity());
    doc.field("string", "STRING_VALUE");
    final Map<String, EntityImpl> docMap = new HashMap<>();
    for (int i = 0; i < 10; i++) {
      EntityImpl doc1 = ((EntityImpl)db.newEntity());
      doc.field("number", i);
      list.add(doc1);
      list = new ArrayList<>();
      for (int j = 0; j < 5; j++) {
        EntityImpl doc2 = ((EntityImpl)db.newEntity());
        doc2.field("blabla", j);
        list.add(doc2);
        EntityImpl doc3 = ((EntityImpl)db.newEntity());
        doc3.field("blubli", String.valueOf(i + j));
        doc2.field("out", doc3);
      }
      doc1.field("out", list);
      docMap.put(String.valueOf(i), doc1);
    }
    doc.field("out", docMap);
    // FIXME: WIP move all @fileTypes from end to signatures:
    // json = doc.toJSON();

    // newDoc = ((EntityImpl)db.newEntity()).fromJSON(new ByteArrayInputStream(json.getBytes()));
    // Assert.assertEquals(json, newDoc.toJSON());
    // Assert.assertTrue(newDoc.hasSameContentOf(doc));
  }

  // "@fieldTypes":"out=z"
  public void testSameNameCollectionsAndMap2() throws IOException {
    final EntityImpl doc = ((EntityImpl)db.newEntity());
    doc.field("string", "STRING_VALUE");
    final List<EntityImpl> list = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      final EntityImpl doc1 = ((EntityImpl)db.newEntity());
      list.add(doc1);
      final Map<String, EntityImpl> docMap = new HashMap<>();
      for (int j = 0; j < 5; j++) {
        final EntityImpl doc2 = ((EntityImpl)db.newEntity());
        doc2.field("blabla", j);
        docMap.put(String.valueOf(j), doc2);
      }
      doc1.field("theMap", docMap);
      list.add(doc1);
    }
    doc.field("theList", list);
    // TODO: WIP move @fileTypes from end to signature:
    final String json =
        "{\"@type\":\"d\",\"@version\":0,\"@fieldTypes\":\"theList=z\",\"theList\":[{\"@type\":\"d\",\"@version\":0,\"theMap\":{\"0\":{\"@type\":\"d\",\"@version\":0,\"blabla\":0},\"1\":{\"@type\":\"d\",\"@version\":0,\"blabla\":1},\"2\":{\"@type\":\"d\",\"@version\":0,\"blabla\":2},\"3\":{\"@type\":\"d\",\"@version\":0,\"blabla\":3},\"4\":{\"@type\":\"d\",\"@version\":0,\"blabla\":4}}},{\"@type\":\"d\",\"@version\":0,\"theMap\":{\"0\":{\"@type\":\"d\",\"@version\":0,\"blabla\":0},\"1\":{\"@type\":\"d\",\"@version\":0,\"blabla\":1},\"2\":{\"@type\":\"d\",\"@version\":0,\"blabla\":2},\"3\":{\"@type\":\"d\",\"@version\":0,\"blabla\":3},\"4\":{\"@type\":\"d\",\"@version\":0,\"blabla\":4}}},{\"@type\":\"d\",\"@version\":0,\"theMap\":{\"0\":{\"@type\":\"d\",\"@version\":0,\"blabla\":0},\"1\":{\"@type\":\"d\",\"@version\":0,\"blabla\":1},\"2\":{\"@type\":\"d\",\"@version\":0,\"blabla\":2},\"3\":{\"@type\":\"d\",\"@version\":0,\"blabla\":3},\"4\":{\"@type\":\"d\",\"@version\":0,\"blabla\":4}}},{\"@type\":\"d\",\"@version\":0,\"theMap\":{\"0\":{\"@type\":\"d\",\"@version\":0,\"blabla\":0},\"1\":{\"@type\":\"d\",\"@version\":0,\"blabla\":1},\"2\":{\"@type\":\"d\",\"@version\":0,\"blabla\":2},\"3\":{\"@type\":\"d\",\"@version\":0,\"blabla\":3},\"4\":{\"@type\":\"d\",\"@version\":0,\"blabla\":4}}}],\"string\":\"STRING_VALUE\"}";
    // final String json = doc.toJSON();

    final EntityImpl newDoc =
        ((EntityImpl)db.newEntity()).fromJSON(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
    Assert.assertEquals(
        "{\"@type\":\"d\",\"@version\":0,\"theList\":[{\"@type\":\"d\",\"@version\":0,\"theMap\":{\"0\":{\"@type\":\"d\",\"@version\":0,\"blabla\":0},\"1\":{\"@type\":\"d\",\"@version\":0,\"blabla\":1},\"2\":{\"@type\":\"d\",\"@version\":0,\"blabla\":2},\"3\":{\"@type\":\"d\",\"@version\":0,\"blabla\":3},\"4\":{\"@type\":\"d\",\"@version\":0,\"blabla\":4}}},{\"@type\":\"d\",\"@version\":0,\"theMap\":{\"0\":{\"@type\":\"d\",\"@version\":0,\"blabla\":0},\"1\":{\"@type\":\"d\",\"@version\":0,\"blabla\":1},\"2\":{\"@type\":\"d\",\"@version\":0,\"blabla\":2},\"3\":{\"@type\":\"d\",\"@version\":0,\"blabla\":3},\"4\":{\"@type\":\"d\",\"@version\":0,\"blabla\":4}}},{\"@type\":\"d\",\"@version\":0,\"theMap\":{\"0\":{\"@type\":\"d\",\"@version\":0,\"blabla\":0},\"1\":{\"@type\":\"d\",\"@version\":0,\"blabla\":1},\"2\":{\"@type\":\"d\",\"@version\":0,\"blabla\":2},\"3\":{\"@type\":\"d\",\"@version\":0,\"blabla\":3},\"4\":{\"@type\":\"d\",\"@version\":0,\"blabla\":4}}},{\"@type\":\"d\",\"@version\":0,\"theMap\":{\"0\":{\"@type\":\"d\",\"@version\":0,\"blabla\":0},\"1\":{\"@type\":\"d\",\"@version\":0,\"blabla\":1},\"2\":{\"@type\":\"d\",\"@version\":0,\"blabla\":2},\"3\":{\"@type\":\"d\",\"@version\":0,\"blabla\":3},\"4\":{\"@type\":\"d\",\"@version\":0,\"blabla\":4}}}],\"string\":\"STRING_VALUE\",\"@fieldTypes\":\"theList=z\"}",
        // json,
        newDoc.toJSON());
    Assert.assertTrue(newDoc.hasSameContentOf(doc));
  }*/

  public void testSameNameCollectionsAndMap3() throws IOException {
    EntityImpl doc = ((EntityImpl) db.newEntity());
    doc.field("string", "STRING_VALUE");
    List<Map<String, EntityImpl>> list = new ArrayList<Map<String, EntityImpl>>();
    for (int i = 0; i < 2; i++) {
      Map<String, EntityImpl> docMap = new HashMap<String, EntityImpl>();
      for (int j = 0; j < 5; j++) {
        EntityImpl doc1 = ((EntityImpl) db.newEntity());
        doc1.field("blabla", j);
        docMap.put(String.valueOf(j), doc1);
      }

      list.add(docMap);
    }
    doc.field("theList", list);
    String json = doc.toJSON();
    EntityImpl newDoc =
        ((EntityImpl) db.newEntity()).updateFromJSON(
            new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
    Assert.assertEquals(newDoc.toJSON(), json);
  }

  public void testSpaces() throws IOException {
    final EntityImpl doc = ((EntityImpl) db.newEntity());
    final String test =
        "{"
            + "\"embedded\": {"
            + "\"second_embedded\":  {"
            + "\"text\":\"this is a test\""
            + "}"
            + "}"
            + "}";
    doc.updateFromJSON(new ByteArrayInputStream(test.getBytes(StandardCharsets.UTF_8)));
    Assert.assertTrue(doc.toJSON("fetchPlan:*:0,rid").indexOf("this is a test") > -1);
  }

  public void testEscaping() throws IOException {
    final EntityImpl doc = ((EntityImpl) db.newEntity());
    final String s =
        "{\"name\": \"test\", \"nested\": { \"key\": \"value\", \"anotherKey\": 123 }, \"deep\":"
            + " {\"deeper\": { \"k\": \"v\",\"quotes\": \"\\\"\\\",\\\"oops\\\":\\\"123\\\"\","
            + " \"likeJson\": \"[1,2,3]\",\"spaces\": \"value with spaces\"}}}";
    doc.updateFromJSON(new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8)));
    Assert.assertEquals(doc.field("deep[deeper][quotes]"), "\"\",\"oops\":\"123\"");

    final String res = doc.toJSON();

    // LOOK FOR "quotes": \"\",\"oops\":\"123\"
    Assert.assertTrue(res.contains("\"quotes\":\"\\\"\\\",\\\"oops\\\":\\\"123\\\"\""));
  }

  public void testEscapingDoubleQuotes() throws IOException {
    final EntityImpl doc = ((EntityImpl) db.newEntity());
    String sb =
        " {\n"
            + "    \"foo\":{\n"
            + "            \"bar\":{\n"
            + "                \"P357\":[\n"
            + "                            {\n"
            + "\n"
            + "                                \"datavalue\":{\n"
            + "                                    \"value\":\"\\\"\\\"\" \n"
            + "                                }\n"
            + "                        }\n"
            + "                ]   \n"
            + "            },\n"
            + "            \"three\": \"a\"\n"
            + "        }\n"
            + "} ";
    doc.updateFromJSON(new ByteArrayInputStream(sb.getBytes(StandardCharsets.UTF_8)));
    Assert.assertEquals(doc.field("foo.three"), "a");
    final Collection c = doc.field("foo.bar.P357");
    Assert.assertEquals(c.size(), 1);
    final Map doc2 = (Map) c.iterator().next();
    Assert.assertEquals(((Map) doc2.get("datavalue")).get("value"), "\"\"");
  }

  // Requires JsonParser.Feature.ALLOW_TRAILING_COMMA
  public void testEscapingDoubleQuotes2() throws IOException {
    final EntityImpl doc = ((EntityImpl) db.newEntity());
    String sb =
        " {\n"
            + "    \"foo\":{\n"
            + "            \"bar\":{\n"
            + "                \"P357\":[\n"
            + "                            {\n"
            + "\n"
            + "                                \"datavalue\":{\n"
            + "                                    \"value\":\"\\\"\",\n"
            + "\n"
            + "                                }\n"
            + "                        }\n"
            + "                ]   \n"
            + "            },\n"
            + "            \"three\": \"a\"\n"
            + "        }\n"
            + "} ";

    doc.updateFromJSON(new ByteArrayInputStream(sb.getBytes(StandardCharsets.UTF_8)));
    Assert.assertEquals(doc.field("foo.three"), "a");
    final Collection c = doc.field("foo.bar.P357");
    Assert.assertEquals(c.size(), 1);
    final Map doc2 = (Map) c.iterator().next();
    Assert.assertEquals(((Map) doc2.get("datavalue")).get("value"), "\"");
  }

  public void testEscapingDoubleQuotes3() throws IOException {
    final EntityImpl doc = ((EntityImpl) db.newEntity());
    String sb =
        " {\n"
            + "    \"foo\":{\n"
            + "            \"bar\":{\n"
            + "                \"P357\":[\n"
            + "                            {\n"
            + "\n"
            + "                                \"datavalue\":{\n"
            + "                                    \"value\":\"\\\"\",\n"
            + "\n"
            + "                                }\n"
            + "                        }\n"
            + "                ]   \n"
            + "            }\n"
            + "        }\n"
            + "} ";

    doc.updateFromJSON(new ByteArrayInputStream(sb.getBytes(StandardCharsets.UTF_8)));
    final Collection c = doc.field("foo.bar.P357");
    Assert.assertEquals(c.size(), 1);
    final Map doc2 = (Map) c.iterator().next();
    Assert.assertEquals(((Map) doc2.get("datavalue")).get("value"), "\"");
  }

  public void testEmbeddedQuotes() throws IOException {
    final EntityImpl doc = ((EntityImpl) db.newEntity());
    // FROM ISSUE 3151
    doc.updateFromJSON(
        new ByteArrayInputStream(
            "{\"mainsnak\":{\"datavalue\":{\"value\":\"Sub\\\\urban\"}}}"
                .getBytes(StandardCharsets.UTF_8)));
    Assert.assertEquals(doc.field("mainsnak.datavalue.value"), "Sub\\urban");
  }

  public void testEmbeddedQuotes2() throws IOException {
    final EntityImpl doc = ((EntityImpl) db.newEntity());
    doc.updateFromJSON(
        new ByteArrayInputStream(
            "{\"datavalue\":{\"value\":\"Sub\\\\urban\"}}".getBytes(StandardCharsets.UTF_8)));
    Assert.assertEquals(doc.field("datavalue.value"), "Sub\\urban");
  }

  public void testEmbeddedQuotes2a() throws IOException {
    final EntityImpl doc = ((EntityImpl) db.newEntity());
    doc.updateFromJSON(
        new ByteArrayInputStream(
            "{\"datavalue\":\"Sub\\\\urban\"}".getBytes(StandardCharsets.UTF_8)));
    Assert.assertEquals(doc.field("datavalue"), "Sub\\urban");
  }

  // TODO: fallback to legacy parser for invalid JSON
  /*public void testEmbeddedQuotes3() throws IOException {
    final EntityImpl doc = ((EntityImpl)db.newEntity());
    final StringBuilder sb = new StringBuilder();
    sb.append("{\"mainsnak\":{\"datavalue\":{\"value\":\"Suburban\\\\\"\"}}}");
    doc.fromJSON(new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8)));
    Assert.assertEquals(doc.field("mainsnak.datavalue.value"), "Suburban\\\"");
  }

  // TODO: fallback to legacy parser for invalid JSON
  public void testEmbeddedQuotes4() throws IOException {
    final EntityImpl doc = ((EntityImpl)db.newEntity());
    final StringBuilder sb = new StringBuilder();
    sb.append("{\"datavalue\":{\"value\":\"Suburban\\\\\"\"}}");
    doc.fromJSON(new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8)));
    Assert.assertEquals(doc.field("datavalue.value"), "Suburban\\\"");
  }

  // TODO: fallback to legacy parser for invalid JSON
  public void testEmbeddedQuotes5() throws IOException {
    final EntityImpl doc = ((EntityImpl)db.newEntity());
    final StringBuilder sb = new StringBuilder();
    sb.append("{\"datavalue\":\"Suburban\\\\\"\"}");
    doc.fromJSON(new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8)));
    Assert.assertEquals(doc.field("datavalue"), "Suburban\\\"");
  }*/

  public void testEmbeddedQuotes6() throws IOException {
    final EntityImpl doc = ((EntityImpl) db.newEntity());
    doc.updateFromJSON(
        new ByteArrayInputStream(
            "{\"mainsnak\":{\"datavalue\":{\"value\":\"Suburban\\\\\"}}}"
                .getBytes(StandardCharsets.UTF_8)));
    Assert.assertEquals(doc.field("mainsnak.datavalue.value"), "Suburban\\");
  }

  public void testEmbeddedQuotes7() throws IOException {
    final EntityImpl doc = ((EntityImpl) db.newEntity());
    doc.updateFromJSON(
        new ByteArrayInputStream(
            "{\"datavalue\":{\"value\":\"Suburban\\\\\"}}".getBytes(StandardCharsets.UTF_8)));
    Assert.assertEquals(doc.field("datavalue.value"), "Suburban\\");
  }

  public void testEmbeddedQuotes8() throws IOException {
    EntityImpl doc = ((EntityImpl) db.newEntity());
    doc.updateFromJSON(
        new ByteArrayInputStream(
            "{\"datavalue\":\"Suburban\\\\\"}".getBytes(StandardCharsets.UTF_8)));
    Assert.assertEquals(doc.field("datavalue"), "Suburban\\");
  }

  public void testEmpty() throws IOException {
    final EntityImpl doc = ((EntityImpl) db.newEntity());
    doc.updateFromJSON(new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)));
    Assert.assertEquals(doc.fieldNames().length, 0);
  }

  public void testInvalidJson() {
    final EntityImpl doc = ((EntityImpl) db.newEntity());
    try {
      doc.updateFromJSON(new ByteArrayInputStream("{".getBytes(StandardCharsets.UTF_8)));
      Assert.fail();
    } catch (SerializationException | IOException e) {
    }

    try {
      doc.updateFromJSON(new ByteArrayInputStream("{\"foo\":{}".getBytes(StandardCharsets.UTF_8)));
      Assert.fail();
    } catch (SerializationException | IOException e) {
    }

    try {
      doc.updateFromJSON(new ByteArrayInputStream("{{}".getBytes(StandardCharsets.UTF_8)));
      Assert.fail();
    } catch (SerializationException | IOException e) {
    }

    try {
      doc.updateFromJSON(new ByteArrayInputStream("{}}".getBytes(StandardCharsets.UTF_8)));
      Assert.fail();
    } catch (SerializationException | IOException e) {
    }

    try {
      doc.updateFromJSON(new ByteArrayInputStream("}".getBytes(StandardCharsets.UTF_8)));
      Assert.fail();
    } catch (SerializationException | IOException e) {
    }
  }

  // @fieldTypes parsing
  public void testDates() throws IOException {
    final Date now = new Date(1350518475000L);

    final EntityImpl doc = ((EntityImpl) db.newEntity());
    doc.field("date", now);
    // TODO: WIP move @fileTypes from end to signature: {"@type":"d","@version":0,"date":"2012-10-18
    // 02:01:15","@fieldTypes":"date=t"}
    // final String json =
    //    "{\"@type\":\"d\",\"@version\":0,\"@fieldTypes\":\"date=t\",\"date\":\"2012-10-18
    // 02:01:15\"}";
    final String json = doc.toJSON();

    final EntityImpl unmarshalled =
        ((EntityImpl) db.newEntity()).updateFromJSON(
            new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
    Assert.assertEquals(unmarshalled.field("date"), now);
  }

  @Test
  public void testList() throws IOException {
    final EntityImpl documentSource = ((EntityImpl) db.newEntity());
    documentSource.updateFromJSON(
        new ByteArrayInputStream("{\"list\" : [\"string\", 42]}".getBytes(StandardCharsets.UTF_8)));

    final EntityImpl documentTarget = ((EntityImpl) db.newEntity());
    documentTarget.fromStream(documentSource.toStream());

    final TrackedList<Object> list = documentTarget.field("list", PropertyType.EMBEDDEDLIST);
    Assert.assertEquals(list.get(0), "string");
    Assert.assertEquals(list.get(1), 42);
  }

  @Test
  public void testEmbeddedRIDBagDeserialisationWhenFieldTypeIsProvided() throws Exception {
    final EntityImpl documentSource = ((EntityImpl) db.newEntity());
    documentSource.updateFromJSON(
        new ByteArrayInputStream(
            "{FirstName:\"Student A 0\",in_EHasGoodStudents:[#57:0],@fieldTypes:\"in_EHasGoodStudents=g\"}"
                .getBytes(StandardCharsets.UTF_8)));
    final RidBag bag = documentSource.field("in_EHasGoodStudents");
    Assert.assertEquals(bag.size(), 1);
    final Identifiable rid = bag.iterator().next();
    Assert.assertEquals(rid.getIdentity().getClusterId(), 57);
    Assert.assertEquals(rid.getIdentity().getClusterPosition(), 0);
  }

  public void testNestedLinkCreation() throws IOException {
    EntityImpl jaimeDoc = ((EntityImpl) db.newEntity("NestedLinkCreation"));
    jaimeDoc.field("name", "jaime");

    db.begin();
    jaimeDoc.save();
    db.commit();

    // The link between jaime and cersei is saved properly - the #2263 test case
    EntityImpl cerseiDoc = ((EntityImpl) db.newEntity("NestedLinkCreation"));
    final String jsonString =
        "{\"@type\":\"d\",\"name\":\"cersei\",\"valonqar\":" + jaimeDoc.toJSON() + "}";
    cerseiDoc.updateFromJSON(new ByteArrayInputStream(jsonString.getBytes(StandardCharsets.UTF_8)));
    db.begin();
    cerseiDoc.save();
    db.commit();

    // The link between jamie and tyrion is not saved properly
    final EntityImpl tyrionDoc = ((EntityImpl) db.newEntity("NestedLinkCreation"));
    final String jsonString2 =
        "{\"@type\":\"d\",\"name\":\"tyrion\",\"emergency_contact\":{\"@type\":\"d\","
            + " \"relationship\":\"brother\",\"contact\":"
            + jaimeDoc.toJSON()
            + "}}";
    tyrionDoc.updateFromJSON(
        new ByteArrayInputStream(jsonString2.getBytes(StandardCharsets.UTF_8)));
    db.begin();
    tyrionDoc.save();
    db.commit();

    final Map<RID, EntityImpl> contentMap = new HashMap<RID, EntityImpl>();

    EntityImpl jaime = ((EntityImpl) db.newEntity("NestedLinkCreation"));
    jaime.field("name", "jaime");

    contentMap.put(jaimeDoc.getIdentity(), jaime);

    EntityImpl cersei = ((EntityImpl) db.newEntity("NestedLinkCreation"));
    cersei.field("name", "cersei");
    cersei.field("valonqar", jaimeDoc.getIdentity());
    contentMap.put(cerseiDoc.getIdentity(), cersei);

    EntityImpl tyrion = ((EntityImpl) db.newEntity("NestedLinkCreation"));
    tyrion.field("name", "tyrion");

    EntityImpl embeddedDoc = ((EntityImpl) db.newEntity());
    embeddedDoc.field("relationship", "brother");
    embeddedDoc.field("contact", jaimeDoc.getIdentity());
    tyrion.field("emergency_contact", embeddedDoc);

    contentMap.put(tyrionDoc.getIdentity(), tyrion);

    final Map<RID, List<RID>> traverseMap = new HashMap<RID, List<RID>>();
    List<RID> jaimeTraverse = new ArrayList<RID>();
    jaimeTraverse.add(jaimeDoc.getIdentity());
    traverseMap.put(jaimeDoc.getIdentity(), jaimeTraverse);

    List<RID> cerseiTraverse = new ArrayList<RID>();
    cerseiTraverse.add(cerseiDoc.getIdentity());
    cerseiTraverse.add(jaimeDoc.getIdentity());

    traverseMap.put(cerseiDoc.getIdentity(), cerseiTraverse);

    List<RID> tyrionTraverse = new ArrayList<RID>();
    tyrionTraverse.add(tyrionDoc.getIdentity());
    tyrionTraverse.add(jaimeDoc.getIdentity());
    traverseMap.put(tyrionDoc.getIdentity(), tyrionTraverse);

    for (EntityImpl o : db.browseClass("NestedLinkCreation")) {
      EntityImpl content = contentMap.get(o.getIdentity());
      Assert.assertTrue(content.hasSameContentOf(o));

      List<RID> traverse = traverseMap.remove(o.getIdentity());
      for (Identifiable id :
          new SQLSynchQuery<EntityImpl>("traverse * from " + o.getIdentity().toString())) {
        Assert.assertTrue(traverse.remove(id.getIdentity()));
      }

      Assert.assertTrue(traverse.isEmpty());
    }
    Assert.assertTrue(traverseMap.isEmpty());
  }

  // TODO: fallback to legacy parser for invalid JSON
  /*public void testNestedLinkCreationFieldTypes() throws IOException {
    EntityImpl jaimeDoc = ((EntityImpl)db.newEntity("NestedLinkCreationFieldTypes"));
    jaimeDoc.field("name", "jaime");
    jaimeDoc.save();

    // The link between jaime and cersei is saved properly - the #2263 test case
    EntityImpl cerseiDoc = ((EntityImpl)db.newEntity("NestedLinkCreationFieldTypes"));
    String jsonString =
        "{\"@type\":\"d\",\"@fieldTypes\":\"valonqar=x\",\"name\":\"cersei\",\"valonqar\":"
            + jaimeDoc.getIdentity()
            + "}";
    cerseiDoc.fromJSON(new ByteArrayInputStream(jsonString.getBytes(StandardCharsets.UTF_8)));
    cerseiDoc.save();

    // The link between jamie and tyrion is not saved properly
    EntityImpl tyrionDoc = ((EntityImpl)db.newEntity("NestedLinkCreationFieldTypes"));
    jsonString =
        "{\"@type\":\"d\",\"name\":\"tyrion\",\"emergency_contact\":{\"@type\":\"d\", \"@fieldTypes\":\"contact=x\",\"relationship\":\"brother\",\"contact\":"
            + jaimeDoc.getIdentity()
            + "}}";
    tyrionDoc.fromJSON(new ByteArrayInputStream(jsonString.getBytes(StandardCharsets.UTF_8)));
    tyrionDoc.save();

    final Map<RID, EntityImpl> contentMap = new HashMap<RID, EntityImpl>();

    EntityImpl jaime = ((EntityImpl)db.newEntity("NestedLinkCreationFieldTypes"));
    jaime.field("name", "jaime");

    contentMap.put(jaimeDoc.getIdentity(), jaime);

    EntityImpl cersei = ((EntityImpl)db.newEntity("NestedLinkCreationFieldTypes"));
    cersei.field("name", "cersei");
    cersei.field("valonqar", jaimeDoc.getIdentity());
    contentMap.put(cerseiDoc.getIdentity(), cersei);

    EntityImpl tyrion = ((EntityImpl)db.newEntity("NestedLinkCreationFieldTypes"));
    tyrion.field("name", "tyrion");

    EntityImpl embeddedDoc = ((EntityImpl)db.newEntity());
    embeddedDoc.field("relationship", "brother");
    embeddedDoc.field("contact", jaimeDoc.getIdentity());
    tyrion.field("emergency_contact", embeddedDoc);

    contentMap.put(tyrionDoc.getIdentity(), tyrion);

    final Map<RID, List<RID>> traverseMap = new HashMap<RID, List<RID>>();
    List<RID> jaimeTraverse = new ArrayList<RID>();
    jaimeTraverse.add(jaimeDoc.getIdentity());
    traverseMap.put(jaimeDoc.getIdentity(), jaimeTraverse);

    List<RID> cerseiTraverse = new ArrayList<RID>();
    cerseiTraverse.add(cerseiDoc.getIdentity());
    cerseiTraverse.add(jaimeDoc.getIdentity());

    traverseMap.put(cerseiDoc.getIdentity(), cerseiTraverse);

    List<RID> tyrionTraverse = new ArrayList<RID>();
    tyrionTraverse.add(tyrionDoc.getIdentity());
    tyrionTraverse.add(jaimeDoc.getIdentity());
    traverseMap.put(tyrionDoc.getIdentity(), tyrionTraverse);

    for (EntityImpl o : database.browseClass("NestedLinkCreationFieldTypes")) {
      EntityImpl content = contentMap.get(o.getIdentity());
      Assert.assertTrue(content.hasSameContentOf(o));

      List<RID> traverse = traverseMap.remove(o.getIdentity());
      for (Identifiable id :
          new SQLSynchQuery<EntityImpl>("traverse * from " + o.getIdentity().toString())) {
        Assert.assertTrue(traverse.remove(id.getIdentity()));
      }
      Assert.assertTrue(traverse.isEmpty());
    }
    Assert.assertTrue(traverseMap.isEmpty());
  }*/

  public void testInnerDocCreation() throws IOException {
    EntityImpl adamDoc = ((EntityImpl) db.newEntity("InnerDocCreation"));
    adamDoc.updateFromJSON(
        new ByteArrayInputStream("{\"name\":\"adam\"}".getBytes(StandardCharsets.UTF_8)));

    db.begin();
    adamDoc.save();
    db.commit();

    EntityImpl eveDoc = ((EntityImpl) db.newEntity("InnerDocCreation"));
    final String jsonString =
        "{\"@type\":\"d\",\"name\":\"eve\",\"friends\":[" + adamDoc.toJSON() + "]}";
    eveDoc.updateFromJSON(new ByteArrayInputStream(jsonString.getBytes(StandardCharsets.UTF_8)));

    db.begin();
    eveDoc.save();
    db.commit();

    Map<RID, EntityImpl> contentMap = new HashMap<RID, EntityImpl>();
    EntityImpl adam = ((EntityImpl) db.newEntity("InnerDocCreation"));
    adam.field("name", "adam");

    contentMap.put(adamDoc.getIdentity(), adam);

    EntityImpl eve = ((EntityImpl) db.newEntity("InnerDocCreation"));
    eve.field("name", "eve");

    List<RID> friends = new ArrayList<RID>();
    friends.add(adamDoc.getIdentity());
    eve.field("friends", friends);

    contentMap.put(eveDoc.getIdentity(), eve);

    Map<RID, List<RID>> traverseMap = new HashMap<RID, List<RID>>();

    List<RID> adamTraverse = new ArrayList<RID>();
    adamTraverse.add(adamDoc.getIdentity());
    traverseMap.put(adamDoc.getIdentity(), adamTraverse);

    List<RID> eveTraverse = new ArrayList<RID>();
    eveTraverse.add(eveDoc.getIdentity());
    eveTraverse.add(adamDoc.getIdentity());

    traverseMap.put(eveDoc.getIdentity(), eveTraverse);

    for (EntityImpl o : db.browseClass("InnerDocCreation")) {
      EntityImpl content = contentMap.get(o.getIdentity());
      Assert.assertTrue(content.hasSameContentOf(o));
    }

    for (final EntityImpl o : db.browseClass("InnerDocCreation")) {
      final List<RID> traverse = traverseMap.remove(o.getIdentity());
      for (final Identifiable id :
          new SQLSynchQuery<EntityImpl>("traverse * from " + o.getIdentity().toString())) {
        Assert.assertTrue(traverse.remove(id.getIdentity()));
      }
      Assert.assertTrue(traverse.isEmpty());
    }
    Assert.assertTrue(traverseMap.isEmpty());
  }

  // TODO: fallback to legacy parser for invalid JSON
  /*public void testInnerDocCreationFieldTypes() throws IOException {
    final EntityImpl adamDoc = ((EntityImpl)db.newEntity("InnerDocCreationFieldTypes"));
    adamDoc.fromJSON(new ByteArrayInputStream("{\"name\":\"adam\"}".getBytes(StandardCharsets.UTF_8)));
    adamDoc.save();

    final EntityImpl eveDoc = ((EntityImpl)db.newEntity("InnerDocCreationFieldTypes"));
    final String jsonString =
        "{\"@type\":\"d\", \"@fieldTypes\" : \"friends=z\", \"name\":\"eve\",\"friends\":["
            + adamDoc.getIdentity()
            + "]}";
    eveDoc.fromJSON(new ByteArrayInputStream(jsonString.getBytes(StandardCharsets.UTF_8)));
    eveDoc.save();

    Map<RID, EntityImpl> contentMap = new HashMap<RID, EntityImpl>();
    EntityImpl adam = ((EntityImpl)db.newEntity("InnerDocCreationFieldTypes"));
    adam.field("name", "adam");

    contentMap.put(adamDoc.getIdentity(), adam);

    EntityImpl eve = ((EntityImpl)db.newEntity("InnerDocCreationFieldTypes"));
    eve.field("name", "eve");

    List<RID> friends = new ArrayList<RID>();
    friends.add(adamDoc.getIdentity());
    eve.field("friends", friends);

    contentMap.put(eveDoc.getIdentity(), eve);

    Map<RID, List<RID>> traverseMap = new HashMap<RID, List<RID>>();

    List<RID> adamTraverse = new ArrayList<RID>();
    adamTraverse.add(adamDoc.getIdentity());
    traverseMap.put(adamDoc.getIdentity(), adamTraverse);

    List<RID> eveTraverse = new ArrayList<RID>();
    eveTraverse.add(eveDoc.getIdentity());
    eveTraverse.add(adamDoc.getIdentity());

    traverseMap.put(eveDoc.getIdentity(), eveTraverse);

    for (EntityImpl o : database.browseClass("InnerDocCreationFieldTypes")) {
      EntityImpl content = contentMap.get(o.getIdentity());
      Assert.assertTrue(content.hasSameContentOf(o));
    }

    for (EntityImpl o : database.browseClass("InnerDocCreationFieldTypes")) {
      List<RID> traverse = traverseMap.remove(o.getIdentity());
      for (Identifiable id :
          new SQLSynchQuery<EntityImpl>("traverse * from " + o.getIdentity().toString())) {
        Assert.assertTrue(traverse.remove(id.getIdentity()));
      }

      Assert.assertTrue(traverse.isEmpty());
    }
    Assert.assertTrue(traverseMap.isEmpty());
  }*/

  public void testJSONTxDocInsertOnly() throws IOException {
    final String classNameOne = "JSONTxDocOneInsertOnly";
    if (!db.getMetadata().getSchema().existsClass(classNameOne)) {
      db.getMetadata().getSchema().createClass(classNameOne);
    }
    final String classNameTwo = "JSONTxDocTwoInsertOnly";
    if (!db.getMetadata().getSchema().existsClass(classNameTwo)) {
      db.getMetadata().getSchema().createClass(classNameTwo);
    }
    db.begin();
    final EntityImpl eveDoc = ((EntityImpl) db.newEntity(classNameOne));
    eveDoc.field("name", "eve");
    eveDoc.save();

    final EntityImpl nestedWithTypeD = ((EntityImpl) db.newEntity(classNameTwo));
    final String jsonString =
        "{\"@type\":\"d\",\"event_name\":\"world cup 2014\",\"admin\":[" + eveDoc.toJSON() + "]}";
    nestedWithTypeD.updateFromJSON(
        new ByteArrayInputStream(jsonString.getBytes(StandardCharsets.UTF_8)));
    nestedWithTypeD.save();
    db.commit();
    Assert.assertEquals(db.countClass(classNameOne), 1);

    final Map<RID, EntityImpl> contentMap = new HashMap<>();

    final EntityImpl eve = ((EntityImpl) db.newEntity(classNameOne));
    eve.field("name", "eve");
    contentMap.put(eveDoc.getIdentity(), eve);

    for (final EntityImpl document : db.browseClass(classNameOne)) {
      final EntityImpl content = contentMap.get(document.getIdentity());
      Assert.assertTrue(content.hasSameContentOf(document));
    }
  }

  public void testJSONTxDoc() throws IOException {
    if (!db.getMetadata().getSchema().existsClass("JSONTxDocOne")) {
      db.getMetadata().getSchema().createClass("JSONTxDocOne");
    }

    if (!db.getMetadata().getSchema().existsClass("JSONTxDocTwo")) {
      db.getMetadata().getSchema().createClass("JSONTxDocTwo");
    }
    final EntityImpl adamDoc = ((EntityImpl) db.newEntity("JSONTxDocOne"));
    adamDoc.field("name", "adam");

    db.begin();
    adamDoc.save();
    db.commit();

    db.begin();
    final EntityImpl eveDoc = ((EntityImpl) db.newEntity("JSONTxDocOne"));
    eveDoc.field("name", "eve");
    eveDoc.save();

    final EntityImpl nestedWithTypeD = ((EntityImpl) db.newEntity("JSONTxDocTwo"));
    final String jsonString =
        "{\"@type\":\"d\",\"event_name\":\"world cup 2014\",\"admin\":["
            + eveDoc.toJSON()
            + ","
            + adamDoc.toJSON()
            + "]}";
    nestedWithTypeD.updateFromJSON(
        new ByteArrayInputStream(jsonString.getBytes(StandardCharsets.UTF_8)));
    nestedWithTypeD.save();
    db.commit();
    Assert.assertEquals(db.countClass("JSONTxDocOne"), 2);

    final Map<RID, EntityImpl> contentMap = new HashMap<>();
    final EntityImpl adam = ((EntityImpl) db.newEntity("JSONTxDocOne"));
    adam.field("name", "adam");
    contentMap.put(adamDoc.getIdentity(), adam);

    final EntityImpl eve = ((EntityImpl) db.newEntity("JSONTxDocOne"));
    eve.field("name", "eve");
    contentMap.put(eveDoc.getIdentity(), eve);

    for (final EntityImpl o : db.browseClass("JSONTxDocOne")) {
      final EntityImpl content = contentMap.get(o.getIdentity());
      Assert.assertTrue(content.hasSameContentOf(o));
    }
  }

  public void testInvalidLink() throws IOException {
    final EntityImpl nullRefDoc = ((EntityImpl) db.newEntity());
    nullRefDoc.updateFromJSON(
        new ByteArrayInputStream(
            "{\"name\":\"Luca\", \"ref\":\"#-1:-1\"}".getBytes(StandardCharsets.UTF_8)));
    // Assert.assertNull(nullRefDoc.rawField("ref"));

    final String json = nullRefDoc.toJSON();
    int pos = json.indexOf("\"ref\":");

    Assert.assertTrue(pos > -1);
    Assert.assertEquals(json.charAt(pos + "\"ref\":".length()), 'n');
  }

  public void testOtherJson() throws IOException {
    ((EntityImpl) db.newEntity())
        .updateFromJSON(
            new ByteArrayInputStream(
                "{\"Salary\":1500.0,\"Type\":\"Person\",\"Address\":[{\"Zip\":\"JX2 MSX\",\"Type\":\"Home\",\"Street1\":\"13 Marge Street\",\"Country\":\"Holland\",\"Id\":\"Address-28813211\",\"City\":\"Amsterdam\",\"From\":\"1996-02-01\",\"To\":\"1998-01-01\"},{\"Zip\":\"90210\",\"Type\":\"Work\",\"Street1\":\"100 Hollywood Drive\",\"Country\":\"USA\",\"Id\":\"Address-11595040\",\"City\":\"Los Angeles\",\"From\":\"2009-09-01\"}],\"Id\":\"Person-7464251\",\"Name\":\"Stan\"}"
                    .getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  public void testNumericFloatListScientific() throws IOException {
    final EntityImpl documentSource = ((EntityImpl) db.newEntity());
    documentSource.updateFromJSON(
        new ByteArrayInputStream(
            "{\"list\" : [-9.27415E-31,741800E+290]}".getBytes(StandardCharsets.UTF_8)));

    final EntityImpl documentTarget = ((EntityImpl) db.newEntity());
    documentTarget.fromStream(documentSource.toStream());

    final TrackedList<Object> list = documentTarget.field("list", PropertyType.EMBEDDEDLIST);
    Assert.assertEquals(list.get(0), -9.27415E-31);
    Assert.assertEquals(list.get(1), 741800E+290);
  }

  @Test
  public void testScientificNotation() throws IOException {
    final EntityImpl doc = ((EntityImpl) db.newEntity());
    doc.updateFromJSON(
        new ByteArrayInputStream(
            "{'number1': -9.2741500e-31, 'number2': 741800E+290}"
                .getBytes(StandardCharsets.UTF_8)));

    final double number1 = doc.field("number1");
    Assert.assertEquals(number1, -9.27415E-31);
    final double number2 = doc.field("number2");
    Assert.assertEquals(number2, 741800E+290);
  }
}
