/* Generated By:JJTree: Do not edit this line. SQLWhereClause.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.api.record.RID;
import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import com.jetbrains.youtrack.db.internal.common.util.RawPair;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.index.CompositeIndexDefinition;
import com.jetbrains.youtrack.db.internal.core.index.CompositeKey;
import com.jetbrains.youtrack.db.internal.core.index.Index;
import com.jetbrains.youtrack.db.internal.core.index.IndexDefinition;
import com.jetbrains.youtrack.db.internal.core.index.PropertyIndexDefinition;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.SchemaClassInternal;
import com.jetbrains.youtrack.db.internal.core.sql.executor.ResultInternal;
import com.jetbrains.youtrack.db.internal.core.sql.executor.metadata.IndexCandidate;
import com.jetbrains.youtrack.db.internal.core.sql.executor.metadata.IndexFinder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SQLWhereClause extends SimpleNode {

  protected SQLBooleanExpression baseExpression;

  private List<SQLAndBlock> flattened;

  public SQLWhereClause(int id) {
    super(id);
  }

  public SQLWhereClause(YouTrackDBSql p, int id) {
    super(p, id);
  }

  public boolean matchesFilters(Identifiable currentRecord, CommandContext ctx) {
    if (baseExpression == null) {
      return true;
    }
    return baseExpression.evaluate(currentRecord, ctx);
  }

  public boolean matchesFilters(Result currentRecord, CommandContext ctx) {
    if (baseExpression == null) {
      return true;
    }
    return baseExpression.evaluate(currentRecord, ctx);
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    if (baseExpression == null) {
      return;
    }
    baseExpression.toString(params, builder);
  }

  public void toGenericStatement(StringBuilder builder) {
    if (baseExpression == null) {
      return;
    }
    baseExpression.toGenericStatement(builder);
  }

  /**
   * estimates how many items of this class will be returned applying this filter
   *
   * @return an estimation of the number of records of this class returned applying this filter, 0
   * if and only if sure that no records are returned
   */
  public long estimate(SchemaClassInternal oClass, long threshold, CommandContext ctx) {
    var database = ctx.getDatabase();
    var count = oClass.count(database);
    if (count > 1) {
      count = count / 2;
    }
    if (count < threshold) {
      return count;
    }

    var indexesCount = 0L;
    var flattenedConditions = flatten();
    var indexes = oClass.getIndexesInternal(database);
    for (var condition : flattenedConditions) {

      var indexedFunctConditions =
          condition.getIndexedFunctionConditions(oClass, ctx.getDatabase());

      var conditionEstimation = Long.MAX_VALUE;

      if (indexedFunctConditions != null) {
        for (var cond : indexedFunctConditions) {
          var from = new SQLFromClause(-1);
          from.item = new SQLFromItem(-1);
          from.item.setIdentifier(new SQLIdentifier(oClass.getName()));
          var newCount = cond.estimateIndexed(from, ctx);
          if (newCount < conditionEstimation) {
            conditionEstimation = newCount;
          }
        }
      } else {
        var conditions = getEqualityOperations(condition, ctx);

        for (var index : indexes) {
          var indexedFields = index.getDefinition().getFields();
          var nMatchingKeys = 0;
          for (var indexedField : indexedFields) {
            if (conditions.containsKey(indexedField)) {
              nMatchingKeys++;
            } else {
              break;
            }
          }
          if (nMatchingKeys > 0) {
            var newCount = estimateFromIndex(database, index, conditions, nMatchingKeys);
            if (newCount < conditionEstimation) {
              conditionEstimation = newCount;
            }
          }
        }
      }
      if (conditionEstimation > count) {
        return count;
      }
      indexesCount += conditionEstimation;
    }
    return Math.min(indexesCount, count);
  }

  private static long estimateFromIndex(
      DatabaseSessionInternal session, Index index, Map<String, Object> conditions,
      int nMatchingKeys) {
    if (nMatchingKeys < 1) {
      throw new IllegalArgumentException("Cannot estimate from an index with zero keys");
    }
    var definition = index.getDefinition();
    var definitionFields = definition.getFields();
    Object key = null;
    if (definition instanceof PropertyIndexDefinition) {
      key = convert(session, conditions.get(definitionFields.get(0)), definition.getTypes()[0]);
    } else if (definition instanceof CompositeIndexDefinition) {
      key = new CompositeKey();
      for (var i = 0; i < nMatchingKeys; i++) {
        var keyValue =
            convert(session, conditions.get(definitionFields.get(i)), definition.getTypes()[i]);
        ((CompositeKey) key).addKey(keyValue);
      }
    }
    if (key != null) {
      if (conditions.size() == definitionFields.size()) {
        try (var rids = index.getInternal().getRids(session, key)) {
          return rids.count();
        }
      } else if (index.supportsOrderedIterations()) {
        final Spliterator<RawPair<Object, RID>> spliterator;

        try (var stream =
            index.getInternal().streamEntriesBetween(session, key, true, key, true, true)) {
          spliterator = stream.spliterator();
          return spliterator.estimateSize();
        }
      }
    }
    return Long.MAX_VALUE;
  }

  private static Object convert(DatabaseSessionInternal session, Object o, PropertyType oType) {
    return PropertyType.convert(session, o, oType.getDefaultJavaType());
  }

  private static Map<String, Object> getEqualityOperations(
      SQLAndBlock condition, CommandContext ctx) {
    Map<String, Object> result = new HashMap<>();
    for (var expression : condition.subBlocks) {
      if (expression instanceof SQLBinaryCondition b) {
        if (b.operator instanceof SQLEqualsCompareOperator) {
          if (b.left.isBaseIdentifier() && b.right.isEarlyCalculated(ctx)) {
            result.put(b.left.toString(), b.right.execute((Result) null, ctx));
          }
        }
      }
    }
    return result;
  }

  public List<SQLAndBlock> flatten() {
    if (this.baseExpression == null) {
      return Collections.emptyList();
    }
    if (flattened == null) {
      flattened = this.baseExpression.flatten();
    }
    // TODO remove false conditions (contraddictions)
    return flattened;
  }

  public List<SQLBinaryCondition> getIndexedFunctionConditions(
      SchemaClass iSchemaClass, DatabaseSessionInternal database) {
    if (baseExpression == null) {
      return null;
    }
    return this.baseExpression.getIndexedFunctionConditions(iSchemaClass, database);
  }

  public boolean needsAliases(Set<String> aliases) {
    return this.baseExpression.needsAliases(aliases);
  }

  public void setBaseExpression(SQLBooleanExpression baseExpression) {
    this.baseExpression = baseExpression;
  }

  public SQLWhereClause copy() {
    var result = new SQLWhereClause(-1);
    result.baseExpression = baseExpression.copy();
    result.flattened =
        Optional.ofNullable(flattened)
            .map(
                oAndBlocks -> {
                  try (var stream = oAndBlocks.stream()) {
                    return stream.map(SQLAndBlock::copy).collect(Collectors.toList());
                  }
                })
            .orElse(null);
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

    var that = (SQLWhereClause) o;

    if (!Objects.equals(baseExpression, that.baseExpression)) {
      return false;
    }
    return Objects.equals(flattened, that.flattened);
  }

  @Override
  public int hashCode() {
    int result = Optional.ofNullable(baseExpression).map(Object::hashCode).orElse(0);
    result = 31 * result + (Optional.ofNullable(flattened).map(List::hashCode).orElse(0));
    return result;
  }

  public void extractSubQueries(SubQueryCollector collector) {
    if (baseExpression != null) {
      baseExpression.extractSubQueries(collector);
    }
    flattened = null;
  }

  public boolean refersToParent() {
    return baseExpression != null && baseExpression.refersToParent();
  }

  public SQLBooleanExpression getBaseExpression() {
    return baseExpression;
  }

  public List<SQLAndBlock> getFlattened() {
    return flattened;
  }

  public void setFlattened(List<SQLAndBlock> flattened) {
    this.flattened = flattened;
  }

  public Result serialize(DatabaseSessionInternal db) {
    var result = new ResultInternal(db);
    if (baseExpression != null) {
      result.setProperty("baseExpression", baseExpression.serialize(db));
    }
    if (flattened != null) {
      try (var stream = flattened.stream()) {
        result.setProperty(
            "flattened",
            stream.map(oAndBlock -> oAndBlock.serialize(db)).collect(Collectors.toList()));
      }
    }
    return result;
  }

  public void deserialize(Result fromResult) {
    if (fromResult.getProperty("baseExpression") != null) {
      baseExpression =
          SQLBooleanExpression.deserializeFromOResult(fromResult.getProperty("baseExpression"));
    }
    if (fromResult.getProperty("flattened") != null) {
      List<Result> ser = fromResult.getProperty("flattened");
      flattened = new ArrayList<>();
      for (var r : ser) {
        var block = new SQLAndBlock(-1);
        block.deserialize(r);
        flattened.add(block);
      }
    }
  }

  public boolean isCacheable(DatabaseSessionInternal session) {
    return baseExpression.isCacheable(session);
  }

  public Optional<IndexCandidate> findIndex(IndexFinder info, CommandContext ctx) {
    return this.baseExpression.findIndex(info, ctx);
  }
}
/* JavaCC - OriginalChecksum=e8015d01ce1ab2bc337062e9e3f2603e (do not edit this line) */
