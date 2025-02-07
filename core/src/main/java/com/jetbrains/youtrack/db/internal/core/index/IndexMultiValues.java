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

import com.jetbrains.youtrack.db.api.record.DBRecord;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.api.record.RID;
import com.jetbrains.youtrack.db.internal.common.comparator.DefaultComparator;
import com.jetbrains.youtrack.db.internal.common.stream.Streams;
import com.jetbrains.youtrack.db.internal.common.types.ModifiableBoolean;
import com.jetbrains.youtrack.db.internal.common.util.RawPair;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.exception.InvalidIndexEngineIdException;
import com.jetbrains.youtrack.db.internal.core.id.RecordId;
import com.jetbrains.youtrack.db.internal.core.index.comparator.AscComparator;
import com.jetbrains.youtrack.db.internal.core.index.comparator.DescComparator;
import com.jetbrains.youtrack.db.internal.core.index.iterator.PureTxMultiValueBetweenIndexBackwardSplititerator;
import com.jetbrains.youtrack.db.internal.core.index.iterator.PureTxMultiValueBetweenIndexForwardSpliterator;
import com.jetbrains.youtrack.db.internal.core.index.multivalue.MultiValuesTransformer;
import com.jetbrains.youtrack.db.internal.core.index.multivalue.MultivalueEntityRemover;
import com.jetbrains.youtrack.db.internal.core.index.multivalue.MultivalueIndexKeyUpdaterImpl;
import com.jetbrains.youtrack.db.internal.core.storage.Storage;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.AbstractPaginatedStorage;
import com.jetbrains.youtrack.db.internal.core.tx.FrontendTransaction;
import com.jetbrains.youtrack.db.internal.core.tx.FrontendTransactionIndexChanges;
import com.jetbrains.youtrack.db.internal.core.tx.FrontendTransactionIndexChanges.OPERATION;
import com.jetbrains.youtrack.db.internal.core.tx.FrontendTransactionIndexChangesPerKey;
import com.jetbrains.youtrack.db.internal.core.tx.FrontendTransactionIndexChangesPerKey.TransactionIndexEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Abstract index implementation that supports multi-values for the same key.
 */
public abstract class IndexMultiValues extends IndexAbstract {

  IndexMultiValues(IndexMetadata im, final Storage storage) {
    super(im, storage);
  }

  @Deprecated
  @Override
  public Collection<RID> get(DatabaseSessionInternal session, Object key) {
    final List<RID> rids;
    try (Stream<RID> stream = getRids(session, key)) {
      rids = stream.collect(Collectors.toList());
    }
    return rids;
  }

  @Override
  public Stream<RID> getRidsIgnoreTx(DatabaseSessionInternal session, Object key) {
    final Object collatedKey = getCollatingValue(key);
    Stream<RID> backedStream;
    acquireSharedLock();
    try {
      Stream<RID> stream;
      while (true) {
        try {
          if (apiVersion == 0) {
            //noinspection unchecked
            final Collection<RID> values =
                (Collection<RID>) storage.getIndexValue(session, indexId, collatedKey);
            if (values != null) {
              //noinspection resource
              stream = values.stream();
            } else {
              //noinspection resource
              stream = Stream.empty();
            }
          } else if (apiVersion == 1) {
            //noinspection resource
            stream = storage.getIndexValues(indexId, collatedKey);
          } else {
            throw new IllegalStateException("Invalid version of index API - " + apiVersion);
          }
          backedStream = IndexStreamSecurityDecorator.decorateRidStream(this, stream);
          break;
        } catch (InvalidIndexEngineIdException ignore) {
          doReloadIndexEngine();
        }
      }
    } finally {
      releaseSharedLock();
    }
    return backedStream;
  }

  @Override
  public Stream<RID> getRids(DatabaseSessionInternal session, Object key) {
    final Object collatedKey = getCollatingValue(key);
    Stream<RID> backedStream = getRidsIgnoreTx(session, key);
    final FrontendTransactionIndexChanges indexChanges =
        session.getTransaction().getIndexChangesInternal(getName());
    if (indexChanges == null) {
      return backedStream;
    }
    Set<Identifiable> txChanges = calculateTxValue(collatedKey, indexChanges);
    if (txChanges == null) {
      txChanges = Collections.emptySet();
    }
    return IndexStreamSecurityDecorator.decorateRidStream(
        this,
        Stream.concat(
            backedStream
                .map((rid) -> calculateTxIndexEntry(collatedKey, rid, indexChanges))
                .filter(Objects::nonNull)
                .map((pair) -> pair.second),
            txChanges.stream().map(Identifiable::getIdentity)));
  }

  public IndexMultiValues put(DatabaseSessionInternal session, Object key,
      final Identifiable singleValue) {
    final RecordId rid = (RecordId) singleValue.getIdentity();

    if (!rid.isValid()) {
      if (singleValue instanceof DBRecord) {
        // EARLY SAVE IT
        ((DBRecord) singleValue).save();
      } else {
        throw new IllegalArgumentException(
            "Cannot store non persistent RID as index value for key '" + key + "'");
      }
    }

    key = getCollatingValue(key);

    FrontendTransaction singleTx = session.getTransaction();
    singleTx.addIndexEntry(
        this, super.getName(), FrontendTransactionIndexChanges.OPERATION.PUT, key, singleValue);
    return this;
  }

  @Override
  public void doPut(DatabaseSessionInternal session, AbstractPaginatedStorage storage,
      Object key,
      RID rid)
      throws InvalidIndexEngineIdException {
    if (apiVersion == 0) {
      doPutV0(session, indexId, storage, im.getValueContainerAlgorithm(), getName(), key, rid);
    } else if (apiVersion == 1) {
      doPutV1(storage, indexId, key, rid);
    } else {
      throw new IllegalStateException("Invalid API version, " + apiVersion);
    }
  }

  @Override
  public boolean isNativeTxSupported() {
    return true;
  }

  private static void doPutV0(
      DatabaseSessionInternal session, final int indexId,
      final AbstractPaginatedStorage storage,
      String valueContainerAlgorithm,
      String indexName,
      Object key,
      RID identity)
      throws InvalidIndexEngineIdException {
    int binaryFormatVersion = storage.getConfiguration().getBinaryFormatVersion();
    final IndexKeyUpdater<Object> creator =
        new MultivalueIndexKeyUpdaterImpl(
            identity, valueContainerAlgorithm, binaryFormatVersion, indexName);

    storage.updateIndexEntry(session, indexId, key, creator);
  }

  private static void doPutV1(
      AbstractPaginatedStorage storage, int indexId, Object key, RID identity)
      throws InvalidIndexEngineIdException {
    storage.putRidIndexEntry(indexId, key, identity);
  }

  @Override
  public boolean remove(DatabaseSessionInternal session, Object key, final Identifiable value) {
    key = getCollatingValue(key);
    session.getTransaction().addIndexEntry(this, super.getName(), OPERATION.REMOVE, key, value);
    return true;
  }

  @Override
  public boolean doRemove(DatabaseSessionInternal session, AbstractPaginatedStorage storage,
      Object key, RID rid)
      throws InvalidIndexEngineIdException {
    if (apiVersion == 0) {
      return doRemoveV0(session, indexId, storage, key, rid);
    }

    if (apiVersion == 1) {
      return doRemoveV1(indexId, storage, key, rid);
    }

    throw new IllegalStateException("Invalid API version, " + apiVersion);
  }

  private static boolean doRemoveV0(
      DatabaseSessionInternal session, int indexId, AbstractPaginatedStorage storage, Object key,
      Identifiable value)
      throws InvalidIndexEngineIdException {
    Set<Identifiable> values;
    //noinspection unchecked
    values = (Set<Identifiable>) storage.getIndexValue(session, indexId, key);

    if (values == null) {
      return false;
    }

    final ModifiableBoolean removed = new ModifiableBoolean(false);
    final IndexKeyUpdater<Object> creator = new MultivalueEntityRemover(value, removed);
    storage.updateIndexEntry(session, indexId, key, creator);

    return removed.getValue();
  }

  private static boolean doRemoveV1(
      int indexId, AbstractPaginatedStorage storage, Object key, Identifiable value)
      throws InvalidIndexEngineIdException {
    return storage.removeRidIndexEntry(indexId, key, value.getIdentity());
  }

  @Override
  public Stream<RawPair<Object, RID>> streamEntriesBetween(
      DatabaseSessionInternal session, Object fromKey, boolean fromInclusive, Object toKey,
      boolean toInclusive, boolean ascOrder) {
    fromKey = getCollatingValue(fromKey);
    toKey = getCollatingValue(toKey);
    Stream<RawPair<Object, RID>> stream;
    acquireSharedLock();
    try {
      while (true) {
        try {
          stream =
              IndexStreamSecurityDecorator.decorateStream(
                  this,
                  storage.iterateIndexEntriesBetween(session,
                      indexId,
                      fromKey,
                      fromInclusive,
                      toKey,
                      toInclusive,
                      ascOrder, MultiValuesTransformer.INSTANCE));
          break;
        } catch (InvalidIndexEngineIdException ignore) {
          doReloadIndexEngine();
        }
      }
    } finally {
      releaseSharedLock();
    }

    final FrontendTransactionIndexChanges indexChanges =
        session.getTransaction().getIndexChangesInternal(getName());
    if (indexChanges == null) {
      return stream;
    }

    final Stream<RawPair<Object, RID>> txStream;
    if (ascOrder) {
      txStream =
          StreamSupport.stream(
              new PureTxMultiValueBetweenIndexForwardSpliterator(
                  this, fromKey, fromInclusive, toKey, toInclusive, indexChanges),
              false);
    } else {
      txStream =
          StreamSupport.stream(
              new PureTxMultiValueBetweenIndexBackwardSplititerator(
                  this, fromKey, fromInclusive, toKey, toInclusive, indexChanges),
              false);
    }

    if (indexChanges.cleared) {
      return IndexStreamSecurityDecorator.decorateStream(this, txStream);
    }

    return IndexStreamSecurityDecorator.decorateStream(
        this, mergeTxAndBackedStreams(indexChanges, txStream, stream, ascOrder));
  }

  @Override
  public Stream<RawPair<Object, RID>> streamEntriesMajor(
      DatabaseSessionInternal session, Object fromKey, boolean fromInclusive, boolean ascOrder) {
    fromKey = getCollatingValue(fromKey);
    Stream<RawPair<Object, RID>> stream;
    acquireSharedLock();
    try {
      while (true) {
        try {
          stream =
              IndexStreamSecurityDecorator.decorateStream(
                  this,
                  storage.iterateIndexEntriesMajor(
                      indexId, fromKey, fromInclusive, ascOrder, MultiValuesTransformer.INSTANCE));
          break;
        } catch (InvalidIndexEngineIdException ignore) {
          doReloadIndexEngine();
        }
      }
    } finally {
      releaseSharedLock();
    }

    final FrontendTransactionIndexChanges indexChanges =
        session.getTransaction().getIndexChangesInternal(getName());
    if (indexChanges == null) {
      return stream;
    }

    final Stream<RawPair<Object, RID>> txStream;

    final Object lastKey = indexChanges.getLastKey();
    if (ascOrder) {
      txStream =
          StreamSupport.stream(
              new PureTxMultiValueBetweenIndexForwardSpliterator(
                  this, fromKey, fromInclusive, lastKey, true, indexChanges),
              false);
    } else {
      txStream =
          StreamSupport.stream(
              new PureTxMultiValueBetweenIndexBackwardSplititerator(
                  this, fromKey, fromInclusive, lastKey, true, indexChanges),
              false);
    }

    if (indexChanges.cleared) {
      return IndexStreamSecurityDecorator.decorateStream(this, txStream);
    }

    return IndexStreamSecurityDecorator.decorateStream(
        this, mergeTxAndBackedStreams(indexChanges, txStream, stream, ascOrder));
  }

  @Override
  public Stream<RawPair<Object, RID>> streamEntriesMinor(
      DatabaseSessionInternal session, Object toKey, boolean toInclusive, boolean ascOrder) {
    toKey = getCollatingValue(toKey);
    Stream<RawPair<Object, RID>> stream;

    acquireSharedLock();
    try {
      while (true) {
        try {
          stream =
              IndexStreamSecurityDecorator.decorateStream(
                  this,
                  storage.iterateIndexEntriesMinor(
                      indexId, toKey, toInclusive, ascOrder, MultiValuesTransformer.INSTANCE));
          break;
        } catch (InvalidIndexEngineIdException ignore) {
          doReloadIndexEngine();
        }
      }
    } finally {
      releaseSharedLock();
    }

    final FrontendTransactionIndexChanges indexChanges =
        session.getTransaction().getIndexChangesInternal(getName());
    if (indexChanges == null) {
      return stream;
    }

    final Stream<RawPair<Object, RID>> txStream;

    final Object firstKey = indexChanges.getFirstKey();
    if (ascOrder) {
      txStream =
          StreamSupport.stream(
              new PureTxMultiValueBetweenIndexForwardSpliterator(
                  this, firstKey, true, toKey, toInclusive, indexChanges),
              false);
    } else {
      txStream =
          StreamSupport.stream(
              new PureTxMultiValueBetweenIndexBackwardSplititerator(
                  this, firstKey, true, toKey, toInclusive, indexChanges),
              false);
    }

    if (indexChanges.cleared) {
      return IndexStreamSecurityDecorator.decorateStream(this, txStream);
    }

    return IndexStreamSecurityDecorator.decorateStream(
        this, mergeTxAndBackedStreams(indexChanges, txStream, stream, ascOrder));
  }

  @Override
  public Stream<RawPair<Object, RID>> streamEntries(DatabaseSessionInternal session,
      Collection<?> keys, boolean ascSortOrder) {
    final List<Object> sortedKeys = new ArrayList<>(keys);
    final Comparator<Object> comparator;
    if (ascSortOrder) {
      comparator = DefaultComparator.INSTANCE;
    } else {
      comparator = Collections.reverseOrder(DefaultComparator.INSTANCE);
    }

    sortedKeys.sort(comparator);

    Stream<RawPair<Object, RID>> stream =
        IndexStreamSecurityDecorator.decorateStream(
            this, sortedKeys.stream().flatMap(key1 -> streamForKey(session, key1)));

    final FrontendTransactionIndexChanges indexChanges = session.getTransaction()
        .getIndexChangesInternal(getName());
    if (indexChanges == null) {
      return stream;
    }

    Comparator<RawPair<Object, RID>> keyComparator;
    if (ascSortOrder) {
      keyComparator = AscComparator.INSTANCE;
    } else {
      keyComparator = DescComparator.INSTANCE;
    }

    final Stream<RawPair<Object, RID>> txStream =
        keys.stream()
            .flatMap((key) -> txStramForKey(indexChanges, key))
            .filter(Objects::nonNull)
            .sorted(keyComparator);

    if (indexChanges.cleared) {
      return IndexStreamSecurityDecorator.decorateStream(this, txStream);
    }

    return IndexStreamSecurityDecorator.decorateStream(
        this, mergeTxAndBackedStreams(indexChanges, txStream, stream, ascSortOrder));
  }

  private Stream<RawPair<Object, RID>> txStramForKey(
      final FrontendTransactionIndexChanges indexChanges, Object key) {
    final Set<Identifiable> result = calculateTxValue(getCollatingValue(key), indexChanges);
    if (result != null) {
      return result.stream()
          .map((rid) -> new RawPair<>(getCollatingValue(key), rid.getIdentity()));
    }
    return null;
  }

  private Stream<RawPair<Object, RID>> streamForKey(DatabaseSessionInternal session,
      Object key) {
    key = getCollatingValue(key);

    final Object entryKey = key;
    acquireSharedLock();
    try {
      while (true) {
        try {
          if (apiVersion == 0) {
            //noinspection unchecked,resource
            return Optional.ofNullable(
                    (Collection<RID>) storage.getIndexValue(session, indexId, key))
                .map((rids) -> rids.stream().map((rid) -> new RawPair<>(entryKey, rid)))
                .orElse(Stream.empty());
          } else if (apiVersion == 1) {
            //noinspection resource
            return storage.getIndexValues(indexId, key).map((rid) -> new RawPair<>(entryKey, rid));
          } else {
            throw new IllegalStateException("Invalid version of index API - " + apiVersion);
          }
        } catch (InvalidIndexEngineIdException ignore) {
          doReloadIndexEngine();
        }
      }

    } finally {
      releaseSharedLock();
    }
  }

  public static Set<Identifiable> calculateTxValue(
      final Object key, FrontendTransactionIndexChanges indexChanges) {
    final List<Identifiable> result = new ArrayList<>();
    final FrontendTransactionIndexChangesPerKey changesPerKey = indexChanges.getChangesPerKey(key);
    if (changesPerKey.isEmpty()) {
      return null;
    }

    for (TransactionIndexEntry entry : changesPerKey.getEntriesAsList()) {
      if (entry.getOperation() == OPERATION.REMOVE) {
        if (entry.getValue() == null) {
          result.clear();
        } else {
          result.remove(entry.getValue());
        }
      } else {
        result.add(entry.getValue());
      }
    }

    if (result.isEmpty()) {
      return null;
    }

    return new HashSet<>(result);
  }

  public long size(DatabaseSessionInternal session) {
    acquireSharedLock();
    long tot;
    try {
      while (true) {
        try {
          tot = storage.getIndexSize(indexId, MultiValuesTransformer.INSTANCE);
          break;
        } catch (InvalidIndexEngineIdException ignore) {
          doReloadIndexEngine();
        }
      }
    } finally {
      releaseSharedLock();
    }

    final FrontendTransactionIndexChanges indexChanges =
        session.getTransaction().getIndexChanges(getName());
    if (indexChanges != null) {
      try (Stream<RawPair<Object, RID>> stream = stream(session)) {
        return stream.count();
      }
    }

    return tot;
  }

  @Override
  public Stream<RawPair<Object, RID>> stream(DatabaseSessionInternal session) {
    Stream<RawPair<Object, RID>> stream;
    acquireSharedLock();
    try {
      while (true) {
        try {
          stream =
              IndexStreamSecurityDecorator.decorateStream(
                  this, storage.getIndexStream(indexId, MultiValuesTransformer.INSTANCE));
          break;
        } catch (InvalidIndexEngineIdException ignore) {
          doReloadIndexEngine();
        }
      }

    } finally {
      releaseSharedLock();
    }

    final FrontendTransactionIndexChanges indexChanges =
        session.getTransaction().getIndexChangesInternal(getName());
    if (indexChanges == null) {
      return stream;
    }

    final Stream<RawPair<Object, RID>> txStream =
        StreamSupport.stream(
            new PureTxMultiValueBetweenIndexForwardSpliterator(
                this, null, true, null, true, indexChanges),
            false);

    if (indexChanges.cleared) {
      return IndexStreamSecurityDecorator.decorateStream(this, txStream);
    }

    return IndexStreamSecurityDecorator.decorateStream(
        this, mergeTxAndBackedStreams(indexChanges, txStream, stream, true));
  }

  private Stream<RawPair<Object, RID>> mergeTxAndBackedStreams(
      FrontendTransactionIndexChanges indexChanges,
      Stream<RawPair<Object, RID>> txStream,
      Stream<RawPair<Object, RID>> backedStream,
      boolean ascOrder) {
    Comparator<RawPair<Object, RID>> keyComparator;
    if (ascOrder) {
      keyComparator = AscComparator.INSTANCE;
    } else {
      keyComparator = DescComparator.INSTANCE;
    }
    return Streams.mergeSortedSpliterators(
        txStream,
        backedStream
            .map((entry) -> calculateTxIndexEntry(entry.first, entry.second, indexChanges))
            .filter(Objects::nonNull),
        keyComparator);
  }

  private RawPair<Object, RID> calculateTxIndexEntry(
      Object key, final RID backendValue, FrontendTransactionIndexChanges indexChanges) {
    key = getCollatingValue(key);
    final FrontendTransactionIndexChangesPerKey changesPerKey = indexChanges.getChangesPerKey(key);
    if (changesPerKey.isEmpty()) {
      return new RawPair<>(key, backendValue);
    }

    int putCounter = 1;
    for (TransactionIndexEntry entry : changesPerKey.getEntriesAsList()) {
      if (entry.getOperation() == OPERATION.PUT && entry.getValue().equals(backendValue)) {
        putCounter++;
      } else if (entry.getOperation() == OPERATION.REMOVE) {
        if (entry.getValue() == null) {
          putCounter = 0;
        } else if (entry.getValue().equals(backendValue) && putCounter > 0) {
          putCounter--;
        }
      }
    }

    if (putCounter <= 0) {
      return null;
    }

    return new RawPair<>(key, backendValue);
  }

  @Override
  public Stream<RawPair<Object, RID>> descStream(DatabaseSessionInternal session) {
    Stream<RawPair<Object, RID>> stream;
    acquireSharedLock();
    try {
      while (true) {
        try {
          stream =
              IndexStreamSecurityDecorator.decorateStream(
                  this, storage.getIndexDescStream(indexId, MultiValuesTransformer.INSTANCE));
          break;
        } catch (InvalidIndexEngineIdException ignore) {
          doReloadIndexEngine();
        }
      }
    } finally {
      releaseSharedLock();
    }

    final FrontendTransactionIndexChanges indexChanges =
        session.getTransaction().getIndexChangesInternal(getName());
    if (indexChanges == null) {
      return stream;
    }

    final Stream<RawPair<Object, RID>> txStream =
        StreamSupport.stream(
            new PureTxMultiValueBetweenIndexBackwardSplititerator(
                this, null, true, null, true, indexChanges),
            false);

    if (indexChanges.cleared) {
      return IndexStreamSecurityDecorator.decorateStream(this, txStream);
    }

    return IndexStreamSecurityDecorator.decorateStream(
        this, mergeTxAndBackedStreams(indexChanges, txStream, stream, false));
  }
}
