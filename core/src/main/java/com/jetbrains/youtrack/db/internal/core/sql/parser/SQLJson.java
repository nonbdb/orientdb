/* Generated By:JJTree: Do not edit this line. SQLJson.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.api.record.Entity;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityHelper;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.string.FieldTypesString;
import com.jetbrains.youtrack.db.internal.core.sql.executor.ResultInternal;
import com.jetbrains.youtrack.db.internal.core.sql.executor.UpdatableResult;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class SQLJson extends SimpleNode {

  protected List<SQLJsonItem> items = new ArrayList<SQLJsonItem>();

  public SQLJson(int id) {
    super(id);
  }

  public SQLJson(YouTrackDBSql p, int id) {
    super(p, id);
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("{");
    boolean first = true;
    for (SQLJsonItem item : items) {
      if (!first) {
        builder.append(", ");
      }
      item.toString(params, builder);

      first = false;
    }
    builder.append("}");
  }

  public void toGenericStatement(StringBuilder builder) {
    builder.append("{");
    boolean first = true;
    for (SQLJsonItem item : items) {
      if (!first) {
        builder.append(", ");
      }
      item.toGenericStatement(builder);

      first = false;
    }
    builder.append("}");
  }

  public EntityImpl toEntity(Identifiable source, CommandContext ctx) {
    String className = getClassNameForDocument(ctx);
    var db = ctx.getDatabase();
    EntityImpl entity;
    if (className != null) {
      entity = new EntityImpl(db, className);
    } else {
      entity = new EntityImpl(db);
    }
    for (SQLJsonItem item : items) {
      String name = item.getLeftValue();
      if (name == null) {
        continue;
      }
      Object value;
      if (item.right.value instanceof SQLJson) {
        value = ((SQLJson) item.right.value).toEntity(source, ctx);
      } else {
        value = item.right.execute(source, ctx);
      }
      entity.field(name, value);
    }

    return entity;
  }

  private EntityImpl toEntity(Result source, CommandContext ctx, String className) {
    EntityImpl retDoc = new EntityImpl(ctx.getDatabase(), className);
    Map<String, Character> types = null;
    for (SQLJsonItem item : items) {
      String name = item.getLeftValue();
      if (name == null
          || EntityHelper.getReservedAttributes().contains(name.toLowerCase(Locale.ENGLISH))) {
        if (name.equals(FieldTypesString.ATTRIBUTE_FIELD_TYPES)) {
          Object value = item.right.execute(source, ctx);
          types = FieldTypesString.loadFieldTypes(value.toString());
          for (Map.Entry<String, Character> entry : types.entrySet()) {
            PropertyType t = FieldTypesString.getOTypeFromChar(entry.getValue());
            retDoc.setFieldType(entry.getKey(), t);
          }
        }
        continue;
      }
      Object value = item.right.execute(source, ctx);
      Character charType;
      if (types != null) {
        charType = types.get(name);
      } else {
        charType = null;
      }
      if (charType != null) {
        PropertyType t = FieldTypesString.getOTypeFromChar(charType);
        retDoc.setProperty(name, value, t);
      } else {
        retDoc.setPropertyInternal(name, value);
      }
    }
    return retDoc;
  }

  /**
   * choosing return type is based on existence of @class and @type field in JSON
   *
   * @param source
   * @param ctx
   * @return
   */
  public Object toObjectDetermineType(Result source, CommandContext ctx) {
    String className = getClassNameForDocument(ctx);
    String type = getTypeForDocument(ctx);
    if (className != null || ("d".equalsIgnoreCase(type))) {
      return toEntity(source, ctx, className);
    } else {
      return toMap(source, ctx);
    }
  }

  public Object toObjectDetermineType(Identifiable source, CommandContext ctx) {
    var db = ctx.getDatabase();
    String className = getClassNameForDocument(ctx);
    String type = getTypeForDocument(ctx);
    if (className != null || ("d".equalsIgnoreCase(type))) {
      UpdatableResult element = null;
      if (source != null) {
        var identity = source.getIdentity();
        if (identity.isPersistent()) {
          element = new UpdatableResult(db, db.load(source.getIdentity()));
        } else if (identity instanceof Entity el) {
          element = new UpdatableResult(db, el);
        }
      }
      return toEntity(element, ctx, className);
    } else {
      return toMap(source, ctx);
    }
  }

  public Map<String, Object> toMap(Identifiable source, CommandContext ctx) {
    Map<String, Object> map = new LinkedHashMap<String, Object>();
    for (SQLJsonItem item : items) {
      String name = item.getLeftValue();
      if (name == null) {
        continue;
      }
      Object value = item.right.execute(source, ctx);
      map.put(name, value);
    }

    return map;
  }

  public Map<String, Object> toMap(Result source, CommandContext ctx) {
    Map<String, Object> map = new LinkedHashMap<String, Object>();
    for (SQLJsonItem item : items) {
      String name = item.getLeftValue();
      if (name == null) {
        continue;
      }
      Object value = item.right.execute(source, ctx);
      map.put(name, value);
    }

    return map;
  }

  private String getClassNameForDocument(CommandContext ctx) {
    for (SQLJsonItem item : items) {
      String left = item.getLeftValue();
      if (left != null && left.toLowerCase(Locale.ENGLISH).equals("@class")) {
        return "" + item.right.execute((Result) null, ctx);
      }
    }
    return null;
  }

  private String getTypeForDocument(CommandContext ctx) {
    for (SQLJsonItem item : items) {
      String left = item.getLeftValue();
      if (left != null && left.toLowerCase(Locale.ENGLISH).equals("@type")) {
        return "" + item.right.execute((Result) null, ctx);
      }
    }
    return null;
  }

  public boolean needsAliases(Set<String> aliases) {
    for (SQLJsonItem item : items) {
      if (item.needsAliases(aliases)) {
        return true;
      }
    }
    return false;
  }

  public boolean isAggregate(DatabaseSessionInternal session) {
    for (SQLJsonItem item : items) {
      if (item.isAggregate(session)) {
        return true;
      }
    }
    return false;
  }

  public SQLJson splitForAggregation(AggregateProjectionSplit aggregateSplit, CommandContext ctx) {
    if (isAggregate(ctx.getDatabase())) {
      SQLJson result = new SQLJson(-1);
      for (SQLJsonItem item : items) {
        result.items.add(item.splitForAggregation(aggregateSplit, ctx));
      }
      return result;
    } else {
      return this;
    }
  }

  public SQLJson copy() {
    SQLJson result = new SQLJson(-1);
    result.items = items.stream().map(x -> x.copy()).collect(Collectors.toList());
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

    SQLJson oJson = (SQLJson) o;

    return Objects.equals(items, oJson.items);
  }

  @Override
  public int hashCode() {
    return items != null ? items.hashCode() : 0;
  }

  public void extractSubQueries(SubQueryCollector collector) {
    for (SQLJsonItem item : items) {
      item.extractSubQueries(collector);
    }
  }

  public boolean refersToParent() {
    for (SQLJsonItem item : items) {
      if (item.refersToParent()) {
        return true;
      }
    }
    return false;
  }

  public Result serialize(DatabaseSessionInternal db) {
    ResultInternal result = new ResultInternal(db);
    if (items != null) {
      result.setProperty(
          "items",
          items.stream().map(oJsonItem -> oJsonItem.serialize(db)).collect(Collectors.toList()));
    }
    return result;
  }

  public void deserialize(Result fromResult) {

    if (fromResult.getProperty("items") != null) {
      List<Result> ser = fromResult.getProperty("items");
      items = new ArrayList<>();
      for (Result r : ser) {
        SQLJsonItem exp = new SQLJsonItem();
        exp.deserialize(r);
        items.add(exp);
      }
    }
  }

  public void addItem(SQLJsonItem item) {
    this.items.add(item);
  }

  public boolean isCacheable() {
    return false; // TODO optimize
  }
}
/* JavaCC - OriginalChecksum=3beec9f6db486de944498588b51a505d (do not edit this line) */
