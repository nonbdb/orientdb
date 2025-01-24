/*
 *
 *
 *  *
 *  *  Licensed under the Apache License, Version 2.0 (the "License");
 *  *  you may not use this file except in compliance with the License.
 *  *  You may obtain a copy of the License at
 *  *
 *  *       http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *  Unless required by applicable law or agreed to in writing, software
 *  *  distributed under the License is distributed on an "AS IS" BASIS,
 *  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  See the License for the specific language governing permissions and
 *  *  limitations under the License.
 *  *
 *
 */

package com.jetbrains.youtrack.db.internal.core.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.jetbrains.youtrack.db.api.query.ResultSet;
import com.jetbrains.youtrack.db.api.record.Entity;
import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import com.jetbrains.youtrack.db.internal.DbTestBase;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class TxNonUniqueIndexWithCollationTest extends DbTestBase {

  @Before
  public void beforeTest() throws Exception {
    super.beforeTest();
    db.getMetadata()
        .getSchema()
        .createClass("user")
        .createProperty(db, "name", PropertyType.STRING)
        .setCollate(db, "ci")
        .createIndex(db, SchemaClass.INDEX_TYPE.NOTUNIQUE);

    db.begin();
    Entity user = db.newEntity("user");
    user.setProperty("name", "abc");
    db.save(user);
    user = db.newEntity("user");
    user.setProperty("name", "aby");
    db.save(user);
    user = db.newEntity("user");
    user.setProperty("name", "aby");
    db.save(user);
    user = db.newEntity("user");
    user.setProperty("name", "abz");
    db.save(user);
    db.commit();
  }

  @Test
  public void testSubstrings() {
    db.begin();

    db.command("update user set name='abd' where name='Aby'").close();

    final ResultSet r = db.command("select * from user where name like '%B%' order by name");
    assertEquals("abc", r.next().getProperty("name"));
    assertEquals("abd", r.next().getProperty("name"));
    assertEquals("abd", r.next().getProperty("name"));
    assertEquals("abz", r.next().getProperty("name"));
    assertFalse(r.hasNext());
    r.close();

    db.commit();
  }

  @Test
  public void testRange() {
    db.begin();

    db.command("update user set name='Abd' where name='Aby'").close();

    final ResultSet r = db.command("select * from user where name >= 'abd' order by name");
    assertEquals("Abd", r.next().getProperty("name"));
    assertEquals("Abd", r.next().getProperty("name"));
    assertEquals("abz", r.next().getProperty("name"));
    assertFalse(r.hasNext());
    r.close();

    db.commit();
  }

  @Test
  public void testIn() {
    db.begin();

    db.command("update user set name='abd' where name='Aby'").close();

    final List<EntityImpl> r =
        db.query("select * from user where name in ['Abc', 'Abd', 'Abz'] order by name").stream()
            .map(x -> ((EntityImpl) (x.asEntity())))
            .toList();
    assertEquals(4, r.size());
    assertEquals("abc", r.get(0).field("name"));
    assertEquals("abd", r.get(1).field("name"));
    assertEquals("abd", r.get(2).field("name"));
    assertEquals("abz", r.get(3).field("name"));

    db.commit();
  }
}
