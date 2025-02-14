/*
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
package com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.wal;

import com.jetbrains.youtrack.db.internal.common.serialization.types.LongSerializer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * @since 30.05.13
 */
public abstract class OperationUnitRecord extends AbstractWALRecord {

  private long operationUnitId;

  protected OperationUnitRecord() {
  }

  protected OperationUnitRecord(long operationUnitId) {
    this.operationUnitId = operationUnitId;
  }

  public long getOperationUnitId() {
    return operationUnitId;
  }

  public void setOperationUnitId(final long operationUnitId) {
    this.operationUnitId = operationUnitId;
  }

  @Override
  public final int toStream(final byte[] content, final int offset) {
    final ByteBuffer buffer = ByteBuffer.wrap(content).order(ByteOrder.nativeOrder());
    buffer.position(offset);

    buffer.putLong(operationUnitId);

    serializeToByteBuffer(buffer);

    return buffer.position();
  }

  @Override
  public final void toStream(ByteBuffer buffer) {
    assert buffer.order().equals(ByteOrder.nativeOrder());

    buffer.putLong(operationUnitId);

    serializeToByteBuffer(buffer);
  }

  @Override
  public final int fromStream(final byte[] content, final int offset) {
    final ByteBuffer buffer = ByteBuffer.wrap(content).order(ByteOrder.nativeOrder());
    buffer.position(offset);

    operationUnitId = buffer.getLong();

    deserializeFromByteBuffer(buffer);

    return buffer.position();
  }

  @Override
  public int serializedSize() {
    return LongSerializer.LONG_SIZE;
  }

  protected abstract void serializeToByteBuffer(final ByteBuffer buffer);

  protected abstract void deserializeFromByteBuffer(final ByteBuffer buffer);

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final OperationUnitRecord that = (OperationUnitRecord) o;

    return operationUnitId == that.operationUnitId;
  }

  @Override
  public int hashCode() {
    return Long.hashCode(operationUnitId);
  }

  @Override
  public String toString() {
    return toString("operationUnitId=" + operationUnitId);
  }
}
