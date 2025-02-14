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

package com.jetbrains.youtrack.db.internal.core.storage.memory;

import com.jetbrains.youtrack.db.api.config.ContextConfiguration;
import com.jetbrains.youtrack.db.api.config.GlobalConfiguration;
import com.jetbrains.youtrack.db.internal.core.command.CommandOutputListener;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.YouTrackDBInternal;
import com.jetbrains.youtrack.db.internal.core.engine.memory.EngineMemory;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.AbstractPaginatedStorage;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.wal.LogSequenceNumber;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.wal.MemoryWriteAheadLog;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.wal.WriteAheadLog;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.zip.ZipOutputStream;

/**
 * @since 7/9/14
 */
public class DirectMemoryStorage extends AbstractPaginatedStorage {

  private static final int ONE_KB = 1024;

  public DirectMemoryStorage(
      final String name, final String filePath, final int id, YouTrackDBInternal context) {
    super(name, filePath, id, context);
  }

  @Override
  protected void initWalAndDiskCache(final ContextConfiguration contextConfiguration) {
    if (writeAheadLog == null) {
      writeAheadLog = new MemoryWriteAheadLog();
    }

    final var diskCache =
        new DirectMemoryOnlyDiskCache(
            contextConfiguration.getValueAsInteger(GlobalConfiguration.DISK_CACHE_PAGE_SIZE)
                * ONE_KB,
            1, getName());

    if (readCache == null) {
      readCache = diskCache;
    }

    if (writeCache == null) {
      writeCache = diskCache;
    }
  }

  @Override
  protected void postCloseSteps(
      final boolean onDelete, final boolean internalError, final long lastTxId) {
  }

  @Override
  public boolean exists() {
    try {
      return readCache != null && !writeCache.files().isEmpty();
    } catch (final RuntimeException e) {
      throw logAndPrepareForRethrow(e);
    } catch (final Error e) {
      throw logAndPrepareForRethrow(e, false);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t, false);
    }
  }

  @Override
  public String getType() {
    return EngineMemory.NAME;
  }

  @Override
  public String getURL() {
    return EngineMemory.NAME + ":" + url;
  }

  @Override
  public void flushAllData() {
  }

  @Override
  protected void readIv() {
  }

  @Override
  protected byte[] getIv() {
    return new byte[0];
  }

  @Override
  protected void initIv() {
  }

  @Override
  public List<String> backup(
      DatabaseSessionInternal db, final OutputStream out,
      final Map<String, Object> options,
      final Callable<Object> callable,
      final CommandOutputListener iListener,
      final int compressionLevel,
      final int bufferSize) {
    try {
      throw new UnsupportedOperationException();
    } catch (final RuntimeException e) {
      throw logAndPrepareForRethrow(e);
    } catch (final Error e) {
      throw logAndPrepareForRethrow(e);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t);
    }
  }

  @Override
  public void restore(
      final InputStream in,
      final Map<String, Object> options,
      final Callable<Object> callable,
      final CommandOutputListener iListener) {
    try {
      throw new UnsupportedOperationException();
    } catch (final RuntimeException e) {
      throw logAndPrepareForRethrow(e);
    } catch (final Error e) {
      throw logAndPrepareForRethrow(e);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t);
    }
  }

  @Override
  protected LogSequenceNumber copyWALToIncrementalBackup(
      final ZipOutputStream zipOutputStream, final long startSegment) {
    return null;
  }

  @Override
  protected boolean isWriteAllowedDuringIncrementalBackup() {
    return false;
  }

  @Override
  protected File createWalTempDirectory() {
    return null;
  }

  @Override
  protected WriteAheadLog createWalFromIBUFiles(
      final File directory,
      final ContextConfiguration contextConfiguration,
      final Locale locale,
      byte[] iv) {
    return null;
  }

  @Override
  public void shutdown() {
    try {
      delete();
    } catch (final RuntimeException e) {
      throw logAndPrepareForRethrow(e);
    } catch (final Error e) {
      throw logAndPrepareForRethrow(e);
    } catch (final Throwable t) {
      throw logAndPrepareForRethrow(t);
    }
  }

  @Override
  protected void checkBackupRunning() {
  }

  @Override
  public boolean isMemory() {
    return true;
  }
}
