/* Generated By:JJTree: Do not edit this line. SQLNotInCondition.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.api.query.Result;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class SQLNotInCondition extends SQLBooleanExpression {

  protected SQLExpression left;
  protected SQLBinaryCompareOperator operator;
  protected SQLSelectStatement rightStatement;

  protected Object right;
  protected SQLInputParameter rightParam;
  protected SQLMathExpression rightMathExpression;

  private static final Object UNSET = new Object();
  private final Object inputFinalValue = UNSET;

  public SQLNotInCondition(int id) {
    super(id);
  }

  public SQLNotInCondition(YouTrackDBSql p, int id) {
    super(p, id);
  }

  @Override
  public boolean evaluate(Identifiable currentRecord, CommandContext ctx) {
    Object leftVal = left.execute(currentRecord, ctx);
    Object rightVal = null;
    if (rightStatement != null) {
      rightVal = SQLInCondition.executeQuery(rightStatement, ctx);
    } else if (rightParam != null) {
      rightVal = rightParam.getValue(ctx.getInputParameters());
    } else if (rightMathExpression != null) {
      rightVal = rightMathExpression.execute(currentRecord, ctx);
    }
    if (rightVal == null) {
      return true;
    }
    return !SQLInCondition.evaluateExpression(ctx.getDatabase(), leftVal, rightVal);
  }

  @Override
  public boolean evaluate(Result currentRecord, CommandContext ctx) {
    Object leftVal = left.execute(currentRecord, ctx);
    Object rightVal = null;
    if (rightStatement != null) {
      rightVal = SQLInCondition.executeQuery(rightStatement, ctx);
    } else if (rightParam != null) {
      rightVal = rightParam.getValue(ctx.getInputParameters());
    } else if (rightMathExpression != null) {
      rightVal = rightMathExpression.execute(currentRecord, ctx);
    }
    if (rightVal == null) {
      return true;
    }
    return !SQLInCondition.evaluateExpression(ctx.getDatabase(), leftVal, rightVal);
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {

    left.toString(params, builder);
    builder.append(" NOT IN ");
    if (rightStatement != null) {
      builder.append("(");
      rightStatement.toString(params, builder);
      builder.append(")");
    } else if (right != null) {
      builder.append(convertToString(right));
    } else if (rightParam != null) {
      rightParam.toString(params, builder);
    } else if (rightMathExpression != null) {
      rightMathExpression.toString(params, builder);
    }
  }

  public void toGenericStatement(StringBuilder builder) {

    left.toGenericStatement(builder);
    builder.append(" NOT IN ");
    if (rightStatement != null) {
      builder.append("(");
      rightStatement.toGenericStatement(builder);
      builder.append(")");
    } else if (right != null) {
      builder.append(PARAMETER_PLACEHOLDER);
    } else if (rightParam != null) {
      rightParam.toGenericStatement(builder);
    } else if (rightMathExpression != null) {
      rightMathExpression.toGenericStatement(builder);
    }
  }

  private String convertToString(Object o) {
    if (o instanceof String) {
      return "\"" + ((String) o).replaceAll("\"", "\\\"") + "\"";
    }
    return o.toString();
  }

  @Override
  public boolean supportsBasicCalculation() {

    if (operator != null && !operator.supportsBasicCalculation()) {
      return false;
    }
    if (left != null && !left.supportsBasicCalculation()) {
      return false;
    }
    return rightMathExpression == null || rightMathExpression.supportsBasicCalculation();
  }

  @Override
  protected int getNumberOfExternalCalculations() {
    int total = 0;
    if (operator != null && !operator.supportsBasicCalculation()) {
      total++;
    }
    if (left != null && !left.supportsBasicCalculation()) {
      total++;
    }
    if (rightMathExpression != null && !rightMathExpression.supportsBasicCalculation()) {
      total++;
    }
    return total;
  }

  @Override
  protected List<Object> getExternalCalculationConditions() {
    List<Object> result = new ArrayList<Object>();
    if (operator != null && !operator.supportsBasicCalculation()) {
      result.add(this);
    }
    if (rightMathExpression != null && !rightMathExpression.supportsBasicCalculation()) {
      result.add(rightMathExpression);
    }
    return result;
  }

  @Override
  public boolean needsAliases(Set<String> aliases) {
    if (left.needsAliases(aliases)) {
      return true;
    }

    return rightMathExpression != null && rightMathExpression.needsAliases(aliases);
  }

  @Override
  public SQLNotInCondition copy() {
    SQLNotInCondition result = new SQLNotInCondition(-1);
    result.operator = operator == null ? null : operator.copy();
    result.left = left == null ? null : left.copy();
    result.rightMathExpression = rightMathExpression == null ? null : rightMathExpression.copy();
    result.rightStatement = rightStatement == null ? null : rightStatement.copy();
    result.rightParam = rightParam == null ? null : rightParam.copy();
    result.right = right == null ? null : right;
    return result;
  }

  @Override
  public void extractSubQueries(SubQueryCollector collector) {
    if (left != null) {
      left.extractSubQueries(collector);
    }

    if (rightMathExpression != null) {
      rightMathExpression.extractSubQueries(collector);
    } else if (rightStatement != null) {
      SQLIdentifier alias = collector.addStatement(rightStatement);
      rightMathExpression = new SQLBaseExpression(alias);
      rightStatement = null;
    }
  }

  @Override
  public boolean refersToParent() {
    if (left != null && left.refersToParent()) {
      return true;
    }
    if (rightStatement != null && rightStatement.refersToParent()) {
      return true;
    }
    return rightMathExpression != null && rightMathExpression.refersToParent();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SQLNotInCondition that = (SQLNotInCondition) o;

    if (!Objects.equals(left, that.left)) {
      return false;
    }
    if (!Objects.equals(operator, that.operator)) {
      return false;
    }
    if (!Objects.equals(rightStatement, that.rightStatement)) {
      return false;
    }
    if (!Objects.equals(right, that.right)) {
      return false;
    }
    if (!Objects.equals(rightParam, that.rightParam)) {
      return false;
    }
    if (!Objects.equals(rightMathExpression, that.rightMathExpression)) {
      return false;
    }
    return Objects.equals(inputFinalValue, that.inputFinalValue);
  }

  @Override
  public int hashCode() {
    int result = left != null ? left.hashCode() : 0;
    result = 31 * result + (operator != null ? operator.hashCode() : 0);
    result = 31 * result + (rightStatement != null ? rightStatement.hashCode() : 0);
    result = 31 * result + (right != null ? right.hashCode() : 0);
    result = 31 * result + (rightParam != null ? rightParam.hashCode() : 0);
    result = 31 * result + (rightMathExpression != null ? rightMathExpression.hashCode() : 0);
    result = 31 * result + (inputFinalValue != null ? inputFinalValue.hashCode() : 0);
    return result;
  }

  @Override
  public List<String> getMatchPatternInvolvedAliases() {
    List<String> leftX = left == null ? null : left.getMatchPatternInvolvedAliases();
    List<String> rightX =
        rightMathExpression == null ? null : rightMathExpression.getMatchPatternInvolvedAliases();

    List<String> result = new ArrayList<String>();
    if (leftX != null) {
      result.addAll(leftX);
    }
    if (rightX != null) {
      result.addAll(rightX);
    }

    return result.size() == 0 ? null : result;
  }

  @Override
  public boolean isCacheable(DatabaseSessionInternal session) {
    if (left != null && !left.isCacheable(session)) {
      return false;
    }

    if (rightStatement != null && !rightStatement.executinPlanCanBeCached(session)) {
      return false;
    }

    return rightMathExpression == null || rightMathExpression.isCacheable(session);
  }
}
/* JavaCC - OriginalChecksum=8fb82bf72cc7d9cbdf2f9e2323ca8ee1 (do not edit this line) */