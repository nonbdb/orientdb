package com.orientechnologies.orient.core.db.hook;

import static org.junit.Assert.assertEquals;

import com.orientechnologies.DBTestBase;
import com.orientechnologies.orient.core.exception.YTValidationException;
import com.orientechnologies.orient.core.hook.YTDocumentHookAbstract;
import com.orientechnologies.orient.core.metadata.schema.YTClass;
import com.orientechnologies.orient.core.metadata.schema.YTSchema;
import com.orientechnologies.orient.core.metadata.schema.YTType;
import com.orientechnologies.orient.core.record.impl.YTEntityImpl;
import org.junit.Assert;
import org.junit.Test;

public class HookChangeValidationTest extends DBTestBase {

  @Test
  public void testHookCreateChangeTx() {

    YTSchema schema = db.getMetadata().getSchema();
    YTClass classA = schema.createClass("TestClass");
    classA.createProperty(db, "property1", YTType.STRING).setNotNull(db, true);
    classA.createProperty(db, "property2", YTType.STRING).setReadonly(db, true);
    classA.createProperty(db, "property3", YTType.STRING).setMandatory(db, true);
    db.registerHook(
        new YTDocumentHookAbstract() {
          @Override
          public RESULT onRecordBeforeCreate(YTEntityImpl doc) {
            doc.removeField("property1");
            doc.removeField("property2");
            doc.removeField("property3");
            return RESULT.RECORD_CHANGED;
          }

          @Override
          public RESULT onRecordBeforeUpdate(YTEntityImpl doc) {
            return RESULT.RECORD_NOT_CHANGED;
          }

          @Override
          public DISTRIBUTED_EXECUTION_MODE getDistributedExecutionMode() {
            return DISTRIBUTED_EXECUTION_MODE.SOURCE_NODE;
          }
        });
    YTEntityImpl doc = new YTEntityImpl(classA);
    doc.field("property1", "value1-create");
    doc.field("property2", "value2-create");
    doc.field("property3", "value3-create");
    try {
      db.begin();
      doc.save();
      db.commit();
      Assert.fail("The document save should fail for validation exception");
    } catch (YTValidationException ex) {

    }
  }

  @Test
  public void testHookUpdateChangeTx() {

    YTSchema schema = db.getMetadata().getSchema();
    YTClass classA = schema.createClass("TestClass");
    classA.createProperty(db, "property1", YTType.STRING).setNotNull(db, true);
    classA.createProperty(db, "property2", YTType.STRING).setReadonly(db, true);
    classA.createProperty(db, "property3", YTType.STRING).setMandatory(db, true);
    db.registerHook(
        new YTDocumentHookAbstract() {
          @Override
          public RESULT onRecordBeforeCreate(YTEntityImpl doc) {
            return RESULT.RECORD_NOT_CHANGED;
          }

          @Override
          public RESULT onRecordBeforeUpdate(YTEntityImpl doc) {
            doc.removeField("property1");
            doc.removeField("property2");
            doc.removeField("property3");
            return RESULT.RECORD_CHANGED;
          }

          @Override
          public DISTRIBUTED_EXECUTION_MODE getDistributedExecutionMode() {
            return DISTRIBUTED_EXECUTION_MODE.SOURCE_NODE;
          }
        });

    db.begin();
    YTEntityImpl doc = new YTEntityImpl(classA);
    doc.field("property1", "value1-create");
    doc.field("property2", "value2-create");
    doc.field("property3", "value3-create");
    doc.save();
    db.commit();

    db.begin();
    doc = db.bindToSession(doc);
    assertEquals("value1-create", doc.field("property1"));
    assertEquals("value2-create", doc.field("property2"));
    assertEquals("value3-create", doc.field("property3"));

    doc.field("property1", "value1-update");
    doc.field("property2", "value2-update");
    try {
      doc.save();
      db.commit();
      Assert.fail("The document save should fail for validation exception");
    } catch (YTValidationException ex) {

    }
  }
}
