/* Generated By:JJTree: Do not edit this line. SQLInstanceofCondition.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.api.exception.RecordNotFoundException;
import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.api.record.DBRecord;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityInternalUtils;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.StringSerializerHelper;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class SQLInstanceofCondition extends SQLBooleanExpression {

  protected SQLExpression left;
  protected SQLIdentifier right;
  protected String rightString;

  public SQLInstanceofCondition(int id) {
    super(id);
  }

  public SQLInstanceofCondition(YouTrackDBSql p, int id) {
    super(p, id);
  }

  @Override
  public boolean evaluate(Identifiable currentRecord, CommandContext ctx) {
    if (currentRecord == null) {
      return false;
    }
    DBRecord record;
    try {
      record = currentRecord.getRecord(ctx.getDatabase());
    } catch (RecordNotFoundException rnf) {
      return false;
    }

    if (!(record instanceof EntityImpl entity)) {
      return false;
    }
    SchemaClass clazz = EntityInternalUtils.getImmutableSchemaClass(entity);
    if (clazz == null) {
      return false;
    }
    if (right != null) {
      return clazz.isSubClassOf(right.getStringValue());
    } else if (rightString != null) {
      return clazz.isSubClassOf(decode(rightString));
    }
    return false;
  }

  @Override
  public boolean evaluate(Result currentRecord, CommandContext ctx) {
    if (currentRecord == null) {
      return false;
    }
    if (!currentRecord.isEntity()) {
      return false;
    }

    var record = currentRecord.getEntity().get().getRecord(ctx.getDatabase());
    if (!(record instanceof EntityImpl entity)) {
      return false;
    }
    SchemaClass clazz = EntityInternalUtils.getImmutableSchemaClass(entity);
    if (clazz == null) {
      return false;
    }
    if (right != null) {
      return clazz.isSubClassOf(right.getStringValue());
    } else if (rightString != null) {
      return clazz.isSubClassOf(decode(rightString));
    }
    return false;
  }

  private String decode(String rightString) {
    if (rightString == null) {
      return null;
    }
    return StringSerializerHelper.decode(rightString.substring(1, rightString.length() - 1));
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    left.toString(params, builder);
    builder.append(" instanceof ");
    if (right != null) {
      right.toString(params, builder);
    } else if (rightString != null) {
      builder.append(rightString);
    }
  }

  public void toGenericStatement(StringBuilder builder) {
    left.toGenericStatement(builder);
    builder.append(" instanceof ");
    if (right != null) {
      right.toGenericStatement(builder);
    } else if (rightString != null) {
      builder.append(rightString);
    }
  }

  @Override
  public boolean supportsBasicCalculation() {
    return left.supportsBasicCalculation();
  }

  @Override
  protected int getNumberOfExternalCalculations() {
    if (!left.supportsBasicCalculation()) {
      return 1;
    }
    return 0;
  }

  @Override
  protected List<Object> getExternalCalculationConditions() {
    if (!left.supportsBasicCalculation()) {
      return Collections.singletonList(left);
    }
    return Collections.EMPTY_LIST;
  }

  @Override
  public boolean needsAliases(Set<String> aliases) {
    return left.needsAliases(aliases);
  }

  @Override
  public SQLInstanceofCondition copy() {
    var result = new SQLInstanceofCondition(-1);
    result.left = left.copy();
    result.right = right == null ? null : right.copy();
    result.rightString = rightString;
    return result;
  }

  @Override
  public void extractSubQueries(SubQueryCollector collector) {
    left.extractSubQueries(collector);
  }

  @Override
  public boolean refersToParent() {
    return left != null && left.refersToParent();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    var that = (SQLInstanceofCondition) o;

    if (!Objects.equals(left, that.left)) {
      return false;
    }
    if (!Objects.equals(right, that.right)) {
      return false;
    }
    return Objects.equals(rightString, that.rightString);
  }

  @Override
  public int hashCode() {
    var result = left != null ? left.hashCode() : 0;
    result = 31 * result + (right != null ? right.hashCode() : 0);
    result = 31 * result + (rightString != null ? rightString.hashCode() : 0);
    return result;
  }

  @Override
  public List<String> getMatchPatternInvolvedAliases() {
    return left == null ? null : left.getMatchPatternInvolvedAliases();
  }

  @Override
  public boolean isCacheable(DatabaseSessionInternal session) {
    return left.isCacheable(session);
  }
}
/* JavaCC - OriginalChecksum=0b5eb529744f307228faa6b26f0592dc (do not edit this line) */
