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

package com.orientechnologies.orient.core.storage.index.engine;

import com.orientechnologies.common.exception.YTException;
import com.orientechnologies.common.serialization.types.OBinarySerializer;
import com.orientechnologies.common.util.ORawPair;
import com.orientechnologies.orient.core.config.IndexEngineData;
import com.orientechnologies.orient.core.db.YTDatabaseSessionInternal;
import com.orientechnologies.orient.core.encryption.OEncryption;
import com.orientechnologies.orient.core.id.YTRID;
import com.orientechnologies.orient.core.index.YTIndexException;
import com.orientechnologies.orient.core.index.OIndexKeyUpdater;
import com.orientechnologies.orient.core.index.OIndexMetadata;
import com.orientechnologies.orient.core.index.engine.IndexEngineValidator;
import com.orientechnologies.orient.core.index.engine.IndexEngineValuesTransformer;
import com.orientechnologies.orient.core.index.engine.OIndexEngine;
import com.orientechnologies.orient.core.storage.impl.local.OAbstractPaginatedStorage;
import com.orientechnologies.orient.core.storage.impl.local.paginated.atomicoperations.OAtomicOperation;
import com.orientechnologies.orient.core.storage.index.sbtree.local.OSBTree;
import com.orientechnologies.orient.core.storage.index.sbtree.local.v1.OSBTreeV1;
import com.orientechnologies.orient.core.storage.index.sbtree.local.v2.OSBTreeV2;
import com.orientechnologies.orient.core.storage.index.versionmap.OVersionPositionMap;
import com.orientechnologies.orient.core.storage.index.versionmap.OVersionPositionMapV0;
import java.io.IOException;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @since 8/30/13
 */
public class OSBTreeIndexEngine implements OIndexEngine {

  public static final int VERSION = 2;

  public static final String DATA_FILE_EXTENSION = ".sbt";
  public static final String NULL_BUCKET_FILE_EXTENSION = ".nbt";

  private final OSBTree<Object, Object> sbTree;
  private final OVersionPositionMap versionPositionMap;

  private final String name;
  private final int id;

  private final OAbstractPaginatedStorage storage;

  public OSBTreeIndexEngine(
      final int id, String name, OAbstractPaginatedStorage storage, int version) {
    this.id = id;
    this.name = name;
    this.storage = storage;

    if (version == 1) {
      sbTree = new OSBTreeV1<>(name, DATA_FILE_EXTENSION, NULL_BUCKET_FILE_EXTENSION, storage);
    } else if (version == 2) {
      sbTree = new OSBTreeV2<>(name, DATA_FILE_EXTENSION, NULL_BUCKET_FILE_EXTENSION, storage);
    } else {
      throw new IllegalStateException("Invalid version of index, version = " + version);
    }
    versionPositionMap =
        new OVersionPositionMapV0(
            storage, name, name + DATA_FILE_EXTENSION, OVersionPositionMap.DEF_EXTENSION);
  }

  @Override
  public int getId() {
    return id;
  }

  @Override
  public void init(OIndexMetadata metadata) {
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void flush() {
  }

  public void create(OAtomicOperation atomicOperation, IndexEngineData data) throws IOException {

    OBinarySerializer valueSerializer =
        storage.resolveObjectSerializer(data.getValueSerializerId());
    OBinarySerializer keySerializer = storage.resolveObjectSerializer(data.getKeySerializedId());

    final OEncryption encryption =
        OAbstractPaginatedStorage.loadEncryption(data.getEncryption(), data.getEncryptionOptions());

    try {
      sbTree.create(
          atomicOperation,
          keySerializer,
          valueSerializer,
          data.getKeyTypes(),
          data.getKeySize(),
          data.isNullValuesSupport(),
          encryption);
      versionPositionMap.create(atomicOperation);
    } catch (IOException e) {
      throw YTException.wrapException(
          new YTIndexException("Error during creation of index " + name), e);
    }
  }

  @Override
  public void delete(OAtomicOperation atomicOperation) {
    try {
      doClearTree(atomicOperation);

      sbTree.delete(atomicOperation);
      versionPositionMap.delete(atomicOperation);
    } catch (IOException e) {
      throw YTException.wrapException(
          new YTIndexException("Error during deletion of index " + name), e);
    }
  }

  private void doClearTree(OAtomicOperation atomicOperation) throws IOException {
    try (final Stream<Object> stream = sbTree.keyStream()) {
      stream.forEach(
          (key) -> {
            try {
              sbTree.remove(atomicOperation, key);
            } catch (final IOException e) {
              throw YTException.wrapException(
                  new YTIndexException("Error during clearing a tree" + name), e);
            }
          });
    }

    if (sbTree.isNullPointerSupport()) {
      sbTree.remove(atomicOperation, null);
    }
  }

  @Override
  public void load(IndexEngineData data) {
    final OEncryption encryption =
        OAbstractPaginatedStorage.loadEncryption(data.getEncryption(), data.getEncryptionOptions());

    sbTree.load(
        data.getName(),
        (OBinarySerializer) storage.resolveObjectSerializer(data.getKeySerializedId()),
        (OBinarySerializer) storage.resolveObjectSerializer(data.getValueSerializerId()),
        data.getKeyTypes(),
        data.getKeySize(),
        data.isNullValuesSupport(),
        encryption);
    try {
      versionPositionMap.open();
    } catch (final IOException e) {
      throw YTException.wrapException(
          new YTIndexException("Error during VPM load of index " + data.getName()), e);
    }
  }

  @Override
  public boolean remove(OAtomicOperation atomicOperation, Object key) {
    try {
      return sbTree.remove(atomicOperation, key) != null;
    } catch (IOException e) {
      throw YTException.wrapException(
          new YTIndexException("Error during removal of key " + key + " from index " + name), e);
    }
  }

  @Override
  public void clear(OAtomicOperation atomicOperation) {
    try {
      doClearTree(atomicOperation);
    } catch (IOException e) {
      throw YTException.wrapException(new YTIndexException("Error during clear index " + name), e);
    }
  }

  @Override
  public void close() {
    sbTree.close();
  }

  @Override
  public Object get(YTDatabaseSessionInternal session, Object key) {
    return sbTree.get(key);
  }

  @Override
  public Stream<ORawPair<Object, YTRID>> stream(IndexEngineValuesTransformer valuesTransformer) {
    final Object firstKey = sbTree.firstKey();
    if (firstKey == null) {
      return StreamSupport.stream(Spliterators.emptySpliterator(), false);
    }
    return convertTreeStreamToIndexStream(
        valuesTransformer, sbTree.iterateEntriesMajor(firstKey, true, true));
  }

  private static Stream<ORawPair<Object, YTRID>> convertTreeStreamToIndexStream(
      IndexEngineValuesTransformer valuesTransformer, Stream<ORawPair<Object, Object>> treeStream) {
    if (valuesTransformer == null) {
      return treeStream.map((entry) -> new ORawPair<>(entry.first, (YTRID) entry.second));
    } else {
      //noinspection resource
      return treeStream.flatMap(
          (entry) ->
              valuesTransformer.transformFromValue(entry.second).stream()
                  .map((rid) -> new ORawPair<>(entry.first, rid)));
    }
  }

  @Override
  public Stream<ORawPair<Object, YTRID>> descStream(
      IndexEngineValuesTransformer valuesTransformer) {
    final Object lastKey = sbTree.lastKey();
    if (lastKey == null) {
      return StreamSupport.stream(Spliterators.emptySpliterator(), false);
    }

    return convertTreeStreamToIndexStream(
        valuesTransformer, sbTree.iterateEntriesMinor(lastKey, true, false));
  }

  @Override
  public Stream<Object> keyStream() {
    return sbTree.keyStream();
  }

  @Override
  public void put(YTDatabaseSessionInternal session, OAtomicOperation atomicOperation, Object key,
      Object value) {
    try {
      sbTree.put(atomicOperation, key, value);
    } catch (IOException e) {
      throw YTException.wrapException(
          new YTIndexException("Error during insertion of key " + key + " in index " + name), e);
    }
  }

  @Override
  public void update(
      YTDatabaseSessionInternal session, OAtomicOperation atomicOperation, Object key,
      OIndexKeyUpdater<Object> updater) {
    try {
      sbTree.update(atomicOperation, key, updater, null);
    } catch (IOException e) {
      throw YTException.wrapException(
          new YTIndexException("Error during update of key " + key + " in index " + name), e);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean validatedPut(
      OAtomicOperation atomicOperation,
      Object key,
      YTRID value,
      IndexEngineValidator<Object, YTRID> validator) {
    try {
      return sbTree.validatedPut(atomicOperation, key, value, (IndexEngineValidator) validator);
    } catch (IOException e) {
      throw YTException.wrapException(
          new YTIndexException("Error during insertion of key " + key + " in index " + name), e);
    }
  }

  @Override
  public Stream<ORawPair<Object, YTRID>> iterateEntriesBetween(
      YTDatabaseSessionInternal session, Object rangeFrom,
      boolean fromInclusive,
      Object rangeTo,
      boolean toInclusive,
      boolean ascSortOrder,
      IndexEngineValuesTransformer transformer) {
    return convertTreeStreamToIndexStream(
        transformer,
        sbTree.iterateEntriesBetween(rangeFrom, fromInclusive, rangeTo, toInclusive, ascSortOrder));
  }

  @Override
  public Stream<ORawPair<Object, YTRID>> iterateEntriesMajor(
      Object fromKey,
      boolean isInclusive,
      boolean ascSortOrder,
      IndexEngineValuesTransformer transformer) {
    return convertTreeStreamToIndexStream(
        transformer, sbTree.iterateEntriesMajor(fromKey, isInclusive, ascSortOrder));
  }

  @Override
  public Stream<ORawPair<Object, YTRID>> iterateEntriesMinor(
      Object toKey,
      boolean isInclusive,
      boolean ascSortOrder,
      IndexEngineValuesTransformer transformer) {
    return convertTreeStreamToIndexStream(
        transformer, sbTree.iterateEntriesMinor(toKey, isInclusive, ascSortOrder));
  }

  @Override
  public long size(final IndexEngineValuesTransformer transformer) {
    if (transformer == null) {
      return sbTree.size();
    } else {
      int counter = 0;

      if (sbTree.isNullPointerSupport()) {
        final Object nullValue = sbTree.get(null);
        if (nullValue != null) {
          counter += transformer.transformFromValue(nullValue).size();
        }
      }

      final Object firstKey = sbTree.firstKey();
      final Object lastKey = sbTree.lastKey();

      if (firstKey != null && lastKey != null) {
        try (final Stream<ORawPair<Object, Object>> stream =
            sbTree.iterateEntriesBetween(firstKey, true, lastKey, true, true)) {
          counter +=
              stream.mapToInt((pair) -> transformer.transformFromValue(pair.second).size()).sum();
        }
        return counter;
      }

      return counter;
    }
  }

  @Override
  public boolean hasRangeQuerySupport() {
    return true;
  }

  @Override
  public boolean acquireAtomicExclusiveLock(Object key) {
    sbTree.acquireAtomicExclusiveLock();
    return true;
  }

  @Override
  public String getIndexNameByKey(Object key) {
    return name;
  }

  @Override
  public void updateUniqueIndexVersion(final Object key) {
    final int keyHash = versionPositionMap.getKeyHash(key);
    versionPositionMap.updateVersion(keyHash);
  }

  @Override
  public int getUniqueIndexVersion(final Object key) {
    final int keyHash = versionPositionMap.getKeyHash(key);
    return versionPositionMap.getVersion(keyHash);
  }

  public boolean hasRidBagTreesSupport() {
    return true;
  }
}
