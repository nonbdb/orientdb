package com.jetbrains.youtrack.db.internal.core.sql.executor;

import com.jetbrains.youtrack.db.api.query.ExecutionPlan;
import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.api.query.ResultSet;
import com.jetbrains.youtrack.db.internal.common.util.Resettable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 *
 */
public class InternalResultSet implements ResultSet, Resettable {

  private List<Result> content = new ArrayList<>();
  private int next = 0;
  protected ExecutionPlan plan;

  @Override
  public boolean hasNext() {
    return content.size() > next;
  }

  @Override
  public Result next() {
    return content.get(next++);
  }

  @Override
  public void close() {
    this.content.clear();
  }

  @Override
  public Optional<ExecutionPlan> getExecutionPlan() {
    return Optional.ofNullable(plan);
  }

  public void setPlan(ExecutionPlan plan) {
    this.plan = plan;
  }

  @Override
  public Map<String, Long> getQueryStats() {
    return new HashMap<>();
  }

  public void add(Result nextResult) {
    content.add(nextResult);
  }

  public void reset() {
    this.next = 0;
  }

  public int size() {
    return content.size();
  }

  public InternalResultSet copy() {
    var result = new InternalResultSet();
    result.content = this.content;
    return result;
  }
}
