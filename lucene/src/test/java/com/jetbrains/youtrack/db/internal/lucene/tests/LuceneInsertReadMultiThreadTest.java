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

import com.jetbrains.youtrack.db.api.DatabaseSession;
import com.jetbrains.youtrack.db.api.query.ResultSet;
import com.jetbrains.youtrack.db.api.record.Entity;
import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.api.schema.Schema;
import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import com.jetbrains.youtrack.db.api.session.SessionPool;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.index.Index;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class LuceneInsertReadMultiThreadTest extends LuceneBaseTest {

  private static final int THREADS = 10;
  private static final int RTHREADS = 10;
  private static final int CYCLE = 100;

  @Before
  public void init() {

    Schema schema = db.getMetadata().getSchema();
    SchemaClass oClass = schema.createClass("City");

    oClass.createProperty(db, "name", PropertyType.STRING);
    db.command("create index City.name on City (name) FULLTEXT ENGINE LUCENE");
  }

  @Test
  public void testConcurrentInsertWithIndex() throws Exception {

    List<CompletableFuture<Void>> futures =
        IntStream.range(0, THREADS)
            .boxed()
            .map(i -> CompletableFuture.runAsync(new LuceneInsert(pool, CYCLE)))
            .collect(Collectors.toList());

    futures.addAll(
        IntStream.range(0, 1)
            .boxed()
            .map(i -> CompletableFuture.runAsync(new LuceneReader(pool, CYCLE)))
            .collect(Collectors.toList()));

    futures.forEach(cf -> cf.join());

    DatabaseSessionInternal db1 = (DatabaseSessionInternal) pool.acquire();
    db1.getMetadata().reload();
    var schema = db1.getMetadata().getSchema();

    Index idx = schema.getClassInternal("City").getClassIndex(db, "City.name");

    db1.begin();
    Assert.assertEquals(idx.getInternal().size(db1), THREADS * CYCLE);
    db1.commit();
  }

  public class LuceneInsert implements Runnable {

    private final SessionPool pool;
    private final int cycle;
    private final int commitBuf;

    public LuceneInsert(SessionPool pool, int cycle) {
      this.pool = pool;
      this.cycle = cycle;

      this.commitBuf = cycle / 10;
    }

    @Override
    public void run() {

      final DatabaseSession db = pool.acquire();
      db.activateOnCurrentThread();
      db.begin();
      int i = 0;
      for (; i < cycle; i++) {
        Entity doc = db.newEntity("City");

        doc.setProperty("name", "Rome");

        db.save(doc);
        if (i % commitBuf == 0) {
          db.commit();
          db.begin();
        }
      }
      db.commit();
      db.close();
    }
  }

  public class LuceneReader implements Runnable {

    private final int cycle;
    private final SessionPool pool;

    public LuceneReader(SessionPool pool, int cycle) {
      this.pool = pool;
      this.cycle = cycle;
    }

    @Override
    public void run() {

      final DatabaseSessionInternal db = (DatabaseSessionInternal) pool.acquire();
      db.activateOnCurrentThread();
      var schema = db.getMetadata().getSchema();
      schema.getClassInternal("City").getClassIndex(db, "City.name");

      for (int i = 0; i < cycle; i++) {

        ResultSet resultSet =
            db.query("select from City where SEARCH_FIELDS(['name'], 'Rome') =true ");

        if (resultSet.hasNext()) {
          assertThat(resultSet.next().toEntity().<String>getProperty("name"))
              .isEqualToIgnoringCase("rome");
        }
        resultSet.close();
      }
      db.close();
    }
  }
}
