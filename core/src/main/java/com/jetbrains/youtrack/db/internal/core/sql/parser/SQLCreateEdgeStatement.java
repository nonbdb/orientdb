/* Generated By:JJTree: Do not edit this line. SQLCreateEdgeStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.api.query.ResultSet;
import com.jetbrains.youtrack.db.internal.core.command.BasicCommandContext;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.sql.executor.CreateEdgeExecutionPlanner;
import com.jetbrains.youtrack.db.internal.core.sql.executor.InsertExecutionPlan;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SQLCreateEdgeStatement extends SQLStatement {

  protected SQLIdentifier targetClass;
  protected SQLIdentifier targetClusterName;

  protected boolean upsert = false;

  protected SQLExpression leftExpression;

  protected SQLExpression rightExpression;

  protected SQLInsertBody body;
  protected Number retry;
  protected Number wait;
  protected SQLBatch batch;

  public SQLCreateEdgeStatement(int id) {
    super(id);
  }

  public SQLCreateEdgeStatement(YouTrackDBSql p, int id) {
    super(p, id);
  }

  @Override
  public ResultSet execute(
      DatabaseSessionInternal db, Object[] args, CommandContext parentCtx,
      boolean usePlanCache) {
    var ctx = new BasicCommandContext();
    if (parentCtx != null) {
      ctx.setParentWithoutOverridingChild(parentCtx);
    }
    ctx.setDatabase(db);
    Map<Object, Object> params = new HashMap<>();
    if (args != null) {
      for (var i = 0; i < args.length; i++) {
        params.put(i, args[i]);
      }
    }
    ctx.setInputParameters(params);
    InsertExecutionPlan executionPlan;
    if (usePlanCache) {
      executionPlan = createExecutionPlan(ctx, false);
    } else {
      executionPlan = createExecutionPlanNoCache(ctx, false);
    }
    executionPlan.executeInternal();
    return new LocalResultSet(executionPlan);
  }

  @Override
  public ResultSet execute(
      DatabaseSessionInternal db, Map<Object, Object> params, CommandContext parentCtx,
      boolean usePlanCache) {
    var ctx = new BasicCommandContext();
    if (parentCtx != null) {
      ctx.setParentWithoutOverridingChild(parentCtx);
    }
    ctx.setDatabase(db);
    ctx.setInputParameters(params);
    InsertExecutionPlan executionPlan;
    if (usePlanCache) {
      executionPlan = createExecutionPlan(ctx, false);
    } else {
      executionPlan = createExecutionPlanNoCache(ctx, false);
    }
    executionPlan.executeInternal();
    return new LocalResultSet(executionPlan);
  }

  public InsertExecutionPlan createExecutionPlan(CommandContext ctx, boolean enableProfiling) {
    var planner = new CreateEdgeExecutionPlanner(this);
    var result = planner.createExecutionPlan(ctx, enableProfiling, true);
    result.setStatement(this.originalStatement);
    result.setGenericStatement(this.toGenericStatement());
    return result;
  }

  public InsertExecutionPlan createExecutionPlanNoCache(
      CommandContext ctx, boolean enableProfiling) {
    var planner = new CreateEdgeExecutionPlanner(this);
    var result = planner.createExecutionPlan(ctx, enableProfiling, false);
    result.setStatement(this.originalStatement);
    result.setGenericStatement(this.toGenericStatement());
    return result;
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("CREATE EDGE");
    if (targetClass != null) {
      builder.append(" ");
      targetClass.toString(params, builder);
      if (targetClusterName != null) {
        builder.append(" CLUSTER ");
        targetClusterName.toString(params, builder);
      }
    }
    if (upsert) {
      builder.append(" UPSERT");
    }
    builder.append(" FROM ");
    leftExpression.toString(params, builder);

    builder.append(" TO ");
    rightExpression.toString(params, builder);

    if (body != null) {
      builder.append(" ");
      body.toString(params, builder);
    }
    if (retry != null) {
      builder.append(" RETRY ");
      builder.append(retry);
    }
    if (wait != null) {
      builder.append(" WAIT ");
      builder.append(wait);
    }
    if (batch != null) {
      batch.toString(params, builder);
    }
  }

  @Override
  public void toGenericStatement(StringBuilder builder) {
    builder.append("CREATE EDGE");
    if (targetClass != null) {
      builder.append(" ");
      targetClass.toGenericStatement(builder);
      if (targetClusterName != null) {
        builder.append(" CLUSTER ");
        targetClusterName.toGenericStatement(builder);
      }
    }
    if (upsert) {
      builder.append(" UPSERT");
    }
    builder.append(" FROM ");
    leftExpression.toGenericStatement(builder);

    builder.append(" TO ");
    rightExpression.toGenericStatement(builder);

    if (body != null) {
      builder.append(" ");
      body.toGenericStatement(builder);
    }
    if (retry != null) {
      builder.append(" RETRY ");
      builder.append(PARAMETER_PLACEHOLDER);
    }
    if (wait != null) {
      builder.append(" WAIT ");
      builder.append(PARAMETER_PLACEHOLDER);
    }
    if (batch != null) {
      batch.toGenericStatement(builder);
    }
  }

  @Override
  public boolean executinPlanCanBeCached(DatabaseSessionInternal session) {
    if (this.leftExpression != null && !this.leftExpression.isCacheable(session)) {
      return false;
    }
    if (this.rightExpression != null && !this.rightExpression.isCacheable(session)) {
      return false;
    }
    return this.body == null || body.isCacheable(session);
  }

  @Override
  public SQLCreateEdgeStatement copy() {
    SQLCreateEdgeStatement result = null;
    try {
      result = getClass().getConstructor(Integer.TYPE).newInstance(-1);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    result.targetClass = targetClass == null ? null : targetClass.copy();
    result.targetClusterName = targetClusterName == null ? null : targetClusterName.copy();

    result.upsert = this.upsert;

    result.leftExpression = leftExpression == null ? null : leftExpression.copy();

    result.rightExpression = rightExpression == null ? null : rightExpression.copy();

    result.body = body == null ? null : body.copy();
    result.retry = retry;
    result.wait = wait;
    result.batch = batch == null ? null : batch.copy();
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    var that = (SQLCreateEdgeStatement) o;

    if (upsert != that.upsert) {
      return false;
    }
    if (!Objects.equals(targetClass, that.targetClass)) {
      return false;
    }
    if (!Objects.equals(targetClusterName, that.targetClusterName)) {
      return false;
    }
    if (!Objects.equals(leftExpression, that.leftExpression)) {
      return false;
    }
    if (!Objects.equals(rightExpression, that.rightExpression)) {
      return false;
    }
    if (!Objects.equals(body, that.body)) {
      return false;
    }
    if (!Objects.equals(retry, that.retry)) {
      return false;
    }
    if (!Objects.equals(wait, that.wait)) {
      return false;
    }
    return Objects.equals(batch, that.batch);
  }

  @Override
  public int hashCode() {
    var result = targetClass != null ? targetClass.hashCode() : 0;
    result = 31 * result + (targetClusterName != null ? targetClusterName.hashCode() : 0);
    result = 31 * result + (upsert ? 1 : 0);
    result = 31 * result + (leftExpression != null ? leftExpression.hashCode() : 0);
    result = 31 * result + (rightExpression != null ? rightExpression.hashCode() : 0);
    result = 31 * result + (body != null ? body.hashCode() : 0);
    result = 31 * result + (retry != null ? retry.hashCode() : 0);
    result = 31 * result + (wait != null ? wait.hashCode() : 0);
    result = 31 * result + (batch != null ? batch.hashCode() : 0);
    return result;
  }

  public SQLIdentifier getTargetClass() {
    return targetClass;
  }

  public void setTargetClass(SQLIdentifier targetClass) {
    this.targetClass = targetClass;
  }

  public SQLIdentifier getTargetClusterName() {
    return targetClusterName;
  }

  public void setTargetClusterName(SQLIdentifier targetClusterName) {
    this.targetClusterName = targetClusterName;
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

  public SQLInsertBody getBody() {
    return body;
  }

  public void setBody(SQLInsertBody body) {
    this.body = body;
  }

  public Number getRetry() {
    return retry;
  }

  public void setRetry(Number retry) {
    this.retry = retry;
  }

  public Number getWait() {
    return wait;
  }

  public void setWait(Number wait) {
    this.wait = wait;
  }

  public SQLBatch getBatch() {
    return batch;
  }

  public void setBatch(SQLBatch batch) {
    this.batch = batch;
  }

  public boolean isUpsert() {
    return upsert;
  }
}
/* JavaCC - OriginalChecksum=2d3dc5693940ffa520146f8f7f505128 (do not edit this line) */
