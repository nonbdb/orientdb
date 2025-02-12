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

import com.jetbrains.youtrack.db.internal.core.YouTrackDBEnginesManager;
import com.jetbrains.youtrack.db.api.config.GlobalConfiguration;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.api.exception.DatabaseException;
import com.jetbrains.youtrack.db.api.record.RID;
import com.jetbrains.youtrack.db.internal.core.record.RecordVersionHelper;
import com.jetbrains.youtrack.db.internal.core.record.RecordAbstract;
import javax.annotation.Nonnull;

/**
 * Local cache. it's one to one with record database instances. It is needed to avoid cases when
 * several instances of the same record will be loaded by user from the same database.
 */
public class LocalRecordCache extends AbstractRecordCache {

  private String cacheHit;
  private String cacheMiss;
  private DatabaseSessionInternal db;

  public LocalRecordCache() {
    super(
        YouTrackDBEnginesManager.instance()
            .getLocalRecordCache()
            .newInstance(GlobalConfiguration.CACHE_LOCAL_IMPL.getValueAsString()));
  }

  @Override
  public void startup(@Nonnull DatabaseSessionInternal db) {
    profilerPrefix = "db." + db.getDatabaseName() + ".cache.level1.";
    profilerMetadataPrefix = "db.*.cache.level1.";

    cacheHit = profilerPrefix + "cache.found";
    cacheMiss = profilerPrefix + "cache.notFound";

    super.startup(db);
    this.db = db;
  }

  /**
   * Pushes record to cache. Identifier of record used as access key
   *
   * @param record record that should be cached
   */
  public void updateRecord(final RecordAbstract record) {
    assert !record.isUnloaded();
    var rid = record.getIdentity();
    if (rid.getClusterId() != excludedCluster
        && !rid.isTemporary()
        && rid.isValid()
        && !record.isDirty()
        && !RecordVersionHelper.isTombstone(record.getVersion())) {
      var loadedRecord = underlying.get(rid);
      if (loadedRecord == null) {
        underlying.put(record);
      } else if (loadedRecord != record) {
        throw new DatabaseException(db.getDatabaseName(),
            "Record with id "
                + record.getIdentity()
                + " already registered in current session, please load "
                + "record again using 'record = db.load(rid)' method or use another session.");
      }
    }
  }

  /**
   * Looks up for record in cache by it's identifier. Optionally look up in secondary cache and
   * update primary with found record
   *
   * @param rid unique identifier of record
   * @return record stored in cache if any, otherwise - {@code null}
   */
  public RecordAbstract findRecord(final RID rid) {
    RecordAbstract record;
    record = underlying.get(rid);

    if (record != null) {
      YouTrackDBEnginesManager.instance()
          .getProfiler()
          .updateCounter(
              cacheHit, "Record found in Level1 Cache", 1L, "db.*.cache.level1.cache.found");
    } else {
      YouTrackDBEnginesManager.instance()
          .getProfiler()
          .updateCounter(
              cacheMiss,
              "Record not found in Level1 Cache",
              1L,
              "db.*.cache.level1.cache.notFound");
    }

    return record;
  }

  /**
   * Removes record with specified identifier from both primary and secondary caches
   *
   * @param rid unique identifier of record
   */
  public void deleteRecord(final RID rid) {
    super.deleteRecord(rid);
  }

  public void shutdown() {
    super.shutdown();
  }

  @Override
  public void clear() {
    super.clear();
  }

  /**
   * Invalidates the cache emptying all the records.
   */
  public void invalidate() {
    underlying.clear();
  }

  @Override
  public String toString() {
    return "DB level cache records = " + getSize();
  }
}
