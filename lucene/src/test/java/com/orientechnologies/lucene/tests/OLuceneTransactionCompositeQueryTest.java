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

package com.orientechnologies.lucene.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.orientechnologies.orient.core.id.YTRID;
import com.orientechnologies.orient.core.id.YTRecordId;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.metadata.schema.YTClass;
import com.orientechnologies.orient.core.metadata.schema.YTType;
import com.orientechnologies.orient.core.record.impl.YTEntityImpl;
import com.orientechnologies.orient.core.sql.executor.YTResultSet;
import java.io.IOException;
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
public class OLuceneTransactionCompositeQueryTest extends OLuceneBaseTest {

  @Before
  public void init() {

    final YTClass c1 = db.createVertexClass("Foo");
    c1.createProperty(db, "name", YTType.STRING);
    c1.createProperty(db, "bar", YTType.STRING);
    c1.createIndex(db, "Foo.bar", "FULLTEXT", null, null, "LUCENE", new String[]{"bar"});
    c1.createIndex(db, "Foo.name", "NOTUNIQUE", null, null, "SBTREE", new String[]{"name"});
  }

  @Test
  public void testRollback() {

    YTEntityImpl doc = new YTEntityImpl("Foo");
    doc.field("name", "Test");
    doc.field("bar", "abc");
    db.begin();
    db.save(doc);

    String query = "select from Foo where name = 'Test' and SEARCH_CLASS(\"abc\") =true ";
    try (YTResultSet vertices = db.command(query)) {

      assertThat(vertices).hasSize(1);
    }
    db.rollback();

    query = "select from Foo where name = 'Test' and SEARCH_CLASS(\"abc\") = true ";
    try (YTResultSet vertices = db.command(query)) {
      assertThat(vertices).hasSize(0);
    }
  }

  @Test
  public void txRemoveTest() {
    db.begin();

    YTEntityImpl doc = new YTEntityImpl("Foo");
    doc.field("name", "Test");
    doc.field("bar", "abc");

    OIndex index = db.getMetadata().getIndexManagerInternal().getIndex(db, "Foo.bar");

    db.save(doc);

    db.commit();

    db.begin();
    doc = db.bindToSession(doc);
    db.delete(doc);

    String query = "select from Foo where name = 'Test' and  SEARCH_CLASS(\"abc\") = true ";
    try (YTResultSet vertices = db.command(query)) {

      Collection coll;
      try (Stream<YTRID> stream = index.getInternal().getRids(db, "abc")) {
        coll = stream.collect(Collectors.toList());
      }

      assertThat(vertices).hasSize(0);

      Assert.assertEquals(coll.size(), 0);

      Assert.assertEquals(index.getInternal().size(db), 0);
    }
    db.rollback();

    query = "select from Foo where name = 'Test' and SEARCH_CLASS(\"abc\") = true ";
    try (YTResultSet vertices = db.command(query)) {

      assertThat(vertices).hasSize(1);

      db.begin();
      Assert.assertEquals(index.getInternal().size(db), 1);
      db.commit();
    }
  }

  @Test
  public void txUpdateTest() {

    OIndex index = db.getMetadata().getIndexManagerInternal().getIndex(db, "Foo.bar");
    YTClass c1 = db.getMetadata().getSchema().getClass("Foo");
    try {
      c1.truncate(db);
    } catch (IOException e) {
      e.printStackTrace();
    }

    db.begin();
    Assert.assertEquals(index.getInternal().size(db), 0);

    YTEntityImpl doc = new YTEntityImpl("Foo");
    doc.field("name", "Test");
    doc.field("bar", "abc");

    db.save(doc);

    db.commit();

    db.begin();

    doc = db.bindToSession(doc);
    doc.field("bar", "removed");
    db.save(doc);

    String query = "select from Foo where name = 'Test' and SEARCH_CLASS(\"abc\") =true";
    Collection coll;
    try (YTResultSet vertices = db.query(query)) {
      try (Stream<YTRID> stream = index.getInternal().getRids(db, "abc")) {
        coll = stream.collect(Collectors.toList());
      }

      assertThat(vertices).hasSize(0);
      Assert.assertEquals(coll.size(), 0);

      Iterator iterator = coll.iterator();
      int i = 0;
      while (iterator.hasNext()) {
        iterator.next();
        i++;
      }
      Assert.assertEquals(i, 0);

      Assert.assertEquals(index.getInternal().size(db), 1);
    }
    query = "select from Foo where name = 'Test' and SEARCH_CLASS(\"removed\")=true ";
    try (YTResultSet vertices = db.query(query)) {
      try (Stream<YTRID> stream = index.getInternal().getRids(db, "removed")) {
        coll = stream.collect(Collectors.toList());
      }

      assertThat(vertices).hasSize(1);
      Assert.assertEquals(coll.size(), 1);
    }

    db.rollback();

    query = "select from Foo where name = 'Test' and SEARCH_CLASS (\"abc\")=true ";
    try (YTResultSet vertices = db.command(query)) {

      assertThat(vertices).hasSize(1);

      Assert.assertEquals(index.getInternal().size(db), 1);
    }
  }

  @Test
  public void txUpdateTestComplex() {
    OIndex index = db.getMetadata().getIndexManagerInternal().getIndex(db, "Foo.bar");
    YTClass c1 = db.getMetadata().getSchema().getClass("Foo");
    try {
      c1.truncate(db);
    } catch (IOException e) {
      e.printStackTrace();
    }

    db.begin();
    Assert.assertEquals(index.getInternal().size(db), 0);

    YTEntityImpl doc = new YTEntityImpl("Foo");
    doc.field("name", "Test");
    doc.field("bar", "abc");

    YTEntityImpl doc1 = new YTEntityImpl("Foo");
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
    try (YTResultSet vertices = db.query(query)) {
      try (Stream<YTRID> stream = index.getInternal().getRids(db, "abc")) {
        coll = stream.collect(Collectors.toList());
      }

      assertThat(vertices).hasSize(1);
      Assert.assertEquals(1, coll.size());

      Iterator iterator = coll.iterator();
      int i = 0;
      YTRecordId rid = null;
      while (iterator.hasNext()) {
        rid = (YTRecordId) iterator.next();
        i++;
      }

      Assert.assertEquals(1, i);
      Assert.assertNotNull(rid);
      Assert.assertEquals(rid.getIdentity().toString(), doc1.getIdentity().toString());
      Assert.assertEquals(2, index.getInternal().size(db));
    }

    query = "select from Foo where name = 'Test' and SEARCH_CLASS(\"removed\" )=true";
    try (YTResultSet vertices = db.query(query)) {
      try (Stream<YTRID> stream = index.getInternal().getRids(db, "removed")) {
        coll = stream.collect(Collectors.toList());
      }

      assertThat(vertices).hasSize(1);

      Assert.assertEquals(1, coll.size());
    }
    db.rollback();

    query = "select from Foo where name = 'Test' and SEARCH_CLASS(\"abc\")=true ";
    try (YTResultSet vertices = db.query(query)) {
      assertThat(vertices).hasSize(2);
      Assert.assertEquals(2, index.getInternal().size(db));
    }
  }
}
