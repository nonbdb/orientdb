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
package com.orientechnologies.orient.core.record.impl;

import static com.orientechnologies.orient.core.config.YTGlobalConfiguration.DB_CUSTOM_SUPPORT;

import com.orientechnologies.common.collection.OMultiValue;
import com.orientechnologies.common.exception.YTException;
import com.orientechnologies.common.io.OIOUtils;
import com.orientechnologies.common.log.OLogManager;
import com.orientechnologies.common.util.OCommonConst;
import com.orientechnologies.orient.core.command.OBasicCommandContext;
import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.orientechnologies.orient.core.db.YTDatabaseSession;
import com.orientechnologies.orient.core.db.YTDatabaseSessionInternal;
import com.orientechnologies.orient.core.db.document.YTDatabaseSessionAbstract;
import com.orientechnologies.orient.core.db.record.YTIdentifiable;
import com.orientechnologies.orient.core.db.record.OList;
import com.orientechnologies.orient.core.db.record.OMap;
import com.orientechnologies.orient.core.db.record.OMultiValueChangeEvent;
import com.orientechnologies.orient.core.db.record.OMultiValueChangeTimeLine;
import com.orientechnologies.orient.core.db.record.ORecordElement;
import com.orientechnologies.orient.core.db.record.OSet;
import com.orientechnologies.orient.core.db.record.OTrackedList;
import com.orientechnologies.orient.core.db.record.OTrackedMap;
import com.orientechnologies.orient.core.db.record.OTrackedMultiValue;
import com.orientechnologies.orient.core.db.record.OTrackedSet;
import com.orientechnologies.orient.core.db.record.ridbag.ORidBag;
import com.orientechnologies.orient.core.exception.YTConfigurationException;
import com.orientechnologies.orient.core.exception.YTDatabaseException;
import com.orientechnologies.orient.core.exception.YTQueryParsingException;
import com.orientechnologies.orient.core.exception.YTRecordNotFoundException;
import com.orientechnologies.orient.core.exception.YTSchemaException;
import com.orientechnologies.orient.core.exception.YTSecurityException;
import com.orientechnologies.orient.core.exception.YTValidationException;
import com.orientechnologies.orient.core.id.ChangeableRecordId;
import com.orientechnologies.orient.core.id.YTRID;
import com.orientechnologies.orient.core.id.YTRecordId;
import com.orientechnologies.orient.core.iterator.OEmptyMapEntryIterator;
import com.orientechnologies.orient.core.metadata.OMetadataInternal;
import com.orientechnologies.orient.core.metadata.schema.YTClass;
import com.orientechnologies.orient.core.metadata.schema.OGlobalProperty;
import com.orientechnologies.orient.core.metadata.schema.YTImmutableClass;
import com.orientechnologies.orient.core.metadata.schema.YTImmutableProperty;
import com.orientechnologies.orient.core.metadata.schema.YTImmutableSchema;
import com.orientechnologies.orient.core.metadata.schema.YTProperty;
import com.orientechnologies.orient.core.metadata.schema.YTSchema;
import com.orientechnologies.orient.core.metadata.schema.OSchemaShared;
import com.orientechnologies.orient.core.metadata.schema.YTType;
import com.orientechnologies.orient.core.metadata.security.OIdentity;
import com.orientechnologies.orient.core.metadata.security.OPropertyAccess;
import com.orientechnologies.orient.core.metadata.security.OPropertyEncryption;
import com.orientechnologies.orient.core.metadata.security.OSecurityInternal;
import com.orientechnologies.orient.core.record.YTEdge;
import com.orientechnologies.orient.core.record.YTEntity;
import com.orientechnologies.orient.core.record.YTRecord;
import com.orientechnologies.orient.core.record.YTRecordAbstract;
import com.orientechnologies.orient.core.record.ORecordInternal;
import com.orientechnologies.orient.core.record.YTRecordSchemaAware;
import com.orientechnologies.orient.core.record.ORecordVersionHelper;
import com.orientechnologies.orient.core.record.YTVertex;
import com.orientechnologies.orient.core.serialization.serializer.OStringSerializerHelper;
import com.orientechnologies.orient.core.sql.OSQLHelper;
import com.orientechnologies.orient.core.sql.executor.OResult;
import com.orientechnologies.orient.core.sql.filter.OSQLPredicate;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Document representation to handle values dynamically. Can be used in schema-less, schema-mixed
 * and schema-full modes. Fields can be added at run-time. Instances can be reused across calls by
 * using the reset() before to re-use.
 */
@SuppressWarnings({"unchecked"})
public class YTDocument extends YTRecordAbstract
    implements Iterable<Entry<String, Object>>,
    YTRecordSchemaAware,
    YTEntityInternal {

  public static final byte RECORD_TYPE = 'd';
  private static final String[] EMPTY_STRINGS = new String[]{};
  private int fieldSize;

  protected Map<String, ODocumentEntry> fields;

  private boolean trackingChanges = true;
  protected boolean ordered = true;
  private boolean lazyLoad = true;
  private boolean allowChainedAccess = true;
  protected transient WeakReference<ORecordElement> owner = null;

  protected YTImmutableSchema schema;
  private String className;
  private YTImmutableClass immutableClazz;
  private int immutableSchemaVersion = 1;
  OPropertyAccess propertyAccess;
  OPropertyEncryption propertyEncryption;

  /**
   * Internal constructor used on unmarshalling.
   */
  public YTDocument() {
    setup(ODatabaseRecordThreadLocal.instance().getIfDefined());
  }

  /**
   * Internal constructor used on unmarshalling.
   */
  public YTDocument(YTDatabaseSessionInternal database) {
    assert database == null || database.assertIfNotActive();
    setup(database);
  }

  public YTDocument(YTDatabaseSession database, YTRID rid) {
    setup((YTDatabaseSessionInternal) database);
    this.recordId = (YTRecordId) rid.copy();
  }

  /**
   * Creates a new instance by the raw stream usually read from the database. New instances are not
   * persistent until {@link #save()} is called.
   *
   * @param iSource Raw stream
   */
  @Deprecated
  public YTDocument(final byte[] iSource) {
    source = iSource;
    setup(ODatabaseRecordThreadLocal.instance().getIfDefined());
  }

  /**
   * Creates a new instance by the raw stream usually read from the database. New instances are not
   * persistent until {@link #save()} is called.
   *
   * @param iSource Raw stream as InputStream
   */
  public YTDocument(final InputStream iSource) throws IOException {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    OIOUtils.copyStream(iSource, out);
    source = out.toByteArray();
    setup(ODatabaseRecordThreadLocal.instance().getIfDefined());
  }

  /**
   * Creates a new instance in memory linked by the Record Id to the persistent one. New instances
   * are not persistent until {@link #save()} is called.
   *
   * @param iRID Record Id
   */
  public YTDocument(final YTRID iRID) {
    setup(ODatabaseRecordThreadLocal.instance().getIfDefined());
    recordId = (YTRecordId) iRID.copy();
    status = STATUS.NOT_LOADED;
    dirty = false;
    contentChanged = false;
  }

  /**
   * Creates a new instance in memory of the specified class, linked by the Record Id to the
   * persistent one. New instances are not persistent until {@link #save()} is called.
   *
   * @param iClassName Class name
   * @param iRID       Record Id
   */
  public YTDocument(final String iClassName, final YTRID iRID) {
    this(iClassName);
    recordId = (YTRecordId) iRID.copy();

    final YTDatabaseSessionInternal database = getSession();
    if (recordId.getClusterId() > -1) {
      final YTSchema schema = database.getMetadata().getImmutableSchemaSnapshot();
      final YTClass cls = schema.getClassByClusterId(recordId.getClusterId());
      if (cls != null && !cls.getName().equals(iClassName)) {
        throw new IllegalArgumentException(
            "Cluster id does not correspond class name should be "
                + iClassName
                + " but found "
                + cls.getName());
      }
    }

    dirty = false;
    contentChanged = false;
    status = STATUS.NOT_LOADED;
  }

  /**
   * Creates a new instance in memory of the specified class. New instances are not persistent until
   * {@link #save()} is called.
   *
   * @param iClassName Class name
   */
  public YTDocument(final String iClassName) {
    setup(ODatabaseRecordThreadLocal.instance().getIfDefined());
    setClassName(iClassName);
  }

  public YTDocument(final String iClassName, YTDatabaseSessionInternal session) {
    assert session == null || session.assertIfNotActive();
    setup(session);
    setClassName(iClassName);
  }

  /**
   * Creates a new instance in memory of the specified class. New instances are not persistent until
   * {@link #save()} is called.
   *
   * @param session    the session the instance will be attached to
   * @param iClassName Class name
   */
  public YTDocument(YTDatabaseSessionInternal session, final String iClassName) {
    assert session == null || session.assertIfNotActive();
    setup(session);
    setClassName(iClassName);
  }

  /**
   * Creates a new instance in memory of the specified schema class. New instances are not
   * persistent until {@link #save()} is called. The database reference is taken from the thread
   * local.
   *
   * @param iClass YTClass instance
   */
  public YTDocument(final YTClass iClass) {
    this(iClass != null ? iClass.getName() : null);
  }

  /**
   * Fills a document passing the field array in form of pairs of field name and value.
   *
   * @param iFields Array of field pairs
   */
  public YTDocument(final Object[] iFields) {
    this(YTEntity.DEFAULT_CLASS_NAME);

    setup(ODatabaseRecordThreadLocal.instance().getIfDefined());
    if (iFields != null && iFields.length > 0) {
      for (int i = 0; i < iFields.length; i += 2) {
        field(iFields[i].toString(), iFields[i + 1]);
      }
    }
  }

  /**
   * Fills a document passing a map of key/values where the key is the field name and the value the
   * field's value.
   *
   * @param iFieldMap Map of Object/Object
   */
  public YTDocument(final Map<?, Object> iFieldMap) {
    setup(ODatabaseRecordThreadLocal.instance().getIfDefined());
    if (iFieldMap != null && !iFieldMap.isEmpty()) {
      for (Entry<?, Object> entry : iFieldMap.entrySet()) {
        field(entry.getKey().toString(), entry.getValue());
      }
    }
  }

  /**
   * Fills a document passing the field names/values pair, where the first pair is mandatory.
   */
  public YTDocument(final String iFieldName, final Object iFieldValue, final Object... iFields) {
    this(iFields);
    field(iFieldName, iFieldValue);
  }

  @Override
  public Optional<YTVertex> asVertex() {
    if (this instanceof YTVertex) {
      return Optional.of((YTVertex) this);
    }
    YTClass type = this.getImmutableSchemaClass();

    if (type == null) {
      return Optional.empty();
    }
    if (type.isVertexType()) {
      return Optional.of(new YTVertexDelegate(this));
    }
    return Optional.empty();
  }

  @Override
  public @Nullable YTVertex toVertex() {
    if (this instanceof YTVertex vertex) {
      return vertex;
    }

    YTClass type = this.getImmutableSchemaClass();
    if (type == null) {
      return null;
    }
    if (type.isVertexType()) {
      return new YTVertexDelegate(this);
    }
    return null;
  }

  @Override
  public Optional<YTEdge> asEdge() {
    if (this instanceof YTEdge) {
      return Optional.of((YTEdge) this);
    }
    YTClass type = this.getImmutableSchemaClass();
    if (type == null) {
      return Optional.empty();
    }
    if (type.isEdgeType()) {
      return Optional.of(new YTEdgeDelegate(this));
    }
    return Optional.empty();
  }

  @Override
  public @Nullable YTEdge toEdge() {
    if (this instanceof YTEdge edge) {
      return edge;
    }
    YTClass type = this.getImmutableSchemaClass();
    if (type == null) {
      return null;
    }
    if (type.isEdgeType()) {
      return new YTEdgeDelegate(this);
    }

    return null;
  }

  @Override
  public boolean isVertex() {
    if (this instanceof YTVertex) {
      return true;
    }

    YTClass type = this.getImmutableSchemaClass();
    if (type == null) {
      return false;
    }

    return type.isVertexType();
  }

  @Override
  public boolean isEdge() {
    if (this instanceof YTEdge) {
      return true;
    }

    YTClass type = this.getImmutableSchemaClass();
    if (type == null) {
      return false;
    }

    return type.isEdgeType();
  }

  @Override
  public Optional<YTClass> getSchemaType() {
    checkForBinding();
    return Optional.ofNullable(getImmutableSchemaClass());
  }

  Set<String> calculatePropertyNames() {
    checkForBinding();

    var session = getSessionIfDefined();
    if (status == ORecordElement.STATUS.LOADED
        && source != null
        && session != null
        && !session.isClosed()) {
      assert session.assertIfNotActive();
      // DESERIALIZE FIELD NAMES ONLY (SUPPORTED ONLY BY BINARY SERIALIZER)
      final String[] fieldNames = recordFormat.getFieldNames(session, this, source);
      if (fieldNames != null) {
        Set<String> fields = new LinkedHashSet<>();
        if (propertyAccess != null && propertyAccess.hasFilters()) {
          for (String fieldName : fieldNames) {
            if (propertyAccess.isReadable(fieldName)) {
              fields.add(fieldName);
            }
          }
        } else {

          Collections.addAll(fields, fieldNames);
        }
        return fields;
      }
    }

    checkForFields();

    if (fields == null || fields.isEmpty()) {
      return Collections.emptySet();
    }

    Set<String> fields = new LinkedHashSet<>();
    if (propertyAccess != null && propertyAccess.hasFilters()) {
      for (Map.Entry<String, ODocumentEntry> entry : this.fields.entrySet()) {
        if (entry.getValue().exists() && propertyAccess.isReadable(entry.getKey())) {
          fields.add(entry.getKey());
        }
      }
    } else {
      for (Map.Entry<String, ODocumentEntry> entry : this.fields.entrySet()) {
        if (entry.getValue().exists()) {
          fields.add(entry.getKey());
        }
      }
    }

    return fields;
  }

  @Override
  public Set<String> getPropertyNames() {
    return getPropertyNamesInternal();
  }

  @Override
  public Set<String> getPropertyNamesInternal() {
    return calculatePropertyNames();
  }

  /**
   * retrieves a property value from the current document
   *
   * @param fieldName The field name, it can contain any character (it's not evaluated as an
   *                  expression, as in #eval()
   * @return the field value. Null if the field does not exist.
   */
  public <RET> RET getProperty(final String fieldName) {
    return getPropertyInternal(fieldName);
  }

  @Override
  public <RET> RET getPropertyInternal(String name) {
    return getPropertyInternal(name, isLazyLoad());
  }

  @Override
  public <RET> RET getPropertyInternal(String name, boolean lazyLoad) {
    if (name == null) {
      return null;
    }

    checkForBinding();
    RET value = (RET) ODocumentHelper.getIdentifiableValue(this, name);
    if (!name.startsWith("@")
        && lazyLoad
        && value instanceof YTRID rid
        && (rid.isPersistent() || rid.isNew())
        && ODatabaseRecordThreadLocal.instance().isDefined()) {
      // CREATE THE DOCUMENT OBJECT IN LAZY WAY
      var db = getSession();
      try {
        RET newValue = db.load((YTRID) value);

        unTrack(rid);
        track((YTIdentifiable) newValue);
        value = newValue;
        if (trackingChanges) {
          ORecordInternal.setDirtyManager((YTRecord) value, this.getDirtyManager());
        }
        ODocumentEntry entry = fields.get(name);
        entry.disableTracking(this, entry.value);
        entry.value = value;
        entry.enableTracking(this);
      } catch (YTRecordNotFoundException e) {
        return null;
      }
    }

    return convertToGraphElement(value);
  }

  @Override
  public <RET> RET getPropertyOnLoadValue(String name) {
    checkForBinding();

    Objects.requireNonNull(name, "Name argument is required.");
    YTVertexInternal.checkPropertyName(name);

    checkForFields();

    var field = fields.get(name);
    if (field != null) {
      RET onLoadValue = (RET) field.getOnLoadValue(getSession());
      if (onLoadValue instanceof ORidBag) {
        throw new IllegalArgumentException(
            "getPropertyOnLoadValue(name) is not designed to work with Edge properties");
      }
      if (onLoadValue instanceof YTRID orid) {
        if (isLazyLoad()) {
          try {
            return getSession().load(orid);
          } catch (YTRecordNotFoundException e) {
            return null;
          }
        } else {
          return onLoadValue;
        }
      }
      if (onLoadValue instanceof YTRecord record) {
        if (isLazyLoad()) {
          return onLoadValue;
        } else {
          return (RET) record.getIdentity();
        }
      }
      return onLoadValue;
    } else {
      return getPropertyInternal(name);
    }
  }

  private static <RET> RET convertToGraphElement(RET value) {
    if (value instanceof YTEntity) {
      if (((YTEntity) value).isVertex()) {
        value = (RET) ((YTEntity) value).toVertex();
      } else {
        if (((YTEntity) value).isEdge()) {
          value = (RET) ((YTEntity) value).toEdge();
        }
      }
    }
    return value;
  }

  /**
   * This method similar to {@link #getProperty(String)} but unlike before mentioned method it does
   * not load link automatically.
   *
   * @param fieldName the name of the link property
   * @return the link property value, or null if the property does not exist
   * @throws IllegalArgumentException if requested property is not a link.
   * @see #getProperty(String)
   */
  @Nullable
  @Override
  public YTIdentifiable getLinkProperty(String fieldName) {
    return getLinkPropertyInternal(fieldName);
  }

  @Nullable
  @Override
  public YTIdentifiable getLinkPropertyInternal(String name) {
    if (name == null) {
      return null;
    }

    var result = accessProperty(name);
    if (result == null) {
      return null;
    }

    if (!(result instanceof YTIdentifiable identifiable)
        || (result instanceof YTDocument document && document.isEmbedded())) {
      throw new IllegalArgumentException("Requested property " + name + " is not a link.");
    }

    var id = identifiable.getIdentity();
    if (!(id.isPersistent() || id.isNew())) {
      throw new IllegalArgumentException("Requested property " + name + " is not a link.");
    }

    return (YTIdentifiable) convertToGraphElement(result);
  }

  /**
   * retrieves a property value from the current document, without evaluating it (eg. no conversion
   * from RID to document)
   *
   * @param iFieldName The field name, it can contain any character (it's not evaluated as an
   *                   expression, as in #eval()
   * @return the field value. Null if the field does not exist.
   */
  <RET> RET getRawProperty(final String iFieldName) {
    checkForBinding();

    if (iFieldName == null) {
      return null;
    }

    return (RET) ODocumentHelper.getIdentifiableValue(this, iFieldName);
  }

  /**
   * sets a property value on current document
   *
   * @param iFieldName     The property name
   * @param iPropertyValue The property value
   */
  public void setProperty(final String iFieldName, Object iPropertyValue) {
    setPropertyInternal(iFieldName, iPropertyValue);
  }

  @Override
  public void setPropertyInternal(String name, Object value) {
    if (value instanceof YTEntity element
        && element.getSchemaClass() == null
        && !element.getIdentity().isValid()) {
      setProperty(name, value, YTType.EMBEDDED);
    } else {
      setPropertyInternal(name, value, OCommonConst.EMPTY_TYPES_ARRAY);
    }
  }

  /**
   * Sets
   *
   * @param name  The property name
   * @param value The property value
   * @param types Forced type (not auto-determined)
   */
  public void setProperty(String name, Object value, YTType... types) {
    setPropertyInternal(name, value, types);
  }

  @Override
  public void setPropertyInternal(String name, Object value, YTType... type) {
    checkForBinding();

    if (name == null) {
      throw new IllegalArgumentException("Field is null");
    }

    if (name.isEmpty()) {
      throw new IllegalArgumentException("Field name is empty");
    }

    final char begin = name.charAt(0);
    if (begin == '@') {
      switch (name.toLowerCase(Locale.ROOT)) {
        case ODocumentHelper.ATTRIBUTE_CLASS -> {
          setClassName(value.toString());
          return;
        }
        case ODocumentHelper.ATTRIBUTE_RID -> {
          if (status == STATUS.UNMARSHALLING) {
            recordId = new YTRecordId(value.toString());
          } else {
            throw new YTDatabaseException(
                "Attribute " + ODocumentHelper.ATTRIBUTE_RID + " is read-only");
          }

        }
        case ODocumentHelper.ATTRIBUTE_VERSION -> {
          if (status == STATUS.UNMARSHALLING) {
            setVersion(Integer.parseInt(value.toString()));
          }
          throw new YTDatabaseException(
              "Attribute " + ODocumentHelper.ATTRIBUTE_VERSION + " is read-only");
        }
      }
    }

    checkForFields();

    ODocumentEntry entry = fields.get(name);
    final boolean knownProperty;
    final Object oldValue;
    final YTType oldType;
    if (entry == null) {
      entry = new ODocumentEntry();
      fieldSize++;
      fields.put(name, entry);
      entry.markCreated();
      knownProperty = false;
      oldValue = null;
      oldType = null;
    } else {
      knownProperty = entry.exists();
      oldValue = entry.value;
      oldType = entry.type;
    }

    YTType fieldType = deriveFieldType(name, entry, type);
    if (value != null && fieldType != null) {
      value = ODocumentHelper.convertField(getSessionIfDefined(), this, name, fieldType, null,
          value);
    } else {
      if (value instanceof Enum) {
        value = value.toString();
      }
    }

    if (knownProperty)

    // CHECK IF IS REALLY CHANGED
    {
      if (value == null) {
        if (oldValue == null)
        // BOTH NULL: UNCHANGED
        {
          return;
        }
      } else {

        try {
          if (value.equals(oldValue)) {
            if (fieldType == oldType) {
              if (!(value instanceof ORecordElement))
              // SAME BUT NOT TRACKABLE: SET THE RECORD AS DIRTY TO BE SURE IT'S SAVED
              {
                setDirty();
              }

              // SAVE VALUE: UNCHANGED
              return;
            }
          }
        } catch (Exception e) {
          OLogManager.instance()
              .warn(
                  this,
                  "Error on checking the value of property %s against the record %s",
                  e,
                  name,
                  getIdentity());
        }
      }
    }

    if (oldValue instanceof
        ORidBag ridBag) {
      ridBag.setOwner(null);
    } else {
      if (oldValue instanceof YTDocument) {
        ((YTDocument) oldValue).removeOwner(this);
      }
    }

    if (oldValue instanceof YTIdentifiable) {
      unTrack((YTIdentifiable) oldValue);
    }

    if (value != null) {
      if (value instanceof YTDocument) {
        if (YTType.EMBEDDED.equals(fieldType)) {
          final YTDocument embeddedDocument = (YTDocument) value;
          ODocumentInternal.addOwner(embeddedDocument, this);
        }
      }
      if (value instanceof YTIdentifiable) {
        track((YTIdentifiable) value);
      }

      if (value instanceof ORidBag ridBag) {
        ridBag.setOwner(
            null); // in order to avoid IllegalStateException when ridBag changes the owner
        ridBag.setOwner(this);
        ridBag.setRecordAndField(recordId, name);
      }
    }

    if (fieldType == YTType.CUSTOM) {
      if (!DB_CUSTOM_SUPPORT.getValueAsBoolean()) {
        throw new YTDatabaseException(
            String.format(
                "YTType CUSTOM used by serializable types, for value  '%s' is not enabled, set"
                    + " `db.custom.support` to true for enable it",
                value));
      }
    }
    if (oldType != fieldType && oldType != null) {
      // can be made in a better way, but "keeping type" issue should be solved before
      if (value == null || fieldType != null || oldType != YTType.getTypeByValue(value)) {
        entry.type = fieldType;
      }
    }
    entry.disableTracking(this, oldValue);
    entry.value = value;
    if (!entry.exists()) {
      entry.setExists(true);
      fieldSize++;
    }
    entry.enableTracking(this);

    setDirty();
    if (!entry.isChanged()) {
      entry.original = oldValue;
      entry.markChanged();
    }
  }

  public <RET> RET removeProperty(final String iFieldName) {
    return removePropertyInternal(iFieldName);
  }

  @Override
  public <RET> RET removePropertyInternal(String name) {
    checkForBinding();
    checkForFields();

    if (ODocumentHelper.ATTRIBUTE_CLASS.equalsIgnoreCase(name)) {
      setClassName(null);
    } else {
      if (ODocumentHelper.ATTRIBUTE_RID.equalsIgnoreCase(name)) {
        throw new YTDatabaseException(
            "Attribute " + ODocumentHelper.ATTRIBUTE_RID + " is read-only");
      } else if (ODocumentHelper.ATTRIBUTE_VERSION.equalsIgnoreCase(name)) {
        if (ODocumentHelper.ATTRIBUTE_VERSION.equalsIgnoreCase(name)) {
          throw new YTDatabaseException(
              "Attribute " + ODocumentHelper.ATTRIBUTE_VERSION + " is read-only");
        }
      }
    }

    final ODocumentEntry entry = fields.get(name);
    if (entry == null) {
      return null;
    }

    Object oldValue = entry.value;

    if (entry.exists() && trackingChanges) {
      // SAVE THE OLD VALUE IN A SEPARATE MAP
      if (entry.original == null) {
        entry.original = entry.value;
      }
      entry.value = null;
      entry.setExists(false);
      entry.markChanged();
    } else {
      fields.remove(name);
    }
    fieldSize--;
    entry.disableTracking(this, oldValue);
    if (oldValue instanceof YTIdentifiable) {
      unTrack((YTIdentifiable) oldValue);
    }
    if (oldValue instanceof ORidBag) {
      ((ORidBag) oldValue).setOwner(null);
    }

    setDirty();
    return (RET) oldValue;
  }

  private static void validateFieldsSecurity(YTDatabaseSessionInternal internal, YTDocument iRecord)
      throws YTValidationException {
    if (internal == null) {
      return;
    }

    iRecord.checkForBinding();
    iRecord = (YTDocument) iRecord.getRecord();

    OSecurityInternal security = internal.getSharedContext().getSecurity();
    for (Entry<String, ODocumentEntry> mapEntry : iRecord.fields.entrySet()) {
      ODocumentEntry entry = mapEntry.getValue();
      if (entry != null && (entry.isTxChanged() || entry.isTxTrackedModified())) {
        if (!security.isAllowedWrite(internal, iRecord, mapEntry.getKey())) {
          throw new YTSecurityException(
              String.format(
                  "Change of field '%s' is not allowed for user '%s'",
                  iRecord.getClassName() + "." + mapEntry.getKey(),
                  internal.getUser().getName(internal)));
        }
      }
    }
  }

  private static void validateField(
      YTDatabaseSessionInternal session, YTImmutableSchema schema, YTDocument iRecord,
      YTImmutableProperty p)
      throws YTValidationException {
    iRecord.checkForBinding();
    iRecord = (YTDocument) iRecord.getRecord();

    final Object fieldValue;
    ODocumentEntry entry = iRecord.fields.get(p.getName());
    if (entry != null && entry.exists()) {
      // AVOID CONVERSIONS: FASTER!
      fieldValue = entry.value;

      if (p.isNotNull() && fieldValue == null)
      // NULLITY
      {
        throw new YTValidationException(
            "The field '" + p.getFullName() + "' cannot be null, record: " + iRecord);
      }

      if (fieldValue != null && p.getRegexp() != null && p.getType().equals(YTType.STRING)) {
        // REGEXP
        if (!((String) fieldValue).matches(p.getRegexp())) {
          throw new YTValidationException(
              "The field '"
                  + p.getFullName()
                  + "' does not match the regular expression '"
                  + p.getRegexp()
                  + "'. Field value is: "
                  + fieldValue
                  + ", record: "
                  + iRecord);
        }
      }

    } else {
      if (p.isMandatory()) {
        throw new YTValidationException(
            "The field '"
                + p.getFullName()
                + "' is mandatory, but not found on record: "
                + iRecord);
      }
      fieldValue = null;
    }

    final YTType type = p.getType();

    if (fieldValue != null && type != null) {
      // CHECK TYPE
      switch (type) {
        case LINK:
          validateLink(schema, p, fieldValue, false);
          break;
        case LINKLIST:
          if (!(fieldValue instanceof List)) {
            throw new YTValidationException(
                "The field '"
                    + p.getFullName()
                    + "' has been declared as LINKLIST but an incompatible type is used. Value: "
                    + fieldValue);
          }
          validateLinkCollection(schema, p, (Collection<Object>) fieldValue, entry);
          break;
        case LINKSET:
          if (!(fieldValue instanceof Set)) {
            throw new YTValidationException(
                "The field '"
                    + p.getFullName()
                    + "' has been declared as LINKSET but an incompatible type is used. Value: "
                    + fieldValue);
          }
          validateLinkCollection(schema, p, (Collection<Object>) fieldValue, entry);
          break;
        case LINKMAP:
          if (!(fieldValue instanceof Map)) {
            throw new YTValidationException(
                "The field '"
                    + p.getFullName()
                    + "' has been declared as LINKMAP but an incompatible type is used. Value: "
                    + fieldValue);
          }
          validateLinkCollection(schema, p, ((Map<?, Object>) fieldValue).values(), entry);
          break;

        case LINKBAG:
          if (!(fieldValue instanceof ORidBag)) {
            throw new YTValidationException(
                "The field '"
                    + p.getFullName()
                    + "' has been declared as LINKBAG but an incompatible type is used. Value: "
                    + fieldValue);
          }
          validateLinkCollection(schema, p, (Iterable<Object>) fieldValue, entry);
          break;
        case EMBEDDED:
          validateEmbedded(p, fieldValue);
          break;
        case EMBEDDEDLIST:
          if (!(fieldValue instanceof List)) {
            throw new YTValidationException(
                "The field '"
                    + p.getFullName()
                    + "' has been declared as EMBEDDEDLIST but an incompatible type is used. Value:"
                    + " "
                    + fieldValue);
          }
          if (p.getLinkedClass() != null) {
            for (Object item : ((List<?>) fieldValue)) {
              validateEmbedded(p, item);
            }
          } else {
            if (p.getLinkedType() != null) {
              for (Object item : ((List<?>) fieldValue)) {
                validateType(session, p, item);
              }
            }
          }
          break;
        case EMBEDDEDSET:
          if (!(fieldValue instanceof Set)) {
            throw new YTValidationException(
                "The field '"
                    + p.getFullName()
                    + "' has been declared as EMBEDDEDSET but an incompatible type is used. Value: "
                    + fieldValue);
          }
          if (p.getLinkedClass() != null) {
            for (Object item : ((Set<?>) fieldValue)) {
              validateEmbedded(p, item);
            }
          } else {
            if (p.getLinkedType() != null) {
              for (Object item : ((Set<?>) fieldValue)) {
                validateType(session, p, item);
              }
            }
          }
          break;
        case EMBEDDEDMAP:
          if (!(fieldValue instanceof Map)) {
            throw new YTValidationException(
                "The field '"
                    + p.getFullName()
                    + "' has been declared as EMBEDDEDMAP but an incompatible type is used. Value: "
                    + fieldValue);
          }
          if (p.getLinkedClass() != null) {
            for (Entry<?, ?> colleEntry : ((Map<?, ?>) fieldValue).entrySet()) {
              validateEmbedded(p, colleEntry.getValue());
            }
          } else {
            if (p.getLinkedType() != null) {
              for (Entry<?, ?> collEntry : ((Map<?, ?>) fieldValue).entrySet()) {
                validateType(session, p, collEntry.getValue());
              }
            }
          }
          break;
      }
    }

    if (p.getMin() != null && fieldValue != null) {
      // MIN
      final String min = p.getMin();
      if (p.getMinComparable().compareTo(fieldValue) > 0) {
        switch (p.getType()) {
          case STRING:
            throw new YTValidationException(
                "The field '"
                    + p.getFullName()
                    + "' contains fewer characters than "
                    + min
                    + " requested");
          case DATE:
          case DATETIME:
            throw new YTValidationException(
                "The field '"
                    + p.getFullName()
                    + "' contains the date "
                    + fieldValue
                    + " which precedes the first acceptable date ("
                    + min
                    + ")");
          case BINARY:
            throw new YTValidationException(
                "The field '"
                    + p.getFullName()
                    + "' contains fewer bytes than "
                    + min
                    + " requested");
          case EMBEDDEDLIST:
          case EMBEDDEDSET:
          case LINKLIST:
          case LINKSET:
          case EMBEDDEDMAP:
          case LINKMAP:
            throw new YTValidationException(
                "The field '"
                    + p.getFullName()
                    + "' contains fewer items than "
                    + min
                    + " requested");
          default:
            throw new YTValidationException(
                "The field '" + p.getFullName() + "' is less than " + min);
        }
      }
    }

    if (p.getMaxComparable() != null && fieldValue != null) {
      final String max = p.getMax();
      if (p.getMaxComparable().compareTo(fieldValue) < 0) {
        switch (p.getType()) {
          case STRING:
            throw new YTValidationException(
                "The field '"
                    + p.getFullName()
                    + "' contains more characters than "
                    + max
                    + " requested");
          case DATE:
          case DATETIME:
            throw new YTValidationException(
                "The field '"
                    + p.getFullName()
                    + "' contains the date "
                    + fieldValue
                    + " which is after the last acceptable date ("
                    + max
                    + ")");
          case BINARY:
            throw new YTValidationException(
                "The field '"
                    + p.getFullName()
                    + "' contains more bytes than "
                    + max
                    + " requested");
          case EMBEDDEDLIST:
          case EMBEDDEDSET:
          case LINKLIST:
          case LINKSET:
          case EMBEDDEDMAP:
          case LINKMAP:
            throw new YTValidationException(
                "The field '"
                    + p.getFullName()
                    + "' contains more items than "
                    + max
                    + " requested");
          default:
            throw new YTValidationException(
                "The field '" + p.getFullName() + "' is greater than " + max);
        }
      }
    }

    if (p.isReadonly() && !ORecordVersionHelper.isTombstone(iRecord.getVersion())) {
      if (entry != null
          && (entry.isTxChanged() || entry.isTxTrackedModified())
          && !entry.isTxCreated()) {
        // check if the field is actually changed by equal.
        // this is due to a limitation in the merge algorithm used server side marking all
        // non-simple fields as dirty
        Object orgVal = entry.getOnLoadValue(session);
        boolean simple =
            fieldValue != null ? YTType.isSimpleType(fieldValue) : YTType.isSimpleType(orgVal);
        if ((simple)
            || (fieldValue != null && orgVal == null)
            || (fieldValue == null && orgVal != null)
            || (fieldValue != null && !fieldValue.equals(orgVal))) {
          throw new YTValidationException(
              "The field '"
                  + p.getFullName()
                  + "' is immutable and cannot be altered. Field value is: "
                  + entry.value);
        }
      }
    }
  }

  private static void validateLinkCollection(
      YTImmutableSchema schema,
      final YTProperty property,
      Iterable<Object> values,
      ODocumentEntry value) {
    if (property.getLinkedClass() != null) {
      if (value.getTimeLine() != null) {
        List<OMultiValueChangeEvent<Object, Object>> event =
            value.getTimeLine().getMultiValueChangeEvents();
        for (var object : event) {
          if (object.getChangeType() == OMultiValueChangeEvent.OChangeType.ADD
              || object.getChangeType() == OMultiValueChangeEvent.OChangeType.UPDATE
              && object.getValue() != null) {
            validateLink(schema, property, object.getValue(), true);
          }
        }
      } else {
        for (Object object : values) {
          validateLink(schema, property, object, true);
        }
      }
    }
  }

  private static void validateType(YTDatabaseSessionInternal session, final YTProperty p,
      final Object value) {
    if (value != null) {
      if (YTType.convert(session, value, p.getLinkedType().getDefaultJavaType()) == null) {
        throw new YTValidationException(
            "The field '"
                + p.getFullName()
                + "' has been declared as "
                + p.getType()
                + " of type '"
                + p.getLinkedType()
                + "' but the value is "
                + value);
      }
    }
  }

  private static void validateLink(
      YTImmutableSchema schema, final YTProperty p, final Object fieldValue, boolean allowNull) {
    if (fieldValue == null) {
      if (allowNull) {
        return;
      } else {
        throw new YTValidationException(
            "The field '"
                + p.getFullName()
                + "' has been declared as "
                + p.getType()
                + " but contains a null record (probably a deleted record?)");
      }
    }

    if (!(fieldValue instanceof YTIdentifiable)) {
      throw new YTValidationException(
          "The field '"
              + p.getFullName()
              + "' has been declared as "
              + p.getType()
              + " but the value is not a record or a record-id");
    }

    final YTClass schemaClass = p.getLinkedClass();
    if (schemaClass != null && !schemaClass.isSubClassOf(OIdentity.CLASS_NAME)) {
      // DON'T VALIDATE OUSER AND OROLE FOR SECURITY RESTRICTIONS
      var identifiable = (YTIdentifiable) fieldValue;
      final YTRID rid = identifiable.getIdentity();
      if (!schemaClass.hasPolymorphicClusterId(rid.getClusterId())) {
        // AT THIS POINT CHECK THE CLASS ONLY IF != NULL BECAUSE IN CASE OF GRAPHS THE RECORD
        // COULD BE PARTIAL
        YTClass cls;
        var clusterId = rid.getClusterId();
        if (clusterId != YTRID.CLUSTER_ID_INVALID) {
          cls = schema.getClassByClusterId(rid.getClusterId());
        } else if (identifiable instanceof YTEntity element) {
          cls = element.getSchemaClass();
        } else {
          cls = null;
        }

        if (cls != null && !schemaClass.isSuperClassOf(cls)) {
          throw new YTValidationException(
              "The field '"
                  + p.getFullName()
                  + "' has been declared as "
                  + p.getType()
                  + " of type '"
                  + schemaClass.getName()
                  + "' but the value is the document "
                  + rid
                  + " of class '"
                  + cls
                  + "'");
        }
      }
    }
  }

  private static void validateEmbedded(final YTProperty p, final Object fieldValue) {
    if (fieldValue == null) {
      return;
    }
    if (fieldValue instanceof YTRecordId) {
      throw new YTValidationException(
          "The field '"
              + p.getFullName()
              + "' has been declared as "
              + p.getType()
              + " but the value is the RecordID "
              + fieldValue);
    } else {
      if (fieldValue instanceof YTIdentifiable embedded) {
        if (embedded.getIdentity().isValid()) {
          throw new YTValidationException(
              "The field '"
                  + p.getFullName()
                  + "' has been declared as "
                  + p.getType()
                  + " but the value is a document with the valid RecordID "
                  + fieldValue);
        }

        final YTRecord embeddedRecord = embedded.getRecord();
        if (embeddedRecord instanceof YTDocument doc) {
          final YTClass embeddedClass = p.getLinkedClass();
          if (doc.isVertex()) {
            throw new YTValidationException(
                "The field '"
                    + p.getFullName()
                    + "' has been declared as "
                    + p.getType()
                    + " with linked class '"
                    + embeddedClass
                    + "' but the record is of class '"
                    + doc.getImmutableSchemaClass().getName()
                    + "' that is vertex class");
          }

          if (doc.isEdge()) {
            throw new YTValidationException(
                "The field '"
                    + p.getFullName()
                    + "' has been declared as "
                    + p.getType()
                    + " with linked class '"
                    + embeddedClass
                    + "' but the record is of class '"
                    + doc.getImmutableSchemaClass().getName()
                    + "' that is edge class");
          }
        }

        final YTClass embeddedClass = p.getLinkedClass();
        if (embeddedClass != null) {

          if (!(embeddedRecord instanceof YTDocument doc)) {
            throw new YTValidationException(
                "The field '"
                    + p.getFullName()
                    + "' has been declared as "
                    + p.getType()
                    + " with linked class '"
                    + embeddedClass
                    + "' but the record was not a document");
          }

          if (doc.getImmutableSchemaClass() == null) {
            throw new YTValidationException(
                "The field '"
                    + p.getFullName()
                    + "' has been declared as "
                    + p.getType()
                    + " with linked class '"
                    + embeddedClass
                    + "' but the record has no class");
          }

          if (!(doc.getImmutableSchemaClass().isSubClassOf(embeddedClass))) {
            throw new YTValidationException(
                "The field '"
                    + p.getFullName()
                    + "' has been declared as "
                    + p.getType()
                    + " with linked class '"
                    + embeddedClass
                    + "' but the record is of class '"
                    + doc.getImmutableSchemaClass().getName()
                    + "' that is not a subclass of that");
          }

          doc.validate();
        }

      } else {
        throw new YTValidationException(
            "The field '"
                + p.getFullName()
                + "' has been declared as "
                + p.getType()
                + " but an incompatible type is used. Value: "
                + fieldValue);
      }
    }
  }

  /**
   * Copies the current instance to a new one. Hasn't been choose the clone() to let YTDocument
   * return type. Once copied the new instance has the same identity and values but all the internal
   * structure are totally independent by the source.
   */
  public YTDocument copy() {
    checkForBinding();

    var doc = new YTDocument();
    ORecordInternal.unsetDirty(doc);
    var newDoc = (YTDocument) copyTo(doc);
    newDoc.dirty = true;

    return newDoc;
  }

  public YTDocument copy(YTDatabaseSessionInternal session) {
    var newDoc = copy();
    newDoc.setup(session);

    return newDoc;
  }

  /**
   * Copies all the fields into iDestination document.
   */
  @Override
  public final YTRecordAbstract copyTo(final YTRecordAbstract iDestination) {
    checkForBinding();

    if (iDestination.isDirty()) {
      throw new YTDatabaseException("Cannot copy to dirty records");
    }

    checkForFields();

    YTDocument destination = (YTDocument) iDestination;

    super.copyTo(iDestination);

    destination.ordered = ordered;

    destination.className = className;
    destination.immutableSchemaVersion = -1;
    destination.immutableClazz = null;

    destination.trackingChanges = trackingChanges;
    destination.owner = owner;

    if (fields != null) {
      destination.fields =
          fields instanceof LinkedHashMap ? new LinkedHashMap<>() : new HashMap<>();
      for (Entry<String, ODocumentEntry> entry : fields.entrySet()) {
        var originalEntry = entry.getValue();
        ODocumentEntry docEntry = originalEntry.clone();
        destination.fields.put(entry.getKey(), docEntry);
        docEntry.value = ODocumentHelper.cloneValue(getSession(), destination,
            entry.getValue().value);
      }
    } else {
      destination.fields = null;
    }
    destination.fieldSize = fieldSize;
    destination.addAllMultiValueChangeListeners();

    destination.dirty = dirty; // LEAVE IT AS LAST TO AVOID SOMETHING SET THE FLAG TO TRUE
    destination.contentChanged = contentChanged;

    var dirtyManager = new ODirtyManager();
    if (dirty) {
      dirtyManager.setDirty(this);
    }

    destination.dirtyManager = dirtyManager;

    return destination;
  }

  public boolean hasSameContentOf(final YTDocument iOther) {
    iOther.checkForBinding();
    checkForBinding();

    final YTDatabaseSessionInternal currentDb = ODatabaseRecordThreadLocal.instance()
        .getIfDefined();
    return ODocumentHelper.hasSameContentOf(this, currentDb, iOther, currentDb, null);
  }

  @Override
  public byte[] toStream() {
    checkForBinding();
    if (recordFormat == null) {
      setup(ODatabaseRecordThreadLocal.instance().getIfDefined());
    }

    STATUS prev = status;
    status = STATUS.MARSHALLING;
    try {
      if (source == null) {
        source = recordFormat.toStream(getSession(), this);
      }
    } finally {
      status = prev;
    }

    return source;
  }

  /**
   * Returns the document as Map String,Object . If the document has identity, then the @rid entry
   * is valued. If the document has a class, then the @class entry is valued.
   *
   * @since 2.0
   */
  public Map<String, Object> toMap() {
    checkForBinding();
    final Map<String, Object> map = new HashMap<>();
    for (String field : fieldNames()) {
      map.put(field, field(field));
    }

    final YTRID id = getIdentity();
    if (id.isValid()) {
      map.put(ODocumentHelper.ATTRIBUTE_RID, id);
    }

    final String className = getClassName();
    if (className != null) {
      map.put(ODocumentHelper.ATTRIBUTE_CLASS, className);
    }

    return map;
  }

  /**
   * Dumps the instance as string.
   */
  @Override
  public String toString() {
    if (isUnloaded()) {
      return "Unloaded record {" + getIdentity() + ", v" + getVersion() + "}";
    }

    return toString(new HashSet<>());
  }

  /**
   * Fills the YTDocument directly with the string representation of the document itself. Use it for
   * faster insertion but pay attention to respect the YouTrackDB record format.
   *
   * <p><code> record.reset();<br> record.setClassName("Account");<br>
   * record.fromString(new String("Account@id:" + data.getCyclesDone() +
   * ",name:'Luca',surname:'Garulli',birthDate:" + date.getTime()<br> + ",salary:" + 3000f +
   * i));<br> record.save();<br> </code>
   *
   * @param iValue String representation of the record.
   */
  @Deprecated
  public void fromString(final String iValue) {
    incrementLoading();
    try {
      dirty = true;
      contentChanged = true;
      source = iValue.getBytes(StandardCharsets.UTF_8);

      removeAllCollectionChangeListeners();

      fields = null;
      fieldSize = 0;
    } finally {
      decrementLoading();
    }
  }

  /**
   * Returns the set of field names.
   */
  public String[] fieldNames() {
    return calculatePropertyNames().toArray(new String[]{});
  }

  /**
   * Returns the array of field values.
   */
  public Object[] fieldValues() {
    checkForBinding();

    checkForFields();
    final List<Object> res = new ArrayList<>(fields.size());
    for (Map.Entry<String, ODocumentEntry> entry : fields.entrySet()) {
      if (entry.getValue().exists()
          && (propertyAccess == null || propertyAccess.isReadable(entry.getKey()))) {
        res.add(entry.getValue().value);
      }
    }
    return res.toArray();
  }

  public <RET> RET rawField(final String iFieldName) {
    if (iFieldName == null || iFieldName.isEmpty()) {
      return null;
    }

    checkForBinding();
    if (!checkForFields(iFieldName))
    // NO FIELDS
    {
      return null;
    }

    // OPTIMIZATION
    if (!allowChainedAccess
        || (iFieldName.charAt(0) != '@'
        && OStringSerializerHelper.indexOf(iFieldName, 0, '.', '[') == -1)) {
      return (RET) accessProperty(iFieldName);
    }

    // NOT FOUND, PARSE THE FIELD NAME
    return ODocumentHelper.getFieldValue(getSession(), this, iFieldName);
  }

  /**
   * Evaluates a SQL expression against current document. Example: <code> long amountPlusVat =
   * doc.eval("amount * 120 / 100");</code>
   *
   * @param iExpression SQL expression to evaluate.
   * @return The result of expression
   * @throws YTQueryParsingException in case the expression is not valid
   */
  public Object eval(final String iExpression) {
    checkForBinding();

    var context = new OBasicCommandContext();
    context.setDatabase(getSession());

    return eval(iExpression, context);
  }

  /**
   * Evaluates a SQL expression against current document by passing a context. The expression can
   * refer to the variables contained in the context. Example: <code> OCommandContext context = new
   * OBasicCommandContext().setVariable("vat", 20); long amountPlusVat = doc.eval("amount *
   * (100+$vat) / 100", context); </code>
   *
   * @param iExpression SQL expression to evaluate.
   * @return The result of expression
   * @throws YTQueryParsingException in case the expression is not valid
   */
  public Object eval(final String iExpression, @Nonnull final OCommandContext iContext) {
    checkForBinding();

    if (iContext.getDatabase() != getSession()) {
      throw new YTDatabaseException(
          "The context is bound to a different database instance, use the context from the same database instance");
    }

    return new OSQLPredicate(iContext, iExpression).evaluate(this, null, iContext);
  }

  /**
   * Reads the field value.
   *
   * @param iFieldName field name
   * @return field value if defined, otherwise null
   */
  @Override
  public <RET> RET field(final String iFieldName) {
    checkForBinding();

    RET value = this.rawField(iFieldName);

    if (!iFieldName.startsWith("@")
        && lazyLoad
        && value instanceof YTRID
        && (((YTRID) value).isPersistent() || ((YTRID) value).isNew())
        && ODatabaseRecordThreadLocal.instance().isDefined()) {
      // CREATE THE DOCUMENT OBJECT IN LAZY WAY
      var db = getSession();
      try {
        RET newValue = db.load((YTRID) value);
        unTrack((YTRID) value);
        track((YTIdentifiable) newValue);
        value = newValue;
        if (this.trackingChanges) {
          ORecordInternal.setDirtyManager((YTRecord) value, this.getDirtyManager());
        }
        if (!iFieldName.contains(".")) {
          ODocumentEntry entry = fields.get(iFieldName);
          entry.disableTracking(this, entry.value);
          entry.value = value;
          entry.enableTracking(this);
        }
      } catch (YTRecordNotFoundException e) {
        return null;
      }
    }
    return value;
  }

  /**
   * Reads the field value forcing the return type. Use this method to force return of YTRID instead
   * of the entire document by passing YTRID.class as iFieldType.
   *
   * @param iFieldName field name
   * @param iFieldType Forced type.
   * @return field value if defined, otherwise null
   */
  public <RET> RET field(final String iFieldName, final Class<?> iFieldType) {
    checkForBinding();
    RET value = this.rawField(iFieldName);

    if (value != null) {
      value =
          ODocumentHelper.convertField(getSession()
              , this, iFieldName, YTType.getTypeByClass(iFieldType), iFieldType, value);
    }

    return value;
  }

  /**
   * Reads the field value forcing the return type. Use this method to force return of binary data.
   *
   * @param iFieldName field name
   * @param iFieldType Forced type.
   * @return field value if defined, otherwise null
   */
  public <RET> RET field(final String iFieldName, final YTType iFieldType) {
    checkForBinding();

    var session = getSessionIfDefined();
    RET value = field(iFieldName);
    YTType original;
    if (iFieldType != null && iFieldType != (original = fieldType(iFieldName))) {
      // this is needed for the csv serializer that don't give back values
      if (original == null) {
        original = YTType.getTypeByValue(value);
        if (iFieldType == original) {
          return value;
        }
      }

      final Object newValue;

      if (iFieldType == YTType.BINARY && value instanceof String) {
        newValue = OStringSerializerHelper.getBinaryContent(value);
      } else {
        if (iFieldType == YTType.DATE && value instanceof Long) {
          newValue = new Date((Long) value);
        } else {
          if ((iFieldType == YTType.EMBEDDEDSET || iFieldType == YTType.LINKSET)
              && value instanceof List) {
            newValue =
                Collections.unmodifiableSet(
                    (Set<?>)
                        ODocumentHelper.convertField(getSession(), this, iFieldName, iFieldType,
                            null,
                            value));
          } else {
            if ((iFieldType == YTType.EMBEDDEDLIST || iFieldType == YTType.LINKLIST)
                && value instanceof Set) {
              newValue =
                  Collections.unmodifiableList(
                      (List<?>)
                          ODocumentHelper.convertField(session, this, iFieldName,
                              iFieldType, null, value));
            } else {
              if ((iFieldType == YTType.EMBEDDEDMAP || iFieldType == YTType.LINKMAP)
                  && value instanceof Map) {
                newValue =
                    Collections.unmodifiableMap(
                        (Map<?, ?>)
                            ODocumentHelper.convertField(session,
                                this, iFieldName, iFieldType, null, value));
              } else {
                newValue = YTType.convert(session, value, iFieldType.getDefaultJavaType());
              }
            }
          }
        }
      }

      if (newValue != null) {
        value = (RET) newValue;
      }
    }
    return value;
  }

  /**
   * Writes the field value. This method sets the current document as dirty.
   *
   * @param iFieldName     field name. If contains dots (.) the change is applied to the nested
   *                       documents in chain. To disable this feature call
   *                       {@link #setAllowChainedAccess(boolean)} to false.
   * @param iPropertyValue field value
   * @return The Record instance itself giving a "fluent interface". Useful to call multiple methods
   * in chain.
   */
  public YTDocument field(final String iFieldName, Object iPropertyValue) {
    return field(iFieldName, iPropertyValue, OCommonConst.EMPTY_TYPES_ARRAY);
  }

  /**
   * Fills a document passing the field names/values.
   */
  public YTDocument fields(
      final String iFieldName, final Object iFieldValue, final Object... iFields) {
    checkForBinding();

    if (iFields != null && iFields.length % 2 != 0) {
      throw new IllegalArgumentException("Fields must be passed in pairs as name and value");
    }

    field(iFieldName, iFieldValue);
    if (iFields != null && iFields.length > 0) {
      for (int i = 0; i < iFields.length; i += 2) {
        field(iFields[i].toString(), iFields[i + 1]);
      }
    }
    return this;
  }

  /**
   * Deprecated. Use fromMap(Map) instead.<br> Fills a document passing the field names/values as a
   * Map String,Object where the keys are the field names and the values are the field values.
   *
   * @see #fromMap(Map)
   */
  @Deprecated
  public YTDocument fields(final Map<String, Object> iMap) {
    fromMap(iMap);
    return this;
  }

  /**
   * Fills a document passing the field names/values as a Map String,Object where the keys are the
   * field names and the values are the field values. It accepts also @rid for record id and @class
   * for class name.
   *
   * @since 2.0
   */
  public void fromMap(final Map<String, ?> map) {
    checkForBinding();

    status = STATUS.UNMARSHALLING;
    try {
      if (map != null) {
        for (Entry<String, ?> entry : map.entrySet()) {
          var key = entry.getKey();
          if (key.isEmpty()) {
            continue;
          }
          if (key.charAt(0) == '@') {
            continue;
          }

          setProperty(entry.getKey(), entry.getValue());
        }
      }
    } finally {
      status = STATUS.LOADED;
    }
  }

  public final YTDocument fromJSON(final String iSource, final String iOptions) {
    return super.fromJSON(iSource, iOptions);
  }

  /**
   * Writes the field value forcing the type. This method sets the current document as dirty.
   *
   * <p>if there's a schema definition for the specified field, the value will be converted to
   * respect the schema definition if needed. if the type defined in the schema support less
   * precision than the iPropertyValue provided, the iPropertyValue will be converted following the
   * java casting rules with possible precision loss.
   *
   * @param iFieldName     field name. If contains dots (.) the change is applied to the nested
   *                       documents in chain. To disable this feature call
   *                       {@link #setAllowChainedAccess(boolean)} to false.
   * @param iPropertyValue field value.
   * @param iFieldType     Forced type (not auto-determined)
   * @return The Record instance itself giving a "fluent interface". Useful to call multiple methods
   * in chain. If the updated document is another document (using the dot (.) notation) then the
   * document returned is the changed one or NULL if no document has been found in chain
   */
  public YTDocument field(String iFieldName, Object iPropertyValue, YTType... iFieldType) {
    checkForBinding();

    if (iFieldName == null) {
      throw new IllegalArgumentException("Field is null");
    }

    if (iFieldName.isEmpty()) {
      throw new IllegalArgumentException("Field name is empty");
    }

    switch (iFieldName) {
      case ODocumentHelper.ATTRIBUTE_CLASS -> {
        setClassName(iPropertyValue.toString());
        return this;
      }
      case ODocumentHelper.ATTRIBUTE_RID -> {
        recordId.fromString(iPropertyValue.toString());
        return this;
      }
      case ODocumentHelper.ATTRIBUTE_VERSION -> {
        if (iPropertyValue != null) {
          int v;

          if (iPropertyValue instanceof Number) {
            v = ((Number) iPropertyValue).intValue();
          } else {
            v = Integer.parseInt(iPropertyValue.toString());
          }

          recordVersion = v;
        }
        return this;
      }
    }

    final int lastDotSep = allowChainedAccess ? iFieldName.lastIndexOf('.') : -1;
    final int lastArraySep = allowChainedAccess ? iFieldName.lastIndexOf('[') : -1;

    final int lastSep = Math.max(lastArraySep, lastDotSep);
    final boolean lastIsArray = lastArraySep > lastDotSep;

    if (lastSep > -1) {
      // SUB PROPERTY GET 1 LEVEL BEFORE LAST
      final Object subObject = field(iFieldName.substring(0, lastSep));
      if (subObject != null) {
        final String subFieldName =
            lastIsArray ? iFieldName.substring(lastSep) : iFieldName.substring(lastSep + 1);
        if (subObject instanceof YTDocument) {
          // SUB-DOCUMENT
          ((YTDocument) subObject).field(subFieldName, iPropertyValue);
          return (YTDocument) (((YTDocument) subObject).isEmbedded() ? this : subObject);
        } else {
          if (subObject instanceof Map<?, ?>) {
            // KEY/VALUE
            ((Map<String, Object>) subObject).put(subFieldName, iPropertyValue);
          } else {
            if (OMultiValue.isMultiValue(subObject)) {
              if ((subObject instanceof List<?> || subObject.getClass().isArray()) && lastIsArray) {
                // List // Array Type with a index subscript.
                final int subFieldNameLen = subFieldName.length();

                if (subFieldName.charAt(subFieldNameLen - 1) != ']') {
                  throw new IllegalArgumentException("Missed closing ']'");
                }

                final String indexPart = subFieldName.substring(1, subFieldNameLen - 1);
                final Object indexPartObject = ODocumentHelper.getIndexPart(null, indexPart);
                final String indexAsString =
                    indexPartObject == null ? null : indexPartObject.toString();

                if (indexAsString == null) {
                  throw new IllegalArgumentException(
                      "List / array subscripts must resolve to integer values.");
                }
                try {
                  final int index = Integer.parseInt(indexAsString);
                  OMultiValue.setValue(subObject, iPropertyValue, index);
                } catch (NumberFormatException e) {
                  throw new IllegalArgumentException(
                      "List / array subscripts must resolve to integer values.", e);
                }
              } else {
                // APPLY CHANGE TO ALL THE ITEM IN SUB-COLLECTION
                for (Object subObjectItem : OMultiValue.getMultiValueIterable(subObject)) {
                  if (subObjectItem instanceof YTDocument) {
                    // SUB-DOCUMENT, CHECK IF IT'S NOT LINKED
                    if (!((YTDocument) subObjectItem).isEmbedded()) {
                      throw new IllegalArgumentException(
                          "Property '"
                              + iFieldName
                              + "' points to linked collection of items. You can only change"
                              + " embedded documents in this way");
                    }
                    ((YTDocument) subObjectItem).field(subFieldName, iPropertyValue);
                  } else {
                    if (subObjectItem instanceof Map<?, ?>) {
                      // KEY/VALUE
                      ((Map<String, Object>) subObjectItem).put(subFieldName, iPropertyValue);
                    }
                  }
                }
              }
              return this;
            }
          }
        }
      } else {
        throw new IllegalArgumentException(
            "Property '"
                + iFieldName.substring(0, lastSep)
                + "' is null, is possible to set a value with dotted notation only on not null"
                + " property");
      }
      return null;
    }

    iFieldName = checkFieldName(iFieldName);

    checkForFields();

    ODocumentEntry entry = fields.get(iFieldName);
    final boolean knownProperty;
    final Object oldValue;
    final YTType oldType;
    if (entry == null) {
      entry = new ODocumentEntry();
      fieldSize++;
      fields.put(iFieldName, entry);
      entry.markCreated();
      knownProperty = false;
      oldValue = null;
      oldType = null;
    } else {
      knownProperty = entry.exists();
      oldValue = entry.value;
      oldType = entry.type;
    }

    YTType fieldType = deriveFieldType(iFieldName, entry, iFieldType);
    if (iPropertyValue != null && fieldType != null) {
      iPropertyValue =
          ODocumentHelper.convertField(getSession(), this, iFieldName, fieldType, null,
              iPropertyValue);
    } else {
      if (iPropertyValue instanceof Enum) {
        iPropertyValue = iPropertyValue.toString();
      }
    }

    if (knownProperty)
    // CHECK IF IS REALLY CHANGED
    {
      if (iPropertyValue == null) {
        if (oldValue == null)
        // BOTH NULL: UNCHANGED
        {
          return this;
        }
      } else {

        try {
          if (iPropertyValue.equals(oldValue)) {
            if (fieldType == oldType) {
              if (!(iPropertyValue instanceof ORecordElement))
              // SAME BUT NOT TRACKABLE: SET THE RECORD AS DIRTY TO BE SURE IT'S SAVED
              {
                setDirty();
              }

              // SAVE VALUE: UNCHANGED
              return this;
            }
          } else {
            if (iPropertyValue instanceof byte[]
                && Arrays.equals((byte[]) iPropertyValue, (byte[]) oldValue)) {
              // SAVE VALUE: UNCHANGED
              return this;
            }
          }
        } catch (Exception e) {
          OLogManager.instance()
              .warn(
                  this,
                  "Error on checking the value of property %s against the record %s",
                  e,
                  iFieldName,
                  getIdentity());
        }
      }
    }

    if (oldValue instanceof ORidBag ridBag) {
      ridBag.setOwner(null);
      ridBag.setRecordAndField(recordId, iFieldName);
    } else {
      if (oldValue instanceof YTDocument) {
        ((YTDocument) oldValue).removeOwner(this);
      }
    }

    if (oldValue instanceof YTIdentifiable) {
      unTrack((YTIdentifiable) oldValue);
    }

    if (iPropertyValue != null) {
      if (iPropertyValue instanceof YTDocument) {
        if (YTType.EMBEDDED.equals(fieldType)) {
          final YTDocument embeddedDocument = (YTDocument) iPropertyValue;
          ODocumentInternal.addOwner(embeddedDocument, this);
        } else {
          if (YTType.LINK.equals(fieldType)) {
            final YTDocument embeddedDocument = (YTDocument) iPropertyValue;
            ODocumentInternal.removeOwner(embeddedDocument, this);
          }
        }
      }
      if (iPropertyValue instanceof YTIdentifiable) {
        track((YTIdentifiable) iPropertyValue);
      }

      if (iPropertyValue instanceof ORidBag ridBag) {
        ridBag.setOwner(
            null); // in order to avoid IllegalStateException when ridBag changes the owner
        // (YTDocument.merge)
        ridBag.setOwner(this);
        ridBag.setRecordAndField(recordId, iFieldName);
      }
    }

    if (fieldType == YTType.CUSTOM) {
      if (!DB_CUSTOM_SUPPORT.getValueAsBoolean()) {
        throw new YTDatabaseException(
            String.format(
                "YTType CUSTOM used by serializable types, for value  '%s' is not enabled, set"
                    + " `db.custom.support` to true for enable it",
                iPropertyValue));
      }
    }

    if (oldType != fieldType && oldType != null) {
      // can be made in a better way, but "keeping type" issue should be solved before
      if (iPropertyValue == null
          || fieldType != null
          || oldType != YTType.getTypeByValue(iPropertyValue)) {
        entry.type = fieldType;
      }
    }
    entry.disableTracking(this, oldValue);
    entry.value = iPropertyValue;
    if (!entry.exists()) {
      entry.setExists(true);
      fieldSize++;
    }
    entry.enableTracking(this);

    setDirty();
    if (!entry.isChanged()) {
      entry.original = oldValue;
      entry.markChanged();
    }

    return this;
  }

  /**
   * Removes a field.
   */
  @Override
  public Object removeField(final String iFieldName) {
    checkForBinding();
    checkForFields();

    if (ODocumentHelper.ATTRIBUTE_CLASS.equalsIgnoreCase(iFieldName)) {
      setClassName(null);
    } else {
      if (ODocumentHelper.ATTRIBUTE_RID.equalsIgnoreCase(iFieldName)) {
        recordId = new ChangeableRecordId();
      }
    }

    final ODocumentEntry entry = fields.get(iFieldName);
    if (entry == null) {
      return null;
    }
    Object oldValue = entry.value;

    if (entry.exists() && trackingChanges) {
      // SAVE THE OLD VALUE IN A SEPARATE MAP
      if (entry.original == null) {
        entry.original = entry.value;
      }
      entry.value = null;
      entry.setExists(false);
      entry.markChanged();
    } else {
      fields.remove(iFieldName);
    }
    fieldSize--;

    entry.disableTracking(this, oldValue);
    if (oldValue instanceof YTIdentifiable) {
      unTrack((YTIdentifiable) oldValue);
    }
    if (oldValue instanceof ORidBag) {
      ((ORidBag) oldValue).setOwner(null);
    }
    setDirty();
    return oldValue;
  }

  /**
   * Merge current document with the document passed as parameter. If the field already exists then
   * the conflicts are managed based on the value of the parameter 'iUpdateOnlyMode'.
   *
   * @param iOther                              Other YTDocument instance to merge
   * @param iUpdateOnlyMode                     if true, the other document properties will always
   *                                            be added or overwritten. If false, the missed
   *                                            properties in the "other" document will be removed
   *                                            by original document
   * @param iMergeSingleItemsOfMultiValueFields If true, merges single items of multi field fields
   *                                            (collections, maps, arrays, etc)
   */
  public YTDocument merge(
      final YTDocument iOther,
      boolean iUpdateOnlyMode,
      boolean iMergeSingleItemsOfMultiValueFields) {
    iOther.checkForBinding();

    checkForBinding();

    iOther.checkForFields();

    if (className == null && iOther.getImmutableSchemaClass() != null) {
      className = iOther.getImmutableSchemaClass().getName();
    }

    return mergeMap(
        ((YTDocument) iOther.getRecord()).fields,
        iUpdateOnlyMode,
        iMergeSingleItemsOfMultiValueFields);
  }

  /**
   * Returns list of changed fields. There are two types of changes:
   *
   * <ol>
   *   <li>Value of field itself was changed by calling of {@link #field(String, Object)} method for
   *       example.
   *   <li>Internal state of field was changed but was not saved. This case currently is applicable
   *       for for collections only.
   * </ol>
   *
   * @return Array of fields, values of which were changed.
   */
  public String[] getDirtyFields() {
    checkForBinding();

    if (fields == null || fields.isEmpty()) {
      return EMPTY_STRINGS;
    }

    final Set<String> dirtyFields = new HashSet<>();
    for (Entry<String, ODocumentEntry> entry : fields.entrySet()) {
      if (entry.getValue().isChanged() || entry.getValue().isTrackedModified()) {
        dirtyFields.add(entry.getKey());
      }
    }
    return dirtyFields.toArray(new String[0]);
  }

  /**
   * Returns the original value of a field before it has been changed.
   *
   * @param iFieldName Property name to retrieve the original value
   */
  public Object getOriginalValue(final String iFieldName) {
    checkForBinding();

    if (fields != null) {
      ODocumentEntry entry = fields.get(iFieldName);
      if (entry != null) {
        return entry.original;
      }
    }
    return null;
  }

  public OMultiValueChangeTimeLine<Object, Object> getCollectionTimeLine(final String iFieldName) {
    checkForBinding();

    ODocumentEntry entry = fields != null ? fields.get(iFieldName) : null;
    return entry != null ? entry.getTimeLine() : null;
  }

  /**
   * Returns the iterator fields
   */
  @Override
  @Nonnull
  public Iterator<Entry<String, Object>> iterator() {
    checkForBinding();
    checkForFields();

    if (fields == null) {
      return OEmptyMapEntryIterator.INSTANCE;
    }

    final Iterator<Entry<String, ODocumentEntry>> iterator = fields.entrySet().iterator();
    return new Iterator<>() {
      private Entry<String, ODocumentEntry> current;
      private boolean read = true;

      @Override
      public boolean hasNext() {
        while (iterator.hasNext()) {
          current = iterator.next();
          if (current.getValue().exists()
              && (propertyAccess == null || propertyAccess.isReadable(current.getKey()))) {
            read = false;
            return true;
          }
        }
        return false;
      }

      @Override
      public Entry<String, Object> next() {
        if (read) {
          if (!hasNext()) {
            // Look wrong but is correct, it need to fail if there isn't next.
            iterator.next();
          }
        }
        final Entry<String, Object> toRet =
            new Entry<>() {
              private final Entry<String, ODocumentEntry> intern = current;

              @Override
              public Object setValue(Object value) {
                throw new UnsupportedOperationException();
              }

              @Override
              public Object getValue() {
                return intern.getValue().value;
              }

              @Override
              public String getKey() {
                return intern.getKey();
              }

              @Override
              public int hashCode() {
                return intern.hashCode();
              }

              @Override
              public boolean equals(Object obj) {
                //noinspection rawtypes
                if (obj instanceof Entry entry) {
                  return intern.getKey().equals(entry.getKey())
                      && intern.getValue().value.equals(entry.getValue());
                }

                return intern.equals(obj);
              }
            };
        read = true;
        return toRet;
      }

      @Override
      public void remove() {
        var entry = current.getValue();
        if (trackingChanges) {
          if (entry.isChanged()) {
            entry.original = entry.value;
          }
          entry.value = null;
          entry.setExists(false);
          entry.markChanged();
        } else {
          iterator.remove();
        }
        fieldSize--;

        entry.disableTracking(YTDocument.this, entry.value);
      }
    };
  }

  /**
   * Checks if a field exists.
   *
   * @return True if exists, otherwise false.
   */
  @Override
  public boolean containsField(final String iFieldName) {
    return hasProperty(iFieldName);
  }

  /**
   * Checks if a property exists.
   *
   * @return True if exists, otherwise false.
   */
  @Override
  public boolean hasProperty(final String propertyName) {
    checkForBinding();
    if (propertyName == null) {
      return false;
    }

    if (checkForFields(propertyName)
        && (propertyAccess == null || propertyAccess.isReadable(propertyName))) {
      ODocumentEntry entry = fields.get(propertyName);
      return entry != null && entry.exists();
    } else {
      return false;
    }
  }

  /**
   * Returns true if the record has some owner.
   */
  public boolean hasOwners() {
    return owner != null && owner.get() != null;
  }

  @Override
  public ORecordElement getOwner() {
    if (owner == null) {
      return null;
    }
    return owner.get();
  }

  @Deprecated
  public Iterable<ORecordElement> getOwners() {
    if (owner == null || owner.get() == null) {
      return Collections.emptyList();
    }

    final List<ORecordElement> result = new ArrayList<>();
    result.add(owner.get());
    return result;
  }

  /**
   * Propagates the dirty status to the owner, if any. This happens when the object is embedded in
   * another one.
   */
  @Override
  public YTRecordAbstract setDirty() {
    if (owner != null) {
      // PROPAGATES TO THE OWNER
      var ownerDoc = owner.get();
      if (ownerDoc != null) {
        ownerDoc.setDirty();
      }
    } else {
      if (!isDirty()) {
        checkForBinding();
        getDirtyManager().setDirty(this);
      }
    }

    // THIS IS IMPORTANT TO BE SURE THAT FIELDS ARE LOADED BEFORE IT'S TOO LATE AND THE RECORD
    // _SOURCE IS NULL
    checkForFields();
    super.setDirty();

    return this;
  }

  @Override
  public void setDirtyNoChanged() {
    if (owner != null) {
      // PROPAGATES TO THE OWNER
      var ownerDoc = owner.get();
      if (ownerDoc != null) {
        ownerDoc.setDirtyNoChanged();
      }
    }

    getDirtyManager().setDirty(this);
    // THIS IS IMPORTANT TO BE SURE THAT FIELDS ARE LOADED BEFORE IT'S TOO LATE AND THE RECORD
    // _SOURCE IS NULL
    checkForFields();

    super.setDirtyNoChanged();
  }

  @Override
  public final YTDocument fromStream(final byte[] iRecordBuffer) {
    if (dirty) {
      throw new YTDatabaseException("Cannot call fromStream() on dirty records");
    }

    status = STATUS.UNMARSHALLING;
    try {
      removeAllCollectionChangeListeners();

      fields = null;
      fieldSize = 0;
      contentChanged = false;
      schema = null;

      fetchSchemaIfCan();
      super.fromStream(iRecordBuffer);

      return this;
    } finally {
      status = STATUS.LOADED;
    }
  }

  @Override
  protected final YTDocument fromStream(final byte[] iRecordBuffer, YTDatabaseSessionInternal db) {
    if (dirty) {
      throw new YTDatabaseException("Cannot call fromStream() on dirty records");
    }

    status = STATUS.UNMARSHALLING;
    try {
      removeAllCollectionChangeListeners();

      fields = null;
      fieldSize = 0;
      contentChanged = false;
      schema = null;
      fetchSchemaIfCan(db);
      super.fromStream(iRecordBuffer);

      return this;
    } finally {
      status = STATUS.LOADED;
    }
  }

  /**
   * Returns the forced field type if any.
   *
   * @param iFieldName name of field to check
   */
  public YTType fieldType(final String iFieldName) {
    checkForBinding();
    checkForFields(iFieldName);

    ODocumentEntry entry = fields.get(iFieldName);
    if (entry != null) {
      if (propertyAccess == null || propertyAccess.isReadable(iFieldName)) {
        return entry.type;
      } else {
        return null;
      }
    }

    return null;
  }

  @Override
  public void unload() {
    if (status == ORecordElement.STATUS.NOT_LOADED) {
      return;
    }

    if (dirty) {
      throw new IllegalStateException("Can not unload dirty document");
    }

    internalReset();

    super.unload();
  }

  /**
   * Clears all the field values and types. Clears only record content, but saves its identity.
   *
   * <p>
   *
   * <p>The following code will clear all data from specified document. <code>
   * doc.clear(); doc.save(); </code>
   *
   * @see #reset()
   */
  @Override
  public void clear() {
    checkForBinding();

    super.clear();
    internalReset();
    owner = null;
  }

  /**
   * Resets the record values and class type to being reused. It's like you create a YTDocument from
   * scratch.
   */
  @Override
  public YTDocument reset() {
    checkForBinding();

    var db = ODatabaseRecordThreadLocal.instance().getIfDefined();
    if (db != null && db.getTransaction().isActive()) {
      throw new IllegalStateException(
          "Cannot reset documents during a transaction. Create a new one each time");
    }

    super.reset();

    className = null;
    immutableClazz = null;
    immutableSchemaVersion = -1;

    internalReset();

    owner = null;
    return this;
  }

  /**
   * Rollbacks changes to the loaded version without reloading the document. Works only if tracking
   * changes is enabled @see {@link #isTrackingChanges()} and {@link #setTrackingChanges(boolean)}
   * methods.
   */
  public void undo() {
    if (!trackingChanges) {
      throw new YTConfigurationException(
          "Cannot undo the document because tracking of changes is disabled");
    }

    if (fields != null) {
      final Iterator<Entry<String, ODocumentEntry>> vals = fields.entrySet().iterator();
      while (vals.hasNext()) {
        final Entry<String, ODocumentEntry> next = vals.next();
        final ODocumentEntry val = next.getValue();
        if (val.isCreated()) {
          vals.remove();
        } else {
          val.undo();
        }
      }
      fieldSize = fields.size();
    }
  }

  public YTDocument undo(final String field) {
    if (!trackingChanges) {
      throw new YTConfigurationException(
          "Cannot undo the document because tracking of changes is disabled");
    }

    if (fields != null) {
      final ODocumentEntry value = fields.get(field);
      if (value != null) {
        if (value.isCreated()) {
          fields.remove(field);
        } else {
          value.undo();
        }
      }
    }
    return this;
  }

  public boolean isLazyLoad() {
    checkForBinding();

    return lazyLoad;
  }

  public void setLazyLoad(final boolean iLazyLoad) {
    checkForBinding();

    this.lazyLoad = iLazyLoad;
    checkForFields();
  }

  public boolean isTrackingChanges() {
    return trackingChanges;
  }

  /**
   * Enabled or disabled the tracking of changes in the document. This is needed by some triggers
   * like {@link com.orientechnologies.orient.core.index.OClassIndexManager} to determine what
   * fields are changed to update indexes.
   *
   * @param iTrackingChanges True to enable it, otherwise false
   * @return this
   */
  public YTDocument setTrackingChanges(final boolean iTrackingChanges) {
    checkForBinding();

    this.trackingChanges = iTrackingChanges;
    if (!iTrackingChanges && fields != null) {
      // FREE RESOURCES
      Iterator<Entry<String, ODocumentEntry>> iter = fields.entrySet().iterator();
      while (iter.hasNext()) {
        Entry<String, ODocumentEntry> cur = iter.next();
        if (!cur.getValue().exists()) {
          iter.remove();
        } else {
          cur.getValue().clear();
        }
      }
      removeAllCollectionChangeListeners();
    } else {
      addAllMultiValueChangeListeners();
    }
    return this;
  }

  protected void clearTrackData() {
    if (fields != null) {
      // FREE RESOURCES
      for (Entry<String, ODocumentEntry> cur : fields.entrySet()) {
        if (cur.getValue().exists()) {
          cur.getValue().clear();
          cur.getValue().enableTracking(this);
        } else {
          cur.getValue().clearNotExists();
        }
      }
    }
  }

  void clearTransactionTrackData() {
    if (fields != null) {
      // FREE RESOURCES
      Iterator<Entry<String, ODocumentEntry>> iter = fields.entrySet().iterator();
      while (iter.hasNext()) {
        Entry<String, ODocumentEntry> cur = iter.next();
        if (cur.getValue().exists()) {
          cur.getValue().transactionClear();
        } else {
          iter.remove();
        }
      }
    }
  }

  public boolean isOrdered() {
    return ordered;
  }

  public YTDocument setOrdered(final boolean iOrdered) {
    checkForBinding();

    this.ordered = iOrdered;
    return this;
  }

  @Override
  public boolean equals(Object obj) {
    if (!super.equals(obj)) {
      return false;
    }

    return this == obj || recordId.isValid();
  }

  @Override
  public int hashCode() {
    if (recordId.isValid()) {
      return super.hashCode();
    }

    return System.identityHashCode(this);
  }

  /**
   * Returns the number of fields in memory.
   */
  @Override
  public int fields() {
    checkForBinding();

    checkForFields();
    return fieldSize;
  }

  public boolean isEmpty() {
    checkForBinding();

    checkForFields();
    return fields == null || fields.isEmpty();
  }

  public boolean isEmbedded() {
    return owner != null;
  }

  /**
   * Sets the field type. This overrides the schema property settings if any.
   *
   * @param iFieldName Field name
   * @param iFieldType Type to set between YTType enumeration values
   */
  public YTDocument setFieldType(final String iFieldName, final YTType iFieldType) {
    checkForBinding();

    checkForFields(iFieldName);
    if (iFieldType != null) {
      if (fields == null) {
        fields = ordered ? new LinkedHashMap<>() : new HashMap<>();
      }

      if (iFieldType == YTType.CUSTOM) {
        if (!DB_CUSTOM_SUPPORT.getValueAsBoolean()) {
          throw new YTDatabaseException(
              "YTType CUSTOM used by serializable types is not enabled, set `db.custom.support`"
                  + " to true for enable it");
        }
      }
      // SET THE FORCED TYPE
      ODocumentEntry entry = getOrCreate(iFieldName);
      if (entry.type != iFieldType) {
        field(iFieldName, field(iFieldName), iFieldType);
      }
    } else {
      if (fields != null) {
        // REMOVE THE FIELD TYPE
        ODocumentEntry entry = fields.get(iFieldName);
        if (entry != null)
        // EMPTY: OPTIMIZE IT BY REMOVING THE ENTIRE MAP
        {
          entry.type = null;
        }
      }
    }
    return this;
  }

  /*
   * Initializes the object if has been unserialized
   */
  public boolean deserializeFields(String... iFields) {
    checkForBinding();

    List<String> additional = null;
    if (source == null)
    // ALREADY UNMARSHALLED OR JUST EMPTY
    {
      return true;
    }

    if (iFields != null && iFields.length > 0) {
      // EXTRACT REAL FIELD NAMES
      for (final String f : iFields) {
        if (f != null && !f.startsWith("@")) {
          int pos1 = f.indexOf('[');
          int pos2 = f.indexOf('.');
          if (pos1 > -1 || pos2 > -1) {
            int pos = pos1 > -1 ? pos1 : pos2;
            if (pos2 > -1 && pos2 < pos) {
              pos = pos2;
            }

            // REPLACE THE FIELD NAME
            if (additional == null) {
              additional = new ArrayList<>();
            }
            additional.add(f.substring(0, pos));
          }
        }
      }

      if (additional != null) {
        String[] copy = new String[iFields.length + additional.size()];
        System.arraycopy(iFields, 0, copy, 0, iFields.length);
        int next = iFields.length;
        for (String s : additional) {
          copy[next++] = s;
        }
        iFields = copy;
      }

      // CHECK IF HAS BEEN ALREADY UNMARSHALLED
      if (fields != null && !fields.isEmpty()) {
        boolean allFound = true;
        for (String f : iFields) {
          if (f != null && !f.startsWith("@") && !fields.containsKey(f)) {
            allFound = false;
            break;
          }
        }

        if (allFound)
        // ALL THE REQUESTED FIELDS HAVE BEEN LOADED BEFORE AND AVAILABLE, AVOID UNMARSHALLING
        {
          return true;
        }
      }
    }

    status = ORecordElement.STATUS.UNMARSHALLING;
    try {
      recordFormat.fromStream(getSession(), source, this, iFields);
    } finally {
      status = ORecordElement.STATUS.LOADED;
    }

    if (iFields != null && iFields.length > 0) {
      for (String field : iFields) {
        if (field != null && field.startsWith("@"))
        // ATTRIBUTE
        {
          return true;
        }
      }

      // PARTIAL UNMARSHALLING
      if (fields != null && !fields.isEmpty()) {
        for (String f : iFields) {
          if (f != null && fields.containsKey(f)) {
            return true;
          }
        }
      }

      // NO FIELDS FOUND
      return false;
    } else {
      if (source != null)
      // FULL UNMARSHALLING
      {
        source = null;
      }
    }

    return true;
  }

  /**
   * Change the behavior of field() methods allowing access to the sub documents with dot notation
   * ('.'). Default is true. Set it to false if you allow to store properties with the dot.
   */
  public YTDocument setAllowChainedAccess(final boolean allowChainedAccess) {
    checkForBinding();

    this.allowChainedAccess = allowChainedAccess;
    return this;
  }

  public void setClassNameIfExists(final String iClassName) {
    checkForBinding();

    immutableClazz = null;
    immutableSchemaVersion = -1;

    className = iClassName;

    if (iClassName == null) {
      return;
    }

    final YTClass _clazz = getSession().getMetadata().getImmutableSchemaSnapshot()
        .getClass(iClassName);
    if (_clazz != null) {
      className = _clazz.getName();
      convertFieldsToClass(_clazz);
    }
  }

  @Override
  public YTClass getSchemaClass() {
    checkForBinding();

    if (className == null) {
      fetchClassName();
    }

    if (className == null) {
      return null;
    }

    return getSession().getMetadata().getSchema().getClass(className);
  }

  public String getClassName() {
    if (className == null) {
      fetchClassName();
    }

    return className;
  }

  public void setClassName(final String className) {
    checkForBinding();

    immutableClazz = null;
    immutableSchemaVersion = -1;

    this.className = className;

    if (className == null) {
      return;
    }

    OMetadataInternal metadata = getSession().getMetadata();
    this.immutableClazz =
        (YTImmutableClass) metadata.getImmutableSchemaSnapshot().getClass(className);
    YTClass clazz;
    if (this.immutableClazz != null) {
      clazz = this.immutableClazz;
    } else {
      clazz = metadata.getSchema().getOrCreateClass(className);
    }
    if (clazz != null) {
      this.className = clazz.getName();
      convertFieldsToClass(clazz);
    }
  }

  /**
   * Validates the record following the declared constraints defined in schema such as mandatory,
   * notNull, min, max, regexp, etc. If the schema is not defined for the current class or there are
   * no constraints then the validation is ignored.
   *
   * @throws YTValidationException if the document breaks some validation constraints defined in the
   *                              schema
   * @see YTProperty
   */
  public void validate() throws YTValidationException {
    checkForBinding();

    checkForFields();
    autoConvertValues();

    var session = getSession();

    validateFieldsSecurity(session, this);
    if (!session.isValidationEnabled()) {
      return;
    }

    final YTImmutableClass immutableSchemaClass = getImmutableSchemaClass();
    if (immutableSchemaClass != null) {
      if (immutableSchemaClass.isStrictMode()) {
        // CHECK IF ALL FIELDS ARE DEFINED
        for (String f : fieldNames()) {
          if (immutableSchemaClass.getProperty(f) == null) {
            throw new YTValidationException(
                "Found additional field '"
                    + f
                    + "'. It cannot be added because the schema class '"
                    + immutableSchemaClass.getName()
                    + "' is defined as STRICT");
          }
        }
      }

      final YTImmutableSchema immutableSchema = session.getMetadata().getImmutableSchemaSnapshot();
      for (YTProperty p : immutableSchemaClass.properties(session)) {
        validateField(session, immutableSchema, this, (YTImmutableProperty) p);
      }
    }
  }

  protected String toString(Set<YTRecord> inspected) {
    checkForBinding();

    if (inspected.contains(this)) {
      return "<recursion:rid=" + (recordId != null ? recordId : "null") + ">";
    } else {
      inspected.add(this);
    }

    final boolean saveDirtyStatus = dirty;
    final boolean oldUpdateContent = contentChanged;

    try {
      final StringBuilder buffer = new StringBuilder(128);

      checkForFields();

      var session = getSessionIfDefined();
      if (session != null && !session.isClosed()) {
        final String clsName = getClassName();
        if (clsName != null) {
          buffer.append(clsName);
        }
      }

      if (recordId != null) {
        if (recordId.isValid()) {
          buffer.append(recordId);
        }
      }

      boolean first = true;
      for (Entry<String, ODocumentEntry> f : fields.entrySet()) {
        if (propertyAccess != null && !propertyAccess.isReadable(f.getKey())) {
          continue;
        }
        buffer.append(first ? '{' : ',');
        buffer.append(f.getKey());
        buffer.append(':');
        if (f.getValue().value == null) {
          buffer.append("null");
        } else {
          if (f.getValue().value instanceof Collection<?>
              || f.getValue().value instanceof Map<?, ?>
              || f.getValue().value.getClass().isArray()) {
            buffer.append('[');
            buffer.append(OMultiValue.getSize(f.getValue().value));
            buffer.append(']');
          } else {
            if (f.getValue().value instanceof YTRecord record) {
              if (record.getIdentity().isValid()) {
                record.getIdentity().toString(buffer);
              } else {
                if (record instanceof YTDocument) {
                  buffer.append(((YTDocument) record).toString(inspected));
                } else {
                  buffer.append(record);
                }
              }
            } else {
              buffer.append(f.getValue().value);
            }
          }
        }

        if (first) {
          first = false;
        }
      }
      if (!first) {
        buffer.append('}');
      }

      if (recordId != null && recordId.isValid()) {
        buffer.append(" v");
        buffer.append(recordVersion);
      }

      return buffer.toString();
    } finally {
      dirty = saveDirtyStatus;
      contentChanged = oldUpdateContent;
    }
  }

  private YTDocument mergeMap(
      final Map<String, ODocumentEntry> iOther,
      final boolean iUpdateOnlyMode,
      boolean iMergeSingleItemsOfMultiValueFields) {
    checkForFields();
    source = null;

    for (Entry<String, ODocumentEntry> entry : iOther.entrySet()) {
      String f = entry.getKey();
      ODocumentEntry docEntry = entry.getValue();
      if (!docEntry.exists()) {
        continue;
      }
      final Object otherValue = docEntry.value;

      ODocumentEntry curValue = fields.get(f);

      if (curValue != null && curValue.exists()) {
        final Object value = curValue.value;
        if (iMergeSingleItemsOfMultiValueFields) {
          if (value instanceof Map<?, ?>) {
            final Map<String, Object> map = (Map<String, Object>) value;
            final Map<String, Object> otherMap = (Map<String, Object>) otherValue;

            map.putAll(otherMap);
            continue;
          } else {
            if (OMultiValue.isMultiValue(value) && !(value instanceof ORidBag)) {
              for (Object item : OMultiValue.getMultiValueIterable(otherValue)) {
                if (!OMultiValue.contains(value, item)) {
                  OMultiValue.add(value, item);
                }
              }
              continue;
            }
          }
        }
        boolean bagsMerged = false;
        if (value instanceof ORidBag && otherValue instanceof ORidBag) {
          bagsMerged =
              ((ORidBag) value).tryMerge((ORidBag) otherValue, iMergeSingleItemsOfMultiValueFields);
        }

        if (!bagsMerged && (value != null && !value.equals(otherValue))
            || (value == null && otherValue != null)) {
          setPropertyInternal(f, otherValue);
        }
      } else {
        setPropertyInternal(f, otherValue);
      }
    }

    if (!iUpdateOnlyMode) {
      // REMOVE PROPERTIES NOT FOUND IN OTHER DOC
      for (String f : getPropertyNamesInternal()) {
        if (!iOther.containsKey(f) || !iOther.get(f).exists()) {
          removePropertyInternal(f);
        }
      }
    }

    return this;
  }

  @Override
  protected final YTRecordAbstract fill(
      final YTRID iRid, final int iVersion, final byte[] iBuffer, final boolean iDirty) {
    if (dirty) {
      throw new YTDatabaseException("Cannot call fill() on dirty records");
    }

    schema = null;
    fetchSchemaIfCan();
    return super.fill(iRid, iVersion, iBuffer, iDirty);
  }

  @Override
  protected final YTRecordAbstract fill(
      final YTRID iRid,
      final int iVersion,
      final byte[] iBuffer,
      final boolean iDirty,
      YTDatabaseSessionInternal db) {
    if (dirty) {
      throw new YTDatabaseException("Cannot call fill() on dirty records");
    }

    schema = null;
    fetchSchemaIfCan(db);
    return super.fill(iRid, iVersion, iBuffer, iDirty, db);
  }

  @Override
  protected void clearSource() {
    super.clearSource();
    schema = null;
  }

  protected OGlobalProperty getGlobalPropertyById(int id) {
    checkForBinding();
    var session = getSession();
    if (schema == null) {
      OMetadataInternal metadata = session.getMetadata();
      schema = metadata.getImmutableSchemaSnapshot();
    }
    OGlobalProperty prop = schema.getGlobalPropertyById(id);
    if (prop == null) {
      if (session.isClosed()) {
        throw new YTDatabaseException(
            "Cannot unmarshall the document because no database is active, use detach for use the"
                + " document outside the database session scope");
      }

      OMetadataInternal metadata = session.getMetadata();
      if (metadata.getImmutableSchemaSnapshot() != null) {
        metadata.clearThreadLocalSchemaSnapshot();
      }
      metadata.reload();
      metadata.makeThreadLocalSchemaSnapshot();
      schema = metadata.getImmutableSchemaSnapshot();
      prop = schema.getGlobalPropertyById(id);
    }
    return prop;
  }

  void fillClassIfNeed(final String iClassName) {
    checkForBinding();

    if (this.className == null) {
      immutableClazz = null;
      immutableSchemaVersion = -1;
      className = iClassName;
    }
  }

  protected YTImmutableClass getImmutableSchemaClass() {
    return getImmutableSchemaClass(getSessionIfDefined());
  }

  protected YTImmutableClass getImmutableSchemaClass(@Nullable YTDatabaseSessionInternal database) {
    if (immutableClazz == null) {
      if (className == null) {
        fetchClassName();
      }

      if (className != null) {
        if (database != null && !database.isClosed()) {
          final YTSchema immutableSchema = database.getMetadata().getImmutableSchemaSnapshot();
          if (immutableSchema == null) {
            return null;
          }
          //noinspection deprecation
          immutableSchemaVersion = immutableSchema.getVersion();
          immutableClazz = (YTImmutableClass) immutableSchema.getClass(className);
        }
      }
    }

    return immutableClazz;
  }

  protected void rawField(
      final String iFieldName, final Object iFieldValue, final YTType iFieldType) {
    checkForBinding();

    if (fields == null) {
      fields = ordered ? new LinkedHashMap<>() : new HashMap<>();
    }

    ODocumentEntry entry = getOrCreate(iFieldName);
    entry.disableTracking(this, entry.value);
    entry.value = iFieldValue;
    entry.type = iFieldType;
    entry.enableTracking(this);
    if (iFieldValue instanceof ORidBag) {
      ((ORidBag) iFieldValue).setRecordAndField(recordId, iFieldName);
    }
    if (iFieldValue instanceof YTIdentifiable
        && !((YTIdentifiable) iFieldValue).getIdentity().isPersistent()) {
      track((YTIdentifiable) iFieldValue);
    }
  }

  private ODocumentEntry getOrCreate(String key) {
    ODocumentEntry entry = fields.get(key);
    if (entry == null) {
      entry = new ODocumentEntry();
      fieldSize++;
      fields.put(key, entry);
    }
    return entry;
  }

  boolean rawContainsField(final String iFiledName) {
    checkForBinding();
    return fields != null && fields.containsKey(iFiledName);
  }

  public void autoConvertValues() {
    checkForBinding();

    var session = getSession();
    YTClass clazz = getImmutableSchemaClass();
    if (clazz != null) {
      for (YTProperty prop : clazz.properties(session)) {
        YTType type = prop.getType();
        YTType linkedType = prop.getLinkedType();
        YTClass linkedClass = prop.getLinkedClass();
        if (type == YTType.EMBEDDED && linkedClass != null) {
          convertToEmbeddedType(prop);
          continue;
        }
        if (fields == null) {
          continue;
        }
        final ODocumentEntry entry = fields.get(prop.getName());
        if (entry == null) {
          continue;
        }
        if (!entry.isCreated() && !entry.isChanged()) {
          continue;
        }
        Object value = entry.value;
        if (value == null) {
          continue;
        }
        try {
          if (type == YTType.LINKBAG
              && !(entry.value instanceof ORidBag)
              && entry.value instanceof Collection) {
            ORidBag newValue = new ORidBag(session);
            newValue.setRecordAndField(recordId, prop.getName());
            for (Object o : ((Collection<Object>) entry.value)) {
              if (!(o instanceof YTIdentifiable)) {
                throw new YTValidationException("Invalid value in ridbag: " + o);
              }
              newValue.add((YTIdentifiable) o);
            }
            entry.value = newValue;
          }
          if (type == YTType.LINKMAP) {
            if (entry.value instanceof Map) {
              Map<String, Object> map = (Map<String, Object>) entry.value;
              var newMap = new OMap(this);
              boolean changed = false;
              for (Entry<String, Object> stringObjectEntry : map.entrySet()) {
                Object val = stringObjectEntry.getValue();
                if (OMultiValue.isMultiValue(val) && OMultiValue.getSize(val) == 1) {
                  val = OMultiValue.getFirstValue(val);
                  if (val instanceof OResult) {
                    val = ((OResult) val).getIdentity().orElse(null);
                  }
                  changed = true;
                }
                newMap.put(stringObjectEntry.getKey(), (YTIdentifiable) val);
              }
              if (changed) {
                entry.value = newMap;
              }
            }
          }

          if (linkedType == null) {
            continue;
          }

          if (type == YTType.EMBEDDEDLIST) {
            OTrackedList<Object> list = new OTrackedList<>(this);
            Collection<Object> values = (Collection<Object>) value;
            for (Object object : values) {
              list.add(YTType.convert(session, object, linkedType.getDefaultJavaType()));
            }
            entry.value = list;
            replaceListenerOnAutoconvert(entry);
          } else {
            if (type == YTType.EMBEDDEDMAP) {
              Map<Object, Object> map = new OTrackedMap<>(this);
              Map<Object, Object> values = (Map<Object, Object>) value;
              for (Entry<Object, Object> object : values.entrySet()) {
                map.put(
                    object.getKey(),
                    YTType.convert(session, object.getValue(), linkedType.getDefaultJavaType()));
              }
              entry.value = map;
              replaceListenerOnAutoconvert(entry);
            } else {
              if (type == YTType.EMBEDDEDSET) {
                Set<Object> set = new OTrackedSet<>(this);
                Collection<Object> values = (Collection<Object>) value;
                for (Object object : values) {
                  set.add(YTType.convert(session, object, linkedType.getDefaultJavaType()));
                }
                entry.value = set;
                replaceListenerOnAutoconvert(entry);
              }
            }
          }
        } catch (Exception e) {
          throw YTException.wrapException(
              new YTValidationException(
                  "impossible to convert value of field \"" + prop.getName() + "\""),
              e);
        }
      }
    }
  }

  private void convertToEmbeddedType(YTProperty prop) {
    final ODocumentEntry entry = fields.get(prop.getName());
    YTClass linkedClass = prop.getLinkedClass();
    if (entry == null || linkedClass == null) {
      return;
    }
    if (!entry.isCreated() && !entry.isChanged()) {
      return;
    }
    Object value = entry.value;
    if (value == null) {
      return;
    }
    try {
      if (value instanceof YTDocument) {
        YTClass docClass = ((YTDocument) value).getImmutableSchemaClass();
        if (docClass == null) {
          ((YTDocument) value).setClass(linkedClass);
        } else {
          if (!docClass.isSubClassOf(linkedClass)) {
            throw new YTValidationException(
                "impossible to convert value of field \""
                    + prop.getName()
                    + "\", incompatible with "
                    + linkedClass);
          }
        }
      } else {
        if (value instanceof Map) {
          entry.disableTracking(this, value);
          YTDocument newValue = new YTDocument(linkedClass);
          //noinspection rawtypes
          newValue.fromMap((Map) value);
          entry.value = newValue;
          newValue.addOwner(this);
        } else {
          throw new YTValidationException(
              "impossible to convert value of field \"" + prop.getName() + "\"");
        }
      }

    } catch (Exception e) {
      throw YTException.wrapException(
          new YTValidationException(
              "impossible to convert value of field \"" + prop.getName() + "\""),
          e);
    }
  }

  private void replaceListenerOnAutoconvert(final ODocumentEntry entry) {
    entry.replaceListener(this);
  }

  /**
   * Internal.
   */
  @Override
  protected byte getRecordType() {
    return RECORD_TYPE;
  }

  /**
   * Internal.
   */
  protected void addOwner(final ORecordElement iOwner) {
    checkForBinding();

    if (iOwner == null) {
      return;
    }

    if (recordId.isPersistent()) {
      throw new YTDatabaseException("Cannot add owner to a persistent element");
    }

    if (owner == null) {
      if (dirtyManager != null && this.getIdentity().isNew()) {
        dirtyManager.removeNew(this);
      }
    }
    this.owner = new WeakReference<>(iOwner);
  }

  void removeOwner(final ORecordElement iRecordElement) {
    if (owner != null && owner.get() == iRecordElement) {
      assert !recordId.isPersistent();
      owner = null;
    }
  }

  void convertAllMultiValuesToTrackedVersions() {
    checkForBinding();

    if (fields == null) {
      return;
    }

    var session = getSession();
    for (Map.Entry<String, ODocumentEntry> fieldEntry : fields.entrySet()) {
      ODocumentEntry entry = fieldEntry.getValue();
      final Object fieldValue = entry.value;
      if (fieldValue instanceof ORidBag) {
        if (isEmbedded()) {
          throw new YTDatabaseException("RidBag are supported only at document root");
        }
        ((ORidBag) fieldValue).checkAndConvert();
      }
      if (!(fieldValue instanceof Collection<?>)
          && !(fieldValue instanceof Map<?, ?>)
          && !(fieldValue instanceof YTDocument)) {
        continue;
      }
      if (entry.enableTracking(this)) {
        if (entry.getTimeLine() != null
            && !entry.getTimeLine().getMultiValueChangeEvents().isEmpty()) {
          //noinspection rawtypes
          checkTimelineTrackable(entry.getTimeLine(), (OTrackedMultiValue) entry.value);
        }
        continue;
      }

      if (fieldValue instanceof YTDocument && ((YTDocument) fieldValue).isEmbedded()) {
        ((YTDocument) fieldValue).convertAllMultiValuesToTrackedVersions();
        continue;
      }

      YTType fieldType = entry.type;
      if (fieldType == null) {
        YTClass clazz = getImmutableSchemaClass();
        if (clazz != null) {
          final YTProperty prop = clazz.getProperty(fieldEntry.getKey());
          fieldType = prop != null ? prop.getType() : null;
        }
      }
      if (fieldType == null) {
        fieldType = YTType.getTypeByValue(fieldValue);
      }

      ORecordElement newValue = null;
      switch (fieldType) {
        case EMBEDDEDLIST:
          if (fieldValue instanceof List<?>) {
            newValue = new OTrackedList<>(this);
            fillTrackedCollection(
                (Collection<Object>) newValue, newValue, (Collection<Object>) fieldValue);
          }
          break;
        case EMBEDDEDSET:
          if (fieldValue instanceof Set<?>) {
            newValue = new OTrackedSet<>(this);
            fillTrackedCollection(
                (Collection<Object>) newValue, newValue, (Collection<Object>) fieldValue);
          }
          break;
        case EMBEDDEDMAP:
          if (fieldValue instanceof Map<?, ?>) {
            newValue = new OTrackedMap<>(this);
            fillTrackedMap(
                (Map<Object, Object>) newValue, newValue, (Map<Object, Object>) fieldValue);
          }
          break;
        case LINKLIST:
          if (fieldValue instanceof List<?>) {
            newValue = new OList(this, (Collection<YTIdentifiable>) fieldValue);
          }
          break;
        case LINKSET:
          if (fieldValue instanceof Set<?>) {
            newValue = new OSet(this, (Collection<YTIdentifiable>) fieldValue);
          }
          break;
        case LINKMAP:
          if (fieldValue instanceof Map<?, ?>) {
            newValue = new OMap(this, (Map<Object, YTIdentifiable>) fieldValue);
          }
          break;
        case LINKBAG:
          if (fieldValue instanceof Collection<?>) {
            ORidBag bag = new ORidBag(session);
            bag.setOwner(this);
            bag.setRecordAndField(recordId, fieldEntry.getKey());
            bag.addAll((Collection<YTIdentifiable>) fieldValue);
            newValue = bag;
          }
          break;
        default:
          break;
      }

      if (newValue != null) {
        entry.enableTracking(this);
        entry.value = newValue;
        if (fieldType == YTType.LINKSET || fieldType == YTType.LINKLIST) {
          for (YTIdentifiable rec : (Collection<YTIdentifiable>) newValue) {
            if (rec instanceof YTDocument) {
              ((YTDocument) rec).convertAllMultiValuesToTrackedVersions();
            }
          }
        } else {
          if (fieldType == YTType.LINKMAP) {
            for (YTIdentifiable rec : (Collection<YTIdentifiable>) ((Map<?, ?>) newValue).values()) {
              if (rec instanceof YTDocument) {
                ((YTDocument) rec).convertAllMultiValuesToTrackedVersions();
              }
            }
          }
        }
      }
    }
  }

  private void checkTimelineTrackable(
      OMultiValueChangeTimeLine<Object, Object> timeLine,
      OTrackedMultiValue<Object, Object> origin) {
    List<OMultiValueChangeEvent<Object, Object>> events = timeLine.getMultiValueChangeEvents();
    for (OMultiValueChangeEvent<Object, Object> event : events) {
      Object value = event.getValue();
      if (event.getChangeType() == OMultiValueChangeEvent.OChangeType.ADD
          && !(value instanceof OTrackedMultiValue)) {
        if (value instanceof List) {
          var newCollection = new OTrackedList<>(this);
          fillTrackedCollection(newCollection, newCollection, (Collection<Object>) value);
          origin.replace(event, newCollection);
        } else {
          if (value instanceof Set) {
            var newCollection = new OTrackedSet<>(this);
            fillTrackedCollection(newCollection, newCollection, (Collection<Object>) value);
            origin.replace(event, newCollection);

          } else {
            if (value instanceof Map) {
              OTrackedMap<Object> newMap = new OTrackedMap<>(this);
              fillTrackedMap(newMap, newMap, (Map<Object, Object>) value);
              origin.replace(event, newMap);
            }
          }
        }
      }
    }
  }

  private void fillTrackedCollection(
      Collection<Object> dest, ORecordElement parent, Collection<Object> source) {
    for (Object cur : source) {
      if (cur instanceof YTDocument) {
        ((YTDocument) cur).addOwner((ORecordElement) dest);
        ((YTDocument) cur).convertAllMultiValuesToTrackedVersions();
        ((YTDocument) cur).clearTrackData();
      } else {
        if (cur instanceof List) {
          @SuppressWarnings("rawtypes")
          OTrackedList newList = new OTrackedList<>(parent);
          fillTrackedCollection(newList, newList, (Collection<Object>) cur);
          cur = newList;
        } else {
          if (cur instanceof Set) {
            OTrackedSet<Object> newSet = new OTrackedSet<>(parent);
            fillTrackedCollection(newSet, newSet, (Collection<Object>) cur);
            cur = newSet;
          } else {
            if (cur instanceof Map) {
              OTrackedMap<Object> newMap = new OTrackedMap<>(parent);
              fillTrackedMap(newMap, newMap, (Map<Object, Object>) cur);
              cur = newMap;
            } else {
              if (cur instanceof ORidBag) {
                throw new YTDatabaseException("RidBag are supported only at document root");
              }
            }
          }
        }
      }
      dest.add(cur);
    }
  }

  private void fillTrackedMap(
      Map<Object, Object> dest, ORecordElement parent, Map<Object, Object> source) {
    for (Entry<Object, Object> cur : source.entrySet()) {
      Object value = cur.getValue();
      if (value instanceof YTDocument) {
        ((YTDocument) value).convertAllMultiValuesToTrackedVersions();
        ((YTDocument) value).clearTrackData();
      } else {
        if (cur.getValue() instanceof List) {
          OTrackedList<Object> newList = new OTrackedList<>(parent);
          fillTrackedCollection(newList, newList, (Collection<Object>) value);
          value = newList;
        } else {
          if (value instanceof Set) {
            OTrackedSet<Object> newSet = new OTrackedSet<>(parent);
            fillTrackedCollection(newSet, newSet, (Collection<Object>) value);
            value = newSet;
          } else {
            if (value instanceof Map) {
              OTrackedMap<Object> newMap = new OTrackedMap<>(parent);
              fillTrackedMap(newMap, newMap, (Map<Object, Object>) value);
              value = newMap;
            } else {
              if (value instanceof ORidBag) {
                throw new YTDatabaseException("RidBag are supported only at document root");
              }
            }
          }
        }
      }
      dest.put(cur.getKey(), value);
    }
  }

  private void internalReset() {
    removeAllCollectionChangeListeners();
    if (fields != null) {
      fields.clear();
    }
    fieldSize = 0;
  }

  boolean checkForFields(final String... iFields) {
    if (fields == null) {
      fields = ordered ? new LinkedHashMap<>() : new HashMap<>();
    }

    if (source != null) {
      checkForBinding();

      if (status == ORecordElement.STATUS.LOADED) {
        return deserializeFields(iFields);
      }
    }

    return true;
  }

  Object accessProperty(final String property) {
    checkForBinding();

    if (checkForFields(property)) {
      if (propertyAccess == null || propertyAccess.isReadable(property)) {
        ODocumentEntry entry = fields.get(property);
        if (entry != null) {
          return entry.value;
        } else {
          return null;
        }
      } else {
        return null;
      }
    } else {
      return null;
    }
  }

  /**
   * Internal.
   */
  @Override
  public void setup(YTDatabaseSessionInternal db) {
    super.setup(db);

    if (db != null) {
      recordFormat = db.getSerializer();
    }

    if (recordFormat == null)
    // GET THE DEFAULT ONE
    {
      recordFormat = YTDatabaseSessionAbstract.getDefaultSerializer();
    }

    if (fields != null) {
      var processedRecords = Collections.newSetFromMap(new IdentityHashMap<>());

      for (ODocumentEntry entry : fields.values()) {
        if (entry.value instanceof YTRecordAbstract recordAbstract) {
          if (processedRecords.add(recordAbstract)) {
            recordAbstract.setup(db);
          }
        } else if (entry.value instanceof Collection<?> collection) {
          for (var item : collection) {
            if (item instanceof YTRecordAbstract recordAbstract) {
              if (processedRecords.add(recordAbstract)) {
                recordAbstract.setup(db);
              }
            }
          }
        } else if (entry.value instanceof Map<?, ?> map) {
          for (var item : map.values()) {
            if (item instanceof YTRecordAbstract recordAbstract) {
              if (processedRecords.add(recordAbstract)) {
                recordAbstract.setup(db);
              }
            }
          }
        }
      }
    }
  }

  private static String checkFieldName(final String iFieldName) {
    final Character c = OSchemaShared.checkFieldNameIfValid(iFieldName);
    if (c != null) {
      throw new IllegalArgumentException(
          "Invalid field name '" + iFieldName + "'. Character '" + c + "' is invalid");
    }

    return iFieldName;
  }

  void setClass(final YTClass iClass) {
    checkForBinding();

    if (iClass != null && iClass.isAbstract()) {
      throw new YTSchemaException(
          "Cannot create a document of the abstract class '" + iClass + "'");
    }

    if (iClass == null) {
      className = null;
    } else {
      className = iClass.getName();
    }

    immutableClazz = null;
    immutableSchemaVersion = -1;
    if (iClass != null) {
      convertFieldsToClass(iClass);
    }
  }

  Set<Entry<String, ODocumentEntry>> getRawEntries() {
    checkForBinding();

    checkForFields();
    return fields == null ? new HashSet<>() : fields.entrySet();
  }

  List<Entry<String, ODocumentEntry>> getFilteredEntries() {
    checkForBinding();
    checkForFields();

    if (fields == null) {
      return Collections.emptyList();
    } else {
      if (propertyAccess == null) {
        return fields.entrySet().stream()
            .filter((x) -> x.getValue().exists())
            .collect(Collectors.toList());
      } else {
        return fields.entrySet().stream()
            .filter((x) -> x.getValue().exists() && propertyAccess.isReadable(x.getKey()))
            .collect(Collectors.toList());
      }
    }
  }

  private void fetchSchemaIfCan() {
    if (schema == null) {
      YTDatabaseSessionInternal db = ODatabaseRecordThreadLocal.instance().getIfDefined();
      if (db != null && !db.isClosed()) {
        OMetadataInternal metadata = db.getMetadata();
        schema = metadata.getImmutableSchemaSnapshot();
      }
    }
  }

  private void fetchSchemaIfCan(YTDatabaseSessionInternal db) {
    if (schema == null) {
      if (db != null) {
        OMetadataInternal metadata = db.getMetadata();
        schema = metadata.getImmutableSchemaSnapshot();
      }
    }
  }

  private void fetchClassName() {
    final YTDatabaseSessionInternal database = getSessionIfDefined();

    if (database != null && !database.isClosed()) {
      if (recordId != null) {
        if (recordId.getClusterId() >= 0) {
          final YTSchema schema = database.getMetadata().getImmutableSchemaSnapshot();
          if (schema != null) {
            YTClass clazz = schema.getClassByClusterId(recordId.getClusterId());
            if (clazz != null) {
              className = clazz.getName();
            }
          }
        }
      }
    }
  }

  void autoConvertFieldsToClass(final YTDatabaseSessionInternal database) {
    checkForBinding();

    if (className != null) {
      YTClass klazz = database.getMetadata().getImmutableSchemaSnapshot().getClass(className);
      if (klazz != null) {
        convertFieldsToClass(klazz);
      }
    }
  }

  /**
   * Checks and convert the field of the document matching the types specified by the class.
   */
  private void convertFieldsToClass(final YTClass clazz) {
    var session = getSession();

    for (YTProperty prop : clazz.properties(session)) {
      ODocumentEntry entry = fields != null ? fields.get(prop.getName()) : null;
      if (entry != null && entry.exists()) {
        if (entry.type == null || entry.type != prop.getType()) {
          boolean preChanged = entry.isChanged();
          boolean preCreated = entry.isCreated();
          field(prop.getName(), entry.value, prop.getType());
          if (recordId.isNew()) {
            if (preChanged) {
              entry.markChanged();
            } else {
              entry.unmarkChanged();
            }
            if (preCreated) {
              entry.markCreated();
            } else {
              entry.unmarkCreated();
            }
          }
        }
      } else {
        String defValue = prop.getDefaultValue();
        if (defValue != null && /*defValue.length() > 0 && */ !containsField(prop.getName())) {
          Object curFieldValue = OSQLHelper.parseDefaultValue(session, this, defValue);
          Object fieldValue =
              ODocumentHelper.convertField(session,
                  this, prop.getName(), prop.getType(), null, curFieldValue);
          rawField(prop.getName(), fieldValue, prop.getType());
        }
      }
    }
  }

  private YTType deriveFieldType(String iFieldName, ODocumentEntry entry, YTType[] iFieldType) {
    YTType fieldType;

    if (iFieldType != null && iFieldType.length == 1) {
      entry.type = iFieldType[0];
      fieldType = iFieldType[0];
    } else {
      fieldType = null;
    }

    YTClass clazz = getImmutableSchemaClass();
    if (clazz != null) {
      // SCHEMA-FULL?
      final YTProperty prop = clazz.getProperty(iFieldName);
      if (prop != null) {
        entry.property = prop;
        fieldType = prop.getType();
        if (fieldType != YTType.ANY) {
          entry.type = fieldType;
        }
      }
    }
    return fieldType;
  }

  private void removeAllCollectionChangeListeners() {
    if (fields == null) {
      return;
    }

    for (final Map.Entry<String, ODocumentEntry> field : fields.entrySet()) {
      var docEntry = field.getValue();

      var value = docEntry.value;
      docEntry.disableTracking(this, value);
    }
  }

  private void addAllMultiValueChangeListeners() {
    if (fields == null) {
      return;
    }

    for (final Map.Entry<String, ODocumentEntry> field : fields.entrySet()) {
      field.getValue().enableTracking(this);
    }
  }

  void checkClass(YTDatabaseSessionInternal database) {
    checkForBinding();
    if (className == null) {
      fetchClassName();
    }

    final YTSchema immutableSchema = database.getMetadata().getImmutableSchemaSnapshot();
    if (immutableSchema == null) {
      return;
    }

    if (immutableClazz == null) {
      //noinspection deprecation
      immutableSchemaVersion = immutableSchema.getVersion();
      immutableClazz = (YTImmutableClass) immutableSchema.getClass(className);
    } else {
      //noinspection deprecation
      if (immutableSchemaVersion < immutableSchema.getVersion()) {
        //noinspection deprecation
        immutableSchemaVersion = immutableSchema.getVersion();
        immutableClazz = (YTImmutableClass) immutableSchema.getClass(className);
      }
    }
  }

  @Override
  protected void track(YTIdentifiable id) {
    if (trackingChanges && id.getIdentity().getClusterId() != -2) {
      super.track(id);
    }
  }

  @Override
  protected void unTrack(YTIdentifiable id) {
    if (trackingChanges && id.getIdentity().getClusterId() != -2) {
      super.unTrack(id);
    }
  }

  YTImmutableSchema getImmutableSchema() {
    return schema;
  }

  void checkEmbeddable() {
    if (isVertex() || isEdge()) {
      throw new YTDatabaseException("Vertices or Edges cannot be stored as embedded");
    }
  }
}
