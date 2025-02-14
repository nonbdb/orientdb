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

import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.api.query.ResultSet;
import com.jetbrains.youtrack.db.api.record.RID;
import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.api.schema.Schema;
import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import com.jetbrains.youtrack.db.internal.common.util.RawPair;
import com.jetbrains.youtrack.db.internal.core.index.Index;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.testng.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

@Test
public class TruncateClassTest extends BaseDBTest {

  @Parameters(value = "remote")
  public TruncateClassTest(boolean remote) {
    super(remote);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testTruncateClass() {
    checkEmbeddedDB();

    Schema schema = database.getMetadata().getSchema();
    SchemaClass testClass = getOrCreateClass(schema);

    final Index index = getOrCreateIndex(testClass);

    database.command("truncate class test_class").close();

    database.begin();
    database.save(
        new EntityImpl(testClass).field("name", "x").field("data", Arrays.asList(1, 2)));
    database.save(
        new EntityImpl(testClass).field("name", "y").field("data", Arrays.asList(3, 0)));
    database.commit();

    database.command("truncate class test_class").close();

    database.begin();
    database.save(
        new EntityImpl(testClass).field("name", "x").field("data", Arrays.asList(5, 6, 7)));
    database.save(
        new EntityImpl(testClass).field("name", "y").field("data", Arrays.asList(8, 9, -1)));
    database.commit();

    List<Result> result =
        database.query("select from test_class").stream().collect(Collectors.toList());
    Assert.assertEquals(result.size(), 2);
    Set<Integer> set = new HashSet<Integer>();
    for (Result document : result) {
      set.addAll(document.getProperty("data"));
    }
    Assert.assertTrue(set.containsAll(Arrays.asList(5, 6, 7, 8, 9, -1)));

    Assert.assertEquals(index.getInternal().size(database), 6);

    Iterator<RawPair<Object, RID>> indexIterator;
    try (Stream<RawPair<Object, RID>> stream = index.getInternal().stream(database)) {
      indexIterator = stream.iterator();

      while (indexIterator.hasNext()) {
        RawPair<Object, RID> entry = indexIterator.next();
        Assert.assertTrue(set.contains((Integer) entry.first));
      }
    }

    schema.dropClass("test_class");
  }

  @Test
  public void testTruncateVertexClass() {
    database.command("create class TestTruncateVertexClass extends V").close();
    database.begin();
    database.command("create vertex TestTruncateVertexClass set name = 'foo'").close();
    database.commit();

    try {
      database.command("truncate class TestTruncateVertexClass ").close();
      Assert.fail();
    } catch (Exception e) {
    }
    ResultSet result = database.query("select from TestTruncateVertexClass");
    Assert.assertEquals(result.stream().count(), 1);

    database.command("truncate class TestTruncateVertexClass unsafe").close();
    result = database.query("select from TestTruncateVertexClass");
    Assert.assertEquals(result.stream().count(), 0);
  }

  @Test
  public void testTruncateVertexClassSubclasses() {

    database.command("create class TestTruncateVertexClassSuperclass").close();
    database
        .command(
            "create class TestTruncateVertexClassSubclass extends"
                + " TestTruncateVertexClassSuperclass")
        .close();

    database.begin();
    database.command("insert into TestTruncateVertexClassSuperclass set name = 'foo'").close();
    database.command("insert into TestTruncateVertexClassSubclass set name = 'bar'").close();
    database.commit();

    ResultSet result = database.query("select from TestTruncateVertexClassSuperclass");
    Assert.assertEquals(result.stream().count(), 2);

    database.command("truncate class TestTruncateVertexClassSuperclass ").close();
    result = database.query("select from TestTruncateVertexClassSubclass");
    Assert.assertEquals(result.stream().count(), 1);

    database.command("truncate class TestTruncateVertexClassSuperclass polymorphic").close();
    result = database.query("select from TestTruncateVertexClassSubclass");
    Assert.assertEquals(result.stream().count(), 0);
  }

  @Test
  public void testTruncateVertexClassSubclassesWithIndex() {
    checkEmbeddedDB();

    database.command("create class TestTruncateVertexClassSuperclassWithIndex").close();
    database
        .command("create property TestTruncateVertexClassSuperclassWithIndex.name STRING")
        .close();
    database
        .command(
            "create index TestTruncateVertexClassSuperclassWithIndex_index on"
                + " TestTruncateVertexClassSuperclassWithIndex (name) NOTUNIQUE")
        .close();

    database
        .command(
            "create class TestTruncateVertexClassSubclassWithIndex extends"
                + " TestTruncateVertexClassSuperclassWithIndex")
        .close();

    database.begin();
    database
        .command("insert into TestTruncateVertexClassSuperclassWithIndex set name = 'foo'")
        .close();
    database
        .command("insert into TestTruncateVertexClassSubclassWithIndex set name = 'bar'")
        .close();
    database.commit();

    final Index index = getIndex("TestTruncateVertexClassSuperclassWithIndex_index");
    Assert.assertEquals(index.getInternal().size(database), 2);

    database.command("truncate class TestTruncateVertexClassSubclassWithIndex").close();
    Assert.assertEquals(index.getInternal().size(database), 1);

    database
        .command("truncate class TestTruncateVertexClassSuperclassWithIndex polymorphic")
        .close();
    Assert.assertEquals(index.getInternal().size(database), 0);
  }

  private Index getOrCreateIndex(SchemaClass testClass) {
    Index index =
        database.getMetadata().getIndexManagerInternal().getIndex(database, "test_class_by_data");
    if (index == null) {
      testClass.createProperty(database, "data", PropertyType.EMBEDDEDLIST, PropertyType.INTEGER);
      testClass.createIndex(database, "test_class_by_data", SchemaClass.INDEX_TYPE.UNIQUE,
          "data");
    }
    return database.getMetadata().getIndexManagerInternal()
        .getIndex(database, "test_class_by_data");
  }

  private SchemaClass getOrCreateClass(Schema schema) {
    SchemaClass testClass;
    if (schema.existsClass("test_class")) {
      testClass = schema.getClass("test_class");
    } else {
      testClass = schema.createClass("test_class");
    }
    return testClass;
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testTruncateClassWithCommandCache() {

    Schema schema = database.getMetadata().getSchema();
    SchemaClass testClass = getOrCreateClass(schema);

    database.command("truncate class test_class").close();

    database.begin();
    database.save(
        new EntityImpl(testClass).field("name", "x").field("data", Arrays.asList(1, 2)));
    database.save(
        new EntityImpl(testClass).field("name", "y").field("data", Arrays.asList(3, 0)));
    database.commit();

    ResultSet result = database.query("select from test_class");
    Assert.assertEquals(result.stream().count(), 2);

    database.command("truncate class test_class").close();

    result = database.query("select from test_class");
    Assert.assertEquals(result.stream().count(), 0);

    schema.dropClass("test_class");
  }
}
