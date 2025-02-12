package com.jetbrains.youtrack.db.internal.client.remote.message;

import com.jetbrains.youtrack.db.internal.client.binary.BinaryRequestExecutor;
import com.jetbrains.youtrack.db.internal.client.remote.BinaryRequest;
import com.jetbrains.youtrack.db.internal.client.remote.BinaryResponse;
import com.jetbrains.youtrack.db.internal.client.remote.StorageRemoteSession;
import com.jetbrains.youtrack.db.internal.client.remote.message.tx.RecordOperationRequest;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.record.RecordOperation;
import com.jetbrains.youtrack.db.internal.core.record.RecordInternal;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.binary.DocumentSerializerDelta;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.binary.RecordSerializerNetwork;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.binary.RecordSerializerNetworkV37;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.binary.RecordSerializerNetworkV37Client;
import com.jetbrains.youtrack.db.internal.core.tx.FrontendTransactionIndexChanges;
import com.jetbrains.youtrack.db.internal.enterprise.channel.binary.ChannelBinaryProtocol;
import com.jetbrains.youtrack.db.internal.enterprise.channel.binary.ChannelDataInput;
import com.jetbrains.youtrack.db.internal.enterprise.channel.binary.ChannelDataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class Commit38Request implements BinaryRequest<Commit37Response> {

  private long txId;
  private boolean hasContent;
  private boolean usingLog;
  private List<RecordOperationRequest> operations;

  public Commit38Request() {
  }

  public Commit38Request(
      DatabaseSessionInternal db, long txId,
      boolean hasContent,
      boolean usingLong,
      Iterable<RecordOperation> operations,
      Map<String, FrontendTransactionIndexChanges> indexChanges) {
    this.txId = txId;
    this.hasContent = hasContent;
    this.usingLog = usingLong;
    if (hasContent) {
      List<RecordOperationRequest> netOperations = new ArrayList<>();
      for (var txEntry : operations) {
        var request = new RecordOperationRequest();
        request.setType(txEntry.type);
        request.setVersion(txEntry.record.getVersion());
        request.setId(txEntry.record.getIdentity());
        switch (txEntry.type) {
          case RecordOperation.CREATED:
            request.setRecordType(RecordInternal.getRecordType(db, txEntry.record));
            request.setRecord(
                RecordSerializerNetworkV37Client.INSTANCE.toStream(db, txEntry.record));
            request.setContentChanged(RecordInternal.isContentChanged(txEntry.record));
            break;
          case RecordOperation.UPDATED:
            if (EntityImpl.RECORD_TYPE == RecordInternal.getRecordType(db, txEntry.record)) {
              request.setRecordType(DocumentSerializerDelta.DELTA_RECORD_TYPE);
              var delta = DocumentSerializerDelta.instance();
              request.setRecord(delta.serializeDelta(db, (EntityImpl) txEntry.record));
              request.setContentChanged(RecordInternal.isContentChanged(txEntry.record));
            } else {
              request.setRecordType(RecordInternal.getRecordType(db, txEntry.record));
              request.setRecord(
                  RecordSerializerNetworkV37.INSTANCE.toStream(db, txEntry.record));
              request.setContentChanged(RecordInternal.isContentChanged(txEntry.record));
            }
            break;
        }
        netOperations.add(request);
      }
      this.operations = netOperations;
    }
  }

  @Override
  public void write(DatabaseSessionInternal databaseSession, ChannelDataOutput network,
      StorageRemoteSession session) throws IOException {
    // from 3.0 the the serializer is bound to the protocol
    var serializer = RecordSerializerNetworkV37Client.INSTANCE;
    network.writeLong(txId);
    network.writeBoolean(hasContent);
    network.writeBoolean(usingLog);
    if (hasContent) {
      for (var txEntry : operations) {
        network.writeByte((byte) 1);
        MessageHelper.writeTransactionEntry(network, txEntry, serializer);
      }

      // END OF RECORD ENTRIES
      network.writeByte((byte) 0);
    }
  }

  @Override
  public void read(DatabaseSessionInternal databaseSession, ChannelDataInput channel,
      int protocolVersion,
      RecordSerializerNetwork serializer)
      throws IOException {
    txId = channel.readLong();
    hasContent = channel.readBoolean();
    usingLog = channel.readBoolean();
    if (hasContent) {
      operations = new ArrayList<>();
      byte hasEntry;
      do {
        hasEntry = channel.readByte();
        if (hasEntry == 1) {
          var entry = MessageHelper.readTransactionEntry(channel, serializer);
          operations.add(entry);
        }
      } while (hasEntry == 1);
    }
  }

  @Override
  public byte getCommand() {
    return ChannelBinaryProtocol.REQUEST_TX_COMMIT;
  }

  @Override
  public Commit37Response createResponse() {
    return new Commit37Response();
  }

  @Override
  public BinaryResponse execute(BinaryRequestExecutor executor) {
    return executor.executeCommit38(this);
  }

  @Override
  public String getDescription() {
    return "Commit";
  }

  public long getTxId() {
    return txId;
  }

  public List<RecordOperationRequest> getOperations() {
    return operations;
  }

  public boolean isUsingLog() {
    return usingLog;
  }

  public boolean isHasContent() {
    return hasContent;
  }
}
