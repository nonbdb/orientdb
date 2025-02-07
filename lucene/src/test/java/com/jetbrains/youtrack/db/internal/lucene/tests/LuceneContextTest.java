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

import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.api.query.ResultSet;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class LuceneContextTest extends LuceneBaseTest {

  @Before
  public void init() {
    InputStream stream = ClassLoader.getSystemResourceAsStream("testLuceneIndex.sql");

    db.execute("sql", getScriptFromStream(stream));

    db.command("create index Song.title on Song (title) FULLTEXT ENGINE LUCENE");
  }

  @Test
  public void shouldReturnScore() {

    ResultSet docs =
        db.query("select *,$score from Song where search_index('Song.title', 'title:man')= true ");

    List<Result> results = docs.stream().collect(Collectors.toList());

    assertThat(results).hasSize(14);
    Float latestScore = 100f;

    // results are ordered by score desc
    for (Result doc : results) {
      Float score = doc.getProperty("$score");
      assertThat(score).isNotNull().isLessThanOrEqualTo(latestScore);
      latestScore = score;
    }
    docs.close();
  }

  @Test
  public void shouldReturnTotalHits() throws Exception {
    ResultSet docs =
        db.query(
            "select *,$totalHits,$Song_title_totalHits from Song where search_class('title:man')="
                + " true  limit 1");

    List<Result> results = docs.stream().collect(Collectors.toList());
    assertThat(results).hasSize(1);

    Result doc = results.get(0);
    System.out.println("doc.toEntity().toJSON() = " + doc.toEntity().toJSON());

    assertThat(doc.<Long>getProperty("$totalHits")).isEqualTo(14L);
    assertThat(doc.<Long>getProperty("$Song_title_totalHits")).isEqualTo(14L);
    docs.close();
  }
}
