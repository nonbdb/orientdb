/*
 *
 *
 *  *
 *  *  Licensed under the Apache License, Version 2.0 (the "License");
 *  *  you may not use this file except in compliance with the License.
 *  *  You may obtain a copy of the License at
 *  *
 *  *       http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *  Unless required by applicable law or agreed to in writing, software
 *  *  distributed under the License is distributed on an "AS IS" BASIS,
 *  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  See the License for the specific language governing permissions and
 *  *  limitations under the License.
 *  *
 *
 *
 */

package com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.wal;

import com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.wal.common.WriteableWALRecord;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Abstract WAL record.
 *
 * @since 12.12.13
 */
public abstract class AbstractWALRecord implements WriteableWALRecord {

  protected volatile LogSequenceNumber logSequenceNumber;

  private int distance = 0;
  private int diskSize = 0;

  private ByteBuffer binaryContent;
  private int binaryContentLen = -1;

  private boolean written;

  protected AbstractWALRecord() {
  }

  @Override
  public LogSequenceNumber getLsn() {
    return logSequenceNumber;
  }

  @Override
  public void setLsn(final LogSequenceNumber lsn) {
    this.logSequenceNumber = lsn;
  }

  @Override
  public void setBinaryContent(ByteBuffer buffer) {
    this.binaryContent = buffer;
    this.binaryContentLen = buffer.limit();
  }

  @Override
  public ByteBuffer getBinaryContent() {
    return binaryContent;
  }

  @Override
  public void freeBinaryContent() {
    this.binaryContent = null;
  }

  @Override
  public int getBinaryContentLen() {
    return this.binaryContentLen;
  }

  @Override
  public void setDistance(int distance) {
    this.distance = distance;
  }

  @Override
  public void setDiskSize(int diskSize) {
    this.diskSize = diskSize;
  }

  @Override
  public int getDistance() {
    if (distance <= 0) {
      throw new IllegalStateException("Record distance is not set");
    }

    return distance;
  }

  @Override
  public int getDiskSize() {
    if (diskSize <= 0) {
      throw new IllegalStateException("Record disk size is not set");
    }

    return diskSize;
  }

  @Override
  public void written() {
    written = true;
  }

  @Override
  public boolean isWritten() {
    return written;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    var that = (AbstractWALRecord) o;

    return Objects.equals(logSequenceNumber, that.logSequenceNumber);
  }

  @Override
  public int hashCode() {
    return logSequenceNumber != null ? logSequenceNumber.hashCode() : 0;
  }

  @Override
  public String toString() {
    return toString(null);
  }

  protected String toString(final String iToAppend) {
    final var buffer = new StringBuilder(getClass().getName());
    buffer.append("{lsn =").append(logSequenceNumber);

    if (iToAppend != null) {
      buffer.append(", ");
      buffer.append(iToAppend);
    }

    buffer.append('}');
    return buffer.toString();
  }
}
