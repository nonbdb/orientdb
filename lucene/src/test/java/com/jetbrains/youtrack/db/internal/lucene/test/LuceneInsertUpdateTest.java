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

package com.jetbrains.youtrack.db.internal.lucene.test;

import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.api.record.RID;
import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.api.schema.Schema;
import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import com.jetbrains.youtrack.db.internal.core.index.Index;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class LuceneInsertUpdateTest extends BaseLuceneTest {

  public LuceneInsertUpdateTest() {
    super();
  }

  @Before
  public void init() {

    Schema schema = db.getMetadata().getSchema();
    var oClass = schema.createClass("City");

    oClass.createProperty(db, "name", PropertyType.STRING);
    db.command("create index City.name on City (name) FULLTEXT ENGINE LUCENE").close();
  }

  @Test
  public void testInsertUpdateWithIndex() {

    Schema schema = db.getMetadata().getSchema();

    db.begin();
    var doc = ((EntityImpl) db.newEntity("City"));
    doc.field("name", "Rome");

    db.save(doc);
    db.commit();

    db.begin();
    var idx = db.getClassInternal("City").getClassIndex(db, "City.name");
    Collection<?> coll;
    try (var stream = idx.getInternal().getRids(db, "Rome")) {
      coll = stream.collect(Collectors.toList());
    }
    Assert.assertEquals(coll.size(), 1);

    var next = (Identifiable) coll.iterator().next();
    doc = db.load(next.getIdentity());
    Assert.assertEquals(doc.field("name"), "Rome");

    doc.field("name", "London");

    db.save(doc);
    db.commit();

    db.begin();
    try (var stream = idx.getInternal().getRids(db, "Rome")) {
      coll = stream.collect(Collectors.toList());
    }
    Assert.assertEquals(coll.size(), 0);
    try (var stream = idx.getInternal().getRids(db, "London")) {
      coll = stream.collect(Collectors.toList());
    }
    Assert.assertEquals(coll.size(), 1);

    next = (Identifiable) coll.iterator().next();
    doc = db.load(next.getIdentity());
    Assert.assertEquals(doc.field("name"), "London");

    doc.field("name", "Berlin");

    db.save(doc);
    db.commit();

    try (var stream = idx.getInternal().getRids(db, "Rome")) {
      coll = stream.collect(Collectors.toList());
    }
    Assert.assertEquals(coll.size(), 0);
    try (var stream = idx.getInternal().getRids(db, "London")) {
      coll = stream.collect(Collectors.toList());
    }
    Assert.assertEquals(coll.size(), 0);
    try (var stream = idx.getInternal().getRids(db, "Berlin")) {
      coll = stream.collect(Collectors.toList());
    }
    Assert.assertEquals(coll.size(), 1);
  }
}
