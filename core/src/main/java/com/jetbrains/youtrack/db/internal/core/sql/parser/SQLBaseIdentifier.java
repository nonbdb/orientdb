/* Generated By:JJTree: Do not edit this line. SQLBaseIdentifier.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.api.exception.CommandExecutionException;
import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.api.record.Entity;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.api.schema.Collate;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.index.Index;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.SchemaClassInternal;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.SchemaPropertyInternal;
import com.jetbrains.youtrack.db.internal.core.sql.executor.AggregationContext;
import com.jetbrains.youtrack.db.internal.core.sql.executor.ResultInternal;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class SQLBaseIdentifier extends SimpleNode {

  protected SQLLevelZeroIdentifier levelZero;

  protected SQLSuffixIdentifier suffix;

  public SQLBaseIdentifier(int id) {
    super(id);
  }

  public SQLBaseIdentifier(YouTrackDBSql p, int id) {
    super(p, id);
  }

  public SQLBaseIdentifier(SQLIdentifier identifier) {
    this.suffix = new SQLSuffixIdentifier(identifier);
  }

  public SQLBaseIdentifier(SQLRecordAttribute attr) {
    this.suffix = new SQLSuffixIdentifier(attr);
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    if (levelZero != null) {
      levelZero.toString(params, builder);
    } else if (suffix != null) {
      suffix.toString(params, builder);
    }
  }

  public void toGenericStatement(StringBuilder builder) {
    if (levelZero != null) {
      levelZero.toGenericStatement(builder);
    } else if (suffix != null) {
      suffix.toGenericStatement(builder);
    }
  }

  public Object execute(Identifiable iCurrentRecord, CommandContext ctx) {
    if (levelZero != null) {
      return levelZero.execute(iCurrentRecord, ctx);
    }
    if (suffix != null) {
      return suffix.execute(iCurrentRecord, ctx);
    }
    return null;
  }

  public Object execute(Result iCurrentRecord, CommandContext ctx) {
    if (levelZero != null) {
      return levelZero.execute(iCurrentRecord, ctx);
    }
    if (suffix != null) {
      return suffix.execute(iCurrentRecord, ctx);
    }
    return null;
  }

  public boolean isFunctionAny() {
    if (levelZero != null) {
      return levelZero.isFunctionAny();
    }
    return false;
  }

  public boolean isFunctionAll() {
    if (levelZero != null) {
      return levelZero.isFunctionAll();
    }
    return false;
  }

  public boolean isIndexedFunctionCall(DatabaseSessionInternal session) {
    if (levelZero != null) {
      return levelZero.isIndexedFunctionCall(session);
    }
    return false;
  }

  public long estimateIndexedFunction(
      SQLFromClause target, CommandContext context, SQLBinaryCompareOperator operator,
      Object right) {
    if (levelZero != null) {
      return levelZero.estimateIndexedFunction(target, context, operator, right);
    }

    return -1;
  }

  public Iterable<Identifiable> executeIndexedFunction(
      SQLFromClause target, CommandContext context, SQLBinaryCompareOperator operator,
      Object right) {
    if (levelZero != null) {
      return levelZero.executeIndexedFunction(target, context, operator, right);
    }

    return null;
  }

  /**
   * tests if current expression is an indexed funciton AND that function can also be executed
   * without using the index
   *
   * @param target   the query target
   * @param context  the execution context
   * @param operator
   * @param right
   * @return true if current expression is an indexed funciton AND that function can also be
   * executed without using the index, false otherwise
   */
  public boolean canExecuteIndexedFunctionWithoutIndex(
      SQLFromClause target, CommandContext context, SQLBinaryCompareOperator operator,
      Object right) {
    if (this.levelZero == null) {
      return false;
    }
    return levelZero.canExecuteIndexedFunctionWithoutIndex(target, context, operator, right);
  }

  /**
   * tests if current expression is an indexed function AND that function can be used on this
   * target
   *
   * @param target   the query target
   * @param context  the execution context
   * @param operator
   * @param right
   * @return true if current expression involves an indexed function AND that function can be used
   * on this target, false otherwise
   */
  public boolean allowsIndexedFunctionExecutionOnTarget(
      SQLFromClause target, CommandContext context, SQLBinaryCompareOperator operator,
      Object right) {
    if (this.levelZero == null) {
      return false;
    }
    return levelZero.allowsIndexedFunctionExecutionOnTarget(target, context, operator, right);
  }

  /**
   * tests if current expression is an indexed function AND the function has also to be executed
   * after the index search. In some cases, the index search is accurate, so this condition can be
   * excluded from further evaluation. In other cases the result from the index is a superset of the
   * expected result, so the function has to be executed anyway for further filtering
   *
   * @param target  the query target
   * @param context the execution context
   * @return true if current expression is an indexed function AND the function has also to be
   * executed after the index search.
   */
  public boolean executeIndexedFunctionAfterIndexSearch(
      SQLFromClause target, CommandContext context, SQLBinaryCompareOperator operator,
      Object right) {
    if (this.levelZero == null) {
      return false;
    }
    return levelZero.executeIndexedFunctionAfterIndexSearch(target, context, operator, right);
  }

  public boolean isBaseIdentifier() {
    return suffix != null && suffix.isBaseIdentifier();
  }

  public boolean isExpand() {
    if (levelZero != null) {
      return levelZero.isExpand();
    }
    return false;
  }

  public SQLExpression getExpandContent() {
    return levelZero.getExpandContent();
  }

  public boolean needsAliases(Set<String> aliases) {
    if (levelZero != null && levelZero.needsAliases(aliases)) {
      return true;
    }
    return suffix != null && suffix.needsAliases(aliases);
  }

  public boolean isAggregate(DatabaseSessionInternal session) {
    if (levelZero != null && levelZero.isAggregate(session)) {
      return true;
    }
    return suffix != null && suffix.isAggregate();
  }

  public boolean isCount() {
    if (levelZero != null && levelZero.isCount()) {
      return true;
    }
    return suffix != null && suffix.isCount();
  }

  public boolean isEarlyCalculated(CommandContext ctx) {
    if (levelZero != null && levelZero.isEarlyCalculated(ctx)) {
      return true;
    }
    return suffix != null && suffix.isEarlyCalculated(ctx);
  }

  public SimpleNode splitForAggregation(
      AggregateProjectionSplit aggregateProj, CommandContext ctx) {
    if (isAggregate(ctx.getDatabase())) {
      SQLBaseIdentifier result = new SQLBaseIdentifier(-1);
      if (levelZero != null) {
        SimpleNode splitResult = levelZero.splitForAggregation(aggregateProj, ctx);
        if (splitResult instanceof SQLLevelZeroIdentifier) {
          result.levelZero = (SQLLevelZeroIdentifier) splitResult;
        } else {
          return splitResult;
        }
      } else if (suffix != null) {
        result.suffix = suffix.splitForAggregation(aggregateProj);
      } else {
        throw new IllegalStateException();
      }
      return result;
    } else {
      return this;
    }
  }

  public AggregationContext getAggregationContext(CommandContext ctx) {
    if (isAggregate(ctx.getDatabase())) {
      if (levelZero != null) {
        return levelZero.getAggregationContext(ctx);
      } else if (suffix != null) {
        return suffix.getAggregationContext(ctx);
      } else {
        throw new CommandExecutionException("cannot aggregate on " + this);
      }
    } else {
      throw new CommandExecutionException("cannot aggregate on " + this);
    }
  }

  public void setLevelZero(SQLLevelZeroIdentifier levelZero) {
    this.levelZero = levelZero;
  }

  public SQLBaseIdentifier copy() {
    SQLBaseIdentifier result = new SQLBaseIdentifier(-1);
    result.levelZero = levelZero == null ? null : levelZero.copy();
    result.suffix = suffix == null ? null : suffix.copy();
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

    SQLBaseIdentifier that = (SQLBaseIdentifier) o;

    if (!Objects.equals(levelZero, that.levelZero)) {
      return false;
    }
    return Objects.equals(suffix, that.suffix);
  }

  @Override
  public int hashCode() {
    int result = levelZero != null ? levelZero.hashCode() : 0;
    result = 31 * result + (suffix != null ? suffix.hashCode() : 0);
    return result;
  }

  public boolean refersToParent() {
    if (levelZero != null && levelZero.refersToParent()) {
      return true;
    }
    return suffix != null && suffix.refersToParent();
  }

  public SQLSuffixIdentifier getSuffix() {
    return suffix;
  }

  public SQLLevelZeroIdentifier getLevelZero() {

    return levelZero;
  }

  public void applyRemove(ResultInternal result, CommandContext ctx) {
    if (suffix != null) {
      suffix.applyRemove(result, ctx);
    } else {
      throw new CommandExecutionException("cannot apply REMOVE " + this);
    }
  }

  public Result serialize(DatabaseSessionInternal db) {
    ResultInternal result = new ResultInternal(db);
    if (levelZero != null) {
      result.setProperty("levelZero", levelZero.serialize(db));
    }
    if (suffix != null) {
      result.setProperty("suffix", suffix.serialize(db));
    }
    return result;
  }

  public void deserialize(Result fromResult) {
    if (fromResult.getProperty("levelZero") != null) {
      levelZero = new SQLLevelZeroIdentifier(-1);
      levelZero.deserialize(fromResult.getProperty("levelZero"));
    }
    if (fromResult.getProperty("suffix") != null) {
      suffix = new SQLSuffixIdentifier(-1);
      suffix.deserialize(fromResult.getProperty("suffix"));
    }
  }

  public boolean isDefinedFor(Result currentRecord) {
    if (suffix != null) {
      return suffix.isDefinedFor(currentRecord);
    }
    return true;
  }

  public boolean isDefinedFor(Entity currentRecord) {
    if (suffix != null) {
      return suffix.isDefinedFor(currentRecord);
    }
    return true;
  }

  public void extractSubQueries(SQLIdentifier letAlias, SubQueryCollector collector) {
    if (this.levelZero != null) {
      this.levelZero.extractSubQueries(letAlias, collector);
    }
  }

  public void extractSubQueries(SubQueryCollector collector) {
    if (this.levelZero != null) {
      this.levelZero.extractSubQueries(collector);
    }
  }

  public Collate getCollate(Result currentRecord, CommandContext ctx) {
    return suffix == null ? null : suffix.getCollate(currentRecord, ctx);
  }

  public boolean isCacheable(DatabaseSessionInternal session) {
    if (levelZero != null) {
      return levelZero.isCacheable(session);
    }

    if (suffix != null) {
      return suffix.isCacheable();
    }

    return true;
  }

  public boolean isIndexChain(CommandContext ctx, SchemaClassInternal clazz) {
    if (suffix != null && suffix.isBaseIdentifier()) {
      SchemaPropertyInternal prop = clazz.getPropertyInternal(
          suffix.getIdentifier().getStringValue());
      if (prop == null) {
        return false;
      }
      Collection<Index> allIndexes = prop.getAllIndexesInternal(ctx.getDatabase());

      return allIndexes != null
          && allIndexes.stream().anyMatch(idx -> idx.getDefinition().getFields().size() == 1);
    }
    return false;
  }
}
/* JavaCC - OriginalChecksum=ed89af10d8be41a83428c5608a4834f6 (do not edit this line) */
