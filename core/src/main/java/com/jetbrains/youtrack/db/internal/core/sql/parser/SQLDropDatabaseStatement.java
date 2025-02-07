/* Generated By:JJTree: Do not edit this line. SQLDropDatabaseStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.internal.core.command.ServerCommandContext;
import com.jetbrains.youtrack.db.api.exception.CommandExecutionException;
import com.jetbrains.youtrack.db.internal.core.db.YouTrackDBInternal;
import com.jetbrains.youtrack.db.internal.core.sql.executor.ResultInternal;
import com.jetbrains.youtrack.db.internal.core.sql.executor.resultset.ExecutionStream;
import java.util.Map;

public class SQLDropDatabaseStatement extends SQLSimpleExecServerStatement {

  protected boolean ifExists = false;
  protected SQLIdentifier name;
  protected SQLInputParameter nameParam;

  public SQLDropDatabaseStatement(int id) {
    super(id);
  }

  public SQLDropDatabaseStatement(YouTrackDBSql p, int id) {
    super(p, id);
  }

  @Override
  public ExecutionStream executeSimple(ServerCommandContext ctx) {
    String nameString;
    if (name != null) {
      nameString = name.getStringValue();
    } else {
      nameString = "" + nameParam.getValue(ctx.getInputParameters());
    }
    YouTrackDBInternal server = ctx.getServer();
    ResultInternal result = new ResultInternal(ctx.getDatabase());
    result.setProperty("operation", "drop database");
    result.setProperty("name", nameString);

    if (ifExists && !server.exists(nameString, null, null)) {
      result.setProperty("dropped", false);
      result.setProperty("existing", false);
    } else {
      try {
        server.drop(nameString, null, null);
        result.setProperty("dropped", true);
      } catch (Exception e) {
        throw new CommandExecutionException(
            "Could not drop database " + nameString + ":" + e.getMessage());
      }
    }

    return ExecutionStream.singleton(result);
  }

  @Override
  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("DROP DATABASE ");
    if (name != null) {
      name.toString(params, builder);
    } else {
      nameParam.toString(params, builder);
    }
    if (ifExists) {
      builder.append(" IF EXISTS");
    }
  }
}
/* JavaCC - OriginalChecksum=3bc7e2aee1f1319f7cb7db5e825f7ee7 (do not edit this line) */
