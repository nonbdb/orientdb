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
import com.jetbrains.youtrack.db.internal.client.remote.StorageRemote;
import com.jetbrains.youtrack.db.internal.client.remote.StorageRemoteSession;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.RecordSerializer;
import com.jetbrains.youtrack.db.internal.enterprise.channel.binary.ChannelBinaryProtocol;
import com.jetbrains.youtrack.db.internal.enterprise.channel.binary.ChannelDataInput;
import com.jetbrains.youtrack.db.internal.enterprise.channel.binary.ChannelDataOutput;
import java.io.IOException;
import java.util.Map;

public final class QueryRequest implements BinaryRequest<QueryResponse> {

  public static byte COMMAND = 0;
  public static byte QUERY = 1;
  public static byte EXECUTE = 2;

  private int recordsPerPage = 100;
  private RecordSerializer serializer;
  private String language;
  private String statement;
  private byte operationType;
  private Map<String, Object> params;
  private byte[] paramsBytes;
  private boolean namedParams;

  public QueryRequest(
      DatabaseSessionInternal session, String language,
      String iCommand,
      Object[] positionalParams,
      byte operationType,
      RecordSerializer serializer,
      int recordsPerPage) {
    this.language = language;
    this.statement = iCommand;
    params = StorageRemote.paramsArrayToParamsMap(positionalParams);
    namedParams = false;
    this.serializer = serializer;
    this.recordsPerPage = recordsPerPage;
    if (this.recordsPerPage <= 0) {
      this.recordsPerPage = 100;
    }
    this.operationType = operationType;
    EntityImpl parms = new EntityImpl();
    parms.field("params", this.params);

    paramsBytes = MessageHelper.getRecordBytes(session, parms, serializer);
  }

  public QueryRequest(
      DatabaseSessionInternal session, String language,
      String iCommand,
      Map<String, Object> namedParams,
      byte operationType,
      RecordSerializer serializer,
      int recordsPerPage) {
    this.language = language;
    this.statement = iCommand;
    this.params = namedParams;
    EntityImpl parms = new EntityImpl(session);
    parms.field("params", this.params);

    paramsBytes = MessageHelper.getRecordBytes(session, parms, serializer);
    this.namedParams = true;
    this.serializer = serializer;
    this.recordsPerPage = recordsPerPage;
    if (this.recordsPerPage <= 0) {
      this.recordsPerPage = 100;
    }
    this.operationType = operationType;
  }

  public QueryRequest() {
  }

  @Override
  public void write(DatabaseSessionInternal database, ChannelDataOutput network,
      StorageRemoteSession session) throws IOException {
    network.writeString(language);
    network.writeString(statement);
    network.writeByte(operationType);
    network.writeInt(recordsPerPage);
    // THIS IS FOR POSSIBLE FUTURE FETCH PLAN
    network.writeString(null);

    // params
    network.writeBytes(paramsBytes);
    network.writeBoolean(namedParams);
  }

  public void read(DatabaseSessionInternal db, ChannelDataInput channel, int protocolVersion,
      RecordSerializer serializer)
      throws IOException {
    this.language = channel.readString();
    this.statement = channel.readString();
    this.operationType = channel.readByte();
    this.recordsPerPage = channel.readInt();
    // THIS IS FOR POSSIBLE FUTURE FETCH PLAN
    channel.readString();

    this.paramsBytes = channel.readBytes();
    this.namedParams = channel.readBoolean();
    this.serializer = serializer;
  }

  @Override
  public byte getCommand() {
    return ChannelBinaryProtocol.REQUEST_QUERY;
  }

  @Override
  public String getDescription() {
    return "Execute remote query";
  }

  @Override
  public QueryResponse createResponse() {
    return new QueryResponse();
  }

  @Override
  public BinaryResponse execute(BinaryRequestExecutor executor) {
    return executor.executeQuery(this);
  }

  public String getStatement() {
    return statement;
  }

  public Map<String, Object> getParams(DatabaseSessionInternal db) {
    if (params == null && this.paramsBytes != null) {
      // params
      EntityImpl paramsEntity = new EntityImpl();
      paramsEntity.setTrackingChanges(false);
      serializer.fromStream(db, this.paramsBytes, paramsEntity, null);
      this.params = paramsEntity.field("params");
    }
    return params;
  }

  public byte getOperationType() {
    return operationType;
  }

  public boolean isNamedParams() {
    return namedParams;
  }

  public Map getNamedParameters(DatabaseSessionInternal db) {
    return getParams(db);
  }

  public Object[] getPositionalParameters(DatabaseSessionInternal db) {
    Map<String, Object> params = getParams(db);
    if (params == null) {
      return null;
    }
    Object[] result = new Object[params.size()];
    params
        .entrySet()
        .forEach(
            e -> {
              result[Integer.parseInt(e.getKey())] = e.getValue();
            });
    return result;
  }

  public int getRecordsPerPage() {
    return recordsPerPage;
  }

  public RecordSerializer getSerializer() {
    return serializer;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }
}
