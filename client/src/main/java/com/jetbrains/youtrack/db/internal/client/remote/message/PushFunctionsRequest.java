package com.jetbrains.youtrack.db.internal.client.remote.message;

import com.jetbrains.youtrack.db.internal.client.remote.RemotePushHandler;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.enterprise.channel.binary.ChannelBinaryProtocol;
import com.jetbrains.youtrack.db.internal.enterprise.channel.binary.ChannelDataInput;
import com.jetbrains.youtrack.db.internal.enterprise.channel.binary.ChannelDataOutput;
import java.io.IOException;

public class PushFunctionsRequest implements BinaryPushRequest<BinaryPushResponse> {

  public PushFunctionsRequest() {
  }

  @Override
  public void write(DatabaseSessionInternal session, ChannelDataOutput channel)
      throws IOException {
  }

  @Override
  public void read(DatabaseSessionInternal session, ChannelDataInput network) throws IOException {
  }

  @Override
  public BinaryPushResponse execute(DatabaseSessionInternal session,
      RemotePushHandler pushHandler) {
    return pushHandler.executeUpdateFunction(this);
  }

  @Override
  public BinaryPushResponse createResponse() {
    return null;
  }

  @Override
  public byte getPushCommand() {
    return ChannelBinaryProtocol.REQUEST_PUSH_FUNCTIONS;
  }
}
