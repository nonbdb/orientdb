/*
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jetbrains.youtrack.db.internal.common.serialization.types;

import com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.wal.WALChanges;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.wal.WALPageChangesPortion;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @since 18.01.12
 */
public class ShortSerializerTest {

  private static final int FIELD_SIZE = 2;
  byte[] stream = new byte[FIELD_SIZE];
  private static final Short OBJECT = 1;
  private ShortSerializer shortSerializer;

  @Before
  public void beforeClass() {
    shortSerializer = new ShortSerializer();
  }

  @Test
  public void testFieldSize() {
    Assert.assertEquals(shortSerializer.getObjectSize(null), FIELD_SIZE);
  }

  @Test
  public void testSerialize() {
    shortSerializer.serialize(OBJECT, stream, 0);
    Assert.assertEquals(shortSerializer.deserialize(stream, 0), OBJECT);
  }

  @Test
  public void testSerializeNative() {
    shortSerializer.serializeNative(OBJECT, stream, 0);
    Assert.assertEquals(shortSerializer.deserializeNativeObject(stream, 0), OBJECT);
  }

  @Test
  public void testNativeDirectMemoryCompatibility() {
    shortSerializer.serializeNative(OBJECT, stream, 0);

    var buffer = ByteBuffer.allocateDirect(stream.length).order(ByteOrder.nativeOrder());
    buffer.put(stream);
    buffer.position(0);

    Assert.assertEquals(shortSerializer.deserializeFromByteBufferObject(buffer), OBJECT);
  }

  @Test
  public void testSerializationInByteBuffer() {
    final var serializationOffset = 5;

    final var buffer = ByteBuffer.allocate(FIELD_SIZE + serializationOffset);
    buffer.position(serializationOffset);

    shortSerializer.serializeInByteBufferObject(OBJECT, buffer);

    final var binarySize = buffer.position() - serializationOffset;
    Assert.assertEquals(binarySize, FIELD_SIZE);

    buffer.position(serializationOffset);
    Assert.assertEquals(shortSerializer.getObjectSizeInByteBuffer(buffer), FIELD_SIZE);

    buffer.position(serializationOffset);
    Assert.assertEquals(shortSerializer.deserializeFromByteBufferObject(buffer), OBJECT);

    Assert.assertEquals(buffer.position() - serializationOffset, FIELD_SIZE);
  }

  @Test
  public void testSerializationWALChanges() {
    final var serializationOffset = 5;

    final var buffer =
        ByteBuffer.allocateDirect(
                FIELD_SIZE + serializationOffset + WALPageChangesPortion.PORTION_BYTES)
            .order(ByteOrder.nativeOrder());
    final var data = new byte[FIELD_SIZE];
    shortSerializer.serializeNative(OBJECT, data, 0);

    final WALChanges walChanges = new WALPageChangesPortion();
    walChanges.setBinaryValue(buffer, data, serializationOffset);

    Assert.assertEquals(
        shortSerializer.getObjectSizeInByteBuffer(buffer, walChanges, serializationOffset),
        FIELD_SIZE);
    Assert.assertEquals(
        shortSerializer.deserializeFromByteBufferObject(buffer, walChanges, serializationOffset),
        OBJECT);
  }
}
