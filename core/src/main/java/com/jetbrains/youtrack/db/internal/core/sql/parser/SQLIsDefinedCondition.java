/* Generated By:JJTree: Do not edit this line. SQLIsDefinedCondition.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.api.exception.RecordNotFoundException;
import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.api.record.Entity;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class SQLIsDefinedCondition extends SQLBooleanExpression implements
    SimpleBooleanExpression {

  protected SQLExpression expression;

  public SQLIsDefinedCondition(int id) {
    super(id);
  }

  public SQLIsDefinedCondition(YouTrackDBSql p, int id) {
    super(p, id);
  }

  @Override
  public boolean evaluate(Identifiable currentRecord, CommandContext ctx) {
    var db = ctx.getDatabase();
    Object elem;
    try {
      elem = currentRecord.getRecord(db);
    } catch (RecordNotFoundException rnf) {
      return false;
    }

    if (elem instanceof Entity) {
      return expression.isDefinedFor(db, (Entity) elem);
    }

    return false;
  }

  @Override
  public boolean evaluate(Result currentRecord, CommandContext ctx) {
    if (expression.isFunctionAny()) {
      return !currentRecord.getPropertyNames().isEmpty();
    }
    if (expression.isFunctionAll()) {
      return true;
    }
    return expression.isDefinedFor(currentRecord);
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    expression.toString(params, builder);
    builder.append(" is defined");
  }

  public void toGenericStatement(StringBuilder builder) {
    expression.toGenericStatement(builder);
    builder.append(" is defined");
  }

  @Override
  public boolean supportsBasicCalculation() {
    return true;
  }

  @Override
  protected int getNumberOfExternalCalculations() {
    return 0;
  }

  @Override
  protected List<Object> getExternalCalculationConditions() {
    return Collections.EMPTY_LIST;
  }

  @Override
  public boolean needsAliases(Set<String> aliases) {
    return expression.needsAliases(aliases);
  }

  @Override
  public SQLIsDefinedCondition copy() {
    var result = new SQLIsDefinedCondition(-1);
    result.expression = expression.copy();
    return result;
  }

  @Override
  public void extractSubQueries(SubQueryCollector collector) {
    this.expression.extractSubQueries(collector);
  }

  @Override
  public boolean refersToParent() {
    return expression != null && expression.refersToParent();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    var that = (SQLIsDefinedCondition) o;

    return Objects.equals(expression, that.expression);
  }

  @Override
  public int hashCode() {
    return expression != null ? expression.hashCode() : 0;
  }

  @Override
  public List<String> getMatchPatternInvolvedAliases() {
    return expression.getMatchPatternInvolvedAliases();
  }

  @Override
  public boolean isCacheable(DatabaseSessionInternal session) {
    return expression.isCacheable(session);
  }
}
/* JavaCC - OriginalChecksum=075954b212c8cb44c8538bf5dea047d3 (do not edit this line) */
