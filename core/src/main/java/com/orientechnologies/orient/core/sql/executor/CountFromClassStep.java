package com.orientechnologies.orient.core.sql.executor;

import com.orientechnologies.common.concur.YTTimeoutException;
import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.exception.YTCommandExecutionException;
import com.orientechnologies.orient.core.metadata.schema.YTClass;
import com.orientechnologies.orient.core.metadata.schema.YTImmutableSchema;
import com.orientechnologies.orient.core.sql.executor.resultset.OExecutionStream;
import com.orientechnologies.orient.core.sql.executor.resultset.OProduceExecutionStream;
import com.orientechnologies.orient.core.sql.parser.OIdentifier;

/**
 * Returns the number of records contained in a class (including subclasses) Executes a count(*) on
 * a class and returns a single record that contains that value (with a specific alias).
 */
public class CountFromClassStep extends AbstractExecutionStep {

  private final OIdentifier target;
  private final String alias;

  /**
   * @param targetClass      An identifier containing the name of the class to count
   * @param alias            the name of the property returned in the result-set
   * @param ctx              the query context
   * @param profilingEnabled true to enable the profiling of the execution (for SQL PROFILE)
   */
  public CountFromClassStep(
      OIdentifier targetClass, String alias, OCommandContext ctx, boolean profilingEnabled) {
    super(ctx, profilingEnabled);
    this.target = targetClass;
    this.alias = alias;
  }

  @Override
  public OExecutionStream internalStart(OCommandContext ctx) throws YTTimeoutException {
    if (prev != null) {
      prev.start(ctx).close(ctx);
    }

    return new OProduceExecutionStream(this::produce).limit(1);
  }

  private OResult produce(OCommandContext ctx) {
    var db = ctx.getDatabase();
    YTImmutableSchema schema = db.getMetadata().getImmutableSchemaSnapshot();
    YTClass clazz = schema.getClass(target.getStringValue());
    if (clazz == null) {
      clazz = schema.getView(target.getStringValue());
    }
    if (clazz == null) {
      throw new YTCommandExecutionException(
          "Class " + target.getStringValue() + " does not exist in the database schema");
    }
    long size = clazz.count(db);
    OResultInternal result = new OResultInternal(db);
    result.setProperty(alias, size);
    return result;
  }

  @Override
  public String prettyPrint(int depth, int indent) {
    String spaces = OExecutionStepInternal.getIndent(depth, indent);
    String result = spaces + "+ CALCULATE CLASS SIZE: " + target;
    if (profilingEnabled) {
      result += " (" + getCostFormatted() + ")";
    }
    return result;
  }

  @Override
  public boolean canBeCached() {
    return false; // explicit: in case of active security policies, the COUNT has to be manual
  }
}
