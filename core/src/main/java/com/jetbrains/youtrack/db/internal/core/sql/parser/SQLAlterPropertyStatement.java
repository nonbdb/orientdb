/* Generated By:JJTree: Do not edit this line. SQLAlterPropertyStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.api.exception.BaseException;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.api.exception.CommandExecutionException;
import com.jetbrains.youtrack.db.api.schema.Property;
import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import com.jetbrains.youtrack.db.api.exception.CommandSQLParsingException;
import com.jetbrains.youtrack.db.internal.core.sql.executor.ResultInternal;
import com.jetbrains.youtrack.db.internal.core.sql.executor.resultset.ExecutionStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class SQLAlterPropertyStatement extends DDLStatement {

  SQLIdentifier className;

  SQLIdentifier propertyName;
  SQLIdentifier customPropertyName;
  SQLExpression customPropertyValue;

  SQLIdentifier settingName;
  public SQLExpression settingValue;

  public SQLAlterPropertyStatement(int id) {
    super(id);
  }

  public SQLAlterPropertyStatement(YouTrackDBSql p, int id) {
    super(p, id);
  }

  @Override
  public ExecutionStream executeDDL(CommandContext ctx) {
    var db = ctx.getDatabase();
    SchemaClass clazz = db.getMetadata().getSchema().getClass(className.getStringValue());

    if (clazz == null) {
      throw new CommandExecutionException("Invalid class name or class not found: " + clazz);
    }

    Property property = clazz.getProperty(propertyName.getStringValue());
    if (property == null) {
      throw new CommandExecutionException(
          "Property " + propertyName.getStringValue() + " not found on class " + clazz);
    }

    ResultInternal result = new ResultInternal(db);
    result.setProperty("class", className.getStringValue());
    result.setProperty("property", propertyName.getStringValue());

    if (customPropertyName != null) {
      String customName = customPropertyName.getStringValue();
      Object oldValue = property.getCustom(customName);
      Object finalValue = customPropertyValue.execute((Identifiable) null, ctx);
      property.setCustom(db, customName, finalValue == null ? null : "" + finalValue);

      result.setProperty("operation", "alter property custom");
      result.setProperty("customAttribute", customPropertyName.getStringValue());
      result.setProperty("oldValue", oldValue != null ? oldValue.toString() : null);
      result.setProperty("newValue", finalValue != null ? finalValue.toString() : null);
    } else {
      String setting = settingName.getStringValue();
      boolean isCollate = setting.equalsIgnoreCase("collate");
      Object finalValue = settingValue.execute((Identifiable) null, ctx);
      if (finalValue == null
          && (setting.equalsIgnoreCase("name")
          || setting.equalsIgnoreCase("shortname")
          || setting.equalsIgnoreCase("type")
          || isCollate)) {
        finalValue = settingValue.toString();
        String stringFinalValue = (String) finalValue;
        if (stringFinalValue.startsWith("`")
            && stringFinalValue.endsWith("`")
            && stringFinalValue.length() > 2) {
          stringFinalValue = stringFinalValue.substring(1, stringFinalValue.length() - 1);
          finalValue = stringFinalValue;
        }
      }
      Property.ATTRIBUTES attribute;
      try {
        attribute = Property.ATTRIBUTES.valueOf(setting.toUpperCase(Locale.ENGLISH));
      } catch (IllegalArgumentException e) {
        throw BaseException.wrapException(
            new CommandExecutionException(
                "Unknown property attribute '"
                    + setting
                    + "'. Supported attributes are: "
                    + Arrays.toString(Property.ATTRIBUTES.values())),
            e);
      }
      Object oldValue = property.get(attribute);
      property.set(db, attribute, finalValue);
      finalValue = property.get(attribute); // it makes some conversions...

      result.setProperty("operation", "alter property");
      result.setProperty("attribute", setting);
      result.setProperty("oldValue", oldValue != null ? oldValue.toString() : null);
      result.setProperty("newValue", finalValue != null ? finalValue.toString() : null);
    }
    return ExecutionStream.singleton(result);
  }

  @Override
  public void validate() throws CommandSQLParsingException {
    super.validate(); // TODO
  }

  @Override
  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("ALTER PROPERTY ");
    className.toString(params, builder);
    builder.append(".");
    propertyName.toString(params, builder);
    if (customPropertyName != null) {
      builder.append(" CUSTOM ");
      customPropertyName.toString(params, builder);
      builder.append(" = ");
      customPropertyValue.toString(params, builder);
    } else {
      builder.append(" ");
      settingName.toString(params, builder);
      builder.append(" ");
      settingValue.toString(params, builder);
    }
  }

  @Override
  public void toGenericStatement(StringBuilder builder) {
    builder.append("ALTER PROPERTY ");
    className.toGenericStatement(builder);
    builder.append(".");
    propertyName.toGenericStatement(builder);
    if (customPropertyName != null) {
      builder.append(" CUSTOM ");
      customPropertyName.toGenericStatement(builder);
      builder.append(" = ");
      customPropertyValue.toGenericStatement(builder);
    } else {
      builder.append(" ");
      settingName.toGenericStatement(builder);
      builder.append(" ");
      settingValue.toGenericStatement(builder);
    }
  }

  @Override
  public SQLAlterPropertyStatement copy() {
    SQLAlterPropertyStatement result = new SQLAlterPropertyStatement(-1);
    result.className = className == null ? null : className.copy();
    result.propertyName = propertyName == null ? null : propertyName.copy();
    result.customPropertyName = customPropertyName == null ? null : customPropertyName.copy();
    result.customPropertyValue = customPropertyValue == null ? null : customPropertyValue.copy();
    result.settingName = settingName == null ? null : settingName.copy();
    result.settingValue = settingValue == null ? null : settingValue.copy();
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

    SQLAlterPropertyStatement that = (SQLAlterPropertyStatement) o;

    if (!Objects.equals(className, that.className)) {
      return false;
    }
    if (!Objects.equals(propertyName, that.propertyName)) {
      return false;
    }
    if (!Objects.equals(customPropertyName, that.customPropertyName)) {
      return false;
    }
    if (!Objects.equals(customPropertyValue, that.customPropertyValue)) {
      return false;
    }
    if (!Objects.equals(settingName, that.settingName)) {
      return false;
    }
    return Objects.equals(settingValue, that.settingValue);
  }

  @Override
  public int hashCode() {
    int result = className != null ? className.hashCode() : 0;
    result = 31 * result + (propertyName != null ? propertyName.hashCode() : 0);
    result = 31 * result + (customPropertyName != null ? customPropertyName.hashCode() : 0);
    result = 31 * result + (customPropertyValue != null ? customPropertyValue.hashCode() : 0);
    result = 31 * result + (settingName != null ? settingName.hashCode() : 0);
    result = 31 * result + (settingValue != null ? settingValue.hashCode() : 0);
    return result;
  }
}
/* JavaCC - OriginalChecksum=2421f6ad3b5f1f8e18149650ff80f1e7 (do not edit this line) */