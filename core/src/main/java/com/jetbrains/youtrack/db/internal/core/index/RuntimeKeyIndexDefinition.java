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
package com.jetbrains.youtrack.db.internal.core.index;

import com.jetbrains.youtrack.db.api.exception.ConfigurationException;
import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.internal.common.serialization.types.BinarySerializer;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.binary.BinarySerializerFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;

/**
 * Index definition that use the serializer specified at run-time not based on type. This is useful
 * to have custom type keys for indexes.
 */
public class RuntimeKeyIndexDefinition<T> extends AbstractIndexDefinition {

  private transient BinarySerializer<T> serializer;

  @SuppressWarnings("unchecked")
  public RuntimeKeyIndexDefinition(final byte iId) {
    super();

    serializer =
        (BinarySerializer<T>) BinarySerializerFactory.getInstance().getObjectSerializer(iId);
    if (serializer == null) {
      throw new ConfigurationException(
          "Runtime index definition cannot find binary serializer with id="
              + iId
              + ". Assure to plug custom serializer into the server.");
    }
  }

  public RuntimeKeyIndexDefinition() {
  }

  public List<String> getFields() {
    return Collections.emptyList();
  }

  public List<String> getFieldsToIndex() {
    return Collections.emptyList();
  }

  public String getClassName() {
    return null;
  }

  public Comparable<?> createValue(DatabaseSessionInternal session, final List<?> params) {
    return (Comparable<?>) refreshRid(session, params.get(0));
  }

  public Comparable<?> createValue(DatabaseSessionInternal session, final Object... params) {
    return createValue(session, Arrays.asList(params));
  }

  public int getParamCount() {
    return 1;
  }

  public PropertyType[] getTypes() {
    return new PropertyType[0];
  }

  @Override
  public @Nonnull EntityImpl toStream(DatabaseSessionInternal db, @Nonnull EntityImpl entity) {
    serializeToStream(db, entity);
    return entity;
  }

  @Override
  protected void serializeToStream(DatabaseSessionInternal db, EntityImpl entity) {
    super.serializeToStream(db, entity);

    entity.setProperty("keySerializerId", serializer.getId());
    entity.setProperty("collate", collate.getName());
    entity.setProperty("nullValuesIgnored", isNullValuesIgnored());
  }

  public void fromStream(@Nonnull EntityImpl entity) {
    serializeFromStream(entity);
  }

  @Override
  protected void serializeFromStream(EntityImpl entity) {
    super.serializeFromStream(entity);

    final var keySerializerId = ((Number) entity.field("keySerializerId")).byteValue();
    //noinspection unchecked
    serializer =
        (BinarySerializer<T>)
            BinarySerializerFactory.getInstance().getObjectSerializer(keySerializerId);
    if (serializer == null) {
      throw new ConfigurationException(
          "Runtime index definition cannot find binary serializer with id="
              + keySerializerId
              + ". Assure to plug custom serializer into the server.");
    }

    setNullValuesIgnored(!Boolean.FALSE.equals(entity.<Boolean>field("nullValuesIgnored")));
  }

  public Object getDocumentValueToIndex(
      DatabaseSessionInternal session, final EntityImpl entity) {
    throw new IndexException("This method is not supported in given index definition.");
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final var that = (RuntimeKeyIndexDefinition<?>) o;
    return serializer.equals(that.serializer);
  }

  @Override
  public int hashCode() {
    var result = super.hashCode();
    result = 31 * result + serializer.getId();
    return result;
  }

  @Override
  public String toString() {
    return "RuntimeKeyIndexDefinition{" + "serializer=" + serializer.getId() + '}';
  }

  /**
   * {@inheritDoc}
   */
  public String toCreateIndexDDL(final String indexName, final String indexType, String engine) {
    return "create index `" + indexName + "` " + indexType + ' ' + "runtime " + serializer.getId();
  }

  public BinarySerializer<T> getSerializer() {
    return serializer;
  }

  @Override
  public boolean isAutomatic() {
    return getClassName() != null;
  }
}
