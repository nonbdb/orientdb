/* Generated By:JJTree: Do not edit this line. SQLProjectionItem.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.internal.core.db.record.ridbag.RidBag;
import com.jetbrains.youtrack.db.api.exception.CommandExecutionException;
import com.jetbrains.youtrack.db.api.record.RID;
import com.jetbrains.youtrack.db.api.record.Vertex;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.record.impl.EdgeToVertexIterable;
import com.jetbrains.youtrack.db.internal.core.record.impl.EdgeToVertexIterator;
import com.jetbrains.youtrack.db.internal.core.sql.executor.AggregationContext;
import com.jetbrains.youtrack.db.internal.core.sql.executor.InternalResultSet;
import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.internal.core.sql.executor.ResultInternal;
import com.jetbrains.youtrack.db.internal.core.sql.executor.resultset.ExecutionStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class SQLProjectionItem extends SimpleNode {

  protected boolean exclude = false;

  protected boolean all = false;

  protected SQLIdentifier alias;

  protected SQLExpression expression;

  protected Boolean aggregate;

  protected SQLNestedProjection nestedProjection;

  public SQLProjectionItem(
      SQLExpression expression, SQLIdentifier alias, SQLNestedProjection nestedProjection) {
    super(-1);
    this.expression = expression;
    this.alias = alias;
    this.nestedProjection = nestedProjection;
  }

  public SQLProjectionItem(int id) {
    super(id);
  }

  public SQLProjectionItem(YouTrackDBSql p, int id) {
    super(p, id);
  }

  public boolean isAll() {
    if (all) {
      return true;
    }
    return expression != null && "*".equals(expression.toString());
  }

  public void setAll(boolean all) {
    this.all = all;
  }

  public SQLIdentifier getAlias() {
    return alias;
  }

  public void setAlias(SQLIdentifier alias) {
    this.alias = alias;
  }

  public SQLExpression getExpression() {
    return expression;
  }

  public void setExpression(SQLExpression expression) {
    this.expression = expression;
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    if (all) {
      builder.append("*");
    } else {
      if (exclude) {
        builder.append("!");
      }
      if (expression != null) {
        expression.toString(params, builder);
      }
      if (nestedProjection != null) {
        builder.append(" ");
        nestedProjection.toString(params, builder);
      }
      if (alias != null) {

        builder.append(" AS ");
        alias.toString(params, builder);
      }
    }
  }

  public void toGenericStatement(StringBuilder builder) {
    if (all) {
      builder.append("*");
    } else {
      if (exclude) {
        builder.append("!");
      }
      if (expression != null) {
        expression.toGenericStatement(builder);
      }
      if (nestedProjection != null) {
        builder.append(" ");
        nestedProjection.toGenericStatement(builder);
      }
      if (alias != null) {

        builder.append(" AS ");
        alias.toGenericStatement(builder);
      }
    }
  }

  public Object execute(Identifiable iCurrentRecord, CommandContext ctx) {
    Object result;
    if (all) {
      result = iCurrentRecord;
    } else {
      result = expression.execute(iCurrentRecord, ctx);
    }
    if (nestedProjection != null) {
      result = nestedProjection.apply(expression, result, ctx);
    }
    return convert(result, ctx);
  }

  public static Object convert(Object value, CommandContext context) {
    if (value instanceof RidBag) {
      List result = new ArrayList();
      ((RidBag) value).iterator().forEachRemaining(result::add);
      return result;
    }
    if (value instanceof EdgeToVertexIterable) {
      value = ((EdgeToVertexIterable) value).iterator();
    }
    if (value instanceof EdgeToVertexIterator) {
      List<RID> result = new ArrayList<>();
      while (((EdgeToVertexIterator) value).hasNext()) {
        Vertex v = ((EdgeToVertexIterator) value).next();
        if (v != null) {
          result.add(v.getIdentity());
        }
      }
      return result;
    }
    if (value instanceof InternalResultSet) {
      ((InternalResultSet) value).reset();
      value = ((InternalResultSet) value).stream().collect(Collectors.toList());
    }
    if (value instanceof ExecutionStream) {
      value = ((ExecutionStream) value).stream(context).collect(Collectors.toList());
    }
    if (!(value instanceof Identifiable)) {
      Iterator<?> iter = null;
      if (value instanceof Iterator) {
        iter = (Iterator<?>) value;
      } else if (value instanceof Iterable && !(value instanceof Collection<?>)) {
        iter = ((Iterable<?>) value).iterator();
      }

      if (iter != null) {
        var list = new ArrayList<>();
        while (iter.hasNext()) {
          list.add(iter.next());
        }

        value = list;
      }
    }

    return value;
  }

  public Object execute(Result iCurrentRecord, CommandContext ctx) {
    Object result;
    if (all) {
      result = iCurrentRecord;
    } else {
      result = expression.execute(iCurrentRecord, ctx);
    }
    if (nestedProjection != null) {
      if (result instanceof EntityImpl entity && entity.isEmpty()) {
        result = ctx.getDatabase().bindToSession(entity);
      }
      result = nestedProjection.apply(expression, result, ctx);
    }
    return convert(result, ctx);
  }

  /**
   * returns the final alias for this projection item (the explicit alias, if defined, or the
   * default alias)
   *
   * @return the final alias for this projection item
   */
  public String getProjectionAliasAsString() {
    return getProjectionAlias().getStringValue();
  }

  public SQLIdentifier getProjectionAlias() {
    if (alias != null) {
      return alias;
    }
    SQLIdentifier result;
    if (all) {
      result = new SQLIdentifier("*");
    } else {
      result = expression.getDefaultAlias();
    }
    return result;
  }

  public boolean isExpand() {
    return expression.isExpand();
  }

  public SQLProjectionItem getExpandContent() {
    SQLProjectionItem result = new SQLProjectionItem(-1);
    result.expression = expression.getExpandContent();
    return result;
  }

  public boolean isAggregate(DatabaseSessionInternal session) {
    if (aggregate != null) {
      return aggregate;
    }
    if (all) {
      aggregate = false;
      return false;
    }
    if (expression.isAggregate(session)) {
      aggregate = true;
      return true;
    }
    aggregate = false;
    return false;
  }

  /**
   * INTERNAL USE ONLY this has to be invoked ONLY if the item is aggregate!!!
   *
   * @param aggregateSplit
   */
  public SQLProjectionItem splitForAggregation(
      AggregateProjectionSplit aggregateSplit, CommandContext ctx) {
    if (isAggregate(ctx.getDatabase())) {
      SQLProjectionItem result = new SQLProjectionItem(-1);
      result.alias = getProjectionAlias();
      result.expression = expression.splitForAggregation(aggregateSplit, ctx);
      result.nestedProjection = nestedProjection;
      return result;
    } else {
      return this;
    }
  }

  public AggregationContext getAggregationContext(CommandContext ctx) {
    if (expression == null) {
      throw new CommandExecutionException("Cannot aggregate on this projection: " + this);
    }
    return expression.getAggregationContext(ctx);
  }

  public SQLProjectionItem copy() {
    SQLProjectionItem result = new SQLProjectionItem(-1);
    result.exclude = exclude;
    result.all = all;
    result.alias = alias == null ? null : alias.copy();
    result.expression = expression == null ? null : expression.copy();
    result.nestedProjection = nestedProjection == null ? null : nestedProjection.copy();
    result.aggregate = aggregate;
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
    SQLProjectionItem that = (SQLProjectionItem) o;
    return exclude == that.exclude
        && all == that.all
        && Objects.equals(alias, that.alias)
        && Objects.equals(expression, that.expression)
        && Objects.equals(aggregate, that.aggregate)
        && Objects.equals(nestedProjection, that.nestedProjection);
  }

  @Override
  public int hashCode() {
    return Objects.hash(exclude, all, alias, expression, aggregate, nestedProjection);
  }

  public void extractSubQueries(SubQueryCollector collector) {
    if (expression != null) {
      expression.extractSubQueries(collector);
    }
  }

  public boolean refersToParent() {
    if (expression != null) {
      return expression.refersToParent();
    }
    return false;
  }

  public Result serialize(DatabaseSessionInternal db) {
    ResultInternal result = new ResultInternal(db);
    result.setProperty("all", all);
    if (alias != null) {
      result.setProperty("alias", alias.serialize(db));
    }
    if (expression != null) {
      result.setProperty("expression", expression.serialize(db));
    }
    result.setProperty("aggregate", aggregate);
    if (nestedProjection != null) {
      result.setProperty("nestedProjection", nestedProjection.serialize(db));
    }
    result.setProperty("exclude", exclude);
    return result;
  }

  public void deserialize(Result fromResult) {
    all = fromResult.getProperty("all");
    if (fromResult.getProperty("alias") != null) {
      alias = SQLIdentifier.deserialize(fromResult.getProperty("alias"));
    }
    if (fromResult.getProperty("expression") != null) {
      expression = new SQLExpression(-1);
      expression.deserialize(fromResult.getProperty("expression"));
    }
    aggregate = fromResult.getProperty("aggregate");
    if (fromResult.getProperty("nestedProjection") != null) {
      nestedProjection = new SQLNestedProjection(-1);
      nestedProjection.deserialize(fromResult.getProperty("nestedProjection"));
    }
    if (Boolean.TRUE.equals(fromResult.getProperty("exclude"))) {
      exclude = true;
    }
  }

  public void setNestedProjection(SQLNestedProjection nestedProjection) {
    this.nestedProjection = nestedProjection;
  }

  public boolean isCacheable(DatabaseSessionInternal session) {
    if (expression != null) {
      return expression.isCacheable(session);
    }
    return true;
  }
}
/* JavaCC - OriginalChecksum=6d6010734c7434a6f516e2eac308e9ce (do not edit this line) */
