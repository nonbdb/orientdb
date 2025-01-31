package com.jetbrains.youtrack.db.internal.core.db.hook;

import static org.junit.Assert.assertEquals;

import com.jetbrains.youtrack.db.api.DatabaseSession;
import com.jetbrains.youtrack.db.api.record.DBRecord;
import com.jetbrains.youtrack.db.api.record.RecordHook;
import com.jetbrains.youtrack.db.internal.DbTestBase;
import com.jetbrains.youtrack.db.internal.core.metadata.security.SecurityPolicy;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import org.junit.Test;

public class HookReadTest extends DbTestBase {

  @Test
  public void testSelectChangedInHook() {
    db.registerHook(
        new RecordHook() {
          @Override
          public void onUnregister() {
          }

          @Override
          public RESULT onTrigger(DatabaseSession db, TYPE iType, DBRecord iRecord) {
            if (iType == TYPE.AFTER_READ
                && !((EntityImpl) iRecord)
                .getClassName()
                .equalsIgnoreCase(SecurityPolicy.class.getSimpleName())) {
              ((EntityImpl) iRecord).field("read", "test");
            }
            return RESULT.RECORD_CHANGED;
          }

          @Override
          public DISTRIBUTED_EXECUTION_MODE getDistributedExecutionMode() {
            return null;
          }
        });

    db.getMetadata().getSchema().createClass("TestClass");
    db.begin();
    db.save(db.newEntity("TestClass"));
    db.commit();

    db.begin();
    var res = db.query("select from TestClass");
    assertEquals("test", res.next().getProperty("read"));
    db.commit();
  }
}
