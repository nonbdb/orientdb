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

import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class LuceneSkipLimitTest extends BaseLuceneTest {

  @Test
  public void testContext() {

    var docs =
        session.query("select * from Song where [title] LUCENE \"(title:man)\"").stream()
            .map((r) -> r.getIdentity().get())
            .collect(Collectors.toList());

    Assert.assertEquals(docs.size(), 14);

    var doc = docs.get(9);
    docs =
        session
            .query("select * from Song where [title] LUCENE \"(title:man)\" skip 10 limit 10")
            .stream()
            .map((r) -> r.getIdentity().get())
            .collect(Collectors.toList());

    Assert.assertEquals(docs.size(), 4);

    Assert.assertFalse(docs.contains(doc));

    docs =
        session
            .query("select * from Song where [title] LUCENE \"(title:man)\" skip 14 limit 10")
            .stream()
            .map((r) -> r.getIdentity().get())
            .collect(Collectors.toList());

    Assert.assertEquals(docs.size(), 0);
  }

  @Before
  public void init() {
    var stream = ClassLoader.getSystemResourceAsStream("testLuceneIndex.sql");

    session.execute("sql", getScriptFromStream(stream)).close();

    session.command("create index Song.title on Song (title) FULLTEXT ENGINE LUCENE").close();
    session.command("create index Song.author on Song (author) FULLTEXT ENGINE LUCENE").close();
  }
}
