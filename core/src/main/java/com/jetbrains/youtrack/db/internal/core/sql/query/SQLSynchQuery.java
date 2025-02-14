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
package com.jetbrains.youtrack.db.internal.core.sql.query;

import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.api.record.RID;
import com.jetbrains.youtrack.db.internal.core.command.CommandResultListener;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseRecordThreadLocal;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.id.RecordId;
import com.jetbrains.youtrack.db.internal.core.serialization.MemoryStream;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.RecordSerializer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * SQL synchronous query. When executed the caller wait for the result.
 *
 * @param <T>
 * @see SQLAsynchQuery
 */
@SuppressWarnings({"unchecked", "serial"})
public class SQLSynchQuery<T extends Object> extends SQLAsynchQuery<T>
    implements CommandResultListener, Iterable<T> {

  private final LegacyResultSet<T> result = new ConcurrentLegacyResultSet<T>();
  private RID nextPageRID;
  private Map<Object, Object> previousQueryParams = new HashMap<Object, Object>();

  public SQLSynchQuery() {
    resultListener = this;
  }

  public SQLSynchQuery(final String iText) {
    super(iText);
    resultListener = this;
  }

  public SQLSynchQuery(final String iText, final int iLimit) {
    super(iText, iLimit, null);
    resultListener = this;
  }

  @Override
  public void reset() {
    result.clear();
  }

  public boolean result(DatabaseSessionInternal querySession, final Object iRecord) {
    if (iRecord != null) {
      result.add((T) iRecord);
    }
    return true;
  }

  @Override
  public void end() {
    result.setCompleted();
  }

  @Override
  public List<T> run(final Object... iArgs) {
    result.clear();

    final Map<Object, Object> queryParams;
    queryParams = fetchQueryParams(iArgs);
    resetNextRIDIfParametersWereChanged(queryParams);

    final List<Object> res = (List<Object>) super.run(iArgs);

    if (res != result && res != null && result.isEmptyNoWait()) {
      Iterator<Object> iter = res.iterator();
      while (iter.hasNext()) {
        Object item = iter.next();
        result.add((T) item);
      }
    }

    ((LegacyResultSet) result).setCompleted();

    if (!result.isEmpty()) {
      previousQueryParams = new HashMap<>(queryParams);
      final RecordId lastRid = (RecordId) ((Identifiable) result.get(
          result.size() - 1)).getIdentity();
      nextPageRID = new RecordId(lastRid.next());
    }

    return result;
  }

  @Override
  public boolean isIdempotent() {
    return true;
  }

  public Object getResult() {
    return result;
  }

  /**
   * @return RID of the record that will be processed first during pagination mode.
   */
  public RID getNextPageRID() {
    return nextPageRID;
  }

  public void resetPagination() {
    nextPageRID = null;
  }

  public Iterator<T> iterator() {
    execute(DatabaseRecordThreadLocal.instance().get());
    return ((Iterable<T>) getResult()).iterator();
  }

  @Override
  public boolean isAsynchronous() {
    return false;
  }

  @Override
  protected MemoryStream queryToStream() {
    final MemoryStream buffer = super.queryToStream();

    buffer.setUtf8(nextPageRID != null ? nextPageRID.toString() : "");

    final byte[] queryParams = serializeQueryParameters(previousQueryParams);
    buffer.set(queryParams);

    return buffer;
  }

  @Override
  protected void queryFromStream(DatabaseSessionInternal db, final MemoryStream buffer,
      RecordSerializer serializer) {
    super.queryFromStream(db, buffer, serializer);

    final String rid = buffer.getAsString();
    if ("".equals(rid)) {
      nextPageRID = null;
    } else {
      nextPageRID = new RecordId(rid);
    }

    final byte[] serializedPrevParams = buffer.getAsByteArray();
    previousQueryParams = deserializeQueryParameters(db, serializedPrevParams, serializer);
  }

  private void resetNextRIDIfParametersWereChanged(final Map<Object, Object> queryParams) {
    if (!queryParams.equals(previousQueryParams)) {
      nextPageRID = null;
    }
  }

  private Map<Object, Object> fetchQueryParams(final Object... iArgs) {
    if (iArgs != null && iArgs.length > 0) {
      return convertToParameters(iArgs);
    }

    Map<Object, Object> queryParams = getParameters();
    if (queryParams == null) {
      queryParams = new HashMap<Object, Object>();
    }
    return queryParams;
  }
}
