/* Generated By:JJTree: Do not edit this line. OConsoleStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.orientechnologies.orient.core.sql.parser;

import com.orientechnologies.common.log.OLogManager;
import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.exception.OCommandExecutionException;
import com.orientechnologies.orient.core.sql.executor.OResultInternal;
import com.orientechnologies.orient.core.sql.executor.resultset.OExecutionStream;
import java.util.Map;
import java.util.Objects;

public class OConsoleStatement extends OSimpleExecStatement {

  protected OIdentifier logLevel;
  protected OExpression message;

  public OConsoleStatement(int id) {
    super(id);
  }

  public OConsoleStatement(OrientSql p, int id) {
    super(p, id);
  }

  @Override
  public OExecutionStream executeSimple(OCommandContext ctx) {
    OResultInternal item = new OResultInternal(ctx.getDatabase());
    Object msg = "" + message.execute((OIdentifiable) null, ctx);

    if (logLevel.getStringValue().equalsIgnoreCase("log")) {
      OLogManager.instance().info(this, "%s", msg);
    } else if (logLevel.getStringValue().equalsIgnoreCase("output")) {
      System.out.println(msg);
    } else if (logLevel.getStringValue().equalsIgnoreCase("error")) {
      System.err.println(msg);
      OLogManager.instance().error(this, "%s", null, msg);
    } else if (logLevel.getStringValue().equalsIgnoreCase("warn")) {
      OLogManager.instance().warn(this, "%s", msg);
    } else if (logLevel.getStringValue().equalsIgnoreCase("debug")) {
      OLogManager.instance().debug(this, "%s", msg);
    } else {
      throw new OCommandExecutionException("Unsupported log level: " + logLevel);
    }

    item.setProperty("operation", "console");
    item.setProperty("level", logLevel.getStringValue());
    item.setProperty("message", msg);
    return OExecutionStream.singleton(item);
  }

  @Override
  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("CONSOLE.");
    logLevel.toString(params, builder);
    builder.append(" ");
    message.toString(params, builder);
  }

  @Override
  public void toGenericStatement(StringBuilder builder) {
    builder.append("CONSOLE.");
    logLevel.toGenericStatement(builder);
    builder.append(" ");
    message.toGenericStatement(builder);
  }

  @Override
  public OConsoleStatement copy() {
    OConsoleStatement result = new OConsoleStatement(-1);
    result.logLevel = logLevel == null ? null : logLevel.copy();
    result.message = message == null ? null : message.copy();
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    OConsoleStatement that = (OConsoleStatement) o;

    if (!Objects.equals(logLevel, that.logLevel)) {
      return false;
    }
    return Objects.equals(message, that.message);
  }

  @Override
  public int hashCode() {
    int result = logLevel != null ? logLevel.hashCode() : 0;
    result = 31 * result + (message != null ? message.hashCode() : 0);
    return result;
  }
}
/* JavaCC - OriginalChecksum=626c09cda52a1a8a63eeefcb37bd66a1 (do not edit this line) */
