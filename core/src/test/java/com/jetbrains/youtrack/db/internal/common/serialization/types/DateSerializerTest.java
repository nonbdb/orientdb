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
import java.util.Calendar;
import java.util.Date;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @since 20.01.12
 */
public class DateSerializerTest {

  private static final int FIELD_SIZE = 8;
  private final byte[] stream = new byte[FIELD_SIZE];
  private final Date OBJECT = new Date();
  private DateSerializer dateSerializer;

  @Before
  public void beforeClass() {
    dateSerializer = new DateSerializer();
  }

  @Test
  public void testFieldSize() {
    Assert.assertEquals(dateSerializer.getObjectSize(OBJECT), FIELD_SIZE);
  }

  @Test
  public void testSerialize() {
    dateSerializer.serialize(OBJECT, stream, 0);
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(OBJECT);
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Assert.assertEquals(dateSerializer.deserialize(stream, 0), calendar.getTime());
  }

  @Test
  public void testSerializeNative() {
    dateSerializer.serializeNativeObject(OBJECT, stream, 0);
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(OBJECT);
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Assert.assertEquals(dateSerializer.deserializeNativeObject(stream, 0), calendar.getTime());
  }

  @Test
  public void testNativeDirectMemoryCompatibility() {
    dateSerializer.serializeNativeObject(OBJECT, stream, 0);
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(OBJECT);
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);

    ByteBuffer buffer = ByteBuffer.allocateDirect(stream.length).order(ByteOrder.nativeOrder());
    buffer.position(0);
    buffer.put(stream);
    buffer.position(0);

    Assert.assertEquals(dateSerializer.deserializeFromByteBufferObject(buffer), calendar.getTime());
  }

  @Test
  public void testSerializeInByteBuffer() {
    final int serializationOffset = 5;

    final ByteBuffer buffer = ByteBuffer.allocate(FIELD_SIZE + serializationOffset);

    Calendar calendar = Calendar.getInstance();
    calendar.setTime(OBJECT);
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);

    buffer.position(serializationOffset);
    dateSerializer.serializeInByteBufferObject(OBJECT, buffer);

    final int binarySize = buffer.position() - serializationOffset;
    Assert.assertEquals(binarySize, FIELD_SIZE);

    buffer.position(serializationOffset);
    Assert.assertEquals(dateSerializer.getObjectSizeInByteBuffer(buffer), FIELD_SIZE);

    buffer.position(serializationOffset);
    Assert.assertEquals(dateSerializer.deserializeFromByteBufferObject(buffer), calendar.getTime());

    Assert.assertEquals(buffer.position() - serializationOffset, binarySize);
  }

  @Test
  public void testSerializeInImmutableByteBufferPosition() {
    final int serializationOffset = 5;

    final ByteBuffer buffer = ByteBuffer.allocate(FIELD_SIZE + serializationOffset);

    Calendar calendar = Calendar.getInstance();
    calendar.setTime(OBJECT);
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);

    buffer.position(serializationOffset);
    dateSerializer.serializeInByteBufferObject(OBJECT, buffer);

    final int binarySize = buffer.position() - serializationOffset;
    Assert.assertEquals(binarySize, FIELD_SIZE);

    buffer.position(0);
    Assert.assertEquals(
        dateSerializer.getObjectSizeInByteBuffer(serializationOffset, buffer), FIELD_SIZE);
    Assert.assertEquals(0, buffer.position());

    Assert.assertEquals(
        dateSerializer.deserializeFromByteBufferObject(serializationOffset, buffer),
        calendar.getTime());
    Assert.assertEquals(0, buffer.position());
  }

  @Test
  public void testSerializeWALChanges() {
    final int serializationOffset = 5;
    final ByteBuffer buffer =
        ByteBuffer.allocateDirect(
                FIELD_SIZE + serializationOffset + WALPageChangesPortion.PORTION_BYTES)
            .order(ByteOrder.nativeOrder());
    final byte[] data = new byte[FIELD_SIZE];
    dateSerializer.serializeNativeObject(OBJECT, data, 0);
    final WALChanges walChanges = new WALPageChangesPortion();

    walChanges.setBinaryValue(buffer, data, serializationOffset);

    Calendar calendar = Calendar.getInstance();
    calendar.setTime(OBJECT);
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);

    Assert.assertEquals(
        dateSerializer.getObjectSizeInByteBuffer(buffer, walChanges, serializationOffset),
        FIELD_SIZE);
    Assert.assertEquals(
        dateSerializer.deserializeFromByteBufferObject(buffer, walChanges, serializationOffset),
        calendar.getTime());

    Assert.assertEquals(0, buffer.position());
  }
}
