package com.orientechnologies.orient.client.remote.message;

import com.orientechnologies.orient.client.remote.ORemotePushHandler;
import com.orientechnologies.orient.core.db.ODatabaseSessionInternal;
import com.orientechnologies.orient.enterprise.channel.binary.OChannelBinaryProtocol;
import com.orientechnologies.orient.enterprise.channel.binary.OChannelDataInput;
import com.orientechnologies.orient.enterprise.channel.binary.OChannelDataOutput;
import java.io.IOException;

public class OPushFunctionsRequest implements OBinaryPushRequest<OBinaryPushResponse> {

  public OPushFunctionsRequest() {
  }

  @Override
  public void write(ODatabaseSessionInternal session, OChannelDataOutput channel)
      throws IOException {
  }

  @Override
  public void read(ODatabaseSessionInternal db, OChannelDataInput network) throws IOException {
  }

  @Override
  public OBinaryPushResponse execute(ODatabaseSessionInternal session,
      ORemotePushHandler pushHandler) {
    return pushHandler.executeUpdateFunction(this);
  }

  @Override
  public OBinaryPushResponse createResponse() {
    return null;
  }

  @Override
  public byte getPushCommand() {
    return OChannelBinaryProtocol.REQUEST_PUSH_FUNCTIONS;
  }
}
