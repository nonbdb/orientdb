package com.jetbrains.youtrack.db.internal.core.serialization.serializer.binary.impl.index;

import com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.wal.WALChanges;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.wal.WALPageChangesPortion;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SimpleKeySerializerTest {

  private static final int FIELD_SIZE = 9;
  private static final Double OBJECT = Math.PI;
  private SimpleKeySerializer<Double> simpleKeySerializer;

  @Before
  public void beforeClass() {
    simpleKeySerializer = new SimpleKeySerializer<>();
  }

  @Test
  public void testFieldSize() {
    Assert.assertEquals(simpleKeySerializer.getObjectSize(OBJECT), FIELD_SIZE);
  }

  @Test
  public void testSerializeInByteBuffer() {
    final var serializationOffset = 5;

    final var buffer = ByteBuffer.allocate(FIELD_SIZE + serializationOffset);
    buffer.position(serializationOffset);

    simpleKeySerializer.serializeInByteBufferObject(OBJECT, buffer);

    final var binarySize = buffer.position() - serializationOffset;
    Assert.assertEquals(binarySize, FIELD_SIZE);

    buffer.position(serializationOffset);
    Assert.assertEquals(simpleKeySerializer.getObjectSizeInByteBuffer(buffer), FIELD_SIZE);

    buffer.position(serializationOffset);
    Assert.assertEquals(simpleKeySerializer.deserializeFromByteBufferObject(buffer), OBJECT);

    Assert.assertEquals(buffer.position() - serializationOffset, FIELD_SIZE);
  }

  @Test
  public void testSerializeInImmutableByteBufferPosition() {
    final var serializationOffset = 5;

    final var buffer = ByteBuffer.allocate(FIELD_SIZE + serializationOffset);
    buffer.position(serializationOffset);

    simpleKeySerializer.serializeInByteBufferObject(OBJECT, buffer);

    final var binarySize = buffer.position() - serializationOffset;
    Assert.assertEquals(binarySize, FIELD_SIZE);

    buffer.position(0);
    Assert.assertEquals(
        simpleKeySerializer.getObjectSizeInByteBuffer(serializationOffset, buffer), FIELD_SIZE);
    Assert.assertEquals(0, buffer.position());

    Assert.assertEquals(
        simpleKeySerializer.deserializeFromByteBufferObject(serializationOffset, buffer), OBJECT);
    Assert.assertEquals(0, buffer.position());
  }

  @Test
  public void testSerializeWALChanges() {
    final var serializationOffset = 5;

    final var buffer =
        ByteBuffer.allocateDirect(
                FIELD_SIZE + serializationOffset + WALPageChangesPortion.PORTION_BYTES)
            .order(ByteOrder.nativeOrder());
    final var data = new byte[FIELD_SIZE];

    simpleKeySerializer.serializeNativeObject(OBJECT, data, 0);
    final WALChanges walChanges = new WALPageChangesPortion();
    walChanges.setBinaryValue(buffer, data, serializationOffset);

    Assert.assertEquals(
        simpleKeySerializer.getObjectSizeInByteBuffer(buffer, walChanges, serializationOffset),
        FIELD_SIZE);
    Assert.assertEquals(
        simpleKeySerializer.deserializeFromByteBufferObject(
            buffer, walChanges, serializationOffset),
        OBJECT);
    Assert.assertEquals(0, buffer.position());
  }
}
