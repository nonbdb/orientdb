/* Generated By:JJTree: Do not edit this line. OAlterSequenceStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.orientechnologies.orient.core.sql.parser;

import com.orientechnologies.common.log.OLogManager;
import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.exception.OCommandExecutionException;
import com.orientechnologies.orient.core.exception.ODatabaseException;
import com.orientechnologies.orient.core.metadata.sequence.OSequence;
import com.orientechnologies.orient.core.metadata.sequence.SequenceOrderType;
import com.orientechnologies.orient.core.sql.executor.OResultInternal;
import com.orientechnologies.orient.core.sql.executor.resultset.OExecutionStream;
import java.util.Map;
import java.util.Objects;

public class OAlterSequenceStatement extends ODDLStatement {
  OIdentifier name;
  OExpression start;
  OExpression increment;
  OExpression cache;
  Boolean positive;
  Boolean cyclic;
  OExpression limitValue;
  boolean turnLimitOff = false;

  public OAlterSequenceStatement(int id) {
    super(id);
  }

  public OAlterSequenceStatement(OrientSql p, int id) {
    super(p, id);
  }

  @Override
  public OExecutionStream executeDDL(OCommandContext ctx) {

    String sequenceName = name.getStringValue();

    if (sequenceName == null) {
      throw new OCommandExecutionException(
          "Cannot execute the command because it has not been parsed yet");
    }
    final var database = getDatabase();
    OSequence sequence = database.getMetadata().getSequenceLibrary().getSequence(sequenceName);
    if (sequence == null) {
      throw new OCommandExecutionException("Sequence not found: " + sequenceName);
    }

    OSequence.CreateParams params = new OSequence.CreateParams();
    params.resetNull();

    if (start != null) {
      Object val = start.execute((OIdentifiable) null, ctx);
      if (!(val instanceof Number)) {
        throw new OCommandExecutionException("invalid start value for a sequence: " + val);
      }
      params.setStart(((Number) val).longValue());
    }
    if (increment != null) {
      Object val = increment.execute((OIdentifiable) null, ctx);
      if (!(val instanceof Number)) {
        throw new OCommandExecutionException("invalid increment value for a sequence: " + val);
      }
      params.setIncrement(((Number) val).intValue());
    }
    if (cache != null) {
      Object val = cache.execute((OIdentifiable) null, ctx);
      if (!(val instanceof Number)) {
        throw new OCommandExecutionException("invalid cache value for a sequence: " + val);
      }
      params.setCacheSize(((Number) val).intValue());
    }
    if (positive != null) {
      params.setOrderType(
          positive == true ? SequenceOrderType.ORDER_POSITIVE : SequenceOrderType.ORDER_NEGATIVE);
    }
    if (cyclic != null) {
      params.setRecyclable(cyclic);
    }
    if (limitValue != null) {
      Object val = limitValue.execute((OIdentifiable) null, ctx);
      if (!(val instanceof Number)) {
        throw new OCommandExecutionException("invalid cache value for a sequence: " + val);
      }
      params.setLimitValue(((Number) val).longValue());
    }
    if (turnLimitOff) {
      params.setTurnLimitOff(true);
    }

    try {
      sequence.updateParams(params);
    } catch (ODatabaseException exc) {
      String message = "Unable to execute command: " + exc.getMessage();
      OLogManager.instance().error(this, message, exc, (Object) null);
      throw new OCommandExecutionException(message);
    }

    OResultInternal item = new OResultInternal();
    item.setProperty("operation", "alter sequence");
    item.setProperty("sequenceName", sequenceName);
    if (params.getStart() != null) {
      item.setProperty("start", params.getStart());
    }
    if (params.getIncrement() != null) {
      item.setProperty("increment", params.getIncrement());
    }
    if (params.getCacheSize() != null) {
      item.setProperty("cacheSize", params.getCacheSize());
    }
    if (params.getLimitValue() != null) {
      item.setProperty("limitValue", params.getLimitValue());
    }
    if (params.getOrderType() != null) {
      item.setProperty("orderType", params.getOrderType().toString());
    }
    if (params.getRecyclable() != null) {
      item.setProperty("recycable", params.getRecyclable());
    }
    if (params.getTurnLimitOff() != null && params.getTurnLimitOff()) {
      item.setProperty("turnLimitOff", params.getTurnLimitOff());
    }
    return OExecutionStream.singleton(item);
  }

  @Override
  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("ALTER SEQUENCE ");
    name.toString(params, builder);

    if (start != null) {
      builder.append(" START ");
      start.toString(params, builder);
    }
    if (increment != null) {
      builder.append(" INCREMENT ");
      increment.toString(params, builder);
    }
    if (cache != null) {
      builder.append(" CACHE ");
      cache.toString(params, builder);
    }
    if (positive != null) {
      String appendString;
      appendString = positive == true ? " ASC" : " DESC";
      builder.append(appendString);
    }
    if (cyclic != null) {
      builder.append(" CYCLE ").append(cyclic.toString().toUpperCase());
    }
    if (limitValue != null) {
      builder.append(" LIMIT ");
      limitValue.toString(params, builder);
    }
    if (turnLimitOff) {
      builder.append(" NOLIMIT");
    }
  }

  @Override
  public void toGenericStatement(StringBuilder builder) {
    builder.append("ALTER SEQUENCE ");
    name.toGenericStatement(builder);

    if (start != null) {
      builder.append(" START ");
      start.toGenericStatement(builder);
    }
    if (increment != null) {
      builder.append(" INCREMENT ");
      increment.toGenericStatement(builder);
    }
    if (cache != null) {
      builder.append(" CACHE ");
      cache.toGenericStatement(builder);
    }
    if (positive != null) {
      String appendString;
      appendString = positive == true ? " ASC" : " DESC";
      builder.append(appendString);
    }
    if (cyclic != null) {
      builder.append(" CYCLE ").append(PARAMETER_PLACEHOLDER);
    }
    if (limitValue != null) {
      builder.append(" LIMIT ");
      limitValue.toGenericStatement(builder);
    }
    if (turnLimitOff) {
      builder.append(" NOLIMIT");
    }
  }

  @Override
  public OAlterSequenceStatement copy() {
    OAlterSequenceStatement result = new OAlterSequenceStatement(-1);
    result.name = name == null ? null : name.copy();
    result.start = start == null ? null : start.copy();
    result.increment = increment == null ? null : increment.copy();
    result.cache = cache == null ? null : cache.copy();
    result.positive = positive;
    result.cyclic = cyclic;
    result.limitValue = limitValue == null ? null : limitValue.copy();
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    OAlterSequenceStatement that = (OAlterSequenceStatement) o;

    if (name != null ? !name.equals(that.name) : that.name != null) return false;
    if (start != null ? !start.equals(that.start) : that.start != null) return false;
    if (increment != null ? !increment.equals(that.increment) : that.increment != null)
      return false;
    if (cache != null ? !cache.equals(that.cache) : that.cache != null) return false;
    if (!Objects.equals(positive, that.positive)) {
      return false;
    }
    if (!Objects.equals(cyclic, that.cyclic)) {
      return false;
    }
    if (!Objects.equals(limitValue, that.limitValue)) {
      return false;
    }
    if (turnLimitOff != that.turnLimitOff) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (start != null ? start.hashCode() : 0);
    result = 31 * result + (increment != null ? increment.hashCode() : 0);
    result = 31 * result + (cache != null ? cache.hashCode() : 0);
    result = 31 * result + (positive != null ? positive.hashCode() : 0);
    result = 31 * result + (cyclic != null ? cyclic.hashCode() : 0);
    result = 31 * result + (limitValue != null ? limitValue.hashCode() : 0);
    result = 31 * result + Boolean.hashCode(turnLimitOff);
    return result;
  }
}
/* JavaCC - OriginalChecksum=def62b1d04db5223307fe51873a9edd0 (do not edit this line) */
