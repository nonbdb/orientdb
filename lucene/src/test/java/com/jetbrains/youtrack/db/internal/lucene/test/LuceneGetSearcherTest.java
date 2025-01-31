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

import com.jetbrains.youtrack.db.internal.core.index.Index;
import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import com.jetbrains.youtrack.db.api.schema.Schema;
import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.internal.lucene.index.LuceneIndexNotUnique;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class LuceneGetSearcherTest extends BaseLuceneTest {

  @Before
  public void init() {
    Schema schema = db.getMetadata().getSchema();
    var v = schema.getClass("V");
    var song = schema.createClass("Person");
    song.setSuperClass(db, v);
    song.createProperty(db, "isDeleted", PropertyType.BOOLEAN);

    db.command("create index Person.isDeleted on Person (isDeleted) FULLTEXT ENGINE LUCENE")
        .close();
  }

  @Test
  public void testSearcherInstance() {

    var index = db.getMetadata().getIndexManagerInternal().getIndex(db, "Person.isDeleted");

    Assert.assertTrue(index.getInternal() instanceof LuceneIndexNotUnique);

    var idx = (LuceneIndexNotUnique) index.getInternal();

    Assert.assertNotNull(idx.searcher());
  }
}
