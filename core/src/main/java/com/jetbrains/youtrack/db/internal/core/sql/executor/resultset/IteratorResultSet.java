package com.jetbrains.youtrack.db.internal.core.sql.executor.resultset;

import com.jetbrains.youtrack.db.api.query.ExecutionPlan;
import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.api.query.ResultSet;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.sql.executor.ResultInternal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 *
 */
public class IteratorResultSet implements ResultSet {

  protected final Iterator iterator;
  @Nullable
  protected final DatabaseSessionInternal session;

  public IteratorResultSet(@Nullable DatabaseSessionInternal session, Iterator iter) {
    this.iterator = iter;
    this.session = session;
  }

  @Override
  public boolean hasNext() {
    assert session == null || session.assertIfNotActive();
    return iterator.hasNext();
  }

  @Override
  public Result next() {
    assert session == null || session.assertIfNotActive();
    var val = iterator.next();
    if (val instanceof Result) {
      return (Result) val;
    }

    ResultInternal result;
    if (val instanceof Identifiable) {
      result = new ResultInternal(session, (Identifiable) val);
    } else {
      result = new ResultInternal(session);
      result.setProperty("value", val);
    }
    return result;
  }

  @Override
  public void close() {
    assert session == null || session.assertIfNotActive();
  }

  @Override
  public Optional<ExecutionPlan> getExecutionPlan() {
    assert session == null || session.assertIfNotActive();
    return Optional.empty();
  }

  @Override
  public Map<String, Long> getQueryStats() {
    assert session == null || session.assertIfNotActive();
    return new HashMap<>();
  }
}
