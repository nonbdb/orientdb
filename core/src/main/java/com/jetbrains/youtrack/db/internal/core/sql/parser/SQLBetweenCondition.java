/* Generated By:JJTree: Do not edit this line. SQLBetweenCondition.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.api.DatabaseSession;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.api.query.Result;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class SQLBetweenCondition extends SQLBooleanExpression {

  protected SQLExpression first;
  protected SQLExpression second;
  protected SQLExpression third;

  public SQLBetweenCondition(int id) {
    super(id);
  }

  public SQLBetweenCondition(YouTrackDBSql p, int id) {
    super(p, id);
  }

  @Override
  public boolean evaluate(Identifiable currentRecord, CommandContext ctx) {
    Object firstValue = first.execute(currentRecord, ctx);
    if (firstValue == null) {
      return false;
    }

    Object secondValue = second.execute(currentRecord, ctx);
    if (secondValue == null) {
      return false;
    }

    var db = ctx.getDatabase();
    secondValue = PropertyType.convert(db, secondValue, firstValue.getClass());

    Object thirdValue = third.execute(currentRecord, ctx);
    if (thirdValue == null) {
      return false;
    }
    thirdValue = PropertyType.convert(db, thirdValue, firstValue.getClass());

    final int leftResult = ((Comparable<Object>) firstValue).compareTo(secondValue);
    final int rightResult = ((Comparable<Object>) firstValue).compareTo(thirdValue);

    return leftResult >= 0 && rightResult <= 0;
  }

  @Override
  public boolean evaluate(Result currentRecord, CommandContext ctx) {

    var db = ctx.getDatabase();
    if (first.isFunctionAny()) {
      return evaluateAny(db, currentRecord, ctx);
    }

    if (first.isFunctionAll()) {
      return evaluateAllFunction(currentRecord, ctx);
    }

    Object firstValue = first.execute(currentRecord, ctx);
    Object secondValue = second.execute(currentRecord, ctx);
    Object thirdValue = third.execute(currentRecord, ctx);

    return evaluate(db, firstValue, secondValue, thirdValue);
  }

  private static boolean evaluate(DatabaseSession session, Object firstValue, Object secondValue,
      Object thirdValue) {
    if (firstValue == null) {
      return false;
    }

    if (secondValue == null) {
      return false;
    }

    secondValue = PropertyType.convert(session, secondValue, firstValue.getClass());

    if (thirdValue == null) {
      return false;
    }
    thirdValue = PropertyType.convert(session, thirdValue, firstValue.getClass());

    final int leftResult = ((Comparable<Object>) firstValue).compareTo(secondValue);
    final int rightResult = ((Comparable<Object>) firstValue).compareTo(thirdValue);

    return leftResult >= 0 && rightResult <= 0;
  }

  private boolean evaluateAny(DatabaseSession session, Result currentRecord,
      CommandContext ctx) {
    Object secondValue = second.execute(currentRecord, ctx);
    Object thirdValue = third.execute(currentRecord, ctx);

    for (String s : currentRecord.getPropertyNames()) {
      Object firstValue = currentRecord.getProperty(s);
      if (evaluate(session, firstValue, secondValue, thirdValue)) {
        return true;
      }
    }
    return false;
  }

  private boolean evaluateAllFunction(Result currentRecord, CommandContext ctx) {
    Object secondValue = second.execute(currentRecord, ctx);
    Object thirdValue = third.execute(currentRecord, ctx);

    var database = ctx.getDatabase();
    for (String s : currentRecord.getPropertyNames()) {
      Object firstValue = currentRecord.getProperty(s);
      if (!evaluate(database, firstValue, secondValue, thirdValue)) {
        return false;
      }
    }
    return true;
  }

  public SQLExpression getFirst() {
    return first;
  }

  public void setFirst(SQLExpression first) {
    this.first = first;
  }

  public SQLExpression getSecond() {
    return second;
  }

  public void setSecond(SQLExpression second) {
    this.second = second;
  }

  public SQLExpression getThird() {
    return third;
  }

  public void setThird(SQLExpression third) {
    this.third = third;
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    first.toString(params, builder);
    builder.append(" BETWEEN ");
    second.toString(params, builder);
    builder.append(" AND ");
    third.toString(params, builder);
  }

  public void toGenericStatement(StringBuilder builder) {
    first.toGenericStatement(builder);
    builder.append(" BETWEEN ");
    second.toGenericStatement(builder);
    builder.append(" AND ");
    third.toGenericStatement(builder);
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
    if (first.needsAliases(aliases)) {
      return true;
    }
    if (second.needsAliases(aliases)) {
      return true;
    }
    return third.needsAliases(aliases);
  }

  @Override
  public SQLBooleanExpression copy() {
    SQLBetweenCondition result = new SQLBetweenCondition(-1);
    result.first = first.copy();
    result.second = second.copy();
    result.third = third.copy();
    return result;
  }

  @Override
  public void extractSubQueries(SubQueryCollector collector) {
    first.extractSubQueries(collector);
    second.extractSubQueries(collector);
    third.extractSubQueries(collector);
  }

  @Override
  public boolean refersToParent() {
    return first.refersToParent() || second.refersToParent() || third.refersToParent();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SQLBetweenCondition that = (SQLBetweenCondition) o;

    if (!Objects.equals(first, that.first)) {
      return false;
    }
    if (!Objects.equals(second, that.second)) {
      return false;
    }
    return Objects.equals(third, that.third);
  }

  @Override
  public int hashCode() {
    int result = first != null ? first.hashCode() : 0;
    result = 31 * result + (second != null ? second.hashCode() : 0);
    result = 31 * result + (third != null ? third.hashCode() : 0);
    return result;
  }

  @Override
  public List<String> getMatchPatternInvolvedAliases() {
    List<String> result = new ArrayList<String>();
    List<String> x = first.getMatchPatternInvolvedAliases();
    if (x != null) {
      result.addAll(x);
    }
    x = second.getMatchPatternInvolvedAliases();
    if (x != null) {
      result.addAll(x);
    }
    x = third.getMatchPatternInvolvedAliases();
    if (x != null) {
      result.addAll(x);
    }

    if (result.size() == 0) {
      return null;
    }
    return result;
  }

  @Override
  public void translateLuceneOperator() {
  }

  @Override
  public boolean isCacheable(DatabaseSessionInternal session) {
    if (first != null && !first.isCacheable(session)) {
      return false;
    }
    if (second != null && !second.isCacheable(session)) {
      return false;
    }
    return third == null || third.isCacheable(session);
  }
}
/* JavaCC - OriginalChecksum=f94f4779c4a6c6d09539446045ceca89 (do not edit this line) */