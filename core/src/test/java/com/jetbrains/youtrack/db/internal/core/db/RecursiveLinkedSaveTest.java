package com.jetbrains.youtrack.db.internal.core.db;

import static org.junit.Assert.assertEquals;

import com.jetbrains.youtrack.db.internal.DbTestBase;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import org.junit.Test;

/**
 *
 */
public class RecursiveLinkedSaveTest extends DbTestBase {

  @Test
  public void testTxLinked() {
    db.getMetadata().getSchema().createClass("Test");
    db.begin();
    var doc = (EntityImpl) db.newEntity("Test");
    var doc1 = (EntityImpl) db.newEntity("Test");
    doc.field("link", doc1);
    var doc2 = (EntityImpl) db.newEntity("Test");
    doc1.field("link", doc2);
    doc2.field("link", doc);
    db.save(doc);
    db.commit();
    assertEquals(3, db.countClass("Test"));
    doc = db.load(doc.getIdentity());
    doc1 = doc.field("link");
    doc2 = doc1.field("link");
    assertEquals(doc, doc2.field("link"));
  }
}
