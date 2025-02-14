package com.jetbrains.youtrack.db.internal.core.sql.executor;

import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.sql.executor.resultset.ExecutionStream;

/**
 *
 */
public class OptionalMatchEdgeTraverser extends MatchEdgeTraverser {

  public static final Result EMPTY_OPTIONAL = new ResultInternal(null);

  public OptionalMatchEdgeTraverser(Result lastUpstreamRecord, EdgeTraversal edge) {
    super(lastUpstreamRecord, edge);
  }

  protected void init(CommandContext ctx) {
    if (downstream == null) {
      super.init(ctx);
      if (!downstream.hasNext(ctx)) {
        downstream = ExecutionStream.singleton(EMPTY_OPTIONAL);
      }
    }
  }

  public Result next(CommandContext ctx) {
    init(ctx);
    if (!downstream.hasNext(ctx)) {
      throw new IllegalStateException();
    }

    var endPointAlias = getEndpointAlias();
    var prevValue = sourceRecord.getProperty(endPointAlias);
    var next = downstream.next(ctx);

    if (isEmptyOptional(prevValue)) {
      return sourceRecord;
    }
    if (!isEmptyOptional(next)) {
      if (prevValue != null && !equals(prevValue, next.castToEntity())) {
        return null;
      }
    }

    var session = ctx.getDatabaseSession();
    var result = new ResultInternal(session);
    for (var prop : sourceRecord.getPropertyNames()) {
      result.setProperty(prop, sourceRecord.getProperty(prop));
    }
    if (next.isEntity()) {
      result.setProperty(endPointAlias, toResult(session, next.castToEntity()));
    } else {
      result.setProperty(endPointAlias, null);
    }

    return result;
  }

  public static boolean isEmptyOptional(Object elem) {
    return elem == EMPTY_OPTIONAL;
  }
}
