/* Generated By:JJTree: Do not edit this line. SQLSkip.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.api.exception.CommandExecutionException;
import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.internal.core.sql.executor.ResultInternal;
import java.util.Map;
import java.util.Objects;

public class SQLSkip extends SimpleNode {

  protected SQLInteger num;

  protected SQLInputParameter inputParam;

  public SQLSkip(int id) {
    super(id);
  }

  public SQLSkip(YouTrackDBSql p, int id) {
    super(p, id);
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    if (num == null && inputParam == null) {
      return;
    }
    builder.append(" SKIP ");
    if (num != null) {
      num.toString(params, builder);
    } else {
      inputParam.toString(params, builder);
    }
  }

  public void toGenericStatement(StringBuilder builder) {
    if (num == null && inputParam == null) {
      return;
    }
    builder.append(" SKIP ");
    if (num != null) {
      num.toGenericStatement(builder);
    } else {
      inputParam.toGenericStatement(builder);
    }
  }

  public int getValue(CommandContext ctx) {
    if (num != null) {
      return num.getValue().intValue();
    }
    if (inputParam != null) {
      var paramValue = inputParam.getValue(ctx.getInputParameters());
      if (paramValue instanceof Number) {
        return ((Number) paramValue).intValue();
      } else {
        throw new CommandExecutionException("Invalid value for SKIP: " + paramValue);
      }
    }
    throw new CommandExecutionException("No value for SKIP");
  }

  public SQLSkip copy() {
    var result = new SQLSkip(-1);
    result.num = num == null ? null : num.copy();
    result.inputParam = inputParam == null ? null : inputParam.copy();
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

    var oSkip = (SQLSkip) o;

    if (!Objects.equals(num, oSkip.num)) {
      return false;
    }
    return Objects.equals(inputParam, oSkip.inputParam);
  }

  @Override
  public int hashCode() {
    var result = num != null ? num.hashCode() : 0;
    result = 31 * result + (inputParam != null ? inputParam.hashCode() : 0);
    return result;
  }

  public Result serialize(DatabaseSessionInternal db) {
    var result = new ResultInternal(db);
    if (num != null) {
      result.setProperty("num", num.serialize(db));
    }
    if (inputParam != null) {
      result.setProperty("inputParam", inputParam.serialize(db));
    }
    return result;
  }

  public void deserialize(Result fromResult) {
    if (fromResult.getProperty("num") != null) {
      num = new SQLInteger(-1);
      num.deserialize(fromResult.getProperty("num"));
    }
    if (fromResult.getProperty("inputParam") != null) {
      inputParam = SQLInputParameter.deserializeFromOResult(fromResult.getProperty("inputParam"));
    }
  }
}
/* JavaCC - OriginalChecksum=8e13ca184705a8fc1b5939ecefe56a60 (do not edit this line) */
