/* Generated By:JJTree: Do not edit this line. OHaSyncDatabaseStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.orientechnologies.orient.core.sql.parser;

import com.orientechnologies.common.exception.OException;
import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.ODatabaseSessionInternal;
import com.orientechnologies.orient.core.exception.OCommandExecutionException;
import com.orientechnologies.orient.core.sql.executor.OResultInternal;
import com.orientechnologies.orient.core.sql.executor.resultset.OExecutionStream;
import java.util.Map;

public class OHaSyncDatabaseStatement extends OSimpleExecStatement {

  public boolean force = false;
  public boolean full = false;

  public OHaSyncDatabaseStatement(int id) {
    super(id);
  }

  public OHaSyncDatabaseStatement(OrientSql p, int id) {
    super(p, id);
  }

  @Override
  public OExecutionStream executeSimple(OCommandContext ctx) {
    final ODatabaseSessionInternal database = ctx.getDatabase();

    try {
      boolean result = database.sync(force, !full);
      OResultInternal r = new OResultInternal(database);
      r.setProperty("result", result);
      return OExecutionStream.singleton(r);
    } catch (Exception e) {
      throw OException.wrapException(
          new OCommandExecutionException("Cannot execute HA SYNC DATABASE"), e);
    }
  }

  @Override
  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("HA SYNC DATABASE");
    if (force) {
      builder.append(" -force");
    }
    if (full) {
      builder.append(" -full");
    }
  }

  @Override
  public void toGenericStatement(StringBuilder builder) {
    builder.append("HA SYNC DATABASE");
    if (force) {
      builder.append(" -force");
    }
    if (full) {
      builder.append(" -full");
    }
  }

  public boolean isForce() {
    return force;
  }

  public boolean isFull() {
    return full;
  }
}
/* JavaCC - OriginalChecksum=f2c9070be78798e3093a98669129ce0d (do not edit this line) */
