package com.orientechnologies.orient.core.sql.executor;

import com.orientechnologies.common.concur.YTTimeoutException;
import com.orientechnologies.common.exception.YTException;
import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.YTDatabaseSessionInternal;
import com.orientechnologies.orient.core.exception.YTCommandExecutionException;
import com.orientechnologies.orient.core.sql.executor.resultset.OExecutionStream;
import com.orientechnologies.orient.core.sql.executor.resultset.OExpireResultSet;
import com.orientechnologies.orient.core.sql.parser.OWhereClause;

/**
 *
 */
public class FilterStep extends AbstractExecutionStep {

  private final long timeoutMillis;
  private OWhereClause whereClause;

  public FilterStep(
      OWhereClause whereClause, OCommandContext ctx, long timeoutMillis, boolean profilingEnabled) {
    super(ctx, profilingEnabled);
    this.whereClause = whereClause;
    this.timeoutMillis = timeoutMillis;
  }

  @Override
  public OExecutionStream internalStart(OCommandContext ctx) throws YTTimeoutException {
    if (prev == null) {
      throw new IllegalStateException("filter step requires a previous step");
    }

    OExecutionStream resultSet = prev.start(ctx);
    resultSet = resultSet.filter(this::filterMap);
    if (timeoutMillis > 0) {
      resultSet = new OExpireResultSet(resultSet, timeoutMillis, this::sendTimeout);
    }
    return resultSet;
  }

  private OResult filterMap(OResult result, OCommandContext ctx) {
    if (whereClause.matchesFilters(result, ctx)) {
      return result;
    }
    return null;
  }

  @Override
  public String prettyPrint(int depth, int indent) {
    StringBuilder result = new StringBuilder();
    result.append(OExecutionStepInternal.getIndent(depth, indent)).append("+ FILTER ITEMS WHERE ");
    if (profilingEnabled) {
      result.append(" (").append(getCostFormatted()).append(")");
    }
    result.append("\n");
    result.append(OExecutionStepInternal.getIndent(depth, indent));
    result.append("  ");
    result.append(whereClause.toString());
    return result.toString();
  }

  @Override
  public OResult serialize(YTDatabaseSessionInternal db) {
    OResultInternal result = OExecutionStepInternal.basicSerialize(db, this);
    if (whereClause != null) {
      result.setProperty("whereClause", whereClause.serialize(db));
    }

    return result;
  }

  @Override
  public void deserialize(OResult fromResult) {
    try {
      OExecutionStepInternal.basicDeserialize(fromResult, this);
      whereClause = new OWhereClause(-1);
      whereClause.deserialize(fromResult.getProperty("whereClause"));
    } catch (Exception e) {
      throw YTException.wrapException(new YTCommandExecutionException(""), e);
    }
  }

  @Override
  public boolean canBeCached() {
    return true;
  }

  @Override
  public OExecutionStep copy(OCommandContext ctx) {
    return new FilterStep(this.whereClause.copy(), ctx, timeoutMillis, profilingEnabled);
  }
}
