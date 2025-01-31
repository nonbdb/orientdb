package com.jetbrains.youtrack.db.auto;

import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import com.jetbrains.youtrack.db.internal.core.index.CompositeKey;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * @since 03.07.12
 */
@Test
public class ByteArrayKeyTest extends BaseDBTest {

  @Parameters(value = "remote")
  public ByteArrayKeyTest(boolean remote) {
    super(remote);
  }

  @BeforeClass
  public void beforeClass() throws Exception {
    super.beforeClass();

    final var byteArrayKeyTest =
        db.getMetadata().getSchema().createClass("ByteArrayKeyTest");
    byteArrayKeyTest.createProperty(db, "byteArrayKey", PropertyType.BINARY);

    byteArrayKeyTest.createIndex(db, "byteArrayKeyIndex", SchemaClass.INDEX_TYPE.UNIQUE,
        "byteArrayKey");

    final var compositeByteArrayKeyTest =
        db.getMetadata().getSchema().createClass("CompositeByteArrayKeyTest");
    compositeByteArrayKeyTest.createProperty(db, "byteArrayKey", PropertyType.BINARY);
    compositeByteArrayKeyTest.createProperty(db, "intKey", PropertyType.INTEGER);

    compositeByteArrayKeyTest.createIndex(db,
        "compositeByteArrayKey", SchemaClass.INDEX_TYPE.UNIQUE, "byteArrayKey", "intKey");
  }

  public void testAutomaticUsage() {
    checkEmbeddedDB();

    var key1 =
        new byte[]{
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8,
            9,
            0, 1
        };

    db.begin();
    var doc1 = ((EntityImpl) db.newEntity("ByteArrayKeyTest"));
    doc1.field("byteArrayKey", key1);
    doc1.save();

    var key2 =
        new byte[]{
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8,
            9,
            0, 2
        };
    var doc2 = ((EntityImpl) db.newEntity("ByteArrayKeyTest"));
    doc2.field("byteArrayKey", key2);
    doc2.save();
    db.commit();

    var index =
        db.getMetadata().getIndexManagerInternal().getIndex(db, "byteArrayKeyIndex");
    try (var stream = index.getInternal().getRids(db, key1)) {
      Assert.assertEquals(stream.findAny().map(rid -> rid.getRecord(db)).orElse(null), doc1);
    }
    try (var stream = index.getInternal().getRids(db, key2)) {
      Assert.assertEquals(stream.findAny().map(rid -> rid.getRecord(db)).orElse(null), doc2);
    }
  }

  public void testAutomaticCompositeUsage() {
    checkEmbeddedDB();

    var key1 = new byte[]{1, 2, 3};
    var key2 = new byte[]{4, 5, 6};

    db.begin();
    var doc1 = ((EntityImpl) db.newEntity("CompositeByteArrayKeyTest"));
    doc1.field("byteArrayKey", key1);
    doc1.field("intKey", 1);
    doc1.save();

    var doc2 = ((EntityImpl) db.newEntity("CompositeByteArrayKeyTest"));
    doc2.field("byteArrayKey", key2);
    doc2.field("intKey", 2);
    doc2.save();
    db.commit();

    var index =
        db
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(db, "compositeByteArrayKey");
    try (var stream = index.getInternal().getRids(db, new CompositeKey(key1, 1))) {
      Assert.assertEquals(stream.findAny().map(rid -> rid.getRecord(db)).orElse(null), doc1);
    }
    try (var stream = index.getInternal().getRids(db, new CompositeKey(key2, 2))) {
      Assert.assertEquals(stream.findAny().map(rid -> rid.getRecord(db)).orElse(null), doc2);
    }
  }

  public void testAutomaticCompositeUsageInTX() {
    checkEmbeddedDB();

    var key1 = new byte[]{7, 8, 9};
    var key2 = new byte[]{10, 11, 12};

    db.begin();
    var doc1 = ((EntityImpl) db.newEntity("CompositeByteArrayKeyTest"));
    doc1.field("byteArrayKey", key1);
    doc1.field("intKey", 1);
    doc1.save();

    var doc2 = ((EntityImpl) db.newEntity("CompositeByteArrayKeyTest"));
    doc2.field("byteArrayKey", key2);
    doc2.field("intKey", 2);
    doc2.save();
    db.commit();

    var index =
        db
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(db, "compositeByteArrayKey");
    try (var stream = index.getInternal().getRids(db, new CompositeKey(key1, 1))) {
      Assert.assertEquals(stream.findAny().map(rid -> rid.getRecord(db)).orElse(null), doc1);
    }
    try (var stream = index.getInternal().getRids(db, new CompositeKey(key2, 2))) {
      Assert.assertEquals(stream.findAny().map(rid -> rid.getRecord(db)).orElse(null), doc2);
    }
  }

  @Test(dependsOnMethods = {"testAutomaticUsage"})
  public void testContains() {
    var key1 =
        new byte[]{
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8,
            9,
            0, 1
        };
    var key2 =
        new byte[]{
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8,
            9,
            0, 2
        };

    var autoIndex =
        db.getMetadata().getIndexManagerInternal().getIndex(db, "byteArrayKeyIndex");
    try (var stream = autoIndex.getInternal().getRids(db, key1)) {
      Assert.assertTrue(stream.findFirst().isPresent());
    }
    try (var stream = autoIndex.getInternal().getRids(db, key2)) {
      Assert.assertTrue(stream.findFirst().isPresent());
    }
  }
}
