package com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.binary;

import com.jetbrains.youtrack.db.internal.core.db.record.MultiValueChangeEvent;
import com.jetbrains.youtrack.db.internal.core.db.record.MultiValueChangeTimeLine;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.internal.core.db.record.ridbag.RidBag;
import com.jetbrains.youtrack.db.internal.core.id.RecordId;

public class DocumentSerializerDeltaDistributed extends DocumentSerializerDelta {

  private static final DocumentSerializerDeltaDistributed INSTANCE =
      new DocumentSerializerDeltaDistributed();

  public static DocumentSerializerDeltaDistributed instance() {
    return INSTANCE;
  }

  protected void deserializeDeltaLinkBag(BytesContainer bytes, RidBag toUpdate) {
    boolean isTree = deserializeByte(bytes) == 1;
    long rootChanges = VarIntSerializer.readAsLong(bytes);
    while (rootChanges-- > 0) {
      byte change = deserializeByte(bytes);
      switch (change) {
        case CREATED: {
          RecordId link = readOptimizedLink(bytes);
          if (toUpdate != null) {
            toUpdate.add(link);
          }
          break;
        }
        case REPLACED: {
          break;
        }
        case REMOVED: {
          RecordId link = readOptimizedLink(bytes);
          if (toUpdate != null) {
            toUpdate.remove(link);
          }
          break;
        }
      }
    }
    if (toUpdate != null) {
      if (isTree) {
        toUpdate.makeTree();
      } else {
        toUpdate.makeEmbedded();
      }
    }
  }

  protected void serializeDeltaLinkBag(BytesContainer bytes, RidBag value) {
    serializeByte(bytes, value.isEmbedded() ? (byte) 0 : 1);
    MultiValueChangeTimeLine<Identifiable, Identifiable> timeline =
        value.getTransactionTimeLine();
    assert timeline != null : "Cx ollection timeline required for link types serialization";
    VarIntSerializer.write(bytes, timeline.getMultiValueChangeEvents().size());
    for (MultiValueChangeEvent<Identifiable, Identifiable> event :
        timeline.getMultiValueChangeEvents()) {
      switch (event.getChangeType()) {
        case ADD:
          serializeByte(bytes, CREATED);
          writeOptimizedLink(bytes, event.getValue());
          break;
        case UPDATE:
          throw new UnsupportedOperationException(
              "update do not happen in sets, it will be like and add");
        case REMOVE:
          serializeByte(bytes, REMOVED);
          writeOptimizedLink(bytes, event.getOldValue());
          break;
      }
    }
  }
}
