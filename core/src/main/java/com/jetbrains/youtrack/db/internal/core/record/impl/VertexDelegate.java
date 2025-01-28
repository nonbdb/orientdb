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
package com.jetbrains.youtrack.db.internal.core.record.impl;

import com.jetbrains.youtrack.db.api.DatabaseSession;
import com.jetbrains.youtrack.db.api.record.Edge;
import com.jetbrains.youtrack.db.api.record.Entity;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.api.record.RID;
import com.jetbrains.youtrack.db.api.record.Vertex;
import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 *
 */
public final class VertexDelegate implements VertexInternal {

  private final EntityImpl entity;


  public VertexDelegate(EntityImpl entry) {
    this.entity = entry;
  }

  @Override
  public void delete() {
    entity.delete();
  }

  public void resetToNew() {
    entity.resetToNew();
  }

  @Override
  public Optional<Vertex> asVertex() {
    return Optional.of(this);
  }

  @Nonnull
  @Override
  public Vertex toVertex() {
    return this;
  }

  @Override
  public Optional<Edge> asEdge() {
    return Optional.empty();
  }

  @Nullable
  @Override
  public Edge toEdge() {
    return null;
  }

  @Override
  public boolean isVertex() {
    return true;
  }

  @Override
  public boolean isEdge() {
    return false;
  }

  @Override
  public Optional<SchemaClass> getSchemaType() {
    return entity.getSchemaType();
  }

  @Nullable
  @Override
  public SchemaClass getSchemaClass() {
    return entity.getSchemaClass();
  }

  @Nonnull
  @SuppressWarnings("unchecked")
  @Override
  public EntityImpl getRecord(DatabaseSession db) {
    return entity;
  }

  @Override
  public int compareTo(Identifiable o) {
    return entity.compareTo(o);
  }

  @Override
  public int compare(Identifiable o1, Identifiable o2) {
    return entity.compare(o1, o2);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Identifiable)) {
      return false;
    }
    if (!(obj instanceof Entity)) {
      obj = ((Identifiable) obj).getRecordSilently(entity.getSession());
    }

    if (obj == null) {
      return false;
    }

    return entity.equals(((Entity) obj).getRecordSilently(entity.getSession()));
  }

  @Override
  public int hashCode() {
    return entity.hashCode();
  }

  @Override
  public void clear() {
    entity.clear();
  }

  @Override
  public VertexDelegate copy() {
    return new VertexDelegate(entity.copy());
  }

  @Override
  public boolean isEmbedded() {
    return false;
  }

  @Override
  public RID getIdentity() {
    return entity.getIdentity();
  }

  @Override
  public int getVersion() {
    return entity.getVersion();
  }

  @Override
  public boolean isDirty() {
    return entity.isDirty();
  }

  @Override
  public void save() {
    entity.save();
  }

  @Override
  public void updateFromJSON(@Nonnull String iJson) {
    entity.updateFromJSON(iJson);
  }

  @Override
  public String toJSON() {
    return entity.toJSON();
  }

  @Override
  public String toJSON(String iFormat) {
    return entity.toJSON(iFormat);
  }

  @Override
  public String toString() {
    if (entity != null) {
      return entity.toString();
    }
    return super.toString();
  }

  @Nonnull
  @Override
  public EntityImpl getBaseEntity() {
    return entity;
  }

  @Override
  public void updateFromMap(Map<String, ?> map) {
    entity.updateFromMap(map);
  }

  @Override
  public Map<String, Object> toMap() {
    return entity.toMap();
  }

  @Override
  public Map<String, Object> toMap(boolean includeMetadata) {
    return entity.toMap(includeMetadata);
  }
}
