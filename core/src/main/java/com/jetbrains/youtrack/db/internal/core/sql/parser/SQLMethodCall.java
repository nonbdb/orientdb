/* Generated By:JJTree: Do not edit this line. SQLMethodCall.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseRecordThreadLocal;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.api.exception.CommandExecutionException;
import com.jetbrains.youtrack.db.internal.core.sql.SQLEngine;
import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.internal.core.sql.executor.ResultInternal;
import com.jetbrains.youtrack.db.internal.core.sql.functions.SQLFunction;
import com.jetbrains.youtrack.db.internal.core.sql.functions.SQLFunctionFiltered;
import com.jetbrains.youtrack.db.internal.core.sql.method.SQLMethod;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class SQLMethodCall extends SimpleNode {

  static Set<String> graphMethods =
      new HashSet<String>(
          Arrays.asList("out", "in", "both", "outE", "inE", "bothE", "bothV", "outV", "inV"));

  static Set<String> bidirectionalMethods =
      new HashSet<String>(
          Arrays.asList("out", "in", "both", "oute", "ine", "inv", "outv", "bothe", "bothv"));

  protected SQLIdentifier methodName;
  protected List<SQLExpression> params = new ArrayList<SQLExpression>();
  private boolean resolved = false;
  private boolean isGraph = false;
  private SQLFunction graphFunction = null;
  private SQLMethod method = null;

  public SQLMethodCall(int id) {
    super(id);
  }

  public SQLMethodCall(YouTrackDBSql p, int id) {
    super(p, id);
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append(".");
    methodName.toString(params, builder);
    builder.append("(");
    var first = true;
    for (var param : this.params) {
      if (!first) {
        builder.append(", ");
      }
      param.toString(params, builder);
      first = false;
    }
    builder.append(")");
  }

  public void toGenericStatement(StringBuilder builder) {
    builder.append(".");
    methodName.toGenericStatement(builder);
    builder.append("(");
    var first = true;
    for (var param : this.params) {
      if (!first) {
        builder.append(", ");
      }
      param.toGenericStatement(builder);
      first = false;
    }
    builder.append(")");
  }

  public boolean isBidirectional() {
    return bidirectionalMethods.contains(methodName.getStringValue().toLowerCase(Locale.ENGLISH));
  }

  public Object execute(Object targetObjects, CommandContext ctx) {
    return execute(targetObjects, ctx, methodName.getStringValue(), params, null);
  }

  public Object execute(
      Object targetObjects, Iterable<Identifiable> iPossibleResults, CommandContext ctx) {
    return execute(targetObjects, ctx, methodName.getStringValue(), params, iPossibleResults);
  }

  private void resolveMethod(DatabaseSessionInternal session) {
    if (!resolved) {
      var name = methodName.getStringValue();
      for (var graphMethod : graphMethods) {
        if (graphMethod.equalsIgnoreCase(name)) {
          isGraph = true;
          break;
        }
      }
      if (this.isGraph) {
        this.graphFunction = SQLEngine.getInstance().getFunction(session, name);

      } else {
        this.method = SQLEngine.getMethod(name);
      }

      resolved = true;
    }
  }

  private boolean resolveIsGraphFunction(DatabaseSessionInternal session) {
    resolveMethod(session);
    return isGraph;
  }

  private Object execute(
      Object targetObjects,
      CommandContext ctx,
      String name,
      List<SQLExpression> iParams,
      Iterable<Identifiable> iPossibleResults) {
    var val = ctx.getVariable("$current");
    if (val == null && targetObjects == null) {
      return null;
    }
    var paramValues = resolveParams(targetObjects, ctx, iParams, val);
    if (resolveIsGraphFunction(ctx.getDatabase())) {
      return invokeGraphFunction(
          this.graphFunction, targetObjects, ctx, iPossibleResults, paramValues);
    }
    if (this.method != null) {
      return invokeMethod(this.method, targetObjects, ctx, val, paramValues);
    }
    throw new UnsupportedOperationException(
        "OMethod call, something missing in the implementation...?");
  }

  private static Object invokeMethod(
      SQLMethod method,
      Object targetObjects,
      CommandContext ctx,
      Object val,
      List<Object> paramValues) {
    if (val instanceof Result) {
      val = ((Result) val).getEntity().orElse(null);
    }
    return method.execute(
        targetObjects, (Identifiable) val, ctx, targetObjects, paramValues.toArray());
  }

  private static Object invokeGraphFunction(
      SQLFunction graphFunction,
      Object targetObjects,
      CommandContext ctx,
      Iterable<Identifiable> iPossibleResults,
      List<Object> paramValues) {
    if (graphFunction instanceof SQLFunctionFiltered) {
      var current = ctx.getVariable("$current");
      if (current instanceof Result) {
        current = ((Result) current).getEntity().orElse(null);
      }
      return ((SQLFunctionFiltered) graphFunction)
          .execute(
              targetObjects,
              (Identifiable) current,
              null,
              paramValues.toArray(),
              iPossibleResults,
              ctx);
    } else {
      var current = ctx.getVariable("$current");
      if (current instanceof Identifiable) {
        return graphFunction.execute(
            targetObjects, (Identifiable) current, null, paramValues.toArray(), ctx);
      } else if (current instanceof Result) {
        return graphFunction.execute(
            targetObjects,
            ((Result) current).getEntity().orElse(null),
            null,
            paramValues.toArray(),
            ctx);
      } else {
        return graphFunction.execute(targetObjects, null, null, paramValues.toArray(), ctx);
      }
    }
  }

  private static Object executeGraphFunction(
      Object targetObjects,
      CommandContext ctx,
      String name,
      List<SQLExpression> iParams,
      Iterable<Identifiable> iPossibleResults) {
    var val = ctx.getVariable("$current");
    if (val == null && targetObjects == null) {
      return null;
    }
    var paramValues = resolveParams(targetObjects, ctx, iParams, val);
    var function = SQLEngine.getInstance().getFunction(ctx.getDatabase(), name);
    return invokeGraphFunction(function, targetObjects, ctx, iPossibleResults, paramValues);
  }

  private static List<Object> resolveParams(
      Object targetObjects, CommandContext ctx, List<SQLExpression> iParams, Object val) {
    List<Object> paramValues = new ArrayList<Object>();
    for (var expr : iParams) {
      if (val instanceof Identifiable) {
        paramValues.add(expr.execute((Identifiable) val, ctx));
      } else if (val instanceof Result) {
        paramValues.add(expr.execute((Result) val, ctx));
      } else if (targetObjects instanceof Identifiable) {
        paramValues.add(expr.execute((Identifiable) targetObjects, ctx));
      } else if (targetObjects instanceof Result) {
        paramValues.add(expr.execute((Result) targetObjects, ctx));
      } else {
        throw new CommandExecutionException("Invalild value for $current: " + val);
      }
    }
    return paramValues;
  }

  public Object executeReverse(Object targetObjects, CommandContext ctx) {
    if (!isBidirectional()) {
      throw new UnsupportedOperationException();
    }

    var straightName = methodName.getStringValue();
    if (straightName.equalsIgnoreCase("out")) {
      return executeGraphFunction(targetObjects, ctx, "in", params, null);
    }
    if (straightName.equalsIgnoreCase("in")) {
      return executeGraphFunction(targetObjects, ctx, "out", params, null);
    }

    if (straightName.equalsIgnoreCase("both")) {
      return executeGraphFunction(targetObjects, ctx, "both", params, null);
    }

    if (straightName.equalsIgnoreCase("outE")) {
      return executeGraphFunction(targetObjects, ctx, "outV", params, null);
    }

    if (straightName.equalsIgnoreCase("outV")) {
      return executeGraphFunction(targetObjects, ctx, "outE", params, null);
    }

    if (straightName.equalsIgnoreCase("inE")) {
      return executeGraphFunction(targetObjects, ctx, "inV", params, null);
    }

    if (straightName.equalsIgnoreCase("inV")) {
      return executeGraphFunction(targetObjects, ctx, "inE", params, null);
    }

    if (straightName.equalsIgnoreCase("bothE")) {
      return executeGraphFunction(targetObjects, ctx, "bothV", params, null);
    }

    if (straightName.equalsIgnoreCase("bothV")) {
      return executeGraphFunction(targetObjects, ctx, "bothE", params, null);
    }

    throw new UnsupportedOperationException("Invalid reverse traversal: " + methodName);
  }

  public static DatabaseSessionInternal getDatabase() {
    return DatabaseRecordThreadLocal.instance().get();
  }

  public boolean needsAliases(Set<String> aliases) {
    for (var param : params) {
      if (param.needsAliases(aliases)) {
        return true;
      }
    }
    return false;
  }

  public SQLMethodCall copy() {
    var result = new SQLMethodCall(-1);
    result.methodName = methodName.copy();
    result.params = params.stream().map(x -> x.copy()).collect(Collectors.toList());
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

    var that = (SQLMethodCall) o;

    if (!Objects.equals(methodName, that.methodName)) {
      return false;
    }
    return Objects.equals(params, that.params);
  }

  @Override
  public int hashCode() {
    var result = methodName != null ? methodName.hashCode() : 0;
    result = 31 * result + (params != null ? params.hashCode() : 0);
    return result;
  }

  public void extractSubQueries(SubQueryCollector collector) {
    if (params != null) {
      for (var param : params) {
        param.extractSubQueries(collector);
      }
    }
  }

  public boolean refersToParent() {
    if (params != null) {
      for (var exp : params) {
        if (exp.refersToParent()) {
          return true;
        }
      }
    }
    return false;
  }

  public Result serialize(DatabaseSessionInternal db) {
    var result = new ResultInternal(db);
    if (methodName != null) {
      result.setProperty("methodName", methodName.serialize(db));
    }
    if (params != null) {
      result.setProperty(
          "items", params.stream().map(oExpression -> oExpression.serialize(db))
              .collect(Collectors.toList()));
    }
    return result;
  }

  public void deserialize(Result fromResult) {
    if (fromResult.getProperty("methodName") != null) {
      methodName = SQLIdentifier.deserialize(fromResult.getProperty("methodName"));
    }
    if (fromResult.getProperty("params") != null) {
      List<Result> ser = fromResult.getProperty("params");
      params = new ArrayList<>();
      for (var r : ser) {
        var exp = new SQLExpression(-1);
        exp.deserialize(r);
        params.add(exp);
      }
    }
  }

  public boolean isCacheable(DatabaseSessionInternal session) {
    return resolveIsGraphFunction(session); // TODO
  }

  public void addParam(SQLExpression param) {
    this.params.add(param);
  }
}
/* JavaCC - OriginalChecksum=da95662da21ceb8dee3ad88c0d980413 (do not edit this line) */
