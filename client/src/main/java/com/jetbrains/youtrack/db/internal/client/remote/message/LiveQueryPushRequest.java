package com.jetbrains.youtrack.db.internal.client.remote.message;

import com.jetbrains.youtrack.db.internal.client.remote.RemotePushHandler;
import com.jetbrains.youtrack.db.internal.client.remote.message.live.LiveQueryResult;
import com.jetbrains.youtrack.db.internal.common.exception.ErrorCode;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.binary.RecordSerializerNetworkV37;
import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.internal.enterprise.channel.binary.ChannelBinaryProtocol;
import com.jetbrains.youtrack.db.internal.enterprise.channel.binary.ChannelDataInput;
import com.jetbrains.youtrack.db.internal.enterprise.channel.binary.ChannelDataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class LiveQueryPushRequest implements BinaryPushRequest {

  public static final byte HAS_MORE = 1;
  public static final byte END = 2;
  public static final byte ERROR = 3;

  private int monitorId;
  private byte status;
  private int errorIdentifier;
  private ErrorCode errorCode;
  private String errorMessage;
  private List<LiveQueryResult> events;

  public LiveQueryPushRequest(
      int monitorId, int errorIdentifier, ErrorCode errorCode, String errorMessage) {
    this.monitorId = monitorId;
    this.status = ERROR;
    this.errorIdentifier = errorIdentifier;
    this.errorCode = errorCode;
    this.errorMessage = errorMessage;
  }

  public LiveQueryPushRequest(int monitorId, byte status, List<LiveQueryResult> events) {
    this.monitorId = monitorId;
    this.status = status;
    this.events = events;
  }

  public LiveQueryPushRequest() {
  }

  @Override
  public void write(DatabaseSessionInternal session, ChannelDataOutput channel)
      throws IOException {
    channel.writeInt(monitorId);
    channel.writeByte(status);
    if (status == ERROR) {
      channel.writeInt(errorIdentifier);
      channel.writeInt(errorCode.getCode());
      channel.writeString(errorMessage);
    } else {
      channel.writeInt(events.size());
      for (var event : events) {
        channel.writeByte(event.getEventType());
        MessageHelper.writeResult(session,
            event.getCurrentValue(), channel, RecordSerializerNetworkV37.INSTANCE);
        if (event.getEventType() == LiveQueryResult.UPDATE_EVENT) {
          MessageHelper.writeResult(session,
              event.getOldValue(), channel, RecordSerializerNetworkV37.INSTANCE);
        }
      }
    }
  }

  @Override
  public void read(DatabaseSessionInternal session, ChannelDataInput network) throws IOException {
    monitorId = network.readInt();
    status = network.readByte();
    if (status == ERROR) {
      errorIdentifier = network.readInt();
      errorCode = ErrorCode.getErrorCode(network.readInt());
      errorMessage = network.readString();
    } else {
      var eventSize = network.readInt();
      events = new ArrayList<>(eventSize);
      while (eventSize-- > 0) {
        var type = network.readByte();
        Result currentValue = MessageHelper.readResult(session, network);
        Result oldValue = null;
        if (type == LiveQueryResult.UPDATE_EVENT) {
          oldValue = MessageHelper.readResult(session, network);
        }
        events.add(new LiveQueryResult(type, currentValue, oldValue));
      }
    }
  }

  @Override
  public BinaryPushResponse execute(DatabaseSessionInternal session, RemotePushHandler remote) {
    remote.executeLiveQueryPush(this);
    return null;
  }

  @Override
  public BinaryPushResponse createResponse() {
    return null;
  }

  @Override
  public byte getPushCommand() {
    return ChannelBinaryProtocol.REQUEST_PUSH_LIVE_QUERY;
  }

  public int getMonitorId() {
    return monitorId;
  }

  public List<LiveQueryResult> getEvents() {
    return events;
  }

  public byte getStatus() {
    return status;
  }

  public void setStatus(byte status) {
    this.status = status;
  }

  public int getErrorIdentifier() {
    return errorIdentifier;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public ErrorCode getErrorCode() {
    return errorCode;
  }
}
