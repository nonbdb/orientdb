/* Generated By:JJTree: Do not edit this line. OExpansion.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.orientechnologies.orient.core.sql.parser;

import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.db.record.ridbag.ORidBag;
import com.orientechnologies.orient.core.record.OElement;
import com.orientechnologies.orient.core.record.ORecord;
import com.orientechnologies.orient.core.sql.executor.OResult;
import com.orientechnologies.orient.core.sql.executor.OResultInternal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ONestedProjection extends SimpleNode {
  protected List<ONestedProjectionItem> includeItems = new ArrayList<>();
  protected List<ONestedProjectionItem> excludeItems = new ArrayList<>();
  protected ONestedProjectionItem starItem;
  private OInteger recursion; // not used for now

  public ONestedProjection(int id) {
    super(id);
  }

  public ONestedProjection(OrientSql p, int id) {
    super(p, id);
  }

  /**
   * @param expression
   * @param input
   * @param ctx
   */
  public Object apply(OExpression expression, Object input, OCommandContext ctx) {
    if (input instanceof OResult) {
      return apply(
          expression,
          (OResult) input,
          ctx,
          recursion == null ? 0 : recursion.getValue().intValue());
    }
    if (input instanceof OIdentifiable) {
      return apply(
          expression,
          (OIdentifiable) input,
          ctx,
          recursion == null ? 0 : recursion.getValue().intValue());
    }
    if (input instanceof Map) {
      return apply(
          expression, (Map) input, ctx, recursion == null ? 0 : recursion.getValue().intValue());
    }
    if (input instanceof Collection) {
      return ((Collection) input)
          .stream().map(x -> apply(expression, x, ctx)).collect(Collectors.toList());
    }
    Iterator iter = null;
    if (input instanceof Iterable) {
      iter = ((Iterable) input).iterator();
    }
    if (input instanceof Iterator) {
      iter = (Iterator) input;
    }
    if (iter != null) {
      List result = new ArrayList();
      while (iter.hasNext()) {
        result.add(apply(expression, iter.next(), ctx));
      }
      return result;
    }
    return input;
  }

  private Object apply(OExpression expression, OResult elem, OCommandContext ctx, int recursion) {
    OResultInternal result = new OResultInternal();
    if (starItem != null || includeItems.size() == 0) {
      for (String property : elem.getPropertyNames()) {
        if (isExclude(property)) {
          continue;
        }
        result.setProperty(
            property,
            convert(tryExpand(expression, property, elem.getProperty(property), ctx, recursion)));
      }
    }
    if (includeItems.size() > 0) {
      // TODO manage wildcards!
      for (ONestedProjectionItem item : includeItems) {
        String alias =
            item.alias != null
                ? item.alias.getStringValue()
                : item.expression.getDefaultAlias().getStringValue();
        Object value = item.expression.execute(elem, ctx);
        if (item.expansion != null) {
          value = item.expand(expression, alias, value, ctx, recursion - 1);
        }
        result.setProperty(alias, convert(value));
      }
    }
    return result;
  }

  private boolean isExclude(String propertyName) {
    for (ONestedProjectionItem item : excludeItems) {
      if (item.matches(propertyName)) {
        return true;
      }
    }
    return false;
  }

  private Object tryExpand(
      OExpression rootExpr, String propName, Object propValue, OCommandContext ctx, int recursion) {
    if (this.starItem != null && starItem.expansion != null) {
      return starItem.expand(rootExpr, propName, propValue, ctx, recursion);
    }
    for (ONestedProjectionItem item : includeItems) {
      if (item.matches(propName) && item.expansion != null) {
        return item.expand(rootExpr, propName, propValue, ctx, recursion);
      }
    }
    return propValue;
  }

  private Object apply(
      OExpression expression, OIdentifiable input, OCommandContext ctx, int recursion) {
    OElement elem;
    if (input instanceof OElement) {
      elem = (OElement) input;
    } else {
      ORecord e = input.getRecord();
      if (e instanceof OElement) {
        elem = (OElement) e;
      } else {
        return input;
      }
    }
    OResultInternal result = new OResultInternal();
    if (starItem != null || includeItems.isEmpty()) {
      for (String property : elem.getPropertyNames()) {
        if (isExclude(property)) {
          continue;
        }
        result.setProperty(
            property,
            convert(tryExpand(expression, property, elem.getProperty(property), ctx, recursion)));
      }
    }

    if (includeItems.size() > 0) {
      // TODO manage wildcards!
      for (ONestedProjectionItem item : includeItems) {
        String alias =
            item.alias != null
                ? item.alias.getStringValue()
                : item.expression.getDefaultAlias().getStringValue();
        Object value = item.expression.execute(elem, ctx);
        if (item.expansion != null) {
          value = item.expand(expression, alias, value, ctx, recursion - 1);
        }
        result.setProperty(alias, convert(value));
      }
    }
    return result;
  }

  private Object apply(
      OExpression expression, Map<String, Object> input, OCommandContext ctx, int recursion) {
    OResultInternal result = new OResultInternal();

    if (starItem != null || includeItems.size() == 0) {
      for (String property : input.keySet()) {
        if (isExclude(property)) {
          continue;
        }
        result.setProperty(
            property,
            convert(tryExpand(expression, property, input.get(property), ctx, recursion)));
      }
    }
    if (includeItems.size() > 0) {
      // TODO manage wildcards!
      for (ONestedProjectionItem item : includeItems) {
        String alias =
            item.alias != null
                ? item.alias.getStringValue()
                : item.expression.getDefaultAlias().getStringValue();
        OResultInternal elem = new OResultInternal();
        input.entrySet().forEach(x -> elem.setProperty(x.getKey(), x.getValue()));
        Object value = item.expression.execute(elem, ctx);
        if (item.expansion != null) {
          value = item.expand(expression, alias, value, ctx, recursion - 1);
        }
        result.setProperty(alias, convert(value));
      }
    }
    return result;
  }

  @Override
  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append(":{");
    boolean first = true;
    if (starItem != null) {
      starItem.toString(params, builder);
      first = false;
    }
    for (ONestedProjectionItem item : includeItems) {
      if (!first) {
        builder.append(", ");
      }
      item.toString(params, builder);
      first = false;
    }
    for (ONestedProjectionItem item : excludeItems) {
      if (!first) {
        builder.append(", ");
      }
      item.toString(params, builder);
      first = false;
    }

    builder.append("}");
    if (recursion != null) {
      builder.append("[");
      recursion.toString(params, builder);
      builder.append("]");
    }
  }

  @Override
  public void toGenericStatement(StringBuilder builder) {
    builder.append(":{");
    boolean first = true;
    if (starItem != null) {
      starItem.toGenericStatement(builder);
      first = false;
    }
    for (ONestedProjectionItem item : includeItems) {
      if (!first) {
        builder.append(", ");
      }
      item.toGenericStatement(builder);
      first = false;
    }
    for (ONestedProjectionItem item : excludeItems) {
      if (!first) {
        builder.append(", ");
      }
      item.toGenericStatement(builder);
      first = false;
    }

    builder.append("}");
    if (recursion != null) {
      builder.append("[");
      recursion.toGenericStatement(builder);
      builder.append("]");
    }
  }

  public ONestedProjection copy() {
    ONestedProjection result = new ONestedProjection(-1);
    result.includeItems = includeItems.stream().map(x -> x.copy()).collect(Collectors.toList());
    result.excludeItems = excludeItems.stream().map(x -> x.copy()).collect(Collectors.toList());
    result.starItem = starItem == null ? null : starItem.copy();
    result.recursion = recursion == null ? null : recursion.copy();
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ONestedProjection that = (ONestedProjection) o;

    if (includeItems != null ? !includeItems.equals(that.includeItems) : that.includeItems != null)
      return false;
    if (excludeItems != null ? !excludeItems.equals(that.excludeItems) : that.excludeItems != null)
      return false;
    if (starItem != null ? !starItem.equals(that.starItem) : that.starItem != null) return false;
    return recursion != null ? recursion.equals(that.recursion) : that.recursion == null;
  }

  @Override
  public int hashCode() {
    int result = includeItems != null ? includeItems.hashCode() : 0;
    result = 31 * result + (excludeItems != null ? excludeItems.hashCode() : 0);
    result = 31 * result + (starItem != null ? starItem.hashCode() : 0);
    result = 31 * result + (recursion != null ? recursion.hashCode() : 0);
    return result;
  }

  private Object convert(Object value) {
    if (value instanceof ORidBag) {
      List result = new ArrayList();
      ((ORidBag) value).forEach(x -> result.add(x));
      return result;
    }
    return value;
  }

  public OResult serialize() {
    OResultInternal result = new OResultInternal();
    if (includeItems != null) {
      result.setProperty(
          "includeItems",
          includeItems.stream().map(x -> x.serialize()).collect(Collectors.toList()));
    }
    if (excludeItems != null) {
      result.setProperty(
          "excludeItems",
          excludeItems.stream().map(x -> x.serialize()).collect(Collectors.toList()));
    }
    if (starItem != null) {
      result.setProperty("starItem", starItem.serialize());
    }
    result.setProperty("recursion", recursion);
    return result;
  }

  public void deserialize(OResult fromResult) {
    if (fromResult.getProperty("includeItems") != null) {
      includeItems = new ArrayList<>();
      List<OResult> ser = fromResult.getProperty("includeItems");
      for (OResult x : ser) {
        ONestedProjectionItem item = new ONestedProjectionItem(-1);
        item.deserialize(x);
        includeItems.add(item);
      }
    }
    if (fromResult.getProperty("excludeItems") != null) {
      excludeItems = new ArrayList<>();
      List<OResult> ser = fromResult.getProperty("excludeItems");
      for (OResult x : ser) {
        ONestedProjectionItem item = new ONestedProjectionItem(-1);
        item.deserialize(x);
        excludeItems.add(item);
      }
    }
    if (fromResult.getProperty("starItem") != null) {
      starItem = new ONestedProjectionItem(-1);
      starItem.deserialize(fromResult.getProperty("starItem"));
    }
    recursion = fromResult.getProperty("recursion");
  }

  public void addExcludeItem(ONestedProjectionItem item) {
    this.excludeItems.add(item);
  }

  public void addIncludeItem(ONestedProjectionItem item) {
    this.includeItems.add(item);
  }
}
/* JavaCC - OriginalChecksum=a7faf9beb3c058e28999b17cb43b26f6 (do not edit this line) */
