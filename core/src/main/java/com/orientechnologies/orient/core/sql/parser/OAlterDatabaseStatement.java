/* Generated By:JJTree: Do not edit this line. OAlterDatabaseStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.orientechnologies.orient.core.sql.parser;

import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.config.OStorageEntryConfiguration;
import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.db.ODatabaseSessionInternal;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.core.metadata.security.ORule;
import com.orientechnologies.orient.core.sql.executor.OResult;
import com.orientechnologies.orient.core.sql.executor.OResultInternal;
import com.orientechnologies.orient.core.sql.executor.resultset.OExecutionStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class OAlterDatabaseStatement extends ODDLStatement {

  OIdentifier customPropertyName;
  OExpression customPropertyValue;

  OIdentifier settingName;
  OExpression settingValue;

  public OAlterDatabaseStatement(int id) {
    super(id);
  }

  public OAlterDatabaseStatement(OrientSql p, int id) {
    super(p, id);
  }

  @Override
  public OExecutionStream executeDDL(OCommandContext ctx) {
    if (customPropertyName == null) {
      return OExecutionStream.singleton(executeSimpleAlter(settingName, settingValue, ctx));
    } else {
      return OExecutionStream.singleton(
          executeCustomAlter(customPropertyName, customPropertyValue, ctx));
    }
  }

  private OResult executeCustomAlter(
      OIdentifier customPropertyName, OExpression customPropertyValue, OCommandContext ctx) {
    ODatabaseSessionInternal db = ctx.getDatabase();
    db.checkSecurity(ORule.ResourceGeneric.DATABASE, ORole.PERMISSION_UPDATE);
    List<OStorageEntryConfiguration> oldValues =
        (List<OStorageEntryConfiguration>) db.get(ODatabaseSession.ATTRIBUTES.CUSTOM);
    String oldValue = null;
    if (oldValues != null) {
      for (OStorageEntryConfiguration entry : oldValues) {
        if (entry.name.equals(customPropertyName.getStringValue())) {
          oldValue = entry.value;
          break;
        }
      }
    }
    Object finalValue = customPropertyValue.execute((OIdentifiable) null, ctx);
    db.setCustom(customPropertyName.getStringValue(), finalValue);

    OResultInternal result = new OResultInternal();
    result.setProperty("operation", "alter database");
    result.setProperty("customAttribute", customPropertyName.getStringValue());
    result.setProperty("oldValue", oldValue);
    result.setProperty("newValue", finalValue);
    return result;
  }

  private OResult executeSimpleAlter(
      OIdentifier settingName, OExpression settingValue, OCommandContext ctx) {
    ODatabaseSession.ATTRIBUTES attribute =
        ODatabaseSession.ATTRIBUTES.valueOf(
            settingName.getStringValue().toUpperCase(Locale.ENGLISH));
    ODatabaseSessionInternal db = ctx.getDatabase();
    db.checkSecurity(ORule.ResourceGeneric.DATABASE, ORole.PERMISSION_UPDATE);
    Object oldValue = db.get(attribute);
    Object finalValue = settingValue.execute((OIdentifiable) null, ctx);
    db.setInternal(attribute, finalValue);

    OResultInternal result = new OResultInternal();
    result.setProperty("operation", "alter database");
    result.setProperty("attribute", settingName.getStringValue());
    result.setProperty("oldValue", oldValue);
    result.setProperty("newValue", finalValue);
    return result;
  }

  @Override
  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("ALTER DATABASE ");

    if (customPropertyName != null) {
      builder.append("CUSTOM ");
      customPropertyName.toString(params, builder);
      builder.append(" = ");
      customPropertyValue.toString(params, builder);
    } else {
      settingName.toString(params, builder);
      builder.append(" ");
      settingValue.toString(params, builder);
    }
  }

  @Override
  public void toGenericStatement(StringBuilder builder) {
    builder.append("ALTER DATABASE ");

    if (customPropertyName != null) {
      builder.append("CUSTOM ");
      customPropertyName.toGenericStatement(builder);
      builder.append(" = ");
      customPropertyValue.toGenericStatement(builder);
    } else {
      settingName.toGenericStatement(builder);
      builder.append(" ");
      settingValue.toGenericStatement(builder);
    }
  }

  @Override
  public OAlterDatabaseStatement copy() {
    OAlterDatabaseStatement result = new OAlterDatabaseStatement(-1);
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

    OAlterDatabaseStatement that = (OAlterDatabaseStatement) o;

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
    int result = customPropertyName != null ? customPropertyName.hashCode() : 0;
    result = 31 * result + (customPropertyValue != null ? customPropertyValue.hashCode() : 0);
    result = 31 * result + (settingName != null ? settingName.hashCode() : 0);
    result = 31 * result + (settingValue != null ? settingValue.hashCode() : 0);
    return result;
  }
}
/* JavaCC - OriginalChecksum=8fec57db8dd2a3b52aaa52dec7367cd4 (do not edit this line) */
