/* Generated By:JJTree: Do not edit this line. OMethodCall.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.orientechnologies.orient.core.sql.parser;

import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.orientechnologies.orient.core.db.ODatabaseSessionInternal;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.exception.OCommandExecutionException;
import com.orientechnologies.orient.core.sql.OSQLEngine;
import com.orientechnologies.orient.core.sql.executor.OResult;
import com.orientechnologies.orient.core.sql.executor.OResultInternal;
import com.orientechnologies.orient.core.sql.functions.OSQLFunction;
import com.orientechnologies.orient.core.sql.functions.OSQLFunctionFiltered;
import com.orientechnologies.orient.core.sql.method.OSQLMethod;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class OMethodCall extends SimpleNode {

  static Set<String> graphMethods =
      new HashSet<String>(
          Arrays.asList("out", "in", "both", "outE", "inE", "bothE", "bothV", "outV", "inV"));

  static Set<String> bidirectionalMethods =
      new HashSet<String>(
          Arrays.asList("out", "in", "both", "oute", "ine", "inv", "outv", "bothe", "bothv"));

  protected OIdentifier methodName;
  protected List<OExpression> params = new ArrayList<OExpression>();
  private boolean resolved = false;
  private boolean isGraph = false;
  private OSQLFunction graphFunction = null;
  private OSQLMethod method = null;

  public OMethodCall(int id) {
    super(id);
  }

  public OMethodCall(OrientSql p, int id) {
    super(p, id);
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append(".");
    methodName.toString(params, builder);
    builder.append("(");
    boolean first = true;
    for (OExpression param : this.params) {
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
    boolean first = true;
    for (OExpression param : this.params) {
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

  public Object execute(Object targetObjects, OCommandContext ctx) {
    return execute(targetObjects, ctx, methodName.getStringValue(), params, null);
  }

  public Object execute(
      Object targetObjects, Iterable<OIdentifiable> iPossibleResults, OCommandContext ctx) {
    return execute(targetObjects, ctx, methodName.getStringValue(), params, iPossibleResults);
  }

  private void resolveMethod() {
    if (!resolved) {
      String name = methodName.getStringValue();
      for (String graphMethod : graphMethods) {
        if (graphMethod.equalsIgnoreCase(name)) {
          isGraph = true;
          break;
        }
      }
      if (this.isGraph) {
        this.graphFunction = OSQLEngine.getInstance().getFunction(name);

      } else {
        this.method = OSQLEngine.getMethod(name);
      }

      resolved = true;
    }
  }

  private boolean resolveIsGraphFunction() {
    resolveMethod();
    return isGraph;
  }

  private Object execute(
      Object targetObjects,
      OCommandContext ctx,
      String name,
      List<OExpression> iParams,
      Iterable<OIdentifiable> iPossibleResults) {
    Object val = ctx.getVariable("$current");
    if (val == null && targetObjects == null) {
      return null;
    }
    List<Object> paramValues = resolveParams(targetObjects, ctx, iParams, val);
    if (resolveIsGraphFunction()) {
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
      OSQLMethod method,
      Object targetObjects,
      OCommandContext ctx,
      Object val,
      List<Object> paramValues) {
    if (val instanceof OResult) {
      val = ((OResult) val).getElement().orElse(null);
    }
    return method.execute(
        targetObjects, (OIdentifiable) val, ctx, targetObjects, paramValues.toArray());
  }

  private static Object invokeGraphFunction(
      OSQLFunction graphFunction,
      Object targetObjects,
      OCommandContext ctx,
      Iterable<OIdentifiable> iPossibleResults,
      List<Object> paramValues) {
    if (graphFunction instanceof OSQLFunctionFiltered) {
      Object current = ctx.getVariable("$current");
      if (current instanceof OResult) {
        current = ((OResult) current).getElement().orElse(null);
      }
      return ((OSQLFunctionFiltered) graphFunction)
          .execute(
              targetObjects,
              (OIdentifiable) current,
              null,
              paramValues.toArray(),
              iPossibleResults,
              ctx);
    } else {
      Object current = ctx.getVariable("$current");
      if (current instanceof OIdentifiable) {
        return graphFunction.execute(
            targetObjects, (OIdentifiable) current, null, paramValues.toArray(), ctx);
      } else if (current instanceof OResult) {
        return graphFunction.execute(
            targetObjects,
            ((OResult) current).getElement().orElse(null),
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
      OCommandContext ctx,
      String name,
      List<OExpression> iParams,
      Iterable<OIdentifiable> iPossibleResults) {
    Object val = ctx.getVariable("$current");
    if (val == null && targetObjects == null) {
      return null;
    }
    List<Object> paramValues = resolveParams(targetObjects, ctx, iParams, val);
    OSQLFunction function = OSQLEngine.getInstance().getFunction(name);
    return invokeGraphFunction(function, targetObjects, ctx, iPossibleResults, paramValues);
  }

  private static List<Object> resolveParams(
      Object targetObjects, OCommandContext ctx, List<OExpression> iParams, Object val) {
    List<Object> paramValues = new ArrayList<Object>();
    for (OExpression expr : iParams) {
      if (val instanceof OIdentifiable) {
        paramValues.add(expr.execute((OIdentifiable) val, ctx));
      } else if (val instanceof OResult) {
        paramValues.add(expr.execute((OResult) val, ctx));
      } else if (targetObjects instanceof OIdentifiable) {
        paramValues.add(expr.execute((OIdentifiable) targetObjects, ctx));
      } else if (targetObjects instanceof OResult) {
        paramValues.add(expr.execute((OResult) targetObjects, ctx));
      } else {
        throw new OCommandExecutionException("Invalild value for $current: " + val);
      }
    }
    return paramValues;
  }

  public Object executeReverse(Object targetObjects, OCommandContext ctx) {
    if (!isBidirectional()) {
      throw new UnsupportedOperationException();
    }

    String straightName = methodName.getStringValue();
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

  public static ODatabaseSessionInternal getDatabase() {
    return ODatabaseRecordThreadLocal.instance().get();
  }

  public boolean needsAliases(Set<String> aliases) {
    for (OExpression param : params) {
      if (param.needsAliases(aliases)) {
        return true;
      }
    }
    return false;
  }

  public OMethodCall copy() {
    OMethodCall result = new OMethodCall(-1);
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

    OMethodCall that = (OMethodCall) o;

    if (!Objects.equals(methodName, that.methodName)) {
      return false;
    }
    return Objects.equals(params, that.params);
  }

  @Override
  public int hashCode() {
    int result = methodName != null ? methodName.hashCode() : 0;
    result = 31 * result + (params != null ? params.hashCode() : 0);
    return result;
  }

  public void extractSubQueries(SubQueryCollector collector) {
    if (params != null) {
      for (OExpression param : params) {
        param.extractSubQueries(collector);
      }
    }
  }

  public boolean refersToParent() {
    if (params != null) {
      for (OExpression exp : params) {
        if (exp.refersToParent()) {
          return true;
        }
      }
    }
    return false;
  }

  public OResult serialize() {
    OResultInternal result = new OResultInternal();
    if (methodName != null) {
      result.setProperty("methodName", methodName.serialize());
    }
    if (params != null) {
      result.setProperty(
          "items", params.stream().map(x -> x.serialize()).collect(Collectors.toList()));
    }
    return result;
  }

  public void deserialize(OResult fromResult) {
    if (fromResult.getProperty("methodName") != null) {
      methodName = OIdentifier.deserialize(fromResult.getProperty("methodName"));
    }
    if (fromResult.getProperty("params") != null) {
      List<OResult> ser = fromResult.getProperty("params");
      params = new ArrayList<>();
      for (OResult r : ser) {
        OExpression exp = new OExpression(-1);
        exp.deserialize(r);
        params.add(exp);
      }
    }
  }

  public boolean isCacheable() {
    return resolveIsGraphFunction(); // TODO
  }

  public void addParam(OExpression param) {
    this.params.add(param);
  }
}
/* JavaCC - OriginalChecksum=da95662da21ceb8dee3ad88c0d980413 (do not edit this line) */
