/*
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.youtrack.db.internal.core.sharding.auto;

import com.jetbrains.youtrack.db.api.exception.BaseException;
import com.jetbrains.youtrack.db.api.exception.ConfigurationException;
import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseRecordThreadLocal;
import com.jetbrains.youtrack.db.internal.core.exception.InvalidIndexEngineIdException;
import com.jetbrains.youtrack.db.internal.core.index.Index;
import com.jetbrains.youtrack.db.internal.core.index.IndexInternal;
import com.jetbrains.youtrack.db.internal.core.index.engine.IndexEngine;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.ClusterSelectionStrategy;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.storage.Storage;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.AbstractPaginatedStorage;
import java.util.List;

/**
 * Returns the cluster selecting through the hash function.
 *
 * @since 3.0
 */
public class AutoShardingClusterSelectionStrategy implements ClusterSelectionStrategy {

  public static final String NAME = "auto-sharding";
  private final Index index;
  private final IndexEngine indexEngine;
  private final List<String> indexedFields;
  private final int[] clusters;

  public AutoShardingClusterSelectionStrategy(final SchemaClass clazz,
      final Index autoShardingIndex) {
    index = autoShardingIndex;
    if (index == null) {
      throw new ConfigurationException(
          "Cannot use auto-sharding cluster strategy because class '"
              + clazz
              + "' has no auto-sharding index defined");
    }

    indexedFields = index.getDefinition().getFields();
    if (indexedFields.size() != 1) {
      throw new ConfigurationException(
          "Cannot use auto-sharding cluster strategy because class '"
              + clazz
              + "' has an auto-sharding index defined with multiple fields");
    }

    final Storage stg = DatabaseRecordThreadLocal.instance().get().getStorage();
    if (!(stg instanceof AbstractPaginatedStorage)) {
      throw new ConfigurationException(
          "Cannot use auto-sharding cluster strategy because storage is not embedded");
    }

    try {
      indexEngine =
          (IndexEngine)
              ((AbstractPaginatedStorage) stg)
                  .getIndexEngine(((IndexInternal) index).getIndexId());
    } catch (InvalidIndexEngineIdException e) {
      throw BaseException.wrapException(
          new ConfigurationException(
              "Cannot use auto-sharding cluster strategy because the underlying index has not"
                  + " found"),
          e);
    }

    if (indexEngine == null) {
      throw new ConfigurationException(
          "Cannot use auto-sharding cluster strategy because the underlying index has not found");
    }

    clusters = clazz.getClusterIds();
  }

  public int getCluster(final SchemaClass iClass, int[] clusters, final EntityImpl entity) {
    // Ignore the subselection.
    return getCluster(iClass, entity);
  }

  public int getCluster(final SchemaClass clazz, final EntityImpl entity) {
    final Object fieldValue = entity.field(indexedFields.get(0));

    return clusters[
        ((AutoShardingIndexEngine) indexEngine)
            .getStrategy()
            .getPartitionsId(fieldValue, clusters.length)];
  }

  @Override
  public String getName() {
    return NAME;
  }
}
