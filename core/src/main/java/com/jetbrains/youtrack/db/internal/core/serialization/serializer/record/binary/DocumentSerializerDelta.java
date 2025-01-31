package com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.binary;

import static com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.binary.HelperClasses.MILLISEC_PER_DAY;
import static com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.binary.HelperClasses.NULL_RECORD_ID;
import static com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.binary.HelperClasses.convertDayToTimezone;
import static com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.binary.HelperClasses.getLinkedType;
import static com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.binary.HelperClasses.getTypeFromValueEmbedded;
import static com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.binary.HelperClasses.readBinary;
import static com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.binary.HelperClasses.readByte;
import static com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.binary.HelperClasses.readInteger;
import static com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.binary.HelperClasses.readLong;
import static com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.binary.HelperClasses.readOType;
import static com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.binary.HelperClasses.readString;
import static com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.binary.HelperClasses.writeBinary;
import static com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.binary.HelperClasses.writeNullLink;
import static com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.binary.HelperClasses.writeOType;
import static com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.binary.HelperClasses.writeString;

import com.jetbrains.youtrack.db.api.exception.RecordNotFoundException;
import com.jetbrains.youtrack.db.api.exception.ValidationException;
import com.jetbrains.youtrack.db.api.record.DBRecord;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.api.record.RID;
import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import com.jetbrains.youtrack.db.api.schema.SchemaProperty;
import com.jetbrains.youtrack.db.internal.common.collection.MultiValue;
import com.jetbrains.youtrack.db.internal.common.serialization.types.DecimalSerializer;
import com.jetbrains.youtrack.db.internal.common.serialization.types.IntegerSerializer;
import com.jetbrains.youtrack.db.internal.common.serialization.types.LongSerializer;
import com.jetbrains.youtrack.db.internal.common.serialization.types.UUIDSerializer;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseRecordThreadLocal;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.record.LinkList;
import com.jetbrains.youtrack.db.internal.core.db.record.LinkMap;
import com.jetbrains.youtrack.db.internal.core.db.record.LinkSet;
import com.jetbrains.youtrack.db.internal.core.db.record.MultiValueChangeEvent;
import com.jetbrains.youtrack.db.internal.core.db.record.MultiValueChangeTimeLine;
import com.jetbrains.youtrack.db.internal.core.db.record.RecordElement;
import com.jetbrains.youtrack.db.internal.core.db.record.TrackedList;
import com.jetbrains.youtrack.db.internal.core.db.record.TrackedMap;
import com.jetbrains.youtrack.db.internal.core.db.record.TrackedMultiValue;
import com.jetbrains.youtrack.db.internal.core.db.record.TrackedSet;
import com.jetbrains.youtrack.db.internal.core.db.record.ridbag.RidBag;
import com.jetbrains.youtrack.db.internal.core.exception.SerializationException;
import com.jetbrains.youtrack.db.internal.core.id.RecordId;
import com.jetbrains.youtrack.db.internal.core.record.impl.EmbeddedEntityImpl;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityEntry;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityInternalUtils;
import com.jetbrains.youtrack.db.internal.core.serialization.EntitySerializable;
import com.jetbrains.youtrack.db.internal.core.serialization.SerializableStream;
import com.jetbrains.youtrack.db.internal.core.storage.ridbag.BTreeCollectionManager;
import com.jetbrains.youtrack.db.internal.core.storage.ridbag.BonsaiCollectionPointer;
import com.jetbrains.youtrack.db.internal.core.storage.ridbag.Change;
import com.jetbrains.youtrack.db.internal.core.storage.ridbag.ChangeSerializationHelper;
import com.jetbrains.youtrack.db.internal.core.storage.ridbag.ridbagbtree.RidBagBucketPointer;
import com.jetbrains.youtrack.db.internal.core.util.DateHelper;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

public class DocumentSerializerDelta {

  protected static final byte CREATED = 1;
  protected static final byte REPLACED = 2;
  protected static final byte CHANGED = 3;
  protected static final byte REMOVED = 4;
  public static final byte DELTA_RECORD_TYPE = 10;

  private static final DocumentSerializerDelta INSTANCE = new DocumentSerializerDelta();

  public static DocumentSerializerDelta instance() {
    return INSTANCE;
  }

  protected DocumentSerializerDelta() {
  }

  public byte[] serialize(DatabaseSessionInternal db, EntityImpl entity) {
    BytesContainer bytes = new BytesContainer();
    serialize(db, entity, bytes);
    return bytes.fitBytes();
  }

  public byte[] serializeDelta(DatabaseSessionInternal db, EntityImpl entity) {
    BytesContainer bytes = new BytesContainer();
    serializeDelta(db, bytes, entity);
    return bytes.fitBytes();
  }

  protected void serializeClass(final EntityImpl entity, final BytesContainer bytes) {
    final SchemaClass clazz = EntityInternalUtils.getImmutableSchemaClass(entity);
    String name = null;
    if (clazz != null) {
      name = clazz.getName();
    }
    if (name == null) {
      name = entity.getClassName();
    }

    if (name != null) {
      writeString(bytes, name);
    } else {
      writeEmptyString(bytes);
    }
  }

  private int writeEmptyString(final BytesContainer bytes) {
    return VarIntSerializer.write(bytes, 0);
  }

  private void serialize(DatabaseSessionInternal db, final EntityImpl entity,
      final BytesContainer bytes) {
    serializeClass(entity, bytes);
    SchemaClass oClass = EntityInternalUtils.getImmutableSchemaClass(entity);
    final Set<Map.Entry<String, EntityEntry>> fields = EntityInternalUtils.rawEntries(entity);
    VarIntSerializer.write(bytes, entity.fields());
    for (Map.Entry<String, EntityEntry> entry : fields) {
      EntityEntry entityEntry = entry.getValue();
      if (!entityEntry.exists()) {
        continue;
      }
      writeString(bytes, entry.getKey());
      final Object value = entry.getValue().value;
      if (value != null) {
        final PropertyType type = getFieldType(entry.getValue());
        if (type == null) {
          throw new SerializationException(
              "Impossible serialize value of type "
                  + value.getClass()
                  + " with the Result binary serializer");
        }
        writeNullableType(bytes, type);
        serializeValue(db, bytes, value, type, getLinkedType(oClass, type, entry.getKey()));
      } else {
        writeNullableType(bytes, null);
      }
    }
  }

  public void deserialize(DatabaseSessionInternal session, byte[] content, EntityImpl toFill) {
    BytesContainer bytesContainer = new BytesContainer(content);
    deserialize(session, toFill, bytesContainer);
  }

  private void deserialize(DatabaseSessionInternal session, final EntityImpl entity,
      final BytesContainer bytes) {
    final String className = readString(bytes);
    if (!className.isEmpty()) {
      EntityInternalUtils.fillClassNameIfNeeded(entity, className);
    }

    String fieldName;
    PropertyType type;
    Object value;
    int size = VarIntSerializer.readAsInteger(bytes);
    while ((size--) > 0) {
      // PARSE FIELD NAME
      fieldName = readString(bytes);
      type = readNullableType(bytes);
      if (type == null) {
        value = null;
      } else {
        value = deserializeValue(session, bytes, type, entity);
      }
      entity.setPropertyInternal(fieldName, value, type);
    }
  }

  public void deserializeDelta(DatabaseSessionInternal session, byte[] content,
      EntityImpl toFill) {
    BytesContainer bytesContainer = new BytesContainer(content);
    deserializeDelta(session, bytesContainer, toFill);
  }

  public void deserializeDelta(DatabaseSessionInternal session, BytesContainer bytes,
      EntityImpl toFill) {
    final String className = readString(bytes);
    if (!className.isEmpty() && toFill != null) {
      EntityInternalUtils.fillClassNameIfNeeded(toFill, className);
    }
    long count = VarIntSerializer.readAsLong(bytes);
    while (count-- > 0) {
      switch (deserializeByte(bytes)) {
        case CREATED, REPLACED:
          deserializeFullEntry(session, bytes, toFill);
          break;
        case CHANGED:
          deserializeDeltaEntry(session, bytes, toFill);
          break;
        case REMOVED:
          String property = readString(bytes);
          if (toFill != null) {
            toFill.removePropertyInternal(property);
          }
          break;
      }
    }
  }

  private void deserializeDeltaEntry(DatabaseSessionInternal session, BytesContainer bytes,
      EntityImpl toFill) {
    String name = readString(bytes);
    PropertyType type = readNullableType(bytes);
    Object toUpdate;
    if (toFill != null) {
      toUpdate = toFill.getPropertyInternal(name);
    } else {
      toUpdate = null;
    }
    deserializeDeltaValue(session, bytes, type, toUpdate);
  }

  private void deserializeDeltaValue(DatabaseSessionInternal db, BytesContainer bytes,
      PropertyType type, Object toUpdate) {
    switch (type) {
      case EMBEDDEDLIST:
        deserializeDeltaEmbeddedList(db, bytes, (TrackedList) toUpdate);
        break;
      case EMBEDDEDSET:
        deserializeDeltaEmbeddedSet(db, bytes, (TrackedSet) toUpdate);
        break;
      case EMBEDDEDMAP:
        deserializeDeltaEmbeddedMap(db, bytes, (TrackedMap) toUpdate);
        break;
      case EMBEDDED:
        deserializeDelta(db, bytes, ((DBRecord) toUpdate).getRecord(db));
        break;
      case LINKLIST:
        deserializeDeltaLinkList(db, bytes, (LinkList) toUpdate);
        break;
      case LINKSET:
        deserializeDeltaLinkSet(db, bytes, (LinkSet) toUpdate);
        break;
      case LINKMAP:
        deserializeDeltaLinkMap(db, bytes, (LinkMap) toUpdate);
        break;
      case LINKBAG:
        deserializeDeltaLinkBag(db, bytes, (RidBag) toUpdate);
        break;
      default:
        throw new SerializationException("delta not supported for type:" + type);
    }
  }

  private static void deserializeDeltaLinkMap(DatabaseSessionInternal db, BytesContainer bytes,
      LinkMap toUpdate) {
    long rootChanges = VarIntSerializer.readAsLong(bytes);
    while (rootChanges-- > 0) {
      byte change = deserializeByte(bytes);
      switch (change) {
        case CREATED: {
          String key = readString(bytes);
          RecordId link = readOptimizedLink(db, bytes);
          if (toUpdate != null) {
            toUpdate.put(key, link);
          }
          break;
        }
        case REPLACED: {
          String key = readString(bytes);
          RecordId link = readOptimizedLink(db, bytes);
          if (toUpdate != null) {
            toUpdate.put(key, link);
          }
          break;
        }
        case REMOVED: {
          String key = readString(bytes);
          if (toUpdate != null) {
            toUpdate.remove(key);
          }
          break;
        }
      }
    }
  }

  protected void deserializeDeltaLinkBag(DatabaseSessionInternal db, BytesContainer bytes,
      RidBag toUpdate) {
    UUID uuid = UUIDSerializer.INSTANCE.deserialize(bytes.bytes, bytes.offset);
    bytes.skip(UUIDSerializer.UUID_SIZE);
    if (toUpdate != null) {
      toUpdate.setTemporaryId(uuid);
    }
    long rootChanges = VarIntSerializer.readAsLong(bytes);
    while (rootChanges-- > 0) {
      byte change = deserializeByte(bytes);
      switch (change) {
        case CREATED: {
          RecordId link = readOptimizedLink(db, bytes);
          if (toUpdate != null) {
            toUpdate.add(link);
          }
          break;
        }
        case REPLACED: {
          break;
        }
        case REMOVED: {
          RecordId link = readOptimizedLink(db, bytes);
          if (toUpdate != null) {
            toUpdate.remove(link);
          }
          break;
        }
      }
    }
  }

  private static void deserializeDeltaLinkList(DatabaseSessionInternal db, BytesContainer bytes,
      LinkList toUpdate) {
    long rootChanges = VarIntSerializer.readAsLong(bytes);
    while (rootChanges-- > 0) {
      byte change = deserializeByte(bytes);
      switch (change) {
        case CREATED: {
          RecordId link = readOptimizedLink(db, bytes);
          if (toUpdate != null) {
            toUpdate.add(link);
          }
          break;
        }
        case REPLACED: {
          long position = VarIntSerializer.readAsLong(bytes);
          RecordId link = readOptimizedLink(db, bytes);
          if (toUpdate != null) {
            toUpdate.set((int) position, link);
          }
          break;
        }
        case REMOVED: {
          RecordId link = readOptimizedLink(db, bytes);
          if (toUpdate != null) {
            toUpdate.remove(link);
          }
          break;
        }
      }
    }
  }

  private static void deserializeDeltaLinkSet(DatabaseSessionInternal db, BytesContainer bytes,
      LinkSet toUpdate) {
    long rootChanges = VarIntSerializer.readAsLong(bytes);
    while (rootChanges-- > 0) {
      byte change = deserializeByte(bytes);
      switch (change) {
        case CREATED: {
          RecordId link = readOptimizedLink(db, bytes);
          if (toUpdate != null) {
            toUpdate.add(link);
          }
          break;
        }
        case REPLACED: {
          break;
        }
        case REMOVED: {
          RecordId link = readOptimizedLink(db, bytes);
          if (toUpdate != null) {
            toUpdate.remove(link);
          }
          break;
        }
      }
    }
  }

  private void deserializeDeltaEmbeddedMap(DatabaseSessionInternal session, BytesContainer bytes,
      TrackedMap toUpdate) {
    long rootChanges = VarIntSerializer.readAsLong(bytes);
    while (rootChanges-- > 0) {
      byte change = deserializeByte(bytes);
      switch (change) {
        case CREATED: {
          String key = readString(bytes);
          PropertyType type = readNullableType(bytes);
          Object value;
          if (type != null) {
            value = deserializeValue(session, bytes, type, toUpdate);
          } else {
            value = null;
          }
          if (toUpdate != null) {
            toUpdate.put(key, value);
          }
          break;
        }
        case REPLACED: {
          String key = readString(bytes);
          PropertyType type = readNullableType(bytes);
          Object value;
          if (type != null) {
            value = deserializeValue(session, bytes, type, toUpdate);
          } else {
            value = null;
          }
          if (toUpdate != null) {
            toUpdate.put(key, value);
          }
          break;
        }
        case REMOVED:
          String key = readString(bytes);
          if (toUpdate != null) {
            toUpdate.remove(key);
          }
          break;
      }
    }
    long nestedChanges = VarIntSerializer.readAsLong(bytes);
    while (nestedChanges-- > 0) {
      byte other = deserializeByte(bytes);
      assert other == CHANGED;
      String key = readString(bytes);
      Object nested;
      if (toUpdate != null) {
        nested = toUpdate.get(key);
      } else {
        nested = null;
      }
      PropertyType type = readNullableType(bytes);
      deserializeDeltaValue(session, bytes, type, nested);
    }
  }

  private void deserializeDeltaEmbeddedSet(DatabaseSessionInternal session, BytesContainer bytes,
      TrackedSet toUpdate) {
    long rootChanges = VarIntSerializer.readAsLong(bytes);
    while (rootChanges-- > 0) {
      byte change = deserializeByte(bytes);
      switch (change) {
        case CREATED: {
          PropertyType type = readNullableType(bytes);
          Object value;
          if (type != null) {
            value = deserializeValue(session, bytes, type, toUpdate);
          } else {
            value = null;
          }
          if (toUpdate != null) {
            toUpdate.add(value);
          }
          break;
        }
        case REPLACED:
          assert false : "this can't ever happen";
        case REMOVED:
          PropertyType type = readNullableType(bytes);
          Object value;
          if (type != null) {
            value = deserializeValue(session, bytes, type, toUpdate);
          } else {
            value = null;
          }
          if (toUpdate != null) {
            toUpdate.remove(value);
          }
          break;
      }
    }
    long nestedChanges = VarIntSerializer.readAsLong(bytes);
    while (nestedChanges-- > 0) {
      byte other = deserializeByte(bytes);
      assert other == CHANGED;
      long position = VarIntSerializer.readAsLong(bytes);
      PropertyType type = readNullableType(bytes);
      Object nested;
      if (toUpdate != null) {
        Iterator iter = toUpdate.iterator();
        for (int i = 0; i < position; i++) {
          iter.next();
        }
        nested = iter.next();
      } else {
        nested = null;
      }

      deserializeDeltaValue(session, bytes, type, nested);
    }
  }

  private void deserializeDeltaEmbeddedList(DatabaseSessionInternal session, BytesContainer bytes,
      TrackedList toUpdate) {
    long rootChanges = VarIntSerializer.readAsLong(bytes);
    while (rootChanges-- > 0) {
      byte change = deserializeByte(bytes);
      switch (change) {
        case CREATED: {
          PropertyType type = readNullableType(bytes);
          Object value;
          if (type != null) {
            value = deserializeValue(session, bytes, type, toUpdate);
          } else {
            value = null;
          }
          if (toUpdate != null) {
            toUpdate.add(value);
          }
          break;
        }
        case REPLACED: {
          long pos = VarIntSerializer.readAsLong(bytes);
          PropertyType type = readNullableType(bytes);
          Object value;
          if (type != null) {
            value = deserializeValue(session, bytes, type, toUpdate);
          } else {
            value = null;
          }
          if (toUpdate != null) {
            toUpdate.set((int) pos, value);
          }
          break;
        }
        case REMOVED: {
          long pos = VarIntSerializer.readAsLong(bytes);
          if (toUpdate != null) {
            toUpdate.remove((int) pos);
          }
          break;
        }
      }
    }
    long nestedChanges = VarIntSerializer.readAsLong(bytes);
    while (nestedChanges-- > 0) {
      byte other = deserializeByte(bytes);
      assert other == CHANGED;
      long position = VarIntSerializer.readAsLong(bytes);
      Object nested;
      if (toUpdate != null) {
        nested = toUpdate.get((int) position);
      } else {
        nested = null;
      }
      PropertyType type = readNullableType(bytes);
      deserializeDeltaValue(session, bytes, type, nested);
    }
  }

  private void deserializeFullEntry(DatabaseSessionInternal session, BytesContainer bytes,
      EntityImpl toFill) {
    String name = readString(bytes);
    PropertyType type = readNullableType(bytes);
    Object value;
    if (type != null) {
      value = deserializeValue(session, bytes, type, toFill);
    } else {
      value = null;
    }
    if (toFill != null) {
      toFill.compareAndSetPropertyInternal(name, value, type);
    }
  }

  public void serializeDelta(DatabaseSessionInternal db, BytesContainer bytes, EntityImpl entity) {
    serializeClass(entity, bytes);
    SchemaClass oClass = EntityInternalUtils.getImmutableSchemaClass(entity);
    long count =
        EntityInternalUtils.rawEntries(entity).stream()
            .filter(
                (e) -> {
                  EntityEntry entry = e.getValue();
                  return entry.isTxCreated()
                      || entry.isTxChanged()
                      || entry.isTxTrackedModified()
                      || !entry.isTxExists();
                })
            .count();
    Set<Map.Entry<String, EntityEntry>> entries = EntityInternalUtils.rawEntries(entity);

    VarIntSerializer.write(bytes, count);
    for (final Map.Entry<String, EntityEntry> entry : entries) {
      final EntityEntry docEntry = entry.getValue();
      if (!docEntry.isTxExists()) {
        serializeByte(bytes, REMOVED);
        writeString(bytes, entry.getKey());
      } else if (docEntry.isTxCreated()) {
        serializeByte(bytes, CREATED);
        serializeFullEntry(db, bytes, oClass, entry.getKey(), docEntry);
      } else if (docEntry.isTxChanged()) {
        serializeByte(bytes, REPLACED);
        serializeFullEntry(db, bytes, oClass, entry.getKey(), docEntry);
      } else if (docEntry.isTxTrackedModified()) {
        serializeByte(bytes, CHANGED);
        // timeline must not be NULL here. Else check that tracker is enabled
        serializeDeltaEntry(db, bytes, oClass, entry.getKey(), docEntry);
      } else {
        continue;
      }
    }
  }

  private void serializeDeltaEntry(
      DatabaseSessionInternal db, BytesContainer bytes, SchemaClass oClass, String name,
      EntityEntry entry) {
    final Object value = entry.value;
    assert value != null;
    final PropertyType type = getFieldType(entry);
    if (type == null) {
      throw new SerializationException(
          "Impossible serialize value of type " + value.getClass() + " with the delta serializer");
    }
    writeString(bytes, name);
    writeNullableType(bytes, type);
    serializeDeltaValue(db, bytes, value, type, getLinkedType(oClass, type, name));
  }

  private void serializeDeltaValue(
      DatabaseSessionInternal db, BytesContainer bytes, Object value, PropertyType type,
      PropertyType linkedType) {
    switch (type) {
      case EMBEDDEDLIST:
        serializeDeltaEmbeddedList(db, bytes, (TrackedList) value);
        break;
      case EMBEDDEDSET:
        serializeDeltaEmbeddedSet(db, bytes, (TrackedSet) value);
        break;
      case EMBEDDEDMAP:
        serializeDeltaEmbeddedMap(db, bytes, (TrackedMap) value);
        break;
      case EMBEDDED:
        serializeDelta(db, bytes, ((DBRecord) value).getRecord(db));
        break;
      case LINKLIST:
        serializeDeltaLinkList(db, bytes, (LinkList) value);
        break;
      case LINKSET:
        serializeDeltaLinkSet(db, bytes, (LinkSet) value);
        break;
      case LINKMAP:
        serializeDeltaLinkMap(db, bytes, (LinkMap) value);
        break;
      case LINKBAG:
        serializeDeltaLinkBag(db, bytes, (RidBag) value);
        break;
      default:
        throw new SerializationException("delta not supported for type:" + type);
    }
  }

  protected void serializeDeltaLinkBag(DatabaseSessionInternal db, BytesContainer bytes,
      RidBag value) {
    UUID uuid = null;
    DatabaseSessionInternal instance = DatabaseRecordThreadLocal.instance().getIfDefined();
    if (instance != null) {
      final BTreeCollectionManager bTreeCollectionManager =
          instance.getSbTreeCollectionManager();
      if (bTreeCollectionManager != null) {
        uuid = bTreeCollectionManager.listenForChanges(value);
      }
    }
    if (uuid == null) {
      uuid = new UUID(-1, -1);
    }
    int uuidPos = bytes.alloc(UUIDSerializer.UUID_SIZE);
    UUIDSerializer.INSTANCE.serialize(uuid, bytes.bytes, uuidPos);

    final MultiValueChangeTimeLine<RID, RID> timeline =
        value.getTransactionTimeLine();
    assert timeline != null : "Collection timeline required for serialization of link types";
    VarIntSerializer.write(bytes, timeline.getMultiValueChangeEvents().size());
    for (MultiValueChangeEvent<RID, RID> event :
        timeline.getMultiValueChangeEvents()) {
      switch (event.getChangeType()) {
        case ADD:
          serializeByte(bytes, CREATED);
          writeOptimizedLink(db, bytes, event.getValue());
          break;
        case UPDATE:
          throw new UnsupportedOperationException(
              "update do not happen in sets, it will be like and add");
        case REMOVE:
          serializeByte(bytes, REMOVED);
          writeOptimizedLink(db, bytes, event.getOldValue());
          break;
      }
    }
  }

  private void serializeDeltaLinkSet(
      DatabaseSessionInternal db, BytesContainer bytes,
      TrackedMultiValue<Identifiable, Identifiable> value) {
    MultiValueChangeTimeLine<Identifiable, Identifiable> timeline =
        value.getTransactionTimeLine();
    assert timeline != null : "Collection timeline required for link* types serialization";
    VarIntSerializer.write(bytes, timeline.getMultiValueChangeEvents().size());
    for (MultiValueChangeEvent<Identifiable, Identifiable> event :
        timeline.getMultiValueChangeEvents()) {
      switch (event.getChangeType()) {
        case ADD:
          serializeByte(bytes, CREATED);
          writeOptimizedLink(db, bytes, event.getValue());
          break;
        case UPDATE:
          throw new UnsupportedOperationException(
              "update do not happen in sets, it will be like and add");
        case REMOVE:
          serializeByte(bytes, REMOVED);
          writeOptimizedLink(db, bytes, event.getOldValue());
          break;
      }
    }
  }

  private void serializeDeltaLinkList(DatabaseSessionInternal db, BytesContainer bytes,
      LinkList value) {
    MultiValueChangeTimeLine<Integer, Identifiable> timeline = value.getTransactionTimeLine();
    assert timeline != null : "Collection timeline required for link* types serialization";
    VarIntSerializer.write(bytes, timeline.getMultiValueChangeEvents().size());
    for (MultiValueChangeEvent<Integer, Identifiable> event :
        timeline.getMultiValueChangeEvents()) {
      switch (event.getChangeType()) {
        case ADD:
          serializeByte(bytes, CREATED);
          writeOptimizedLink(db, bytes, event.getValue());
          break;
        case UPDATE:
          serializeByte(bytes, REPLACED);
          VarIntSerializer.write(bytes, event.getKey().longValue());
          writeOptimizedLink(db, bytes, event.getValue());
          break;
        case REMOVE:
          serializeByte(bytes, REMOVED);
          writeOptimizedLink(db, bytes, event.getOldValue());
          break;
      }
    }
  }

  private void serializeDeltaLinkMap(DatabaseSessionInternal db, BytesContainer bytes,
      LinkMap value) {
    MultiValueChangeTimeLine<String, Identifiable> timeline = value.getTransactionTimeLine();
    assert timeline != null : "Collection timeline required for link* types serialization";
    VarIntSerializer.write(bytes, timeline.getMultiValueChangeEvents().size());
    for (MultiValueChangeEvent<String, Identifiable> event :
        timeline.getMultiValueChangeEvents()) {
      switch (event.getChangeType()) {
        case ADD:
          serializeByte(bytes, CREATED);
          writeString(bytes, event.getKey());
          writeOptimizedLink(db, bytes, event.getValue());
          break;
        case UPDATE:
          serializeByte(bytes, REPLACED);
          writeString(bytes, event.getKey());
          writeOptimizedLink(db, bytes, event.getValue());
          break;
        case REMOVE:
          serializeByte(bytes, REMOVED);
          writeString(bytes, event.getKey());
          break;
      }
    }
  }

  private void serializeDeltaEmbeddedMap(DatabaseSessionInternal db, BytesContainer bytes,
      TrackedMap value) {
    MultiValueChangeTimeLine<Object, Object> timeline = value.getTransactionTimeLine();
    if (timeline != null) {
      VarIntSerializer.write(bytes, timeline.getMultiValueChangeEvents().size());
      for (MultiValueChangeEvent<Object, Object> event : timeline.getMultiValueChangeEvents()) {
        switch (event.getChangeType()) {
          case ADD: {
            serializeByte(bytes, CREATED);
            writeString(bytes, event.getKey().toString());
            if (event.getValue() != null) {
              PropertyType type = PropertyType.getTypeByValue(event.getValue());
              writeNullableType(bytes, type);
              serializeValue(db, bytes, event.getValue(), type, null);
            } else {
              writeNullableType(bytes, null);
            }
            break;
          }
          case UPDATE: {
            serializeByte(bytes, REPLACED);
            writeString(bytes, event.getKey().toString());
            if (event.getValue() != null) {
              PropertyType type = PropertyType.getTypeByValue(event.getValue());
              writeNullableType(bytes, type);
              serializeValue(db, bytes, event.getValue(), type, null);
            } else {
              writeNullableType(bytes, null);
            }
            break;
          }
          case REMOVE:
            serializeByte(bytes, REMOVED);
            writeString(bytes, event.getKey().toString());
            break;
        }
      }
    } else {
      VarIntSerializer.write(bytes, 0);
    }
    long count =
        value.values().stream()
            .filter(
                (v) -> {
                  return v instanceof TrackedMultiValue && ((TrackedMultiValue) v).isModified()
                      || v instanceof EntityImpl
                      && ((EntityImpl) v).isEmbedded()
                      && ((EntityImpl) v).isDirty();
                })
            .count();
    VarIntSerializer.write(bytes, count);
    Iterator<Map.Entry<Object, Object>> iterator = value.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<Object, Object> singleEntry = iterator.next();
      Object singleValue = singleEntry.getValue();
      if (singleValue instanceof TrackedMultiValue
          && ((TrackedMultiValue) singleValue).isModified()) {
        serializeByte(bytes, CHANGED);
        writeString(bytes, singleEntry.getKey().toString());
        PropertyType type = PropertyType.getTypeByValue(singleValue);
        writeNullableType(bytes, type);
        serializeDeltaValue(db, bytes, singleValue, type, null);
      } else if (singleValue instanceof EntityImpl
          && ((EntityImpl) singleValue).isEmbedded()
          && ((EntityImpl) singleValue).isDirty()) {
        serializeByte(bytes, CHANGED);
        writeString(bytes, singleEntry.getKey().toString());
        PropertyType type = PropertyType.getTypeByValue(singleValue);
        writeNullableType(bytes, type);
        serializeDeltaValue(db, bytes, singleValue, type, null);
      }
    }
  }

  private void serializeDeltaEmbeddedList(DatabaseSessionInternal db, BytesContainer bytes,
      TrackedList value) {
    MultiValueChangeTimeLine<Integer, Object> timeline = value.getTransactionTimeLine();
    if (timeline != null) {
      VarIntSerializer.write(bytes, timeline.getMultiValueChangeEvents().size());
      for (MultiValueChangeEvent<Integer, Object> event : timeline.getMultiValueChangeEvents()) {
        switch (event.getChangeType()) {
          case ADD: {
            serializeByte(bytes, CREATED);
            if (event.getValue() != null) {
              PropertyType type = PropertyType.getTypeByValue(event.getValue());
              writeNullableType(bytes, type);
              serializeValue(db, bytes, event.getValue(), type, null);
            } else {
              writeNullableType(bytes, null);
            }
            break;
          }
          case UPDATE: {
            serializeByte(bytes, REPLACED);
            VarIntSerializer.write(bytes, event.getKey().longValue());
            if (event.getValue() != null) {
              PropertyType type = PropertyType.getTypeByValue(event.getValue());
              writeNullableType(bytes, type);
              serializeValue(db, bytes, event.getValue(), type, null);
            } else {
              writeNullableType(bytes, null);
            }
            break;
          }
          case REMOVE: {
            serializeByte(bytes, REMOVED);
            VarIntSerializer.write(bytes, event.getKey().longValue());
            break;
          }
        }
      }
    } else {
      VarIntSerializer.write(bytes, 0);
    }
    long count =
        value.stream()
            .filter(
                (v) -> {
                  return v instanceof TrackedMultiValue && ((TrackedMultiValue) v).isModified()
                      || v instanceof EntityImpl
                      && ((EntityImpl) v).isEmbedded()
                      && ((EntityImpl) v).isDirty();
                })
            .count();
    VarIntSerializer.write(bytes, count);
    for (int i = 0; i < value.size(); i++) {
      Object singleValue = value.get(i);
      if (singleValue instanceof TrackedMultiValue
          && ((TrackedMultiValue) singleValue).isModified()) {
        serializeByte(bytes, CHANGED);
        VarIntSerializer.write(bytes, i);
        PropertyType type = PropertyType.getTypeByValue(singleValue);
        writeNullableType(bytes, type);
        serializeDeltaValue(db, bytes, singleValue, type, null);
      } else if (singleValue instanceof EntityImpl
          && ((EntityImpl) singleValue).isEmbedded()
          && ((EntityImpl) singleValue).isDirty()) {
        serializeByte(bytes, CHANGED);
        VarIntSerializer.write(bytes, i);
        PropertyType type = PropertyType.getTypeByValue(singleValue);
        writeNullableType(bytes, type);
        serializeDeltaValue(db, bytes, singleValue, type, null);
      }
    }
  }

  private void serializeDeltaEmbeddedSet(DatabaseSessionInternal db, BytesContainer bytes,
      TrackedSet value) {
    MultiValueChangeTimeLine<Object, Object> timeline = value.getTransactionTimeLine();
    if (timeline != null) {
      VarIntSerializer.write(bytes, timeline.getMultiValueChangeEvents().size());
      for (MultiValueChangeEvent<Object, Object> event : timeline.getMultiValueChangeEvents()) {
        switch (event.getChangeType()) {
          case ADD: {
            serializeByte(bytes, CREATED);
            if (event.getValue() != null) {
              PropertyType type = PropertyType.getTypeByValue(event.getValue());
              writeNullableType(bytes, type);
              serializeValue(db, bytes, event.getValue(), type, null);
            } else {
              writeNullableType(bytes, null);
            }
            break;
          }
          case UPDATE:
            throw new UnsupportedOperationException(
                "update do not happen in sets, it will be like and add");
          case REMOVE: {
            serializeByte(bytes, REMOVED);
            if (event.getOldValue() != null) {
              PropertyType type = PropertyType.getTypeByValue(event.getOldValue());
              writeNullableType(bytes, type);
              serializeValue(db, bytes, event.getOldValue(), type, null);
            } else {
              writeNullableType(bytes, null);
            }
            break;
          }
        }
      }
    } else {
      VarIntSerializer.write(bytes, 0);
    }
    long count =
        value.stream()
            .filter(
                (v) -> {
                  return v instanceof TrackedMultiValue && ((TrackedMultiValue) v).isModified()
                      || v instanceof EntityImpl
                      && ((EntityImpl) v).isEmbedded()
                      && ((EntityImpl) v).isDirty();
                })
            .count();
    VarIntSerializer.write(bytes, count);
    int i = 0;
    for (Object singleValue : value) {
      if (singleValue instanceof TrackedMultiValue
          && ((TrackedMultiValue) singleValue).isModified()) {
        serializeByte(bytes, CHANGED);
        VarIntSerializer.write(bytes, i);
        PropertyType type = PropertyType.getTypeByValue(singleValue);
        writeNullableType(bytes, type);
        serializeDeltaValue(db, bytes, singleValue, type, null);
      } else if (singleValue instanceof EntityImpl
          && ((EntityImpl) singleValue).isEmbedded()
          && ((EntityImpl) singleValue).isDirty()) {
        serializeByte(bytes, CHANGED);
        VarIntSerializer.write(bytes, i);
        PropertyType type = PropertyType.getTypeByValue(singleValue);
        writeNullableType(bytes, type);
        serializeDeltaValue(db, bytes, singleValue, type, null);
      }
      i++;
    }
  }

  protected PropertyType getFieldType(final EntityEntry entry) {
    PropertyType type = entry.type;
    if (type == null) {
      final SchemaProperty prop = entry.property;
      if (prop != null) {
        type = prop.getType();
      }
    }
    if (type == null || PropertyType.ANY == type) {
      type = PropertyType.getTypeByValue(entry.value);
    }
    return type;
  }

  private void serializeFullEntry(
      DatabaseSessionInternal db, BytesContainer bytes, SchemaClass oClass, String name,
      EntityEntry entry) {
    final Object value = entry.value;
    if (value != null) {
      final PropertyType type = getFieldType(entry);
      if (type == null) {
        throw new SerializationException(
            "Impossible serialize value of type "
                + value.getClass()
                + " with the delta serializer");
      }
      writeString(bytes, name);
      writeNullableType(bytes, type);
      serializeValue(db, bytes, value, type, getLinkedType(oClass, type, name));
    } else {
      writeString(bytes, name);
      writeNullableType(bytes, null);
    }
  }

  protected static byte deserializeByte(BytesContainer bytes) {
    int pos = bytes.offset;
    bytes.skip(1);
    return bytes.bytes[pos];
  }

  protected void serializeByte(BytesContainer bytes, byte value) {
    int pointer = bytes.alloc(1);
    bytes.bytes[pointer] = value;
  }

  public void serializeValue(
      DatabaseSessionInternal db, final BytesContainer bytes, Object value, final PropertyType type,
      final PropertyType linkedType) {
    int pointer = 0;
    switch (type) {
      case INTEGER:
      case LONG:
      case SHORT:
        VarIntSerializer.write(bytes, ((Number) value).longValue());
        break;
      case STRING:
        writeString(bytes, value.toString());
        break;
      case DOUBLE:
        long dg = Double.doubleToLongBits((Double) value);
        pointer = bytes.alloc(LongSerializer.LONG_SIZE);
        LongSerializer.INSTANCE.serializeLiteral(dg, bytes.bytes, pointer);
        break;
      case FLOAT:
        int fg = Float.floatToIntBits((Float) value);
        pointer = bytes.alloc(IntegerSerializer.INT_SIZE);
        IntegerSerializer.INSTANCE.serializeLiteral(fg, bytes.bytes, pointer);
        break;
      case BYTE:
        pointer = bytes.alloc(1);
        bytes.bytes[pointer] = (Byte) value;
        break;
      case BOOLEAN:
        pointer = bytes.alloc(1);
        bytes.bytes[pointer] = ((Boolean) value) ? (byte) 1 : (byte) 0;
        break;
      case DATETIME:
        if (value instanceof Long) {
          VarIntSerializer.write(bytes, (Long) value);
        } else {
          VarIntSerializer.write(bytes, ((Date) value).getTime());
        }
        break;
      case DATE:
        long dateValue;
        if (value instanceof Long) {
          dateValue = (Long) value;
        } else {
          dateValue = ((Date) value).getTime();
        }
        dateValue =
            convertDayToTimezone(
                DateHelper.getDatabaseTimeZone(), TimeZone.getTimeZone("GMT"), dateValue);
        VarIntSerializer.write(bytes, dateValue / MILLISEC_PER_DAY);
        break;
      case EMBEDDED:
        if (value instanceof EntitySerializable) {
          EntityImpl cur = ((EntitySerializable) value).toEntity(db);
          cur.field(EntitySerializable.CLASS_NAME, value.getClass().getName());
          serialize(db, cur, bytes);
        } else {
          serialize(db, ((DBRecord) value).getRecord(db), bytes);
        }
        break;
      case EMBEDDEDSET:
      case EMBEDDEDLIST:
        if (value.getClass().isArray()) {
          writeEmbeddedCollection(db, bytes, Arrays.asList(MultiValue.array(value)), linkedType);
        } else {
          writeEmbeddedCollection(db, bytes, (Collection<?>) value, linkedType);
        }
        break;
      case DECIMAL:
        BigDecimal decimalValue = (BigDecimal) value;
        pointer = bytes.alloc(DecimalSerializer.INSTANCE.getObjectSize(decimalValue));
        DecimalSerializer.INSTANCE.serialize(decimalValue, bytes.bytes, pointer);
        break;
      case BINARY:
        pointer = writeBinary(bytes, (byte[]) (value));
        break;
      case LINKSET:
      case LINKLIST:
        Collection<Identifiable> ridCollection = (Collection<Identifiable>) value;
        writeLinkCollection(db, bytes, ridCollection);
        break;
      case LINK:
        if (!(value instanceof Identifiable)) {
          throw new ValidationException("Value '" + value + "' is not a Identifiable");
        }

        writeOptimizedLink(db, bytes, (Identifiable) value);
        break;
      case LINKMAP:
        writeLinkMap(db, bytes, (Map<Object, Identifiable>) value);
        break;
      case EMBEDDEDMAP:
        writeEmbeddedMap(db, bytes, (Map<Object, Object>) value);
        break;
      case LINKBAG:
        writeRidBag(db, bytes, (RidBag) value);
        break;
      case CUSTOM:
        if (!(value instanceof SerializableStream)) {
          value = new SerializableWrapper((Serializable) value);
        }
        writeString(bytes, value.getClass().getName());
        writeBinary(bytes, ((SerializableStream) value).toStream());
        break;
      case TRANSIENT:
        break;
      case ANY:
        break;
    }
  }

  private static int writeLinkCollection(
      DatabaseSessionInternal db, final BytesContainer bytes,
      final Collection<Identifiable> value) {
    final int pos = VarIntSerializer.write(bytes, value.size());

    for (Identifiable itemValue : value) {
      // TODO: handle the null links
      if (itemValue == null) {
        writeNullLink(bytes);
      } else {
        writeOptimizedLink(db, bytes, itemValue);
      }
    }

    return pos;
  }

  private static int writeLinkMap(DatabaseSessionInternal db, final BytesContainer bytes,
      final Map<Object, Identifiable> map) {
    final int fullPos = VarIntSerializer.write(bytes, map.size());
    for (Map.Entry<Object, Identifiable> entry : map.entrySet()) {
      // TODO:check skip of complex types
      // FIXME: changed to support only string key on map
      final PropertyType type = PropertyType.STRING;
      writeOType(bytes, bytes.alloc(1), type);
      writeString(bytes, entry.getKey().toString());
      if (entry.getValue() == null) {
        writeNullLink(bytes);
      } else {
        writeOptimizedLink(db, bytes, entry.getValue());
      }
    }
    return fullPos;
  }

  private void writeEmbeddedCollection(
      DatabaseSessionInternal db, final BytesContainer bytes, final Collection<?> value,
      final PropertyType linkedType) {
    VarIntSerializer.write(bytes, value.size());
    // TODO manage embedded type from schema and auto-determined.
    for (Object itemValue : value) {
      // TODO:manage in a better way null entry
      if (itemValue == null) {
        writeNullableType(bytes, null);
        continue;
      }
      PropertyType type;
      if (linkedType == null) {
        type = getTypeFromValueEmbedded(itemValue);
      } else {
        type = linkedType;
      }
      if (type != null) {
        writeNullableType(bytes, type);
        serializeValue(db, bytes, itemValue, type, null);
      } else {
        throw new SerializationException(
            "Impossible serialize value of type "
                + value.getClass()
                + " with the EntityImpl binary serializer");
      }
    }
  }

  private void writeEmbeddedMap(DatabaseSessionInternal db, BytesContainer bytes,
      Map<Object, Object> map) {
    VarIntSerializer.write(bytes, map.size());
    for (Map.Entry<Object, Object> entry : map.entrySet()) {
      writeString(bytes, entry.getKey().toString());
      final Object value = entry.getValue();
      if (value != null) {
        final PropertyType type = getTypeFromValueEmbedded(value);
        if (type == null) {
          throw new SerializationException(
              "Impossible serialize value of type "
                  + value.getClass()
                  + " with the Result binary serializer");
        }
        writeNullableType(bytes, type);
        serializeValue(db, bytes, value, type, null);
      } else {
        writeNullableType(bytes, null);
      }
    }
  }

  public Object deserializeValue(DatabaseSessionInternal db, BytesContainer bytes,
      PropertyType type,
      RecordElement owner) {
    Object value = null;
    switch (type) {
      case INTEGER:
        value = VarIntSerializer.readAsInteger(bytes);
        break;
      case LONG:
        value = VarIntSerializer.readAsLong(bytes);
        break;
      case SHORT:
        value = VarIntSerializer.readAsShort(bytes);
        break;
      case STRING:
        value = readString(bytes);
        break;
      case DOUBLE:
        value = Double.longBitsToDouble(readLong(bytes));
        break;
      case FLOAT:
        value = Float.intBitsToFloat(readInteger(bytes));
        break;
      case BYTE:
        value = readByte(bytes);
        break;
      case BOOLEAN:
        value = readByte(bytes) == 1;
        break;
      case DATETIME:
        value = new Date(VarIntSerializer.readAsLong(bytes));
        break;
      case DATE:
        long savedTime = VarIntSerializer.readAsLong(bytes) * MILLISEC_PER_DAY;
        savedTime =
            convertDayToTimezone(
                TimeZone.getTimeZone("GMT"), DateHelper.getDatabaseTimeZone(), savedTime);
        value = new Date(savedTime);
        break;
      case EMBEDDED:
        value = new EmbeddedEntityImpl(db);
        deserialize(db, (EntityImpl) value, bytes);
        if (((EntityImpl) value).containsField(EntitySerializable.CLASS_NAME)) {
          String className = ((EntityImpl) value).field(EntitySerializable.CLASS_NAME);
          try {
            Class<?> clazz = Class.forName(className);
            EntitySerializable newValue = (EntitySerializable) clazz.newInstance();
            newValue.fromDocument((EntityImpl) value);
            value = newValue;
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        } else {
          EntityInternalUtils.addOwner((EntityImpl) value, owner);
        }

        break;
      case EMBEDDEDSET:
        value = readEmbeddedSet(db, bytes, owner);
        break;
      case EMBEDDEDLIST:
        value = readEmbeddedList(db, bytes, owner);
        break;
      case LINKSET:
        value = readLinkSet(db, bytes, owner);
        break;
      case LINKLIST:
        value = readLinkList(db, bytes, owner);
        break;
      case BINARY:
        value = readBinary(bytes);
        break;
      case LINK:
        value = readOptimizedLink(db, bytes);
        break;
      case LINKMAP:
        value = readLinkMap(db, bytes, owner);
        break;
      case EMBEDDEDMAP:
        value = readEmbeddedMap(db, bytes, owner);
        break;
      case DECIMAL:
        value = DecimalSerializer.INSTANCE.deserialize(bytes.bytes, bytes.offset);
        bytes.skip(DecimalSerializer.INSTANCE.getObjectSize(bytes.bytes, bytes.offset));
        break;
      case LINKBAG:
        RidBag bag = readRidBag(db, bytes);
        bag.setOwner(owner);
        value = bag;
        break;
      case TRANSIENT:
        break;
      case CUSTOM:
        try {
          String className = readString(bytes);
          Class<?> clazz = Class.forName(className);
          SerializableStream stream = (SerializableStream) clazz.newInstance();
          stream.fromStream(readBinary(bytes));
          if (stream instanceof SerializableWrapper) {
            value = ((SerializableWrapper) stream).getSerializable();
          } else {
            value = stream;
          }
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
        break;
      case ANY:
        break;
    }
    return value;
  }

  private Collection<?> readEmbeddedList(DatabaseSessionInternal session,
      final BytesContainer bytes, final RecordElement owner) {
    TrackedList<Object> found = new TrackedList<>(owner);
    final int items = VarIntSerializer.readAsInteger(bytes);
    for (int i = 0; i < items; i++) {
      PropertyType itemType = readNullableType(bytes);
      if (itemType == null) {
        found.addInternal(null);
      } else {
        found.addInternal(deserializeValue(session, bytes, itemType, found));
      }
    }
    return found;
  }

  private Collection<?> readEmbeddedSet(DatabaseSessionInternal session,
      final BytesContainer bytes, final RecordElement owner) {
    TrackedSet<Object> found = new TrackedSet<>(owner);
    final int items = VarIntSerializer.readAsInteger(bytes);
    for (int i = 0; i < items; i++) {
      PropertyType itemType = readNullableType(bytes);
      if (itemType == null) {
        found.addInternal(null);
      } else {
        found.addInternal(deserializeValue(session, bytes, itemType, found));
      }
    }
    return found;
  }

  private static Collection<Identifiable> readLinkList(DatabaseSessionInternal db,
      BytesContainer bytes,
      RecordElement owner) {
    LinkList found = new LinkList(owner);
    final int items = VarIntSerializer.readAsInteger(bytes);
    for (int i = 0; i < items; i++) {
      Identifiable id = readOptimizedLink(db, bytes);
      if (id.equals(NULL_RECORD_ID)) {
        found.addInternal(null);
      } else {
        found.addInternal(id);
      }
    }
    return found;
  }

  private static Collection<Identifiable> readLinkSet(DatabaseSessionInternal db,
      BytesContainer bytes,
      RecordElement owner) {
    LinkSet found = new LinkSet(owner);
    final int items = VarIntSerializer.readAsInteger(bytes);
    for (int i = 0; i < items; i++) {
      Identifiable id = readOptimizedLink(db, bytes);
      if (id.equals(NULL_RECORD_ID)) {
        found.addInternal(null);
      } else {
        found.addInternal(id);
      }
    }
    return found;
  }

  private Map<String, Identifiable> readLinkMap(
      DatabaseSessionInternal db, final BytesContainer bytes, final RecordElement owner) {
    int size = VarIntSerializer.readAsInteger(bytes);
    LinkMap result = new LinkMap(owner);
    while ((size--) > 0) {
      PropertyType keyType = readOType(bytes, false);
      Object key = deserializeValue(db, bytes, keyType, result);
      Identifiable value = readOptimizedLink(db, bytes);
      if (value.equals(NULL_RECORD_ID)) {
        result.putInternal(key.toString(), null);
      } else {
        result.putInternal(key.toString(), value);
      }
    }

    return result;
  }

  private Object readEmbeddedMap(DatabaseSessionInternal session, final BytesContainer bytes,
      final RecordElement owner) {
    int size = VarIntSerializer.readAsInteger(bytes);
    final TrackedMap result = new TrackedMap<Object>(owner);
    while ((size--) > 0) {
      String key = readString(bytes);
      PropertyType valType = readNullableType(bytes);
      Object value = null;
      if (valType != null) {
        value = deserializeValue(session, bytes, valType, result);
      }
      result.putInternal(key, value);
    }
    return result;
  }

  private RidBag readRidBag(DatabaseSessionInternal db, BytesContainer bytes) {
    UUID uuid = UUIDSerializer.INSTANCE.deserialize(bytes.bytes, bytes.offset);
    bytes.skip(UUIDSerializer.UUID_SIZE);
    if (uuid.getMostSignificantBits() == -1 && uuid.getLeastSignificantBits() == -1) {
      uuid = null;
    }
    byte b = bytes.bytes[bytes.offset];
    bytes.skip(1);
    if (b == 1) {
      RidBag bag = new RidBag(db, uuid);
      int size = VarIntSerializer.readAsInteger(bytes);
      for (int i = 0; i < size; i++) {
        var id = readOptimizedLink(db, bytes);
        if (id.equals(NULL_RECORD_ID)) {
          bag.add(null);
        } else {
          bag.add(id);
        }
      }
      return bag;
    } else {
      long fileId = VarIntSerializer.readAsLong(bytes);
      long pageIndex = VarIntSerializer.readAsLong(bytes);
      int pageOffset = VarIntSerializer.readAsInteger(bytes);
      int bagSize = VarIntSerializer.readAsInteger(bytes);

      Map<RID, Change> changes = new HashMap<>();
      int size = VarIntSerializer.readAsInteger(bytes);
      while (size-- > 0) {
        RID link = readOptimizedLink(db, bytes);
        byte type = bytes.bytes[bytes.offset];
        bytes.skip(1);
        int change = VarIntSerializer.readAsInteger(bytes);
        changes.put(link, ChangeSerializationHelper.createChangeInstance(type, change));
      }

      BonsaiCollectionPointer pointer = null;
      if (fileId != -1) {
        pointer =
            new BonsaiCollectionPointer(fileId, new RidBagBucketPointer(pageIndex, pageOffset));
      }
      return new RidBag(db, pointer, changes, uuid);
    }
  }

  private static void writeRidBag(DatabaseSessionInternal db, BytesContainer bytes, RidBag bag) {
    final BTreeCollectionManager bTreeCollectionManager =
        DatabaseRecordThreadLocal.instance().get().getSbTreeCollectionManager();
    UUID uuid = null;
    if (bTreeCollectionManager != null) {
      uuid = bTreeCollectionManager.listenForChanges(bag);
    }
    if (uuid == null) {
      uuid = new UUID(-1, -1);
    }
    int uuidPos = bytes.alloc(UUIDSerializer.UUID_SIZE);
    UUIDSerializer.INSTANCE.serialize(uuid, bytes.bytes, uuidPos);
    if (bag.isToSerializeEmbedded()) {
      int pos = bytes.alloc(1);
      bytes.bytes[pos] = 1;
      VarIntSerializer.write(bytes, bag.size());
      for (Identifiable itemValue : bag) {
        if (itemValue == null) {
          writeNullLink(bytes);
        } else {
          writeOptimizedLink(db, bytes, itemValue);
        }
      }
    } else {
      int pos = bytes.alloc(1);
      bytes.bytes[pos] = 2;
      BonsaiCollectionPointer pointer = bag.getPointer();
      if (pointer == null) {
        pointer = BonsaiCollectionPointer.INVALID;
      }
      VarIntSerializer.write(bytes, pointer.getFileId());
      VarIntSerializer.write(bytes, pointer.getRootPointer().getPageIndex());
      VarIntSerializer.write(bytes, pointer.getRootPointer().getPageOffset());
      VarIntSerializer.write(bytes, bag.size());
      NavigableMap<RID, Change> changes = bag.getChanges();
      if (changes != null) {
        VarIntSerializer.write(bytes, changes.size());
        for (Map.Entry<RID, Change> change : changes.entrySet()) {
          writeOptimizedLink(db, bytes, change.getKey());
          int posAll = bytes.alloc(1);
          bytes.bytes[posAll] = change.getValue().getType();
          VarIntSerializer.write(bytes, change.getValue().getValue());
        }
      } else {
        VarIntSerializer.write(bytes, 0);
      }
    }
  }

  public static void writeNullableType(BytesContainer bytes, PropertyType type) {
    int pos = bytes.alloc(1);
    if (type == null) {
      bytes.bytes[pos] = -1;
    } else {
      bytes.bytes[pos] = (byte) type.getId();
    }
  }

  public static PropertyType readNullableType(BytesContainer bytes) {
    byte typeId = bytes.bytes[bytes.offset++];
    if (typeId == -1) {
      return null;
    }
    return PropertyType.getById(typeId);
  }

  public static RecordId readOptimizedLink(DatabaseSessionInternal db, final BytesContainer bytes) {
    int clusterId = VarIntSerializer.readAsInteger(bytes);
    long clusterPos = VarIntSerializer.readAsLong(bytes);

    if (clusterId == -2 && clusterPos == -2) {
      return null;
    } else {
      var rid = new RecordId(clusterId, clusterPos);

      if (rid.isTemporary()) {
        try {
          // rid will be changed during commit we need to keep track original rid
          var record = rid.getRecord(db);

          rid = (RecordId) record.getIdentity();
          if (rid == null) {
            rid = new RecordId(clusterId, clusterPos);
          }

        } catch (RecordNotFoundException rnf) {
          return rid;
        }
      }

      return rid;
    }
  }

  public static void writeOptimizedLink(DatabaseSessionInternal db, final BytesContainer bytes,
      Identifiable link) {
    if (link == null) {
      VarIntSerializer.write(bytes, -2);
      VarIntSerializer.write(bytes, -2);
    } else {
      if (!link.getIdentity().isPersistent()) {
        try {
          link = link.getRecord(db);
        } catch (RecordNotFoundException ignored) {
          // IGNORE IT WILL FAIL THE ASSERT IN CASE
        }
      }

      VarIntSerializer.write(bytes, link.getIdentity().getClusterId());
      VarIntSerializer.write(bytes, link.getIdentity().getClusterPosition());
    }
  }
}
