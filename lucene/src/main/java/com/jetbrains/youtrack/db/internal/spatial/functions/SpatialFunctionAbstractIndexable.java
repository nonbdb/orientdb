/**
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * <p>*
 */
package com.jetbrains.youtrack.db.internal.spatial.functions;

import com.jetbrains.youtrack.db.api.exception.CommandExecutionException;
import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseRecordThreadLocal;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.index.Index;
import com.jetbrains.youtrack.db.internal.core.metadata.MetadataInternal;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.sql.executor.ResultInternal;
import com.jetbrains.youtrack.db.internal.core.sql.functions.IndexableSQLFunction;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLBinaryCompareOperator;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLExpression;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLFromClause;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLFromItem;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLIdentifier;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLJson;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLLeOperator;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLLtOperator;
import com.jetbrains.youtrack.db.internal.lucene.collections.LuceneResultSetEmpty;
import com.jetbrains.youtrack.db.internal.spatial.index.LuceneSpatialIndex;
import com.jetbrains.youtrack.db.internal.spatial.strategy.SpatialQueryBuilderAbstract;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 */
public abstract class SpatialFunctionAbstractIndexable extends SpatialFunctionAbstract
    implements IndexableSQLFunction {

  public SpatialFunctionAbstractIndexable(String iName, int iMinParams, int iMaxParams) {
    super(iName, iMinParams, iMaxParams);
  }

  protected LuceneSpatialIndex searchForIndex(DatabaseSessionInternal session,
      SQLFromClause target,
      SQLExpression[] args) {
    MetadataInternal dbMetadata = getDb().getMetadata();

    SQLFromItem item = target.getItem();
    SQLIdentifier identifier = item.getIdentifier();
    String fieldName = args[0].toString();

    String className = identifier.getStringValue();
    List<LuceneSpatialIndex> indices =
        dbMetadata.getImmutableSchemaSnapshot().getClassInternal(className)
            .getIndexesInternal(session).stream()
            .filter(idx -> idx instanceof LuceneSpatialIndex)
            .map(idx -> (LuceneSpatialIndex) idx)
            .filter(
                idx ->
                    intersect(
                        idx.getDefinition().getFields(), Collections.singletonList(fieldName)))
            .toList();

    if (indices.size() > 1) {
      throw new IllegalArgumentException(
          "too many indices matching given field name: " + String.join(",", fieldName));
    }

    return indices.isEmpty() ? null : indices.get(0);
  }

  protected DatabaseSessionInternal getDb() {
    return DatabaseRecordThreadLocal.instance().get();
  }

  protected Iterable<Identifiable> results(
      SQLFromClause target, SQLExpression[] args, CommandContext ctx, Object rightValue) {
    Index index = searchForIndex(ctx.getDatabase(), target, args);

    if (index == null) {
      return null;
    }

    Map<String, Object> queryParams = new HashMap<>();
    queryParams.put(SpatialQueryBuilderAbstract.GEO_FILTER, operator());
    Object shape;
    var db = ctx.getDatabase();
    if (args[1].getValue() instanceof SQLJson json) {
      EntityImpl doc = new EntityImpl(null);
      doc.fromJSON(json.toString());
      shape = doc.toMap();
    } else {
      shape = args[1].execute((Identifiable) null, ctx);
    }

    if (shape instanceof Collection) {
      int size = ((Collection) shape).size();

      if (size == 0) {
        return new LuceneResultSetEmpty();
      }
      if (size == 1) {

        Object next = ((Collection) shape).iterator().next();

        if (next instanceof Result inner) {
          var propertyNames = inner.getPropertyNames();
          if (propertyNames.size() == 1) {
            Object property = inner.getProperty(propertyNames.iterator().next());
            if (property instanceof Result) {
              shape = ((Result) property).asEntity();
            }
          } else {
            return new LuceneResultSetEmpty();
          }
        }
      } else {
        throw new CommandExecutionException("The collection in input cannot be major than 1");
      }
    }

    if (shape instanceof ResultInternal) {
      shape = ((ResultInternal) shape).asEntity();
    }
    queryParams.put(SpatialQueryBuilderAbstract.SHAPE, shape);

    onAfterParsing(queryParams, args, ctx, rightValue);

    Set<String> indexes = (Set<String>) ctx.getVariable("involvedIndexes");
    if (indexes == null) {
      indexes = new HashSet<>();
      ctx.setVariable("involvedIndexes", indexes);
    }
    indexes.add(index.getName());
    return index.getInternal().getRids(ctx.getDatabase(), queryParams).collect(Collectors.toSet());
  }

  protected void onAfterParsing(
      Map<String, Object> params, SQLExpression[] args, CommandContext ctx, Object rightValue) {
  }

  protected abstract String operator();

  @Override
  public boolean canExecuteInline(
      SQLFromClause target,
      SQLBinaryCompareOperator operator,
      Object rightValue,
      CommandContext ctx,
      SQLExpression... args) {

    return true;
  }

  @Override
  public boolean allowsIndexedExecution(
      SQLFromClause target,
      SQLBinaryCompareOperator operator,
      Object rightValue,
      CommandContext ctx,
      SQLExpression... args) {

    if (!isValidBinaryOperator(operator)) {
      return false;
    }
    LuceneSpatialIndex index = searchForIndex(ctx.getDatabase(), target, args);

    return index != null;
  }

  @Override
  public boolean shouldExecuteAfterSearch(
      SQLFromClause target,
      SQLBinaryCompareOperator operator,
      Object rightValue,
      CommandContext ctx,
      SQLExpression... args) {
    return true;
  }

  @Override
  public long estimate(
      SQLFromClause target,
      SQLBinaryCompareOperator operator,
      Object rightValue,
      CommandContext ctx,
      SQLExpression... args) {

    LuceneSpatialIndex index = searchForIndex(ctx.getDatabase(), target, args);
    return index == null ? -1 : index.size(ctx.getDatabase());
  }

  public static <T> boolean intersect(List<T> list1, List<T> list2) {
    for (T t : list1) {
      if (list2.contains(t)) {
        return true;
      }
    }

    return false;
  }

  protected boolean isValidBinaryOperator(SQLBinaryCompareOperator operator) {
    return operator instanceof SQLLtOperator || operator instanceof SQLLeOperator;
  }
}
