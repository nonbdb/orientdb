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
import com.jetbrains.youtrack.db.internal.common.log.LogManager;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.internal.core.exception.FetchException;
import com.jetbrains.youtrack.db.internal.core.fetch.FetchContext;
import com.jetbrains.youtrack.db.internal.core.fetch.FetchListener;
import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.JSONWriter;
import java.io.IOException;

/**
 *
 */
public class JSONFetchListener implements FetchListener {

  public boolean requireFieldProcessing() {
    return true;
  }

  public void processStandardField(
      final EntityImpl iRecord,
      final Object iFieldValue,
      final String iFieldName,
      final FetchContext iContext,
      final Object iusObject,
      final String iFormat,
      PropertyType filedType) {
    try {
      ((JSONFetchContext) iContext)
          .getJsonWriter()
          .writeAttribute(
              ((JSONFetchContext) iContext).getIndentLevel() + 1,
              true,
              iFieldName,
              iFieldValue,
              iFormat,
              filedType);
    } catch (IOException e) {
      throw BaseException.wrapException(
          new FetchException(
              "Error processing field '" + iFieldValue + " of record " + iRecord.getIdentity()),
          e);
    }
  }

  public void processStandardCollectionValue(final Object iFieldValue, final FetchContext iContext)
      throws FetchException {
    try {
      ((JSONFetchContext) iContext)
          .getJsonWriter()
          .writeValue(
              ((JSONFetchContext) iContext).getIndentLevel(),
              true,
              JSONWriter.encode(iFieldValue));
    } catch (IOException e) {
      LogManager.instance().error(this, "Error on processStandardCollectionValue", e);
    }
  }

  public Object fetchLinked(
      final EntityImpl iRecord,
      final Object iUserObject,
      final String iFieldName,
      final EntityImpl iLinked,
      final FetchContext iContext)
      throws FetchException {
    return iLinked;
  }

  public Object fetchLinkedMapEntry(
      final EntityImpl iRecord,
      final Object iUserObject,
      final String iFieldName,
      final String iKey,
      final EntityImpl iLinked,
      final FetchContext iContext)
      throws FetchException {
    return iLinked;
  }

  public void parseLinked(
      final EntityImpl iRootRecord,
      final Identifiable iLinked,
      final Object iUserObject,
      final String iFieldName,
      final FetchContext iContext)
      throws FetchException {
    try {
      ((JSONFetchContext) iContext).writeLinkedAttribute(iLinked, iFieldName);
    } catch (IOException e) {
      throw BaseException.wrapException(
          new FetchException(
              "Error writing linked field "
                  + iFieldName
                  + " (record:"
                  + iLinked.getIdentity()
                  + ") of record "
                  + iRootRecord.getIdentity()),
          e);
    }
  }

  public void parseLinkedCollectionValue(
      EntityImpl iRootRecord,
      Identifiable iLinked,
      Object iUserObject,
      String iFieldName,
      FetchContext iContext)
      throws FetchException {
    try {
      if (((JSONFetchContext) iContext).isInCollection(iRootRecord)) {
        ((JSONFetchContext) iContext).writeLinkedValue(iLinked, iFieldName);
      } else {
        ((JSONFetchContext) iContext).writeLinkedAttribute(iLinked, iFieldName);
      }
    } catch (IOException e) {
      throw BaseException.wrapException(
          new FetchException(
              "Error writing linked field "
                  + iFieldName
                  + " (record:"
                  + iLinked.getIdentity()
                  + ") of record "
                  + iRootRecord.getIdentity()),
          e);
    }
  }

  public Object fetchLinkedCollectionValue(
      EntityImpl iRoot,
      Object iUserObject,
      String iFieldName,
      EntityImpl iLinked,
      FetchContext iContext)
      throws FetchException {
    return iLinked;
  }

  @Override
  public void skipStandardField(
      EntityImpl iRecord,
      String iFieldName,
      FetchContext iContext,
      Object iUserObject,
      String iFormat)
      throws FetchException {
  }
}
