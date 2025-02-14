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

package com.jetbrains.youtrack.db.internal.core.storage.index.hashindex.local.v3;

import com.jetbrains.youtrack.db.internal.common.serialization.types.IntegerSerializer;
import com.jetbrains.youtrack.db.api.config.GlobalConfiguration;
import com.jetbrains.youtrack.db.internal.core.storage.cache.CacheEntry;

/**
 * @since 5/14/14
 */
public final class DirectoryFirstPage extends DirectoryPage {

  private static final int TREE_SIZE_OFFSET = NEXT_FREE_POSITION;
  private static final int TOMBSTONE_OFFSET = TREE_SIZE_OFFSET + IntegerSerializer.INT_SIZE;

  private static final int ITEMS_OFFSET = TOMBSTONE_OFFSET + IntegerSerializer.INT_SIZE;

  public static final int NODES_PER_PAGE =
      (GlobalConfiguration.DISK_CACHE_PAGE_SIZE.getValueAsInteger() * 1024 - ITEMS_OFFSET)
          / HashTableDirectory.BINARY_LEVEL_SIZE;

  DirectoryFirstPage(CacheEntry cacheEntry, CacheEntry entry) {
    super(cacheEntry, entry);
  }

  public void setTreeSize(int treeSize) {
    setIntValue(TREE_SIZE_OFFSET, treeSize);
  }

  public int getTreeSize() {
    return getIntValue(TREE_SIZE_OFFSET);
  }

  void setTombstone(int tombstone) {
    setIntValue(TOMBSTONE_OFFSET, tombstone);
  }

  int getTombstone() {
    return getIntValue(TOMBSTONE_OFFSET);
  }

  @Override
  protected int getItemsOffset() {
    return ITEMS_OFFSET;
  }
}
