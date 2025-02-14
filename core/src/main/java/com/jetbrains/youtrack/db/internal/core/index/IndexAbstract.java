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
package com.jetbrains.youtrack.db.internal.core.index;

import com.jetbrains.youtrack.db.api.config.GlobalConfiguration;
import com.jetbrains.youtrack.db.api.exception.BaseException;
import com.jetbrains.youtrack.db.api.exception.CommandExecutionException;
import com.jetbrains.youtrack.db.api.exception.ConfigurationException;
import com.jetbrains.youtrack.db.api.exception.ManualIndexesAreProhibited;
import com.jetbrains.youtrack.db.api.exception.RecordDuplicatedException;
import com.jetbrains.youtrack.db.api.record.DBRecord;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.api.record.RID;
import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.internal.common.concur.lock.OneEntryPerKeyLockManager;
import com.jetbrains.youtrack.db.internal.common.concur.lock.PartitionedLockManager;
import com.jetbrains.youtrack.db.internal.common.listener.ProgressListener;
import com.jetbrains.youtrack.db.internal.common.log.LogManager;
import com.jetbrains.youtrack.db.internal.common.util.RawPair;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.exception.InvalidIndexEngineIdException;
import com.jetbrains.youtrack.db.internal.core.index.comparator.AlwaysGreaterKey;
import com.jetbrains.youtrack.db.internal.core.index.comparator.AlwaysLessKey;
import com.jetbrains.youtrack.db.internal.core.index.engine.BaseIndexEngine;
import com.jetbrains.youtrack.db.internal.core.index.iterator.IndexCursorStream;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityInternalUtils;
import com.jetbrains.youtrack.db.internal.core.storage.Storage;
import com.jetbrains.youtrack.db.internal.core.storage.cache.ReadCache;
import com.jetbrains.youtrack.db.internal.core.storage.cache.WriteCache;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.AbstractPaginatedStorage;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.atomicoperations.AtomicOperation;
import com.jetbrains.youtrack.db.internal.core.storage.ridbag.sbtree.IndexRIDContainer;
import com.jetbrains.youtrack.db.internal.core.tx.FrontendTransactionIndexChanges.OPERATION;
import com.jetbrains.youtrack.db.internal.core.tx.FrontendTransactionIndexChangesPerKey;
import com.jetbrains.youtrack.db.internal.core.tx.FrontendTransactionIndexChangesPerKey.TransactionIndexEntry;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

/**
 * Handles indexing when records change. The underlying lock manager for keys can be the
 * {@link PartitionedLockManager}, the default one, or the {@link OneEntryPerKeyLockManager} in case
 * of distributed. This is to avoid deadlock situation between nodes where keys have the same hash
 * code.
 */
public abstract class IndexAbstract implements IndexInternal {

  private static final AlwaysLessKey ALWAYS_LESS_KEY = new AlwaysLessKey();
  private static final AlwaysGreaterKey ALWAYS_GREATER_KEY = new AlwaysGreaterKey();
  protected static final String CONFIG_MAP_RID = "mapRid";
  private static final String CONFIG_CLUSTERS = "clusters";
  protected final AbstractPaginatedStorage storage;
  private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

  protected volatile int indexId = -1;
  protected volatile int apiVersion = -1;

  protected Set<String> clustersToIndex = new HashSet<>();
  protected IndexMetadata im;

  public IndexAbstract(IndexMetadata im, final Storage storage) {
    acquireExclusiveLock();
    try {
      this.im = im;
      this.storage = (AbstractPaginatedStorage) storage;
    } finally {
      releaseExclusiveLock();
    }
  }

  public static IndexMetadata loadMetadataFromDoc(final EntityImpl config) {
    return loadMetadataInternal(
        config,
        config.field(CONFIG_TYPE),
        config.field(ALGORITHM),
        config.field(VALUE_CONTAINER_ALGORITHM));
  }

  public static IndexMetadata loadMetadataInternal(
      final EntityImpl config,
      final String type,
      final String algorithm,
      final String valueContainerAlgorithm) {
    final String indexName = config.field(CONFIG_NAME);

    final EntityImpl indexDefinitionEntity = config.field(INDEX_DEFINITION);
    IndexDefinition loadedIndexDefinition = null;
    if (indexDefinitionEntity != null) {
      try {
        final String indexDefClassName = config.field(INDEX_DEFINITION_CLASS);
        final Class<?> indexDefClass = Class.forName(indexDefClassName);
        loadedIndexDefinition =
            (IndexDefinition) indexDefClass.getDeclaredConstructor().newInstance();
        loadedIndexDefinition.fromStream(indexDefinitionEntity);

      } catch (final ClassNotFoundException
                     | IllegalAccessException
                     | InstantiationException
                     | InvocationTargetException
                     | NoSuchMethodException e) {
        throw BaseException.wrapException(
            new IndexException("Error during deserialization of index definition"), e);
      }
    } else {
      // @COMPATIBILITY 1.0rc6 new index model was implemented
      final Boolean isAutomatic = config.field(CONFIG_AUTOMATIC);
      IndexFactory factory = Indexes.getFactory(type, algorithm);
      if (Boolean.TRUE.equals(isAutomatic)) {
        final int pos = indexName.lastIndexOf('.');
        if (pos < 0) {
          throw new IndexException(
              "Cannot convert from old index model to new one. "
                  + "Invalid index name. Dot (.) separator should be present");
        }
        final String className = indexName.substring(0, pos);
        final String propertyName = indexName.substring(pos + 1);

        final String keyTypeStr = config.field(CONFIG_KEYTYPE);
        if (keyTypeStr == null) {
          throw new IndexException(
              "Cannot convert from old index model to new one. " + "Index key type is absent");
        }
        final PropertyType keyType = PropertyType.valueOf(keyTypeStr.toUpperCase(Locale.ENGLISH));

        loadedIndexDefinition = new PropertyIndexDefinition(className, propertyName, keyType);

        config.removeField(CONFIG_AUTOMATIC);
        config.removeField(CONFIG_KEYTYPE);
      } else if (config.field(CONFIG_KEYTYPE) != null) {
        final String keyTypeStr = config.field(CONFIG_KEYTYPE);
        final PropertyType keyType = PropertyType.valueOf(keyTypeStr.toUpperCase(Locale.ENGLISH));

        loadedIndexDefinition = new SimpleKeyIndexDefinition(keyType);

        config.removeField(CONFIG_KEYTYPE);
      }
    }

    final Set<String> clusters = new HashSet<>(
        config.field(CONFIG_CLUSTERS, PropertyType.EMBEDDEDSET));

    final int indexVersion =
        config.field(INDEX_VERSION) == null
            ? 1
            : (Integer) config.field(INDEX_VERSION);

    //this trick is used to keep backward compatibility with old index model
    var metadataEntity = config.<EntityImpl>field(METADATA);
    return new IndexMetadata(
        indexName,
        loadedIndexDefinition,
        clusters,
        type,
        algorithm,
        valueContainerAlgorithm,
        indexVersion, metadataEntity != null ? metadataEntity.toMap() : null
    );
  }

  @Override
  public boolean hasRangeQuerySupport() {
    acquireSharedLock();
    try {
      while (true) {
        try {
          return storage.hasIndexRangeQuerySupport(indexId);
        } catch (InvalidIndexEngineIdException ignore) {
          doReloadIndexEngine();
        }
      }
    } finally {
      releaseSharedLock();
    }
  }

  /**
   * Creates the index.
   */
  public IndexInternal create(
      DatabaseSessionInternal session, final IndexMetadata indexMetadata,
      boolean rebuild,
      final ProgressListener progressListener) {
    acquireExclusiveLock();
    try {
      Set<String> clustersToIndex = indexMetadata.getClustersToIndex();

      if (clustersToIndex != null) {
        this.clustersToIndex = new HashSet<>(clustersToIndex);
      } else {
        this.clustersToIndex = new HashSet<>();
      }

      // do not remove this, it is needed to remove index garbage if such one exists
      try {
        if (apiVersion == 0) {
          removeValuesContainer();
        }
      } catch (Exception e) {
        LogManager.instance().error(this, "Error during deletion of index '%s'", e, im.getName());
      }
      Map<String, String> engineProperties = new HashMap<>();
      indexMetadata.setVersion(im.getVersion());
      indexId = storage.addIndexEngine(indexMetadata, engineProperties);
      apiVersion = AbstractPaginatedStorage.extractEngineAPIVersion(indexId);

      assert indexId >= 0;
      assert apiVersion >= 0;

      onIndexEngineChange(indexId);

      if (rebuild) {
        fillIndex(session, progressListener, false);
      }
    } catch (Exception e) {
      LogManager.instance().error(this, "Exception during index '%s' creation", e, im.getName());
      // index is created inside of storage
      if (indexId >= 0) {
        doDelete(session);
      }
      throw BaseException.wrapException(
          new IndexException("Cannot create the index '" + im.getName() + "'"), e);
    } finally {
      releaseExclusiveLock();
    }

    return this;
  }

  protected void doReloadIndexEngine() {
    indexId = storage.loadIndexEngine(im.getName());
    apiVersion = AbstractPaginatedStorage.extractEngineAPIVersion(indexId);

    if (indexId < 0) {
      throw new IllegalStateException("Index " + im.getName() + " can not be loaded");
    }
  }

  public boolean loadFromConfiguration(DatabaseSessionInternal session,
      final EntityImpl config) {
    acquireExclusiveLock();
    try {
      clustersToIndex.clear();

      final IndexMetadata indexMetadata = loadMetadata(config);
      this.im = indexMetadata;
      clustersToIndex.addAll(indexMetadata.getClustersToIndex());

      try {
        indexId = storage.loadIndexEngine(im.getName());
        apiVersion = AbstractPaginatedStorage.extractEngineAPIVersion(indexId);

        if (indexId == -1) {
          Map<String, String> engineProperties = new HashMap<>();
          indexId = storage.loadExternalIndexEngine(indexMetadata, engineProperties);
          apiVersion = AbstractPaginatedStorage.extractEngineAPIVersion(indexId);
        }

        if (indexId == -1) {
          return false;
        }

        onIndexEngineChange(indexId);

      } catch (Exception e) {
        LogManager.instance()
            .error(
                this,
                "Error during load of index '%s'",
                e,
                im.getName());

        if (isAutomatic()) {
          // AUTOMATIC REBUILD IT
          LogManager.instance()
              .warn(this, "Cannot load index '%s' rebuilt it from scratch", im.getName());
          try {
            rebuild(session);
          } catch (Exception t) {
            LogManager.instance()
                .error(
                    this,
                    "Cannot rebuild index '%s' because '"
                        + t
                        + "'. The index will be removed in configuration",
                    e,
                    im.getName());
            // REMOVE IT
            return false;
          }
        }
      }

      return true;
    } finally {
      releaseExclusiveLock();
    }
  }

  @Override
  public IndexMetadata loadMetadata(final EntityImpl config) {
    return loadMetadataInternal(
        config, im.getType(), im.getAlgorithm(), im.getValueContainerAlgorithm());
  }

  /**
   * {@inheritDoc}
   */
  public long rebuild(DatabaseSessionInternal session) {
    return rebuild(session, new IndexRebuildOutputListener(this));
  }

  @Override
  public void close() {
  }

  /**
   * @return number of entries in the index.
   */
  @Deprecated
  public long getSize(DatabaseSessionInternal session) {
    return size(session);
  }

  /**
   * Counts the entries for the key.
   */
  @Deprecated
  public long count(DatabaseSessionInternal session, Object iKey) {
    try (Stream<RawPair<Object, RID>> stream =
        streamEntriesBetween(session, iKey, true, iKey, true, true)) {
      return stream.count();
    }
  }

  /**
   * @return Number of keys in index
   */
  @Deprecated
  public long getKeySize() {
    try (Stream<Object> stream = keyStream()) {
      return stream.distinct().count();
    }
  }

  /**
   * Flushes in-memory changes to disk.
   */
  @Deprecated
  public void flush() {
    // do nothing
  }

  @Deprecated
  public long getRebuildVersion() {
    return 0;
  }

  /**
   * @return Indicates whether index is rebuilding at the moment.
   * @see #getRebuildVersion()
   */
  @Deprecated
  public boolean isRebuilding() {
    return false;
  }

  @Deprecated
  public Object getFirstKey() {
    try (final Stream<Object> stream = keyStream()) {
      final Iterator<Object> iterator = stream.iterator();
      if (iterator.hasNext()) {
        return iterator.next();
      }

      return null;
    }
  }

  @Deprecated
  public Object getLastKey(DatabaseSessionInternal session) {
    try (final Stream<RawPair<Object, RID>> stream = descStream(session)) {
      final Iterator<RawPair<Object, RID>> iterator = stream.iterator();
      if (iterator.hasNext()) {
        return iterator.next().first;
      }

      return null;
    }
  }

  @Deprecated
  public IndexCursor cursor(DatabaseSessionInternal session) {
    return new IndexCursorStream(stream(session));
  }

  @Deprecated
  @Override
  public IndexCursor descCursor(DatabaseSessionInternal session) {
    return new IndexCursorStream(descStream(session));
  }

  @Deprecated
  @Override
  public IndexKeyCursor keyCursor() {
    return new IndexKeyCursor() {
      private final Iterator<Object> keyIterator = keyStream().iterator();

      @Override
      public Object next(int prefetchSize) {
        if (keyIterator.hasNext()) {
          return keyIterator.next();
        }

        return null;
      }
    };
  }

  @Deprecated
  @Override
  public IndexCursor iterateEntries(DatabaseSessionInternal session, Collection<?> keys,
      boolean ascSortOrder) {
    return new IndexCursorStream(streamEntries(session, keys, ascSortOrder));
  }

  @Deprecated
  @Override
  public IndexCursor iterateEntriesBetween(
      DatabaseSessionInternal session, Object fromKey, boolean fromInclusive, Object toKey,
      boolean toInclusive, boolean ascOrder) {
    return new IndexCursorStream(
        streamEntriesBetween(session, fromKey, fromInclusive, toKey, toInclusive, ascOrder));
  }

  @Deprecated
  @Override
  public IndexCursor iterateEntriesMajor(DatabaseSessionInternal session, Object fromKey,
      boolean fromInclusive, boolean ascOrder) {
    return new IndexCursorStream(streamEntriesMajor(session, fromKey, fromInclusive, ascOrder));
  }

  @Deprecated
  @Override
  public IndexCursor iterateEntriesMinor(DatabaseSessionInternal session, Object toKey,
      boolean toInclusive, boolean ascOrder) {
    return new IndexCursorStream(streamEntriesMajor(session, toKey, toInclusive, ascOrder));
  }

  /**
   * {@inheritDoc}
   */
  public long rebuild(DatabaseSessionInternal session,
      final ProgressListener iProgressListener) {
    long entitiesIndexed;

    acquireExclusiveLock();
    try {
      try {
        if (indexId >= 0) {
          doDelete(session);
        }
      } catch (Exception e) {
        LogManager.instance().error(this, "Error during index '%s' delete", e, im.getName());
      }

      IndexMetadata indexMetadata = this.loadMetadata(updateConfiguration(session));
      Map<String, String> engineProperties = new HashMap<>();
      indexId = storage.addIndexEngine(indexMetadata, engineProperties);
      apiVersion = AbstractPaginatedStorage.extractEngineAPIVersion(indexId);

      onIndexEngineChange(indexId);
    } catch (Exception e) {
      try {
        if (indexId >= 0) {
          storage.clearIndex(indexId);
        }
      } catch (Exception e2) {
        LogManager.instance().error(this, "Error during index rebuild", e2);
        // IGNORE EXCEPTION: IF THE REBUILD WAS LAUNCHED IN CASE OF RID INVALID CLEAR ALWAYS GOES IN
        // ERROR
      }

      throw BaseException.wrapException(
          new IndexException("Error on rebuilding the index for clusters: " + clustersToIndex),
          e);
    } finally {
      releaseExclusiveLock();
    }

    acquireSharedLock();
    try {
      entitiesIndexed = fillIndex(session, iProgressListener, true);
    } catch (final Exception e) {
      LogManager.instance().error(this, "Error during index rebuild", e);
      try {
        if (indexId >= 0) {
          storage.clearIndex(indexId);
        }
      } catch (Exception e2) {
        LogManager.instance().error(this, "Error during index rebuild", e2);
        // IGNORE EXCEPTION: IF THE REBUILD WAS LAUNCHED IN CASE OF RID INVALID CLEAR ALWAYS GOES IN
        // ERROR
      }

      throw BaseException.wrapException(
          new IndexException("Error on rebuilding the index for clusters: " + clustersToIndex),
          e);
    } finally {
      releaseSharedLock();
    }

    return entitiesIndexed;
  }

  private long fillIndex(DatabaseSessionInternal session,
      final ProgressListener iProgressListener, final boolean rebuild) {
    long entitiesIndexed = 0;
    try {
      long entityNum = 0;
      long entitiesTotal = 0;

      for (final String cluster : clustersToIndex) {
        entitiesTotal += storage.count(session, storage.getClusterIdByName(cluster));
      }

      if (iProgressListener != null) {
        iProgressListener.onBegin(this, entitiesTotal, rebuild);
      }

      // INDEX ALL CLUSTERS
      for (final String clusterName : clustersToIndex) {
        final long[] metrics =
            indexCluster(session, clusterName, iProgressListener, entityNum,
                entitiesIndexed, entitiesTotal);
        entityNum = metrics[0];
        entitiesIndexed = metrics[1];
      }

      if (iProgressListener != null) {
        iProgressListener.onCompletition(session, this, true);
      }
    } catch (final RuntimeException e) {
      if (iProgressListener != null) {
        iProgressListener.onCompletition(session, this, false);
      }
      throw e;
    }
    return entitiesIndexed;
  }

  @Override
  public boolean doRemove(DatabaseSessionInternal session, AbstractPaginatedStorage storage,
      Object key, RID rid)
      throws InvalidIndexEngineIdException {
    return doRemove(storage, key);
  }

  public boolean remove(DatabaseSessionInternal session, Object key, final Identifiable rid) {
    key = getCollatingValue(key);
    session.getTransaction().addIndexEntry(this, getName(), OPERATION.REMOVE, key, rid);
    return true;
  }

  public boolean remove(DatabaseSessionInternal session, Object key) {
    key = getCollatingValue(key);

    session.getTransaction().addIndexEntry(this, getName(), OPERATION.REMOVE, key, null);
    return true;
  }

  @Override
  public boolean doRemove(AbstractPaginatedStorage storage, Object key)
      throws InvalidIndexEngineIdException {
    return storage.removeKeyFromIndex(indexId, key);
  }

  /**
   * {@inheritDoc}
   *
   * @deprecated Manual indexes are deprecated and will be removed
   */
  @Override
  @Deprecated
  public Index clear(DatabaseSessionInternal session) {
    session.getTransaction().addIndexEntry(this, this.getName(), OPERATION.CLEAR, null, null);
    return this;
  }

  public IndexInternal delete(DatabaseSessionInternal session) {
    acquireExclusiveLock();

    try {
      doDelete(session);
      // REMOVE THE INDEX ALSO FROM CLASS MAP
      if (session.getMetadata() != null) {
        session.getMetadata().getIndexManagerInternal()
            .removeClassPropertyIndex(session, this);
      }

      return this;
    } finally {
      releaseExclusiveLock();
    }
  }

  protected void doDelete(DatabaseSessionInternal session) {
    while (true) {
      try {
        //noinspection ObjectAllocationInLoop
        try {
          try (final Stream<RawPair<Object, RID>> stream = stream(session)) {
            session.executeInTxBatches(stream, (db, entry) -> {
              remove(session, entry.first, entry.second);
            });
          }
        } catch (IndexEngineException e) {
          throw e;
        } catch (RuntimeException e) {
          LogManager.instance().error(this, "Error Dropping Index %s", e, getName());
          // Just log errors of removing keys while dropping and keep dropping
        }

        try {
          try (Stream<RID> stream = getRids(session, null)) {
            stream.forEach((rid) -> remove(session, null, rid));
          }
        } catch (IndexEngineException e) {
          throw e;
        } catch (RuntimeException e) {
          LogManager.instance().error(this, "Error Dropping Index %s", e, getName());
          // Just log errors of removing keys while dropping and keep dropping
        }

        storage.deleteIndexEngine(indexId);
        break;
      } catch (InvalidIndexEngineIdException ignore) {
        doReloadIndexEngine();
      }
    }

    removeValuesContainer();
  }

  public String getName() {
    return im.getName();
  }

  public String getType() {
    return im.getType();
  }

  @Override
  public String getAlgorithm() {
    acquireSharedLock();
    try {
      return im.getAlgorithm();
    } finally {
      releaseSharedLock();
    }
  }

  @Override
  public String toString() {
    acquireSharedLock();
    try {
      return im.getName();
    } finally {
      releaseSharedLock();
    }
  }

  public IndexInternal getInternal() {
    return this;
  }

  public Set<String> getClusters() {
    acquireSharedLock();
    try {
      return Collections.unmodifiableSet(clustersToIndex);
    } finally {
      releaseSharedLock();
    }
  }

  public IndexAbstract addCluster(DatabaseSessionInternal session, final String clusterName) {
    acquireExclusiveLock();
    try {
      if (clustersToIndex.add(clusterName)) {
        // INDEX SINGLE CLUSTER
        indexCluster(session, clusterName, null, 0, 0, 0);
      }

      return this;
    } finally {
      releaseExclusiveLock();
    }
  }

  public void removeCluster(DatabaseSessionInternal session, String iClusterName) {
    acquireExclusiveLock();
    try {
      if (clustersToIndex.remove(iClusterName)) {
        rebuild(session);
      }

    } finally {
      releaseExclusiveLock();
    }
  }

  @Override
  public int getVersion() {
    return im.getVersion();
  }

  public EntityImpl updateConfiguration(DatabaseSessionInternal session) {
    EntityImpl entity = new EntityImpl(session);
    entity.field(CONFIG_TYPE, im.getType());
    entity.field(CONFIG_NAME, im.getName());
    entity.field(INDEX_VERSION, im.getVersion());

    if (im.getIndexDefinition() != null) {

      final EntityImpl indexDefEntity = im.getIndexDefinition()
          .toStream(new EntityImpl(session));
      if (!indexDefEntity.hasOwners()) {
        EntityInternalUtils.addOwner(indexDefEntity, entity);
      }

      entity.field(INDEX_DEFINITION, indexDefEntity, PropertyType.EMBEDDED);
      entity.field(
          INDEX_DEFINITION_CLASS, im.getIndexDefinition().getClass().getName());
    } else {
      entity.removeField(INDEX_DEFINITION);
      entity.removeField(INDEX_DEFINITION_CLASS);
    }

    entity.field(CONFIG_CLUSTERS, clustersToIndex, PropertyType.EMBEDDEDSET);
    entity.field(ALGORITHM, im.getAlgorithm());
    entity.field(VALUE_CONTAINER_ALGORITHM, im.getValueContainerAlgorithm());

    if (im.getMetadata() != null) {
      var imEntity = new EntityImpl();
      imEntity.fromMap(im.getMetadata());
      entity.field(METADATA, imEntity, PropertyType.EMBEDDED);
    }

    return entity;
  }

  /**
   * Interprets transaction index changes for a certain key. Override it to customize index
   * behaviour on interpreting index changes. This may be viewed as an optimization, but in some
   * cases this is a requirement. For example, if you put multiple values under the same key during
   * the transaction for single-valued/unique index, but remove all of them except one before
   * commit, there is no point in throwing {@link RecordDuplicatedException} while applying index
   * changes.
   *
   * @param changes the changes to interpret.
   * @return the interpreted index key changes.
   */
  public Iterable<TransactionIndexEntry> interpretTxKeyChanges(
      FrontendTransactionIndexChangesPerKey changes) {
    return changes.getEntriesAsList();
  }

  public EntityImpl getConfiguration(DatabaseSessionInternal session) {
    return updateConfiguration(session);
  }

  @Override
  public Map<String, ?> getMetadata() {
    return im.getMetadata();
  }

  @Override
  public boolean isUnique() {
    return false;
  }

  public boolean isAutomatic() {
    acquireSharedLock();
    try {
      return im.getIndexDefinition() != null && im.getIndexDefinition().getClassName() != null;
    } finally {
      releaseSharedLock();
    }
  }

  public PropertyType[] getKeyTypes() {
    acquireSharedLock();
    try {
      if (im.getIndexDefinition() == null) {
        return null;
      }

      return im.getIndexDefinition().getTypes();
    } finally {
      releaseSharedLock();
    }
  }

  @Override
  public Stream<Object> keyStream() {
    acquireSharedLock();
    try {
      while (true) {
        try {
          return storage.getIndexKeyStream(indexId);
        } catch (InvalidIndexEngineIdException ignore) {
          doReloadIndexEngine();
        }
      }
    } finally {
      releaseSharedLock();
    }
  }

  public IndexDefinition getDefinition() {
    return im.getIndexDefinition();
  }

  @Override
  public boolean equals(final Object o) {
    acquireSharedLock();
    try {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      final IndexAbstract that = (IndexAbstract) o;

      return im.getName().equals(that.im.getName());
    } finally {
      releaseSharedLock();
    }
  }

  @Override
  public int hashCode() {
    acquireSharedLock();
    try {
      return im.getName().hashCode();
    } finally {
      releaseSharedLock();
    }
  }

  public int getIndexId() {
    return indexId;
  }

  public String getDatabaseName() {
    return storage.getName();
  }

  public Object getCollatingValue(final Object key) {
    if (key != null && im.getIndexDefinition() != null) {
      return im.getIndexDefinition().getCollate().transform(key);
    }
    return key;
  }

  @Override
  public int compareTo(Index index) {
    acquireSharedLock();
    try {
      final String name = index.getName();
      return this.im.getName().compareTo(name);
    } finally {
      releaseSharedLock();
    }
  }

  @Override
  public String getIndexNameByKey(final Object key) {
    BaseIndexEngine engine;

    while (true) {
      try {
        engine = storage.getIndexEngine(indexId);
        break;
      } catch (InvalidIndexEngineIdException ignore) {
        doReloadIndexEngine();
      }
    }
    return engine.getIndexNameByKey(key);
  }

  @Override
  public boolean acquireAtomicExclusiveLock(Object key) {
    BaseIndexEngine engine;

    while (true) {
      try {
        engine = storage.getIndexEngine(indexId);
        break;
      } catch (InvalidIndexEngineIdException ignore) {
        doReloadIndexEngine();
      }
    }

    return engine.acquireAtomicExclusiveLock(key);
  }

  private long[] indexCluster(
      DatabaseSessionInternal session, final String clusterName,
      final ProgressListener iProgressListener,
      long documentNum,
      long documentIndexed,
      long documentTotal) {
    if (im.getIndexDefinition() == null) {
      throw new ConfigurationException(
          "Index '"
              + im.getName()
              + "' cannot be rebuilt because has no a valid definition ("
              + im.getIndexDefinition()
              + ")");
    }

    var stat = new long[]{documentNum, documentIndexed};

    var clusterIterator = session.browseCluster(clusterName);
    session.executeInTxBatches((Iterator<DBRecord>) clusterIterator, (db, record) -> {
      if (Thread.interrupted()) {
        throw new CommandExecutionException("The index rebuild has been interrupted");
      }

      if (record instanceof EntityImpl entity) {
        ClassIndexManager.reIndex(session, entity, this);
        ++stat[1];
      }

      stat[0]++;

      if (iProgressListener != null) {
        iProgressListener.onProgress(
            this, documentNum, (float) (documentNum * 100.0 / documentTotal));
      }
    });

    return stat;
  }

  protected void releaseExclusiveLock() {
    rwLock.writeLock().unlock();
  }

  protected void acquireExclusiveLock() {
    rwLock.writeLock().lock();
  }

  protected void releaseSharedLock() {
    rwLock.readLock().unlock();
  }

  protected void acquireSharedLock() {
    rwLock.readLock().lock();
  }

  private void removeValuesContainer() {
    if (im.getAlgorithm().equals(DefaultIndexFactory.SBTREE_BONSAI_VALUE_CONTAINER)) {

      final AtomicOperation atomicOperation =
          storage.getAtomicOperationsManager().getCurrentOperation();

      final ReadCache readCache = storage.getReadCache();
      final WriteCache writeCache = storage.getWriteCache();

      if (atomicOperation == null) {
        try {
          final String fileName = im.getName() + IndexRIDContainer.INDEX_FILE_EXTENSION;
          if (writeCache.exists(fileName)) {
            final long fileId = writeCache.loadFile(fileName);
            readCache.deleteFile(fileId, writeCache);
          }
        } catch (IOException e) {
          LogManager.instance().error(this, "Cannot delete file for value containers", e);
        }
      } else {
        try {
          final String fileName = im.getName() + IndexRIDContainer.INDEX_FILE_EXTENSION;
          if (atomicOperation.isFileExists(fileName)) {
            final long fileId = atomicOperation.loadFile(fileName);
            atomicOperation.deleteFile(fileId);
          }
        } catch (IOException e) {
          LogManager.instance().error(this, "Cannot delete file for value containers", e);
        }
      }
    }
  }

  protected void onIndexEngineChange(final int indexId) {
    while (true) {
      try {
        storage.callIndexEngine(
            false,
            indexId,
            engine -> {
              engine.init(im);
              return null;
            });
        break;
      } catch (InvalidIndexEngineIdException ignore) {
        doReloadIndexEngine();
      }
    }
  }

  public static void manualIndexesWarning() {
    if (!GlobalConfiguration.INDEX_ALLOW_MANUAL_INDEXES.getValueAsBoolean()) {
      throw new ManualIndexesAreProhibited(
          "Manual indexes are deprecated, not supported any more and will be removed in next"
              + " versions if you still want to use them, please set global property `"
              + GlobalConfiguration.INDEX_ALLOW_MANUAL_INDEXES.getKey()
              + "` to `true`");
    }

    if (GlobalConfiguration.INDEX_ALLOW_MANUAL_INDEXES_WARNING.getValueAsBoolean()) {
      LogManager.instance()
          .warn(
              IndexAbstract.class,
              "Seems you use manual indexes. Manual indexes are deprecated, not supported any more"
                  + " and will be removed in next versions if you do not want to see warning,"
                  + " please set global property `"
                  + GlobalConfiguration.INDEX_ALLOW_MANUAL_INDEXES_WARNING.getKey()
                  + "` to `false`");
    }
  }

  /**
   * Indicates search behavior in case of {@link CompositeKey} keys that have less amount of
   * internal keys are used, whether lowest or highest partially matched key should be used. Such
   * keys is allowed to use only in
   */
  public enum PartialSearchMode {
    /**
     * Any partially matched key will be used as search result.
     */
    NONE,
    /**
     * The biggest partially matched key will be used as search result.
     */
    HIGHEST_BOUNDARY,

    /**
     * The smallest partially matched key will be used as search result.
     */
    LOWEST_BOUNDARY
  }

  private static Object enhanceCompositeKey(
      Object key, PartialSearchMode partialSearchMode, IndexDefinition definition) {
    if (!(key instanceof CompositeKey compositeKey)) {
      return key;
    }

    final int keySize = definition.getParamCount();

    if (!(keySize == 1
        || compositeKey.getKeys().size() == keySize
        || partialSearchMode.equals(PartialSearchMode.NONE))) {
      final CompositeKey fullKey = new CompositeKey(compositeKey);
      int itemsToAdd = keySize - fullKey.getKeys().size();

      final Comparable<?> keyItem;
      if (partialSearchMode.equals(PartialSearchMode.HIGHEST_BOUNDARY)) {
        keyItem = ALWAYS_GREATER_KEY;
      } else {
        keyItem = ALWAYS_LESS_KEY;
      }

      for (int i = 0; i < itemsToAdd; i++) {
        fullKey.addKey(keyItem);
      }

      return fullKey;
    }

    return key;
  }

  public Object enhanceToCompositeKeyBetweenAsc(Object keyTo, boolean toInclusive) {
    PartialSearchMode partialSearchModeTo;
    if (toInclusive) {
      partialSearchModeTo = PartialSearchMode.HIGHEST_BOUNDARY;
    } else {
      partialSearchModeTo = PartialSearchMode.LOWEST_BOUNDARY;
    }

    keyTo = enhanceCompositeKey(keyTo, partialSearchModeTo, getDefinition());
    return keyTo;
  }

  public Object enhanceFromCompositeKeyBetweenAsc(Object keyFrom, boolean fromInclusive) {
    PartialSearchMode partialSearchModeFrom;
    if (fromInclusive) {
      partialSearchModeFrom = PartialSearchMode.LOWEST_BOUNDARY;
    } else {
      partialSearchModeFrom = PartialSearchMode.HIGHEST_BOUNDARY;
    }

    keyFrom = enhanceCompositeKey(keyFrom, partialSearchModeFrom, getDefinition());
    return keyFrom;
  }

  public Object enhanceToCompositeKeyBetweenDesc(Object keyTo, boolean toInclusive) {
    PartialSearchMode partialSearchModeTo;
    if (toInclusive) {
      partialSearchModeTo = PartialSearchMode.HIGHEST_BOUNDARY;
    } else {
      partialSearchModeTo = PartialSearchMode.LOWEST_BOUNDARY;
    }

    keyTo = enhanceCompositeKey(keyTo, partialSearchModeTo, getDefinition());
    return keyTo;
  }

  public Object enhanceFromCompositeKeyBetweenDesc(Object keyFrom, boolean fromInclusive) {
    PartialSearchMode partialSearchModeFrom;
    if (fromInclusive) {
      partialSearchModeFrom = PartialSearchMode.LOWEST_BOUNDARY;
    } else {
      partialSearchModeFrom = PartialSearchMode.HIGHEST_BOUNDARY;
    }

    keyFrom = enhanceCompositeKey(keyFrom, partialSearchModeFrom, getDefinition());
    return keyFrom;
  }
}
