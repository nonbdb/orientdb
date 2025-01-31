/*
 *
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 * <p>
 * *
 */
package com.jetbrains.youtrack.db.internal.spatial.index;

import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.internal.core.exception.InvalidIndexEngineIdException;
import com.jetbrains.youtrack.db.internal.core.index.IndexMetadata;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.storage.Storage;
import com.jetbrains.youtrack.db.internal.core.tx.FrontendTransactionIndexChanges;
import com.jetbrains.youtrack.db.internal.core.tx.FrontendTransactionIndexChangesPerKey;
import com.jetbrains.youtrack.db.internal.core.tx.FrontendTransactionIndexChangesPerKey.TransactionIndexEntry;
import com.jetbrains.youtrack.db.internal.lucene.index.LuceneIndexNotUnique;
import com.jetbrains.youtrack.db.internal.spatial.engine.LuceneSpatialIndexContainer;
import com.jetbrains.youtrack.db.internal.spatial.shape.ShapeFactory;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.spatial4j.shape.Shape;

public class LuceneSpatialIndex extends LuceneIndexNotUnique {

  private final ShapeFactory shapeFactory = ShapeFactory.INSTANCE;

  public LuceneSpatialIndex(IndexMetadata im, final Storage storage) {
    super(im, storage);
  }

  @Override
  public LuceneIndexNotUnique put(DatabaseSessionInternal db, Object key,
      Identifiable value) {
    if (key == null) {
      return this;
    }
    return super.put(db, key, value);
  }

  @Override
  public Iterable<TransactionIndexEntry> interpretTxKeyChanges(
      final FrontendTransactionIndexChangesPerKey changes) {

    try {
      return storage.callIndexEngine(
          false,
          indexId,
          engine -> {
            if (((LuceneSpatialIndexContainer) engine).isLegacy()) {
              return LuceneSpatialIndex.super.interpretTxKeyChanges(changes);
            } else {
              return interpretAsSpatial(changes);
            }
          });
    } catch (InvalidIndexEngineIdException e) {
      e.printStackTrace();
    }

    return super.interpretTxKeyChanges(changes);
  }

  @Override
  protected Object encodeKey(Object key) {

    if (key instanceof EntityImpl) {
      var shape = shapeFactory.fromDoc((EntityImpl) key);
      return shapeFactory.toGeometry(shape);
    }
    return key;
  }

  @Override
  protected Object decodeKey(Object key) {

    if (key instanceof Geometry geom) {
      return shapeFactory.toEntitty(geom);
    }
    return key;
  }

  private static Iterable<TransactionIndexEntry> interpretAsSpatial(
      FrontendTransactionIndexChangesPerKey item) {
    // 1. Handle common fast paths.

    var entries = item.getEntriesAsList();
    Map<Identifiable, Integer> counters = new LinkedHashMap<>();

    for (var entry : entries) {

      var counter = counters.get(entry.getValue());
      if (counter == null) {
        counter = 0;
      }
      switch (entry.getOperation()) {
        case PUT:
          counter++;
          break;
        case REMOVE:
          counter--;
          break;
        case CLEAR:
          break;
      }
      counters.put(entry.getValue(), counter);
    }

    var changes = new FrontendTransactionIndexChangesPerKey(
        item.key);

    for (var entry : counters.entrySet()) {
      var oIdentifiable = entry.getKey();
      switch (entry.getValue()) {
        case 1:
          changes.add(oIdentifiable, FrontendTransactionIndexChanges.OPERATION.PUT);
          break;
        case -1:
          changes.add(oIdentifiable, FrontendTransactionIndexChanges.OPERATION.REMOVE);
          break;
      }
    }
    return changes.getEntriesAsList();
  }
}
