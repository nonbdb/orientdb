package com.orientechnologies.orient.core.sql.executor;

import com.orientechnologies.common.concur.YTTimeoutException;
import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.sql.executor.resultset.OExecutionStream;

/**
 * takes a result set made of OUpdatableRecord instances and transforms it in another result set
 * made of normal OResultInternal instances.
 *
 * <p>This is the opposite of ConvertToUpdatableResultStep
 */
public class ConvertToResultInternalStep extends AbstractExecutionStep {

  public ConvertToResultInternalStep(OCommandContext ctx, boolean profilingEnabled) {
    super(ctx, profilingEnabled);
  }

  @Override
  public OExecutionStream internalStart(OCommandContext ctx) throws YTTimeoutException {
    if (prev == null) {
      throw new IllegalStateException("filter step requires a previous step");
    }
    OExecutionStream resultSet = prev.start(ctx);
    return resultSet.filter(this::filterMap);
  }

  private OResult filterMap(OResult result, OCommandContext ctx) {
    if (result instanceof OUpdatableResult) {
      var element = result.toElement();
      if (element != null) {
        return new OResultInternal(ctx.getDatabase(), element);
      }
      return result;
    }
    return null;
  }

  @Override
  public String prettyPrint(int depth, int indent) {
    String result =
        OExecutionStepInternal.getIndent(depth, indent) + "+ CONVERT TO REGULAR RESULT ITEM";
    if (profilingEnabled) {
      result += " (" + getCostFormatted() + ")";
    }
    return result;
  }
}
