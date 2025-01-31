package com.jetbrains.youtrack.db.internal.core.metadata.security.binary;

import com.jetbrains.youtrack.db.internal.core.id.RecordId;
import com.jetbrains.youtrack.db.internal.core.metadata.security.jwt.BinaryTokenPayload;
import com.jetbrains.youtrack.db.internal.core.metadata.security.jwt.TokenMetaInfo;
import com.jetbrains.youtrack.db.internal.core.metadata.security.jwt.TokenPayloadDeserializer;
import java.io.DataInputStream;
import java.io.IOException;

public class BinaryTokenPayloadDeserializer implements TokenPayloadDeserializer {

  @Override
  public BinaryTokenPayload deserialize(DataInputStream input, TokenMetaInfo base)
      throws IOException {
    var payload = new BinaryTokenPayloadImpl();

    payload.setDatabase(BinaryTokenSerializer.readString(input));
    var pos = input.readByte();
    if (pos >= 0) {
      payload.setDatabaseType(base.getDbType(pos));
    }

    var cluster = input.readShort();
    var position = input.readLong();
    if (cluster != -1 && position != -1) {
      payload.setUserRid(new RecordId(cluster, position));
    }
    payload.setExpiry(input.readLong());
    payload.setServerUser(input.readBoolean());
    if (payload.isServerUser()) {
      payload.setUserName(BinaryTokenSerializer.readString(input));
    }
    payload.setProtocolVersion(input.readShort());
    payload.setSerializer(BinaryTokenSerializer.readString(input));
    payload.setDriverName(BinaryTokenSerializer.readString(input));
    payload.setDriverVersion(BinaryTokenSerializer.readString(input));
    return payload;
  }
}
