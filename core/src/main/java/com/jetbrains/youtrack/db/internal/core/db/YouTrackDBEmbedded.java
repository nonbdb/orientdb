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

package com.jetbrains.youtrack.db.internal.core.db;

import static com.jetbrains.youtrack.db.api.config.GlobalConfiguration.FILE_DELETE_DELAY;
import static com.jetbrains.youtrack.db.api.config.GlobalConfiguration.FILE_DELETE_RETRY;
import static com.jetbrains.youtrack.db.api.config.GlobalConfiguration.WARNING_DEFAULT_USERS;

import com.jetbrains.youtrack.db.api.DatabaseType;
import com.jetbrains.youtrack.db.api.config.ContextConfiguration;
import com.jetbrains.youtrack.db.api.config.GlobalConfiguration;
import com.jetbrains.youtrack.db.api.config.YouTrackDBConfig;
import com.jetbrains.youtrack.db.api.exception.BaseException;
import com.jetbrains.youtrack.db.api.exception.DatabaseException;
import com.jetbrains.youtrack.db.api.exception.ModificationOperationProhibitedException;
import com.jetbrains.youtrack.db.api.query.ResultSet;
import com.jetbrains.youtrack.db.internal.common.log.LogManager;
import com.jetbrains.youtrack.db.internal.common.thread.SourceTraceExecutorService;
import com.jetbrains.youtrack.db.internal.common.thread.ThreadPoolExecutors;
import com.jetbrains.youtrack.db.internal.core.YouTrackDBEnginesManager;
import com.jetbrains.youtrack.db.internal.core.command.CommandOutputListener;
import com.jetbrains.youtrack.db.internal.core.command.script.ScriptManager;
import com.jetbrains.youtrack.db.internal.core.engine.Engine;
import com.jetbrains.youtrack.db.internal.core.engine.MemoryAndLocalPaginatedEnginesInitializer;
import com.jetbrains.youtrack.db.internal.core.exception.StorageException;
import com.jetbrains.youtrack.db.internal.core.metadata.security.auth.AuthenticationInfo;
import com.jetbrains.youtrack.db.internal.core.security.DefaultSecuritySystem;
import com.jetbrains.youtrack.db.internal.core.sql.SQLEngine;
import com.jetbrains.youtrack.db.internal.core.sql.executor.InternalResultSet;
import com.jetbrains.youtrack.db.internal.core.sql.parser.LocalResultSetLifecycleDecorator;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLServerStatement;
import com.jetbrains.youtrack.db.internal.core.storage.Storage;
import com.jetbrains.youtrack.db.internal.core.storage.config.ClusterBasedStorageConfiguration;
import com.jetbrains.youtrack.db.internal.core.storage.disk.LocalPaginatedStorage;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.AbstractPaginatedStorage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.apache.commons.lang.NullArgumentException;

/**
 *
 */
public class YouTrackDBEmbedded implements YouTrackDBInternal {

  /**
   * Keeps track of next possible storage id.
   */
  private static final AtomicInteger nextStorageId = new AtomicInteger();

  /**
   * Storage IDs current assigned to the storage.
   */
  private static final Set<Integer> currentStorageIds =
      Collections.newSetFromMap(new ConcurrentHashMap<>());

  protected final Map<String, AbstractPaginatedStorage> storages = new ConcurrentHashMap<>();
  protected final Map<String, SharedContext> sharedContexts = new ConcurrentHashMap<>();
  protected final Set<DatabasePoolInternal> pools =
      Collections.newSetFromMap(new ConcurrentHashMap<>());
  protected final YouTrackDBConfigImpl configuration;
  protected final String basePath;
  protected final Engine memory;
  protected final Engine disk;
  protected final YouTrackDBEnginesManager youTrack;
  protected final CachedDatabasePoolFactory cachedPoolFactory;
  private volatile boolean open = true;
  private final ExecutorService executor;
  private final ExecutorService ioExecutor;
  private final Timer timer;
  private TimerTask autoCloseTimer = null;
  private final ScriptManager scriptManager = new ScriptManager();
  private final SystemDatabase systemDatabase;
  private final DefaultSecuritySystem securitySystem;
  private final CommandTimeoutChecker timeoutChecker;

  protected final long maxWALSegmentSize;
  protected final long doubleWriteLogMaxSegSize;

  public YouTrackDBEmbedded(String directoryPath, YouTrackDBConfig configuration,
      YouTrackDBEnginesManager youTrack) {
    super();
    this.youTrack = youTrack;
    youTrack.onEmbeddedFactoryInit(this);
    memory = youTrack.getEngine("memory");
    disk = youTrack.getEngine("plocal");
    directoryPath = directoryPath.trim();

    if (!directoryPath.isEmpty()) {
      final File dirFile = new File(directoryPath);
      if (!dirFile.exists()) {
        LogManager.instance()
            .info(this, "Directory " + dirFile + " does not exist, try to create it.");

        if (!dirFile.mkdirs()) {
          LogManager.instance().error(this, "Can not create directory " + dirFile, null);
        }
      }
      this.basePath = dirFile.getAbsolutePath();
    } else {
      this.basePath = null;
    }

    this.configuration =
        (YouTrackDBConfigImpl) (configuration != null ? configuration
            : YouTrackDBConfig.defaultConfig());

    if (basePath == null) {
      maxWALSegmentSize = -1;
      doubleWriteLogMaxSegSize = -1;
    } else {
      try {
        doubleWriteLogMaxSegSize = calculateDoubleWriteLogMaxSegSize(Paths.get(basePath));
        maxWALSegmentSize = calculateInitialMaxWALSegSize();

        if (maxWALSegmentSize <= 0) {
          throw new DatabaseException(
              "Invalid configuration settings. Can not set maximum size of WAL segment");
        }

        LogManager.instance()
            .info(
                this, "WAL maximum segment size is set to %,d MB", maxWALSegmentSize / 1024 / 1024);
      } catch (IOException e) {
        throw BaseException.wrapException(
            new DatabaseException("Cannot initialize YouTrackDB engine"), e);
      }
    }

    MemoryAndLocalPaginatedEnginesInitializer.INSTANCE.initialize();

    youTrack.addYouTrackDB(this);
    executor = newExecutor();
    ioExecutor = newIoExecutor();
    String timerName;
    if (basePath != null) {
      timerName = "embedded:" + basePath;
    } else {
      timerName = "memory:";
    }
    timer = new Timer("YouTrackDB Timer[" + timerName + "]");

    cachedPoolFactory = createCachedDatabasePoolFactory(this.configuration);

    initAutoClose();

    long timeout = getLongConfig(GlobalConfiguration.COMMAND_TIMEOUT);
    timeoutChecker = new CommandTimeoutChecker(timeout, this);
    systemDatabase = new SystemDatabase(this);
    securitySystem = new DefaultSecuritySystem();

    securitySystem.activate(this, this.configuration.getSecurityConfig());
  }

  private void initAutoClose() {

    boolean autoClose = getBoolConfig(GlobalConfiguration.AUTO_CLOSE_AFTER_DELAY);
    if (autoClose) {
      int autoCloseDelay = getIntConfig(GlobalConfiguration.AUTO_CLOSE_DELAY);
      final long delay = (long) autoCloseDelay * 60 * 1000;
      initAutoClose(delay);
    }
  }

  private ExecutorService newIoExecutor() {
    if (getBoolConfig(GlobalConfiguration.EXECUTOR_POOL_IO_ENABLED)) {
      int ioSize = excutorMaxSize(GlobalConfiguration.EXECUTOR_POOL_IO_MAX_SIZE);
      ExecutorService exec =
          ThreadPoolExecutors.newScalingThreadPool(
              "YouTrackDB-IO", 1, excutorBaseSize(ioSize), ioSize, 30, TimeUnit.MINUTES);
      if (getBoolConfig(GlobalConfiguration.EXECUTOR_DEBUG_TRACE_SOURCE)) {
        exec = new SourceTraceExecutorService(exec);
      }
      return exec;
    } else {
      return null;
    }
  }

  private ExecutorService newExecutor() {
    int size = excutorMaxSize(GlobalConfiguration.EXECUTOR_POOL_MAX_SIZE);
    ExecutorService exec =
        ThreadPoolExecutors.newScalingThreadPool(
            "YouTrackDBEmbedded", 1, excutorBaseSize(size), size, 30, TimeUnit.MINUTES);
    if (getBoolConfig(GlobalConfiguration.EXECUTOR_DEBUG_TRACE_SOURCE)) {
      exec = new SourceTraceExecutorService(exec);
    }
    return exec;
  }

  private boolean getBoolConfig(GlobalConfiguration config) {
    return this.configuration.getConfiguration().getValueAsBoolean(config);
  }

  private int getIntConfig(GlobalConfiguration config) {
    return this.configuration.getConfiguration().getValueAsInteger(config);
  }

  private long getLongConfig(GlobalConfiguration config) {
    return this.configuration.getConfiguration().getValueAsLong(config);
  }

  private int excutorMaxSize(GlobalConfiguration config) {
    int size = getIntConfig(config);
    if (size == 0) {
      LogManager.instance()
          .warn(
              this,
              "Configuration "
                  + config.getKey()
                  + " has a value 0 using number of CPUs as base value");
      size = Runtime.getRuntime().availableProcessors();
    } else if (size <= -1) {
      size = Runtime.getRuntime().availableProcessors();
    }
    return size;
  }

  private static int excutorBaseSize(int size) {
    int baseSize;

    if (size > 10) {
      baseSize = size / 10;
    } else if (size > 4) {
      baseSize = size / 2;
    } else {
      baseSize = size;
    }
    return baseSize;
  }

  protected CachedDatabasePoolFactory createCachedDatabasePoolFactory(YouTrackDBConfig config) {
    int capacity = getIntConfig(GlobalConfiguration.DB_CACHED_POOL_CAPACITY);
    long timeout = getIntConfig(GlobalConfiguration.DB_CACHED_POOL_CLEAN_UP_TIMEOUT);
    return new CachedDatabasePoolFactoryImpl(this, capacity, timeout);
  }

  public void initAutoClose(long delay) {
    final long scheduleTime = delay / 3;
    autoCloseTimer =
        new TimerTask() {
          @Override
          public void run() {
            YouTrackDBEmbedded.this.execute(() -> checkAndCloseStorages(delay));
          }
        };
    schedule(autoCloseTimer, scheduleTime, scheduleTime);
  }

  private synchronized void checkAndCloseStorages(long delay) {
    Set<String> toClose = new HashSet<>();
    for (AbstractPaginatedStorage storage : storages.values()) {
      if (storage.getType().equalsIgnoreCase(DatabaseType.PLOCAL.name())
          && storage.getSessionsCount() == 0) {
        long currentTime = System.currentTimeMillis();
        if (currentTime > storage.getLastCloseTime() + delay) {
          toClose.add(storage.getName());
        }
      }
    }
    for (String storage : toClose) {
      forceDatabaseClose(storage);
    }
  }

  private long calculateInitialMaxWALSegSize() throws IOException {
    String walPath =
        configuration.getConfiguration().getValueAsString(GlobalConfiguration.WAL_LOCATION);

    if (walPath == null) {
      walPath = basePath;
    }

    final FileStore fileStore = Files.getFileStore(Paths.get(walPath));
    final long freeSpace = fileStore.getUsableSpace();

    long filesSize;
    try {
      filesSize =
          Files.walk(Paths.get(walPath))
              .mapToLong(
                  p -> {
                    try {
                      if (Files.isRegularFile(p)) {
                        return Files.size(p);
                      }

                      return 0;
                    } catch (IOException | UncheckedIOException e) {
                      LogManager.instance()
                          .error(this, "Error during calculation of free space for database", e);
                      return 0;
                    }
                  })
              .sum();
    } catch (IOException | UncheckedIOException e) {
      LogManager.instance().error(this, "Error during calculation of free space for database", e);

      filesSize = 0;
    }

    long maxSegSize = getLongConfig(GlobalConfiguration.WAL_MAX_SEGMENT_SIZE) * 1024 * 1024;

    if (maxSegSize <= 0) {
      int sizePercent = getIntConfig(GlobalConfiguration.WAL_MAX_SEGMENT_SIZE_PERCENT);
      if (sizePercent <= 0) {
        throw new DatabaseException(
            "Invalid configuration settings. Can not set maximum size of WAL segment");
      }

      maxSegSize = (freeSpace + filesSize) / 100 * sizePercent;
    }

    final long minSegSizeLimit = (long) (freeSpace * 0.25);

    long minSegSize = getLongConfig(GlobalConfiguration.WAL_MIN_SEG_SIZE) * 1024 * 1024;

    if (minSegSize > minSegSizeLimit) {
      minSegSize = minSegSizeLimit;
    }

    if (minSegSize > 0 && maxSegSize < minSegSize) {
      maxSegSize = minSegSize;
    }
    return maxSegSize;
  }

  private long calculateDoubleWriteLogMaxSegSize(Path storagePath) throws IOException {
    final FileStore fileStore = Files.getFileStore(storagePath);
    final long freeSpace = fileStore.getUsableSpace();

    long filesSize;
    try {
      filesSize =
          Files.walk(storagePath)
              .mapToLong(
                  p -> {
                    try {
                      if (Files.isRegularFile(p)) {
                        return Files.size(p);
                      }

                      return 0;
                    } catch (IOException | UncheckedIOException e) {
                      LogManager.instance()
                          .error(this, "Error during calculation of free space for database", e);

                      return 0;
                    }
                  })
              .sum();
    } catch (IOException | UncheckedIOException e) {
      LogManager.instance().error(this, "Error during calculation of free space for database", e);

      filesSize = 0;
    }

    long maxSegSize =
        getLongConfig(GlobalConfiguration.STORAGE_DOUBLE_WRITE_LOG_MAX_SEG_SIZE) * 1024 * 1024;

    if (maxSegSize <= 0) {
      int sizePercent =
          getIntConfig(GlobalConfiguration.STORAGE_DOUBLE_WRITE_LOG_MAX_SEG_SIZE_PERCENT);

      if (sizePercent <= 0) {
        throw new DatabaseException(
            "Invalid configuration settings. Can not set maximum size of WAL segment");
      }

      maxSegSize = (freeSpace + filesSize) / 100 * sizePercent;
    }

    long minSegSize =
        getLongConfig(GlobalConfiguration.STORAGE_DOUBLE_WRITE_LOG_MIN_SEG_SIZE) * 1024 * 1024;

    if (minSegSize > 0 && maxSegSize < minSegSize) {
      maxSegSize = minSegSize;
    }
    return maxSegSize;
  }

  @Override
  public DatabaseSessionInternal open(String name, String user, String password) {
    return open(name, user, password, null);
  }

  public DatabaseSessionEmbedded openNoAuthenticate(String name, String user) {
    checkDatabaseName(name);
    try {
      final DatabaseSessionEmbedded embedded;
      var config = solveConfig(null);
      synchronized (this) {
        checkOpen();
        AbstractPaginatedStorage storage = getAndOpenStorage(name, config);
        embedded = newSessionInstance(storage, config, getOrCreateSharedContext(storage));
      }
      embedded.rebuildIndexes();
      embedded.internalOpen(user, "nopwd", false);
      embedded.callOnOpenListeners();
      return embedded;
    } catch (Exception e) {
      throw BaseException.wrapException(
          new DatabaseException("Cannot open database '" + name + "'"), e);
    }
  }

  protected DatabaseSessionEmbedded newSessionInstance(
      AbstractPaginatedStorage storage, YouTrackDBConfigImpl config, SharedContext sharedContext) {
    DatabaseSessionEmbedded embedded = new DatabaseSessionEmbedded(storage);
    embedded.init(config, getOrCreateSharedContext(storage));
    return embedded;
  }

  protected DatabaseSessionEmbedded newCreateSessionInstance(
      AbstractPaginatedStorage storage, YouTrackDBConfigImpl config, SharedContext sharedContext) {
    DatabaseSessionEmbedded embedded = new DatabaseSessionEmbedded(storage);
    embedded.internalCreate(config, sharedContext);
    return embedded;
  }

  public DatabaseSessionEmbedded openNoAuthorization(String name) {
    checkDatabaseName(name);
    try {
      final DatabaseSessionEmbedded embedded;
      var config = solveConfig(null);
      synchronized (this) {
        checkOpen();
        AbstractPaginatedStorage storage = getAndOpenStorage(name, config);
        embedded = newSessionInstance(storage, config, getOrCreateSharedContext(storage));
      }
      embedded.rebuildIndexes();
      embedded.callOnOpenListeners();
      return embedded;
    } catch (Exception e) {
      throw BaseException.wrapException(
          new DatabaseException("Cannot open database '" + name + "'"), e);
    }
  }

  @Override
  public DatabaseSessionInternal open(
      String name, String user, String password, YouTrackDBConfig config) {
    checkDatabaseName(name);
    checkDefaultPassword(name, user, password);
    try {
      final DatabaseSessionEmbedded embedded;
      synchronized (this) {
        checkOpen();
        config = solveConfig((YouTrackDBConfigImpl) config);
        AbstractPaginatedStorage storage = getAndOpenStorage(name, (YouTrackDBConfigImpl) config);

        embedded = newSessionInstance(storage, (YouTrackDBConfigImpl) config,
            getOrCreateSharedContext(storage));
      }
      embedded.rebuildIndexes();
      embedded.internalOpen(user, password);
      embedded.callOnOpenListeners();
      return embedded;
    } catch (Exception e) {
      throw BaseException.wrapException(
          new DatabaseException("Cannot open database '" + name + "'"), e);
    }
  }

  @Override
  public DatabaseSessionInternal open(
      AuthenticationInfo authenticationInfo, YouTrackDBConfig config) {
    try {
      final DatabaseSessionEmbedded embedded;
      synchronized (this) {
        checkOpen();
        config = solveConfig((YouTrackDBConfigImpl) config);
        if (!authenticationInfo.getDatabase().isPresent()) {
          throw new SecurityException("Authentication info do not contain the database");
        }
        String database = authenticationInfo.getDatabase().get();
        AbstractPaginatedStorage storage = getAndOpenStorage(database,
            (YouTrackDBConfigImpl) config);
        embedded = newSessionInstance(storage, (YouTrackDBConfigImpl) config,
            getOrCreateSharedContext(storage));
      }
      embedded.rebuildIndexes();
      embedded.internalOpen(authenticationInfo);
      embedded.callOnOpenListeners();
      return embedded;
    } catch (Exception e) {
      throw BaseException.wrapException(
          new DatabaseException(
              "Cannot open database '" + authenticationInfo.getDatabase() + "'"),
          e);
    }
  }

  private AbstractPaginatedStorage getAndOpenStorage(String name, YouTrackDBConfigImpl config) {
    AbstractPaginatedStorage storage = getOrInitStorage(name);
    // THIS OPEN THE STORAGE ONLY THE FIRST TIME
    try {
      // THIS OPEN THE STORAGE ONLY THE FIRST TIME
      storage.open(config.getConfiguration());
    } catch (RuntimeException e) {
      if (storage != null) {
        storages.remove(storage.getName());
      } else {
        storages.remove(name);
      }

      throw e;
    }
    return storage;
  }

  private void checkDefaultPassword(String database, String user, String password) {
    if ((("admin".equals(user) && "admin".equals(password))
        || ("reader".equals(user) && "reader".equals(password))
        || ("writer".equals(user) && "writer".equals(password)))
        && WARNING_DEFAULT_USERS.getValueAsBoolean()) {
      LogManager.instance()
          .warn(
              this,
              String.format(
                  "IMPORTANT! Using default password is unsafe, please change password for user"
                      + " '%s' on database '%s'",
                  user, database));
    }
  }

  protected YouTrackDBConfigImpl solveConfig(YouTrackDBConfigImpl config) {
    if (config != null) {
      config.setParent(this.configuration);
      return config;
    } else {
      YouTrackDBConfigImpl cfg = (YouTrackDBConfigImpl) YouTrackDBConfig.defaultConfig();
      cfg.setParent(this.configuration);
      return cfg;
    }
  }

  public DatabaseSessionInternal poolOpen(
      String name, String user, String password, DatabasePoolInternal pool) {
    final DatabaseSessionEmbedded embedded;
    synchronized (this) {
      checkOpen();
      AbstractPaginatedStorage storage = getAndOpenStorage(name, pool.getConfig());
      embedded = newPooledSessionInstance(pool, storage, getOrCreateSharedContext(storage));
    }
    embedded.rebuildIndexes();
    embedded.internalOpen(user, password);
    embedded.callOnOpenListeners();
    return embedded;
  }

  protected DatabaseSessionEmbedded newPooledSessionInstance(
      DatabasePoolInternal pool, AbstractPaginatedStorage storage, SharedContext sharedContext) {
    DatabaseSessionEmbeddedPooled embedded = new DatabaseSessionEmbeddedPooled(pool, storage);
    embedded.init(pool.getConfig(), sharedContext);
    return embedded;
  }

  protected AbstractPaginatedStorage getOrInitStorage(String name) {
    AbstractPaginatedStorage storage = storages.get(name);
    if (storage == null) {
      if (basePath == null) {
        throw new DatabaseException(
            "Cannot open database '" + name + "' because it does not exists");
      }
      Path storagePath = Paths.get(buildName(name));
      if (LocalPaginatedStorage.exists(storagePath)) {
        name = storagePath.getFileName().toString();
      }

      storage = storages.get(name);
      if (storage == null) {
        storage =
            (AbstractPaginatedStorage)
                disk.createStorage(
                    buildName(name),
                    maxWALSegmentSize,
                    doubleWriteLogMaxSegSize,
                    generateStorageId(),
                    this);
        if (storage.exists()) {
          storages.put(name, storage);
        }
      }
    }
    return storage;
  }

  protected final int generateStorageId() {
    int storageId = Math.abs(nextStorageId.getAndIncrement());
    while (!currentStorageIds.add(storageId)) {
      storageId = Math.abs(nextStorageId.getAndIncrement());
    }

    return storageId;
  }

  public synchronized AbstractPaginatedStorage getStorage(String name) {
    return storages.get(name);
  }

  protected String buildName(String name) {
    if (basePath == null) {
      throw new DatabaseException(
          "YouTrackDB instanced created without physical path, only memory databases are allowed");
    }
    return basePath + "/" + name;
  }

  public void create(String name, String user, String password, DatabaseType type) {
    create(name, user, password, type, null);
  }

  @Override
  public void create(
      String name, String user, String password, DatabaseType type, YouTrackDBConfig config) {
    create(name, user, password, type, config, null);
  }

  @Override
  public void create(
      String name,
      String user,
      String password,
      DatabaseType type,
      YouTrackDBConfig config,
      DatabaseTask<Void> createOps) {
    checkDatabaseName(name);
    final DatabaseSessionEmbedded embedded;
    synchronized (this) {
      if (!exists(name, user, password)) {
        try {
          config = solveConfig((YouTrackDBConfigImpl) config);
          AbstractPaginatedStorage storage;
          if (type == DatabaseType.MEMORY) {
            storage =
                (AbstractPaginatedStorage)
                    memory.createStorage(
                        name,
                        maxWALSegmentSize,
                        doubleWriteLogMaxSegSize,
                        generateStorageId(),
                        this);
          } else {
            storage =
                (AbstractPaginatedStorage)
                    disk.createStorage(
                        buildName(name),
                        maxWALSegmentSize,
                        doubleWriteLogMaxSegSize,
                        generateStorageId(),
                        this);
          }
          storages.put(name, storage);
          embedded = internalCreate((YouTrackDBConfigImpl) config, storage);
          if (createOps != null) {
            ScenarioThreadLocal.executeAsDistributed(
                () -> {
                  createOps.call(embedded);
                  return null;
                });
          }
        } catch (Exception e) {
          throw BaseException.wrapException(
              new DatabaseException("Cannot create database '" + name + "'"), e);
        }
      } else {
        throw new DatabaseException(
            "Cannot create new database '" + name + "' because it already exists");
      }
    }
    embedded.callOnCreateListeners();
    DatabaseRecordThreadLocal.instance().remove();
  }

  @Override
  public void networkRestore(String name, InputStream in, Callable<Object> callable) {
    checkDatabaseName(name);
    AbstractPaginatedStorage storage = null;
    try {
      SharedContext context;
      synchronized (this) {
        context = sharedContexts.get(name);
        if (context != null) {
          context.close();
        }
        storage = getOrInitStorage(name);
        storages.put(name, storage);
      }
      storage.restore(in, null, callable, null);
      distributedSetOnline(name);
    } catch (ModificationOperationProhibitedException e) {
      throw e;
    } catch (Exception e) {
      try {
        if (storage != null) {
          storage.delete();
        }
      } catch (Exception e1) {
        LogManager.instance()
            .warn(this, "Error doing cleanups, should be safe do progress anyway", e1);
      }
      synchronized (this) {
        sharedContexts.remove(name);
        storages.remove(name);
      }

      ContextConfiguration configs = configuration.getConfiguration();
      LocalPaginatedStorage.deleteFilesFromDisc(
          name,
          configs.getValueAsInteger(FILE_DELETE_RETRY),
          configs.getValueAsInteger(FILE_DELETE_DELAY),
          buildName(name));
      throw BaseException.wrapException(
          new DatabaseException("Cannot create database '" + name + "'"), e);
    }
  }

  public void restore(
      String name,
      String user,
      String password,
      DatabaseType type,
      String path,
      YouTrackDBConfig config) {
    checkDatabaseName(name);
    config = solveConfig((YouTrackDBConfigImpl) config);
    final DatabaseSessionEmbedded embedded;
    AbstractPaginatedStorage storage;
    synchronized (this) {
      if (!exists(name, null, null)) {
        try {
          storage =
              (AbstractPaginatedStorage)
                  disk.createStorage(
                      buildName(name),
                      maxWALSegmentSize,
                      doubleWriteLogMaxSegSize,
                      generateStorageId(),
                      this);
          embedded = internalCreate((YouTrackDBConfigImpl) config, storage);
          storages.put(name, storage);
        } catch (Exception e) {
          throw BaseException.wrapException(
              new DatabaseException("Cannot restore database '" + name + "'"), e);
        }
      } else {
        throw new DatabaseException(
            "Cannot create new storage '" + name + "' because it already exists");
      }
    }
    storage.restoreFromIncrementalBackup(null, path);
    embedded.callOnCreateListeners();
    embedded.getSharedContext().reInit(storage, embedded);
    DatabaseRecordThreadLocal.instance().remove();
  }

  public void restore(
      String name,
      InputStream in,
      Map<String, Object> options,
      Callable<Object> callable,
      CommandOutputListener iListener) {
    checkDatabaseName(name);
    try {
      AbstractPaginatedStorage storage;
      synchronized (this) {
        SharedContext context = sharedContexts.remove(name);
        if (context != null) {
          context.close();
        }
        storage = getOrInitStorage(name);
        storages.put(name, storage);
      }
      storage.restore(in, options, callable, iListener);
    } catch (Exception e) {
      synchronized (this) {
        storages.remove(name);
      }
      ContextConfiguration configs = configuration.getConfiguration();
      LocalPaginatedStorage.deleteFilesFromDisc(
          name,
          configs.getValueAsInteger(FILE_DELETE_RETRY),
          configs.getValueAsInteger(FILE_DELETE_DELAY),
          buildName(name));
      throw BaseException.wrapException(
          new DatabaseException("Cannot create database '" + name + "'"), e);
    }
  }

  protected DatabaseSessionEmbedded internalCreate(
      YouTrackDBConfigImpl config, AbstractPaginatedStorage storage) {
    storage.create(config.getConfiguration());
    return newCreateSessionInstance(storage, config, getOrCreateSharedContext(storage));
  }

  protected synchronized SharedContext getOrCreateSharedContext(
      AbstractPaginatedStorage storage) {
    SharedContext result = sharedContexts.get(storage.getName());
    if (result == null) {
      result = createSharedContext(storage);
      sharedContexts.put(storage.getName(), result);
    }
    return result;
  }

  protected SharedContext createSharedContext(AbstractPaginatedStorage storage) {
    return new SharedContextEmbedded(storage, this);
  }

  @Override
  public synchronized boolean exists(String name, String user, String password) {
    checkOpen();
    Storage storage = storages.get(name);
    if (storage == null) {
      if (basePath != null) {
        return LocalPaginatedStorage.exists(Paths.get(buildName(name)));
      } else {
        return false;
      }
    }
    return storage.exists();
  }

  @Override
  public void internalDrop(String database) {
    this.drop(database, null, null);
  }

  @Override
  public void drop(String name, String user, String password) {
    synchronized (this) {
      checkOpen();
    }
    checkDatabaseName(name);
    DatabaseSessionInternal current = DatabaseRecordThreadLocal.instance().getIfDefined();
    try {
      DatabaseSessionInternal db = openNoAuthenticate(name, user);
      for (Iterator<DatabaseLifecycleListener> it = youTrack.getDbLifecycleListeners();
          it.hasNext(); ) {
        it.next().onDrop(db);
      }
      db.close();
    } finally {
      DatabaseRecordThreadLocal.instance().set(current);
      synchronized (this) {
        if (exists(name, user, password)) {
          AbstractPaginatedStorage storage = getOrInitStorage(name);
          SharedContext sharedContext = sharedContexts.get(name);
          if (sharedContext != null) {
            sharedContext.close();
          }
          final int storageId = storage.getId();
          storage.delete();
          storages.remove(name);
          currentStorageIds.remove(storageId);
          sharedContexts.remove(name);
        }
      }
    }
  }

  protected interface DatabaseFound {

    void found(String name);
  }

  @Override
  public synchronized Set<String> listDatabases(String user, String password) {
    checkOpen();
    // SEARCH IN CONFIGURED PATHS
    final Set<String> databases = new HashSet<>();
    // SEARCH IN DEFAULT DATABASE DIRECTORY
    if (basePath != null) {
      scanDatabaseDirectory(new File(basePath), databases::add);
    }
    databases.addAll(this.storages.keySet());
    // TODO: Verify validity this generic permission on guest
    if (!securitySystem.isAuthorized(null, "guest", "server.listDatabases.system")) {
      databases.remove(SystemDatabase.SYSTEM_DB_NAME);
    }
    return databases;
  }

  public synchronized void loadAllDatabases() {
    if (basePath != null) {
      scanDatabaseDirectory(
          new File(basePath),
          (name) -> {
            if (!storages.containsKey(name)) {
              AbstractPaginatedStorage storage = getOrInitStorage(name);
              // THIS OPEN THE STORAGE ONLY THE FIRST TIME
              storage.open(configuration.getConfiguration());
            }
          });
    }
  }

  public DatabasePoolInternal openPool(String name, String user, String password) {
    return openPool(name, user, password, null);
  }

  @Override
  public DatabasePoolInternal openPool(
      String name, String user, String password, YouTrackDBConfig config) {
    checkDatabaseName(name);
    checkOpen();
    DatabasePoolImpl pool = new DatabasePoolImpl(this, name, user, password,
        solveConfig((YouTrackDBConfigImpl) config));
    pools.add(pool);
    return pool;
  }

  @Override
  public DatabasePoolInternal cachedPool(String database, String user, String password) {
    return cachedPool(database, user, password, null);
  }

  @Override
  public DatabasePoolInternal cachedPool(
      String database, String user, String password, YouTrackDBConfig config) {
    checkDatabaseName(database);
    checkOpen();
    DatabasePoolInternal pool =
        cachedPoolFactory.get(database, user, password, solveConfig((YouTrackDBConfigImpl) config));
    pools.add(pool);
    return pool;
  }

  @Override
  public void close() {
    if (!open) {
      return;
    }
    timeoutChecker.close();
    timer.cancel();
    securitySystem.shutdown();
    executor.shutdown();
    try {
      while (!executor.awaitTermination(1, TimeUnit.MINUTES)) {
        LogManager.instance().warn(this, "Failed waiting background operations termination");
        executor.shutdownNow();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    synchronized (this) {
      scriptManager.closeAll();
      internalClose();
      currentStorageIds.clear();
    }
    if (ioExecutor != null) {
      try {
        ioExecutor.shutdown();
        while (!ioExecutor.awaitTermination(1, TimeUnit.MINUTES)) {
          LogManager.instance().warn(this, "Failed waiting background io operations termination");
          ioExecutor.shutdownNow();
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    removeShutdownHook();
  }


  public synchronized void internalClose() {
    if (!open) {
      return;
    }
    open = false;
    this.sharedContexts.values().forEach(SharedContext::close);
    final List<AbstractPaginatedStorage> storagesCopy = new ArrayList<>(storages.values());

    Exception storageException = null;

    for (AbstractPaginatedStorage stg : storagesCopy) {
      try {
        LogManager.instance().info(this, "- shutdown storage: %s ...", stg.getName());
        stg.shutdown();
      } catch (Exception e) {
        LogManager.instance().warn(this, "-- error on shutdown storage", e);
        storageException = e;
      } catch (Error e) {
        LogManager.instance().warn(this, "-- error on shutdown storage", e);
        throw e;
      }
    }
    this.sharedContexts.clear();
    storages.clear();
    youTrack.onEmbeddedFactoryClose(this);
    if (autoCloseTimer != null) {
      autoCloseTimer.cancel();
    }

    if (storageException != null) {
      throw BaseException.wrapException(
          new StorageException("Error during closing the storages"), storageException);
    }
  }

  public YouTrackDBConfigImpl getConfiguration() {
    return configuration;
  }

  public void removePool(DatabasePoolInternal pool) {
    pools.remove(pool);
  }

  private static void scanDatabaseDirectory(final File directory, DatabaseFound found) {
    if (directory.exists() && directory.isDirectory()) {
      final File[] files = directory.listFiles();
      if (files != null) {
        for (File db : files) {
          if (db.isDirectory()) {
            for (File cf : db.listFiles()) {
              String fileName = cf.getName();
              if (fileName.equals("database.ocf")
                  || (fileName.startsWith(ClusterBasedStorageConfiguration.COMPONENT_NAME)
                  && fileName.endsWith(
                  ClusterBasedStorageConfiguration.DATA_FILE_EXTENSION))) {
                found.found(db.getName());
                break;
              }
            }
          }
        }
      }
    }
  }

  public synchronized void initCustomStorage(
      String name, String path, String userName, String userPassword) {
    DatabaseSessionEmbedded embedded = null;
    synchronized (this) {
      boolean exists = LocalPaginatedStorage.exists(Paths.get(path));
      AbstractPaginatedStorage storage =
          (AbstractPaginatedStorage)
              disk.createStorage(
                  path, maxWALSegmentSize, doubleWriteLogMaxSegSize, generateStorageId(), this);
      // TODO: Add Creation settings and parameters
      if (!exists) {
        embedded = internalCreate(configuration, storage);
      }
      storages.put(name, storage);
    }
    if (embedded != null) {
      embedded.callOnCreateListeners();
      DatabaseRecordThreadLocal.instance().remove();
    }
  }

  public void removeShutdownHook() {
    youTrack.removeYouTrackDB(this);
  }

  public synchronized Collection<Storage> getStorages() {
    return storages.values().stream().map((x) -> (Storage) x).collect(Collectors.toSet());
  }

  public synchronized void forceDatabaseClose(String iDatabaseName) {
    AbstractPaginatedStorage storage = storages.remove(iDatabaseName);
    if (storage != null) {
      SharedContext ctx = sharedContexts.remove(iDatabaseName);
      ctx.close();
      storage.shutdown();
    }
  }

  protected void checkOpen() {
    if (!open) {
      throw new DatabaseException("YouTrackDB Instance is closed");
    }
  }

  public boolean isOpen() {
    return open;
  }

  @Override
  public boolean isEmbedded() {
    return true;
  }

  public void schedule(TimerTask task, long delay, long period) {
    timer.schedule(task, delay, period);
  }

  public void scheduleOnce(TimerTask task, long delay) {
    timer.schedule(task, delay);
  }

  @Override
  public <X> Future<X> execute(String database, String user, DatabaseTask<X> task) {
    return executor.submit(
        () -> {
          try (var session = openNoAuthenticate(database, user)) {
            return task.call(session);
          }
        });
  }

  @Override
  public Future<?> execute(Runnable task) {
    return executor.submit(task);
  }

  @Override
  public <X> Future<X> execute(Callable<X> task) {
    return executor.submit(task);
  }

  @Override
  public <X> Future<X> executeNoAuthorizationAsync(String database, DatabaseTask<X> task) {
    return executor.submit(
        () -> {
          if (open) {
            try (var session = openNoAuthorization(database)) {
              return task.call(session);
            }
          } else {
            LogManager.instance()
                .warn(this, " Cancelled execution of task, YouTrackDB instance is closed");
            return null;
          }
        });
  }

  @Override
  public <X> X executeNoAuthorizationSync(
      DatabaseSessionInternal database, DatabaseTask<X> task) {
    var dbName = database.getName();
    if (open) {
      try (var session = openNoAuthorization(dbName)) {
        return task.call(session);
      } finally {
        database.activateOnCurrentThread();
      }
    } else {
      throw new DatabaseException("YouTrackDB instance is closed");
    }
  }

  public <X> Future<X> executeNoDb(Callable<X> callable) {
    return executor.submit(callable);
  }

  public ScriptManager getScriptManager() {
    return scriptManager;
  }

  public ResultSet executeServerStatementNamedParams(String script, String username, String pw,
      Map<String, Object> args) {
    SQLServerStatement statement = SQLEngine.parseServerStatement(script, this);
    ResultSet original = statement.execute(this, args, true);
    LocalResultSetLifecycleDecorator result;
    //    if (!statement.isIdempotent()) {
    // fetch all, close and detach
    // TODO pagination!
    InternalResultSet prefetched = new InternalResultSet();
    original.forEachRemaining(x -> prefetched.add(x));
    original.close();
    result = new LocalResultSetLifecycleDecorator(prefetched);
    //    } else {
    // stream, keep open and attach to the current DB
    //      result = new LocalResultSetLifecycleDecorator(original);
    //      this.queryStarted(result.getQueryId(), result);
    //      result.addLifecycleListener(this);
    //    }
    return result;
  }

  public ResultSet executeServerStatementPositionalParams(
      String script, String username, String pw, Object... args) {
    SQLServerStatement statement = SQLEngine.parseServerStatement(script, this);
    ResultSet original = statement.execute(this, args, true);
    LocalResultSetLifecycleDecorator result;
    //    if (!statement.isIdempotent()) {
    // fetch all, close and detach
    // TODO pagination!
    InternalResultSet prefetched = new InternalResultSet();
    original.forEachRemaining(x -> prefetched.add(x));
    original.close();
    result = new LocalResultSetLifecycleDecorator(prefetched);
    //    } else {
    // stream, keep open and attach to the current DB
    //      result = new LocalResultSetLifecycleDecorator(original);
    //      this.queryStarted(result.getQueryId(), result);
    //      result.addLifecycleListener(this);
    //    }
    return result;
  }

  @Override
  public SystemDatabase getSystemDatabase() {
    return systemDatabase;
  }

  public DefaultSecuritySystem getSecuritySystem() {
    return securitySystem;
  }

  @Override
  public String getBasePath() {
    return basePath;
  }

  public boolean isMemoryOnly() {
    return basePath == null;
  }

  private void checkDatabaseName(String name) {
    if (name == null) {
      throw new NullArgumentException("database");
    }
    if (name.contains("/") || name.contains(":")) {
      throw new DatabaseException(String.format("Invalid database name:'%s'", name));
    }
  }

  public Set<String> listLodadedDatabases() {
    Set<String> dbs;
    synchronized (this) {
      dbs = new HashSet<String>(storages.keySet());
    }
    dbs.remove(SystemDatabase.SYSTEM_DB_NAME);
    return dbs;
  }

  public void startCommand(Optional<Long> timeout) {
    timeoutChecker.startCommand(timeout);
  }

  public void endCommand() {
    timeoutChecker.endCommand();
  }

  @Override
  public String getConnectionUrl() {
    String connectionUrl = "embedded:";
    if (basePath != null) {
      connectionUrl += basePath;
    }
    return connectionUrl;
  }

  public ExecutorService getIoExecutor() {
    return ioExecutor;
  }
}
