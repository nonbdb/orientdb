/* Generated By:JJTree: Do not edit this line. SQLArrayConcatExpression.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.internal.common.collection.MultiValue;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.api.exception.CommandExecutionException;
import com.jetbrains.youtrack.db.internal.core.sql.executor.AggregationContext;
import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.internal.core.sql.executor.ResultInternal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class SQLArrayConcatExpression extends SimpleNode {

  protected List<SQLArrayConcatExpressionElement> childExpressions = new ArrayList<>();

  public SQLArrayConcatExpression(int id) {
    super(id);
  }

  public SQLArrayConcatExpression(YouTrackDBSql p, int id) {
    super(p, id);
  }

  public List<SQLArrayConcatExpressionElement> getChildExpressions() {
    return childExpressions;
  }

  public void setChildExpressions(List<SQLArrayConcatExpressionElement> childExpressions) {
    this.childExpressions = childExpressions;
  }

  public void addChildExpression(SQLArrayConcatExpressionElement elem) {
    this.childExpressions.add(elem);
  }

  public Object apply(Object left, Object right) {

    if (left == null && right == null) {
      return null;
    }

    if (right == null) {
      if (MultiValue.isMultiValue(left)) {
        return left;
      } else {
        return Collections.singletonList(left);
      }
    }

    if (left == null) {
      if (MultiValue.isMultiValue(right)) {
        return right;
      } else {
        return Collections.singletonList(right);
      }
    }

    List<Object> result = new ArrayList<>();
    if (MultiValue.isMultiValue(left)) {
      var leftIter = MultiValue.getMultiValueIterator(left);
      while (leftIter.hasNext()) {
        result.add(leftIter.next());
      }
    } else {
      result.add(left);
    }

    if (MultiValue.isMultiValue(right)) {
      var rigthIter = MultiValue.getMultiValueIterator(right);
      while (rigthIter.hasNext()) {
        result.add(rigthIter.next());
      }
    } else {
      result.add(right);
    }

    return result;
  }

  public Object execute(Identifiable iCurrentRecord, CommandContext ctx) {
    var result = childExpressions.get(0).execute(iCurrentRecord, ctx);
    for (var i = 1; i < childExpressions.size(); i++) {
      result = apply(result, childExpressions.get(i).execute(iCurrentRecord, ctx));
    }
    return result;
  }

  public Object execute(Result iCurrentRecord, CommandContext ctx) {
    var result = childExpressions.get(0).execute(iCurrentRecord, ctx);
    for (var i = 1; i < childExpressions.size(); i++) {
      result = apply(result, childExpressions.get(i).execute(iCurrentRecord, ctx));
    }
    return result;
  }

  public boolean isEarlyCalculated(CommandContext ctx) {
    for (var element : childExpressions) {
      if (!element.isEarlyCalculated(ctx)) {
        return false;
      }
    }
    return true;
  }

  protected boolean supportsBasicCalculation() {
    for (var expr : this.childExpressions) {
      if (!expr.supportsBasicCalculation()) {
        return false;
      }
    }
    return true;
  }

  public boolean needsAliases(Set<String> aliases) {
    for (var expr : childExpressions) {
      if (expr.needsAliases(aliases)) {
        return true;
      }
    }
    return false;
  }

  public boolean isAggregate(DatabaseSessionInternal session) {
    for (var expr : this.childExpressions) {
      if (expr.isAggregate(session)) {
        return true;
      }
    }
    return false;
  }

  public SimpleNode splitForAggregation(DatabaseSessionInternal session,
      AggregateProjectionSplit aggregateProj) {
    if (isAggregate(session)) {
      throw new CommandExecutionException(
          "Cannot use aggregate functions in array concatenation");
    } else {
      return this;
    }
  }

  public AggregationContext getAggregationContext(CommandContext ctx) {
    throw new UnsupportedOperationException(
        "array concatenation expressions do not allow plain aggregation");
  }

  public SQLArrayConcatExpression copy() {
    var result = new SQLArrayConcatExpression(-1);
    this.childExpressions.forEach(x -> result.childExpressions.add(x.copy()));
    return result;
  }

  public void extractSubQueries(SubQueryCollector collector) {
    for (var expr : this.childExpressions) {
      expr.extractSubQueries(collector);
    }
  }

  public boolean refersToParent() {
    for (var expr : this.childExpressions) {
      if (expr.refersToParent()) {
        return true;
      }
    }
    return false;
  }

  public List<String> getMatchPatternInvolvedAliases() {
    List<String> result = new ArrayList<String>();
    for (var exp : childExpressions) {
      var x = exp.getMatchPatternInvolvedAliases();
      if (x != null) {
        result.addAll(x);
      }
    }
    if (result.size() == 0) {
      return null;
    }
    return result;
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    for (var i = 0; i < childExpressions.size(); i++) {
      if (i > 0) {
        builder.append(" || ");
      }
      childExpressions.get(i).toString(params, builder);
    }
  }

  @Override
  public void toGenericStatement(StringBuilder builder) {
    for (var i = 0; i < childExpressions.size(); i++) {
      if (i > 0) {
        builder.append(" || ");
      }
      childExpressions.get(i).toGenericStatement(builder);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    var that = (SQLArrayConcatExpression) o;

    return Objects.equals(childExpressions, that.childExpressions);
  }

  @Override
  public int hashCode() {
    return childExpressions != null ? childExpressions.hashCode() : 0;
  }

  public Result serialize(DatabaseSessionInternal db) {
    var result = new ResultInternal(db);
    if (childExpressions != null) {
      result.setProperty(
          "childExpressions",
          childExpressions.stream().map(x -> x.serialize(db)).collect(Collectors.toList()));
    }
    return result;
  }

  public void deserialize(Result fromResult) {

    if (fromResult.getProperty("childExpressions") != null) {
      List<Result> ser = fromResult.getProperty("childExpressions");
      childExpressions = new ArrayList<>();
      for (var r : ser) {
        var exp = new SQLArrayConcatExpressionElement(-1);
        exp.deserialize(r);
        childExpressions.add(exp);
      }
    }
  }

  public boolean isCacheable(DatabaseSessionInternal session) {
    for (var exp : childExpressions) {
      if (!exp.isCacheable(session)) {
        return false;
      }
    }
    return true;
  }
}
/* JavaCC - OriginalChecksum=8d976a02f84460bf21c4304009135345 (do not edit this line) */
