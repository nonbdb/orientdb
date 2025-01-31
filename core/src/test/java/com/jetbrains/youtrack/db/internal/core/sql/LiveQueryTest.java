/*
 *
 *  *  Copyright YouTrackDB
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
 *
 */
package com.jetbrains.youtrack.db.internal.core.sql;

import com.jetbrains.youtrack.db.api.DatabaseSession;
import com.jetbrains.youtrack.db.api.config.YouTrackDBConfig;
import com.jetbrains.youtrack.db.api.exception.BaseException;
import com.jetbrains.youtrack.db.api.query.LiveQueryMonitor;
import com.jetbrains.youtrack.db.api.query.LiveQueryResultListener;
import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.api.query.ResultSet;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.api.record.RID;
import com.jetbrains.youtrack.db.api.schema.Schema;
import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.YouTrackDBImpl;
import com.jetbrains.youtrack.db.internal.core.db.record.RecordOperation;
import com.jetbrains.youtrack.db.internal.core.sql.query.LiveResultListener;
import com.jetbrains.youtrack.db.internal.core.storage.Storage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class LiveQueryTest {

  private YouTrackDBImpl odb;
  private DatabaseSessionInternal db;

  @Before
  public void before() {
    odb = new YouTrackDBImpl("memory:", YouTrackDBConfig.defaultConfig());
    odb.execute(
        "create database LiveQueryTest memory users ( admin identified by 'admin' role admin,"
            + " reader identified by 'reader' role reader)");
    db = (DatabaseSessionInternal) odb.open("LiveQueryTest", "admin", "admin");
  }

  @After
  public void after() {
    db.close();
    odb.drop("LiveQueryTest");
    odb.close();
  }

  class MyLiveQueryListener implements LiveResultListener, LiveQueryResultListener {

    public CountDownLatch latch;

    public MyLiveQueryListener(CountDownLatch latch) {
      this.latch = latch;
    }

    public List<RecordOperation> ops = new ArrayList<RecordOperation>();
    public List<Result> created = new ArrayList<Result>();

    @Override
    public void onLiveResult(DatabaseSessionInternal db, int iLiveToken, RecordOperation iOp)
        throws BaseException {
      ops.add(iOp);
      latch.countDown();
    }

    @Override
    public void onError(int iLiveToken) {
    }

    @Override
    public void onUnsubscribe(int iLiveToken) {
    }

    @Override
    public void onCreate(DatabaseSessionInternal database, Result data) {
      created.add(data);
      latch.countDown();
    }

    @Override
    public void onUpdate(DatabaseSessionInternal database, Result before, Result after) {
    }

    @Override
    public void onDelete(DatabaseSessionInternal database, Result data) {
    }

    @Override
    public void onError(DatabaseSession database, BaseException exception) {
    }

    @Override
    public void onEnd(DatabaseSession database) {
    }
  }

  @Test
  public void testLiveInsert() throws InterruptedException {

    db.getMetadata().getSchema().createClass("test");
    db.getMetadata().getSchema().createClass("test2");
    var listener = new MyLiveQueryListener(new CountDownLatch(2));

    var tokens = db.live("live select from test", listener);
    Integer token = tokens.getMonitorId();
    Assert.assertNotNull(token);

    db.begin();
    db.command("insert into test set name = 'foo', surname = 'bar'").close();
    db.command("insert into test set name = 'foo', surname = 'baz'").close();
    db.command("insert into test2 set name = 'foo'").close();
    db.commit();

    Assert.assertTrue(listener.latch.await(1, TimeUnit.MINUTES));

    tokens.unSubscribe();

    db.begin();
    db.command("insert into test set name = 'foo', surname = 'bax'").close();
    db.command("insert into test2 set name = 'foo'").close();
    db.command("insert into test set name = 'foo', surname = 'baz'").close();
    db.commit();

    Assert.assertEquals(2, listener.created.size());
    for (var res : listener.created) {
      Assert.assertEquals("foo", res.getProperty("name"));
    }
  }

  @Test
  public void testLiveInsertOnCluster() {

    var clazz = db.getMetadata().getSchema().createClass("test");

    var defaultCluster = clazz.getClusterIds()[0];
    final var storage = db.getStorage();

    var listener = new MyLiveQueryListener(new CountDownLatch(1));

    db.live("live select from cluster:" + storage.getClusterNameById(defaultCluster), listener);

    db.begin();
    db.command(
            "insert into cluster:"
                + storage.getClusterNameById(defaultCluster)
                + " set name = 'foo', surname = 'bar'")
        .close();
    db.commit();

    try {
      Assert.assertTrue(listener.latch.await(1, TimeUnit.MINUTES));
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    Assert.assertEquals(1, listener.created.size());
    for (var doc : listener.created) {
      Assert.assertEquals("foo", doc.getProperty("name"));
      RID rid = doc.getProperty("@rid");
      Assert.assertNotNull(rid);
      Assert.assertTrue(rid.getClusterPosition() >= 0);
    }
  }

  @Test
  public void testRestrictedLiveInsert() throws ExecutionException, InterruptedException {

    Schema schema = db.getMetadata().getSchema();
    var oRestricted = schema.getClass("ORestricted");
    schema.createClass("test", oRestricted);

    var liveMatch = 2;
    var query = db.query("select from OUSer where name = 'reader'");

    final Identifiable reader = query.next().getIdentity().get();
    final var current = db.geCurrentUser().getIdentity();

    var executorService = Executors.newSingleThreadExecutor();

    final var latch = new CountDownLatch(1);
    final var dataArrived = new CountDownLatch(2);
    var future =
        executorService.submit(
            new Callable<Integer>() {
              @Override
              public Integer call() throws Exception {
                var otherDb = odb.open("LiveQueryTest", "reader", "reader");

                final var integer = new AtomicInteger(0);
                try {
                  otherDb.live(
                      "live select from test",
                      new LiveQueryResultListener() {

                        @Override
                        public void onCreate(DatabaseSessionInternal database, Result data) {
                          integer.incrementAndGet();
                          dataArrived.countDown();
                        }

                        @Override
                        public void onUpdate(
                            DatabaseSessionInternal database, Result before, Result after) {
                        }

                        @Override
                        public void onDelete(DatabaseSessionInternal database, Result data) {
                        }

                        @Override
                        public void onError(DatabaseSession database, BaseException exception) {
                        }

                        @Override
                        public void onEnd(DatabaseSession database) {
                        }
                      });
                } catch (RuntimeException e) {
                  e.printStackTrace();
                }

                latch.countDown();
                Assert.assertTrue(dataArrived.await(1, TimeUnit.MINUTES));
                return integer.get();
              }
            });

    latch.await();

    db.begin();
    db.command("insert into test set name = 'foo', surname = 'bar'").close();

    db.command(
            "insert into test set name = 'foo', surname = 'bar', _allow=?",
            new ArrayList<Identifiable>() {
              {
                add(current);
                add(reader);
              }
            })
        .close();
    db.commit();

    var integer = future.get();
    Assert.assertEquals(liveMatch, integer.intValue());
  }
}
