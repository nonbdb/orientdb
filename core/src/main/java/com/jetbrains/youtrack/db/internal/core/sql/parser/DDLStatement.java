package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.api.query.ResultSet;
import com.jetbrains.youtrack.db.internal.core.command.BasicCommandContext;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.sql.executor.DDLExecutionPlan;
import com.jetbrains.youtrack.db.internal.core.sql.executor.InternalExecutionPlan;
import com.jetbrains.youtrack.db.internal.core.sql.executor.resultset.ExecutionResultSet;
import com.jetbrains.youtrack.db.internal.core.sql.executor.resultset.ExecutionStream;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public abstract class DDLStatement extends SQLStatement {

  public DDLStatement(int id) {
    super(id);
  }

  public DDLStatement(YouTrackDBSql p, int id) {
    super(p, id);
  }

  public abstract ExecutionStream executeDDL(CommandContext ctx);

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
    var executionPlan = (DDLExecutionPlan) createExecutionPlan(ctx, false);
    return new ExecutionResultSet(executionPlan.executeInternal(ctx), ctx, executionPlan);
  }

  public ResultSet execute(
      DatabaseSessionInternal db, Map<Object, Object> params, CommandContext parentCtx,
      boolean usePlanCache) {
    var ctx = new BasicCommandContext();
    if (parentCtx != null) {
      ctx.setParentWithoutOverridingChild(parentCtx);
    }
    ctx.setDatabase(db);
    ctx.setInputParameters(params);
    var executionPlan = (DDLExecutionPlan) createExecutionPlan(ctx, false);
    return new ExecutionResultSet(executionPlan.executeInternal(ctx), ctx, executionPlan);
  }

  public InternalExecutionPlan createExecutionPlan(CommandContext ctx, boolean enableProfiling) {
    return new DDLExecutionPlan(ctx, this);
  }
}
