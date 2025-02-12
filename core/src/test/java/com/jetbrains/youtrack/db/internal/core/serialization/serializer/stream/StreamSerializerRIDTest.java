package com.jetbrains.youtrack.db.internal.core.serialization.serializer.stream;

import com.jetbrains.youtrack.db.internal.common.serialization.types.LongSerializer;
import com.jetbrains.youtrack.db.internal.common.serialization.types.ShortSerializer;
import com.jetbrains.youtrack.db.internal.core.id.RecordId;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.binary.BinarySerializerFactory;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.wal.WALChanges;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.wal.WALPageChangesPortion;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class StreamSerializerRIDTest {

  private static final int FIELD_SIZE = ShortSerializer.SHORT_SIZE + LongSerializer.LONG_SIZE;
  private static final int clusterId = 5;
  private static final long position = 100500L;
  private static RecordId OBJECT;
  private static StreamSerializerRID streamSerializerRID;
  private static BinarySerializerFactory serializerFactory;

  @BeforeClass
  public static void beforeClass() {
    OBJECT = new RecordId(clusterId, position);
    streamSerializerRID = new StreamSerializerRID();
    serializerFactory = BinarySerializerFactory.create(
        BinarySerializerFactory.currentBinaryFormatVersion());
  }

  @Test
  public void testFieldSize() {
    Assert.assertEquals(FIELD_SIZE, streamSerializerRID.getObjectSize(serializerFactory, OBJECT));
  }

  @Test
  public void testSerializeInByteBuffer() {
    final var serializationOffset = 5;

    final var buffer = ByteBuffer.allocate(FIELD_SIZE + serializationOffset);

    buffer.position(serializationOffset);
    streamSerializerRID.serializeInByteBufferObject(serializerFactory, OBJECT, buffer);

    final var binarySize = buffer.position() - serializationOffset;
    Assert.assertEquals(FIELD_SIZE, binarySize);

    buffer.position(serializationOffset);
    Assert.assertEquals(FIELD_SIZE,
        streamSerializerRID.getObjectSizeInByteBuffer(serializerFactory, buffer));

    buffer.position(serializationOffset);
    Assert.assertEquals(
        streamSerializerRID.deserializeFromByteBufferObject(serializerFactory, buffer), OBJECT);

    Assert.assertEquals(FIELD_SIZE, buffer.position() - serializationOffset);
  }

  @Test
  public void testSerializeInImmutableByteBufferPosition() {
    final var serializationOffset = 5;

    final var buffer = ByteBuffer.allocate(FIELD_SIZE + serializationOffset);

    buffer.position(serializationOffset);
    streamSerializerRID.serializeInByteBufferObject(serializerFactory, OBJECT, buffer);

    final var binarySize = buffer.position() - serializationOffset;
    Assert.assertEquals(FIELD_SIZE, binarySize);

    buffer.position(0);
    Assert.assertEquals(
        FIELD_SIZE,
        streamSerializerRID.getObjectSizeInByteBuffer(serializerFactory, serializationOffset,
            buffer));
    Assert.assertEquals(0, buffer.position());

    Assert.assertEquals(
        streamSerializerRID.deserializeFromByteBufferObject(serializerFactory, serializationOffset,
            buffer), OBJECT);
    Assert.assertEquals(0, buffer.position());
  }

  @Test
  public void testsSerializeWALChanges() {
    final var serializationOffset = 5;

    final var buffer =
        ByteBuffer.allocateDirect(
                FIELD_SIZE + serializationOffset + WALPageChangesPortion.PORTION_BYTES)
            .order(ByteOrder.nativeOrder());
    final var data = new byte[FIELD_SIZE];
    streamSerializerRID.serializeNativeObject(OBJECT, serializerFactory, data, 0);

    final WALChanges walChanges = new WALPageChangesPortion();
    walChanges.setBinaryValue(buffer, data, serializationOffset);

    Assert.assertEquals(
        FIELD_SIZE,
        streamSerializerRID.getObjectSizeInByteBuffer(buffer, walChanges, serializationOffset));
    Assert.assertEquals(
        streamSerializerRID.deserializeFromByteBufferObject(serializerFactory,
            buffer, walChanges, serializationOffset),
        OBJECT);
  }
}
