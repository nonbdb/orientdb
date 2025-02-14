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
import java.io.InputStream;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class LuceneMixIndexTest extends LuceneBaseTest {

  @Before
  public void initLocal() {

    InputStream stream = ClassLoader.getSystemResourceAsStream("testLuceneIndex.sql");

    db.execute("sql", getScriptFromStream(stream));

    db.command("create index Song.author on Song (author) NOTUNIQUE");

    db.command("create index Song.composite on Song (title,lyrics) FULLTEXT ENGINE LUCENE");
  }

  @Test
  public void testMixQuery() {

    ResultSet docs =
        db.query(
            "select * from Song where  author = 'Hornsby' and"
                + " search_index('Song.composite','title:mountain')=true ");

    assertThat(docs).hasSize(1);
    docs.close();
    docs =
        db.query(
            "select * from Song where  author = 'Hornsby' and"
                + " search_index('Song.composite','title:ballad')=true");
    assertThat(docs).hasSize(0);
    docs.close();
  }

  @Test
  public void testMixCompositeQuery() {

    ResultSet docs =
        db.query(
            "select * from Song where  author = 'Hornsby' and"
                + " search_index('Song.composite','title:mountain')=true ");
    assertThat(docs).hasSize(1);
    docs.close();
    docs =
        db.query(
            "select * from Song where author = 'Hornsby' and"
                + " search_index('Song.composite','lyrics:happy')=true ");

    assertThat(docs).hasSize(1);
    docs.close();
  }
}
