package com.jetbrains.youtrack.db.internal.client.remote.message;

import com.jetbrains.youtrack.db.internal.client.binary.BinaryRequestExecutor;
import com.jetbrains.youtrack.db.internal.client.remote.BinaryRequest;
import com.jetbrains.youtrack.db.internal.client.remote.BinaryResponse;
import com.jetbrains.youtrack.db.internal.client.remote.StorageRemoteSession;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.RecordSerializer;
import com.jetbrains.youtrack.db.internal.enterprise.channel.binary.ChannelBinaryProtocol;
import com.jetbrains.youtrack.db.internal.enterprise.channel.binary.ChannelDataInput;
import com.jetbrains.youtrack.db.internal.enterprise.channel.binary.ChannelDataOutput;
import java.io.IOException;

/**
 *
 */
public class UnsubscribeLiveQueryRequest implements BinaryRequest<UnsubscribLiveQueryResponse> {

  private int monitorId;

  public UnsubscribeLiveQueryRequest() {
  }

  public UnsubscribeLiveQueryRequest(int monitorId) {
    this.monitorId = monitorId;
  }

  @Override
  public void write(DatabaseSessionInternal database, ChannelDataOutput network,
      StorageRemoteSession session) throws IOException {
    network.writeInt(monitorId);
  }

  @Override
  public void read(DatabaseSessionInternal db, ChannelDataInput channel, int protocolVersion,
      RecordSerializer serializer)
      throws IOException {
    monitorId = channel.readInt();
  }

  @Override
  public byte getCommand() {
    return ChannelBinaryProtocol.UNSUBSCRIBE_PUSH_LIVE_QUERY;
  }

  @Override
  public UnsubscribLiveQueryResponse createResponse() {
    return new UnsubscribLiveQueryResponse();
  }

  @Override
  public BinaryResponse execute(BinaryRequestExecutor executor) {
    return executor.executeUnsubscribeLiveQuery(this);
  }

  public int getMonitorId() {
    return monitorId;
  }

  @Override
  public String getDescription() {
    return "unsubscribe from live query";
  }
}
