/* Generated By:JJTree: Do not edit this line. OCommitStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.orientechnologies.orient.core.sql.parser;

import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.sql.executor.OResultInternal;
import com.orientechnologies.orient.core.sql.executor.resultset.OExecutionStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OCommitStatement extends OSimpleExecStatement {

  protected OInteger retry;
  protected List<OStatement> elseStatements;
  protected Boolean elseFail;

  public OCommitStatement(int id) {
    super(id);
  }

  public OCommitStatement(OrientSql p, int id) {
    super(p, id);
  }

  public void addElse(OStatement statement) {
    if (elseStatements == null) {
      elseStatements = new ArrayList<>();
    }
    this.elseStatements.add(statement);
  }

  @Override
  public OExecutionStream executeSimple(OCommandContext ctx) {
    ctx.getDatabase()
        .commit(); // no RETRY and ELSE here, that case is allowed only for batch scripts;
    OResultInternal item = new OResultInternal();
    item.setProperty("operation", "commit");
    return OExecutionStream.singleton(item);
  }

  @Override
  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("COMMIT");
    if (retry != null) {
      builder.append(" RETRY ");
      retry.toString(params, builder);
      if (elseFail != null || elseStatements != null) {
        builder.append(" ELSE ");
      }
      if (elseStatements != null) {
        builder.append("{\n");
        for (OStatement stm : elseStatements) {
          stm.toString(params, builder);
          builder.append(";\n");
        }
        builder.append("}");
      }
      if (elseFail != null) {
        if (elseStatements != null) {
          builder.append(" AND");
        }
        if (elseFail) {
          builder.append(" FAIL");
        } else {
          builder.append(" CONTINUE");
        }
      }
    }
  }

  @Override
  public void toGenericStatement(StringBuilder builder) {
    builder.append("COMMIT");
    if (retry != null) {
      builder.append(" RETRY ");
      retry.toGenericStatement(builder);
      if (elseFail != null || elseStatements != null) {
        builder.append(" ELSE ");
      }
      if (elseStatements != null) {
        builder.append("{\n");
        for (OStatement stm : elseStatements) {
          stm.toGenericStatement(builder);
          builder.append(";\n");
        }
        builder.append("}");
      }
      if (elseFail != null) {
        if (elseStatements != null) {
          builder.append(" AND");
        }
        if (elseFail) {
          builder.append(" FAIL");
        } else {
          builder.append(" CONTINUE");
        }
      }
    }
  }

  @Override
  public OCommitStatement copy() {
    OCommitStatement result = new OCommitStatement(-1);
    result.retry = retry == null ? null : retry.copy();
    if (this.elseStatements != null) {
      result.elseStatements = new ArrayList<>();
      for (OStatement stm : elseStatements) {
        result.elseStatements.add(stm.copy());
      }
    }
    if (elseFail != null) {
      result.elseFail = elseFail;
    }
    return result;
  }

  public OInteger getRetry() {
    return retry;
  }

  public List<OStatement> getElseStatements() {
    return elseStatements;
  }

  public Boolean getElseFail() {
    return elseFail;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    OCommitStatement that = (OCommitStatement) o;

    if (retry != null ? !retry.equals(that.retry) : that.retry != null) {
      return false;
    }
    if (elseStatements != null
        ? !elseStatements.equals(that.elseStatements)
        : that.elseStatements != null) {
      return false;
    }
    return elseFail != null ? elseFail.equals(that.elseFail) : that.elseFail == null;
  }

  @Override
  public int hashCode() {
    int result = retry != null ? retry.hashCode() : 0;
    result = 31 * result + (elseStatements != null ? elseStatements.hashCode() : 0);
    result = 31 * result + (elseFail != null ? elseFail.hashCode() : 0);
    return result;
  }
}
/* JavaCC - OriginalChecksum=eaa0bc8f765fdaa017789953861bc0aa (do not edit this line) */
