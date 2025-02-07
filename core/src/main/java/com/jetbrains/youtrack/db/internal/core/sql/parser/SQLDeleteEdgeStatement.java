/* Generated By:JJTree: Do not edit this line. SQLDeleteEdgeStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.internal.core.command.BasicCommandContext;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.sql.executor.InternalExecutionPlan;
import com.jetbrains.youtrack.db.internal.core.sql.executor.DeleteEdgeExecutionPlanner;
import com.jetbrains.youtrack.db.internal.core.sql.executor.DeleteExecutionPlan;
import com.jetbrains.youtrack.db.api.query.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class SQLDeleteEdgeStatement extends SQLStatement {

  private static final Object unset = new Object();

  protected SQLIdentifier className;
  protected SQLIdentifier targetClusterName;

  protected SQLRid rid;
  protected List<SQLRid> rids;

  protected SQLExpression leftExpression;
  protected SQLExpression rightExpression;

  protected SQLWhereClause whereClause;

  protected SQLLimit limit;
  protected SQLBatch batch = null;

  public SQLDeleteEdgeStatement(int id) {
    super(id);
  }

  public SQLDeleteEdgeStatement(YouTrackDBSql p, int id) {
    super(p, id);
  }

  @Override
  public ResultSet execute(
      DatabaseSessionInternal db, Map params, CommandContext parentCtx, boolean usePlanCache) {
    BasicCommandContext ctx = new BasicCommandContext();
    if (parentCtx != null) {
      ctx.setParentWithoutOverridingChild(parentCtx);
    }
    ctx.setDatabase(db);
    ctx.setInputParameters(params);
    DeleteExecutionPlan executionPlan;
    if (usePlanCache) {
      executionPlan = (DeleteExecutionPlan) createExecutionPlan(ctx, false);
    } else {
      executionPlan = (DeleteExecutionPlan) createExecutionPlanNoCache(ctx, false);
    }
    executionPlan.executeInternal();
    return new LocalResultSet(executionPlan);
  }

  @Override
  public ResultSet execute(
      DatabaseSessionInternal db, Object[] args, CommandContext parentCtx,
      boolean usePlanCache) {
    Map<Object, Object> params = new HashMap<>();
    if (args != null) {
      for (int i = 0; i < args.length; i++) {
        params.put(i, args[i]);
      }
    }
    return execute(db, params, parentCtx, usePlanCache);
  }

  public InternalExecutionPlan createExecutionPlan(CommandContext ctx, boolean enableProfiling) {
    DeleteEdgeExecutionPlanner planner = new DeleteEdgeExecutionPlanner(this);
    InternalExecutionPlan result = planner.createExecutionPlan(ctx, enableProfiling, true);
    result.setStatement(this.originalStatement);
    result.setGenericStatement(this.toGenericStatement());
    return result;
  }

  public InternalExecutionPlan createExecutionPlanNoCache(
      CommandContext ctx, boolean enableProfiling) {
    DeleteEdgeExecutionPlanner planner = new DeleteEdgeExecutionPlanner(this);
    InternalExecutionPlan result = planner.createExecutionPlan(ctx, enableProfiling, false);
    result.setStatement(this.originalStatement);
    result.setGenericStatement(this.toGenericStatement());
    return result;
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("DELETE EDGE");

    if (className != null) {
      builder.append(" ");
      className.toString(params, builder);
      if (targetClusterName != null) {
        builder.append(" CLUSTER ");
        targetClusterName.toString(params, builder);
      }
    }

    if (rid != null) {
      builder.append(" ");
      rid.toString(params, builder);
    }
    if (rids != null) {
      builder.append(" [");
      boolean first = true;
      for (SQLRid rid : rids) {
        if (!first) {
          builder.append(", ");
        }
        rid.toString(params, builder);
        first = false;
      }
      builder.append("]");
    }
    if (leftExpression != null) {
      builder.append(" FROM ");
      leftExpression.toString(params, builder);
    }
    if (rightExpression != null) {
      builder.append(" TO ");
      rightExpression.toString(params, builder);
    }

    if (whereClause != null) {
      builder.append(" WHERE ");
      whereClause.toString(params, builder);
    }

    if (limit != null) {
      limit.toString(params, builder);
    }
    if (batch != null) {
      batch.toString(params, builder);
    }
  }

  public void toGenericStatement(StringBuilder builder) {
    builder.append("DELETE EDGE");

    if (className != null) {
      builder.append(" ");
      className.toGenericStatement(builder);
      if (targetClusterName != null) {
        builder.append(" CLUSTER ");
        targetClusterName.toGenericStatement(builder);
      }
    }

    if (rid != null) {
      builder.append(" ");
      rid.toGenericStatement(builder);
    }
    if (rids != null) {
      builder.append(" [");
      boolean first = true;
      for (SQLRid rid : rids) {
        if (!first) {
          builder.append(", ");
        }
        rid.toGenericStatement(builder);
        first = false;
      }
      builder.append("]");
    }
    if (leftExpression != null) {
      builder.append(" FROM ");
      leftExpression.toGenericStatement(builder);
    }
    if (rightExpression != null) {
      builder.append(" TO ");
      rightExpression.toGenericStatement(builder);
    }

    if (whereClause != null) {
      builder.append(" WHERE ");
      whereClause.toGenericStatement(builder);
    }

    if (limit != null) {
      limit.toGenericStatement(builder);
    }
    if (batch != null) {
      batch.toGenericStatement(builder);
    }
  }

  @Override
  public SQLDeleteEdgeStatement copy() {
    SQLDeleteEdgeStatement result = null;
    try {
      result = getClass().getConstructor(Integer.TYPE).newInstance(-1);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    result.className = className == null ? null : className.copy();
    result.targetClusterName = targetClusterName == null ? null : targetClusterName.copy();
    result.rid = rid == null ? null : rid.copy();
    result.rids =
        rids == null ? null : rids.stream().map(x -> x.copy()).collect(Collectors.toList());
    result.leftExpression = leftExpression == null ? null : leftExpression.copy();
    result.rightExpression = rightExpression == null ? null : rightExpression.copy();
    result.whereClause = whereClause == null ? null : whereClause.copy();
    result.limit = limit == null ? null : limit.copy();
    result.batch = batch == null ? null : batch.copy();
    return result;
  }

  @Override
  public boolean executinPlanCanBeCached(DatabaseSessionInternal session) {
    if (leftExpression != null && !leftExpression.isCacheable(session)) {
      return false;
    }
    if (rightExpression != null && !rightExpression.isCacheable(session)) {
      return false;
    }

    return whereClause == null || whereClause.isCacheable(session);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SQLDeleteEdgeStatement that = (SQLDeleteEdgeStatement) o;

    if (!Objects.equals(className, that.className)) {
      return false;
    }
    if (!Objects.equals(targetClusterName, that.targetClusterName)) {
      return false;
    }
    if (!Objects.equals(rid, that.rid)) {
      return false;
    }
    if (!Objects.equals(rids, that.rids)) {
      return false;
    }
    if (!Objects.equals(leftExpression, that.leftExpression)) {
      return false;
    }
    if (!Objects.equals(rightExpression, that.rightExpression)) {
      return false;
    }
    if (!Objects.equals(whereClause, that.whereClause)) {
      return false;
    }
    if (!Objects.equals(limit, that.limit)) {
      return false;
    }
    return Objects.equals(batch, that.batch);
  }

  @Override
  public int hashCode() {
    int result = className != null ? className.hashCode() : 0;
    result = 31 * result + (targetClusterName != null ? targetClusterName.hashCode() : 0);
    result = 31 * result + (rid != null ? rid.hashCode() : 0);
    result = 31 * result + (rids != null ? rids.hashCode() : 0);
    result = 31 * result + (leftExpression != null ? leftExpression.hashCode() : 0);
    result = 31 * result + (rightExpression != null ? rightExpression.hashCode() : 0);
    result = 31 * result + (whereClause != null ? whereClause.hashCode() : 0);
    result = 31 * result + (limit != null ? limit.hashCode() : 0);
    result = 31 * result + (batch != null ? batch.hashCode() : 0);
    return result;
  }

  public SQLIdentifier getClassName() {
    return className;
  }

  public void setClassName(SQLIdentifier className) {
    this.className = className;
  }

  public SQLIdentifier getTargetClusterName() {
    return targetClusterName;
  }

  public void setTargetClusterName(SQLIdentifier targetClusterName) {
    this.targetClusterName = targetClusterName;
  }

  public SQLRid getRid() {
    return rid;
  }

  public void setRid(SQLRid rid) {
    this.rid = rid;
  }

  public List<SQLRid> getRids() {
    return rids;
  }

  public void setRids(List<SQLRid> rids) {
    this.rids = rids;
  }

  public void addRid(SQLRid rid) {
    if (this.rids == null) {
      this.rids = new ArrayList<>();
    }
    this.rids.add(rid);
  }

  public SQLWhereClause getWhereClause() {
    return whereClause;
  }

  public void setWhereClause(SQLWhereClause whereClause) {
    this.whereClause = whereClause;
  }

  public SQLLimit getLimit() {
    return limit;
  }

  public void setLimit(SQLLimit limit) {
    this.limit = limit;
  }

  public SQLBatch getBatch() {
    return batch;
  }

  public void setBatch(SQLBatch batch) {
    this.batch = batch;
  }

  public SQLExpression getLeftExpression() {
    return leftExpression;
  }

  public void setLeftExpression(SQLExpression leftExpression) {
    this.leftExpression = leftExpression;
  }

  public SQLExpression getRightExpression() {
    return rightExpression;
  }

  public void setRightExpression(SQLExpression rightExpression) {
    this.rightExpression = rightExpression;
  }
}
/* JavaCC - OriginalChecksum=8f4c5bafa99572d7d87a5d0a2c7d55a7 (do not edit this line) */
