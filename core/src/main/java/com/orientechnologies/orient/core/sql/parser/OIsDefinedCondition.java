/* Generated By:JJTree: Do not edit this line. OIsDefinedCondition.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.orientechnologies.orient.core.sql.parser;

import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.exception.ORecordNotFoundException;
import com.orientechnologies.orient.core.record.OElement;
import com.orientechnologies.orient.core.sql.executor.OResult;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class OIsDefinedCondition extends OBooleanExpression implements OSimpleBooleanExpression {

  protected OExpression expression;

  public OIsDefinedCondition(int id) {
    super(id);
  }

  public OIsDefinedCondition(OrientSql p, int id) {
    super(p, id);
  }

  @Override
  public boolean evaluate(OIdentifiable currentRecord, OCommandContext ctx) {
    Object elem;
    try {
      elem = currentRecord.getRecord();
    } catch (ORecordNotFoundException rnf) {
      return false;
    }

    if (elem instanceof OElement) {
      return expression.isDefinedFor((OElement) elem);
    }

    return false;
  }

  @Override
  public boolean evaluate(OResult currentRecord, OCommandContext ctx) {
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
  public OIsDefinedCondition copy() {
    OIsDefinedCondition result = new OIsDefinedCondition(-1);
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

    OIsDefinedCondition that = (OIsDefinedCondition) o;

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
  public boolean isCacheable() {
    return expression.isCacheable();
  }
}
/* JavaCC - OriginalChecksum=075954b212c8cb44c8538bf5dea047d3 (do not edit this line) */
