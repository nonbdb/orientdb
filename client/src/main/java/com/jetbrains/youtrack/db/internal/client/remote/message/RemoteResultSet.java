package com.jetbrains.youtrack.db.internal.client.remote.message;

import com.jetbrains.youtrack.db.api.query.ExecutionPlan;
import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.api.query.ResultSet;
import com.jetbrains.youtrack.db.api.record.DBRecord;
import com.jetbrains.youtrack.db.internal.client.remote.db.DatabaseSessionRemote;
import com.jetbrains.youtrack.db.internal.core.db.QueryDatabaseState;
import com.jetbrains.youtrack.db.internal.core.sql.executor.ResultInternal;
import com.jetbrains.youtrack.db.internal.core.tx.FrontendTransactionAbstract;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

public class RemoteResultSet implements ResultSet {

  @Nullable
  private final DatabaseSessionRemote session;
  private final String queryId;
  private List<Result> currentPage;
  private ExecutionPlan executionPlan;
  private Map<String, Long> queryStats;
  private boolean hasNextPage;

  public RemoteResultSet(
      @Nullable DatabaseSessionRemote session,
      String queryId,
      List<Result> currentPage,
      ExecutionPlan executionPlan,
      Map<String, Long> queryStats,
      boolean hasNextPage) {
    this.session = session;
    this.queryId = queryId;
    this.currentPage = currentPage;
    this.executionPlan = executionPlan;
    this.queryStats = queryStats;
    this.hasNextPage = hasNextPage;
    if (session != null) {
      session.queryStarted(queryId, new QueryDatabaseState(this));
      for (var result : currentPage) {
        if (result instanceof ResultInternal) {
          ((ResultInternal) result).bindToCache(session);
        }
      }
    }
  }

  @Override
  public boolean hasNext() {
    assert session == null || session.assertIfNotActive();
    if (!currentPage.isEmpty()) {
      return true;
    }
    if (!hasNextPage()) {
      return false;
    }
    fetchNextPage();
    return !currentPage.isEmpty();
  }

  private void fetchNextPage() {
    assert session == null || session.assertIfNotActive();
    if (session != null) {
      session.fetchNextPage(this);
    }
  }

  @Override
  public Result next() {
    assert session == null || session.assertIfNotActive();
    if (currentPage.isEmpty()) {
      if (!hasNextPage()) {
        throw new IllegalStateException();
      }
      fetchNextPage();
    }
    if (currentPage.isEmpty()) {
      throw new IllegalStateException();
    }
    var internal = currentPage.removeFirst();

    if (internal.isRecord() && session != null && session.getTransaction().isActive()) {
      DBRecord record = session.getTransaction().getRecord(internal.getIdentity());
      if (record != null && record != FrontendTransactionAbstract.DELETED_RECORD) {
        internal = new ResultInternal(session, record);
      }
    }
    return internal;
  }

  @Override
  public void close() {
    assert session == null || session.assertIfNotActive();
    if (hasNextPage && session != null) {
      // CLOSES THE QUERY SERVER SIDE ONLY IF THERE IS ANOTHER PAGE. THE SERVER ALREADY
      // AUTOMATICALLY CLOSES THE QUERY AFTER SENDING THE LAST PAGE
      session.closeQuery(queryId);
    }
  }

  @Override
  public ExecutionPlan getExecutionPlan() {
    assert session == null || session.assertIfNotActive();
    return executionPlan;
  }

  @Override
  public Map<String, Long> getQueryStats() {
    assert session == null || session.assertIfNotActive();
    return queryStats;
  }

  public void add(ResultInternal item) {
    assert session == null || session.assertIfNotActive();
    currentPage.add(item);
  }

  public boolean hasNextPage() {
    assert session == null || session.assertIfNotActive();
    return hasNextPage;
  }

  public String getQueryId() {
    assert session == null || session.assertIfNotActive();
    return queryId;
  }

  public void fetched(
      List<Result> result,
      boolean hasNextPage,
      ExecutionPlan executionPlan,
      Map<String, Long> queryStats) {
    assert session == null || session.assertIfNotActive();
    this.currentPage = result;
    this.hasNextPage = hasNextPage;

    if (queryStats != null) {
      this.queryStats = queryStats;
    }
    if (executionPlan != null) {
      this.executionPlan = executionPlan;
    }
  }
}
