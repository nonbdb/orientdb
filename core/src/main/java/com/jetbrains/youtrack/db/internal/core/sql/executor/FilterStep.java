package com.jetbrains.youtrack.db.internal.core.sql.executor;

import com.jetbrains.youtrack.db.api.exception.BaseException;
import com.jetbrains.youtrack.db.api.exception.CommandExecutionException;
import com.jetbrains.youtrack.db.api.query.ExecutionStep;
import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.internal.common.concur.TimeoutException;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.sql.executor.resultset.ExecutionStream;
import com.jetbrains.youtrack.db.internal.core.sql.executor.resultset.ExpireResultSet;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLWhereClause;

/**
 *
 */
public class FilterStep extends AbstractExecutionStep {

  private final long timeoutMillis;
  private SQLWhereClause whereClause;

  public FilterStep(
      SQLWhereClause whereClause, CommandContext ctx, long timeoutMillis,
      boolean profilingEnabled) {
    super(ctx, profilingEnabled);
    this.whereClause = whereClause;
    this.timeoutMillis = timeoutMillis;
  }

  @Override
  public ExecutionStream internalStart(CommandContext ctx) throws TimeoutException {
    if (prev == null) {
      throw new IllegalStateException("filter step requires a previous step");
    }

    var resultSet = prev.start(ctx);
    resultSet = resultSet.filter(this::filterMap);
    if (timeoutMillis > 0) {
      resultSet = new ExpireResultSet(resultSet, timeoutMillis, this::sendTimeout);
    }
    return resultSet;
  }

  private Result filterMap(Result result, CommandContext ctx) {
    if (whereClause.matchesFilters(result, ctx)) {
      return result;
    }
    return null;
  }

  @Override
  public String prettyPrint(int depth, int indent) {
    var result = new StringBuilder();
    result.append(ExecutionStepInternal.getIndent(depth, indent)).append("+ FILTER ITEMS WHERE ");
    if (profilingEnabled) {
      result.append(" (").append(getCostFormatted()).append(")");
    }
    result.append("\n");
    result.append(ExecutionStepInternal.getIndent(depth, indent));
    result.append("  ");
    result.append(whereClause.toString());
    return result.toString();
  }

  @Override
  public Result serialize(DatabaseSessionInternal session) {
    var result = ExecutionStepInternal.basicSerialize(session, this);
    if (whereClause != null) {
      result.setProperty("whereClause", whereClause.serialize(session));
    }

    return result;
  }

  @Override
  public void deserialize(Result fromResult, DatabaseSessionInternal session) {
    try {
      ExecutionStepInternal.basicDeserialize(fromResult, this, session);
      whereClause = new SQLWhereClause(-1);
      whereClause.deserialize(fromResult.getProperty("whereClause"));
    } catch (Exception e) {
      throw BaseException.wrapException(new CommandExecutionException(session, ""), e, session);
    }
  }

  @Override
  public boolean canBeCached() {
    return true;
  }

  @Override
  public ExecutionStep copy(CommandContext ctx) {
    return new FilterStep(this.whereClause.copy(), ctx, timeoutMillis, profilingEnabled);
  }
}
