package com.jetbrains.youtrack.db.internal.core.storage.index.sbtree.local.v2;

import com.jetbrains.youtrack.db.internal.common.directmemory.ByteBufferPool;
import com.jetbrains.youtrack.db.internal.common.directmemory.DirectMemoryAllocator.Intention;
import com.jetbrains.youtrack.db.internal.common.directmemory.Pointer;
import com.jetbrains.youtrack.db.internal.common.serialization.types.LongSerializer;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.binary.impl.LinkSerializer;
import com.jetbrains.youtrack.db.internal.core.storage.cache.CacheEntryImpl;
import com.jetbrains.youtrack.db.internal.core.storage.cache.CachePointer;
import com.jetbrains.youtrack.db.internal.core.storage.cache.CacheEntry;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;
import org.junit.Assert;
import org.junit.Test;

/**
 * @since 12.08.13
 */
public class SBTreeNonLeafBucketV2Test {

  @Test
  public void testInitialization() {
    final ByteBufferPool bufferPool = ByteBufferPool.instance(null);
    final Pointer pointer = bufferPool.acquireDirect(true, Intention.TEST);

    CachePointer cachePointer = new CachePointer(pointer, bufferPool, 0, 0);
    CacheEntry cacheEntry = new CacheEntryImpl(0, 0, cachePointer, false, null);
    cacheEntry.acquireExclusiveLock();
    cachePointer.incrementReferrer();

    SBTreeBucketV2<Long, Identifiable> treeBucket = new SBTreeBucketV2<>(cacheEntry);
    treeBucket.init(false);

    Assert.assertEquals(treeBucket.size(), 0);
    Assert.assertFalse(treeBucket.isLeaf());

    treeBucket = new SBTreeBucketV2<>(cacheEntry);
    Assert.assertEquals(treeBucket.size(), 0);
    Assert.assertFalse(treeBucket.isLeaf());
    Assert.assertEquals(treeBucket.getLeftSibling(), -1);
    Assert.assertEquals(treeBucket.getRightSibling(), -1);

    cacheEntry.releaseExclusiveLock();
    cachePointer.decrementReferrer();
  }

  @Test
  public void testSearch() {
    long seed = System.currentTimeMillis();
    System.out.println("testSearch seed : " + seed);

    TreeSet<Long> keys = new TreeSet<>();
    Random random = new Random(seed);

    while (keys.size() < 2 * SBTreeBucketV2.MAX_PAGE_SIZE_BYTES / LongSerializer.LONG_SIZE) {
      keys.add(random.nextLong());
    }

    final ByteBufferPool bufferPool = ByteBufferPool.instance(null);
    final Pointer pointer = bufferPool.acquireDirect(true, Intention.TEST);

    CachePointer cachePointer = new CachePointer(pointer, bufferPool, 0, 0);
    CacheEntry cacheEntry = new CacheEntryImpl(0, 0, cachePointer, false, null);
    cacheEntry.acquireExclusiveLock();
    cachePointer.incrementReferrer();

    SBTreeBucketV2<Long, Identifiable> treeBucket = new SBTreeBucketV2<>(cacheEntry);
    treeBucket.init(false);

    int index = 0;
    Map<Long, Integer> keyIndexMap = new HashMap<>();
    for (Long key : keys) {
      if (!treeBucket.addNonLeafEntry(
          index,
          LongSerializer.INSTANCE.serializeNativeAsWhole(key),
          random.nextInt(Integer.MAX_VALUE),
          random.nextInt(Integer.MAX_VALUE),
          true)) {
        break;
      }

      keyIndexMap.put(key, index);
      index++;
    }

    Assert.assertEquals(treeBucket.size(), keyIndexMap.size());

    for (Map.Entry<Long, Integer> keyIndexEntry : keyIndexMap.entrySet()) {
      int bucketIndex = treeBucket.find(keyIndexEntry.getKey(), LongSerializer.INSTANCE);
      Assert.assertEquals(bucketIndex, (int) keyIndexEntry.getValue());
    }

    long prevRight = -1;
    for (int i = 0; i < treeBucket.size(); i++) {
      SBTreeBucketV2.SBTreeEntry<Long, Identifiable> entry =
          treeBucket.getEntry(i, LongSerializer.INSTANCE, LinkSerializer.INSTANCE);
      if (prevRight > 0) {
        Assert.assertEquals(entry.leftChild, prevRight);
      }

      prevRight = entry.rightChild;
    }

    long prevLeft = -1;
    for (int i = treeBucket.size() - 1; i >= 0; i--) {
      SBTreeBucketV2.SBTreeEntry<Long, Identifiable> entry =
          treeBucket.getEntry(i, LongSerializer.INSTANCE, LinkSerializer.INSTANCE);

      if (prevLeft > 0) {
        Assert.assertEquals(entry.rightChild, prevLeft);
      }

      prevLeft = entry.leftChild;
    }

    cacheEntry.releaseExclusiveLock();
    cachePointer.decrementReferrer();
  }

  @Test
  public void testShrink() {
    long seed = System.currentTimeMillis();
    System.out.println("testShrink seed : " + seed);

    TreeSet<Long> keys = new TreeSet<>();
    Random random = new Random(seed);

    while (keys.size() < 2 * SBTreeBucketV2.MAX_PAGE_SIZE_BYTES / LongSerializer.LONG_SIZE) {
      keys.add(random.nextLong());
    }

    final ByteBufferPool bufferPool = ByteBufferPool.instance(null);
    final Pointer pointer = bufferPool.acquireDirect(true, Intention.TEST);

    CachePointer cachePointer = new CachePointer(pointer, bufferPool, 0, 0);
    CacheEntry cacheEntry = new CacheEntryImpl(0, 0, cachePointer, false, null);
    cacheEntry.acquireExclusiveLock();

    cachePointer.incrementReferrer();

    SBTreeBucketV2<Long, Identifiable> treeBucket = new SBTreeBucketV2<>(cacheEntry);
    treeBucket.init(false);

    int index = 0;
    for (Long key : keys) {
      if (!treeBucket.addNonLeafEntry(
          index, LongSerializer.INSTANCE.serializeNativeAsWhole(key), index, index + 1, true)) {
        break;
      }

      index++;
    }

    int originalSize = treeBucket.size();

    treeBucket.shrink(treeBucket.size() / 2, LongSerializer.INSTANCE, LinkSerializer.INSTANCE);
    Assert.assertEquals(treeBucket.size(), index / 2);

    index = 0;
    final Map<Long, Integer> keyIndexMap = new HashMap<>();

    Iterator<Long> keysIterator = keys.iterator();
    while (keysIterator.hasNext() && index < treeBucket.size()) {
      Long key = keysIterator.next();
      keyIndexMap.put(key, index);
      index++;
    }

    for (Map.Entry<Long, Integer> keyIndexEntry : keyIndexMap.entrySet()) {
      int bucketIndex = treeBucket.find(keyIndexEntry.getKey(), LongSerializer.INSTANCE);
      Assert.assertEquals(bucketIndex, (int) keyIndexEntry.getValue());
    }

    for (Map.Entry<Long, Integer> keyIndexEntry : keyIndexMap.entrySet()) {
      SBTreeBucketV2.SBTreeEntry<Long, Identifiable> entry =
          treeBucket.getEntry(
              keyIndexEntry.getValue(), LongSerializer.INSTANCE, LinkSerializer.INSTANCE);

      Assert.assertEquals(
          entry,
          new SBTreeBucketV2.SBTreeEntry<Long, Identifiable>(
              keyIndexEntry.getValue(),
              keyIndexEntry.getValue() + 1,
              keyIndexEntry.getKey(),
              null));
    }

    int keysToAdd = originalSize - treeBucket.size();
    int addedKeys = 0;
    while (keysIterator.hasNext() && index < originalSize) {
      Long key = keysIterator.next();

      if (!treeBucket.addNonLeafEntry(
          index, LongSerializer.INSTANCE.serializeNativeAsWhole(key), index, index + 1, true)) {
        break;
      }

      keyIndexMap.put(key, index);
      index++;
      addedKeys++;
    }

    for (Map.Entry<Long, Integer> keyIndexEntry : keyIndexMap.entrySet()) {
      SBTreeBucketV2.SBTreeEntry<Long, Identifiable> entry =
          treeBucket.getEntry(
              keyIndexEntry.getValue(), LongSerializer.INSTANCE, LinkSerializer.INSTANCE);

      Assert.assertEquals(
          entry,
          new SBTreeBucketV2.SBTreeEntry<Long, Identifiable>(
              keyIndexEntry.getValue(),
              keyIndexEntry.getValue() + 1,
              keyIndexEntry.getKey(),
              null));
    }

    Assert.assertEquals(treeBucket.size(), originalSize);
    Assert.assertEquals(addedKeys, keysToAdd);

    cacheEntry.releaseExclusiveLock();
    cachePointer.decrementReferrer();
  }
}