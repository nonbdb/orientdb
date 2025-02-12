/*
 *
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.youtrack.db.internal.core.fetch.json;

import com.jetbrains.youtrack.db.api.exception.BaseException;
import com.jetbrains.youtrack.db.api.record.DBRecord;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.internal.common.log.LogManager;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.record.LinkSet;
import com.jetbrains.youtrack.db.internal.core.db.record.ridbag.RidBag;
import com.jetbrains.youtrack.db.internal.core.exception.FetchException;
import com.jetbrains.youtrack.db.internal.core.fetch.FetchContext;
import com.jetbrains.youtrack.db.internal.core.id.RecordId;
import com.jetbrains.youtrack.db.internal.core.record.RecordInternal;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityHelper;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.JSONWriter;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.string.FieldTypesString;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.string.RecordSerializerJackson.FormatSettings;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Set;
import java.util.Stack;

/**
 *
 */
public class JSONFetchContext implements FetchContext {

  protected final JSONWriter jsonWriter;
  protected final FormatSettings settings;
  protected final Stack<StringBuilder> typesStack = new Stack<>();
  protected final Stack<EntityImpl> collectionStack = new Stack<>();

  public JSONFetchContext(final JSONWriter jsonWriter, final FormatSettings settings) {
    this.jsonWriter = jsonWriter;
    this.settings = settings;
  }

  public void onBeforeFetch(final EntityImpl rootRecord) {
    typesStack.add(new StringBuilder());
  }

  public void onAfterFetch(DatabaseSessionInternal db, final EntityImpl rootRecord) {
    final var sb = typesStack.pop();
    if (settings.keepTypes && !sb.isEmpty()) {
      try {
        jsonWriter.writeAttribute(db,
            settings.indentLevel > -1 ? settings.indentLevel : 1,
            true,
            FieldTypesString.ATTRIBUTE_FIELD_TYPES, sb.toString());
      } catch (final IOException e) {
        throw BaseException.wrapException(
            new FetchException(db.getDatabaseName(), "Error writing field types"), e,
            db.getDatabaseName());
      }
    }
  }

  public void onBeforeStandardField(
      final Object iFieldValue,
      final String iFieldName,
      final Object iUserObject,
      PropertyType fieldType) {
    manageTypes(iFieldName, iFieldValue, fieldType);
  }

  public void onAfterStandardField(
      Object iFieldValue, String iFieldName, Object iUserObject, PropertyType fieldType) {
  }

  public void onBeforeArray(
      DatabaseSessionInternal db, final EntityImpl iRootRecord,
      final String iFieldName,
      final Object iUserObject,
      final Identifiable[] iArray) {
    onBeforeCollection(db, iRootRecord, iFieldName, iUserObject, null);
  }

  public void onAfterArray(
      DatabaseSessionInternal db, final EntityImpl iRootRecord, final String iFieldName,
      final Object iUserObject) {
    onAfterCollection(db, iRootRecord, iFieldName, iUserObject);
  }

  public void onBeforeCollection(
      DatabaseSessionInternal db, final EntityImpl rootRecord,
      final String fieldName,
      final Object userObject,
      final Iterable<?> iterable) {
    try {
      manageTypes(fieldName, iterable, null);
      jsonWriter.beginCollection(db, ++settings.indentLevel, true, fieldName);
      collectionStack.add(rootRecord);
    } catch (final IOException e) {
      throw BaseException.wrapException(
          new FetchException(db.getDatabaseName(),
              "Error writing collection field "
                  + fieldName
                  + " of record "
                  + rootRecord.getIdentity()),
          e, db.getDatabaseName());
    }
  }

  public void onAfterCollection(
      DatabaseSessionInternal db, final EntityImpl iRootRecord, final String iFieldName,
      final Object iUserObject) {
    try {
      jsonWriter.endCollection(settings.indentLevel--, true);
      collectionStack.pop();
    } catch (IOException e) {
      throw BaseException.wrapException(
          new FetchException(db.getDatabaseName(),
              "Error writing collection field "
                  + iFieldName
                  + " of record "
                  + iRootRecord.getIdentity()),
          e, db.getDatabaseName());
    }
  }

  public void onBeforeMap(
      DatabaseSessionInternal db, final EntityImpl iRootRecord, final String iFieldName,
      final Object iUserObject) {
    try {
      jsonWriter.beginObject(++settings.indentLevel, true, iFieldName);
      if (!(iUserObject instanceof EntityImpl)) {
        collectionStack.add(
            new EntityImpl(null)); // <-- sorry for this... fixes #2845 but this mess should be
        // rewritten...
      }
    } catch (IOException e) {
      throw BaseException.wrapException(
          new FetchException(db.getDatabaseName(),
              "Error writing map field " + iFieldName + " of record " + iRootRecord.getIdentity()),
          e, db.getDatabaseName());
    }
  }

  public void onAfterMap(
      DatabaseSessionInternal db, final EntityImpl iRootRecord, final String iFieldName,
      final Object iUserObject) {
    try {
      jsonWriter.endObject(--settings.indentLevel, true);
      if (!(iUserObject instanceof EntityImpl)) {
        collectionStack.pop();
      }
    } catch (IOException e) {
      throw BaseException.wrapException(
          new FetchException(db.getDatabaseName(),
              "Error writing map field " + iFieldName + " of record " + iRootRecord.getIdentity()),
          e, db.getDatabaseName());
    }
  }

  public void onBeforeDocument(
      DatabaseSessionInternal db, final EntityImpl iRootRecord,
      final EntityImpl entity,
      final String iFieldName,
      final Object iUserObject) {
    try {
      final String fieldName;
      if (!collectionStack.isEmpty() && collectionStack.peek().equals(iRootRecord)) {
        fieldName = null;
      } else {
        fieldName = iFieldName;
      }
      jsonWriter.beginObject(++settings.indentLevel, true, fieldName);
      writeSignature(db, jsonWriter, entity);
    } catch (IOException e) {
      throw BaseException.wrapException(
          new FetchException(db.getDatabaseName(),
              "Error writing link field " + iFieldName + " of record " + iRootRecord.getIdentity()),
          e, db.getDatabaseName());
    }
  }

  public void onAfterDocument(
      DatabaseSessionInternal db, final EntityImpl iRootRecord,
      final EntityImpl entity,
      final String iFieldName,
      final Object iUserObject) {
    try {
      jsonWriter.endObject(settings.indentLevel--, true);
    } catch (IOException e) {
      throw BaseException.wrapException(
          new FetchException(db.getDatabaseName(),
              "Error writing link field " + iFieldName + " of record " + iRootRecord.getIdentity()),
          e, db.getDatabaseName());
    }
  }

  public void writeLinkedValue(DatabaseSessionInternal db, final Identifiable iRecord)
      throws IOException {
    jsonWriter.writeValue(db, settings.indentLevel, true, JSONWriter.encode(iRecord.getIdentity()));
  }

  public void writeLinkedAttribute(DatabaseSessionInternal db, final Identifiable iRecord,
      final String iFieldName)
      throws IOException {
    final var link =
        ((RecordId) iRecord.getIdentity()).isValid() ? JSONWriter.encode(iRecord.getIdentity())
            : null;
    jsonWriter.writeAttribute(db, settings.indentLevel, true, iFieldName, link);
  }

  public boolean isInCollection(EntityImpl record) {
    return !collectionStack.isEmpty() && collectionStack.peek().equals(record);
  }

  public JSONWriter getJsonWriter() {
    return jsonWriter;
  }

  public int getIndentLevel() {
    return settings.indentLevel;
  }

  public void writeSignature(DatabaseSessionInternal db, final JSONWriter json,
      final DBRecord record)
      throws IOException {
    if (record == null) {
      json.write("null");
      return;
    }
    var firstAttribute = true;

    if (settings.includeType) {
      json.writeAttribute(db,
          firstAttribute ? settings.indentLevel : 1,
          firstAttribute,
          EntityHelper.ATTRIBUTE_TYPE, "" + (char) RecordInternal.getRecordType(db, record));
      if (settings.attribSameRow) {
        firstAttribute = false;
      }
    }
    if (settings.includeId && record.getIdentity() != null
        && ((RecordId) record.getIdentity()).isValid()) {
      json.writeAttribute(db,
          !firstAttribute ? settings.indentLevel : 1,
          firstAttribute,
          EntityHelper.ATTRIBUTE_RID, record.getIdentity().toString());
      if (settings.attribSameRow) {
        firstAttribute = false;
      }
    }
    if (settings.includeVer) {
      json.writeAttribute(db,
          firstAttribute ? settings.indentLevel : 1,
          firstAttribute,
          EntityHelper.ATTRIBUTE_VERSION, record.getVersion());
      if (settings.attribSameRow) {
        firstAttribute = false;
      }
    }
    if (settings.includeClazz
        && record instanceof EntityImpl
        && ((EntityImpl) record).getClassName() != null) {
      json.writeAttribute(db,
          firstAttribute ? settings.indentLevel : 1,
          firstAttribute,
          EntityHelper.ATTRIBUTE_CLASS, ((EntityImpl) record).getClassName());
      if (settings.attribSameRow) {
        firstAttribute = false;
      }
    }
  }

  public boolean fetchEmbeddedDocuments() {
    return settings.alwaysFetchEmbeddedDocuments;
  }

  protected void manageTypes(
      final String fieldName, final Object fieldValue, final PropertyType fieldType) {
    // TODO: avoid `EmptyStackException`, but check root cause
    if (typesStack.empty()) {
      typesStack.push(new StringBuilder());
      LogManager.instance()
          .debug(
              JSONFetchContext.class,
              "Type stack in `manageTypes` null for `field` %s, `value` %s, and `type` %s.",
              fieldName,
              fieldValue,
              fieldType);
    }
    if (settings.keepTypes) {
      if (fieldValue instanceof Long) {
        appendType(typesStack.peek(), fieldName, 'l');
      } else if (fieldValue instanceof Identifiable) {
        appendType(typesStack.peek(), fieldName, 'x');
      } else if (fieldValue instanceof Float) {
        appendType(typesStack.peek(), fieldName, 'f');
      } else if (fieldValue instanceof Short) {
        appendType(typesStack.peek(), fieldName, 's');
      } else if (fieldValue instanceof Double) {
        appendType(typesStack.peek(), fieldName, 'd');
      } else if (fieldValue instanceof Date) {
        appendType(typesStack.peek(), fieldName, 't');
      } else if (fieldValue instanceof Byte || fieldValue instanceof byte[]) {
        appendType(typesStack.peek(), fieldName, 'b');
      } else if (fieldValue instanceof BigDecimal) {
        appendType(typesStack.peek(), fieldName, 'c');
      } else if (fieldValue instanceof LinkSet) {
        appendType(typesStack.peek(), fieldName, 'n');
      } else if (fieldValue instanceof Set<?>) {
        appendType(typesStack.peek(), fieldName, 'e');
      } else if (fieldValue instanceof RidBag) {
        appendType(typesStack.peek(), fieldName, 'g');
      } else {
        var t = fieldType;
        if (t == null) {
          t = PropertyType.getTypeByValue(fieldValue);
        }
        if (t == PropertyType.LINKLIST) {
          appendType(typesStack.peek(), fieldName, 'z');
        } else if (t == PropertyType.LINKMAP) {
          appendType(typesStack.peek(), fieldName, 'm');
        } else if (t == PropertyType.CUSTOM) {
          appendType(typesStack.peek(), fieldName, 'u');
        }
      }
    }
  }

  private void appendType(final StringBuilder iBuffer, final String iFieldName, final char iType) {
    if (iBuffer.length() > 0) {
      iBuffer.append(',');
    }
    iBuffer.append(iFieldName);
    iBuffer.append('=');
    iBuffer.append(iType);
  }
}
