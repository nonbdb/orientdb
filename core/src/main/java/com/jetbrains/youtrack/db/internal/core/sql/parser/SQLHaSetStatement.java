/* Generated By:JJTree: Do not edit this line. SQLHaSetStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.api.exception.CommandExecutionException;
import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.internal.core.sql.executor.ResultInternal;
import com.jetbrains.youtrack.db.internal.core.sql.executor.resultset.ExecutionStream;
import com.jetbrains.youtrack.db.internal.enterprise.EnterpriseEndpoint;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SQLHaSetStatement extends SQLSimpleExecStatement {

  protected SQLIdentifier operation;
  protected SQLExpression key;
  protected SQLExpression value;

  public SQLHaSetStatement(int id) {
    super(id);
  }

  public SQLHaSetStatement(YouTrackDBSql p, int id) {
    super(p, id);
  }

  @Override
  public ExecutionStream executeSimple(CommandContext ctx) {
    List<Result> result = new ArrayList<>();

    String operation = this.operation.getStringValue();
    var db = ctx.getDatabase();
    Object key = this.key.execute(new ResultInternal(db), ctx);
    if (key == null) {
      key = this.key.getDefaultAlias();
    }

    Object value = this.value.execute(new ResultInternal(db), ctx);
    if (value == null) {
      value = this.value.getDefaultAlias();
      if (value.equals("null")) {
        value = null;
      }
    }

    EnterpriseEndpoint ee = db.getEnterpriseEndpoint();
    if (ee == null) {
      throw new CommandExecutionException(
          "HA SET statements are only supported in YouTrackDB Enterprise Edition");
    }
    if (operation.equalsIgnoreCase("status")) {
      String finalResult;
      try {
        ee.haSetDbStatus(db, String.valueOf(key), String.valueOf(value));
        finalResult = "OK";
      } catch (UnsupportedOperationException e) {
        finalResult = e.getMessage();
      }
      ResultInternal item = new ResultInternal(db);
      item.setProperty("operation", "ha set status");
      item.setProperty("result", finalResult);
      result.add(item);
    } else if (operation.equalsIgnoreCase("owner")) {
      String finalResult;
      try {
        ee.haSetOwner(db, String.valueOf(key), String.valueOf(value));
        finalResult = "OK";
      } catch (UnsupportedOperationException e) {
        finalResult = e.getMessage();
      }
      ResultInternal item = new ResultInternal(db);
      item.setProperty("operation", "ha set owner");
      item.setProperty("result", finalResult);
      result.add(item);
    } else if (operation.equalsIgnoreCase("role")) {
      String finalResult;
      try {
        ee.haSetRole(db, String.valueOf(key), String.valueOf(value));
        finalResult = "OK";
      } catch (UnsupportedOperationException e) {
        finalResult = e.getMessage();
      }
      ResultInternal item = new ResultInternal(db);
      item.setProperty("operation", "ha set role");
      item.setProperty("result", finalResult);
      result.add(item);
    }

    return ExecutionStream.resultIterator(result.iterator());
  }

  @Override
  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("HA SET ");
    operation.toString(params, builder);
    builder.append(" ");
    key.toString(params, builder);
    builder.append(" = ");
    value.toString(params, builder);
  }

  @Override
  public void toGenericStatement(StringBuilder builder) {
    builder.append("HA SET ");
    operation.toGenericStatement(builder);
    builder.append(" ");
    key.toGenericStatement(builder);
    builder.append(" = ");
    value.toGenericStatement(builder);
  }
}
/* JavaCC - OriginalChecksum=21dffd729680550a5deb24492465084d (do not edit this line) */
