package com.jetbrains.youtrack.db.internal.core.sql.executor;

import com.jetbrains.youtrack.db.api.query.ExecutionPlan;
import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.api.exception.CommandExecutionException;
import com.jetbrains.youtrack.db.internal.core.sql.parser.ExecutionPlanCache;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLBatch;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLDeleteEdgeStatement;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLExpression;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLFromClause;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLFromItem;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLIdentifier;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLIndexIdentifier;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLLimit;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLRid;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLSelectStatement;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLWhereClause;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 */
public class DeleteEdgeExecutionPlanner {

  private final SQLDeleteEdgeStatement statement;

  protected SQLIdentifier className;
  protected SQLIdentifier targetClusterName;

  protected List<SQLRid> rids;

  private SQLExpression leftExpression;
  private SQLExpression rightExpression;

  protected SQLBatch batch = null;

  private SQLWhereClause whereClause;

  private SQLLimit limit;

  public DeleteEdgeExecutionPlanner(SQLDeleteEdgeStatement stm2) {
    this.statement = stm2;
  }

  private void init() {
    this.className =
        this.statement.getClassName() == null ? null : this.statement.getClassName().copy();
    this.targetClusterName =
        this.statement.getTargetClusterName() == null
            ? null
            : this.statement.getTargetClusterName().copy();
    if (this.statement.getRid() != null) {
      this.rids = new ArrayList<>();
      rids.add(this.statement.getRid().copy());
    } else if (this.statement.getRids() == null) {
      this.rids = null;
    } else {
      this.rids = this.statement.getRids().stream().map(x -> x.copy()).collect(Collectors.toList());
    }

    this.leftExpression =
        this.statement.getLeftExpression() == null
            ? null
            : this.statement.getLeftExpression().copy();
    this.rightExpression =
        this.statement.getRightExpression() == null
            ? null
            : this.statement.getRightExpression().copy();

    this.whereClause =
        this.statement.getWhereClause() == null ? null : this.statement.getWhereClause().copy();
    this.batch = this.statement.getBatch() == null ? null : this.statement.getBatch().copy();
    this.limit = this.statement.getLimit() == null ? null : this.statement.getLimit().copy();
  }

  public InternalExecutionPlan createExecutionPlan(
      CommandContext ctx, boolean enableProfiling, boolean useCache) {
    var db = ctx.getDatabase();
    if (useCache && !enableProfiling && statement.executinPlanCanBeCached(db)) {
      var plan = ExecutionPlanCache.get(statement.getOriginalStatement(), ctx, db);
      if (plan != null) {
        return (InternalExecutionPlan) plan;
      }
    }
    var planningStart = System.currentTimeMillis();

    init();
    var result = new DeleteExecutionPlan(ctx);

    if (leftExpression != null || rightExpression != null) {
      handleGlobalLet(
          result,
          new SQLIdentifier("$__YOUTRACKDB_DELETE_EDGE_fromV"),
          leftExpression,
          ctx,
          enableProfiling);
      handleGlobalLet(
          result,
          new SQLIdentifier("$__YOUTRACKDB_DELETE_EDGE_toV"),
          rightExpression,
          ctx,
          enableProfiling);
      String fromLabel = null;
      if (leftExpression != null) {
        fromLabel = "$__YOUTRACKDB_DELETE_EDGE_fromV";
      }
      handleFetchFromTo(
          result,
          ctx,
          fromLabel,
          "$__YOUTRACKDB_DELETE_EDGE_toV",
          className,
          targetClusterName,
          enableProfiling);
      handleWhere(result, ctx, whereClause, enableProfiling);
    } else if (whereClause != null) {
      var fromClause = new SQLFromClause(-1);
      var item = new SQLFromItem(-1);
      if (className == null) {
        item.setIdentifier(new SQLIdentifier("E"));
      } else {
        item.setIdentifier(className);
      }
      fromClause.setItem(item);
      handleTarget(result, ctx, fromClause, this.whereClause, enableProfiling);
    } else {
      handleTargetClass(result, ctx, className, enableProfiling);
      handleTargetCluster(result, ctx, targetClusterName, enableProfiling);
      handleTargetRids(result, ctx, rids, enableProfiling);
    }

    handleLimit(result, ctx, this.limit, enableProfiling);
    handleCastToEdge(result, ctx, enableProfiling);
    handleDelete(result, ctx, enableProfiling);
    handleReturn(result, ctx, enableProfiling);

    if (useCache
        && !enableProfiling
        && this.statement.executinPlanCanBeCached(db)
        && result.canBeCached()
        && ExecutionPlanCache.getLastInvalidation(db) < planningStart) {
      ExecutionPlanCache.put(this.statement.getOriginalStatement(), result, ctx.getDatabase());
    }

    return result;
  }

  private void handleWhere(
      DeleteExecutionPlan result,
      CommandContext ctx,
      SQLWhereClause whereClause,
      boolean profilingEnabled) {
    if (whereClause != null) {
      result.chain(new FilterStep(whereClause, ctx, -1, profilingEnabled));
    }
  }

  private void handleFetchFromTo(
      DeleteExecutionPlan result,
      CommandContext ctx,
      String fromAlias,
      String toAlias,
      SQLIdentifier targetClass,
      SQLIdentifier targetCluster,
      boolean profilingEnabled) {
    if (fromAlias != null && toAlias != null) {
      result.chain(
          new FetchEdgesFromToVerticesStep(
              fromAlias, toAlias, targetClass, targetCluster, ctx, profilingEnabled));
    } else if (toAlias != null) {
      result.chain(
          new FetchEdgesToVerticesStep(toAlias, targetClass, targetCluster, ctx, profilingEnabled));
    }
  }

  private void handleTargetRids(
      DeleteExecutionPlan result, CommandContext ctx, List<SQLRid> rids,
      boolean profilingEnabled) {
    if (rids != null) {
      result.chain(
          new FetchFromRidsStep(
              rids.stream()
                  .map(x -> x.toRecordId((Result) null, ctx))
                  .collect(Collectors.toList()),
              ctx,
              profilingEnabled));
    }
  }

  private void handleTargetCluster(
      DeleteExecutionPlan result,
      CommandContext ctx,
      SQLIdentifier targetClusterName,
      boolean profilingEnabled) {
    if (targetClusterName != null) {
      var name = targetClusterName.getStringValue();
      var clusterId = ctx.getDatabase().getClusterIdByName(name);
      if (clusterId < 0) {
        throw new CommandExecutionException("Cluster not found: " + name);
      }
      result.chain(new FetchFromClusterExecutionStep(clusterId, ctx, profilingEnabled));
    }
  }

  private void handleTargetClass(
      DeleteExecutionPlan result,
      CommandContext ctx,
      SQLIdentifier className,
      boolean profilingEnabled) {
    if (className != null) {
      result.chain(
          new FetchFromClassExecutionStep(
              className.getStringValue(), null, ctx, null, profilingEnabled));
    }
  }

  private boolean handleIndexAsTarget(
      DeleteExecutionPlan result,
      SQLIndexIdentifier indexIdentifier,
      SQLWhereClause whereClause,
      CommandContext ctx,
      boolean profilingEnabled) {
    if (indexIdentifier == null) {
      return false;
    }
    throw new CommandExecutionException("DELETE VERTEX FROM INDEX is not supported");
  }

  private void handleDelete(
      DeleteExecutionPlan result, CommandContext ctx, boolean profilingEnabled) {
    result.chain(new DeleteStep(ctx, profilingEnabled));
  }

  private void handleReturn(
      DeleteExecutionPlan result, CommandContext ctx, boolean profilingEnabled) {
    result.chain(new CountStep(ctx, profilingEnabled));
  }

  private void handleLimit(
      UpdateExecutionPlan plan, CommandContext ctx, SQLLimit limit, boolean profilingEnabled) {
    if (limit != null) {
      plan.chain(new LimitExecutionStep(limit, ctx, profilingEnabled));
    }
  }

  private void handleCastToEdge(
      DeleteExecutionPlan plan, CommandContext ctx, boolean profilingEnabled) {
    plan.chain(new CastToEdgeStep(ctx, profilingEnabled));
  }

  private void handleTarget(
      UpdateExecutionPlan result,
      CommandContext ctx,
      SQLFromClause target,
      SQLWhereClause whereClause,
      boolean profilingEnabled) {
    var sourceStatement = new SQLSelectStatement(-1);
    sourceStatement.setTarget(target);
    sourceStatement.setWhereClause(whereClause);
    var planner = new SelectExecutionPlanner(sourceStatement);
    result.chain(
        new SubQueryStep(
            planner.createExecutionPlan(ctx, profilingEnabled, false), ctx, ctx, profilingEnabled));
  }

  private void handleGlobalLet(
      DeleteExecutionPlan result,
      SQLIdentifier name,
      SQLExpression expression,
      CommandContext ctx,
      boolean profilingEnabled) {
    if (expression != null) {
      result.chain(new GlobalLetExpressionStep(name, expression, ctx, profilingEnabled));
    }
  }
}
