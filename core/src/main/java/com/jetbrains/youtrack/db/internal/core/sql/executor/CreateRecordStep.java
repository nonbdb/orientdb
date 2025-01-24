package com.jetbrains.youtrack.db.internal.core.sql.executor;

import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.api.record.Entity;
import com.jetbrains.youtrack.db.internal.common.concur.TimeoutException;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.sql.executor.resultset.ExecutionStream;
import com.jetbrains.youtrack.db.internal.core.sql.executor.resultset.ProduceExecutionStream;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLIdentifier;

/**
 *
 */
public class CreateRecordStep extends AbstractExecutionStep {

  private final int total;
  private String targetClass;

  public CreateRecordStep(CommandContext ctx, SQLIdentifier targetClass, int total,
      boolean profilingEnabled) {
    super(ctx, profilingEnabled);
    this.total = total;
    if (targetClass != null) {
      this.targetClass = targetClass.getStringValue();
    }
  }

  @Override
  public ExecutionStream internalStart(CommandContext ctx) throws TimeoutException {
    if (prev != null) {
      prev.start(ctx).close(ctx);
    }

    return new ProduceExecutionStream(this::produce).limit(total);
  }

  private Result produce(CommandContext ctx) {
    var db = ctx.getDatabase();
    final Entity entity;
    if (targetClass != null) {
      entity = db.newEntity(targetClass);
    } else {
      entity = db.newEntity();
    }

    return new UpdatableResult(db, entity);
  }

  @Override
  public String prettyPrint(int depth, int indent) {
    String spaces = ExecutionStepInternal.getIndent(depth, indent);
    StringBuilder result = new StringBuilder();
    result.append(spaces);
    result.append("+ CREATE EMPTY RECORDS");
    if (profilingEnabled) {
      result.append(" (").append(getCostFormatted()).append(")");
    }
    result.append("\n");
    result.append(spaces);
    if (total == 1) {
      result.append("  1 record");
    } else {
      result.append("  ").append(total).append(" record");
    }
    return result.toString();
  }
}
