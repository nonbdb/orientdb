/* Generated By:JJTree: Do not edit this line. ODropPropertyStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.orientechnologies.orient.core.sql.parser;

import com.orientechnologies.common.comparator.OCaseInsentiveComparator;
import com.orientechnologies.common.util.OCollections;
import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.ODatabaseSessionInternal;
import com.orientechnologies.orient.core.exception.OCommandExecutionException;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.metadata.schema.OClassImpl;
import com.orientechnologies.orient.core.sql.executor.OResult;
import com.orientechnologies.orient.core.sql.executor.OResultInternal;
import com.orientechnologies.orient.core.sql.executor.resultset.OExecutionStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ODropPropertyStatement extends ODDLStatement {

  protected OIdentifier className;
  protected OIdentifier propertyName;
  protected boolean ifExists = false;
  protected boolean force = false;

  public ODropPropertyStatement(int id) {
    super(id);
  }

  public ODropPropertyStatement(OrientSql p, int id) {
    super(p, id);
  }

  @Override
  public OExecutionStream executeDDL(OCommandContext ctx) {

    final ODatabaseSessionInternal database = ctx.getDatabase();
    final OClassImpl sourceClass =
        (OClassImpl) database.getMetadata().getSchema().getClass(className.getStringValue());
    if (sourceClass == null) {
      throw new OCommandExecutionException("Source class '" + className + "' not found");
    }

    if (sourceClass.getProperty(propertyName.getStringValue()) == null) {
      if (ifExists) {
        return OExecutionStream.empty();
      }
      throw new OCommandExecutionException(
          "Property '" + propertyName + "' not found on class " + className);
    }
    final List<OIndex> indexes = relatedIndexes(propertyName.getStringValue(), database);
    List<OResult> rs = new ArrayList<>();
    if (!indexes.isEmpty()) {
      if (force) {
        for (final OIndex index : indexes) {
          database.getMetadata().getIndexManager().dropIndex(index.getName());
          OResultInternal result = new OResultInternal();
          result.setProperty("operation", "cascade drop index");
          result.setProperty("indexName", index.getName());
          rs.add(result);
        }
      } else {
        final StringBuilder indexNames = new StringBuilder();

        boolean first = true;
        for (final OIndex index :
            sourceClass.getClassInvolvedIndexes(propertyName.getStringValue())) {
          if (!first) {
            indexNames.append(", ");
          } else {
            first = false;
          }
          indexNames.append(index.getName());
        }

        throw new OCommandExecutionException(
            "Property used in indexes ("
                + indexNames
                + "). Please drop these indexes before removing property or use FORCE parameter.");
      }
    }

    // REMOVE THE PROPERTY
    sourceClass.dropProperty(propertyName.getStringValue());

    OResultInternal result = new OResultInternal();
    result.setProperty("operation", "drop property");
    result.setProperty("className", className.getStringValue());
    result.setProperty("propertyname", propertyName.getStringValue());
    rs.add(result);
    return OExecutionStream.resultIterator(rs.iterator());
  }

  private List<OIndex> relatedIndexes(final String fieldName, ODatabaseSessionInternal database) {
    final List<OIndex> result = new ArrayList<OIndex>();
    for (final OIndex oIndex :
        database
            .getMetadata()
            .getIndexManagerInternal()
            .getClassIndexes(database, className.getStringValue())) {
      if (OCollections.indexOf(
              oIndex.getDefinition().getFields(), fieldName, new OCaseInsentiveComparator())
          > -1) {
        result.add(oIndex);
      }
    }

    return result;
  }

  @Override
  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("DROP PROPERTY ");
    className.toString(params, builder);
    builder.append(".");
    propertyName.toString(params, builder);
    if (ifExists) {
      builder.append(" IF EXISTS");
    }
    if (force) {
      builder.append(" FORCE");
    }
  }

  @Override
  public void toGenericStatement(StringBuilder builder) {
    builder.append("DROP PROPERTY ");
    className.toGenericStatement(builder);
    builder.append(".");
    propertyName.toGenericStatement(builder);
    if (ifExists) {
      builder.append(" IF EXISTS");
    }
    if (force) {
      builder.append(" FORCE");
    }
  }

  @Override
  public ODropPropertyStatement copy() {
    ODropPropertyStatement result = new ODropPropertyStatement(-1);
    result.className = className == null ? null : className.copy();
    result.propertyName = propertyName == null ? null : propertyName.copy();
    result.force = force;
    result.ifExists = ifExists;
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

    ODropPropertyStatement that = (ODropPropertyStatement) o;

    if (force != that.force) {
      return false;
    }
    if (ifExists != that.ifExists) {
      return false;
    }
    if (!Objects.equals(className, that.className)) {
      return false;
    }
    return Objects.equals(propertyName, that.propertyName);
  }

  @Override
  public int hashCode() {
    int result = className != null ? className.hashCode() : 0;
    result = 31 * result + (propertyName != null ? propertyName.hashCode() : 0);
    result = 31 * result + (force ? 1 : 0);
    return result;
  }
}
/* JavaCC - OriginalChecksum=6a9b4b1694dc36caf2b801218faebe42 (do not edit this line) */
