/* Generated By:JJTree: Do not edit this line. OBetweenCondition.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.orientechnologies.orient.core.sql.parser;

import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.sql.executor.OResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OBetweenCondition extends OBooleanExpression {

  protected OExpression first;
  protected OExpression second;
  protected OExpression third;

  public OBetweenCondition(int id) {
    super(id);
  }

  public OBetweenCondition(OrientSql p, int id) {
    super(p, id);
  }

  @Override
  public boolean evaluate(OIdentifiable currentRecord, OCommandContext ctx) {
    Object firstValue = first.execute(currentRecord, ctx);
    if (firstValue == null) {
      return false;
    }

    Object secondValue = second.execute(currentRecord, ctx);
    if (secondValue == null) {
      return false;
    }

    secondValue = OType.convert(secondValue, firstValue.getClass());

    Object thirdValue = third.execute(currentRecord, ctx);
    if (thirdValue == null) {
      return false;
    }
    thirdValue = OType.convert(thirdValue, firstValue.getClass());

    final int leftResult = ((Comparable<Object>) firstValue).compareTo(secondValue);
    final int rightResult = ((Comparable<Object>) firstValue).compareTo(thirdValue);

    return leftResult >= 0 && rightResult <= 0;
  }

  @Override
  public boolean evaluate(OResult currentRecord, OCommandContext ctx) {

    if (first.isFunctionAny()) {
      return evaluateAny(currentRecord, ctx);
    }

    if (first.isFunctionAll()) {
      return evaluateAllFunction(currentRecord, ctx);
    }

    Object firstValue = first.execute(currentRecord, ctx);
    Object secondValue = second.execute(currentRecord, ctx);
    Object thirdValue = third.execute(currentRecord, ctx);

    return evaluate(firstValue, secondValue, thirdValue);
  }

  private boolean evaluate(Object firstValue, Object secondValue, Object thirdValue) {
    if (firstValue == null) {
      return false;
    }

    if (secondValue == null) {
      return false;
    }

    secondValue = OType.convert(secondValue, firstValue.getClass());

    if (thirdValue == null) {
      return false;
    }
    thirdValue = OType.convert(thirdValue, firstValue.getClass());

    final int leftResult = ((Comparable<Object>) firstValue).compareTo(secondValue);
    final int rightResult = ((Comparable<Object>) firstValue).compareTo(thirdValue);

    return leftResult >= 0 && rightResult <= 0;
  }

  private boolean evaluateAny(OResult currentRecord, OCommandContext ctx) {
    Object secondValue = second.execute(currentRecord, ctx);
    Object thirdValue = third.execute(currentRecord, ctx);

    for (String s : currentRecord.getPropertyNames()) {
      Object firstValue = currentRecord.getProperty(s);
      if (evaluate(firstValue, secondValue, thirdValue)) {
        return true;
      }
    }
    return false;
  }

  private boolean evaluateAllFunction(OResult currentRecord, OCommandContext ctx) {
    Object secondValue = second.execute(currentRecord, ctx);
    Object thirdValue = third.execute(currentRecord, ctx);

    for (String s : currentRecord.getPropertyNames()) {
      Object firstValue = currentRecord.getProperty(s);
      if (!evaluate(firstValue, secondValue, thirdValue)) {
        return false;
      }
    }
    return true;
  }

  public OExpression getFirst() {
    return first;
  }

  public void setFirst(OExpression first) {
    this.first = first;
  }

  public OExpression getSecond() {
    return second;
  }

  public void setSecond(OExpression second) {
    this.second = second;
  }

  public OExpression getThird() {
    return third;
  }

  public void setThird(OExpression third) {
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
    if (third.needsAliases(aliases)) {
      return true;
    }
    return false;
  }

  @Override
  public OBooleanExpression copy() {
    OBetweenCondition result = new OBetweenCondition(-1);
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

    OBetweenCondition that = (OBetweenCondition) o;

    if (first != null ? !first.equals(that.first) : that.first != null) {
      return false;
    }
    if (second != null ? !second.equals(that.second) : that.second != null) {
      return false;
    }
    if (third != null ? !third.equals(that.third) : that.third != null) {
      return false;
    }

    return true;
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
  public void translateLuceneOperator() {}

  @Override
  public boolean isCacheable() {
    if (first != null && !first.isCacheable()) {
      return false;
    }
    if (second != null && !second.isCacheable()) {
      return false;
    }
    if (third != null && !third.isCacheable()) {
      return false;
    }
    return true;
  }
}
/* JavaCC - OriginalChecksum=f94f4779c4a6c6d09539446045ceca89 (do not edit this line) */
