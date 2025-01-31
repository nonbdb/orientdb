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

import static com.jetbrains.youtrack.db.api.schema.SchemaClass.INDEX_TYPE.FULLTEXT;
import static org.assertj.core.api.Assertions.assertThat;

import com.jetbrains.youtrack.db.internal.core.command.CommandOutputListener;
import com.jetbrains.youtrack.db.internal.core.db.tool.DatabaseExport;
import com.jetbrains.youtrack.db.internal.core.db.tool.DatabaseImport;
import com.jetbrains.youtrack.db.internal.core.index.Index;
import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import com.jetbrains.youtrack.db.api.schema.Schema;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.api.query.ResultSet;
import com.jetbrains.youtrack.db.internal.lucene.LuceneIndexFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class LuceneExportImportTest extends BaseLuceneTest {

  @Before
  public void init() {

    Schema schema = db.getMetadata().getSchema();
    var oClass = schema.createClass("City");

    oClass.createProperty(db, "name", PropertyType.STRING);
    db.command("create index City.name on City (name) FULLTEXT ENGINE LUCENE").close();

    var doc = ((EntityImpl) db.newEntity("City"));
    doc.field("name", "Rome");

    db.begin();
    db.save(doc);
    db.commit();
  }

  @Test
  public void testExportImport() {

    var file = "./target/exportTest.json";

    var query = db.query("select from City where name lucene 'Rome'");

    Assert.assertEquals(query.stream().count(), 1);

    try {

      // export
      new DatabaseExport(
          db,
          file,
          new CommandOutputListener() {
            @Override
            public void onMessage(String s) {
            }
          })
          .exportDatabase();

      dropDatabase();

      createDatabase();

      db = openDatabase();

      var stream = new GZIPInputStream(new FileInputStream(file + ".gz"));
      new DatabaseImport(
          db,
          stream,
          new CommandOutputListener() {
            @Override
            public void onMessage(String s) {
            }
          })
          .importDatabase();
    } catch (IOException e) {
      Assert.fail(e.getMessage());
    }

    assertThat(db.countClass("City")).isEqualTo(1);
    var index = db.getMetadata().getIndexManagerInternal().getIndex(db, "City.name");

    assertThat(index.getType()).isEqualTo(FULLTEXT.toString());

    assertThat(index.getAlgorithm()).isEqualTo(LuceneIndexFactory.LUCENE_ALGORITHM);

    // redo the query
    query = db.query("select from City where name lucene 'Rome'");

    assertThat(query).hasSize(1);
  }
}
