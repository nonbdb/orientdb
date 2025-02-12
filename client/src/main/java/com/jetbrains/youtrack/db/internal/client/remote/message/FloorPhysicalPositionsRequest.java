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
package com.jetbrains.youtrack.db.internal.client.remote.message;

import com.jetbrains.youtrack.db.internal.client.binary.BinaryRequestExecutor;
import com.jetbrains.youtrack.db.internal.client.remote.BinaryRequest;
import com.jetbrains.youtrack.db.internal.client.remote.BinaryResponse;
import com.jetbrains.youtrack.db.internal.client.remote.StorageRemoteSession;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.binary.RecordSerializerNetwork;
import com.jetbrains.youtrack.db.internal.core.storage.PhysicalPosition;
import com.jetbrains.youtrack.db.internal.enterprise.channel.binary.ChannelBinaryProtocol;
import com.jetbrains.youtrack.db.internal.enterprise.channel.binary.ChannelDataInput;
import com.jetbrains.youtrack.db.internal.enterprise.channel.binary.ChannelDataOutput;
import java.io.IOException;

public class FloorPhysicalPositionsRequest
    implements BinaryRequest<FloorPhysicalPositionsResponse> {

  private PhysicalPosition physicalPosition;
  private int clusterId;

  public FloorPhysicalPositionsRequest(PhysicalPosition physicalPosition, int clusterId) {
    this.physicalPosition = physicalPosition;
    this.clusterId = clusterId;
  }

  public FloorPhysicalPositionsRequest() {
  }

  @Override
  public void write(DatabaseSessionInternal databaseSession, ChannelDataOutput network,
      StorageRemoteSession session) throws IOException {
    network.writeInt(clusterId);
    network.writeLong(physicalPosition.clusterPosition);
  }

  public void read(DatabaseSessionInternal databaseSession, ChannelDataInput channel,
      int protocolVersion,
      RecordSerializerNetwork serializer)
      throws IOException {
    this.clusterId = channel.readInt();
    this.physicalPosition = new PhysicalPosition(channel.readLong());
  }

  @Override
  public byte getCommand() {
    return ChannelBinaryProtocol.REQUEST_POSITIONS_FLOOR;
  }

  @Override
  public String getDescription() {
    return "Retrieve floor positions";
  }

  public int getClusterId() {
    return clusterId;
  }

  public PhysicalPosition getPhysicalPosition() {
    return physicalPosition;
  }

  public FloorPhysicalPositionsResponse createResponse() {
    return new FloorPhysicalPositionsResponse();
  }

  @Override
  public BinaryResponse execute(BinaryRequestExecutor executor) {
    return executor.executeFloorPosition(this);
  }
}
