package com.orientechnologies.orient.client.remote.message;

import com.orientechnologies.orient.client.remote.OBinaryResponse;
import com.orientechnologies.orient.client.remote.OStorageRemoteSession;
import com.orientechnologies.orient.core.db.ODatabaseSessionInternal;
import com.orientechnologies.orient.core.serialization.serializer.record.ORecordSerializer;
import com.orientechnologies.orient.enterprise.channel.binary.OChannelDataInput;
import com.orientechnologies.orient.enterprise.channel.binary.OChannelDataOutput;
import java.io.IOException;

/**
 *
 */
public class OOpen37Response implements OBinaryResponse {

  private int sessionId;
  private byte[] sessionToken;

  public OOpen37Response() {
  }

  public OOpen37Response(int sessionId, byte[] sessionToken) {
    this.sessionId = sessionId;
    this.sessionToken = sessionToken;
  }

  @Override
  public void write(ODatabaseSessionInternal session, OChannelDataOutput channel,
      int protocolVersion, ORecordSerializer serializer)
      throws IOException {
    channel.writeInt(sessionId);
    channel.writeBytes(sessionToken);
  }

  @Override
  public void read(ODatabaseSessionInternal db, OChannelDataInput network,
      OStorageRemoteSession session) throws IOException {
    sessionId = network.readInt();
    sessionToken = network.readBytes();
  }

  public int getSessionId() {
    return sessionId;
  }

  public byte[] getSessionToken() {
    return sessionToken;
  }
}
