/* Generated By:JJTree: Do not edit this line. OAlterSequenceStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.orientechnologies.orient.core.sql.parser;

import com.orientechnologies.common.log.OLogManager;
import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.exception.YTCommandExecutionException;
import com.orientechnologies.orient.core.exception.YTDatabaseException;
import com.orientechnologies.orient.core.metadata.sequence.YTSequence;
import com.orientechnologies.orient.core.sql.executor.OResultInternal;
import com.orientechnologies.orient.core.sql.executor.resultset.OExecutionStream;
import java.util.Map;
import java.util.Objects;

public class ODropSequenceStatement extends ODDLStatement {

  OIdentifier name;

  boolean ifExists = false;

  public ODropSequenceStatement(int id) {
    super(id);
  }

  public ODropSequenceStatement(OrientSql p, int id) {
    super(p, id);
  }

  @Override
  public OExecutionStream executeDDL(OCommandContext ctx) {
    final var database = ctx.getDatabase();
    YTSequence sequence =
        database.getMetadata().getSequenceLibrary().getSequence(this.name.getStringValue());
    if (sequence == null) {
      if (ifExists) {
        return OExecutionStream.empty();
      } else {
        throw new YTCommandExecutionException("Sequence not found: " + name);
      }
    }

    try {
      database.getMetadata().getSequenceLibrary().dropSequence(name.getStringValue());
    } catch (YTDatabaseException exc) {
      String message = "Unable to execute command: " + exc.getMessage();
      OLogManager.instance().error(this, message, exc, (Object) null);
      throw new YTCommandExecutionException(message);
    }

    OResultInternal result = new OResultInternal(database);
    result.setProperty("operation", "drop sequence");
    result.setProperty("sequenceName", name.getStringValue());
    return OExecutionStream.singleton(result);
  }

  @Override
  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("DROP SEQUENCE ");
    name.toString(params, builder);
    if (ifExists) {
      builder.append(" IF EXISTS");
    }
  }

  @Override
  public void toGenericStatement(StringBuilder builder) {
    builder.append("DROP SEQUENCE ");
    name.toGenericStatement(builder);
    if (ifExists) {
      builder.append(" IF EXISTS");
    }
  }

  @Override
  public ODropSequenceStatement copy() {
    ODropSequenceStatement result = new ODropSequenceStatement(-1);
    result.name = name == null ? null : name.copy();
    result.ifExists = this.ifExists;
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

    ODropSequenceStatement that = (ODropSequenceStatement) o;

    if (this.ifExists != that.ifExists) {
      return false;
    }
    return Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return name != null ? name.hashCode() : 0;
  }
}
/* JavaCC - OriginalChecksum=def62b1d04db5223307fe51873a9edd0 (do not edit this line) */
