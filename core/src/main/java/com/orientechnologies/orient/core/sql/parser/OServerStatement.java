/* Generated By:JJTree: Do not edit this line. OServerStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.orientechnologies.orient.core.sql.parser;

import com.orientechnologies.common.exception.YTException;
import com.orientechnologies.common.listener.OProgressListener;
import com.orientechnologies.orient.core.command.OServerCommandContext;
import com.orientechnologies.orient.core.db.YTDatabaseSessionInternal;
import com.orientechnologies.orient.core.db.YouTrackDBInternal;
import com.orientechnologies.orient.core.exception.YTCommandExecutionException;
import com.orientechnologies.orient.core.record.impl.YTDocument;
import com.orientechnologies.orient.core.sql.YTCommandSQLParsingException;
import com.orientechnologies.orient.core.sql.executor.OInternalExecutionPlan;
import com.orientechnologies.orient.core.sql.executor.OResult;
import com.orientechnologies.orient.core.sql.executor.OResultInternal;
import com.orientechnologies.orient.core.sql.executor.OResultSet;
import com.orientechnologies.orient.core.sql.query.OSQLAsynchQuery;
import java.util.Map;

public class OServerStatement extends SimpleNode {

  public OServerStatement(int id) {
    super(id);
  }

  public OServerStatement(OrientSql p, int id) {
    super(p, id);
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    throw new UnsupportedOperationException(
        "missing implementation in " + getClass().getSimpleName());
  }

  @Override
  public void toGenericStatement(StringBuilder builder) {
    throw new UnsupportedOperationException();
  }

  public void validate() throws YTCommandSQLParsingException {
  }

  @Override
  public String toString(String prefix) {
    StringBuilder builder = new StringBuilder();
    toString(null, builder);
    return builder.toString();
  }

  public Object execute(
      OSQLAsynchQuery<YTDocument> request,
      OServerCommandContext context,
      OProgressListener progressListener) {
    throw new UnsupportedOperationException("Unsupported command: " + getClass().getSimpleName());
  }

  public OResultSet execute(YouTrackDBInternal db, Object[] args) {
    return execute(db, args, true);
  }

  public OResultSet execute(
      YouTrackDBInternal db, Object[] args, OServerCommandContext parentContext) {
    return execute(db, args, parentContext, true);
  }

  public OResultSet execute(YouTrackDBInternal db, Map args) {
    return execute(db, args, true);
  }

  public OResultSet execute(YouTrackDBInternal db, Map args, OServerCommandContext parentContext) {
    return execute(db, args, parentContext, true);
  }

  public OResultSet execute(YouTrackDBInternal db, Object[] args, boolean usePlanCache) {
    return execute(db, args, null, usePlanCache);
  }

  public OResultSet execute(
      YouTrackDBInternal db,
      Object[] args,
      OServerCommandContext parentContext,
      boolean usePlanCache) {
    throw new UnsupportedOperationException();
  }

  public OResultSet execute(YouTrackDBInternal db, Map args, boolean usePlanCache) {
    return execute(db, args, null, usePlanCache);
  }

  public OResultSet execute(
      YouTrackDBInternal db, Map args, OServerCommandContext parentContext, boolean usePlanCache) {
    throw new UnsupportedOperationException();
  }

  /**
   * creates an execution plan for current statement, with profiling disabled
   *
   * @param ctx the context that will be used to execute the statement
   * @return an execution plan
   */
  public OInternalExecutionPlan createExecutionPlan(OServerCommandContext ctx) {
    return createExecutionPlan(ctx, false);
  }

  /**
   * creates an execution plan for current statement
   *
   * @param ctx     the context that will be used to execute the statement
   * @param profile true to enable profiling, false to disable it
   * @return an execution plan
   */
  public OInternalExecutionPlan createExecutionPlan(OServerCommandContext ctx, boolean profile) {
    throw new UnsupportedOperationException();
  }

  public OInternalExecutionPlan createExecutionPlanNoCache(
      OServerCommandContext ctx, boolean profile) {
    return createExecutionPlan(ctx, profile);
  }

  public OServerStatement copy() {
    throw new UnsupportedOperationException("IMPLEMENT copy() ON " + getClass().getSimpleName());
  }

  public boolean refersToParent() {
    throw new UnsupportedOperationException(
        "Implement " + getClass().getSimpleName() + ".refersToParent()");
  }

  public boolean isIdempotent() {
    return false;
  }

  public static OStatement deserializeFromOResult(OResult doc) {
    try {
      OStatement result =
          (OStatement)
              Class.forName(doc.getProperty("__class"))
                  .getConstructor(Integer.class)
                  .newInstance(-1);
      result.deserialize(doc);
    } catch (Exception e) {
      throw YTException.wrapException(new YTCommandExecutionException(""), e);
    }
    return null;
  }

  public OResult serialize(YTDatabaseSessionInternal db) {
    OResultInternal result = new OResultInternal(db);
    result.setProperty("__class", getClass().getName());
    return result;
  }

  public void deserialize(OResult fromResult) {
    throw new UnsupportedOperationException();
  }

  public boolean executinPlanCanBeCached() {
    return false;
  }
}
/* JavaCC - OriginalChecksum=86cab5eeff02ee2a2f8c5e0c0a017e6b (do not edit this line) */
