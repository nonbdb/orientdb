/* Generated By:JJTree: Do not edit this line. SQLDropPropertyStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.api.exception.CommandExecutionException;
import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.internal.common.comparator.CaseInsentiveComparator;
import com.jetbrains.youtrack.db.internal.common.util.Collections;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.index.Index;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.SchemaClassImpl;
import com.jetbrains.youtrack.db.internal.core.sql.executor.ResultInternal;
import com.jetbrains.youtrack.db.internal.core.sql.executor.resultset.ExecutionStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SQLDropPropertyStatement extends DDLStatement {

  protected SQLIdentifier className;
  protected SQLIdentifier propertyName;
  protected boolean ifExists = false;
  protected boolean force = false;

  public SQLDropPropertyStatement(int id) {
    super(id);
  }

  public SQLDropPropertyStatement(YouTrackDBSql p, int id) {
    super(p, id);
  }

  @Override
  public ExecutionStream executeDDL(CommandContext ctx) {

    final DatabaseSessionInternal database = ctx.getDatabase();
    final SchemaClassImpl sourceClass =
        (SchemaClassImpl) database.getMetadata().getSchema().getClass(className.getStringValue());
    if (sourceClass == null) {
      throw new CommandExecutionException("Source class '" + className + "' not found");
    }

    if (sourceClass.getProperty(propertyName.getStringValue()) == null) {
      if (ifExists) {
        return ExecutionStream.empty();
      }
      throw new CommandExecutionException(
          "Property '" + propertyName + "' not found on class " + className);
    }
    final List<Index> indexes = relatedIndexes(propertyName.getStringValue(), database);
    List<Result> rs = new ArrayList<>();
    if (!indexes.isEmpty()) {
      if (force) {
        for (final Index index : indexes) {
          database.getMetadata().getIndexManager().dropIndex(index.getName());
          ResultInternal result = new ResultInternal(database);
          result.setProperty("operation", "cascade drop index");
          result.setProperty("indexName", index.getName());
          rs.add(result);
        }
      } else {
        final StringBuilder indexNames = new StringBuilder();

        boolean first = true;
        for (final Index index :
            sourceClass.getClassInvolvedIndexesInternal(database, propertyName.getStringValue())) {
          if (!first) {
            indexNames.append(", ");
          } else {
            first = false;
          }
          indexNames.append(index.getName());
        }

        throw new CommandExecutionException(
            "Property used in indexes ("
                + indexNames
                + "). Please drop these indexes before removing property or use FORCE parameter.");
      }
    }

    // REMOVE THE PROPERTY
    sourceClass.dropProperty(database, propertyName.getStringValue());

    ResultInternal result = new ResultInternal(database);
    result.setProperty("operation", "drop property");
    result.setProperty("className", className.getStringValue());
    result.setProperty("propertyname", propertyName.getStringValue());
    rs.add(result);
    return ExecutionStream.resultIterator(rs.iterator());
  }

  private List<Index> relatedIndexes(final String fieldName, DatabaseSessionInternal database) {
    final List<Index> result = new ArrayList<Index>();
    for (final Index index :
        database
            .getMetadata()
            .getIndexManagerInternal()
            .getClassIndexes(database, className.getStringValue())) {
      if (Collections.indexOf(
          index.getDefinition().getFields(), fieldName, new CaseInsentiveComparator())
          > -1) {
        result.add(index);
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
  public SQLDropPropertyStatement copy() {
    SQLDropPropertyStatement result = new SQLDropPropertyStatement(-1);
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

    SQLDropPropertyStatement that = (SQLDropPropertyStatement) o;

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