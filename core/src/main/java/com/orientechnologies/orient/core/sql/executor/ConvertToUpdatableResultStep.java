package com.orientechnologies.orient.core.sql.executor;

import com.orientechnologies.common.concur.OTimeoutException;
import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.sql.executor.resultset.OExecutionStream;

/**
 * takes a normal result set and transforms it in another result set made of OUpdatableRecord
 * instances. Records that are not identifiable are discarded.
 *
 * <p>This is the opposite of ConvertToResultInternalStep
 */
public class ConvertToUpdatableResultStep extends AbstractExecutionStep {

  public ConvertToUpdatableResultStep(OCommandContext ctx, boolean profilingEnabled) {
    super(ctx, profilingEnabled);
  }

  @Override
  public OExecutionStream internalStart(OCommandContext ctx) throws OTimeoutException {
    if (prev == null) {
      throw new IllegalStateException("filter step requires a previous step");
    }
    OExecutionStream resultSet = prev.start(ctx);
    return resultSet.filter(this::filterMap);
  }

  private OResult filterMap(OResult result, OCommandContext ctx) {
    if (result instanceof OUpdatableResult) {
      return result;
    }
    if (result.isElement()) {
      var element = result.toElement();
      if (element != null) {
        return new OUpdatableResult(ctx.getDatabase(), element);
      }
      return result;
    }
    return null;
  }

  @Override
  public String prettyPrint(int depth, int indent) {
    String result = OExecutionStepInternal.getIndent(depth, indent) + "+ CONVERT TO UPDATABLE ITEM";
    if (profilingEnabled) {
      result += " (" + getCostFormatted() + ")";
    }
    return result;
  }
}
