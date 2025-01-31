package com.jetbrains.youtrack.db.internal.core.sql.executor;

import com.jetbrains.youtrack.db.api.exception.CommandExecutionException;
import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.internal.common.concur.TimeoutException;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.query.live.LiveQueryHookV2;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityInternalUtils;
import com.jetbrains.youtrack.db.internal.core.sql.executor.resultset.ExecutionStream;

/**
 * Reads an upstream result set and returns a new result set that contains copies of the original
 * Result instances
 *
 * <p>This is mainly used from statements that need to copy of the original data before modifying
 * it, eg. UPDATE ... RETURN BEFORE
 */
public class CopyRecordContentBeforeUpdateStep extends AbstractExecutionStep {

  public CopyRecordContentBeforeUpdateStep(CommandContext ctx, boolean profilingEnabled) {
    super(ctx, profilingEnabled);
  }

  @Override
  public ExecutionStream internalStart(CommandContext ctx) throws TimeoutException {
    assert prev != null;
    var lastFetched = prev.start(ctx);
    return lastFetched.map(CopyRecordContentBeforeUpdateStep::mapResult);
  }

  private static Result mapResult(Result result, CommandContext ctx) {
    var db = ctx.getDatabase();
    if (result instanceof UpdatableResult) {
      var prevValue = new ResultInternal(db);
      var rec = result.asEntity();
      prevValue.setProperty("@rid", rec.getIdentity());
      prevValue.setProperty("@version", rec.getVersion());
      if (rec instanceof EntityImpl) {
        prevValue.setProperty(
            "@class", EntityInternalUtils.getImmutableSchemaClass(((EntityImpl) rec)).getName());
      }
      if (!result.asEntity().getIdentity().isNew()) {
        for (var propName : result.getPropertyNames()) {
          prevValue.setProperty(
              propName, LiveQueryHookV2.unboxRidbags(result.getProperty(propName)));
        }
      }
      ((UpdatableResult) result).previousValue = prevValue;
    } else {
      throw new CommandExecutionException("Cannot fetch previous value: " + result);
    }
    return result;
  }

  @Override
  public String prettyPrint(int depth, int indent) {
    var spaces = ExecutionStepInternal.getIndent(depth, indent);
    var result = new StringBuilder();
    result.append(spaces);
    result.append("+ COPY RECORD CONTENT BEFORE UPDATE");
    if (profilingEnabled) {
      result.append(" (").append(getCostFormatted()).append(")");
    }
    return result.toString();
  }
}
