package com.orientechnologies.orient.core.record.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.orientechnologies.DBTestBase;
import com.orientechnologies.orient.core.metadata.schema.YTClass;
import com.orientechnologies.orient.core.metadata.schema.YTProperty;
import com.orientechnologies.orient.core.metadata.schema.YTSchema;
import com.orientechnologies.orient.core.metadata.schema.YTType;
import com.orientechnologies.orient.core.record.ORecordInternal;
import com.orientechnologies.orient.core.sql.executor.YTResult;
import com.orientechnologies.orient.core.util.ODateHelper;
import java.util.Date;
import org.junit.Test;

public class DefaultValueTest extends DBTestBase {

  @Test
  public void testKeepValueSerialization() {
    // create example schema
    YTSchema schema = db.getMetadata().getSchema();
    YTClass classA = schema.createClass("ClassC");

    YTProperty prop = classA.createProperty(db, "name", YTType.STRING);
    prop.setDefaultValue(db, "uuid()");

    YTEntityImpl doc = new YTEntityImpl("ClassC");

    byte[] val = doc.toStream();
    YTEntityImpl doc1 = new YTEntityImpl();
    ORecordInternal.unsetDirty(doc1);
    doc1.fromStream(val);
    doc1.deserializeFields();
    assertEquals(doc.field("name"), (String) doc1.field("name"));
  }

  @Test
  public void testDefaultValueDate() {
    YTSchema schema = db.getMetadata().getSchema();
    YTClass classA = schema.createClass("ClassA");

    YTProperty prop = classA.createProperty(db, "date", YTType.DATE);
    prop.setDefaultValue(db, ODateHelper.getDateTimeFormatInstance().format(new Date()));
    YTProperty some = classA.createProperty(db, "id", YTType.STRING);
    some.setDefaultValue(db, "uuid()");

    db.begin();
    YTEntityImpl doc = new YTEntityImpl(classA);
    YTEntityImpl saved = db.save(doc);
    db.commit();

    saved = db.bindToSession(saved);
    assertNotNull(saved.field("date"));
    assertTrue(saved.field("date") instanceof Date);
    assertNotNull(saved.field("id"));

    db.begin();
    YTResult inserted = db.command("insert into ClassA content {}").next();
    db.commit();

    YTEntityImpl seved1 = db.load(inserted.getIdentity().get());
    assertNotNull(seved1.field("date"));
    assertNotNull(seved1.field("id"));
    assertTrue(seved1.field("date") instanceof Date);
  }

  @Test
  public void testDefaultValueDateFromContent() {
    YTSchema schema = db.getMetadata().getSchema();
    YTClass classA = schema.createClass("ClassA");

    YTProperty prop = classA.createProperty(db, "date", YTType.DATE);
    prop.setDefaultValue(db, ODateHelper.getDateTimeFormatInstance().format(new Date()));
    YTProperty some = classA.createProperty(db, "id", YTType.STRING);
    some.setDefaultValue(db, "uuid()");

    String value = "2000-01-01 00:00:00";

    db.begin();
    YTEntityImpl doc = new YTEntityImpl(classA);
    YTEntityImpl saved = db.save(doc);
    db.commit();

    saved = db.bindToSession(saved);
    assertNotNull(saved.field("date"));
    assertTrue(saved.field("date") instanceof Date);
    assertNotNull(saved.field("id"));

    db.begin();
    YTResult inserted = db.command("insert into ClassA content {\"date\":\"" + value + "\"}")
        .next();
    db.commit();

    YTEntityImpl seved1 = db.load(inserted.getIdentity().get());
    assertNotNull(seved1.field("date"));
    assertNotNull(seved1.field("id"));
    assertTrue(seved1.field("date") instanceof Date);
    assertEquals(ODateHelper.getDateTimeFormatInstance().format(seved1.field("date")), value);
  }

  @Test
  public void testDefaultValueFromJson() {
    YTSchema schema = db.getMetadata().getSchema();
    YTClass classA = schema.createClass("ClassA");

    YTProperty prop = classA.createProperty(db, "date", YTType.DATE);
    prop.setDefaultValue(db, ODateHelper.getDateTimeFormatInstance().format(new Date()));

    db.begin();
    YTEntityImpl doc = new YTEntityImpl();
    doc.fromJSON("{'@class':'ClassA','other':'other'}");
    YTEntityImpl saved = db.save(doc);
    db.commit();

    saved = db.bindToSession(saved);
    assertNotNull(saved.field("date"));
    assertTrue(saved.field("date") instanceof Date);
    assertNotNull(saved.field("other"));
  }

  @Test
  public void testDefaultValueProvidedFromJson() {
    YTSchema schema = db.getMetadata().getSchema();
    YTClass classA = schema.createClass("ClassA");

    YTProperty prop = classA.createProperty(db, "date", YTType.DATETIME);
    prop.setDefaultValue(db, ODateHelper.getDateTimeFormatInstance().format(new Date()));

    String value1 = ODateHelper.getDateTimeFormatInstance().format(new Date());
    db.begin();
    YTEntityImpl doc = new YTEntityImpl();
    doc.fromJSON("{'@class':'ClassA','date':'" + value1 + "','other':'other'}");
    YTEntityImpl saved = db.save(doc);
    db.commit();

    saved = db.bindToSession(saved);
    assertNotNull(saved.field("date"));
    assertEquals(ODateHelper.getDateTimeFormatInstance().format(saved.field("date")), value1);
    assertNotNull(saved.field("other"));
  }

  @Test
  public void testDefaultValueMandatoryReadonlyFromJson() {
    YTSchema schema = db.getMetadata().getSchema();
    YTClass classA = schema.createClass("ClassA");

    YTProperty prop = classA.createProperty(db, "date", YTType.DATE);
    prop.setMandatory(db, true);
    prop.setReadonly(db, true);
    prop.setDefaultValue(db, ODateHelper.getDateTimeFormatInstance().format(new Date()));

    db.begin();
    YTEntityImpl doc = new YTEntityImpl();
    doc.fromJSON("{'@class':'ClassA','other':'other'}");
    YTEntityImpl saved = db.save(doc);
    db.commit();

    saved = db.bindToSession(saved);
    assertNotNull(saved.field("date"));
    assertTrue(saved.field("date") instanceof Date);
    assertNotNull(saved.field("other"));
  }

  @Test
  public void testDefaultValueProvidedMandatoryReadonlyFromJson() {
    YTSchema schema = db.getMetadata().getSchema();
    YTClass classA = schema.createClass("ClassA");

    YTProperty prop = classA.createProperty(db, "date", YTType.DATETIME);
    prop.setMandatory(db, true);
    prop.setReadonly(db, true);
    prop.setDefaultValue(db, ODateHelper.getDateTimeFormatInstance().format(new Date()));

    String value1 = ODateHelper.getDateTimeFormatInstance().format(new Date());
    YTEntityImpl doc = new YTEntityImpl();
    doc.fromJSON("{'@class':'ClassA','date':'" + value1 + "','other':'other'}");
    db.begin();
    YTEntityImpl saved = db.save(doc);
    db.commit();
    saved = db.bindToSession(saved);
    assertNotNull(saved.field("date"));
    assertEquals(ODateHelper.getDateTimeFormatInstance().format(saved.field("date")), value1);
    assertNotNull(saved.field("other"));
  }

  @Test
  public void testDefaultValueUpdateMandatoryReadonlyFromJson() {
    YTSchema schema = db.getMetadata().getSchema();
    YTClass classA = schema.createClass("ClassA");

    YTProperty prop = classA.createProperty(db, "date", YTType.DATETIME);
    prop.setMandatory(db, true);
    prop.setReadonly(db, true);
    prop.setDefaultValue(db, ODateHelper.getDateTimeFormatInstance().format(new Date()));

    db.begin();
    YTEntityImpl doc = new YTEntityImpl();
    doc.fromJSON("{'@class':'ClassA','other':'other'}");
    YTEntityImpl saved = db.save(doc);
    db.commit();

    db.begin();
    saved = db.bindToSession(saved);
    doc = db.bindToSession(doc);

    assertNotNull(saved.field("date"));
    assertTrue(saved.field("date") instanceof Date);
    assertNotNull(saved.field("other"));
    String val = ODateHelper.getDateTimeFormatInstance().format(doc.field("date"));
    YTEntityImpl doc1 = new YTEntityImpl();
    doc1.fromJSON("{'@class':'ClassA','date':'" + val + "','other':'other1'}");
    saved.merge(doc1, true, true);

    saved = db.save(saved);
    db.commit();

    saved = db.bindToSession(saved);
    assertNotNull(saved.field("date"));
    assertEquals(ODateHelper.getDateTimeFormatInstance().format(saved.field("date")), val);
    assertEquals(saved.field("other"), "other1");
  }
}
