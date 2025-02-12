/*
 *
 *
 *  *
 *  *  Licensed under the Apache License, Version 2.0 (the "License");
 *  *  you may not use this file except in compliance with the License.
 *  *  You may obtain a copy of the License at
 *  *
 *  *       http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *  Unless required by applicable law or agreed to in writing, software
 *  *  distributed under the License is distributed on an "AS IS" BASIS,
 *  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  See the License for the specific language governing permissions and
 *  *  limitations under the License.
 *  *
 *
 *
 */

package com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.wal;

import com.jetbrains.youtrack.db.internal.core.exception.StorageException;
import com.jetbrains.youtrack.db.internal.core.storage.cluster.PaginatedCluster;
import com.jetbrains.youtrack.db.internal.core.storage.cluster.v2.PaginatedClusterV2;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.AbstractPaginatedStorage;

/**
 * @since 10/8/13
 */
public final class PaginatedClusterFactory {

  public static PaginatedCluster createCluster(
      final String name,
      final int configurationVersion,
      final int binaryVersion,
      final AbstractPaginatedStorage storage) {
    if (configurationVersion >= 0 && configurationVersion < 6) {
      throw new StorageException(storage.getName(),
          "You use deprecated version of storage cluster, this version is not supported in current"
              + " implementation. Please do export/import or recreate database.");
    }

    return switch (binaryVersion) {
      case 0 -> throw new IllegalStateException(
          "Version 0 of cluster is not supported with given configuration");
      case 1 -> throw new IllegalStateException(
          "Version 1 of cluster is not supported with given configuration");
      case 2 -> new PaginatedClusterV2(name, storage);
      default ->
          throw new IllegalStateException("Invalid binary version of cluster " + binaryVersion);
    };
  }

  public static PaginatedCluster createCluster(
      final String name,
      final int binaryVersion,
      final AbstractPaginatedStorage storage,
      final String dataExtension,
      final String cpmExtension,
      final String fsmExtension) {
    return switch (binaryVersion) {
      case 0 -> throw new IllegalStateException(
          "Version 0 of cluster is not supported with given configuration");
      case 1 -> throw new IllegalStateException(
          "Version 1 of cluster is not supported with given configuration");
      case 2 -> new PaginatedClusterV2(name, dataExtension, cpmExtension, fsmExtension, storage);
      default ->
          throw new IllegalStateException("Invalid binary version of cluster " + binaryVersion);
    };
  }
}
