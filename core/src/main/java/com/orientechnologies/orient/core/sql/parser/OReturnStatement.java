/* Generated By:JJTree: Do not edit this line. OReturnStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.orientechnologies.orient.core.sql.parser;

import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.sql.executor.OResult;
import com.orientechnologies.orient.core.sql.executor.OResultInternal;
import com.orientechnologies.orient.core.sql.executor.OResultSet;
import com.orientechnologies.orient.core.sql.executor.resultset.OExecutionStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class OReturnStatement extends OSimpleExecStatement {

  protected OExpression expression;

  public OReturnStatement(int id) {
    super(id);
  }

  public OReturnStatement(OrientSql p, int id) {
    super(p, id);
  }

  @Override
  public OExecutionStream executeSimple(OCommandContext ctx) {
    List<OResult> rs = new ArrayList<>();

    var database = ctx.getDatabase();
    Object result = expression == null ? null : expression.execute((OResult) null, ctx);
    if (result instanceof OResult) {
      rs.add((OResult) result);
    } else if (result instanceof OIdentifiable) {
      OResultInternal res = new OResultInternal(database, (OIdentifiable) result);
      rs.add(res);
    } else if (result instanceof OResultSet) {
      if (!((OResultSet) result).hasNext()) {
        try {
          ((OResultSet) result).reset();
        } catch (UnsupportedOperationException ignore) {
          // just try to reset the RS, in case it was already used during the script execution
          // already
          // You can have two cases here:
          // - a result stored in a LET, that is always resettable, as it's copied
          // - a result from a direct query (eg. RETURN SELECT...), that is new or just empty, so
          // this operation does not hurt
        }
      }
      return OExecutionStream.resultIterator(((OResultSet) result).stream().iterator());
    } else if (result instanceof OExecutionStream) {
      return (OExecutionStream) result;
    } else {
      OResultInternal res = new OResultInternal(database);
      res.setProperty("value", result);
      rs.add(res);
    }
    return OExecutionStream.resultIterator(rs.iterator());
  }

  @Override
  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("RETURN");
    if (expression != null) {
      builder.append(" ");
      expression.toString(params, builder);
    }
  }

  @Override
  public void toGenericStatement(StringBuilder builder) {
    builder.append("RETURN");
    if (expression != null) {
      builder.append(" ");
      expression.toGenericStatement(builder);
    }
  }

  @Override
  public OReturnStatement copy() {
    OReturnStatement result = new OReturnStatement(-1);
    result.expression = expression == null ? null : expression.copy();
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

    OReturnStatement that = (OReturnStatement) o;

    return Objects.equals(expression, that.expression);
  }

  @Override
  public int hashCode() {
    return expression != null ? expression.hashCode() : 0;
  }
}
/* JavaCC - OriginalChecksum=c72ec860d1fa92cbf52e42ae1c2935c0 (do not edit this line) */
