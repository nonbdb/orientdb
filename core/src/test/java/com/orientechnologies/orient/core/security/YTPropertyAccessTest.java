package com.orientechnologies.orient.core.security;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.orientechnologies.DBTestBase;
import com.orientechnologies.orient.core.metadata.schema.YTType;
import com.orientechnologies.orient.core.metadata.security.PropertyAccess;
import com.orientechnologies.orient.core.record.ORecordInternal;
import com.orientechnologies.orient.core.record.impl.ODocumentInternal;
import com.orientechnologies.orient.core.record.impl.YTEntityImpl;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class YTPropertyAccessTest extends DBTestBase {

  @Test
  public void testNotAccessible() {
    YTEntityImpl doc = new YTEntityImpl();
    doc.setProperty("name", "one value");
    assertEquals("one value", doc.getProperty("name"));
    assertEquals("one value", doc.field("name"));
    assertTrue(doc.containsField("name"));
    Set<String> toHide = new HashSet<>();
    toHide.add("name");
    ODocumentInternal.setPropertyAccess(doc, new PropertyAccess(toHide));
    assertNull(doc.getProperty("name"));
    assertNull(doc.field("name"));
    assertNull(doc.field("name", YTType.STRING));
    assertNull(doc.field("name", String.class));
    assertFalse(doc.containsField("name"));
    assertNull(doc.fieldType("name"));
  }

  @Test
  public void testNotAccessibleAfterConvert() {
    YTEntityImpl doc = new YTEntityImpl();
    doc.setProperty("name", "one value");
    YTEntityImpl doc1 = new YTEntityImpl();
    ORecordInternal.unsetDirty(doc1);
    doc1.fromStream(doc.toStream());
    assertEquals("one value", doc1.getProperty("name"));
    assertEquals("one value", doc1.field("name"));
    assertTrue(doc1.containsField("name"));
    assertEquals(YTType.STRING, doc1.fieldType("name"));

    Set<String> toHide = new HashSet<>();
    toHide.add("name");
    ODocumentInternal.setPropertyAccess(doc1, new PropertyAccess(toHide));
    assertNull(doc1.getProperty("name"));
    assertNull(doc1.field("name"));
    assertFalse(doc1.containsField("name"));
    assertNull(doc1.fieldType("name"));
  }

  @Test
  public void testNotAccessiblePropertyListing() {
    YTEntityImpl doc = new YTEntityImpl();
    doc.setProperty("name", "one value");
    assertArrayEquals(new String[]{"name"}, doc.getPropertyNames().toArray());
    assertArrayEquals(
        new String[]{"one value"},
        doc.getPropertyNames().stream().map(doc::getProperty).toArray());
    assertEquals(new HashSet<String>(List.of("name")), doc.getPropertyNames());
    for (Map.Entry<String, Object> e : doc) {
      assertEquals("name", e.getKey());
    }

    Set<String> toHide = new HashSet<>();
    toHide.add("name");
    ODocumentInternal.setPropertyAccess(doc, new PropertyAccess(toHide));
    assertArrayEquals(new String[]{}, doc.fieldNames());
    assertArrayEquals(new String[]{}, doc.fieldValues());
    assertEquals(new HashSet<String>(), doc.getPropertyNames());
    for (Map.Entry<String, Object> e : doc) {
      assertNotEquals("name", e.getKey());
    }
  }

  @Test
  public void testNotAccessiblePropertyListingSer() {
    YTEntityImpl docPre = new YTEntityImpl();
    docPre.setProperty("name", "one value");
    assertArrayEquals(new String[]{"name"}, docPre.getPropertyNames().toArray());
    assertArrayEquals(
        new String[]{"one value"},
        docPre.getPropertyNames().stream().map(docPre::getProperty).toArray());
    assertEquals(new HashSet<String>(List.of("name")), docPre.getPropertyNames());
    for (Map.Entry<String, Object> e : docPre) {
      assertEquals("name", e.getKey());
    }

    Set<String> toHide = new HashSet<>();
    toHide.add("name");
    YTEntityImpl doc = new YTEntityImpl();
    ORecordInternal.unsetDirty(doc);
    doc.fromStream(docPre.toStream());
    ODocumentInternal.setPropertyAccess(doc, new PropertyAccess(toHide));
    assertArrayEquals(new String[]{}, doc.getPropertyNames().toArray());
    assertArrayEquals(
        new String[]{}, doc.getPropertyNames().stream().map(doc::getProperty).toArray());
    assertEquals(new HashSet<String>(), doc.getPropertyNames());
    for (Map.Entry<String, Object> e : doc) {
      assertNotEquals("name", e.getKey());
    }
  }

  @Test
  public void testJsonSerialization() {
    YTEntityImpl doc = new YTEntityImpl();
    doc.setProperty("name", "one value");
    assertTrue(doc.toJSON().contains("name"));

    Set<String> toHide = new HashSet<>();
    toHide.add("name");
    ODocumentInternal.setPropertyAccess(doc, new PropertyAccess(toHide));
    assertFalse(doc.toJSON().contains("name"));
  }

  @Test
  public void testToMap() {
    YTEntityImpl doc = new YTEntityImpl();
    doc.setProperty("name", "one value");
    assertTrue(doc.toMap().containsKey("name"));

    Set<String> toHide = new HashSet<>();
    toHide.add("name");
    ODocumentInternal.setPropertyAccess(doc, new PropertyAccess(toHide));
    assertFalse(doc.toMap().containsKey("name"));
  }

  @Test
  public void testStringSerialization() {
    YTEntityImpl doc = new YTEntityImpl();
    doc.setProperty("name", "one value");
    assertTrue(doc.toString().contains("name"));

    Set<String> toHide = new HashSet<>();
    toHide.add("name");
    ODocumentInternal.setPropertyAccess(doc, new PropertyAccess(toHide));
    assertFalse(doc.toString().contains("name"));
  }
}
