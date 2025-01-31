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

import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.api.record.Vertex;
import com.jetbrains.youtrack.db.api.query.ResultSet;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class LuceneGraphTxTest extends LuceneBaseTest {

  @Before
  public void init() {
    var type = db.createVertexClass("City");
    type.createProperty(db, "name", PropertyType.STRING);

    db.command("create index City.name on City (name) FULLTEXT ENGINE LUCENE");
  }

  @Test
  public void graphTxTest() throws Exception {

    var v = db.newVertex("City");
    v.setProperty("name", "London");

    // save london
    db.begin();
    db.save(v);
    db.commit();

    db.begin();
    var resultSet = db.command("select from City where search_class('London') =true ");

    assertThat(resultSet).hasSize(1);

    v = db.bindToSession(v);
    // modifiy vertex
    v.setProperty("name", "Berlin");

    // re-save

    db.save(v);
    db.commit();

    // only berlin
    resultSet = db.command("select from City where search_class('Berlin') =true ");
    assertThat(resultSet).hasSize(1);

    resultSet = db.command("select from City where search_class('London') =true ");
    assertThat(resultSet).hasSize(0);

    resultSet = db.command("select from City where search_class('Berlin') =true ");
    assertThat(resultSet).hasSize(1);

    resultSet = db.command("select from City where search_class('London') =true ");
    assertThat(resultSet).hasSize(0);
  }
}
