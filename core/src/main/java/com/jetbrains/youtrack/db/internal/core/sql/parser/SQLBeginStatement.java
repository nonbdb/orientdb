/* Generated By:JJTree: Do not edit this line. SQLBeginStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.sql.executor.ResultInternal;
import com.jetbrains.youtrack.db.internal.core.sql.executor.resultset.ExecutionStream;
import java.util.Map;

public class SQLBeginStatement extends SQLSimpleExecStatement {

  public SQLBeginStatement(int id) {
    super(id);
  }

  public SQLBeginStatement(YouTrackDBSql p, int id) {
    super(p, id);
  }

  @Override
  public ExecutionStream executeSimple(CommandContext ctx) {
    var db = ctx.getDatabase();
    db.begin();
    var item = new ResultInternal(db);
    item.setProperty("operation", "begin");
    return ExecutionStream.singleton(item);
  }

  @Override
  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("BEGIN");
  }

  @Override
  public void toGenericStatement(StringBuilder builder) {
    builder.append("BEGIN");
  }

  @Override
  public SQLBeginStatement copy() {
    return new SQLBeginStatement(-1);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    return o != null && getClass() == o.getClass();
  }

  @Override
  public int hashCode() {
    return 0;
  }
}
/* JavaCC - OriginalChecksum=aaa994acbe63cc4169fe33144d412fed (do not edit this line) */
