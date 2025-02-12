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
package com.jetbrains.youtrack.db.internal.core.fetch.remote;

import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.exception.FetchException;
import com.jetbrains.youtrack.db.internal.core.fetch.FetchContext;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;

/**
 * Fetch context for {@class ONetworkBinaryProtocol} class
 */
public class RemoteFetchContext implements FetchContext {

  public void onBeforeStandardField(
      Object iFieldValue, String iFieldName, Object iUserObject, PropertyType fieldType) {
  }

  public void onAfterStandardField(
      Object iFieldValue, String iFieldName, Object iUserObject, PropertyType fieldType) {
  }

  public void onBeforeMap(DatabaseSessionInternal db, EntityImpl iRootRecord, String iFieldName,
      final Object iUserObject)
      throws FetchException {
  }

  public void onBeforeFetch(EntityImpl iRootRecord) throws FetchException {
  }

  public void onBeforeArray(
      DatabaseSessionInternal db, EntityImpl iRootRecord, String iFieldName, Object iUserObject,
      Identifiable[] iArray)
      throws FetchException {
  }

  public void onAfterArray(DatabaseSessionInternal db, EntityImpl iRootRecord, String iFieldName,
      Object iUserObject)
      throws FetchException {
  }

  public void onBeforeDocument(
      DatabaseSessionInternal db, EntityImpl iRecord, final EntityImpl entity, String iFieldName,
      final Object iUserObject)
      throws FetchException {
  }

  public void onBeforeCollection(
      DatabaseSessionInternal db, EntityImpl iRootRecord,
      String iFieldName,
      final Object iUserObject,
      final Iterable<?> iterable)
      throws FetchException {
  }

  public void onAfterMap(DatabaseSessionInternal db, EntityImpl iRootRecord, String iFieldName,
      final Object iUserObject)
      throws FetchException {
  }

  public void onAfterFetch(DatabaseSessionInternal db, EntityImpl iRootRecord)
      throws FetchException {
  }

  public void onAfterDocument(
      DatabaseSessionInternal db, EntityImpl iRootRecord, final EntityImpl entity,
      String iFieldName,
      final Object iUserObject)
      throws FetchException {
  }

  public void onAfterCollection(DatabaseSessionInternal db, EntityImpl iRootRecord,
      String iFieldName,
      final Object iUserObject)
      throws FetchException {
  }

  public boolean fetchEmbeddedDocuments() {
    return false;
  }
}
