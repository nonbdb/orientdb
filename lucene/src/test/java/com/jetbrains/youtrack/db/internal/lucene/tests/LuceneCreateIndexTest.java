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

import com.jetbrains.youtrack.db.api.record.Vertex;
import com.jetbrains.youtrack.db.api.query.ResultSet;
import java.io.InputStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class LuceneCreateIndexTest extends LuceneBaseTest {

  @Before
  public void setUp() throws Exception {
    var stream = ClassLoader.getSystemResourceAsStream("testLuceneIndex.sql");

    db.execute("sql", getScriptFromStream(stream)).close();

    db.command(
            "create index Song.title on Song (title) fulltext ENGINE LUCENE METADATA"
                + " {\"analyzer\":\""
                + StandardAnalyzer.class.getName()
                + "\"}")
        .close();
    db.command(
            "create index Song.author on Song (author) FULLTEXT ENGINE lucene METADATA"
                + " {\"analyzer\":\""
                + StandardAnalyzer.class.getName()
                + "\"}")
        .close();

    var doc = db.newVertex("Song");

    doc.setProperty("title", "Local");
    doc.setProperty("author", "Local");

    db.begin();
    db.save(doc);
    db.commit();
  }

  @Test
  public void testMetadata() {
    var index =
        db.getMetadata().getIndexManagerInternal().getIndex(db, "Song.title").getMetadata();

    Assert.assertEquals(index.get("analyzer"), StandardAnalyzer.class.getName());
  }

  @Test
  public void testQueries() {
    var docs = db.query(
        "select * from Song where search_fields(['title'],'mountain')=true");

    assertThat(docs).hasSize(4);
    docs.close();
    docs = db.query("select * from Song where search_fields(['author'],'Fabbio')=true");

    assertThat(docs).hasSize(87);
    docs.close();
    var query =
        "select * from Song where search_fields(['title'],'mountain')=true AND"
            + " search_fields(['author'],'Fabbio')=true";
    docs = db.query(query);
    assertThat(docs).hasSize(1);
    docs.close();
    query =
        "select * from Song where search_fields(['title'],'mountain')=true   and author = 'Fabbio'";
    docs = db.query(query);

    assertThat(docs).hasSize(1);
    docs.close();
  }

  @Test
  public void testQeuryOnAddedDocs() {
    var query = "select * from Song where search_fields(['title'],'local')=true ";
    var docs = db.query(query);

    assertThat(docs).hasSize(1);
    docs.close();
  }
}
