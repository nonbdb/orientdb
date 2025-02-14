/*
 *
 *
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.jetbrains.youtrack.db.internal.lucene.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.jetbrains.youtrack.db.api.query.ResultSet;
import com.jetbrains.youtrack.db.api.record.RID;
import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import com.jetbrains.youtrack.db.internal.core.id.RecordId;
import com.jetbrains.youtrack.db.internal.core.index.Index;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class LuceneTransactionCompositeQueryTest extends LuceneBaseTest {

  @Before
  public void init() {

    final SchemaClass c1 = db.createVertexClass("Foo");
    c1.createProperty(db, "name", PropertyType.STRING);
    c1.createProperty(db, "bar", PropertyType.STRING);
    c1.createIndex(db, "Foo.bar", "FULLTEXT", null, null, "LUCENE", new String[]{"bar"});
    c1.createIndex(db, "Foo.name", "NOTUNIQUE", null, null, "SBTREE", new String[]{"name"});
  }

  @Test
  public void testRollback() {

    EntityImpl doc = new EntityImpl("Foo");
    doc.field("name", "Test");
    doc.field("bar", "abc");
    db.begin();
    db.save(doc);

    String query = "select from Foo where name = 'Test' and SEARCH_CLASS(\"abc\") =true ";
    try (ResultSet vertices = db.command(query)) {

      assertThat(vertices).hasSize(1);
    }
    db.rollback();

    query = "select from Foo where name = 'Test' and SEARCH_CLASS(\"abc\") = true ";
    try (ResultSet vertices = db.command(query)) {
      assertThat(vertices).hasSize(0);
    }
  }

  @Test
  public void txRemoveTest() {
    db.begin();

    EntityImpl doc = new EntityImpl("Foo");
    doc.field("name", "Test");
    doc.field("bar", "abc");

    Index index = db.getMetadata().getIndexManagerInternal().getIndex(db, "Foo.bar");

    db.save(doc);

    db.commit();

    db.begin();
    doc = db.bindToSession(doc);
    db.delete(doc);

    String query = "select from Foo where name = 'Test' and  SEARCH_CLASS(\"abc\") = true ";
    try (ResultSet vertices = db.command(query)) {

      Collection coll;
      try (Stream<RID> stream = index.getInternal().getRids(db, "abc")) {
        coll = stream.collect(Collectors.toList());
      }

      assertThat(vertices).hasSize(0);

      Assert.assertEquals(0, coll.size());

      Assert.assertEquals(0, index.getInternal().size(db));
    }
    db.rollback();

    query = "select from Foo where name = 'Test' and SEARCH_CLASS(\"abc\") = true ";
    try (ResultSet vertices = db.command(query)) {

      assertThat(vertices).hasSize(1);

      db.begin();
      Assert.assertEquals(1, index.getInternal().size(db));
      db.commit();
    }
  }

  @Test
  public void txUpdateTest() {
    Index index = db.getMetadata().getIndexManagerInternal().getIndex(db, "Foo.bar");
    var c1 = db.getMetadata().getSchema().getClassInternal("Foo");
    c1.truncate(db);

    db.begin();
    Assert.assertEquals(0, index.getInternal().size(db));

    EntityImpl doc = new EntityImpl("Foo");
    doc.field("name", "Test");
    doc.field("bar", "abc");

    db.save(doc);

    db.commit();

    db.begin();

    doc = db.bindToSession(doc);
    doc.field("bar", "removed");
    db.save(doc);

    String query = "select from Foo where name = 'Test' and SEARCH_CLASS(\"abc\") =true";
    Collection<?> coll;
    try (ResultSet vertices = db.query(query)) {
      try (Stream<RID> stream = index.getInternal().getRids(db, "abc")) {
        coll = stream.collect(Collectors.toList());
      }

      assertThat(vertices).hasSize(0);
      Assert.assertEquals(0, coll.size());

      Iterator iterator = coll.iterator();
      int i = 0;
      while (iterator.hasNext()) {
        iterator.next();
        i++;
      }
      Assert.assertEquals(0, i);

      Assert.assertEquals(1, index.getInternal().size(db));
    }
    query = "select from Foo where name = 'Test' and SEARCH_CLASS(\"removed\")=true ";
    try (ResultSet vertices = db.query(query)) {
      try (Stream<RID> stream = index.getInternal().getRids(db, "removed")) {
        coll = stream.collect(Collectors.toList());
      }

      assertThat(vertices).hasSize(1);
      Assert.assertEquals(1, coll.size());
    }

    db.rollback();

    query = "select from Foo where name = 'Test' and SEARCH_CLASS (\"abc\")=true ";
    try (ResultSet vertices = db.command(query)) {

      assertThat(vertices).hasSize(1);

      Assert.assertEquals(1, index.getInternal().size(db));
    }
  }

  @Test
  public void txUpdateTestComplex() {
    Index index = db.getMetadata().getIndexManagerInternal().getIndex(db, "Foo.bar");
    var c1 = db.getMetadata().getSchema().getClassInternal("Foo");
    c1.truncate(db);

    db.begin();
    Assert.assertEquals(0, index.getInternal().size(db));

    EntityImpl doc = new EntityImpl("Foo");
    doc.field("name", "Test");
    doc.field("bar", "abc");

    EntityImpl doc1 = new EntityImpl("Foo");
    doc1.field("name", "Test");
    doc1.field("bar", "abc");

    db.save(doc1);
    db.save(doc);

    db.commit();

    db.begin();
    doc = db.bindToSession(doc);
    doc.field("bar", "removed");
    db.save(doc);

    String query = "select from Foo where name = 'Test' and SEARCH_CLASS(\"abc\")=true ";
    Collection coll;
    try (ResultSet vertices = db.query(query)) {
      try (Stream<RID> stream = index.getInternal().getRids(db, "abc")) {
        coll = stream.collect(Collectors.toList());
      }

      assertThat(vertices).hasSize(1);
      Assert.assertEquals(1, coll.size());

      Iterator iterator = coll.iterator();
      int i = 0;
      RecordId rid = null;
      while (iterator.hasNext()) {
        rid = (RecordId) iterator.next();
        i++;
      }

      Assert.assertEquals(1, i);
      Assert.assertNotNull(rid);
      Assert.assertEquals(rid.getIdentity().toString(), doc1.getIdentity().toString());
      Assert.assertEquals(2, index.getInternal().size(db));
    }

    query = "select from Foo where name = 'Test' and SEARCH_CLASS(\"removed\" )=true";
    try (ResultSet vertices = db.query(query)) {
      try (Stream<RID> stream = index.getInternal().getRids(db, "removed")) {
        coll = stream.collect(Collectors.toList());
      }

      assertThat(vertices).hasSize(1);

      Assert.assertEquals(1, coll.size());
    }
    db.rollback();

    query = "select from Foo where name = 'Test' and SEARCH_CLASS(\"abc\")=true ";
    try (ResultSet vertices = db.query(query)) {
      assertThat(vertices).hasSize(2);
      Assert.assertEquals(2, index.getInternal().size(db));
    }
  }
}
