package com.jetbrains.youtrack.db.internal.core.sql.executor;

/**
 *
 */

import com.jetbrains.youtrack.db.api.DatabaseSession;
import com.jetbrains.youtrack.db.api.query.ExecutionStep;
import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.sql.executor.resultset.ExecutionStream;
import java.util.Collections;
import java.util.List;

/**
 *
 */
public class IfExecutionPlan implements InternalExecutionPlan {

  private String location;

  private final CommandContext ctx;

  @Override
  public CommandContext getContext() {
    return ctx;
  }

  protected IfStep step;

  public IfExecutionPlan(CommandContext ctx) {
    this.ctx = ctx;
  }

  @Override
  public void reset(CommandContext ctx) {
    // TODO
    throw new UnsupportedOperationException();
  }

  @Override
  public void close() {
    step.close();
  }

  @Override
  public ExecutionStream start() {
    return step.start(ctx);
  }

  @Override
  public String prettyPrint(int depth, int indent) {
    return step.prettyPrint(depth, indent);
  }

  public void chain(IfStep step) {
    this.step = step;
  }

  @Override
  public List<ExecutionStep> getSteps() {
    // TODO do a copy of the steps
    return Collections.singletonList(step);
  }

  public void setSteps(List<ExecutionStepInternal> steps) {
    this.step = (IfStep) steps.get(0);
  }

  @Override
  public Result toResult(DatabaseSession db) {
    var session = (DatabaseSessionInternal) db;
    var result = new ResultInternal(session);
    result.setProperty("type", "IfExecutionPlan");
    result.setProperty("javaType", getClass().getName());
    result.setProperty("cost", getCost());
    result.setProperty("prettyPrint", prettyPrint(0, 2));
    result.setProperty("steps", Collections.singletonList(step.toResult(session)));
    return result;
  }

  @Override
  public long getCost() {
    return 0L;
  }

  @Override
  public boolean canBeCached() {
    return false;
  }

  public ExecutionStepInternal executeUntilReturn() {
    var plan = step.producePlan(ctx);
    if (plan != null) {
      return plan.executeUntilReturn();
    } else {
      return null;
    }
  }

  public boolean containsReturn() {
    return step.containsReturn();
  }
}
