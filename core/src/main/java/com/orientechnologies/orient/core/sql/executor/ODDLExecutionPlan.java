package com.orientechnologies.orient.core.sql.executor;

import com.orientechnologies.orient.core.command.OBasicCommandContext;
import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.ODatabaseSessionInternal;
import com.orientechnologies.orient.core.exception.OCommandExecutionException;
import com.orientechnologies.orient.core.sql.executor.resultset.OExecutionStream;
import com.orientechnologies.orient.core.sql.parser.ODDLStatement;
import java.util.Collections;
import java.util.List;

/**
 *
 */
public class ODDLExecutionPlan implements OInternalExecutionPlan {

  private final ODDLStatement statement;
  private final OCommandContext ctx;

  private boolean executed = false;

  public ODDLExecutionPlan(OCommandContext ctx, ODDLStatement stm) {
    this.ctx = ctx;
    this.statement = stm;
  }

  @Override
  public void close() {
  }

  @Override
  public OCommandContext getContext() {
    return ctx;
  }

  @Override
  public OExecutionStream start() {
    return OExecutionStream.empty();
  }

  public void reset(OCommandContext ctx) {
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

  public OExecutionStream executeInternal(OBasicCommandContext ctx)
      throws OCommandExecutionException {
    if (executed) {
      throw new OCommandExecutionException(
          "Trying to execute a result-set twice. Please use reset()");
    }
    executed = true;
    OExecutionStream result = statement.executeDDL(this.ctx);
    return result;
  }

  @Override
  public List<OExecutionStep> getSteps() {
    return Collections.emptyList();
  }

  @Override
  public String prettyPrint(int depth, int indent) {
    String spaces = OExecutionStepInternal.getIndent(depth, indent);
    String result = spaces + "+ DDL\n" + "  " + statement.toString();
    return result;
  }

  @Override
  public OResult toResult(ODatabaseSessionInternal db) {
    OResultInternal result = new OResultInternal(db);
    result.setProperty("type", "DDLExecutionPlan");
    result.setProperty(JAVA_TYPE, getClass().getName());
    result.setProperty("stmText", statement.toString());
    result.setProperty("cost", getCost());
    result.setProperty("prettyPrint", prettyPrint(0, 2));
    return result;
  }
}
