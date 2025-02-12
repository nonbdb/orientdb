package com.jetbrains.youtrack.db.internal.core.sql.executor;

import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.internal.common.concur.TimeoutException;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.sql.executor.resultset.ExecutionStream;

/**
 * takes a result set made of OUpdatableRecord instances and transforms it in another result set
 * made of normal ResultInternal instances.
 *
 * <p>This is the opposite of ConvertToUpdatableResultStep
 */
public class ConvertToResultInternalStep extends AbstractExecutionStep {

  public ConvertToResultInternalStep(CommandContext ctx, boolean profilingEnabled) {
    super(ctx, profilingEnabled);
  }

  @Override
  public ExecutionStream internalStart(CommandContext ctx) throws TimeoutException {
    if (prev == null) {
      throw new IllegalStateException("filter step requires a previous step");
    }
    var resultSet = prev.start(ctx);
    return resultSet.filter(ConvertToResultInternalStep::filterMap);
  }

  private static Result filterMap(Result result, CommandContext ctx) {
    if (result instanceof UpdatableResult) {
      if (result.isEntity()) {
        var entity = result.asEntity();
        return new ResultInternal(ctx.getDatabaseSession(), entity);
      }
      return result;
    }
    return null;
  }

  @Override
  public String prettyPrint(int depth, int indent) {
    var result =
        ExecutionStepInternal.getIndent(depth, indent) + "+ CONVERT TO REGULAR RESULT ITEM";
    if (profilingEnabled) {
      result += " (" + getCostFormatted() + ")";
    }
    return result;
  }
}
