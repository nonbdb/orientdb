/* Generated By:JJTree: Do not edit this line. SQLUpdateStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.api.exception.DatabaseException;
import com.jetbrains.youtrack.db.api.query.ResultSet;
import com.jetbrains.youtrack.db.internal.core.command.BasicCommandContext;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.sql.executor.UpdateExecutionPlan;
import com.jetbrains.youtrack.db.internal.core.sql.executor.UpdateExecutionPlanner;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class SQLUpdateStatement extends SQLStatement {

  public SQLFromClause target;

  protected List<SQLUpdateOperations> operations = new ArrayList<SQLUpdateOperations>();

  protected boolean upsert = false;

  protected boolean returnBefore = false;
  protected boolean returnAfter = false;
  protected boolean returnCount = false;
  protected SQLProjection returnProjection;

  public SQLWhereClause whereClause;

  public SQLLimit limit;
  public SQLTimeout timeout;

  public SQLUpdateStatement(int id) {
    super(id);
  }

  public SQLUpdateStatement(YouTrackDBSql p, int id) {
    super(p, id);
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append(getStatementType());
    if (target != null) {
      target.toString(params, builder);
    }

    for (SQLUpdateOperations ops : this.operations) {
      builder.append(" ");
      ops.toString(params, builder);
    }

    if (upsert) {
      builder.append(" UPSERT");
    }

    if (returnBefore || returnAfter || returnCount) {
      builder.append(" RETURN");
      if (returnBefore) {
        throw new DatabaseException("BEFORE is not supported");
      } else if (returnAfter) {
        builder.append(" AFTER");
      } else {
        builder.append(" COUNT");
      }
      if (returnProjection != null) {
        builder.append(" ");
        returnProjection.toString(params, builder);
      }
    }
    if (whereClause != null) {
      builder.append(" WHERE ");
      whereClause.toString(params, builder);
    }

    if (limit != null) {
      limit.toString(params, builder);
    }
    if (timeout != null) {
      timeout.toString(params, builder);
    }
  }

  public void toGenericStatement(StringBuilder builder) {
    builder.append(getStatementType());
    if (target != null) {
      target.toGenericStatement(builder);
    }

    for (SQLUpdateOperations ops : this.operations) {
      builder.append(" ");
      ops.toGenericStatement(builder);
    }

    if (upsert) {
      builder.append(" UPSERT");
    }

    if (returnBefore || returnAfter || returnCount) {
      builder.append(" RETURN");
      if (returnBefore) {
        throw new DatabaseException("BEFORE is not supported");
      } else if (returnAfter) {
        builder.append(" AFTER");
      } else {
        builder.append(" COUNT");
      }
      if (returnProjection != null) {
        builder.append(" ");
        returnProjection.toGenericStatement(builder);
      }
    }
    if (whereClause != null) {
      builder.append(" WHERE ");
      whereClause.toGenericStatement(builder);
    }

    if (limit != null) {
      limit.toGenericStatement(builder);
    }
    if (timeout != null) {
      timeout.toGenericStatement(builder);
    }
  }

  protected String getStatementType() {
    return "UPDATE ";
  }

  @Override
  public SQLUpdateStatement copy() {
    SQLUpdateStatement result = null;
    try {
      result = getClass().getConstructor(Integer.TYPE).newInstance(-1);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    result.target = target == null ? null : target.copy();
    result.operations =
        operations == null
            ? null
            : operations.stream().map(x -> x.copy()).collect(Collectors.toList());
    result.upsert = upsert;
    result.returnBefore = returnBefore;
    result.returnAfter = returnAfter;
    result.returnProjection = returnProjection == null ? null : returnProjection.copy();
    result.whereClause = whereClause == null ? null : whereClause.copy();
    result.limit = limit == null ? null : limit.copy();
    result.timeout = timeout == null ? null : timeout.copy();
    return result;
  }

  @Override
  public ResultSet execute(
      DatabaseSessionInternal db, Object[] args, CommandContext parentCtx,
      boolean usePlanCache) {
    BasicCommandContext ctx = new BasicCommandContext();
    if (parentCtx != null) {
      ctx.setParentWithoutOverridingChild(parentCtx);
    }
    ctx.setDatabase(db);
    Map<Object, Object> params = new HashMap<>();
    if (args != null) {
      for (int i = 0; i < args.length; i++) {
        params.put(i, args[i]);
      }
    }
    ctx.setInputParameters(params);
    UpdateExecutionPlan executionPlan;
    if (usePlanCache) {
      executionPlan = createExecutionPlan(ctx, false);
    } else {
      executionPlan = (UpdateExecutionPlan) createExecutionPlanNoCache(ctx, false);
    }
    executionPlan.executeInternal();
    return new LocalResultSet(executionPlan);
  }

  @Override
  public ResultSet execute(
      DatabaseSessionInternal db, Map<Object, Object> params, CommandContext parentCtx,
      boolean usePlanCache) {
    BasicCommandContext ctx = new BasicCommandContext();
    if (parentCtx != null) {
      ctx.setParentWithoutOverridingChild(parentCtx);
    }
    ctx.setDatabase(db);
    ctx.setInputParameters(params);
    UpdateExecutionPlan executionPlan;
    if (usePlanCache) {
      executionPlan = createExecutionPlan(ctx, false);
    } else {
      executionPlan = (UpdateExecutionPlan) createExecutionPlanNoCache(ctx, false);
    }
    executionPlan.executeInternal();
    return new LocalResultSet(executionPlan);
  }

  public UpdateExecutionPlan createExecutionPlan(CommandContext ctx, boolean enableProfiling) {
    UpdateExecutionPlanner planner = new UpdateExecutionPlanner(this);
    UpdateExecutionPlan result = planner.createExecutionPlan(ctx, enableProfiling);
    result.setStatement(this.originalStatement);
    result.setGenericStatement(this.toGenericStatement());
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

    SQLUpdateStatement that = (SQLUpdateStatement) o;

    if (upsert != that.upsert) {
      return false;
    }
    if (returnBefore != that.returnBefore) {
      return false;
    }
    if (returnAfter != that.returnAfter) {
      return false;
    }
    if (!Objects.equals(target, that.target)) {
      return false;
    }
    if (!Objects.equals(operations, that.operations)) {
      return false;
    }
    if (!Objects.equals(returnProjection, that.returnProjection)) {
      return false;
    }
    if (!Objects.equals(whereClause, that.whereClause)) {
      return false;
    }
    if (!Objects.equals(limit, that.limit)) {
      return false;
    }
    return Objects.equals(timeout, that.timeout);
  }

  @Override
  public int hashCode() {
    int result = target != null ? target.hashCode() : 0;
    result = 31 * result + (operations != null ? operations.hashCode() : 0);
    result = 31 * result + (upsert ? 1 : 0);
    result = 31 * result + (returnBefore ? 1 : 0);
    result = 31 * result + (returnAfter ? 1 : 0);
    result = 31 * result + (returnProjection != null ? returnProjection.hashCode() : 0);
    result = 31 * result + (whereClause != null ? whereClause.hashCode() : 0);
    result = 31 * result + (limit != null ? limit.hashCode() : 0);
    result = 31 * result + (timeout != null ? timeout.hashCode() : 0);
    return result;
  }

  public SQLFromClause getTarget() {
    return target;
  }

  public List<SQLUpdateOperations> getOperations() {
    return operations;
  }

  public void addOperations(SQLUpdateOperations op) {
    this.operations.add(op);
  }

  public boolean isUpsert() {
    return upsert;
  }

  public boolean isReturnBefore() {
    if (returnBefore) {
      throw new DatabaseException("BEFORE is not supported");
    }

    return false;
  }

  public boolean isReturnAfter() {
    return returnAfter;
  }

  public boolean isReturnCount() {
    return returnCount;
  }

  public SQLProjection getReturnProjection() {
    return returnProjection;
  }

  public SQLWhereClause getWhereClause() {
    return whereClause;
  }

  public SQLLimit getLimit() {
    return limit;
  }

  public SQLTimeout getTimeout() {
    return timeout;
  }
}
/* JavaCC - OriginalChecksum=093091d7273f1073ad49f2a2bf709a53 (do not edit this line) */
