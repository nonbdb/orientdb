package com.jetbrains.youtrack.db.internal.core.db.hook;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.jetbrains.youtrack.db.api.DatabaseSession;
import com.jetbrains.youtrack.db.api.record.DBRecord;
import com.jetbrains.youtrack.db.api.record.RecordHook;
import com.jetbrains.youtrack.db.internal.DbTestBase;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import org.junit.Test;

/**
 *
 */
public class HookSaveTest extends DbTestBase {

  @Test
  public void testCreatedLinkedInHook() {
    db.registerHook(
        new RecordHook() {
          @Override
          public void onUnregister() {
          }

          @Override
          public RESULT onTrigger(DatabaseSession db, TYPE iType, DBRecord iRecord) {
            if (iType != TYPE.BEFORE_CREATE) {
              return RESULT.RECORD_NOT_CHANGED;
            }
            var doc = (EntityImpl) iRecord;
            if (doc.containsField("test")) {
              return RESULT.RECORD_NOT_CHANGED;
            }
            var doc1 = (EntityImpl) HookSaveTest.this.db.newEntity("test");
            doc1.field("test", "value");
            doc.field("testNewLinkedRecord", doc1);
            return RESULT.RECORD_CHANGED;
          }

          @Override
          public DISTRIBUTED_EXECUTION_MODE getDistributedExecutionMode() {
            return null;
          }
        });

    db.getMetadata().getSchema().createClass("test");
    db.begin();
    EntityImpl doc = db.save(db.newEntity("test"));
    db.commit();

    EntityImpl newRef = db.bindToSession(doc).field("testNewLinkedRecord");
    assertNotNull(newRef);
    assertFalse(newRef.getIdentity().isPersistent());
  }

  @Test
  public void testCreatedBackLinkedInHook() {
    db.registerHook(
        new RecordHook() {
          @Override
          public void onUnregister() {
          }

          @Override
          public RESULT onTrigger(DatabaseSession db, TYPE iType, DBRecord iRecord) {
            if (iType != TYPE.BEFORE_CREATE) {
              return RESULT.RECORD_NOT_CHANGED;
            }
            var doc = (EntityImpl) iRecord;
            if (doc.containsField("test")) {
              return RESULT.RECORD_NOT_CHANGED;
            }
            var doc1 = (EntityImpl) HookSaveTest.this.db.newEntity("test");
            doc1.field("test", "value");
            doc.field("testNewLinkedRecord", doc1);
            doc1.field("backLink", doc);
            return RESULT.RECORD_CHANGED;
          }

          @Override
          public DISTRIBUTED_EXECUTION_MODE getDistributedExecutionMode() {
            return null;
          }
        });

    db.getMetadata().getSchema().createClass("test");
    db.begin();
    EntityImpl doc = db.save(db.newEntity("test"));
    db.commit();

    EntityImpl newRef = db.bindToSession(doc).field("testNewLinkedRecord");
    assertNotNull(newRef);
    assertTrue(newRef.getIdentity().isPersistent());
  }
}
