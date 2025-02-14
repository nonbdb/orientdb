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
package com.jetbrains.youtrack.db.internal.core.metadata.schema.clusterselection;

import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseRecordThreadLocal;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.ClusterSelectionStrategy;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;

/**
 * Returns the cluster selecting the most empty between all configured clusters.
 */
public class BalancedClusterSelectionStrategy implements ClusterSelectionStrategy {

  public static final String NAME = "balanced";
  protected static final long REFRESH_TIMEOUT = 5000;
  protected long lastCount = -1;
  protected int smallerClusterId = -1;

  public int getCluster(final SchemaClass iClass, final EntityImpl entity) {
    return getCluster(iClass, iClass.getClusterIds(), entity);
  }

  public int getCluster(final SchemaClass iClass, final int[] clusters, final EntityImpl entity) {

    if (clusters.length == 1)
    // ONLY ONE: RETURN THE FIRST ONE
    {
      return clusters[0];
    }

    final var db = DatabaseRecordThreadLocal.instance().getIfDefined();
    if (db == null) {
      return clusters[0];
    }

    if (lastCount < 0 || System.currentTimeMillis() - lastCount > REFRESH_TIMEOUT) {
      // REFRESH COUNTERS
      long min = Long.MAX_VALUE;

      for (int cluster : clusters) {
        final long count = db.countClusterElements(cluster);
        if (count < min) {
          min = count;
          smallerClusterId = cluster;
        }
      }
      lastCount = System.currentTimeMillis();
    }

    return smallerClusterId;
  }

  @Override
  public String getName() {
    return NAME;
  }
}
