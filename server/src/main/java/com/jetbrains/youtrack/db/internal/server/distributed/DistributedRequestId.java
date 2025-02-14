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
package com.jetbrains.youtrack.db.internal.server.distributed;

import com.jetbrains.youtrack.db.internal.core.serialization.Streamable;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Immutable object representing the distributed request id.
 */
public class DistributedRequestId implements Comparable, Streamable, Externalizable {

  private int nodeId;
  private long messageId;

  public DistributedRequestId() {
  }

  public DistributedRequestId(final int iNodeId, final long iMessageId) {
    nodeId = iNodeId;
    messageId = iMessageId;
  }

  public long getMessageId() {
    return messageId;
  }

  public int getNodeId() {
    return nodeId;
  }

  @Override
  public boolean equals(final Object obj) {
    if (!(obj instanceof DistributedRequestId other)) {
      return false;
    }

    return nodeId == other.nodeId && messageId == other.messageId;
  }

  @Override
  public int compareTo(final Object obj) {
    if (!(obj instanceof DistributedRequestId other)) {
      return -1;
    }

    return Integer.compare(hashCode(), other.hashCode());
  }

  @Override
  public int hashCode() {
    return 31 * nodeId + 103 * (int) messageId;
  }

  public void toStream(final DataOutput out) throws IOException {
    out.writeInt(nodeId);
    out.writeLong(messageId);
  }

  public void fromStream(final DataInput in) throws IOException {
    nodeId = in.readInt();
    messageId = in.readLong();
  }

  @Override
  public void writeExternal(final ObjectOutput out) throws IOException {
    out.writeInt(nodeId);
    out.writeLong(messageId);
  }

  @Override
  public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
    nodeId = in.readInt();
    messageId = in.readLong();
  }

  @Override
  public String toString() {
    return nodeId + "." + messageId;
  }
}
