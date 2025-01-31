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
package com.jetbrains.youtrack.db.internal.core.cache;

import com.jetbrains.youtrack.db.api.record.RID;
import com.jetbrains.youtrack.db.internal.common.profiler.AbstractProfiler.ProfilerHookValue;
import com.jetbrains.youtrack.db.internal.common.profiler.Profiler.METRIC_TYPE;
import com.jetbrains.youtrack.db.internal.core.YouTrackDBEnginesManager;
import java.util.HashSet;
import java.util.Set;

/**
 * Cache of documents. Delegates real work on storing to {@link RecordCache} implementation passed
 * at creation time leaving only DB specific functionality
 */
public abstract class AbstractRecordCache {

  protected RecordCache underlying;
  protected String profilerPrefix = "noname";
  protected String profilerMetadataPrefix = "noname";
  protected int excludedCluster = -1;

  /**
   * Create cache backed by given implementation
   *
   * @param impl actual implementation of cache
   */
  public AbstractRecordCache(final RecordCache impl) {
    underlying = impl;
  }

  /**
   * Tell whether cache is enabled
   *
   * @return {@code true} if cache enabled at call time, otherwise - {@code false}
   */
  public boolean isEnabled() {
    return underlying.isEnabled();
  }

  /**
   * Switch cache state between enabled and disabled
   *
   * @param enable pass {@code true} to enable, otherwise - {@code false}
   */
  public void setEnable(final boolean enable) {
    if (enable) {
      underlying.enable();
    } else {
      underlying.disable();
    }
  }

  /**
   * Remove all records belonging to specified cluster
   *
   * @param cid identifier of cluster
   */
  public void freeCluster(final int cid) {
    final Set<RID> toRemove = new HashSet<RID>(underlying.size() / 2);

    final Set<RID> keys = new HashSet<RID>(underlying.keys());
    for (final var id : keys) {
      if (id.getClusterId() == cid) {
        toRemove.add(id);
      }
    }

    for (final var ridToRemove : toRemove) {
      underlying.remove(ridToRemove);
    }
  }

  /**
   * Remove record entry
   *
   * @param rid unique record identifier
   */
  public void deleteRecord(final RID rid) {
    underlying.remove(rid);
  }

  /**
   * Clear the entire cache by removing all the entries
   */
  public void clear() {
    underlying.clear();
  }

  /**
   * Transfer all records contained in cache to unload state.
   */
  public void unloadRecords() {
    underlying.unloadRecords();
  }

  public void unloadNotModifiedRecords() {
    underlying.unloadNotModifiedRecords();
  }

  /**
   * Total number of cached entries
   *
   * @return non-negative integer
   */
  public int getSize() {
    return underlying.size();
  }

  /**
   * All operations running at cache initialization stage
   */
  public void startup() {
    underlying.startup();

    YouTrackDBEnginesManager.instance()
        .getProfiler()
        .registerHookValue(
            profilerPrefix + "current",
            "Number of entries in cache",
            METRIC_TYPE.SIZE,
            new ProfilerHookValue() {
              public Object getValue() {
                return getSize();
              }
            },
            profilerMetadataPrefix + "current");
  }

  /**
   * All operations running at cache destruction stage
   */
  public void shutdown() {
    underlying.shutdown();

    if (YouTrackDBEnginesManager.instance().getProfiler() != null) {
      YouTrackDBEnginesManager.instance().getProfiler()
          .unregisterHookValue(profilerPrefix + "enabled");
      YouTrackDBEnginesManager.instance().getProfiler()
          .unregisterHookValue(profilerPrefix + "current");
      YouTrackDBEnginesManager.instance().getProfiler().unregisterHookValue(profilerPrefix + "max");
    }
  }
}
