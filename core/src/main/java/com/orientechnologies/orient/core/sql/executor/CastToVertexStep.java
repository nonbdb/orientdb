package com.orientechnologies.orient.core.sql.executor;

import com.orientechnologies.common.concur.OTimeoutException;
import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.exception.OCommandExecutionException;
import com.orientechnologies.orient.core.record.OVertex;
import com.orientechnologies.orient.core.sql.executor.resultset.OExecutionStream;

/**
 *
 */
public class CastToVertexStep extends AbstractExecutionStep {

  public CastToVertexStep(OCommandContext ctx, boolean profilingEnabled) {
    super(ctx, profilingEnabled);
  }

  @Override
  public OExecutionStream internalStart(OCommandContext ctx) throws OTimeoutException {
    assert prev != null;
    OExecutionStream upstream = prev.start(ctx);
    return upstream.map(this::mapResult);
  }

  private OResult mapResult(OResult result, OCommandContext ctx) {
    if (result.getElement().orElse(null) instanceof OVertex) {
      return result;
    }
    var db = ctx.getDatabase();
    if (result.isVertex()) {
      if (result instanceof OResultInternal) {
        ((OResultInternal) result).setIdentifiable(result.toElement().toVertex());
      } else {
        result = new OResultInternal(db, result.toElement().toVertex());
      }
    } else {
      throw new OCommandExecutionException("Current element is not a vertex: " + result);
    }
    return result;
  }

  @Override
  public String prettyPrint(int depth, int indent) {
    String result = OExecutionStepInternal.getIndent(depth, indent) + "+ CAST TO VERTEX";
    if (profilingEnabled) {
      result += " (" + getCostFormatted() + ")";
    }
    return result;
  }
}
