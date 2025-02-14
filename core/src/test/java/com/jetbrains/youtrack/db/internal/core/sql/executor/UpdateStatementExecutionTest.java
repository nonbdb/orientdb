package com.jetbrains.youtrack.db.internal.core.sql.executor;

import static com.jetbrains.youtrack.db.internal.core.sql.executor.ExecutionPlanPrintUtils.printExecutionPlan;
import static org.junit.Assert.assertEquals;

import com.jetbrains.youtrack.db.api.YouTrackDB;
import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.api.query.ResultSet;
import com.jetbrains.youtrack.db.internal.DbTestBase;
import com.jetbrains.youtrack.db.internal.core.CreateDatabaseUtil;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.api.record.RID;
import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.api.record.Vertex;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

/**
 *
 */
public class UpdateStatementExecutionTest {

  @Rule
  public TestName name = new TestName();

  private DatabaseSessionInternal db;

  private String className;
  private YouTrackDB youTrackDB;

  @Before
  public void before() {
    youTrackDB =
        CreateDatabaseUtil.createDatabase(
            name.getMethodName(), DbTestBase.embeddedDBUrl(getClass()),
            CreateDatabaseUtil.TYPE_MEMORY);
    db =
        (DatabaseSessionInternal)
            youTrackDB.open(name.getMethodName(), "admin", CreateDatabaseUtil.NEW_ADMIN_PASSWORD);

    className = name.getMethodName();
    db.getMetadata().getSchema().createClass(className);

    db.begin();
    for (int i = 0; i < 10; i++) {
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i);
      doc.setProperty("surname", "surname" + i);
      doc.setProperty("number", 4L);

      List<String> tagsList = new ArrayList<>();
      tagsList.add("foo");
      tagsList.add("bar");
      tagsList.add("baz");
      doc.setProperty("tagsList", tagsList);

      Map<String, String> tagsMap = new HashMap<>();
      tagsMap.put("foo", "foo");
      tagsMap.put("bar", "bar");
      tagsMap.put("baz", "baz");
      doc.setProperty("tagsMap", tagsMap);

      doc.save();
    }
    db.commit();
  }

  @After
  public void after() {
    db.close();

    youTrackDB.close();
  }

  @Test
  public void testSetString() {
    db.begin();
    ResultSet result = db.command("update " + className + " set surname = 'foo'");
    db.commit();

    printExecutionPlan(result);
    Assert.assertTrue(result.hasNext());
    Result item = result.next();
    Assert.assertNotNull(item);
    Assert.assertEquals((Object) 10L, item.getProperty("count"));

    Assert.assertFalse(result.hasNext());
    result.close();

    result = db.query("select from " + className);
    for (int i = 0; i < 10; i++) {
      Assert.assertTrue(result.hasNext());
      item = result.next();
      Assert.assertNotNull(item);
      Assert.assertEquals("foo", item.getProperty("surname"));
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testCopyField() {
    db.begin();
    ResultSet result = db.command("update " + className + " set surname = name");
    db.commit();

    Assert.assertTrue(result.hasNext());
    Result item = result.next();
    Assert.assertNotNull(item);
    Assert.assertEquals((Object) 10L, item.getProperty("count"));
    Assert.assertFalse(result.hasNext());
    result.close();

    result = db.query("select from " + className);
    for (int i = 0; i < 10; i++) {
      Assert.assertTrue(result.hasNext());
      item = result.next();
      Assert.assertNotNull(item);
      Assert.assertEquals((Object) item.getProperty("name"), item.getProperty("surname"));
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testSetExpression() {
    db.begin();
    ResultSet result = db.command("update " + className + " set surname = 'foo'+name ");
    db.commit();

    Assert.assertTrue(result.hasNext());
    Result item = result.next();
    Assert.assertNotNull(item);
    Assert.assertEquals((Object) 10L, item.getProperty("count"));
    Assert.assertFalse(result.hasNext());
    result.close();

    result = db.query("select from " + className);
    for (int i = 0; i < 10; i++) {
      Assert.assertTrue(result.hasNext());
      item = result.next();
      Assert.assertNotNull(item);
      Assert.assertEquals("foo" + item.getProperty("name"), item.getProperty("surname"));
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testConditionalSet() {
    db.begin();
    ResultSet result =
        db.command("update " + className + " set surname = 'foo' where name = 'name3'");
    db.commit();

    Assert.assertTrue(result.hasNext());
    Result item = result.next();
    Assert.assertNotNull(item);
    Assert.assertEquals((Object) 1L, item.getProperty("count"));
    Assert.assertFalse(result.hasNext());
    result.close();

    result = db.query("select from " + className);
    boolean found = false;
    for (int i = 0; i < 10; i++) {
      Assert.assertTrue(result.hasNext());
      item = result.next();
      Assert.assertNotNull(item);
      if ("name3".equals(item.getProperty("name"))) {
        Assert.assertEquals("foo", item.getProperty("surname"));
        found = true;
      }
    }
    Assert.assertTrue(found);
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testSetOnList() {
    db.begin();
    ResultSet result =
        db.command("update " + className + " set tagsList[0] = 'abc' where name = 'name3'");
    db.commit();

    printExecutionPlan(result);
    Assert.assertTrue(result.hasNext());
    Result item = result.next();
    Assert.assertNotNull(item);
    Assert.assertEquals((Object) 1L, item.getProperty("count"));
    Assert.assertFalse(result.hasNext());
    result.close();

    result = db.query("select from " + className);
    boolean found = false;
    for (int i = 0; i < 10; i++) {
      Assert.assertTrue(result.hasNext());
      item = result.next();
      Assert.assertNotNull(item);
      if ("name3".equals(item.getProperty("name"))) {
        List<String> tags = new ArrayList<>();
        tags.add("abc");
        tags.add("bar");
        tags.add("baz");
        Assert.assertEquals(tags, item.getProperty("tagsList"));
        found = true;
      }
    }
    Assert.assertTrue(found);
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testSetOnList2() {
    db.begin();
    ResultSet result =
        db.command("update " + className + " set tagsList[6] = 'abc' where name = 'name3'");
    db.commit();

    printExecutionPlan(result);
    Assert.assertTrue(result.hasNext());
    Result item = result.next();
    Assert.assertNotNull(item);
    Assert.assertEquals((Object) 1L, item.getProperty("count"));
    Assert.assertFalse(result.hasNext());
    result.close();

    result = db.query("select from " + className);
    boolean found = false;
    for (int i = 0; i < 10; i++) {
      Assert.assertTrue(result.hasNext());
      item = result.next();
      Assert.assertNotNull(item);
      if ("name3".equals(item.getProperty("name"))) {
        List<String> tags = new ArrayList<>();
        tags.add("foo");
        tags.add("bar");
        tags.add("baz");
        tags.add(null);
        tags.add(null);
        tags.add(null);
        tags.add("abc");
        Assert.assertEquals(tags, item.getProperty("tagsList"));
        found = true;
      }
    }
    Assert.assertTrue(found);
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testSetOnMap() {
    db.begin();
    ResultSet result =
        db.command("update " + className + " set tagsMap['foo'] = 'abc' where name = 'name3'");
    db.commit();

    Assert.assertTrue(result.hasNext());
    Result item = result.next();
    Assert.assertNotNull(item);
    Assert.assertEquals((Object) 1L, item.getProperty("count"));
    Assert.assertFalse(result.hasNext());
    result.close();

    result = db.query("select from " + className);
    boolean found = false;
    for (int i = 0; i < 10; i++) {
      Assert.assertTrue(result.hasNext());
      item = result.next();
      Assert.assertNotNull(item);
      if ("name3".equals(item.getProperty("name"))) {
        Map<String, String> tags = new HashMap<>();
        tags.put("foo", "abc");
        tags.put("bar", "bar");
        tags.put("baz", "baz");
        Assert.assertEquals(tags, item.getProperty("tagsMap"));
        found = true;
      } else {
        Map<String, String> tags = new HashMap<>();
        tags.put("foo", "foo");
        tags.put("bar", "bar");
        tags.put("baz", "baz");
        Assert.assertEquals(tags, item.getProperty("tagsMap"));
      }
    }
    Assert.assertTrue(found);
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testPlusAssign() {
    db.begin();
    ResultSet result =
        db.command("update " + className + " set name += 'foo', newField += 'bar', number += 5");
    db.commit();

    Assert.assertTrue(result.hasNext());
    Result item = result.next();
    Assert.assertNotNull(item);
    Assert.assertEquals((Object) 10L, item.getProperty("count"));
    Assert.assertFalse(result.hasNext());
    result.close();

    result = db.query("select from " + className);
    for (int i = 0; i < 10; i++) {
      Assert.assertTrue(result.hasNext());
      item = result.next();
      Assert.assertNotNull(item);
      Assert.assertTrue(
          item.getProperty("name").toString().endsWith("foo")); // test concatenate string to string
      Assert.assertEquals(8, item.getProperty("name").toString().length());
      Assert.assertEquals("bar", item.getProperty("newField")); // test concatenate null to string
      Assert.assertEquals((Object) 9L, item.getProperty("number")); // test sum numbers
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testMinusAssign() {
    db.begin();
    ResultSet result = db.command("update " + className + " set number -= 5");
    db.commit();

    printExecutionPlan(result);
    Assert.assertTrue(result.hasNext());
    Result item = result.next();
    Assert.assertNotNull(item);
    Assert.assertEquals((Object) 10L, item.getProperty("count"));
    Assert.assertFalse(result.hasNext());
    result.close();

    result = db.query("select from " + className);
    for (int i = 0; i < 10; i++) {
      Assert.assertTrue(result.hasNext());
      item = result.next();
      Assert.assertNotNull(item);
      Assert.assertEquals((Object) (-1L), item.getProperty("number"));
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testStarAssign() {
    db.begin();
    ResultSet result = db.command("update " + className + " set number *= 5");
    db.commit();

    Assert.assertTrue(result.hasNext());
    Result item = result.next();
    Assert.assertNotNull(item);
    Assert.assertEquals((Object) 10L, item.getProperty("count"));
    Assert.assertFalse(result.hasNext());
    result.close();

    result = db.query("select from " + className);
    for (int i = 0; i < 10; i++) {
      Assert.assertTrue(result.hasNext());
      item = result.next();
      Assert.assertNotNull(item);
      Assert.assertEquals((Object) 20L, item.getProperty("number"));
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testSlashAssign() {
    db.begin();
    ResultSet result = db.command("update " + className + " set number /= 2");
    db.commit();

    Assert.assertTrue(result.hasNext());
    Result item = result.next();
    Assert.assertNotNull(item);
    Assert.assertEquals((Object) 10L, item.getProperty("count"));
    Assert.assertFalse(result.hasNext());
    result.close();

    result = db.query("select from " + className);
    for (int i = 0; i < 10; i++) {
      Assert.assertTrue(result.hasNext());
      item = result.next();
      Assert.assertNotNull(item);
      Assert.assertEquals((Object) 2L, item.getProperty("number"));
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testRemove() {
    ResultSet result = db.query("select from " + className);
    for (int i = 0; i < 10; i++) {
      Assert.assertTrue(result.hasNext());
      Result item = result.next();
      Assert.assertNotNull(item);
      Assert.assertNotNull(item.getProperty("surname"));
    }

    result.close();
    db.begin();
    result = db.command("update " + className + " remove surname");
    db.commit();

    for (int i = 0; i < 1; i++) {
      Assert.assertTrue(result.hasNext());
      Result item = result.next();
      Assert.assertNotNull(item);
      Assert.assertEquals((Object) 10L, item.getProperty("count"));
    }
    Assert.assertFalse(result.hasNext());
    result.close();

    result = db.query("select from " + className);
    for (int i = 0; i < 10; i++) {
      Assert.assertTrue(result.hasNext());
      Result item = result.next();
      Assert.assertNotNull(item);
      Assert.assertNull(item.getProperty("surname"));
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testContent() {

    db.begin();
    ResultSet result =
        db.command("update " + className + " content {'name': 'foo', 'secondName': 'bar'}");
    db.commit();

    Assert.assertTrue(result.hasNext());
    Result item = result.next();
    Assert.assertNotNull(item);
    Assert.assertEquals((Object) 10L, item.getProperty("count"));
    Assert.assertFalse(result.hasNext());
    result.close();

    result = db.query("select from " + className);
    for (int i = 0; i < 10; i++) {
      Assert.assertTrue(result.hasNext());
      item = result.next();
      Assert.assertNotNull(item);
      Assert.assertEquals("foo", item.getProperty("name"));
      Assert.assertEquals("bar", item.getProperty("secondName"));
      Assert.assertNull(item.getProperty("surname"));
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testMerge() {

    db.begin();
    ResultSet result =
        db.command("update " + className + " merge {'name': 'foo', 'secondName': 'bar'}");
    db.commit();

    Assert.assertTrue(result.hasNext());
    Result item = result.next();
    Assert.assertNotNull(item);
    Assert.assertEquals((Object) 10L, item.getProperty("count"));
    Assert.assertFalse(result.hasNext());
    result.close();

    result = db.query("select from " + className);
    for (int i = 0; i < 10; i++) {
      Assert.assertTrue(result.hasNext());
      item = result.next();
      Assert.assertNotNull(item);
      Assert.assertEquals("foo", item.getProperty("name"));
      Assert.assertEquals("bar", item.getProperty("secondName"));
      Assert.assertTrue(item.getProperty("surname").toString().startsWith("surname"));
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testUpsert1() {

    db.begin();
    ResultSet result =
        db.command("update " + className + " set foo = 'bar' upsert where name = 'name1'");
    db.commit();

    Assert.assertTrue(result.hasNext());
    Result item = result.next();
    Assert.assertNotNull(item);
    Assert.assertEquals((Object) 1L, item.getProperty("count"));
    Assert.assertFalse(result.hasNext());
    result.close();

    result = db.query("select from " + className);
    for (int i = 0; i < 10; i++) {
      Assert.assertTrue(result.hasNext());
      item = result.next();
      Assert.assertNotNull(item);
      String name = item.getProperty("name");
      Assert.assertNotNull(name);
      if ("name1".equals(name)) {
        Assert.assertEquals("bar", item.getProperty("foo"));
      } else {
        Assert.assertNull(item.getProperty("foo"));
      }
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testUpsertAndReturn() {

    db.begin();
    ResultSet result =
        db.command(
            "update " + className + " set foo = 'bar' upsert  return after  where name = 'name1' ");
    db.commit();

    Assert.assertTrue(result.hasNext());
    Result item = result.next();
    Assert.assertNotNull(item);
    Assert.assertEquals("bar", item.getProperty("foo"));
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testUpsert2() {

    db.begin();
    ResultSet result =
        db.command("update " + className + " set foo = 'bar' upsert where name = 'name11'");
    db.commit();

    Assert.assertTrue(result.hasNext());
    Result item = result.next();
    Assert.assertNotNull(item);
    Assert.assertEquals((Object) 1L, item.getProperty("count"));
    Assert.assertFalse(result.hasNext());
    result.close();

    result = db.query("select from " + className);
    for (int i = 0; i < 11; i++) {
      Assert.assertTrue(result.hasNext());
      item = result.next();
      Assert.assertNotNull(item);
      String name = item.getProperty("name");
      Assert.assertNotNull(name);
      if ("name11".equals(name)) {
        Assert.assertEquals("bar", item.getProperty("foo"));
      } else {
        Assert.assertNull(item.getProperty("foo"));
      }
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testRemove1() {
    String className = "overridden" + this.className;

    SchemaClass clazz = db.getMetadata().getSchema().createClass(className);
    clazz.createProperty(db, "theProperty", PropertyType.EMBEDDEDLIST);

    db.begin();
    EntityImpl doc = db.newInstance(className);
    List theList = new ArrayList();
    for (int i = 0; i < 10; i++) {
      theList.add("n" + i);
    }
    doc.setProperty("theProperty", theList);

    doc.save();
    db.commit();

    db.begin();
    ResultSet result = db.command("update " + className + " remove theProperty[0]");
    db.commit();

    printExecutionPlan(result);
    Assert.assertTrue(result.hasNext());
    Result item = result.next();
    Assert.assertNotNull(item);
    Assert.assertFalse(result.hasNext());
    result.close();

    result = db.query("select from " + className);
    Assert.assertTrue(result.hasNext());
    item = result.next();
    Assert.assertNotNull(item);
    List ls = item.getProperty("theProperty");
    Assert.assertNotNull(ls);
    Assert.assertEquals(9, ls.size());
    Assert.assertFalse(ls.contains("n0"));
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testRemove2() {
    String className = "overridden" + this.className;
    SchemaClass clazz = db.getMetadata().getSchema().createClass(className);
    clazz.createProperty(db, "theProperty", PropertyType.EMBEDDEDLIST);

    db.begin();
    EntityImpl doc = db.newInstance(className);
    List theList = new ArrayList();
    for (int i = 0; i < 10; i++) {
      theList.add("n" + i);
    }
    doc.setProperty("theProperty", theList);

    doc.save();
    db.commit();

    db.begin();
    ResultSet result = db.command("update " + className + " remove theProperty[0, 1, 3]");
    db.commit();

    printExecutionPlan(result);
    Assert.assertTrue(result.hasNext());
    Result item = result.next();
    Assert.assertNotNull(item);
    Assert.assertFalse(result.hasNext());
    result.close();

    result = db.query("select from " + className);
    Assert.assertTrue(result.hasNext());
    item = result.next();
    Assert.assertNotNull(item);
    List ls = item.getProperty("theProperty");

    Assertions.assertThat(ls)
        .isNotNull()
        .hasSize(7)
        .doesNotContain("n0")
        .doesNotContain("n1")
        .contains("n2")
        .doesNotContain("n3")
        .contains("n4");

    //    Assert.assertNotNull(ls);
    //    Assert.assertEquals(7, ls.size());
    //    Assert.assertFalse(ls.contains("n0"));
    //    Assert.assertFalse(ls.contains("n1"));
    //    Assert.assertTrue(ls.contains("n2"));
    //    Assert.assertFalse(ls.contains("n3"));
    //    Assert.assertTrue(ls.contains("n4"));

    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testRemove3() {
    String className = "overriden" + this.className;
    SchemaClass clazz = db.getMetadata().getSchema().createClass(className);
    clazz.createProperty(db, "theProperty", PropertyType.EMBEDDED);

    db.begin();
    EntityImpl doc = db.newInstance(className);
    EntityImpl emb = new EntityImpl();
    emb.setProperty("sub", "foo");
    emb.setProperty("aaa", "bar");
    doc.setProperty("theProperty", emb);

    doc.save();
    db.commit();

    db.begin();
    ResultSet result = db.command("update " + className + " remove theProperty.sub");
    db.commit();

    printExecutionPlan(result);
    Assert.assertTrue(result.hasNext());
    Result item = result.next();
    Assert.assertNotNull(item);
    Assert.assertFalse(result.hasNext());
    result.close();

    result = db.query("select from " + className);
    Assert.assertTrue(result.hasNext());
    item = result.next();
    Assert.assertNotNull(item);
    Result ls = item.getProperty("theProperty");
    Assert.assertNotNull(ls);
    Assert.assertFalse(ls.getPropertyNames().contains("sub"));
    Assert.assertEquals("bar", ls.getProperty("aaa"));
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testRemoveFromMapSquare() {

    db.begin();
    db.command("UPDATE " + className + " REMOVE tagsMap[\"bar\"]").close();
    db.commit();

    ResultSet result = db.query("SELECT tagsMap FROM " + className);
    printExecutionPlan(result);
    for (int i = 0; i < 10; i++) {
      Assert.assertTrue(result.hasNext());
      Result item = result.next();
      Assert.assertNotNull(item);
      Assert.assertEquals(2, ((Map) item.getProperty("tagsMap")).size());
      Assert.assertFalse(((Map) item.getProperty("tagsMap")).containsKey("bar"));
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testRemoveFromMapEquals() {

    db.begin();
    db.command("UPDATE " + className + " REMOVE tagsMap = \"bar\"").close();
    db.commit();

    ResultSet result = db.query("SELECT tagsMap FROM " + className);
    printExecutionPlan(result);
    for (int i = 0; i < 10; i++) {
      Assert.assertTrue(result.hasNext());
      Result item = result.next();
      Assert.assertNotNull(item);
      Assert.assertEquals(2, ((Map) item.getProperty("tagsMap")).size());
      Assert.assertFalse(((Map) item.getProperty("tagsMap")).containsKey("bar"));
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testUpdateWhereSubquery() {

    db.begin();
    Vertex vertex = db.newVertex();
    vertex.setProperty("one", "two");
    RID identity = db.save(vertex).getIdentity();
    db.commit();

    db.begin();
    try (ResultSet result =
        db.command(
            "update v set first='value' where @rid in (select @rid from [" + identity + "]) ")) {

      assertEquals((long) result.next().getProperty("count"), 1L);
    }
    db.commit();

    db.begin();
    try (ResultSet result =
        db.command(
            "update v set other='value' where @rid in (select * from [" + identity + "]) ")) {
      assertEquals((long) result.next().getProperty("count"), 1L);
    }
    db.commit();
  }
}
