/* Generated By:JJTree: Do not edit this line. ODropClassStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.orientechnologies.orient.core.sql.parser;

import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.exception.OCommandExecutionException;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.sql.executor.OResultInternal;
import com.orientechnologies.orient.core.sql.executor.resultset.OExecutionStream;
import java.util.Map;
import java.util.Objects;

public class ODropClassStatement extends ODDLStatement {

  public OIdentifier name;
  public OInputParameter nameParam;
  public boolean ifExists = false;
  public boolean unsafe = false;

  public ODropClassStatement(int id) {
    super(id);
  }

  public ODropClassStatement(OrientSql p, int id) {
    super(p, id);
  }

  @Override
  public OExecutionStream executeDDL(OCommandContext ctx) {
    var db = ctx.getDatabase();
    OSchema schema = db.getMetadata().getSchema();
    String className;
    if (name != null) {
      className = name.getStringValue();
    } else {
      className = String.valueOf(nameParam.getValue(ctx.getInputParameters()));
    }
    OClass clazz = schema.getClass(className);
    if (clazz == null) {
      if (ifExists) {
        return OExecutionStream.empty();
      }
      throw new OCommandExecutionException("Class " + className + " does not exist");
    }

    if (!unsafe && clazz.count(db) > 0) {
      // check vertex or edge
      if (clazz.isVertexType()) {
        throw new OCommandExecutionException(
            "'DROP CLASS' command cannot drop class '"
                + className
                + "' because it contains Vertices. Use 'DELETE VERTEX' command first to avoid"
                + " broken edges in a database, or apply the 'UNSAFE' keyword to force it");
      } else if (clazz.isEdgeType()) {
        // FOUND EDGE CLASS
        throw new OCommandExecutionException(
            "'DROP CLASS' command cannot drop class '"
                + className
                + "' because it contains Edges. Use 'DELETE EDGE' command first to avoid broken"
                + " vertices in a database, or apply the 'UNSAFE' keyword to force it");
      }
    }

    schema.dropClass(className);

    OResultInternal result = new OResultInternal(db);
    result.setProperty("operation", "drop class");
    result.setProperty("className", className);
    return OExecutionStream.singleton(result);
  }

  @Override
  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("DROP CLASS ");
    if (name != null) {
      name.toString(params, builder);
    } else {
      nameParam.toString(params, builder);
    }
    if (ifExists) {
      builder.append(" IF EXISTS");
    }
    if (unsafe) {
      builder.append(" UNSAFE");
    }
  }

  @Override
  public void toGenericStatement(StringBuilder builder) {
    builder.append("DROP CLASS ");
    if (name != null) {
      name.toGenericStatement(builder);
    } else {
      nameParam.toGenericStatement(builder);
    }
    if (ifExists) {
      builder.append(" IF EXISTS");
    }
    if (unsafe) {
      builder.append(" UNSAFE");
    }
  }

  @Override
  public ODropClassStatement copy() {
    ODropClassStatement result = new ODropClassStatement(-1);
    result.name = name == null ? null : name.copy();
    result.nameParam = nameParam == null ? null : nameParam.copy();
    result.ifExists = ifExists;
    result.unsafe = unsafe;
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
    ODropClassStatement that = (ODropClassStatement) o;
    return ifExists == that.ifExists
        && unsafe == that.unsafe
        && Objects.equals(name, that.name)
        && Objects.equals(nameParam, that.nameParam);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, nameParam, ifExists, unsafe);
  }
}
/* JavaCC - OriginalChecksum=8c475e1225074f68be37fce610987d54 (do not edit this line) */
