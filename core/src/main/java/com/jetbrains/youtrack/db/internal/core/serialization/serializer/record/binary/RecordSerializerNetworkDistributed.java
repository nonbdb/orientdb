package com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.binary;

import com.jetbrains.youtrack.db.api.exception.DatabaseException;
import com.jetbrains.youtrack.db.api.exception.RecordNotFoundException;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityEntry;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityInternalUtils;
import java.util.Collection;
import java.util.Map;

public class RecordSerializerNetworkDistributed extends RecordSerializerNetworkV37 {

  public static final RecordSerializerNetworkDistributed INSTANCE =
      new RecordSerializerNetworkDistributed();

  protected void writeOptimizedLink(DatabaseSessionInternal db, final BytesContainer bytes,
      Identifiable link) {
    if (!link.getIdentity().isPersistent()) {
      try {
        link = link.getRecord(db);
      } catch (RecordNotFoundException rnf) {
        // IGNORE IT
      }
    }

    if (!link.getIdentity().isPersistent() && !link.getIdentity().isTemporary()) {
      throw new DatabaseException(
          "Found not persistent link with no connected record, probably missing save `"
              + link.getIdentity()
              + "` ");
    }
    final var pos = VarIntSerializer.write(bytes, link.getIdentity().getClusterId());
    VarIntSerializer.write(bytes, link.getIdentity().getClusterPosition());
  }

  protected Collection<Map.Entry<String, EntityEntry>> fetchEntries(EntityImpl entity) {
    return EntityInternalUtils.rawEntries(entity);
  }
}
