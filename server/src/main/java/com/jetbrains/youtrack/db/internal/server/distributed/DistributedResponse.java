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

import com.jetbrains.youtrack.db.internal.core.serialization.StreamableHelper;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

/**
 *
 */
public class DistributedResponse {

  private ODistributedResponseManager distributedResponseManager;
  private DistributedRequestId requestId;
  private String executorNodeName;
  private String senderNodeName;
  private Object payload;

  /**
   * Constructor used by serializer.
   */
  public DistributedResponse() {
  }

  public DistributedResponse(
      final ODistributedResponseManager msg,
      final DistributedRequestId iRequestId,
      final String executorNodeName,
      final String senderNodeName,
      final Object payload) {
    this.distributedResponseManager = msg;
    this.requestId = iRequestId;
    this.executorNodeName = executorNodeName;
    this.senderNodeName = senderNodeName;
    this.payload = payload;
  }

  public DistributedRequestId getRequestId() {
    return requestId;
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof DistributedResponse && ((DistributedResponse) obj).payload != null) {
      return ((DistributedResponse) obj).payload.equals(payload);
    }

    return false;
  }

  public String getExecutorNodeName() {
    return executorNodeName;
  }

  public String getSenderNodeName() {
    return senderNodeName;
  }

  public Object getPayload() {
    return payload;
  }

  public DistributedResponse setPayload(final Object payload) {
    this.payload = payload;
    return this;
  }

  public DistributedResponse setExecutorNodeName(final String executorNodeName) {
    this.executorNodeName = executorNodeName;
    return this;
  }

  public void toStream(final DataOutput out) throws IOException {
    requestId.toStream(out);
    out.writeUTF(executorNodeName);
    out.writeUTF(senderNodeName);
    StreamableHelper.toStream(out, payload);
  }

  public void fromStream(final DataInput in) throws IOException {
    requestId = new DistributedRequestId();
    requestId.fromStream(in);
    executorNodeName = in.readUTF();
    senderNodeName = in.readUTF();
    payload = StreamableHelper.fromStream(in);
  }

  public ODistributedResponseManager getDistributedResponseManager() {
    return distributedResponseManager;
  }

  @Override
  public String toString() {
    if (payload == null) {
      return "null";
    }

    if (payload.getClass().isArray()) {
      return Arrays.toString((Object[]) payload);
    }

    return payload.toString();
  }

  public void setDistributedResponseManager(
      final ODistributedResponseManager distributedResponseManager) {
    this.distributedResponseManager = distributedResponseManager;
  }
}
