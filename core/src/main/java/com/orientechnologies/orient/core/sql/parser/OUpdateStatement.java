/* Generated By:JJTree: Do not edit this line. OUpdateStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.orientechnologies.orient.core.sql.parser;

import com.orientechnologies.orient.core.command.OBasicCommandContext;
import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.YTDatabaseSessionInternal;
import com.orientechnologies.orient.core.exception.YTDatabaseException;
import com.orientechnologies.orient.core.sql.executor.OResultSet;
import com.orientechnologies.orient.core.sql.executor.OUpdateExecutionPlan;
import com.orientechnologies.orient.core.sql.executor.OUpdateExecutionPlanner;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class OUpdateStatement extends OStatement {

  public OFromClause target;

  protected List<OUpdateOperations> operations = new ArrayList<OUpdateOperations>();

  protected boolean upsert = false;

  protected boolean returnBefore = false;
  protected boolean returnAfter = false;
  protected boolean returnCount = false;
  protected OProjection returnProjection;

  public OWhereClause whereClause;

  public OLimit limit;
  public OTimeout timeout;

  public OUpdateStatement(int id) {
    super(id);
  }

  public OUpdateStatement(OrientSql p, int id) {
    super(p, id);
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append(getStatementType());
    if (target != null) {
      target.toString(params, builder);
    }

    for (OUpdateOperations ops : this.operations) {
      builder.append(" ");
      ops.toString(params, builder);
    }

    if (upsert) {
      builder.append(" UPSERT");
    }

    if (returnBefore || returnAfter || returnCount) {
      builder.append(" RETURN");
      if (returnBefore) {
        throw new YTDatabaseException("BEFORE is not supported");
      } else if (returnAfter) {
        builder.append(" AFTER");
      } else {
        builder.append(" COUNT");
      }
      if (returnProjection != null) {
        builder.append(" ");
        returnProjection.toString(params, builder);
      }
    }
    if (whereClause != null) {
      builder.append(" WHERE ");
      whereClause.toString(params, builder);
    }

    if (limit != null) {
      limit.toString(params, builder);
    }
    if (timeout != null) {
      timeout.toString(params, builder);
    }
  }

  public void toGenericStatement(StringBuilder builder) {
    builder.append(getStatementType());
    if (target != null) {
      target.toGenericStatement(builder);
    }

    for (OUpdateOperations ops : this.operations) {
      builder.append(" ");
      ops.toGenericStatement(builder);
    }

    if (upsert) {
      builder.append(" UPSERT");
    }

    if (returnBefore || returnAfter || returnCount) {
      builder.append(" RETURN");
      if (returnBefore) {
        throw new YTDatabaseException("BEFORE is not supported");
      } else if (returnAfter) {
        builder.append(" AFTER");
      } else {
        builder.append(" COUNT");
      }
      if (returnProjection != null) {
        builder.append(" ");
        returnProjection.toGenericStatement(builder);
      }
    }
    if (whereClause != null) {
      builder.append(" WHERE ");
      whereClause.toGenericStatement(builder);
    }

    if (limit != null) {
      limit.toGenericStatement(builder);
    }
    if (timeout != null) {
      timeout.toGenericStatement(builder);
    }
  }

  protected String getStatementType() {
    return "UPDATE ";
  }

  @Override
  public OUpdateStatement copy() {
    OUpdateStatement result = null;
    try {
      result = getClass().getConstructor(Integer.TYPE).newInstance(-1);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    result.target = target == null ? null : target.copy();
    result.operations =
        operations == null
            ? null
            : operations.stream().map(x -> x.copy()).collect(Collectors.toList());
    result.upsert = upsert;
    result.returnBefore = returnBefore;
    result.returnAfter = returnAfter;
    result.returnProjection = returnProjection == null ? null : returnProjection.copy();
    result.whereClause = whereClause == null ? null : whereClause.copy();
    result.limit = limit == null ? null : limit.copy();
    result.timeout = timeout == null ? null : timeout.copy();
    return result;
  }

  @Override
  public OResultSet execute(
      YTDatabaseSessionInternal db, Object[] args, OCommandContext parentCtx,
      boolean usePlanCache) {
    OBasicCommandContext ctx = new OBasicCommandContext();
    if (parentCtx != null) {
      ctx.setParentWithoutOverridingChild(parentCtx);
    }
    ctx.setDatabase(db);
    Map<Object, Object> params = new HashMap<>();
    if (args != null) {
      for (int i = 0; i < args.length; i++) {
        params.put(i, args[i]);
      }
    }
    ctx.setInputParameters(params);
    OUpdateExecutionPlan executionPlan;
    if (usePlanCache) {
      executionPlan = createExecutionPlan(ctx, false);
    } else {
      executionPlan = (OUpdateExecutionPlan) createExecutionPlanNoCache(ctx, false);
    }
    executionPlan.executeInternal();
    return new OLocalResultSet(executionPlan);
  }

  @Override
  public OResultSet execute(
      YTDatabaseSessionInternal db, Map params, OCommandContext parentCtx, boolean usePlanCache) {
    OBasicCommandContext ctx = new OBasicCommandContext();
    if (parentCtx != null) {
      ctx.setParentWithoutOverridingChild(parentCtx);
    }
    ctx.setDatabase(db);
    ctx.setInputParameters(params);
    OUpdateExecutionPlan executionPlan;
    if (usePlanCache) {
      executionPlan = createExecutionPlan(ctx, false);
    } else {
      executionPlan = (OUpdateExecutionPlan) createExecutionPlanNoCache(ctx, false);
    }
    executionPlan.executeInternal();
    return new OLocalResultSet(executionPlan);
  }

  public OUpdateExecutionPlan createExecutionPlan(OCommandContext ctx, boolean enableProfiling) {
    OUpdateExecutionPlanner planner = new OUpdateExecutionPlanner(this);
    OUpdateExecutionPlan result = planner.createExecutionPlan(ctx, enableProfiling);
    result.setStatement(this.originalStatement);
    result.setGenericStatement(this.toGenericStatement());
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

    OUpdateStatement that = (OUpdateStatement) o;

    if (upsert != that.upsert) {
      return false;
    }
    if (returnBefore != that.returnBefore) {
      return false;
    }
    if (returnAfter != that.returnAfter) {
      return false;
    }
    if (!Objects.equals(target, that.target)) {
      return false;
    }
    if (!Objects.equals(operations, that.operations)) {
      return false;
    }
    if (!Objects.equals(returnProjection, that.returnProjection)) {
      return false;
    }
    if (!Objects.equals(whereClause, that.whereClause)) {
      return false;
    }
    if (!Objects.equals(limit, that.limit)) {
      return false;
    }
    return Objects.equals(timeout, that.timeout);
  }

  @Override
  public int hashCode() {
    int result = target != null ? target.hashCode() : 0;
    result = 31 * result + (operations != null ? operations.hashCode() : 0);
    result = 31 * result + (upsert ? 1 : 0);
    result = 31 * result + (returnBefore ? 1 : 0);
    result = 31 * result + (returnAfter ? 1 : 0);
    result = 31 * result + (returnProjection != null ? returnProjection.hashCode() : 0);
    result = 31 * result + (whereClause != null ? whereClause.hashCode() : 0);
    result = 31 * result + (limit != null ? limit.hashCode() : 0);
    result = 31 * result + (timeout != null ? timeout.hashCode() : 0);
    return result;
  }

  public OFromClause getTarget() {
    return target;
  }

  public List<OUpdateOperations> getOperations() {
    return operations;
  }

  public void addOperations(OUpdateOperations op) {
    this.operations.add(op);
  }

  public boolean isUpsert() {
    return upsert;
  }

  public boolean isReturnBefore() {
    if (returnBefore) {
      throw new YTDatabaseException("BEFORE is not supported");
    }

    return false;
  }

  public boolean isReturnAfter() {
    return returnAfter;
  }

  public boolean isReturnCount() {
    return returnCount;
  }

  public OProjection getReturnProjection() {
    return returnProjection;
  }

  public OWhereClause getWhereClause() {
    return whereClause;
  }

  public OLimit getLimit() {
    return limit;
  }

  public OTimeout getTimeout() {
    return timeout;
  }
}
/* JavaCC - OriginalChecksum=093091d7273f1073ad49f2a2bf709a53 (do not edit this line) */
