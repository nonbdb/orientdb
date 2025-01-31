package com.jetbrains.youtrack.db.internal.core.sql.fetch;

import static org.junit.Assert.assertEquals;

import com.jetbrains.youtrack.db.internal.DbTestBase;
import com.jetbrains.youtrack.db.internal.core.fetch.FetchContext;
import com.jetbrains.youtrack.db.internal.core.fetch.FetchHelper;
import com.jetbrains.youtrack.db.internal.core.fetch.remote.RemoteFetchContext;
import com.jetbrains.youtrack.db.internal.core.fetch.remote.RemoteFetchListener;
import com.jetbrains.youtrack.db.internal.core.record.RecordAbstract;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import org.junit.Test;

public class DepthFetchPlanTest extends DbTestBase {

  @Test
  public void testFetchPlanDepth() {
    db.getMetadata().getSchema().createClass("Test");

    db.begin();
    var doc = ((EntityImpl) db.newEntity("Test"));
    var doc1 = ((EntityImpl) db.newEntity("Test"));
    var doc2 = ((EntityImpl) db.newEntity("Test"));
    doc.field("name", "name");
    db.save(doc);
    db.commit();

    db.begin();
    doc = db.bindToSession(doc);
    doc1.field("name", "name1");
    doc1.field("ref", doc);
    db.save(doc1);
    db.commit();

    db.begin();
    doc1 = db.bindToSession(doc1);
    doc2.field("name", "name2");
    doc2.field("ref", doc1);
    db.save(doc2);
    db.commit();

    doc2 = db.bindToSession(doc2);
    FetchContext context = new RemoteFetchContext();
    var listener = new CountFetchListener();
    FetchHelper.fetch(db,
        doc2, doc2, FetchHelper.buildFetchPlan("ref:1 *:-2"), listener, context, "");

    assertEquals(1, listener.count);
  }

  @Test
  public void testFullDepthFetchPlan() {
    db.getMetadata().getSchema().createClass("Test");

    db.begin();
    var doc = ((EntityImpl) db.newEntity("Test"));
    var doc1 = ((EntityImpl) db.newEntity("Test"));
    var doc2 = ((EntityImpl) db.newEntity("Test"));
    var doc3 = ((EntityImpl) db.newEntity("Test"));
    doc.field("name", "name");
    db.save(doc);
    db.commit();

    db.begin();
    doc = db.bindToSession(doc);
    doc1.field("name", "name1");
    doc1.field("ref", doc);
    db.save(doc1);
    db.commit();

    db.begin();
    doc1 = db.bindToSession(doc1);

    doc2.field("name", "name2");
    doc2.field("ref", doc1);
    db.save(doc2);
    db.commit();

    db.begin();
    doc2 = db.bindToSession(doc2);

    doc3.field("name", "name2");
    doc3.field("ref", doc2);
    db.save(doc3);
    db.commit();

    doc3 = db.bindToSession(doc3);
    FetchContext context = new RemoteFetchContext();
    var listener = new CountFetchListener();
    FetchHelper.fetch(db, doc3, doc3, FetchHelper.buildFetchPlan("[*]ref:-1"), listener, context,
        "");
    assertEquals(3, listener.count);
  }

  private final class CountFetchListener extends RemoteFetchListener {

    public int count;

    @Override
    public boolean requireFieldProcessing() {
      return true;
    }

    @Override
    protected void sendRecord(RecordAbstract iLinked) {
      count++;
    }
  }
}
