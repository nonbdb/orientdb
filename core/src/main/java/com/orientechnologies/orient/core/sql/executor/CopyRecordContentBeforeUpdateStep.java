package com.orientechnologies.orient.core.sql.executor;

import com.orientechnologies.common.concur.OTimeoutException;
import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.exception.OCommandExecutionException;
import com.orientechnologies.orient.core.query.live.OLiveQueryHookV2;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.record.impl.ODocumentInternal;
import com.orientechnologies.orient.core.sql.executor.resultset.OExecutionStream;

/**
 * Reads an upstream result set and returns a new result set that contains copies of the original
 * OResult instances
 *
 * <p>This is mainly used from statements that need to copy of the original data before modifying
 * it, eg. UPDATE ... RETURN BEFORE
 */
public class CopyRecordContentBeforeUpdateStep extends AbstractExecutionStep {

  public CopyRecordContentBeforeUpdateStep(OCommandContext ctx, boolean profilingEnabled) {
    super(ctx, profilingEnabled);
  }

  @Override
  public OExecutionStream internalStart(OCommandContext ctx) throws OTimeoutException {
    assert prev != null;
    OExecutionStream lastFetched = prev.start(ctx);
    return lastFetched.map(this::mapResult);
  }

  private OResult mapResult(OResult result, OCommandContext ctx) {
    var db = ctx.getDatabase();
    if (result instanceof OUpdatableResult) {
      OResultInternal prevValue = new OResultInternal(db);
      var rec = result.toElement();
      prevValue.setProperty("@rid", rec.getIdentity());
      prevValue.setProperty("@version", rec.getVersion());
      if (rec instanceof ODocument) {
        prevValue.setProperty(
            "@class", ODocumentInternal.getImmutableSchemaClass(((ODocument) rec)).getName());
      }
      if (!result.toElement().getIdentity().isNew()) {
        for (String propName : result.getPropertyNames()) {
          prevValue.setProperty(
              propName, OLiveQueryHookV2.unboxRidbags(result.getProperty(propName)));
        }
      }
      ((OUpdatableResult) result).previousValue = prevValue;
    } else {
      throw new OCommandExecutionException("Cannot fetch previous value: " + result);
    }
    return result;
  }

  @Override
  public String prettyPrint(int depth, int indent) {
    String spaces = OExecutionStepInternal.getIndent(depth, indent);
    StringBuilder result = new StringBuilder();
    result.append(spaces);
    result.append("+ COPY RECORD CONTENT BEFORE UPDATE");
    if (profilingEnabled) {
      result.append(" (").append(getCostFormatted()).append(")");
    }
    return result.toString();
  }
}
