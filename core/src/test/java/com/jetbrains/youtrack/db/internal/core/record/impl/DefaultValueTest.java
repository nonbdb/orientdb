package com.jetbrains.youtrack.db.internal.core.record.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.api.schema.Schema;
import com.jetbrains.youtrack.db.internal.DbTestBase;
import com.jetbrains.youtrack.db.internal.core.record.RecordInternal;
import com.jetbrains.youtrack.db.internal.core.util.DateHelper;
import java.util.Date;
import org.junit.Test;

public class DefaultValueTest extends DbTestBase {

  @Test
  public void testKeepValueSerialization() {
    // create example schema
    Schema schema = db.getMetadata().getSchema();
    var classA = schema.createClass("ClassC");

    var prop = classA.createProperty(db, "name", PropertyType.STRING);
    prop.setDefaultValue(db, "uuid()");

    var doc = (EntityImpl) db.newEntity("ClassC");

    var val = doc.toStream();
    var doc1 = (EntityImpl) db.newEntity();
    RecordInternal.unsetDirty(doc1);
    doc1.fromStream(val);
    doc1.deserializeFields();
    assertEquals(doc.field("name"), (String) doc1.field("name"));
  }

  @Test
  public void testDefaultValueDate() {
    Schema schema = db.getMetadata().getSchema();
    var classA = schema.createClass("ClassA");

    var prop = classA.createProperty(db, "date", PropertyType.DATE);
    prop.setDefaultValue(db, DateHelper.getDateTimeFormatInstance().format(new Date()));
    var some = classA.createProperty(db, "id", PropertyType.STRING);
    some.setDefaultValue(db, "uuid()");

    db.begin();
    var doc = (EntityImpl) db.newEntity(classA);
    EntityImpl saved = db.save(doc);
    db.commit();

    saved = db.bindToSession(saved);
    assertNotNull(saved.field("date"));
    assertTrue(saved.field("date") instanceof Date);
    assertNotNull(saved.field("id"));

    db.begin();
    var inserted = db.command("insert into ClassA content {}").next();
    db.commit();

    EntityImpl seved1 = db.load(inserted.getIdentity().get());
    assertNotNull(seved1.field("date"));
    assertNotNull(seved1.field("id"));
    assertTrue(seved1.field("date") instanceof Date);
  }

  @Test
  public void testDefaultValueDateFromContent() {
    Schema schema = db.getMetadata().getSchema();
    var classA = schema.createClass("ClassA");

    var prop = classA.createProperty(db, "date", PropertyType.DATE);
    prop.setDefaultValue(db, DateHelper.getDateTimeFormatInstance().format(new Date()));
    var some = classA.createProperty(db, "id", PropertyType.STRING);
    some.setDefaultValue(db, "uuid()");

    var value = "2000-01-01 00:00:00";

    db.begin();
    var doc = (EntityImpl) db.newEntity(classA);
    EntityImpl saved = db.save(doc);
    db.commit();

    saved = db.bindToSession(saved);
    assertNotNull(saved.field("date"));
    assertTrue(saved.field("date") instanceof Date);
    assertNotNull(saved.field("id"));

    db.begin();
    var inserted = db.command("insert into ClassA content {\"date\":\"" + value + "\"}")
        .next();
    db.commit();

    EntityImpl seved1 = db.load(inserted.getIdentity().get());
    assertNotNull(seved1.field("date"));
    assertNotNull(seved1.field("id"));
    assertTrue(seved1.field("date") instanceof Date);
    assertEquals(DateHelper.getDateTimeFormatInstance().format(seved1.field("date")), value);
  }

  @Test
  public void testDefaultValueFromJson() {
    Schema schema = db.getMetadata().getSchema();
    var classA = schema.createClass("ClassA");

    var prop = classA.createProperty(db, "date", PropertyType.DATE);
    prop.setDefaultValue(db, DateHelper.getDateTimeFormatInstance().format(new Date()));

    db.begin();
    var doc = (EntityImpl) db.newEntity();
    doc.updateFromJSON("{'@class':'ClassA','other':'other'}");
    EntityImpl saved = db.save(doc);
    db.commit();

    saved = db.bindToSession(saved);
    assertNotNull(saved.field("date"));
    assertTrue(saved.field("date") instanceof Date);
    assertNotNull(saved.field("other"));
  }

  @Test
  public void testDefaultValueProvidedFromJson() {
    Schema schema = db.getMetadata().getSchema();
    var classA = schema.createClass("ClassA");

    var prop = classA.createProperty(db, "date", PropertyType.DATETIME);
    prop.setDefaultValue(db, DateHelper.getDateTimeFormatInstance().format(new Date()));

    var value1 = DateHelper.getDateTimeFormatInstance().format(new Date());
    db.begin();
    var doc = (EntityImpl) db.newEntity();
    doc.updateFromJSON("{'@class':'ClassA','date':'" + value1 + "','other':'other'}");
    EntityImpl saved = db.save(doc);
    db.commit();

    saved = db.bindToSession(saved);
    assertNotNull(saved.field("date"));
    assertEquals(DateHelper.getDateTimeFormatInstance().format(saved.field("date")), value1);
    assertNotNull(saved.field("other"));
  }

  @Test
  public void testDefaultValueMandatoryReadonlyFromJson() {
    Schema schema = db.getMetadata().getSchema();
    var classA = schema.createClass("ClassA");

    var prop = classA.createProperty(db, "date", PropertyType.DATE);
    prop.setMandatory(db, true);
    prop.setReadonly(db, true);
    prop.setDefaultValue(db, DateHelper.getDateTimeFormatInstance().format(new Date()));

    db.begin();
    var doc = (EntityImpl) db.newEntity();
    doc.updateFromJSON("{'@class':'ClassA','other':'other'}");
    EntityImpl saved = db.save(doc);
    db.commit();

    saved = db.bindToSession(saved);
    assertNotNull(saved.field("date"));
    assertTrue(saved.field("date") instanceof Date);
    assertNotNull(saved.field("other"));
  }

  @Test
  public void testDefaultValueProvidedMandatoryReadonlyFromJson() {
    Schema schema = db.getMetadata().getSchema();
    var classA = schema.createClass("ClassA");

    var prop = classA.createProperty(db, "date", PropertyType.DATETIME);
    prop.setMandatory(db, true);
    prop.setReadonly(db, true);
    prop.setDefaultValue(db, DateHelper.getDateTimeFormatInstance().format(new Date()));

    var value1 = DateHelper.getDateTimeFormatInstance().format(new Date());
    var doc = (EntityImpl) db.newEntity();
    doc.updateFromJSON("{'@class':'ClassA','date':'" + value1 + "','other':'other'}");
    db.begin();
    EntityImpl saved = db.save(doc);
    db.commit();
    saved = db.bindToSession(saved);
    assertNotNull(saved.field("date"));
    assertEquals(DateHelper.getDateTimeFormatInstance().format(saved.field("date")), value1);
    assertNotNull(saved.field("other"));
  }

  @Test
  public void testDefaultValueUpdateMandatoryReadonlyFromJson() {
    Schema schema = db.getMetadata().getSchema();
    var classA = schema.createClass("ClassA");

    var prop = classA.createProperty(db, "date", PropertyType.DATETIME);
    prop.setMandatory(db, true);
    prop.setReadonly(db, true);
    prop.setDefaultValue(db, DateHelper.getDateTimeFormatInstance().format(new Date()));

    db.begin();
    var doc = (EntityImpl) db.newEntity();
    doc.updateFromJSON("{'@class':'ClassA','other':'other'}");
    EntityImpl saved = db.save(doc);
    db.commit();

    db.begin();
    saved = db.bindToSession(saved);
    doc = db.bindToSession(doc);

    assertNotNull(saved.field("date"));
    assertTrue(saved.field("date") instanceof Date);
    assertNotNull(saved.field("other"));
    var val = DateHelper.getDateTimeFormatInstance().format(doc.field("date"));
    var doc1 = (EntityImpl) db.newEntity();
    doc1.updateFromJSON("{'@class':'ClassA','date':'" + val + "','other':'other1'}");
    saved.merge(doc1, true, true);

    saved = db.save(saved);
    db.commit();

    saved = db.bindToSession(saved);
    assertNotNull(saved.field("date"));
    assertEquals(DateHelper.getDateTimeFormatInstance().format(saved.field("date")), val);
    assertEquals(saved.field("other"), "other1");
  }
}
