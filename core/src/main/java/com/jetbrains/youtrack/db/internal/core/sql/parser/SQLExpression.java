/* Generated By:JJTree: Do not edit this line. SQLExpression.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.api.schema.Collate;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.api.exception.CommandExecutionException;
import com.jetbrains.youtrack.db.internal.core.id.RecordId;
import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import com.jetbrains.youtrack.db.api.record.Entity;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.SchemaClassInternal;
import com.jetbrains.youtrack.db.internal.core.sql.executor.AggregationContext;
import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.internal.core.sql.executor.ResultInternal;
import com.jetbrains.youtrack.db.internal.core.sql.executor.metadata.MetadataPath;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class SQLExpression extends SimpleNode {

  protected Boolean singleQuotes;
  protected Boolean doubleQuotes;

  protected boolean isNull = false;
  protected SQLRid rid;
  protected SQLMathExpression mathExpression;
  protected SQLArrayConcatExpression arrayConcatExpression;
  protected SQLJson json;
  protected Boolean booleanValue;

  public SQLExpression(int id) {
    super(id);
  }

  public SQLExpression(YouTrackDBSql p, int id) {
    super(p, id);
  }

  public SQLExpression(SQLIdentifier identifier) {
    mathExpression = new SQLBaseExpression(identifier);
  }

  public SQLExpression(SQLIdentifier identifier, SQLModifier modifier) {

    mathExpression = new SQLBaseExpression(identifier, modifier);
  }

  public SQLExpression(SQLRecordAttribute attr, SQLModifier modifier) {
    mathExpression = new SQLBaseExpression(attr, modifier);
  }

  public Object execute(Identifiable iCurrentRecord, CommandContext ctx) {
    if (isNull) {
      return null;
    }
    if (rid != null) {
      return rid.toRecordId(iCurrentRecord, ctx);
    }
    if (mathExpression != null) {
      return mathExpression.execute(iCurrentRecord, ctx);
    }
    if (arrayConcatExpression != null) {
      return arrayConcatExpression.execute(iCurrentRecord, ctx);
    }
    if (json != null) {
      return json.toObjectDetermineType(iCurrentRecord, ctx);
    }
    if (booleanValue != null) {
      return booleanValue;
    }
    if (value instanceof SQLNumber) {
      return ((SQLNumber) value).getValue(); // only for old executor (manually replaced params)
    }

    // from here it's old stuff, only for the old executor
    if (value instanceof SQLRid v) {
      return new RecordId(v.cluster.getValue().intValue(), v.position.getValue().longValue());
    } else if (value instanceof SQLMathExpression) {
      return ((SQLMathExpression) value).execute(iCurrentRecord, ctx);
    } else if (value instanceof SQLArrayConcatExpression) {
      return ((SQLArrayConcatExpression) value).execute(iCurrentRecord, ctx);
    } else if (value instanceof SQLJson) {
      return ((SQLJson) value).toMap(iCurrentRecord, ctx);
    } else if (value instanceof String) {
      return value;
    } else if (value instanceof Number) {
      return value;
    }

    return value;
  }

  public Object execute(Result iCurrentRecord, CommandContext ctx) {
    if (isNull) {
      return null;
    }
    if (rid != null) {
      return rid.toRecordId(iCurrentRecord, ctx);
    }
    if (mathExpression != null) {
      return mathExpression.execute(iCurrentRecord, ctx);
    }
    if (arrayConcatExpression != null) {
      return arrayConcatExpression.execute(iCurrentRecord, ctx);
    }
    if (json != null) {
      return json.toObjectDetermineType(iCurrentRecord, ctx);
    }
    if (booleanValue != null) {
      return booleanValue;
    }
    if (value instanceof SQLNumber) {
      return ((SQLNumber) value).getValue(); // only for old executor (manually replaced params)
    }

    // from here it's old stuff, only for the old executor
    if (value instanceof SQLRid v) {
      return new RecordId(v.cluster.getValue().intValue(), v.position.getValue().longValue());
    } else if (value instanceof SQLMathExpression) {
      return ((SQLMathExpression) value).execute(iCurrentRecord, ctx);
    } else if (value instanceof SQLArrayConcatExpression) {
      return ((SQLArrayConcatExpression) value).execute(iCurrentRecord, ctx);
    } else if (value instanceof SQLJson) {
      return ((SQLJson) value).toMap(iCurrentRecord, ctx);
    } else if (value instanceof String) {
      return value;
    } else if (value instanceof Number) {
      return value;
    }

    return value;
  }

  public boolean isBaseIdentifier() {
    if (mathExpression != null) {
      return mathExpression.isBaseIdentifier();
    }
    if (value instanceof SQLMathExpression) { // only backward stuff, remote it
      return ((SQLMathExpression) value).isBaseIdentifier();
    }

    return false;
  }

  public Optional<MetadataPath> getPath() {
    if (mathExpression != null) {
      return mathExpression.getPath();
    }
    if (value instanceof SQLMathExpression) {
      return ((SQLMathExpression) value).getPath();
    }

    return Optional.empty();
  }

  public boolean isEarlyCalculated(CommandContext ctx) {
    if (this.mathExpression != null) {
      return this.mathExpression.isEarlyCalculated(ctx);
    }
    if (this.arrayConcatExpression != null) {
      return this.arrayConcatExpression.isEarlyCalculated(ctx);
    }

    if (booleanValue != null) {
      return true;
    }
    if (rid != null) {
      return true;
    }
    if (value instanceof Number) {
      return true;
    }
    if (value instanceof String) {
      return true;
    }
    if (value instanceof SQLMathExpression) {
      return ((SQLMathExpression) value).isEarlyCalculated(ctx);
    }

    return false;
  }

  public SQLIdentifier getDefaultAlias() {
    SQLIdentifier identifier;
    if (isBaseIdentifier()) {
      identifier =
          new SQLIdentifier(
              ((SQLBaseExpression) mathExpression)
                  .getIdentifier()
                  .getSuffix()
                  .getIdentifier()
                  .getStringValue());
    } else {
      identifier = new SQLIdentifier(this.toString());
    }
    return identifier;
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    if (isNull) {
      builder.append("null");
    } else if (rid != null) {
      rid.toString(params, builder);
    } else if (mathExpression != null) {
      mathExpression.toString(params, builder);
    } else if (arrayConcatExpression != null) {
      arrayConcatExpression.toString(params, builder);
    } else if (json != null) {
      json.toString(params, builder);
    } else if (booleanValue != null) {
      builder.append(booleanValue);
    } else if (value instanceof SimpleNode) {
      ((SimpleNode) value)
          .toString(
              params,
              builder); // only for translated input params, will disappear with new executor
    } else if (value instanceof String) {
      if (Boolean.TRUE.equals(singleQuotes)) {
        builder.append("'" + value + "'");
      } else {
        builder.append("\"" + value + "\"");
      }

    } else {
      builder.append(
          "" + value); // only for translated input params, will disappear with new executor
    }
  }

  @Override
  public void toGenericStatement(StringBuilder builder) {
    if (isNull) {
      builder.append("null");
    } else if (rid != null) {
      rid.toGenericStatement(builder);
    } else if (mathExpression != null) {
      mathExpression.toGenericStatement(builder);
    } else if (arrayConcatExpression != null) {
      arrayConcatExpression.toGenericStatement(builder);
    } else if (json != null) {
      json.toGenericStatement(builder);
    } else if (booleanValue != null) {
      builder.append(PARAMETER_PLACEHOLDER);
    } else if (value instanceof SimpleNode) {
      ((SimpleNode) value).toGenericStatement(builder);
    } else if (value instanceof String) {
      builder.append(PARAMETER_PLACEHOLDER);
    } else {
      builder.append(PARAMETER_PLACEHOLDER);
    }
  }

  public static String encode(String s) {
    StringBuilder builder = new StringBuilder(s.length());
    for (char c : s.toCharArray()) {
      if (c == '\n') {
        builder.append("\\n");
        continue;
      }
      if (c == '\t') {
        builder.append("\\t");
        continue;
      }
      if (c == '\\' || c == '"') {
        builder.append("\\");
      }
      builder.append(c);
    }
    return builder.toString();
  }

  public boolean supportsBasicCalculation() {
    if (mathExpression != null) {
      return mathExpression.supportsBasicCalculation();
    }
    if (arrayConcatExpression != null) {
      return arrayConcatExpression.supportsBasicCalculation();
    }
    return true;
  }

  public boolean isIndexedFunctionCal(DatabaseSessionInternal session) {
    if (mathExpression != null) {
      return mathExpression.isIndexedFunctionCall(session);
    }
    return false;
  }

  public static String encodeSingle(String s) {

    StringBuilder builder = new StringBuilder(s.length());
    for (char c : s.toCharArray()) {
      if (c == '\n') {
        builder.append("\\n");
        continue;
      }
      if (c == '\t') {
        builder.append("\\t");
        continue;
      }
      if (c == '\\' || c == '\'') {
        builder.append("\\");
      }
      builder.append(c);
    }
    return builder.toString();
  }

  public long estimateIndexedFunction(
      SQLFromClause target, CommandContext context, SQLBinaryCompareOperator operator,
      Object right) {
    if (mathExpression != null) {
      return mathExpression.estimateIndexedFunction(target, context, operator, right);
    }
    return -1;
  }

  public Iterable<Identifiable> executeIndexedFunction(
      SQLFromClause target, CommandContext context, SQLBinaryCompareOperator operator,
      Object right) {
    if (mathExpression != null) {
      return mathExpression.executeIndexedFunction(target, context, operator, right);
    }
    return null;
  }

  /**
   * tests if current expression is an indexed function AND that function can also be executed
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
    if (mathExpression != null) {
      return mathExpression.canExecuteIndexedFunctionWithoutIndex(target, context, operator, right);
    }
    return false;
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
    if (mathExpression != null) {
      return mathExpression.allowsIndexedFunctionExecutionOnTarget(
          target, context, operator, right);
    }
    return false;
  }

  /**
   * tests if current expression is an indexed function AND the function has also to be executed
   * after the index search. In some cases, the index search is accurate, so this condition can be
   * excluded from further evaluation. In other cases the result from the index is a superset of the
   * expected result, so the function has to be executed anyway for further filtering
   *
   * @param target  the query target
   * @param context the execution context
   * @return true if current expression involves an indexed function AND the function has also to be
   * executed after the index search.
   */
  public boolean executeIndexedFunctionAfterIndexSearch(
      SQLFromClause target, CommandContext context, SQLBinaryCompareOperator operator,
      Object right) {
    if (mathExpression != null) {
      return mathExpression.executeIndexedFunctionAfterIndexSearch(
          target, context, operator, right);
    }
    return false;
  }

  public boolean isExpand() {
    if (mathExpression != null) {
      return mathExpression.isExpand();
    }
    return false;
  }

  public SQLExpression getExpandContent() {
    return mathExpression.getExpandContent();
  }

  public boolean needsAliases(Set<String> aliases) {
    if (mathExpression != null) {
      return mathExpression.needsAliases(aliases);
    }
    if (arrayConcatExpression != null) {
      return arrayConcatExpression.needsAliases(aliases);
    }
    if (json != null) {
      return json.needsAliases(aliases);
    }
    return false;
  }

  public boolean isAggregate(DatabaseSessionInternal session) {
    if (mathExpression != null && mathExpression.isAggregate(session)) {
      return true;
    }
    if (arrayConcatExpression != null && arrayConcatExpression.isAggregate(session)) {
      return true;
    }
    return json != null && json.isAggregate(session);
  }

  public SQLExpression splitForAggregation(
      AggregateProjectionSplit aggregateSplit, CommandContext ctx) {
    DatabaseSessionInternal database = ctx.getDatabase();
    if (isAggregate(database)) {
      SQLExpression result = new SQLExpression(-1);
      if (mathExpression != null) {
        SimpleNode splitResult = mathExpression.splitForAggregation(aggregateSplit, ctx);
        if (splitResult instanceof SQLMathExpression) {
          result.mathExpression = (SQLMathExpression) splitResult;
        } else if (splitResult instanceof SQLExpression) {
          return (SQLExpression) splitResult;
        } else {
          throw new IllegalStateException(
              "something went wrong while splitting expression for aggregate " + this);
        }
      }
      if (arrayConcatExpression != null) {
        SimpleNode splitResult = arrayConcatExpression.splitForAggregation(database,
            aggregateSplit);
        if (splitResult instanceof SQLArrayConcatExpression) {
          result.arrayConcatExpression = (SQLArrayConcatExpression) splitResult;
        } else if (splitResult instanceof SQLExpression) {
          return (SQLExpression) splitResult;
        } else {
          throw new IllegalStateException(
              "something went wrong while splitting expression for aggregate " + this);
        }
      }
      if (json != null) {
        result.json = json.splitForAggregation(aggregateSplit, ctx);
      }
      return result;
    } else {
      return this;
    }
  }

  public AggregationContext getAggregationContext(CommandContext ctx) {
    if (mathExpression != null) {
      return mathExpression.getAggregationContext(ctx);
    } else if (arrayConcatExpression != null) {
      return arrayConcatExpression.getAggregationContext(ctx);
    } else {
      throw new CommandExecutionException("Cannot aggregate on " + this);
    }
  }

  public SQLExpression copy() {

    SQLExpression result = new SQLExpression(-1);
    result.singleQuotes = singleQuotes;
    result.doubleQuotes = doubleQuotes;
    result.isNull = isNull;
    result.rid = rid == null ? null : rid.copy();
    result.mathExpression = mathExpression == null ? null : mathExpression.copy();
    result.arrayConcatExpression =
        arrayConcatExpression == null ? null : arrayConcatExpression.copy();
    result.json = json == null ? null : json.copy();
    result.booleanValue = booleanValue;

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

    SQLExpression that = (SQLExpression) o;

    if (isNull != that.isNull) {
      return false;
    }
    if (!Objects.equals(singleQuotes, that.singleQuotes)) {
      return false;
    }
    if (!Objects.equals(doubleQuotes, that.doubleQuotes)) {
      return false;
    }
    if (!Objects.equals(rid, that.rid)) {
      return false;
    }
    if (!Objects.equals(mathExpression, that.mathExpression)) {
      return false;
    }
    if (!Objects.equals(arrayConcatExpression, that.arrayConcatExpression)) {
      return false;
    }
    if (!Objects.equals(json, that.json)) {
      return false;
    }
    return Objects.equals(booleanValue, that.booleanValue);
  }

  @Override
  public int hashCode() {
    int result = singleQuotes != null ? singleQuotes.hashCode() : 0;
    result = 31 * result + (doubleQuotes != null ? doubleQuotes.hashCode() : 0);
    result = 31 * result + (isNull ? 1 : 0);
    result = 31 * result + (rid != null ? rid.hashCode() : 0);
    result = 31 * result + (mathExpression != null ? mathExpression.hashCode() : 0);
    result = 31 * result + (arrayConcatExpression != null ? arrayConcatExpression.hashCode() : 0);
    result = 31 * result + (json != null ? json.hashCode() : 0);
    result = 31 * result + (booleanValue != null ? booleanValue.hashCode() : 0);
    return result;
  }

  public void setMathExpression(SQLMathExpression mathExpression) {
    this.mathExpression = mathExpression;
  }

  public void extractSubQueries(SubQueryCollector collector) {
    if (mathExpression != null) {
      mathExpression.extractSubQueries(collector);
    }
    if (arrayConcatExpression != null) {
      arrayConcatExpression.extractSubQueries(collector);
    }
    if (json != null) {
      json.extractSubQueries(collector);
    }
  }

  public void extractSubQueries(SQLIdentifier letAlias, SubQueryCollector collector) {
    if (mathExpression != null) {
      mathExpression.extractSubQueries(letAlias, collector);
    }
    if (arrayConcatExpression != null) {
      arrayConcatExpression.extractSubQueries(collector);
    }
    if (json != null) {
      json.extractSubQueries(collector);
    }
  }

  public boolean refersToParent() {
    if (mathExpression != null && mathExpression.refersToParent()) {
      return true;
    }
    if (arrayConcatExpression != null && arrayConcatExpression.refersToParent()) {
      return true;
    }
    return json != null && json.refersToParent();
  }

  public SQLRid getRid() {
    return rid;
  }

  public void setRid(SQLRid rid) {
    this.rid = rid;
  }

  public SQLMathExpression getMathExpression() {
    return mathExpression;
  }

  /**
   * if the condition involved the current pattern (MATCH statement, eg. $matched.something = foo),
   * returns the name of involved pattern aliases ("something" in this case)
   *
   * @return a list of pattern aliases involved in this condition. Null it does not involve the
   * pattern
   */
  List<String> getMatchPatternInvolvedAliases() {
    if (mathExpression != null) {
      return mathExpression.getMatchPatternInvolvedAliases();
    }
    if (arrayConcatExpression != null) {
      return arrayConcatExpression.getMatchPatternInvolvedAliases();
    }
    return null;
  }

  public void applyRemove(ResultInternal result, CommandContext ctx) {
    if (mathExpression != null) {
      mathExpression.applyRemove(result, ctx);
    } else {
      throw new CommandExecutionException("Cannot apply REMOVE " + this);
    }
  }

  public boolean isCount() {
    if (mathExpression == null) {
      return false;
    }
    return mathExpression.isCount();
  }

  public SQLArrayConcatExpression getArrayConcatExpression() {
    return arrayConcatExpression;
  }

  public void setArrayConcatExpression(SQLArrayConcatExpression arrayConcatExpression) {
    this.arrayConcatExpression = arrayConcatExpression;
  }

  public Result serialize(DatabaseSessionInternal db) {
    ResultInternal result = new ResultInternal(db);
    result.setProperty("singleQuotes", singleQuotes);
    result.setProperty("doubleQuotes", doubleQuotes);
    result.setProperty("isNull", isNull);

    if (rid != null) {
      result.setProperty("rid", rid.serialize(db));
    }
    if (mathExpression != null) {
      result.setProperty("mathExpression", mathExpression.serialize(db));
    }
    if (arrayConcatExpression != null) {
      result.setProperty("arrayConcatExpression", arrayConcatExpression.serialize(db));
    }
    if (json != null) {
      result.setProperty("json", json.serialize(db));
    }
    result.setProperty("booleanValue", booleanValue);
    return result;
  }

  public void deserialize(Result fromResult) {
    singleQuotes = fromResult.getProperty("singleQuotes");
    doubleQuotes = fromResult.getProperty("doubleQuotes");
    isNull = fromResult.getProperty("isNull");

    if (fromResult.getProperty("rid") != null) {
      rid = new SQLRid(-1);
      rid.deserialize(fromResult.getProperty("rid"));
    }
    if (fromResult.getProperty("mathExpression") != null) {
      mathExpression =
          SQLMathExpression.deserializeFromResult(fromResult.getProperty("mathExpression"));
    }
    if (fromResult.getProperty("arrayConcatExpression") != null) {
      arrayConcatExpression = new SQLArrayConcatExpression(-1);
      arrayConcatExpression.deserialize(fromResult.getProperty("arrayConcatExpression"));
    }
    if (fromResult.getProperty("json") != null) {
      json = new SQLJson(-1);
      json.deserialize(fromResult.getProperty("json"));
    }
    booleanValue = fromResult.getProperty("booleanValue");
  }

  public boolean isDefinedFor(Result currentRecord) {
    if (mathExpression != null) {
      return mathExpression.isDefinedFor(currentRecord);
    } else {
      return true;
    }
  }

  public boolean isDefinedFor(Entity currentRecord) {
    if (mathExpression != null) {
      return mathExpression.isDefinedFor(currentRecord);
    } else {
      return true;
    }
  }

  public Collate getCollate(Result currentRecord, CommandContext ctx) {
    if (mathExpression != null) {
      return mathExpression.getCollate(currentRecord, ctx);
    }
    return null;
  }

  public boolean isCacheable(DatabaseSessionInternal session) {
    if (mathExpression != null) {
      return mathExpression.isCacheable(session);
    }
    if (arrayConcatExpression != null) {
      return arrayConcatExpression.isCacheable(session);
    }
    if (json != null) {
      return json.isCacheable();
    }

    return true;
  }

  public boolean isIndexChain(CommandContext ctx, SchemaClassInternal clazz) {
    if (mathExpression != null) {
      return mathExpression.isIndexChain(ctx, clazz);
    }
    return false;
  }

  public boolean isFunctionAny() {
    if (mathExpression != null) {
      return mathExpression.isFunctionAny();
    }
    return false;
  }

  public boolean isFunctionAll() {
    if (mathExpression != null) {
      return mathExpression.isFunctionAll();
    }
    return false;
  }
}
/* JavaCC - OriginalChecksum=9c860224b121acdc89522ae97010be01 (do not edit this line) */
