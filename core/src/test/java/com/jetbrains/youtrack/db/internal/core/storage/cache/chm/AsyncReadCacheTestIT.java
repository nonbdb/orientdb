package com.jetbrains.youtrack.db.internal.core.storage.cache.chm;

import com.jetbrains.youtrack.db.internal.common.directmemory.ByteBufferPool;
import com.jetbrains.youtrack.db.internal.common.directmemory.DirectMemoryAllocator;
import com.jetbrains.youtrack.db.internal.common.directmemory.DirectMemoryAllocator.Intention;
import com.jetbrains.youtrack.db.internal.common.types.ModifiableBoolean;
import com.jetbrains.youtrack.db.internal.core.command.CommandOutputListener;
import com.jetbrains.youtrack.db.internal.core.storage.cache.CachePointer;
import com.jetbrains.youtrack.db.internal.core.storage.cache.PageDataVerificationError;
import com.jetbrains.youtrack.db.internal.core.storage.cache.WriteCache;
import com.jetbrains.youtrack.db.internal.core.storage.cache.local.BackgroundExceptionListener;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.PageIsBrokenListener;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.wal.LogSequenceNumber;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Assert;
import org.junit.Test;

public class AsyncReadCacheTestIT {

  @Test
  public void testEvenDistribution() throws Exception {
    final var pageSize = 4 * 1024;

    final var allocator = new DirectMemoryAllocator();
    final var byteBufferPool = new ByteBufferPool(pageSize, allocator, 256);
    final long maxMemory = 1024 * 1024 * 1024;

    final var readCache = new AsyncReadCache(byteBufferPool, maxMemory, pageSize, true);
    final WriteCache writeCache = new MockedWriteCache(byteBufferPool);

    final var executor = Executors.newCachedThreadPool();
    final List<Future<Void>> futures = new ArrayList<>();

    final var fileLimit = 10;
    final var pageLimit = (int) (5L * 1024 * 1024 * 1024 / pageSize / fileLimit);
    final var pageCount = (int) (1024L * 1024 * 1024 * 1024 / pageSize / 8);
    final var timer = new Timer();

    final var memoryAboveLimit = new AtomicBoolean();
    timer.schedule(
        new TimerTask() {
          @Override
          public void run() {
            final var memoryConsumption = allocator.getMemoryConsumption();
            memoryAboveLimit.set(memoryAboveLimit.get() || memoryConsumption > maxMemory * 1.1);

            if (memoryConsumption > maxMemory * 1.1) {
              System.out.println("Memory limit is exceeded " + memoryConsumption);
            }
          }
        },
        1000,
        1000);

    for (var i = 0; i < 4; i++) {
      futures.add(
          executor.submit(new PageReader(fileLimit, pageLimit, writeCache, pageCount, readCache)));
    }

    for (var i = 0; i < 4; i++) {
      futures.add(
          executor.submit(new PageWriter(fileLimit, pageLimit, writeCache, pageCount, readCache)));
    }

    for (var future : futures) {
      future.get();
    }

    readCache.assertSize();
    readCache.assertConsistency();

    Assert.assertEquals(
        allocator.getMemoryConsumption() - (long) byteBufferPool.getPoolSize() * pageSize,
        readCache.getUsedMemory());

    Assert.assertFalse(memoryAboveLimit.get());
    Assert.assertTrue(
        "Invalid cache size " + readCache.getUsedMemory(), readCache.getUsedMemory() <= maxMemory);
    System.out.println(
        "Disk cache size "
            + readCache.getUsedMemory() / 1024 / 1024
            + " megabytes, "
            + ((long) pageCount * pageSize) / 1024 / 1024
            + " megabytes were accessed.");
    System.out.println("Hit rate " + readCache.hitRate());

    timer.cancel();

    readCache.clear();
    Assert.assertEquals(
        0, allocator.getMemoryConsumption() - (long) byteBufferPool.getPoolSize() * pageSize);
    Assert.assertEquals(0, readCache.getUsedMemory());
    readCache.assertSize();
  }

  @Test
  public void testZiphianDistribution() throws Exception {
    final var pageSize = 4 * 1024;

    final var allocator = new DirectMemoryAllocator();
    final var byteBufferPool = new ByteBufferPool(pageSize, allocator, 2048);
    final long maxMemory = 1024 * 1024 * 1024;

    final var readCache = new AsyncReadCache(byteBufferPool, maxMemory, pageSize, true);
    final WriteCache writeCache = new MockedWriteCache(byteBufferPool);

    final var executor = Executors.newCachedThreadPool();
    final List<Future<Void>> futures = new ArrayList<>();

    final var pageLimit = (int) (5 * 1024L * 1024 * 1024 / pageSize);
    final var pageCount = (int) (1024L * 1024 * 1024 * 1024 / pageSize / 8);
    final var timer = new Timer();

    final var memoryAboveLimit = new AtomicBoolean();
    timer.schedule(
        new TimerTask() {
          @Override
          public void run() {
            final var memoryConsumption = allocator.getMemoryConsumption();
            memoryAboveLimit.set(memoryAboveLimit.get() || memoryConsumption > maxMemory * 1.1);

            if (memoryConsumption > maxMemory * 1.1) {
              System.out.println("Memory limit is exceeded " + memoryConsumption);
            }
          }
        },
        1000,
        1000);

    var start = System.nanoTime();
    for (var i = 0; i < 4; i++) {
      futures.add(
          executor.submit(new ZiphianPageReader(pageLimit, writeCache, pageCount, readCache)));
    }

    for (var i = 0; i < 4; i++) {
      futures.add(
          executor.submit(new ZiphianPageWriter(pageLimit, writeCache, pageCount, readCache)));
    }

    for (var future : futures) {
      future.get();
    }
    var end = System.nanoTime();

    readCache.assertSize();
    readCache.assertConsistency();

    Assert.assertEquals(
        allocator.getMemoryConsumption() - (long) byteBufferPool.getPoolSize() * pageSize,
        readCache.getUsedMemory());

    Assert.assertFalse(memoryAboveLimit.get());
    Assert.assertTrue(
        "Invalid cache size " + readCache.getUsedMemory(), readCache.getUsedMemory() <= maxMemory);
    System.out.println(
        "Disk cache size "
            + readCache.getUsedMemory() / 1024 / 1024
            + " megabytes, "
            + ((long) pageCount) * pageSize / 1024 / 1024
            + " megabytes were accessed.");
    System.out.println("Hit rate " + readCache.hitRate());
    final var total = end - start;
    final var nsPerPage = total / pageCount;
    final var opPerSec = 1_000_000_000 / nsPerPage;
    System.out.println("Speed " + opPerSec + " pages/sec.");

    timer.cancel();

    readCache.clear();
    Assert.assertEquals(
        0, allocator.getMemoryConsumption() - (long) byteBufferPool.getPoolSize() * pageSize);
    Assert.assertEquals(0, readCache.getUsedMemory());
    readCache.assertSize();
  }

  private static final class PageWriter implements Callable<Void> {

    private final int fileLimit;
    private final int pageLimit;
    private final WriteCache writeCache;
    private final int pageCount;

    private final AsyncReadCache readCache;

    private PageWriter(
        final int fileLimit,
        final int pageLimit,
        final WriteCache writeCache,
        final int pageCount,
        final AsyncReadCache readCache) {
      this.fileLimit = fileLimit;
      this.pageLimit = pageLimit;
      this.writeCache = writeCache;
      this.pageCount = pageCount;
      this.readCache = readCache;
    }

    @Override
    public Void call() {
      var pageCounter = 0;

      final var random = ThreadLocalRandom.current();
      while (pageCounter < pageCount) {
        final var fileId = random.nextInt(fileLimit);
        final var pageIndex = random.nextInt(pageLimit);

        final var cacheEntry =
            readCache.loadForWrite(fileId, pageIndex, writeCache, true, null);
        readCache.releaseFromWrite(cacheEntry, writeCache, true);
        pageCounter++;
      }

      return null;
    }
  }

  private static final class PageReader implements Callable<Void> {

    private final int fileLimit;
    private final int pageLimit;
    private final WriteCache writeCache;
    private final int pageCount;

    private final AsyncReadCache readCache;

    private PageReader(
        final int fileLimit,
        final int pageLimit,
        final WriteCache writeCache,
        final int pageCount,
        final AsyncReadCache readCache) {
      this.fileLimit = fileLimit;
      this.pageLimit = pageLimit;
      this.writeCache = writeCache;
      this.pageCount = pageCount;
      this.readCache = readCache;
    }

    @Override
    public Void call() {
      var pageCounter = 0;

      final var random = ThreadLocalRandom.current();
      while (pageCounter < pageCount) {
        final var fileId = random.nextInt(fileLimit);
        final var pageIndex = random.nextInt(pageLimit);

        final var cacheEntry = readCache.loadForRead(fileId, pageIndex, writeCache, true);
        readCache.releaseFromRead(cacheEntry);
        pageCounter++;
      }

      return null;
    }
  }

  private static final class ZiphianPageWriter implements Callable<Void> {

    private final int pageLimit;
    private final WriteCache writeCache;
    private final int pageCount;

    private final AsyncReadCache readCache;

    private ZiphianPageWriter(
        final int pageLimit,
        final WriteCache writeCache,
        final int pageCount,
        final AsyncReadCache readCache) {
      this.pageLimit = pageLimit;
      this.writeCache = writeCache;
      this.pageCount = pageCount;
      this.readCache = readCache;
    }

    @Override
    public Void call() {
      var pageCounter = 0;

      final var random = new ScrambledZipfianGenerator(pageLimit);
      while (pageCounter < pageCount) {
        final var pageIndex = random.nextInt();
        assert pageIndex < pageLimit;
        final var cacheEntry = readCache.loadForWrite(0, pageIndex, writeCache, true, null);
        readCache.releaseFromWrite(cacheEntry, writeCache, true);
        pageCounter++;
      }

      return null;
    }
  }

  private static final class ZiphianPageReader implements Callable<Void> {

    private final int pageLimit;
    private final WriteCache writeCache;
    private final int pageCount;

    private final AsyncReadCache readCache;

    private ZiphianPageReader(
        final int pageLimit,
        final WriteCache writeCache,
        final int pageCount,
        final AsyncReadCache readCache) {
      this.pageLimit = pageLimit;
      this.writeCache = writeCache;
      this.pageCount = pageCount;
      this.readCache = readCache;
    }

    @Override
    public Void call() {
      var pageCounter = 0;

      final var random = new ScrambledZipfianGenerator(pageLimit);
      while (pageCounter < pageCount) {
        final var pageIndex = random.nextInt();
        assert pageIndex < pageLimit;
        final var cacheEntry = readCache.loadForRead(0, pageIndex, writeCache, true);
        readCache.releaseFromRead(cacheEntry);
        pageCounter++;
      }

      return null;
    }
  }

  private static final class MockedWriteCache implements WriteCache {

    private final ByteBufferPool byteBufferPool;

    MockedWriteCache(final ByteBufferPool byteBufferPool) {
      this.byteBufferPool = byteBufferPool;
    }

    @Override
    public void addPageIsBrokenListener(final PageIsBrokenListener listener) {
    }

    @Override
    public void removePageIsBrokenListener(final PageIsBrokenListener listener) {
    }

    @Override
    public long bookFileId(final String fileName) {
      return 0;
    }

    @Override
    public long loadFile(final String fileName) {
      return 0;
    }

    @Override
    public long addFile(final String fileName) {
      return 0;
    }

    @Override
    public long addFile(final String fileName, final long fileId) {
      return 0;
    }

    @Override
    public long fileIdByName(final String fileName) {
      return 0;
    }

    @Override
    public boolean checkLowDiskSpace() {
      return false;
    }

    @Override
    public void syncDataFiles(long segmentId, byte[] lastMetadata) {
    }

    @Override
    public void flushTillSegment(final long segmentId) {
    }

    @Override
    public boolean exists(final String fileName) {
      return false;
    }

    @Override
    public boolean exists(final long fileId) {
      return false;
    }

    @Override
    public void restoreModeOn() {
    }

    @Override
    public void restoreModeOff() {
    }

    @Override
    public void store(final long fileId, final long pageIndex, final CachePointer dataPointer) {
    }

    @Override
    public void checkCacheOverflow() {
    }

    @Override
    public int allocateNewPage(final long fileId) {
      return 0;
    }

    @Override
    public CachePointer load(
        final long fileId,
        final long startPageIndex,
        final ModifiableBoolean cacheHit,
        final boolean verifyChecksums) {
      final var pointer = byteBufferPool.acquireDirect(true, Intention.TEST);
      final var cachePointer =
          new CachePointer(pointer, byteBufferPool, fileId, (int) startPageIndex);
      cachePointer.incrementReadersReferrer();
      return cachePointer;
    }

    @Override
    public void flush(final long fileId) {
    }

    @Override
    public void flush() {
    }

    @Override
    public long getFilledUpTo(final long fileId) {
      return 0;
    }

    @Override
    public long getExclusiveWriteCachePagesSize() {
      return 0;
    }

    @Override
    public void deleteFile(final long fileId) {
    }

    @Override
    public void truncateFile(final long fileId) {
    }

    @Override
    public void renameFile(final long fileId, final String newFileName) {
    }

    @Override
    public long[] close() {
      return new long[0];
    }

    @Override
    public void close(final long fileId, final boolean flush) {
    }

    @Override
    public PageDataVerificationError[] checkStoredPages(
        final CommandOutputListener commandOutputListener) {
      return new PageDataVerificationError[0];
    }

    @Override
    public long[] delete() {
      return new long[0];
    }

    @Override
    public String fileNameById(final long fileId) {
      return null;
    }

    @Override
    public String nativeFileNameById(final long fileId) {
      return null;
    }

    @Override
    public int getId() {
      return 0;
    }

    @Override
    public Map<String, Long> files() {
      return null;
    }

    @Override
    public int pageSize() {
      return 0;
    }

    @Override
    public String restoreFileById(final long fileId) {
      return null;
    }

    @Override
    public void addBackgroundExceptionListener(final BackgroundExceptionListener listener) {
    }

    @Override
    public void removeBackgroundExceptionListener(final BackgroundExceptionListener listener) {
    }

    @Override
    public Path getRootDirectory() {
      return null;
    }

    @Override
    public int internalFileId(final long fileId) {
      return 0;
    }

    @Override
    public long externalFileId(final int fileId) {
      return 0;
    }

    @Override
    public boolean fileIdsAreEqual(long firsId, long secondId) {
      return false;
    }

    @Override
    public Long getMinimalNotFlushedSegment() {
      return null;
    }

    @Override
    public void updateDirtyPagesTable(
        final CachePointer pointer, final LogSequenceNumber startLSN) {
    }

    @Override
    public void create() {
    }

    @Override
    public void open() throws IOException {
    }

    @Override
    public void replaceFileId(long fileId, long newFileId) {
    }

    @Override
    public String getStorageName() {
      return null;
    }
  }

  private static final class ScrambledZipfianGenerator {

    static final long FNV_offset_basis_64 = 0xCBF29CE484222325L;
    static final long FNV_prime_64 = 1099511628211L;

    static final double ZETAN = 26.46902820178302;
    static final double USED_ZIPFIAN_CONSTANT = 0.99;
    static final long ITEM_COUNT = 10000000000L;

    ZipfianGenerator gen;
    long _min, _max, _itemcount;

    ScrambledZipfianGenerator(long _items) {
      this(0, _items - 1);
    }

    ScrambledZipfianGenerator(long _min, long _max) {
      this(_min, _max, ZipfianGenerator.ZIPFIAN_CONSTANT);
    }

    ScrambledZipfianGenerator(long min, long max, double _zipfianconstant) {
      _min = min;
      _max = max;
      _itemcount = _max - _min + 1;
      if (_zipfianconstant == USED_ZIPFIAN_CONSTANT) {
        gen = new ZipfianGenerator(0, ITEM_COUNT, _zipfianconstant, ZETAN);
      } else {
        gen = new ZipfianGenerator(0, ITEM_COUNT, _zipfianconstant);
      }
    }

    public int nextInt() {
      return (int) nextLong();
    }

    public long nextLong() {
      var ret = gen.nextLong();
      ret = _min + FNVhash64(ret) % _itemcount;
      return ret;
    }

    static long FNVhash64(long val) {
      // from http://en.wikipedia.org/wiki/Fowler_Noll_Vo_hash
      var hashval = FNV_offset_basis_64;

      for (var i = 0; i < 8; i++) {
        var octet = val & 0x00ff;
        val = val >> 8;

        hashval = hashval ^ octet;
        hashval = hashval * FNV_prime_64;
      }
      return Math.abs(hashval);
    }
  }

  private static final class ZipfianGenerator {

    static final double ZIPFIAN_CONSTANT = 0.99;

    long items;

    long base;

    double zipfianconstant;

    double alpha, zetan, eta, theta, zeta2theta;

    long countforzeta;

    boolean allowitemcountdecrease = false;

    ZipfianGenerator(long min, long max, double _zipfianconstant) {
      this(min, max, _zipfianconstant, zetastatic(max - min + 1, _zipfianconstant));
    }

    ZipfianGenerator(long min, long max, double _zipfianconstant, double _zetan) {

      items = max - min + 1;
      base = min;
      zipfianconstant = _zipfianconstant;

      theta = zipfianconstant;

      zeta2theta = zeta(2, theta);

      alpha = 1.0 / (1.0 - theta);
      zetan = _zetan;
      countforzeta = items;
      eta = (1 - Math.pow(2.0 / items, 1 - theta)) / (1 - zeta2theta / zetan);

      nextInt();
    }

    double zeta(long n, double theta) {
      countforzeta = n;
      return zetastatic(n, theta);
    }

    static double zetastatic(long n, double theta) {
      return zetastatic(0, n, theta, 0);
    }

    double zeta(long st, long n, double theta, double initialsum) {
      countforzeta = n;
      return zetastatic(st, n, theta, initialsum);
    }

    static double zetastatic(long st, long n, double theta, double initialsum) {
      var sum = initialsum;
      for (var i = st; i < n; i++) {

        sum += 1 / (Math.pow(i + 1, theta));
      }
      return sum;
    }

    public int nextInt(int itemcount) {
      return (int) nextLong(itemcount);
    }

    public long nextLong(long itemcount) {
      // from "Quickly Generating Billion-Record Synthetic Databases", Jim Gray et al, SIGMOD 1994

      if (itemcount != countforzeta) {
        // have to recompute zetan and eta, since they depend on itemcount
        synchronized (this) {
          if (itemcount > countforzeta) {
            zetan = zeta(countforzeta, itemcount, theta, zetan);
            eta = (1 - Math.pow(2.0 / items, 1 - theta)) / (1 - zeta2theta / zetan);
          } else if ((itemcount < countforzeta) && (allowitemcountdecrease)) {
            System.err.println(
                "WARNING: Recomputing Zipfian distribtion. This is slow and should be avoided."
                    + " (itemcount="
                    + itemcount
                    + " countforzeta="
                    + countforzeta
                    + ")");

            zetan = zeta(itemcount, theta);
            eta = (1 - Math.pow(2.0 / items, 1 - theta)) / (1 - zeta2theta / zetan);
          }
        }
      }

      var u = ThreadLocalRandom.current().nextDouble();
      var uz = u * zetan;

      if (uz < 1.0) {
        return base;
      }

      if (uz < 1.0 + Math.pow(0.5, theta)) {
        return base + 1;
      }

      return base + (long) ((itemcount) * Math.pow(eta * u - eta + 1, alpha));
    }

    public int nextInt() {
      return (int) nextLong(items);
    }

    public long nextLong() {
      return nextLong(items);
    }
  }
}
