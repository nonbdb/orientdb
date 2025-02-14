package com.jetbrains.youtrack.db.internal.core.sql.executor;

import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.internal.common.concur.TimeoutException;
import com.jetbrains.youtrack.db.internal.core.command.BasicCommandContext;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.sql.executor.resultset.ExecutionStream;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLBooleanExpression;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLIfStatement;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLReturnStatement;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLStatement;
import java.util.List;

/**
 *
 */
public class IfStep extends AbstractExecutionStep {

  protected SQLBooleanExpression condition;
  public List<SQLStatement> positiveStatements;
  public List<SQLStatement> negativeStatements;

  public IfStep(CommandContext ctx, boolean profilingEnabled) {
    super(ctx, profilingEnabled);
  }

  @Override
  public ExecutionStream internalStart(CommandContext ctx) throws TimeoutException {
    ScriptExecutionPlan plan = producePlan(ctx);
    if (plan != null) {
      return plan.start();
    } else {
      return ExecutionStream.empty();
    }
  }

  public ScriptExecutionPlan producePlan(CommandContext ctx) {
    if (condition.evaluate((Result) null, ctx)) {
      ScriptExecutionPlan positivePlan = initPositivePlan(ctx);
      return positivePlan;
    } else {
      ScriptExecutionPlan negativePlan = initNegativePlan(ctx);
      return negativePlan;
    }
  }

  public ScriptExecutionPlan initPositivePlan(CommandContext ctx) {
    BasicCommandContext subCtx1 = new BasicCommandContext();
    subCtx1.setParent(ctx);
    ScriptExecutionPlan positivePlan = new ScriptExecutionPlan(subCtx1);
    for (SQLStatement stm : positiveStatements) {
      positivePlan.chain(stm.createExecutionPlan(subCtx1, profilingEnabled), profilingEnabled);
    }
    return positivePlan;
  }

  public ScriptExecutionPlan initNegativePlan(CommandContext ctx) {
    if (negativeStatements != null) {
      if (negativeStatements.size() > 0) {
        BasicCommandContext subCtx2 = new BasicCommandContext();
        subCtx2.setParent(ctx);
        ScriptExecutionPlan negativePlan = new ScriptExecutionPlan(subCtx2);
        for (SQLStatement stm : negativeStatements) {
          negativePlan.chain(stm.createExecutionPlan(subCtx2, profilingEnabled), profilingEnabled);
        }
        return negativePlan;
      }
    }
    return null;
  }

  public SQLBooleanExpression getCondition() {
    return condition;
  }

  public void setCondition(SQLBooleanExpression condition) {
    this.condition = condition;
  }

  public boolean containsReturn() {
    if (positiveStatements != null) {
      for (SQLStatement stm : positiveStatements) {
        if (containsReturn(stm)) {
          return true;
        }
      }
    }
    if (negativeStatements != null) {
      for (SQLStatement stm : negativeStatements) {
        if (containsReturn(stm)) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean containsReturn(SQLStatement stm) {
    if (stm instanceof SQLReturnStatement) {
      return true;
    }
    if (stm instanceof SQLIfStatement) {
      for (SQLStatement o : ((SQLIfStatement) stm).getStatements()) {
        if (containsReturn(o)) {
          return true;
        }
      }
    }
    return false;
  }
}
