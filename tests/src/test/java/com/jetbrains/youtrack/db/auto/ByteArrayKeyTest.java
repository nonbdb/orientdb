package com.jetbrains.youtrack.db.auto;

import com.jetbrains.youtrack.db.api.record.RID;
import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import com.jetbrains.youtrack.db.internal.core.index.CompositeKey;
import com.jetbrains.youtrack.db.internal.core.index.Index;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import java.util.stream.Stream;
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

    final SchemaClass byteArrayKeyTest =
        db.getMetadata().getSchema().createClass("ByteArrayKeyTest");
    byteArrayKeyTest.createProperty(db, "byteArrayKey", PropertyType.BINARY);

    byteArrayKeyTest.createIndex(db, "byteArrayKeyIndex", SchemaClass.INDEX_TYPE.UNIQUE,
        "byteArrayKey");

    final SchemaClass compositeByteArrayKeyTest =
        db.getMetadata().getSchema().createClass("CompositeByteArrayKeyTest");
    compositeByteArrayKeyTest.createProperty(db, "byteArrayKey", PropertyType.BINARY);
    compositeByteArrayKeyTest.createProperty(db, "intKey", PropertyType.INTEGER);

    compositeByteArrayKeyTest.createIndex(db,
        "compositeByteArrayKey", SchemaClass.INDEX_TYPE.UNIQUE, "byteArrayKey", "intKey");
  }

  public void testAutomaticUsage() {
    checkEmbeddedDB();

    byte[] key1 =
        new byte[]{
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8,
            9,
            0, 1
        };

    db.begin();
    EntityImpl doc1 = ((EntityImpl) db.newEntity("ByteArrayKeyTest"));
    doc1.field("byteArrayKey", key1);
    doc1.save();

    byte[] key2 =
        new byte[]{
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8,
            9,
            0, 2
        };
    EntityImpl doc2 = ((EntityImpl) db.newEntity("ByteArrayKeyTest"));
    doc2.field("byteArrayKey", key2);
    doc2.save();
    db.commit();

    Index index =
        db.getMetadata().getIndexManagerInternal().getIndex(db, "byteArrayKeyIndex");
    try (Stream<RID> stream = index.getInternal().getRids(db, key1)) {
      Assert.assertEquals(stream.findAny().map(rid -> rid.getRecord(db)).orElse(null), doc1);
    }
    try (Stream<RID> stream = index.getInternal().getRids(db, key2)) {
      Assert.assertEquals(stream.findAny().map(rid -> rid.getRecord(db)).orElse(null), doc2);
    }
  }

  public void testAutomaticCompositeUsage() {
    checkEmbeddedDB();

    byte[] key1 = new byte[]{1, 2, 3};
    byte[] key2 = new byte[]{4, 5, 6};

    db.begin();
    EntityImpl doc1 = ((EntityImpl) db.newEntity("CompositeByteArrayKeyTest"));
    doc1.field("byteArrayKey", key1);
    doc1.field("intKey", 1);
    doc1.save();

    EntityImpl doc2 = ((EntityImpl) db.newEntity("CompositeByteArrayKeyTest"));
    doc2.field("byteArrayKey", key2);
    doc2.field("intKey", 2);
    doc2.save();
    db.commit();

    Index index =
        db
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(db, "compositeByteArrayKey");
    try (Stream<RID> stream = index.getInternal().getRids(db, new CompositeKey(key1, 1))) {
      Assert.assertEquals(stream.findAny().map(rid -> rid.getRecord(db)).orElse(null), doc1);
    }
    try (Stream<RID> stream = index.getInternal().getRids(db, new CompositeKey(key2, 2))) {
      Assert.assertEquals(stream.findAny().map(rid -> rid.getRecord(db)).orElse(null), doc2);
    }
  }

  public void testAutomaticCompositeUsageInTX() {
    checkEmbeddedDB();

    byte[] key1 = new byte[]{7, 8, 9};
    byte[] key2 = new byte[]{10, 11, 12};

    db.begin();
    EntityImpl doc1 = ((EntityImpl) db.newEntity("CompositeByteArrayKeyTest"));
    doc1.field("byteArrayKey", key1);
    doc1.field("intKey", 1);
    doc1.save();

    EntityImpl doc2 = ((EntityImpl) db.newEntity("CompositeByteArrayKeyTest"));
    doc2.field("byteArrayKey", key2);
    doc2.field("intKey", 2);
    doc2.save();
    db.commit();

    Index index =
        db
            .getMetadata()
            .getIndexManagerInternal()
            .getIndex(db, "compositeByteArrayKey");
    try (Stream<RID> stream = index.getInternal().getRids(db, new CompositeKey(key1, 1))) {
      Assert.assertEquals(stream.findAny().map(rid -> rid.getRecord(db)).orElse(null), doc1);
    }
    try (Stream<RID> stream = index.getInternal().getRids(db, new CompositeKey(key2, 2))) {
      Assert.assertEquals(stream.findAny().map(rid -> rid.getRecord(db)).orElse(null), doc2);
    }
  }

  @Test(dependsOnMethods = {"testAutomaticUsage"})
  public void testContains() {
    byte[] key1 =
        new byte[]{
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8,
            9,
            0, 1
        };
    byte[] key2 =
        new byte[]{
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8,
            9,
            0, 2
        };

    Index autoIndex =
        db.getMetadata().getIndexManagerInternal().getIndex(db, "byteArrayKeyIndex");
    try (Stream<RID> stream = autoIndex.getInternal().getRids(db, key1)) {
      Assert.assertTrue(stream.findFirst().isPresent());
    }
    try (Stream<RID> stream = autoIndex.getInternal().getRids(db, key2)) {
      Assert.assertTrue(stream.findFirst().isPresent());
    }
  }
}
