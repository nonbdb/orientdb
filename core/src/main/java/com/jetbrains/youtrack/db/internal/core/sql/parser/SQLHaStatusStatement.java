/* Generated By:JJTree: Do not edit this line. SQLHaStatusStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.api.exception.BaseException;
import com.jetbrains.youtrack.db.internal.common.log.LogManager;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.api.exception.CommandExecutionException;
import com.jetbrains.youtrack.db.internal.core.sql.executor.ResultInternal;
import com.jetbrains.youtrack.db.internal.core.sql.executor.resultset.ExecutionStream;
import java.util.Map;

public class SQLHaStatusStatement extends SQLSimpleExecStatement {

  public boolean servers = false;
  public boolean db = false;
  public boolean latency = false;
  public boolean messages = false;
  public boolean outputText = false;
  public boolean locks = false;

  public SQLHaStatusStatement(int id) {
    super(id);
  }

  public SQLHaStatusStatement(YouTrackDBSql p, int id) {
    super(p, id);
  }

  @Override
  public boolean isIdempotent() {
    return true;
  }

  @Override
  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("HA STATUS");
    if (servers) {
      builder.append(" -servers");
    }
    if (db) {
      builder.append(" -db");
    }
    if (latency) {
      builder.append(" -latency");
    }
    if (messages) {
      builder.append(" -messages");
    }
    if (locks) {
      builder.append(" -locks");
    }
    if (outputText) {
      builder.append(" -output=text");
    }
    if (servers) {
      builder.append(" -servers");
    }
  }

  @Override
  public void toGenericStatement(StringBuilder builder) {
    builder.append("HA STATUS");
    if (servers) {
      builder.append(" -servers");
    }
    if (db) {
      builder.append(" -db");
    }
    if (latency) {
      builder.append(" -latency");
    }
    if (messages) {
      builder.append(" -messages");
    }
    if (locks) {
      builder.append(" -locks");
    }
    if (outputText) {
      builder.append(" -output=text");
    }
    if (servers) {
      builder.append(" -servers");
    }
  }

  @Override
  public ExecutionStream executeSimple(CommandContext ctx) {
    if (outputText) {
      LogManager.instance().info(this, "HA STATUS with text output is deprecated");
    }
    final var database = ctx.getDatabase();

    try {
      var res = database.getHaStatus(servers, this.db, latency, messages);
      if (res != null) {
        var row = new ResultInternal(database);
        res.entrySet().forEach(x -> row.setProperty(x.getKey(), x.getValue()));
        return ExecutionStream.singleton(row);
      } else {
        return ExecutionStream.empty();
      }
    } catch (Exception x) {
      throw BaseException.wrapException(new CommandExecutionException("Cannot execute HA STATUS"),
          x);
    }
  }
}
/* JavaCC - OriginalChecksum=c8ab1b0172e8cdbea2078efe2c629e6a (do not edit this line) */
