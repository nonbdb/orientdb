/* Generated By:JJTree: Do not edit this line. SQLMatchesCondition.java Version 4.3 */
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

public class SQLMatchesCondition extends SQLBooleanExpression {

  protected SQLExpression expression;

  protected String right;
  protected SQLExpression rightExpression;
  protected SQLInputParameter rightParam;

  public SQLMatchesCondition(int id) {
    super(id);
  }

  public SQLMatchesCondition(YouTrackDBSql p, int id) {
    super(p, id);
  }

  @Override
  public boolean evaluate(Identifiable currentRecord, CommandContext ctx) {
    var regex = right;
    if (regex != null) {
      regex = regex.substring(1, regex.length() - 1);
    } else if (rightExpression != null) {
      var val = rightExpression.execute(currentRecord, ctx);
      if (val instanceof String) {
        regex = (String) val;
      } else {
        return false;
      }
    } else {
      var paramVal = rightParam.getValue(ctx.getInputParameters());
      if (paramVal instanceof String) {
        regex = (String) paramVal;
      } else {
        return false;
      }
    }
    var value = expression.execute(currentRecord, ctx);

    return matches(value, regex, ctx);
  }

  private boolean matches(Object value, String regex, CommandContext ctx) {
    final var key = "MATCHES_" + regex.hashCode();
    var p = (java.util.regex.Pattern) ctx.getVariable(key);
    if (p == null) {
      p = java.util.regex.Pattern.compile(regex);
      ctx.setVariable(key, p);
    }

    if (value instanceof CharSequence) {
      return p.matcher((CharSequence) value).matches();
    } else {
      return false;
    }
  }

  @Override
  public boolean evaluate(Result currentRecord, CommandContext ctx) {
    var regex = right;
    if (regex != null) {
      regex = regex.substring(1, regex.length() - 1);
    } else if (rightExpression != null) {
      var val = rightExpression.execute(currentRecord, ctx);
      if (val instanceof String) {
        regex = (String) val;
      } else {
        return false;
      }
    } else {
      var paramVal = rightParam.getValue(ctx.getInputParameters());
      if (paramVal instanceof String) {
        regex = (String) paramVal;
      } else {
        return false;
      }
    }
    var value = expression.execute(currentRecord, ctx);

    return matches(value, regex, ctx);
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    expression.toString(params, builder);
    builder.append(" MATCHES ");
    if (right != null) {
      builder.append(right);
    } else if (rightExpression != null) {
      rightExpression.toString(params, builder);
    } else {
      rightParam.toString(params, builder);
    }
  }

  public void toGenericStatement(StringBuilder builder) {
    expression.toGenericStatement(builder);
    builder.append(" MATCHES ");
    if (right != null) {
      builder.append(PARAMETER_PLACEHOLDER);
    } else if (rightExpression != null) {
      rightExpression.toGenericStatement(builder);
    } else {
      rightParam.toGenericStatement(builder);
    }
  }

  @Override
  public boolean supportsBasicCalculation() {
    if (!expression.supportsBasicCalculation()) {
      return false;
    }
    return rightExpression == null || rightExpression.supportsBasicCalculation();
  }

  @Override
  protected int getNumberOfExternalCalculations() {
    var result = 0;
    if (expression != null && !expression.supportsBasicCalculation()) {
      result++;
    }
    if (rightExpression != null && !rightExpression.supportsBasicCalculation()) {
      result++;
    }
    return result;
  }

  @Override
  protected List<Object> getExternalCalculationConditions() {
    List<Object> result = new ArrayList<>();
    if (expression != null && !expression.supportsBasicCalculation()) {
      result.add(expression);
    }
    if (rightExpression != null && !rightExpression.supportsBasicCalculation()) {
      result.add(rightExpression);
    }
    return result;
  }

  @Override
  public boolean needsAliases(Set<String> aliases) {
    if (expression.needsAliases(aliases)) {
      return true;
    }
    return rightExpression.needsAliases(aliases);
  }

  @Override
  public SQLMatchesCondition copy() {
    var result = new SQLMatchesCondition(-1);
    result.expression = expression == null ? null : expression.copy();
    result.right = right;
    result.rightParam = rightParam == null ? null : rightParam.copy();
    result.rightExpression = rightExpression == null ? null : rightExpression.copy();
    return result;
  }

  @Override
  public void extractSubQueries(SubQueryCollector collector) {
    expression.extractSubQueries(collector);
    if (rightExpression != null) {
      rightExpression.extractSubQueries(collector);
    }
  }

  @Override
  public boolean refersToParent() {
    if (expression != null && expression.refersToParent()) {
      return true;
    }
    return rightExpression != null && rightExpression.refersToParent();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    var that = (SQLMatchesCondition) o;

    if (!Objects.equals(expression, that.expression)) {
      return false;
    }
    if (!Objects.equals(right, that.right)) {
      return false;
    }
    if (!Objects.equals(rightExpression, that.rightExpression)) {
      return false;
    }
    return Objects.equals(rightParam, that.rightParam);
  }

  @Override
  public int hashCode() {
    var result = expression != null ? expression.hashCode() : 0;
    result = 31 * result + (right != null ? right.hashCode() : 0);
    result = 31 * result + (rightExpression != null ? rightExpression.hashCode() : 0);
    result = 31 * result + (rightParam != null ? rightParam.hashCode() : 0);
    return result;
  }

  @Override
  public List<String> getMatchPatternInvolvedAliases() {
    List<String> result = new ArrayList<>();
    result.addAll(expression.getMatchPatternInvolvedAliases());
    if (rightExpression != null) {
      result.addAll(rightExpression.getMatchPatternInvolvedAliases());
    }
    return result;
  }

  @Override
  public boolean isCacheable(DatabaseSessionInternal session) {
    if (!expression.isCacheable(session)) {
      return false;
    }
    return rightExpression == null || rightExpression.isCacheable(session);
  }
}
/* JavaCC - OriginalChecksum=68712f476e2e633c2bbfc34cb6c39356 (do not edit this line) */
