/* Generated By:JJTree: Do not edit this line. ODropIndexStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.orientechnologies.orient.core.sql.parser;

import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.YTDatabaseSessionInternal;
import com.orientechnologies.orient.core.exception.YTCommandExecutionException;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.index.OIndexManagerAbstract;
import com.orientechnologies.orient.core.sql.executor.OResult;
import com.orientechnologies.orient.core.sql.executor.OResultInternal;
import com.orientechnologies.orient.core.sql.executor.resultset.OExecutionStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ODropIndexStatement extends ODDLStatement {

  protected boolean all = false;
  protected OIndexName name;
  protected boolean ifExists = false;

  public ODropIndexStatement(int id) {
    super(id);
  }

  public ODropIndexStatement(OrientSql p, int id) {
    super(p, id);
  }

  @Override
  public OExecutionStream executeDDL(OCommandContext ctx) {
    List<OResult> rs = new ArrayList<>();
    YTDatabaseSessionInternal db = ctx.getDatabase();
    OIndexManagerAbstract idxMgr = db.getMetadata().getIndexManagerInternal();
    if (all) {
      for (OIndex idx : idxMgr.getIndexes(db)) {
        db.getMetadata().getIndexManagerInternal().dropIndex(db, idx.getName());
        OResultInternal result = new OResultInternal(db);
        result.setProperty("operation", "drop index");
        result.setProperty("clusterName", idx.getName());
        rs.add(result);
      }

    } else {
      if (!idxMgr.existsIndex(name.getValue()) && !ifExists) {
        throw new YTCommandExecutionException("Index not found: " + name.getValue());
      }
      idxMgr.dropIndex(db, name.getValue());
      OResultInternal result = new OResultInternal(db);
      result.setProperty("operation", "drop index");
      result.setProperty("indexName", name.getValue());
      rs.add(result);
    }

    return OExecutionStream.resultIterator(rs.iterator());
  }

  @Override
  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("DROP INDEX ");
    if (all) {
      builder.append("*");
    } else {
      name.toString(params, builder);
    }
    if (ifExists) {
      builder.append(" IF EXISTS");
    }
  }

  @Override
  public void toGenericStatement(StringBuilder builder) {
    builder.append("DROP INDEX ");
    if (all) {
      builder.append("*");
    } else {
      name.toGenericStatement(builder);
    }
    if (ifExists) {
      builder.append(" IF EXISTS");
    }
  }

  @Override
  public ODropIndexStatement copy() {
    ODropIndexStatement result = new ODropIndexStatement(-1);
    result.all = all;
    result.name = name == null ? null : name.copy();
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

    ODropIndexStatement that = (ODropIndexStatement) o;

    if (all != that.all) {
      return false;
    }
    return Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    int result = (all ? 1 : 0);
    result = 31 * result + (name != null ? name.hashCode() : 0);
    return result;
  }
}
/* JavaCC - OriginalChecksum=51c8221d049e4f114378e4be03797050 (do not edit this line) */
