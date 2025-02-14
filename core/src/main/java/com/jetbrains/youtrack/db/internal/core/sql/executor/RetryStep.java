package com.jetbrains.youtrack.db.internal.core.sql.executor;

import com.jetbrains.youtrack.db.internal.common.concur.NeedRetryException;
import com.jetbrains.youtrack.db.internal.common.concur.TimeoutException;
import com.jetbrains.youtrack.db.internal.core.command.BasicCommandContext;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.ExecutionThreadLocal;
import com.jetbrains.youtrack.db.internal.core.exception.CommandInterruptedException;
import com.jetbrains.youtrack.db.internal.core.sql.executor.resultset.ExecutionStream;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLStatement;
import java.util.List;

/**
 *
 */
public class RetryStep extends AbstractExecutionStep {

  public List<SQLStatement> body;
  public List<SQLStatement> elseBody;
  public boolean elseFail;
  private final int retries;

  public RetryStep(
      List<SQLStatement> statements,
      int retries,
      List<SQLStatement> elseStatements,
      Boolean elseFail,
      CommandContext ctx,
      boolean enableProfiling) {
    super(ctx, enableProfiling);
    this.body = statements;
    this.retries = retries;
    this.elseBody = elseStatements;
    this.elseFail = !(Boolean.FALSE.equals(elseFail));
  }

  @Override
  public ExecutionStream internalStart(CommandContext ctx) throws TimeoutException {
    if (prev != null) {
      prev.start(ctx).close(ctx);
    }

    for (int i = 0; i < retries; i++) {
      try {

        if (ExecutionThreadLocal.isInterruptCurrentOperation()) {
          throw new CommandInterruptedException("The command has been interrupted");
        }
        ScriptExecutionPlan plan = initPlan(body, ctx);
        ExecutionStepInternal result = plan.executeFull();
        if (result != null) {
          return result.start(ctx);
        }
        break;
      } catch (NeedRetryException ex) {
        try {
          var db = ctx.getDatabase();
          db.rollback();
        } catch (Exception ignored) {
        }

        if (i == retries - 1) {
          if (elseBody != null && !elseBody.isEmpty()) {
            ScriptExecutionPlan plan = initPlan(elseBody, ctx);
            ExecutionStepInternal result = plan.executeFull();
            if (result != null) {
              return result.start(ctx);
            }
          }
          if (elseFail) {
            throw ex;
          } else {
            return ExecutionStream.empty();
          }
        }
      }
    }

    return new EmptyStep(ctx, false).start(ctx);
  }

  public ScriptExecutionPlan initPlan(List<SQLStatement> body, CommandContext ctx) {
    BasicCommandContext subCtx1 = new BasicCommandContext();
    subCtx1.setParent(ctx);
    ScriptExecutionPlan plan = new ScriptExecutionPlan(subCtx1);
    for (SQLStatement stm : body) {
      plan.chain(stm.createExecutionPlan(subCtx1, profilingEnabled), profilingEnabled);
    }
    return plan;
  }
}
