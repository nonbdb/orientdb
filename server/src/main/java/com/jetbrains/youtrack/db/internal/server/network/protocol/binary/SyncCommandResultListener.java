/*
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
 */

package com.jetbrains.youtrack.db.internal.server.network.protocol.binary;

import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.id.RecordId;
import com.jetbrains.youtrack.db.internal.core.record.RecordAbstract;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.client.remote.FetchPlanResults;
import com.jetbrains.youtrack.db.internal.core.command.CommandResultListener;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.internal.core.exception.FetchException;
import com.jetbrains.youtrack.db.internal.core.fetch.FetchContext;
import com.jetbrains.youtrack.db.internal.core.fetch.FetchHelper;
import com.jetbrains.youtrack.db.internal.core.fetch.remote.RemoteFetchContext;
import com.jetbrains.youtrack.db.internal.core.fetch.remote.RemoteFetchListener;
import com.jetbrains.youtrack.db.api.record.Record;
import java.util.HashSet;
import java.util.Set;

/**
 * Synchronous command result manager.
 */
public class SyncCommandResultListener extends AbstractCommandResultListener
    implements FetchPlanResults {

  private final Set<Record> fetchedRecordsToSend = new HashSet<Record>();
  private final Set<Record> alreadySent = new HashSet<Record>();

  public SyncCommandResultListener(final CommandResultListener wrappedResultListener) {
    super(wrappedResultListener);
  }

  @Override
  public boolean result(DatabaseSessionInternal db, final Object iRecord) {
    if (iRecord instanceof Record) {
      alreadySent.add((Record) iRecord);
      fetchedRecordsToSend.remove(iRecord);
    }

    if (wrappedResultListener != null)
    // NOTIFY THE WRAPPED LISTENER
    {
      wrappedResultListener.result(db, iRecord);
    }

    fetchRecord(db,
        iRecord, new RemoteFetchListener() {
          @Override
          protected void sendRecord(RecordAbstract iLinked) {
            if (!alreadySent.contains(iLinked)) {
              fetchedRecordsToSend.add(iLinked);
            }
          }
        });
    return true;
  }

  public Set<Record> getFetchedRecordsToSend() {
    return fetchedRecordsToSend;
  }

  public boolean isEmpty() {
    return false;
  }

  @Override
  public void linkdedBySimpleValue(DatabaseSessionInternal db, EntityImpl entity) {

    RemoteFetchListener listener =
        new RemoteFetchListener() {
          @Override
          protected void sendRecord(RecordAbstract iLinked) {
            if (!alreadySent.contains(iLinked)) {
              fetchedRecordsToSend.add(iLinked);
            }
          }

          @Override
          public void parseLinked(
              DatabaseSessionInternal db, EntityImpl iRootRecord,
              Identifiable iLinked,
              Object iUserObject,
              String iFieldName,
              FetchContext iContext)
              throws FetchException {
            if (!(iLinked instanceof RecordId)) {
              sendRecord(iLinked.getRecord(db));
            }
          }

          @Override
          public void parseLinkedCollectionValue(
              DatabaseSessionInternal db, EntityImpl iRootRecord,
              Identifiable iLinked,
              Object iUserObject,
              String iFieldName,
              FetchContext iContext)
              throws FetchException {

            if (!(iLinked instanceof RecordId)) {
              sendRecord(iLinked.getRecord(db));
            }
          }
        };
    final FetchContext context = new RemoteFetchContext();
    FetchHelper.fetch(db, entity, entity, FetchHelper.buildFetchPlan(""), listener, context, "");
  }
}
