package com.jetbrains.youtrack.db.internal.core.sql.executor;

import com.jetbrains.youtrack.db.api.DatabaseSession;
import com.jetbrains.youtrack.db.api.exception.CommandExecutionException;
import com.jetbrains.youtrack.db.api.query.ExecutionStep;
import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.command.ServerCommandContext;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.sql.executor.resultset.ExecutionStream;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLSimpleExecServerStatement;
import java.util.Collections;
import java.util.List;

/**
 *
 */
public class SingleOpServerExecutionPlan implements InternalExecutionPlan {

  protected final SQLSimpleExecServerStatement statement;
  private final ServerCommandContext ctx;

  private boolean executed = false;
  private ExecutionStream result;

  public SingleOpServerExecutionPlan(ServerCommandContext ctx, SQLSimpleExecServerStatement stm) {
    this.ctx = ctx;
    this.statement = stm;
  }

  @Override
  public CommandContext getContext() {
    return ctx;
  }

  @Override
  public void close() {
  }

  @Override
  public ExecutionStream start() {
    if (executed && result == null) {
      return ExecutionStream.empty();
    }
    if (!executed) {
      executed = true;
      result = statement.executeSimple(this.ctx);
    }
    return result;
  }

  public void reset(CommandContext ctx) {
    executed = false;
  }

  @Override
  public long getCost() {
    return 0;
  }

  @Override
  public boolean canBeCached() {
    return false;
  }

  public ExecutionStream executeInternal()
      throws CommandExecutionException {
    if (executed) {
      throw new CommandExecutionException(
          "Trying to execute a result-set twice. Please use reset()");
    }
    executed = true;
    result = statement.executeSimple(this.ctx);
    return result;
  }

  @Override
  public List<ExecutionStep> getSteps() {
    return Collections.emptyList();
  }

  @Override
  public String prettyPrint(int depth, int indent) {
    String spaces = ExecutionStepInternal.getIndent(depth, indent);
    String result = spaces + "+ " + statement.toString();
    return result;
  }

  @Override
  public Result toResult(DatabaseSession db) {
    ResultInternal result = new ResultInternal((DatabaseSessionInternal) db);
    result.setProperty("type", "QueryExecutionPlan");
    result.setProperty("javaType", getClass().getName());
    result.setProperty("stmText", statement.toString());
    result.setProperty("cost", getCost());
    result.setProperty("prettyPrint", prettyPrint(0, 2));
    result.setProperty("steps", null);
    return result;
  }
}
