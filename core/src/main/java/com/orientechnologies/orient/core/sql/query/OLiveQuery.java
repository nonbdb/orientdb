/*
 *
 *  *  Copyright YouTrackDB
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
package com.orientechnologies.orient.core.sql.query;

import com.orientechnologies.common.exception.YTException;
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.orientechnologies.orient.core.db.OLiveQueryMonitor;
import com.orientechnologies.orient.core.db.OLiveQueryResultListener;
import com.orientechnologies.orient.core.db.YTDatabaseSession;
import com.orientechnologies.orient.core.db.YTDatabaseSessionInternal;
import com.orientechnologies.orient.core.db.record.ORecordOperation;
import com.orientechnologies.orient.core.record.YTRecordAbstract;
import com.orientechnologies.orient.core.record.impl.YTDocument;
import com.orientechnologies.orient.core.sql.executor.OResult;
import javax.annotation.Nonnull;

/**
 * SQL live query. <br>
 * <br>
 * The statement syntax is the same as a normal SQL SELECT statement, but with LIVE as prefix: <br>
 * <br>
 * LIVE SELECT FROM Foo WHERE name = 'bar' <br>
 * <br>
 * Executing this query, the caller will subscribe to receive changes happening in the database,
 * that match this query condition. The query returns a query token in the result set. To
 * unsubscribe, the user has to execute another live query with the following syntax: <br>
 * <br>
 * LIVE UNSUBSCRIBE &lt;token&gt; <br>
 * <br>
 * The callback passed as second parameter will be invoked every time a record is
 * created/updated/deleted and it matches the query conditions.
 */
public class OLiveQuery<T> extends OSQLSynchQuery<T> {

  public OLiveQuery() {
  }

  public OLiveQuery(String iText, final OLiveResultListener iResultListener) {
    super(iText);
    setResultListener(new OLocalLiveResultListener(iResultListener));
  }

  @Override
  public <RET> RET execute(@Nonnull YTDatabaseSessionInternal querySession, Object... iArgs) {
    YTDatabaseSessionInternal database = ODatabaseRecordThreadLocal.instance().get();
    if (database.isRemote()) {
      BackwardOLiveQueryResultListener listener = new BackwardOLiveQueryResultListener();
      OLiveQueryMonitor monitor = database.live(getText(), listener, iArgs);
      listener.token = monitor.getMonitorId();
      YTDocument doc = new YTDocument();
      doc.setProperty("token", listener.token);
      OLegacyResultSet<YTDocument> result = new OBasicLegacyResultSet<>();
      result.add(doc);
      return (RET) result;
    }
    return super.execute(querySession, iArgs);
  }

  private class BackwardOLiveQueryResultListener implements OLiveQueryResultListener {

    protected int token;

    @Override
    public void onCreate(YTDatabaseSession database, OResult data) {
      ((OLocalLiveResultListener) getResultListener())
          .onLiveResult(
              token,
              new ORecordOperation((YTRecordAbstract) data.toElement(), ORecordOperation.CREATED));
    }

    @Override
    public void onUpdate(YTDatabaseSession database, OResult before, OResult after) {
      ((OLocalLiveResultListener) getResultListener())
          .onLiveResult(
              token,
              new ORecordOperation((YTRecordAbstract) after.toElement(), ORecordOperation.UPDATED));
    }

    @Override
    public void onDelete(YTDatabaseSession database, OResult data) {
      ((OLocalLiveResultListener) getResultListener())
          .onLiveResult(
              token,
              new ORecordOperation((YTRecordAbstract) data.toElement(), ORecordOperation.DELETED));
    }

    @Override
    public void onError(YTDatabaseSession database, YTException exception) {
      ((OLocalLiveResultListener) getResultListener()).onError(token);
    }

    @Override
    public void onEnd(YTDatabaseSession database) {
      ((OLocalLiveResultListener) getResultListener()).onUnsubscribe(token);
    }
  }
}
