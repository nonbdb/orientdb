package com.jetbrains.youtrack.db.internal.core.sql.executor;

import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.api.record.RID;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.record.ridbag.RidBag;
import com.jetbrains.youtrack.db.internal.core.id.RecordId;
import com.jetbrains.youtrack.db.internal.core.sql.executor.resultset.ExecutionStream;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLInteger;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLTraverseProjectionItem;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLWhereClause;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class DepthFirstTraverseStep extends AbstractTraverseStep {

  public DepthFirstTraverseStep(
      List<SQLTraverseProjectionItem> projections,
      SQLWhereClause whileClause,
      SQLInteger maxDepth,
      CommandContext ctx,
      boolean profilingEnabled) {
    super(projections, whileClause, maxDepth, ctx, profilingEnabled);
  }

  @Override
  protected void fetchNextEntryPoints(
      ExecutionStream nextN, CommandContext ctx, List<Result> entryPoints,
      Set<RID> traversed) {
    // Doing max batch of 100 entry points for now
    while (nextN.hasNext(ctx) && entryPoints.size() < 100) {
      var item = toTraverseResult(ctx.getDatabase(), nextN.next(ctx));
      if (item == null) {
        continue;
      }
      ((ResultInternal) item).setMetadata("$depth", 0);

      List stack = new ArrayList();
      item.getIdentity().ifPresent(x -> stack.add(x));
      ((ResultInternal) item).setMetadata("$stack", stack);

      List<Identifiable> path = new ArrayList<>();
      if (item.getIdentity().isPresent()) {
        path.add(item.getIdentity().get());
      } else if (item.getProperty("@rid") != null) {
        path.add(item.getProperty("@rid"));
      }
      ((ResultInternal) item).setMetadata("$path", path);

      if (item.isEntity() && !traversed.contains(item.getEntity().get().getIdentity())) {
        tryAddEntryPointAtTheEnd(item, ctx, entryPoints, traversed);
        traversed.add(item.getEntity().get().getIdentity());
      } else if (item.getProperty("@rid") != null
          && item.getProperty("@rid") instanceof Identifiable) {
        tryAddEntryPointAtTheEnd(item, ctx, entryPoints, traversed);
        traversed.add(((Identifiable) item.getProperty("@rid")).getIdentity());
      }
    }
  }

  private Result toTraverseResult(DatabaseSessionInternal db, Result item) {
    TraverseResult res = null;
    if (item instanceof TraverseResult) {
      res = (TraverseResult) item;
    } else if (item.isEntity() && ((RecordId) item.getEntity().get().getIdentity()).isValid()) {
      res = new TraverseResult(db, item.getEntity().get());
      res.depth = 0;
    } else if (item.getPropertyNames().size() == 1) {
      var val = item.getProperty(item.getPropertyNames().iterator().next());
      if (val instanceof Identifiable) {
        res = new TraverseResult(db, (Identifiable) val);
        res.depth = 0;
        res.setMetadata("$depth", 0);
      }
    } else {
      res = new TraverseResult(db);
      for (var key : item.getPropertyNames()) {
        res.setProperty(key, convert(item.getProperty(key)));
      }
      for (var md : item.getMetadataKeys()) {
        res.setMetadata(md, item.getMetadata(md));
      }
    }

    return res;
  }

  public Object convert(Object value) {
    if (value instanceof RidBag) {
      List result = new ArrayList();
      ((RidBag) value).forEach(x -> result.add(x));
      return result;
    }
    return value;
  }

  @Override
  protected void fetchNextResults(
      CommandContext ctx, List<Result> results, List<Result> entryPoints,
      Set<RID> traversed) {
    if (!entryPoints.isEmpty()) {
      var item = (TraverseResult) entryPoints.remove(0);
      results.add(item);
      for (var proj : projections) {
        var nextStep = proj.execute(item, ctx);
        var depth = item.depth != null ? item.depth : (Integer) item.getMetadata("$depth");
        if (this.maxDepth == null || this.maxDepth.getValue().intValue() > depth) {
          addNextEntryPoints(
              nextStep,
              depth + 1,
              (List) item.getMetadata("$path"),
              (List) item.getMetadata("$stack"),
              ctx,
              entryPoints,
              traversed);
        }
      }
    }
  }

  private void addNextEntryPoints(
      Object nextStep,
      int depth,
      List<Identifiable> path,
      List<Identifiable> stack,
      CommandContext ctx,
      List<Result> entryPoints,
      Set<RID> traversed) {
    if (nextStep instanceof Identifiable) {
      addNextEntryPoint(
          ((Identifiable) nextStep), depth, path, stack, ctx, entryPoints, traversed);
    } else if (nextStep instanceof Iterable) {
      addNextEntryPoints(
          ((Iterable) nextStep).iterator(), depth, path, stack, ctx, entryPoints, traversed);
    } else if (nextStep instanceof Map) {
      addNextEntryPoints(
          ((Map) nextStep).values().iterator(), depth, path, stack, ctx, entryPoints, traversed);
    } else if (nextStep instanceof Result) {
      addNextEntryPoint(((Result) nextStep), depth, path, stack, ctx, entryPoints, traversed);
    }
  }

  private void addNextEntryPoints(
      Iterator nextStep,
      int depth,
      List<Identifiable> path,
      List<Identifiable> stack,
      CommandContext ctx,
      List<Result> entryPoints,
      Set<RID> traversed) {
    while (nextStep.hasNext()) {
      addNextEntryPoints(nextStep.next(), depth, path, stack, ctx, entryPoints, traversed);
    }
  }

  private void addNextEntryPoint(
      Identifiable nextStep,
      int depth,
      List<Identifiable> path,
      List<Identifiable> stack,
      CommandContext ctx,
      List<Result> entryPoints,
      Set<RID> traversed) {
    if (traversed.contains(nextStep.getIdentity())) {
      return;
    }
    var res = new TraverseResult(ctx.getDatabase(), nextStep);
    res.depth = depth;
    res.setMetadata("$depth", depth);

    List<Identifiable> newPath = new ArrayList<>(path);
    newPath.add(res.getIdentity().get());
    res.setMetadata("$path", newPath);

    List newStack = new ArrayList();
    newStack.add(res.getIdentity().get());
    newStack.addAll(stack);
    //    for (int i = 0; i < newPath.size(); i++) {
    //      newStack.offerLast(newPath.get(i));
    //    }
    res.setMetadata("$stack", newStack);

    tryAddEntryPoint(res, ctx, entryPoints, traversed);
  }

  private void addNextEntryPoint(
      Result nextStep,
      int depth,
      List<Identifiable> path,
      List<Identifiable> stack,
      CommandContext ctx,
      List<Result> entryPoints,
      Set<RID> traversed) {
    if (!nextStep.isEntity()) {
      return;
    }
    if (traversed.contains(nextStep.getEntity().get().getIdentity())) {
      return;
    }
    if (nextStep instanceof TraverseResult) {
      ((TraverseResult) nextStep).depth = depth;
      ((TraverseResult) nextStep).setMetadata("$depth", depth);
      List<Identifiable> newPath = new ArrayList<>();
      newPath.addAll(path);
      nextStep.getIdentity().ifPresent(x -> newPath.add(x.getIdentity()));
      ((TraverseResult) nextStep).setMetadata("$path", newPath);

      List reverseStack = new ArrayList();
      reverseStack.addAll(newPath);
      Collections.reverse(reverseStack);
      List newStack = new ArrayList();
      newStack.addAll(reverseStack);
      ((TraverseResult) nextStep).setMetadata("$stack", newStack);

      tryAddEntryPoint(nextStep, ctx, entryPoints, traversed);
    } else {
      var res = new TraverseResult(ctx.getDatabase(), nextStep.getEntity().get());
      res.depth = depth;
      res.setMetadata("$depth", depth);
      List<Identifiable> newPath = new ArrayList<>();
      newPath.addAll(path);
      nextStep.getIdentity().ifPresent(x -> newPath.add(x.getIdentity()));
      ((TraverseResult) nextStep).setMetadata("$path", newPath);

      List reverseStack = new ArrayList();
      reverseStack.addAll(newPath);
      Collections.reverse(reverseStack);
      List newStack = new ArrayList();
      newStack.addAll(reverseStack);
      ((TraverseResult) nextStep).setMetadata("$stack", newStack);

      tryAddEntryPoint(res, ctx, entryPoints, traversed);
    }
  }

  private void tryAddEntryPoint(
      Result res, CommandContext ctx, List<Result> entryPoints, Set<RID> traversed) {
    if (whileClause == null || whileClause.matchesFilters(res, ctx)) {
      entryPoints.add(0, res);
    }

    if (res.isEntity()) {
      traversed.add(res.getEntity().get().getIdentity());
    } else if (res.getProperty("@rid") != null
        && res.getProperty("@rid") instanceof Identifiable) {
      traversed.add(((Identifiable) res.getProperty("@rid")).getIdentity());
    }
  }

  private void tryAddEntryPointAtTheEnd(
      Result res, CommandContext ctx, List<Result> entryPoints, Set<RID> traversed) {
    if (whileClause == null || whileClause.matchesFilters(res, ctx)) {
      entryPoints.add(res);
    }

    if (res.isEntity()) {
      traversed.add(res.getEntity().get().getIdentity());
    } else if (res.getProperty("@rid") != null
        && res.getProperty("@rid") instanceof Identifiable) {
      traversed.add(((Identifiable) res.getProperty("@rid")).getIdentity());
    }
  }

  @Override
  public String prettyPrint(int depth, int indent) {
    var spaces = ExecutionStepInternal.getIndent(depth, indent);
    var result = new StringBuilder();
    result.append(spaces);
    result.append("+ DEPTH-FIRST TRAVERSE \n");
    result.append(spaces);
    result.append("  " + projections.toString());
    if (whileClause != null) {
      result.append("\n");
      result.append(spaces);
      result.append("WHILE " + whileClause);
    }
    return result.toString();
  }
}
