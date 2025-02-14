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

package com.jetbrains.youtrack.db.internal.core.storage.index.versionmap;

import com.jetbrains.youtrack.db.api.exception.BaseException;
import com.jetbrains.youtrack.db.internal.common.log.LogManager;
import com.jetbrains.youtrack.db.api.config.GlobalConfiguration;
import com.jetbrains.youtrack.db.internal.core.exception.StorageException;
import com.jetbrains.youtrack.db.internal.core.storage.cache.CacheEntry;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.AbstractPaginatedStorage;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.atomicoperations.AtomicOperation;
import java.io.IOException;

/**
 * The version position map in version 0 stores a version of type int for all change operations on
 * the `AbstractPaginatedStorage` storage. It creates one file with extension `vpm` (i.e. w/o meta
 * data) and expected number of elements BaseIndexEngine.DEFAULT_VERSION_ARRAY_SIZE.
 */
public final class VersionPositionMapV0 extends VersionPositionMap {

  private long fileId;
  private int numberOfPages;
  public static final int PAGE_SIZE =
      GlobalConfiguration.DISK_CACHE_PAGE_SIZE.getValueAsInteger() * 1024;

  public VersionPositionMapV0(
      final AbstractPaginatedStorage storage,
      final String name,
      final String lockName,
      final String extension) {
    super(storage, name, extension, lockName);
  }

  @Override
  public void create(final AtomicOperation atomicOperation) {
    executeInsideComponentOperation(
        atomicOperation,
        operation -> {
          acquireExclusiveLock();
          try {
            this.createVPM(atomicOperation);
          } finally {
            releaseExclusiveLock();
          }
        });
  }

  @Override
  public void delete(final AtomicOperation atomicOperation) {
    executeInsideComponentOperation(
        atomicOperation,
        operation -> {
          acquireExclusiveLock();
          try {
            this.deleteVPM(atomicOperation);
          } finally {
            releaseExclusiveLock();
          }
        });
  }

  @Override
  public void open() throws IOException {
    acquireExclusiveLock();
    try {
      final AtomicOperation atomicOperation = atomicOperationsManager.getCurrentOperation();
      this.openVPM(atomicOperation);
    } finally {
      releaseExclusiveLock();
    }
  }

  @Override
  public void updateVersion(final int hash) {
    final AtomicOperation atomicOperation = atomicOperationsManager.getCurrentOperation();
    executeInsideComponentOperation(
        atomicOperation,
        operation -> {
          acquireExclusiveLock();
          try {
            final int startPositionWithOffset = VersionPositionMapBucket.entryPosition(hash);
            final int pageIndex = calculatePageIndex(startPositionWithOffset);
            try (final CacheEntry cacheEntry =
                loadPageForWrite(atomicOperation, fileId, pageIndex, true)) {
              final VersionPositionMapBucket bucket = new VersionPositionMapBucket(cacheEntry);
              bucket.incrementVersion(hash);
            }
          } finally {
            releaseExclusiveLock();
          }
        });
  }

  @Override
  public int getVersion(final int hash) {
    final int startPositionWithOffset = VersionPositionMapBucket.entryPosition(hash);
    final int pageIndex = calculatePageIndex(startPositionWithOffset);
    acquireSharedLock();
    try {
      final AtomicOperation atomicOperation = atomicOperationsManager.getCurrentOperation();

      try (final CacheEntry cacheEntry = loadPageForRead(atomicOperation, fileId, pageIndex)) {
        final VersionPositionMapBucket bucket = new VersionPositionMapBucket(cacheEntry);
        return bucket.getVersion(hash);
      }
    } catch (final IOException e) {
      throw BaseException.wrapException(
          new StorageException("Error during reading the size of rid bag"), e);
    } finally {
      releaseSharedLock();
    }
  }

  @Override
  public int getKeyHash(final Object key) {
    int keyHash = 0; // as for null values in hash map
    if (key != null) {
      keyHash = Math.abs(key.hashCode()) % DEFAULT_VERSION_ARRAY_SIZE;
    }
    return keyHash;
  }

  private void openVPM(final AtomicOperation atomicOperation) throws IOException {
    // In case an old storage does not have a VPM yet, it will be created.
    // If the creation of a VPM is interrupted due to any error / exception, the file is either
    // created corrupt, and thus subsequent access will (not hiding the issue), or the file will not
    // be created properly and a new one will be created during the next access on openVPM.
    if (!isFileExists(atomicOperation, getFullName())) {
      LogManager.instance()
          .debug(
              this,
              "VPM missing with fileId:%s: fileName = %s. A new VPM will be created.",
              fileId,
              getFullName());
      if (atomicOperation != null) {
        createVPM(atomicOperation);
      } else {
        atomicOperationsManager.executeInsideAtomicOperation(null, this::createVPM);
      }
    }
    fileId = openFile(atomicOperation, getFullName());
    LogManager.instance().debug(this, "VPM open fileId:%s: fileName = %s", fileId, getFullName());
  }

  private void createVPM(final AtomicOperation atomicOperation) throws IOException {
    fileId = addFile(atomicOperation, getFullName());
    final int sizeOfIntInBytes = Integer.SIZE / 8;
    numberOfPages =
        (int)
            Math.ceil(
                (DEFAULT_VERSION_ARRAY_SIZE * sizeOfIntInBytes * 1.0)
                    / VersionPositionMapV0.PAGE_SIZE);
    final long foundNumberOfPages = getFilledUpTo(atomicOperation, fileId);
    LogManager.instance()
        .debug(
            this,
            "VPM created with fileId:%s: fileName = %s, expected #pages = %d, actual #pages = %d",
            fileId,
            getFullName(),
            numberOfPages,
            foundNumberOfPages);
    if (foundNumberOfPages != numberOfPages) {
      for (int i = 0; i < numberOfPages; i++) {
        addInitializedPage(atomicOperation);
      }
    } else {
      try (final CacheEntry cacheEntry = loadPageForWrite(atomicOperation, fileId, 0, false)) {
        final MapEntryPoint mapEntryPoint = new MapEntryPoint(cacheEntry);
        mapEntryPoint.setFileSize(0);
      }
    }
  }

  private void addInitializedPage(final AtomicOperation atomicOperation) throws IOException {
    try (final CacheEntry cacheEntry = addPage(atomicOperation, fileId)) {
      final MapEntryPoint mapEntryPoint = new MapEntryPoint(cacheEntry);
      mapEntryPoint.setFileSize(0);
    }
  }

  private void deleteVPM(final AtomicOperation atomicOperation) throws IOException {
    deleteFile(atomicOperation, fileId);
  }

  private int calculatePageIndex(final int startPositionWithOffset) {
    return (int) Math.ceil(startPositionWithOffset / VersionPositionMapV0.PAGE_SIZE);
  }

  int getNumberOfPages() {
    return numberOfPages;
  }
}
