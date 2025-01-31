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

package com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.binary;

import com.jetbrains.youtrack.db.api.record.Blob;
import com.jetbrains.youtrack.db.internal.common.log.LogManager;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.record.RecordAbstract;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.RecordSerializer;
import java.util.Base64;

public class RecordSerializerBinary implements RecordSerializer {

  public static final String NAME = "RecordSerializerBinary";
  public static final RecordSerializerBinary INSTANCE = new RecordSerializerBinary();
  private static final byte CURRENT_RECORD_VERSION = 1;

  private EntitySerializer[] serializerByVersion;
  private final byte currentSerializerVersion;

  private void init() {
    serializerByVersion = new EntitySerializer[2];
    serializerByVersion[0] = new RecordSerializerBinaryV0();
    serializerByVersion[1] = new RecordSerializerBinaryV1();
  }

  public RecordSerializerBinary(byte serializerVersion) {
    currentSerializerVersion = serializerVersion;
    init();
  }

  public RecordSerializerBinary() {
    currentSerializerVersion = CURRENT_RECORD_VERSION;
    init();
  }

  public int getNumberOfSupportedVersions() {
    return serializerByVersion.length;
  }

  @Override
  public int getCurrentVersion() {
    return currentSerializerVersion;
  }

  @Override
  public int getMinSupportedVersion() {
    return currentSerializerVersion;
  }

  public EntitySerializer getSerializer(final int iVersion) {
    return serializerByVersion[iVersion];
  }

  public EntitySerializer getCurrentSerializer() {
    return serializerByVersion[currentSerializerVersion];
  }

  @Override
  public String toString() {
    return NAME;
  }

  @Override
  public RecordAbstract fromStream(
      DatabaseSessionInternal db, final byte[] iSource, RecordAbstract iRecord,
      final String[] iFields) {
    if (iSource == null || iSource.length == 0) {
      return iRecord;
    }
    if (iRecord == null) {
      iRecord = new EntityImpl(db);
    } else if (iRecord instanceof Blob) {
      iRecord.fromStream(iSource);
      return iRecord;
    }

    final var container = new BytesContainer(iSource).skip(1);

    try {
      if (iFields != null && iFields.length > 0) {
        serializerByVersion[iSource[0]].deserializePartial(db, (EntityImpl) iRecord, container,
            iFields);
      } else {
        serializerByVersion[iSource[0]].deserialize(db, (EntityImpl) iRecord, container);
      }
    } catch (RuntimeException e) {
      LogManager.instance()
          .warn(
              this,
              "Error deserializing record with id %s send this data for debugging: %s ",
              iRecord.getIdentity().toString(),
              Base64.getEncoder().encodeToString(iSource));
      throw e;
    }
    return iRecord;
  }

  @Override
  public byte[] toStream(DatabaseSessionInternal db, RecordAbstract record) {
    if (record instanceof Blob) {
      return record.toStream();
    } else {
      var documentToSerialize = (EntityImpl) record;

      final var container = new BytesContainer();

      // WRITE SERIALIZER VERSION
      var pos = container.alloc(1);
      container.bytes[pos] = currentSerializerVersion;
      // SERIALIZE RECORD
      serializerByVersion[currentSerializerVersion].serialize(db, documentToSerialize,
          container);

      return container.fitBytes();
    }
  }

  @Override
  public String[] getFieldNames(DatabaseSessionInternal db, EntityImpl reference,
      final byte[] iSource) {
    if (iSource == null || iSource.length == 0) {
      return new String[0];
    }

    final var container = new BytesContainer(iSource).skip(1);

    try {
      return serializerByVersion[iSource[0]].getFieldNames(reference, container, false);
    } catch (RuntimeException e) {
      LogManager.instance()
          .warn(
              this,
              "Error deserializing record to get field-names, send this data for debugging: %s ",
              Base64.getEncoder().encodeToString(iSource));
      throw e;
    }
  }

  @Override
  public boolean getSupportBinaryEvaluate() {
    return true;
  }

  @Override
  public String getName() {
    return NAME;
  }
}
