package com.jetbrains.youtrack.db.internal.core.sql.functions.graph;

import com.jetbrains.youtrack.db.api.DatabaseSession;
import com.jetbrains.youtrack.db.api.record.Direction;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.api.record.RID;
import com.jetbrains.youtrack.db.api.record.Vertex;
import com.jetbrains.youtrack.db.internal.common.collection.MultiCollectionIterator;
import com.jetbrains.youtrack.db.internal.common.util.Sizeable;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.index.CompositeKey;
import com.jetbrains.youtrack.db.internal.core.index.Index;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 */
public class SQLFunctionIn extends SQLFunctionMoveFiltered {

  public static final String NAME = "in";

  public SQLFunctionIn() {
    super(NAME, 0, -1);
  }

  @Override
  protected Object move(
      final DatabaseSession graph, final Identifiable iRecord, final String[] iLabels) {
    return v2v(graph, iRecord, Direction.IN, iLabels);
  }

  protected Object move(
      final DatabaseSession graph,
      final Identifiable iRecord,
      final String[] iLabels,
      Iterable<Identifiable> iPossibleResults) {
    if (iPossibleResults == null) {
      return v2v(graph, iRecord, Direction.IN, iLabels);
    }

    if (!iPossibleResults.iterator().hasNext()) {
      return Collections.emptyList();
    }

    Object edges = v2e(graph, iRecord, Direction.IN, iLabels);
    if (edges instanceof Sizeable) {
      int size = ((Sizeable) edges).size();
      if (size > supernodeThreshold) {
        Object result = fetchFromIndex(graph, iRecord, iPossibleResults, iLabels);
        if (result != null) {
          return result;
        }
      }
    }

    return v2v(graph, iRecord, Direction.IN, iLabels);
  }

  private static Object fetchFromIndex(
      DatabaseSession graph,
      Identifiable iFrom,
      Iterable<Identifiable> to,
      String[] iEdgeTypes) {
    String edgeClassName = null;
    if (iEdgeTypes == null) {
      edgeClassName = "E";
    } else if (iEdgeTypes.length == 1) {
      edgeClassName = iEdgeTypes[0];
    } else {
      return null;
    }
    var edgeClass =
        ((DatabaseSessionInternal) graph)
            .getMetadata()
            .getImmutableSchemaSnapshot()
            .getClassInternal(edgeClassName);
    if (edgeClass == null) {
      return null;
    }
    Set<Index> indexes = edgeClass.getInvolvedIndexesInternal(graph, "in", "out");
    if (indexes == null || indexes.isEmpty()) {
      return null;
    }
    Index index = indexes.iterator().next();

    MultiCollectionIterator<Vertex> result = new MultiCollectionIterator<Vertex>();
    for (Identifiable identifiable : to) {
      CompositeKey key = new CompositeKey(iFrom, identifiable);
      try (Stream<RID> stream = index.getInternal()
          .getRids((DatabaseSessionInternal) graph, key)) {
        result.add(
            stream
                .map((edge) -> ((EntityImpl) edge.getRecord()).rawField("out"))
                .collect(Collectors.toSet()));
      }
    }

    return result;
  }
}
