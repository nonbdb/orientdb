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

package com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.base;

import com.jetbrains.youtrack.db.internal.common.concur.resource.SharedResourceAbstract;
import com.jetbrains.youtrack.db.internal.common.function.TxConsumer;
import com.jetbrains.youtrack.db.internal.common.function.TxFunction;
import com.jetbrains.youtrack.db.internal.core.storage.cache.CacheEntry;
import com.jetbrains.youtrack.db.internal.core.storage.cache.ReadCache;
import com.jetbrains.youtrack.db.internal.core.storage.cache.WriteCache;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.AbstractPaginatedStorage;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.atomicoperations.AtomicOperation;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.atomicoperations.AtomicOperationsManager;
import java.io.IOException;
import javax.annotation.Nonnull;

/**
 * Base class for all durable data structures, that is data structures state of which can be
 * consistently restored after system crash but results of last operations in small interval before
 * crash may be lost.
 *
 * @since 8/27/13
 */
public abstract class DurableComponent extends SharedResourceAbstract {
  protected final AtomicOperationsManager atomicOperationsManager;
  protected final AbstractPaginatedStorage storage;
  protected final ReadCache readCache;
  protected final WriteCache writeCache;

  private volatile String name;
  private volatile String fullName;

  private final String extension;

  private final String lockName;

  public DurableComponent(
      @Nonnull final AbstractPaginatedStorage storage,
      @Nonnull final String name,
      final String extension,
      final String lockName) {
    super();

    this.extension = extension;
    this.storage = storage;
    this.fullName = name + extension;
    this.name = name;
    this.atomicOperationsManager = storage.getAtomicOperationsManager();
    this.readCache = storage.getReadCache();
    this.writeCache = storage.getWriteCache();
    this.lockName = lockName;
  }

  public String getLockName() {
    return lockName;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
    this.fullName = name + extension;
  }

  public String getFullName() {
    return fullName;
  }

  public String getExtension() {
    return extension;
  }

  protected <T> T calculateInsideComponentOperation(
      final AtomicOperation atomicOperation, final TxFunction<T> function) {
    return atomicOperationsManager.calculateInsideComponentOperation(
        atomicOperation, this, function);
  }

  protected void executeInsideComponentOperation(
      final AtomicOperation operation, final TxConsumer consumer) {
    atomicOperationsManager.executeInsideComponentOperation(operation, this, consumer);
  }

  protected long getFilledUpTo(final AtomicOperation atomicOperation, final long fileId) {
    if (atomicOperation == null) {
      return writeCache.getFilledUpTo(fileId);
    }
    return atomicOperation.filledUpTo(fileId);
  }

  protected static CacheEntry loadPageForWrite(
      final AtomicOperation atomicOperation,
      final long fileId,
      final long pageIndex,
      final boolean verifyCheckSum)
      throws IOException {
    return atomicOperation.loadPageForWrite(fileId, pageIndex, 1, verifyCheckSum);
  }

  protected CacheEntry loadOrAddPageForWrite(
      final AtomicOperation atomicOperation, final long fileId, final long pageIndex)
      throws IOException {
    var entry = atomicOperation.loadPageForWrite(fileId, pageIndex, 1, true);
    if (entry == null) {
      entry = addPage(atomicOperation, fileId);
    }
    return entry;
  }

  protected CacheEntry loadPageForRead(
      final AtomicOperation atomicOperation, final long fileId, final long pageIndex)
      throws IOException {
    if (atomicOperation == null) {
      return readCache.loadForRead(fileId, pageIndex, writeCache, true);
    }
    return atomicOperation.loadPageForRead(fileId, pageIndex);
  }

  protected CacheEntry addPage(final AtomicOperation atomicOperation, final long fileId)
      throws IOException {
    assert atomicOperation != null;
    return atomicOperation.addPage(fileId);
  }

  protected void releasePageFromWrite(
      final AtomicOperation atomicOperation, final CacheEntry cacheEntry) throws IOException {
    assert atomicOperation != null;
    atomicOperation.releasePageFromWrite(cacheEntry);
  }

  protected void releasePageFromRead(
      final AtomicOperation atomicOperation, final CacheEntry cacheEntry) {
    if (atomicOperation == null) {
      readCache.releaseFromRead(cacheEntry);
    } else {
      atomicOperation.releasePageFromRead(cacheEntry);
    }
  }

  protected long addFile(final AtomicOperation atomicOperation, final String fileName)
      throws IOException {
    assert atomicOperation != null;
    return atomicOperation.addFile(fileName);
  }

  protected long openFile(final AtomicOperation atomicOperation, final String fileName)
      throws IOException {
    if (atomicOperation == null) {
      return writeCache.loadFile(fileName);
    }
    return atomicOperation.loadFile(fileName);
  }

  protected void deleteFile(final AtomicOperation atomicOperation, final long fileId)
      throws IOException {
    assert atomicOperation != null;
    atomicOperation.deleteFile(fileId);
  }

  protected boolean isFileExists(final AtomicOperation atomicOperation, final String fileName) {
    if (atomicOperation == null) {
      return writeCache.exists(fileName);
    }
    return atomicOperation.isFileExists(fileName);
  }

  protected void truncateFile(final AtomicOperation atomicOperation, final long filedId)
      throws IOException {
    assert atomicOperation != null;
    atomicOperation.truncateFile(filedId);
  }
}
