/* Generated By:JJTree: Do not edit this line. SQLUpdateItem.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.api.exception.CommandExecutionException;
import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.api.record.Entity;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import com.jetbrains.youtrack.db.api.schema.SchemaProperty;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.record.LinkList;
import com.jetbrains.youtrack.db.internal.core.db.record.LinkSet;
import com.jetbrains.youtrack.db.internal.core.db.record.ridbag.RidBag;
import com.jetbrains.youtrack.db.internal.core.sql.executor.ResultInternal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class SQLUpdateItem extends SimpleNode {

  public static final int OPERATOR_EQ = 0;
  public static final int OPERATOR_PLUSASSIGN = 1;
  public static final int OPERATOR_MINUSASSIGN = 2;
  public static final int OPERATOR_STARASSIGN = 3;
  public static final int OPERATOR_SLASHASSIGN = 4;

  protected SQLIdentifier left;
  protected SQLModifier leftModifier;
  protected int operator;
  protected SQLExpression right;

  public SQLUpdateItem(int id) {
    super(id);
  }

  public SQLUpdateItem(YouTrackDBSql p, int id) {
    super(p, id);
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    left.toString(params, builder);
    if (leftModifier != null) {
      leftModifier.toString(params, builder);
    }
    switch (operator) {
      case OPERATOR_EQ:
        builder.append(" = ");
        break;
      case OPERATOR_PLUSASSIGN:
        builder.append(" += ");
        break;
      case OPERATOR_MINUSASSIGN:
        builder.append(" -= ");
        break;
      case OPERATOR_STARASSIGN:
        builder.append(" *= ");
        break;
      case OPERATOR_SLASHASSIGN:
        builder.append(" /= ");
        break;
    }
    right.toString(params, builder);
  }

  public void toGenericStatement(StringBuilder builder) {
    left.toGenericStatement(builder);
    if (leftModifier != null) {
      leftModifier.toGenericStatement(builder);
    }
    switch (operator) {
      case OPERATOR_EQ:
        builder.append(" = ");
        break;
      case OPERATOR_PLUSASSIGN:
        builder.append(" += ");
        break;
      case OPERATOR_MINUSASSIGN:
        builder.append(" -= ");
        break;
      case OPERATOR_STARASSIGN:
        builder.append(" *= ");
        break;
      case OPERATOR_SLASHASSIGN:
        builder.append(" /= ");
        break;
    }
    right.toGenericStatement(builder);
  }

  public SQLUpdateItem copy() {
    SQLUpdateItem result = new SQLUpdateItem(-1);
    result.left = left == null ? null : left.copy();
    result.leftModifier = leftModifier == null ? null : leftModifier.copy();
    result.operator = operator;
    result.right = right == null ? null : right.copy();
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

    SQLUpdateItem that = (SQLUpdateItem) o;

    if (operator != that.operator) {
      return false;
    }
    if (!Objects.equals(left, that.left)) {
      return false;
    }
    if (!Objects.equals(leftModifier, that.leftModifier)) {
      return false;
    }
    return Objects.equals(right, that.right);
  }

  @Override
  public int hashCode() {
    int result = left != null ? left.hashCode() : 0;
    result = 31 * result + (leftModifier != null ? leftModifier.hashCode() : 0);
    result = 31 * result + operator;
    result = 31 * result + (right != null ? right.hashCode() : 0);
    return result;
  }

  public void applyUpdate(ResultInternal entity, CommandContext ctx) {
    Object rightValue = right.execute(entity, ctx);
    SchemaClass linkedType = calculateLinkedTypeForThisItem(entity, ctx);
    if (leftModifier == null) {
      applyOperation(entity, left, rightValue, ctx);
    } else {
      var propertyName = left.getStringValue();
      rightValue = convertToType(rightValue, null, linkedType, ctx);
      Object val = entity.getProperty(propertyName);
      if (val == null) {
        val = initSchemafullCollections(entity, propertyName);
      }
      leftModifier.setValue(entity, val, rightValue, ctx);
    }
  }

  private Object initSchemafullCollections(ResultInternal entity, String propName) {
    SchemaClass oClass = entity.getEntity().flatMap(x -> x.getSchemaType()).orElse(null);
    if (oClass == null) {
      return null;
    }
    SchemaProperty prop = oClass.getProperty(propName);

    Object result = null;
    if (prop == null) {
      if (leftModifier.isArraySingleValue()) {
        result = new HashMap<>();
        entity.setProperty(propName, result);
      }
    } else {
      if (prop.getType() == PropertyType.EMBEDDEDMAP || prop.getType() == PropertyType.LINKMAP) {
        result = new HashMap<>();
        entity.setProperty(propName, result);
      } else if (prop.getType() == PropertyType.EMBEDDEDLIST
          || prop.getType() == PropertyType.LINKLIST) {
        result = new ArrayList<>();
        entity.setProperty(propName, result);
      } else if (prop.getType() == PropertyType.EMBEDDEDSET
          || prop.getType() == PropertyType.LINKSET) {
        result = new HashSet<>();
        entity.setProperty(propName, result);
      }
    }
    return result;
  }

  private SchemaClass calculateLinkedTypeForThisItem(ResultInternal entity, CommandContext ctx) {
    if (entity.isEntity()) {
      var elem = entity.toEntity();

    }
    return null;
  }

  private PropertyType calculateTypeForThisItem(ResultInternal entity, String propertyName,
      CommandContext ctx) {
    Entity elem = entity.toEntity();
    SchemaClass clazz = elem.getSchemaType().orElse(null);
    if (clazz == null) {
      return null;
    }
    return calculateTypeForThisItem(clazz, left.getStringValue(), leftModifier, ctx);
  }

  private PropertyType calculateTypeForThisItem(
      SchemaClass clazz, String propName, SQLModifier modifier, CommandContext ctx) {
    SchemaProperty prop = clazz.getProperty(propName);
    if (prop == null) {
      return null;
    }
    PropertyType type = prop.getType();
    if (type == PropertyType.LINKMAP && modifier != null) {
      if (prop.getLinkedClass() != null && modifier.next != null) {
        if (modifier.suffix == null) {
          return null;
        }
        return calculateTypeForThisItem(
            prop.getLinkedClass(), modifier.suffix.toString(), modifier.next, ctx);
      }
      return PropertyType.LINK;
    }
    // TODO specialize more
    return null;
  }

  public void applyOperation(
      ResultInternal entity, SQLIdentifier attrName, Object rightValue, CommandContext ctx) {

    switch (operator) {
      case OPERATOR_EQ:
        Object newValue = convertResultToDocument(rightValue);
        newValue = convertToPropertyType(entity, attrName, newValue, ctx);
        entity.setProperty(attrName.getStringValue(), cleanValue(newValue));
        break;
      case OPERATOR_MINUSASSIGN:
        entity.setProperty(
            attrName.getStringValue(),
            calculateNewValue(entity, ctx, SQLMathExpression.Operator.MINUS));
        break;
      case OPERATOR_PLUSASSIGN:
        entity.setProperty(
            attrName.getStringValue(),
            calculateNewValue(entity, ctx, SQLMathExpression.Operator.PLUS));
        break;
      case OPERATOR_SLASHASSIGN:
        entity.setProperty(
            attrName.getStringValue(),
            calculateNewValue(entity, ctx, SQLMathExpression.Operator.SLASH));
        break;
      case OPERATOR_STARASSIGN:
        entity.setProperty(
            attrName.getStringValue(),
            calculateNewValue(entity, ctx, SQLMathExpression.Operator.STAR));
        break;
    }
  }

  @SuppressWarnings("unchecked")
  public static Object cleanValue(Object newValue) {
    if (newValue instanceof Iterator) {
      List<Object> value = new ArrayList<Object>();
      while (((Iterator<Object>) newValue).hasNext()) {
        value.add(((Iterator<Object>) newValue).next());
      }
      return value;
    }
    return newValue;
  }

  public static Object convertToPropertyType(
      ResultInternal res, SQLIdentifier attrName, Object newValue, CommandContext ctx) {
    Entity entity = res.toEntity();
    Optional<SchemaClass> optSchema = entity.getSchemaType();
    if (!optSchema.isPresent()) {
      return newValue;
    }
    SchemaProperty prop = optSchema.get().getProperty(attrName.getStringValue());
    if (prop == null) {
      return newValue;
    }

    PropertyType type = prop.getType();
    SchemaClass linkedClass = prop.getLinkedClass();
    return convertToType(newValue, type, linkedClass, ctx);
  }

  @SuppressWarnings("unchecked")
  private static Object convertToType(
      Object value, PropertyType type, SchemaClass linkedClass, CommandContext ctx) {
    if (type == null) {
      return value;
    }
    if (value instanceof Collection) {
      if (type == PropertyType.LINK) {
        if (((Collection<?>) value).isEmpty()) {
          value = null;
        } else if (((Collection<?>) value).size() == 1) {
          value = ((Collection<?>) value).iterator().next();
        } else {
          throw new CommandExecutionException("Cannot assign a collection to a LINK property");
        }
      } else {
        if (type == PropertyType.EMBEDDEDLIST && linkedClass != null) {
          return ((Collection<?>) value)
              .stream()
              .map(item -> convertToType(item, linkedClass, ctx))
              .collect(Collectors.toList());

        } else if (type == PropertyType.EMBEDDEDSET && linkedClass != null) {
          return ((Collection<?>) value)
              .stream()
              .map(item -> convertToType(item, linkedClass, ctx))
              .collect(Collectors.toSet());
        }
        if (type == PropertyType.LINKSET && !(value instanceof LinkSet)) {
          var db = ctx.getDatabase();
          return ((Collection<?>) value)
              .stream()
              .map(item -> PropertyType.convert(db, item, Identifiable.class))
              .collect(Collectors.toSet());
        } else if (type == PropertyType.LINKLIST && !(value instanceof LinkList)) {
          var db = ctx.getDatabase();
          return ((Collection<?>) value)
              .stream()
              .map(item -> PropertyType.convert(db, item, Identifiable.class))
              .collect(Collectors.toList());
        } else if (type == PropertyType.LINKBAG && !(value instanceof RidBag)) {
          var db = ctx.getDatabase();
          var bag = new RidBag(db);

          ((Collection<?>) value)
              .stream()
              .map(item -> (Identifiable) PropertyType.convert(db, item, Identifiable.class))
              .forEach(bag::add);

        }
      }
    }
    return value;
  }

  private static Object convertToType(Object item, SchemaClass linkedClass, CommandContext ctx) {
    if (item instanceof Entity) {
      SchemaClass currentType = ((Entity) item).getSchemaType().orElse(null);
      if (currentType == null || !currentType.isSubClassOf(linkedClass)) {
        Entity result = ctx.getDatabase().newEntity(linkedClass.getName());
        for (String prop : ((Entity) item).getPropertyNames()) {
          result.setProperty(prop, ((Entity) item).getProperty(prop));
        }
        return result;
      } else {
        return item;
      }
    } else if (item instanceof Map) {
      Entity result = ctx.getDatabase().newEntity(linkedClass.getName());
      ((Map<String, Object>) item)
          .entrySet().stream().forEach(x -> result.setProperty(x.getKey(), x.getValue()));
      return result;
    }
    return item;
  }

  public static Object convertResultToDocument(Object value) {
    if (value instanceof Result) {
      return ((Result) value).toEntity();
    }
    if (value instanceof Identifiable) {
      return value;
    }
    if (value instanceof List && containsOResult((Collection) value)) {
      return ((List) value)
          .stream().map(x -> convertResultToDocument(x)).collect(Collectors.toList());
    }
    if (value instanceof Set && containsOResult((Collection) value)) {
      return ((Set) value)
          .stream().map(x -> convertResultToDocument(x)).collect(Collectors.toSet());
    }
    return value;
  }

  public static boolean containsOResult(Collection value) {
    return value.stream().anyMatch(x -> x instanceof Result);
  }

  private Object calculateNewValue(
      ResultInternal entity, CommandContext ctx, SQLMathExpression.Operator explicitOperator) {
    SQLExpression leftEx = new SQLExpression(left.copy());
    if (leftModifier != null) {
      ((SQLBaseExpression) leftEx.mathExpression).modifier = leftModifier.copy();
    }
    SQLMathExpression mathExp = new SQLMathExpression(-1);
    mathExp.addChildExpression(leftEx.getMathExpression());
    mathExp.addChildExpression(new SQLParenthesisExpression(right.copy()));
    mathExp.addOperator(explicitOperator);
    return mathExp.execute(entity, ctx);
  }

  public SQLIdentifier getLeft() {
    return left;
  }

  public void setLeft(SQLIdentifier left) {
    this.left = left;
  }

  public SQLModifier getLeftModifier() {
    return leftModifier;
  }

  public void setLeftModifier(SQLModifier leftModifier) {
    this.leftModifier = leftModifier;
  }

  public int getOperator() {
    return operator;
  }

  public void setOperator(int operator) {
    this.operator = operator;
  }

  public SQLExpression getRight() {
    return right;
  }

  public void setRight(SQLExpression right) {
    this.right = right;
  }
}
/* JavaCC - OriginalChecksum=df7444be87bba741316df8df0d653600 (do not edit this line) */
