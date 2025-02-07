/* Generated By:JJTree: Do not edit this line. SQLInCondition.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.internal.common.collection.MultiValue;
import com.jetbrains.youtrack.db.internal.core.command.BasicCommandContext;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.internal.core.sql.executor.IndexSearchInfo;
import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.api.query.ResultSet;
import com.jetbrains.youtrack.db.internal.core.sql.executor.metadata.IndexCandidate;
import com.jetbrains.youtrack.db.internal.core.sql.executor.metadata.IndexFinder;
import com.jetbrains.youtrack.db.internal.core.sql.executor.metadata.MetadataPath;
import com.jetbrains.youtrack.db.internal.core.sql.operator.QueryOperatorEquals;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class SQLInCondition extends SQLBooleanExpression {

  protected SQLExpression left;
  protected SQLBinaryCompareOperator operator;
  protected SQLSelectStatement rightStatement;
  protected SQLInputParameter rightParam;
  protected SQLMathExpression rightMathExpression;
  protected Object right;

  private static final Object UNSET = new Object();
  private final Object inputFinalValue = UNSET;

  public SQLInCondition(int id) {
    super(id);
  }

  public SQLInCondition(YouTrackDBSql p, int id) {
    super(p, id);
  }

  @Override
  public boolean evaluate(Identifiable currentRecord, CommandContext ctx) {
    Object leftVal = evaluateLeft(currentRecord, ctx);
    Object rightVal = evaluateRight(currentRecord, ctx);
    if (rightVal == null) {
      return false;
    }
    return evaluateExpression(ctx.getDatabase(), leftVal, rightVal);
  }

  public Object evaluateRight(Identifiable currentRecord, CommandContext ctx) {
    Object rightVal = null;
    if (rightStatement != null) {
      rightVal = executeQuery(rightStatement, ctx);
    } else if (rightParam != null) {
      rightVal = rightParam.getValue(ctx.getInputParameters());
    } else if (rightMathExpression != null) {
      rightVal = rightMathExpression.execute(currentRecord, ctx);
    }
    return rightVal;
  }

  public Object evaluateLeft(Identifiable currentRecord, CommandContext ctx) {
    return left.execute(currentRecord, ctx);
  }

  @Override
  public boolean evaluate(Result currentRecord, CommandContext ctx) {
    Object rightVal = evaluateRight(currentRecord, ctx);
    if (rightVal == null) {
      return false;
    }

    if (left.isFunctionAny()) {
      return evaluateAny(currentRecord, rightVal, ctx);
    }

    if (left.isFunctionAll()) {
      return evaluateAllFunction(currentRecord, rightVal, ctx);
    }

    Object leftVal = evaluateLeft(currentRecord, ctx);
    return evaluateExpression(ctx.getDatabase(), leftVal, rightVal);
  }

  private boolean evaluateAny(Result currentRecord, Object rightVal, CommandContext ctx) {
    for (String s : currentRecord.getPropertyNames()) {
      Object leftVal = currentRecord.getProperty(s);
      if (evaluateExpression(ctx.getDatabase(), leftVal, rightVal)) {
        return true;
      }
    }
    return false;
  }

  private boolean evaluateAllFunction(Result currentRecord, Object rightVal,
      CommandContext ctx) {
    for (String s : currentRecord.getPropertyNames()) {
      Object leftVal = currentRecord.getProperty(s);
      if (!evaluateExpression(ctx.getDatabase(), leftVal, rightVal)) {
        return false;
      }
    }
    return true;
  }

  public Object evaluateRight(Result currentRecord, CommandContext ctx) {
    Object rightVal = null;
    if (rightStatement != null) {
      rightVal = executeQuery(rightStatement, ctx);
    } else if (rightParam != null) {
      rightVal = rightParam.getValue(ctx.getInputParameters());
    } else if (rightMathExpression != null) {
      rightVal = rightMathExpression.execute(currentRecord, ctx);
    }
    return rightVal;
  }

  public Object evaluateLeft(Result currentRecord, CommandContext ctx) {
    return left.execute(currentRecord, ctx);
  }

  protected static Object executeQuery(SQLSelectStatement rightStatement, CommandContext ctx) {
    BasicCommandContext subCtx = new BasicCommandContext();
    subCtx.setParentWithoutOverridingChild(ctx);
    ResultSet result = rightStatement.execute(ctx.getDatabase(), ctx.getInputParameters(), false);
    return result.stream().collect(Collectors.toSet());
  }

  protected static boolean evaluateExpression(DatabaseSessionInternal session, final Object iLeft,
      final Object iRight) {
    if (MultiValue.isMultiValue(iRight)) {
      if (iRight instanceof Set<?> set) {
        if (set.contains(iLeft)) {
          return true;
        }
        if (MultiValue.isMultiValue(iLeft)) {
          for (final Object o : MultiValue.getMultiValueIterable(iLeft)) {
            if (!set.contains(o)) {
              return false;
            }
          }
        }
      }

      for (final Object rightItem : MultiValue.getMultiValueIterable(iRight)) {
        if (QueryOperatorEquals.equals(session, iLeft, rightItem)) {
          return true;
        }
        if (MultiValue.isMultiValue(iLeft)) {
          if (MultiValue.getSize(iLeft) == 1) {
            Object leftItem = MultiValue.getFirstValue(iLeft);
            if (compareItems(session, rightItem, leftItem)) {
              return true;
            }
          } else {
            for (final Object leftItem : MultiValue.getMultiValueIterable(iLeft)) {
              if (compareItems(session, rightItem, leftItem)) {
                return true;
              }
            }
          }
        }
      }
    } else if (iRight.getClass().isArray()) {
      for (final Object rightItem : (Object[]) iRight) {
        if (QueryOperatorEquals.equals(session, iLeft, rightItem)) {
          return true;
        }
      }
    } else if (iRight instanceof ResultSet rsRight) {
      rsRight.reset();

      while (rsRight.hasNext()) {
        if (QueryOperatorEquals.equals(session, iLeft, rsRight.next())) {
          return true;
        }
      }
    }

    return false;
  }

  private static boolean compareItems(DatabaseSessionInternal session, Object rightItem,
      Object leftItem) {
    if (QueryOperatorEquals.equals(session, leftItem, rightItem)) {
      return true;
    }

    if (leftItem instanceof Result && ((Result) leftItem).getPropertyNames().size() == 1) {
      Object propValue =
          ((Result) leftItem)
              .getProperty(((Result) leftItem).getPropertyNames().iterator().next());
      return QueryOperatorEquals.equals(session, propValue, rightItem);
    }

    return false;
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    left.toString(params, builder);
    builder.append(" IN ");
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
    builder.append(" IN ");
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
    if (!left.supportsBasicCalculation()) {
      return false;
    }
    if (!rightMathExpression.supportsBasicCalculation()) {
      return false;
    }
    return operator.supportsBasicCalculation();
  }

  @Override
  protected int getNumberOfExternalCalculations() {
    int total = 0;
    if (operator != null && !operator.supportsBasicCalculation()) {
      total++;
    }
    if (!left.supportsBasicCalculation()) {
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

    if (operator != null) {
      result.add(this);
    }
    if (!left.supportsBasicCalculation()) {
      result.add(left);
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
  public SQLInCondition copy() {
    SQLInCondition result = new SQLInCondition(-1);
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
    }
    if (rightStatement != null) {
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

    SQLInCondition that = (SQLInCondition) o;

    if (!Objects.equals(left, that.left)) {
      return false;
    }
    if (!Objects.equals(operator, that.operator)) {
      return false;
    }
    if (!Objects.equals(rightStatement, that.rightStatement)) {
      return false;
    }
    if (!Objects.equals(rightParam, that.rightParam)) {
      return false;
    }
    if (!Objects.equals(rightMathExpression, that.rightMathExpression)) {
      return false;
    }
    if (!Objects.equals(right, that.right)) {
      return false;
    }
    return Objects.equals(inputFinalValue, that.inputFinalValue);
  }

  @Override
  public int hashCode() {
    int result = left != null ? left.hashCode() : 0;
    result = 31 * result + (operator != null ? operator.hashCode() : 0);
    result = 31 * result + (rightStatement != null ? rightStatement.hashCode() : 0);
    result = 31 * result + (rightParam != null ? rightParam.hashCode() : 0);
    result = 31 * result + (rightMathExpression != null ? rightMathExpression.hashCode() : 0);
    result = 31 * result + (right != null ? right.hashCode() : 0);
    result = 31 * result + (inputFinalValue != null ? inputFinalValue.hashCode() : 0);
    return result;
  }

  @Override
  public List<String> getMatchPatternInvolvedAliases() {
    List<String> leftX = left == null ? null : left.getMatchPatternInvolvedAliases();

    List<String> conditionX =
        rightMathExpression == null ? null : rightMathExpression.getMatchPatternInvolvedAliases();

    List<String> result = new ArrayList<String>();
    if (leftX != null) {
      result.addAll(leftX);
    }
    if (conditionX != null) {
      result.addAll(conditionX);
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

  public SQLExpression getLeft() {
    return left;
  }

  public void setLeft(SQLExpression left) {
    this.left = left;
  }

  public SQLSelectStatement getRightStatement() {
    return rightStatement;
  }

  public SQLInputParameter getRightParam() {
    return rightParam;
  }

  public SQLMathExpression getRightMathExpression() {
    return rightMathExpression;
  }

  public void setRightParam(SQLInputParameter rightParam) {
    this.rightParam = rightParam;
  }

  public void setRightMathExpression(SQLMathExpression rightMathExpression) {
    this.rightMathExpression = rightMathExpression;
  }

  public boolean isIndexAware(IndexSearchInfo info) {
    if (left.isBaseIdentifier()) {
      if (info.getField().equals(left.getDefaultAlias().getStringValue())) {
        if (rightMathExpression != null) {
          return rightMathExpression.isEarlyCalculated(info.getCtx());
        } else {
          return rightParam != null;
        }
      }
    }
    return false;
  }

  public Optional<IndexCandidate> findIndex(IndexFinder info, CommandContext ctx) {
    Optional<MetadataPath> path = left.getPath();
    if (path.isPresent()) {
      if (rightMathExpression != null && rightMathExpression.isEarlyCalculated(ctx)) {
        Object value = rightMathExpression.execute((Result) null, ctx);
        return info.findExactIndex(path.get(), value, ctx);
      }
    }

    return Optional.empty();
  }

  @Override
  public SQLExpression resolveKeyFrom(SQLBinaryCondition additional) {
    SQLExpression item = new SQLExpression(-1);
    if (rightMathExpression != null) {
      item.setMathExpression(rightMathExpression);
      return item;
    } else if (rightParam != null) {
      SQLBaseExpression e = new SQLBaseExpression(-1);
      e.setInputParam(rightParam.copy());
      item.setMathExpression(e);
      return item;
    } else {
      throw new UnsupportedOperationException("Cannot execute index query with " + this);
    }
  }

  @Override
  public SQLExpression resolveKeyTo(SQLBinaryCondition additional) {
    SQLExpression item = new SQLExpression(-1);
    if (rightMathExpression != null) {
      item.setMathExpression(rightMathExpression);
      return item;
    } else if (rightParam != null) {
      SQLBaseExpression e = new SQLBaseExpression(-1);
      e.setInputParam(rightParam.copy());
      item.setMathExpression(e);
      return item;
    } else {
      throw new UnsupportedOperationException("Cannot execute index query with " + this);
    }
  }
}
/* JavaCC - OriginalChecksum=00df7cb1877c0a12d24205c1700653c7 (do not edit this line) */
