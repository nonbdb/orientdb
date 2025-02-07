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

package com.jetbrains.youtrack.db.internal.core.storage.index.sbtreebonsai.local;

import com.jetbrains.youtrack.db.internal.common.comparator.DefaultComparator;
import com.jetbrains.youtrack.db.internal.common.serialization.types.BinarySerializer;
import com.jetbrains.youtrack.db.internal.common.serialization.types.ByteSerializer;
import com.jetbrains.youtrack.db.internal.common.serialization.types.IntegerSerializer;
import com.jetbrains.youtrack.db.internal.common.serialization.types.LongSerializer;
import com.jetbrains.youtrack.db.api.config.GlobalConfiguration;
import com.jetbrains.youtrack.db.internal.core.exception.SBTreeBonsaiLocalException;
import com.jetbrains.youtrack.db.internal.core.storage.cache.CacheEntry;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @since 8/7/13
 */
public final class SBTreeBonsaiBucket<K, V> extends BonsaiBucketAbstract {

  public static final int MAX_BUCKET_SIZE_BYTES =
      GlobalConfiguration.SBTREEBONSAI_BUCKET_SIZE.getValueAsInteger() * 1024;

  /**
   * Maximum size of key-value pair which can be put in SBTreeBonsai in bytes (24576000 by default)
   */
  private static final byte LEAF = 0x1;

  private static final byte DELETED = 0x2;
  private static final byte TO_DELETE = 0x4;
  private static final int MAX_ENTREE_SIZE = 24576000;
  private static final int FREE_POINTER_OFFSET = WAL_POSITION_OFFSET + LongSerializer.LONG_SIZE;
  private static final int SIZE_OFFSET = FREE_POINTER_OFFSET + IntegerSerializer.INT_SIZE;
  private static final int FLAGS_OFFSET = SIZE_OFFSET + IntegerSerializer.INT_SIZE;
  private static final int FREE_LIST_POINTER_OFFSET = FLAGS_OFFSET + ByteSerializer.BYTE_SIZE;
  private static final int LEFT_SIBLING_OFFSET =
      FREE_LIST_POINTER_OFFSET + BonsaiBucketPointer.SIZE;
  private static final int RIGHT_SIBLING_OFFSET = LEFT_SIBLING_OFFSET + BonsaiBucketPointer.SIZE;
  private static final int TREE_SIZE_OFFSET = RIGHT_SIBLING_OFFSET + BonsaiBucketPointer.SIZE;
  private static final int KEY_SERIALIZER_OFFSET = TREE_SIZE_OFFSET + LongSerializer.LONG_SIZE;
  private static final int VALUE_SERIALIZER_OFFSET =
      KEY_SERIALIZER_OFFSET + ByteSerializer.BYTE_SIZE;
  private static final int POSITIONS_ARRAY_OFFSET =
      VALUE_SERIALIZER_OFFSET + ByteSerializer.BYTE_SIZE;
  private final boolean isLeaf;
  private final int offset;

  private final BinarySerializer<K> keySerializer;
  private final BinarySerializer<V> valueSerializer;

  private final Comparator<? super K> comparator = DefaultComparator.INSTANCE;

  private final SBTreeBonsaiLocal<K, V> tree;

  public static final class SBTreeEntry<K, V>
      implements Map.Entry<K, V>, Comparable<SBTreeEntry<K, V>> {

    public final BonsaiBucketPointer leftChild;
    public final BonsaiBucketPointer rightChild;
    public final K key;
    public final V value;
    private final Comparator<? super K> comparator = DefaultComparator.INSTANCE;

    public SBTreeEntry(
        BonsaiBucketPointer leftChild, BonsaiBucketPointer rightChild, K key, V value) {
      this.leftChild = leftChild;
      this.rightChild = rightChild;
      this.key = key;
      this.value = value;
    }

    @Override
    public K getKey() {
      return key;
    }

    @Override
    public V getValue() {
      return value;
    }

    @Override
    public V setValue(V value) {
      throw new UnsupportedOperationException("CellBTreeEntry.setValue");
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      SBTreeEntry that = (SBTreeEntry) o;

      if (!leftChild.equals(that.leftChild)) {
        return false;
      }
      if (!rightChild.equals(that.rightChild)) {
        return false;
      }
      if (!key.equals(that.key)) {
        return false;
      }
      return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
      int result = leftChild.hashCode();
      result = 31 * result + rightChild.hashCode();
      result = 31 * result + key.hashCode();
      result = 31 * result + (value != null ? value.hashCode() : 0);
      return result;
    }

    @Override
    public String toString() {
      return "CellBTreeEntry{"
          + "leftChild="
          + leftChild
          + ", rightChild="
          + rightChild
          + ", key="
          + key
          + ", value="
          + value
          + '}';
    }

    @Override
    public int compareTo(SBTreeEntry<K, V> other) {
      return comparator.compare(key, other.key);
    }
  }

  public SBTreeBonsaiBucket(
      CacheEntry cacheEntry,
      int pageOffset,
      boolean isLeaf,
      BinarySerializer<K> keySerializer,
      BinarySerializer<V> valueSerializer,
      SBTreeBonsaiLocal<K, V> tree)
      throws IOException {
    super(cacheEntry);

    this.offset = pageOffset;
    this.isLeaf = isLeaf;
    this.keySerializer = keySerializer;
    this.valueSerializer = valueSerializer;

    setIntValue(offset + FREE_POINTER_OFFSET, MAX_BUCKET_SIZE_BYTES);
    setIntValue(offset + SIZE_OFFSET, 0);

    // THIS REMOVE ALSO THE EVENTUAL DELETED FLAG
    setByteValue(offset + FLAGS_OFFSET, isLeaf ? LEAF : 0);
    setLongValue(offset + LEFT_SIBLING_OFFSET, -1);
    setLongValue(offset + RIGHT_SIBLING_OFFSET, -1);

    setLongValue(offset + TREE_SIZE_OFFSET, 0);

    setByteValue(offset + KEY_SERIALIZER_OFFSET, keySerializer.getId());
    setByteValue(offset + VALUE_SERIALIZER_OFFSET, valueSerializer.getId());
    this.tree = tree;
  }

  public SBTreeBonsaiBucket(
      CacheEntry cacheEntry,
      int pageOffset,
      BinarySerializer<K> keySerializer,
      BinarySerializer<V> valueSerializer,
      SBTreeBonsaiLocal<K, V> tree) {
    super(cacheEntry);

    this.offset = pageOffset;
    this.isLeaf = (getByteValue(offset + FLAGS_OFFSET) & LEAF) == LEAF;
    this.keySerializer = keySerializer;
    this.valueSerializer = valueSerializer;
    this.tree = tree;
  }

  public byte getKeySerializerId() {
    return getByteValue(offset + KEY_SERIALIZER_OFFSET);
  }

  public void setKeySerializerId(byte keySerializerId) {
    setByteValue(offset + KEY_SERIALIZER_OFFSET, keySerializerId);
  }

  public byte getValueSerializerId() {
    return getByteValue(offset + VALUE_SERIALIZER_OFFSET);
  }

  public void setValueSerializerId(byte valueSerializerId) {
    setByteValue(offset + VALUE_SERIALIZER_OFFSET, valueSerializerId);
  }

  public long getTreeSize() {
    return getLongValue(offset + TREE_SIZE_OFFSET);
  }

  public void setTreeSize(long size) throws IOException {
    setLongValue(offset + TREE_SIZE_OFFSET, size);
  }

  public boolean isEmpty() {
    return size() == 0;
  }

  public int find(K key) {
    int low = 0;
    int high = size() - 1;

    while (low <= high) {
      int mid = (low + high) >>> 1;
      K midVal = getKey(mid);
      int cmp = comparator.compare(midVal, key);

      if (cmp < 0) {
        low = mid + 1;
      } else if (cmp > 0) {
        high = mid - 1;
      } else {
        return mid; // key found
      }
    }
    return -(low + 1); // key not found.
  }

  public void remove(int entryIndex) throws IOException {
    int entryPosition =
        getIntValue(offset + POSITIONS_ARRAY_OFFSET + entryIndex * IntegerSerializer.INT_SIZE);

    int entrySize = getObjectSizeInDirectMemory(keySerializer, offset + entryPosition);
    if (isLeaf) {
      assert valueSerializer.isFixedLength();
      entrySize += valueSerializer.getFixedLength();
    } else {
      throw new IllegalStateException("Remove is applies to leaf buckets only");
    }

    int size = size();
    if (entryIndex < size - 1) {
      moveData(
          offset + POSITIONS_ARRAY_OFFSET + (entryIndex + 1) * IntegerSerializer.INT_SIZE,
          offset + POSITIONS_ARRAY_OFFSET + entryIndex * IntegerSerializer.INT_SIZE,
          (size - entryIndex - 1) * IntegerSerializer.INT_SIZE);
    }

    size--;
    setIntValue(offset + SIZE_OFFSET, size);

    int freePointer = getIntValue(offset + FREE_POINTER_OFFSET);
    if (size > 0 && entryPosition > freePointer) {
      moveData(offset + freePointer, offset + freePointer + entrySize, entryPosition - freePointer);
    }
    setIntValue(offset + FREE_POINTER_OFFSET, freePointer + entrySize);

    int currentPositionOffset = offset + POSITIONS_ARRAY_OFFSET;

    for (int i = 0; i < size; i++) {
      int currentEntryPosition = getIntValue(currentPositionOffset);
      if (currentEntryPosition < entryPosition) {
        setIntValue(currentPositionOffset, currentEntryPosition + entrySize);
      }
      currentPositionOffset += IntegerSerializer.INT_SIZE;
    }
  }

  public int size() {
    return getIntValue(offset + SIZE_OFFSET);
  }

  public SBTreeEntry<K, V> getEntry(int entryIndex) {
    int entryPosition =
        getIntValue(offset + entryIndex * IntegerSerializer.INT_SIZE + POSITIONS_ARRAY_OFFSET);

    if (isLeaf) {
      K key = deserializeFromDirectMemory(keySerializer, offset + entryPosition);
      entryPosition += getObjectSizeInDirectMemory(keySerializer, offset + entryPosition);

      V value = deserializeFromDirectMemory(valueSerializer, offset + entryPosition);

      return new SBTreeEntry<K, V>(
          BonsaiBucketPointer.NULL, BonsaiBucketPointer.NULL, key, value);
    } else {
      BonsaiBucketPointer leftChild = getBucketPointer(offset + entryPosition);
      entryPosition += BonsaiBucketPointer.SIZE;

      BonsaiBucketPointer rightChild = getBucketPointer(offset + entryPosition);
      entryPosition += BonsaiBucketPointer.SIZE;

      K key = deserializeFromDirectMemory(keySerializer, offset + entryPosition);

      return new SBTreeEntry<K, V>(leftChild, rightChild, key, null);
    }
  }

  public K getKey(int index) {
    int entryPosition =
        getIntValue(offset + index * IntegerSerializer.INT_SIZE + POSITIONS_ARRAY_OFFSET);

    if (!isLeaf) {
      entryPosition += 2 * (LongSerializer.LONG_SIZE + IntegerSerializer.INT_SIZE);
    }

    return deserializeFromDirectMemory(keySerializer, offset + entryPosition);
  }

  public boolean isLeaf() {
    return isLeaf;
  }

  public void addAll(List<SBTreeEntry<K, V>> entries) throws IOException {
    for (int i = 0; i < entries.size(); i++) {
      addEntry(i, entries.get(i), false);
    }
  }

  public void shrink(int newSize) throws IOException {
    List<SBTreeEntry<K, V>> treeEntries = new ArrayList<SBTreeEntry<K, V>>(newSize);

    for (int i = 0; i < newSize; i++) {
      treeEntries.add(getEntry(i));
    }

    setIntValue(offset + FREE_POINTER_OFFSET, MAX_BUCKET_SIZE_BYTES);
    setIntValue(offset + SIZE_OFFSET, 0);

    int index = 0;
    for (SBTreeEntry<K, V> entry : treeEntries) {
      addEntry(index, entry, false);
      index++;
    }
  }

  public boolean addEntry(int index, SBTreeEntry<K, V> treeEntry, boolean updateNeighbors)
      throws IOException {
    final int keySize = keySerializer.getObjectSize(treeEntry.key);
    int valueSize = 0;
    int entrySize = keySize;

    if (isLeaf) {
      assert valueSerializer.isFixedLength();
      valueSize = valueSerializer.getFixedLength();

      entrySize += valueSize;

      checkEntreeSize(entrySize);
    } else {
      entrySize += 2 * (LongSerializer.LONG_SIZE + IntegerSerializer.INT_SIZE);
    }

    int size = size();
    int freePointer = getIntValue(offset + FREE_POINTER_OFFSET);
    if (freePointer - entrySize
        < (size + 1) * IntegerSerializer.INT_SIZE + POSITIONS_ARRAY_OFFSET) {
      if (size > 1) {
        return false;
      } else {
        throw new SBTreeBonsaiLocalException(
            "Entry size ('key + value') is more than is more than allowed "
                + (freePointer - 2 * IntegerSerializer.INT_SIZE + POSITIONS_ARRAY_OFFSET)
                + " bytes, either increase page size using '"
                + GlobalConfiguration.SBTREEBONSAI_BUCKET_SIZE.getKey()
                + "' parameter, or decrease 'key + value' size.",
            tree);
      }
    }

    if (index <= size - 1) {
      moveData(
          offset + POSITIONS_ARRAY_OFFSET + index * IntegerSerializer.INT_SIZE,
          offset + POSITIONS_ARRAY_OFFSET + (index + 1) * IntegerSerializer.INT_SIZE,
          (size - index) * IntegerSerializer.INT_SIZE);
    }

    freePointer -= entrySize;

    setIntValue(offset + FREE_POINTER_OFFSET, freePointer);
    setIntValue(offset + POSITIONS_ARRAY_OFFSET + index * IntegerSerializer.INT_SIZE, freePointer);
    setIntValue(offset + SIZE_OFFSET, size + 1);

    if (isLeaf) {
      byte[] serializedKey = new byte[keySize];
      keySerializer.serializeNativeObject(treeEntry.key, serializedKey, 0);

      setBinaryValue(offset + freePointer, serializedKey);
      freePointer += keySize;

      byte[] serializedValue = new byte[valueSize];
      valueSerializer.serializeNativeObject(treeEntry.value, serializedValue, 0);
      setBinaryValue(offset + freePointer, serializedValue);

    } else {
      setBucketPointer(offset + freePointer, treeEntry.leftChild);
      freePointer += LongSerializer.LONG_SIZE + IntegerSerializer.INT_SIZE;

      setBucketPointer(offset + freePointer, treeEntry.rightChild);
      freePointer += LongSerializer.LONG_SIZE + IntegerSerializer.INT_SIZE;

      byte[] serializedKey = new byte[keySize];
      keySerializer.serializeNativeObject(treeEntry.key, serializedKey, 0);
      setBinaryValue(offset + freePointer, serializedKey);

      size++;

      if (updateNeighbors && size > 1) {
        if (index < size - 1) {
          final int nextEntryPosition =
              getIntValue(
                  offset + POSITIONS_ARRAY_OFFSET + (index + 1) * IntegerSerializer.INT_SIZE);
          setBucketPointer(offset + nextEntryPosition, treeEntry.rightChild);
        }

        if (index > 0) {
          final int prevEntryPosition =
              getIntValue(
                  offset + POSITIONS_ARRAY_OFFSET + (index - 1) * IntegerSerializer.INT_SIZE);
          setBucketPointer(
              offset + prevEntryPosition + LongSerializer.LONG_SIZE + IntegerSerializer.INT_SIZE,
              treeEntry.leftChild);
        }
      }
    }

    return true;
  }

  public int updateValue(int index, V value) throws IOException {
    assert valueSerializer.isFixedLength();

    int entryPosition =
        getIntValue(offset + index * IntegerSerializer.INT_SIZE + POSITIONS_ARRAY_OFFSET);
    entryPosition += getObjectSizeInDirectMemory(keySerializer, offset + entryPosition);

    final int size = valueSerializer.getFixedLength();

    byte[] serializedValue = new byte[size];
    valueSerializer.serializeNativeObject(value, serializedValue, 0);

    byte[] oldSerializedValue = getBinaryValue(offset + entryPosition, size);

    if (DefaultComparator.INSTANCE.compare(oldSerializedValue, serializedValue) == 0) {
      return 0;
    }

    setBinaryValue(offset + entryPosition, serializedValue);

    return 1;
  }

  public BonsaiBucketPointer getFreeListPointer() {
    return getBucketPointer(offset + FREE_LIST_POINTER_OFFSET);
  }

  public void setFreeListPointer(BonsaiBucketPointer pointer) throws IOException {
    setBucketPointer(offset + FREE_LIST_POINTER_OFFSET, pointer);
  }

  public void setDelted(boolean deleted) {
    byte value = getByteValue(offset + FLAGS_OFFSET);
    if (deleted) {
      setByteValue(offset + FLAGS_OFFSET, (byte) (value | DELETED));
    } else
    // REMOVE THE FLAG the &(and) ~(not) is the opreation to remove flags in bits
    {
      setByteValue(offset + FLAGS_OFFSET, (byte) (value & (~DELETED)));
    }
  }

  public boolean isDeleted() {
    return (getByteValue(offset + FLAGS_OFFSET) & DELETED) == DELETED;
  }

  public void setToDelete(boolean toDelete) {
    byte value = getByteValue(offset + FLAGS_OFFSET);
    if (toDelete) {
      setByteValue(offset + FLAGS_OFFSET, (byte) (value | TO_DELETE));
    } else
    // REMOVE THE FLAG the &(and) ~(not) is the opreation to remove flags in bits
    {
      setByteValue(offset + FLAGS_OFFSET, (byte) (value & (~TO_DELETE)));
    }
  }

  public boolean isToDelete() {
    return (getByteValue(offset + FLAGS_OFFSET) & TO_DELETE) == TO_DELETE;
  }

  public BonsaiBucketPointer getLeftSibling() {
    return getBucketPointer(offset + LEFT_SIBLING_OFFSET);
  }

  public void setLeftSibling(BonsaiBucketPointer pointer) throws IOException {
    setBucketPointer(offset + LEFT_SIBLING_OFFSET, pointer);
  }

  public BonsaiBucketPointer getRightSibling() {
    return getBucketPointer(offset + RIGHT_SIBLING_OFFSET);
  }

  public void setRightSibling(BonsaiBucketPointer pointer) throws IOException {
    setBucketPointer(offset + RIGHT_SIBLING_OFFSET, pointer);
  }

  private void checkEntreeSize(int entreeSize) {
    if (entreeSize > MAX_ENTREE_SIZE) {
      throw new SBTreeBonsaiLocalException(
          "Serialized key-value pair size bigger than allowed "
              + entreeSize
              + " vs "
              + MAX_ENTREE_SIZE
              + ".",
          tree);
    }
  }
}
