package com.jetbrains.youtrack.db.internal.core.storage.index.hashindex.local.v2;

import com.jetbrains.youtrack.db.internal.core.exception.CommandInterruptedException;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.AbstractPaginatedStorage;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.atomicoperations.AtomicOperation;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.atomicoperations.AtomicOperationsManager;
import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public abstract class LocalHashTableV2Base {

  private static final int KEYS_COUNT = 1_000_000;
  LocalHashTableV2<Integer, String> localHashTable;
  protected AbstractPaginatedStorage storage;

  @Test
  public void testKeyPut() throws IOException {
    doInRollbackLoop(
        0,
        KEYS_COUNT,
        100,
        (value, rollback, atomicOperation) ->
            localHashTable.put(atomicOperation, value, String.valueOf(value)));

    for (int i = 0; i < KEYS_COUNT; i++) {
      Assert.assertEquals(i + " key is absent", localHashTable.get(i), String.valueOf(i));
    }

    for (int i = KEYS_COUNT; i < 2 * KEYS_COUNT; i++) {
      Assert.assertNull(localHashTable.get(i));
    }
  }

  @Test
  public void testKeyPutRandomUniform() throws IOException {
    final Set<Integer> keys = new HashSet<>();
    final Random random = new Random();

    final AtomicOperationsManager manager = storage.getAtomicOperationsManager();
    while (keys.size() < KEYS_COUNT) {
      final int key = random.nextInt();

      for (int k = 0; k < 2; k++) {
        final AtomicOperation atomicOperation = manager.startAtomicOperation(null);
        localHashTable.put(atomicOperation, key, String.valueOf(key));
        manager.endAtomicOperation(k == 0 ? new CommandInterruptedException("") : null);
      }

      keys.add(key);
      Assert.assertEquals(localHashTable.get(key), String.valueOf(key));
    }

    for (int key : keys) {
      Assert.assertEquals(localHashTable.get(key), String.valueOf(key));
    }
  }

  @Test
  public void testKeyPutRandomGaussian() throws IOException {
    final Set<Integer> keys = new HashSet<>();
    Random random = new Random();

    final AtomicOperationsManager manager = storage.getAtomicOperationsManager();
    while (keys.size() < KEYS_COUNT) {
      final int key = (int) (random.nextGaussian() * Integer.MAX_VALUE / 2 + Integer.MAX_VALUE);

      for (int k = 0; k < 2; k++) {
        final AtomicOperation atomicOperation = manager.startAtomicOperation(null);
        localHashTable.put(atomicOperation, key, String.valueOf(key));
        manager.endAtomicOperation(k == 0 ? new CommandInterruptedException("") : null);
      }
      keys.add(key);
      Assert.assertEquals(localHashTable.get(key), String.valueOf(key));
    }

    for (final int key : keys) {
      Assert.assertEquals(localHashTable.get(key), String.valueOf(key));
    }
  }

  @Test
  public void testKeyDeleteRandomUniform() throws IOException {
    final Set<Integer> keys = new HashSet<>();
    long ms = System.currentTimeMillis();
    System.out.println("testKeyDelete : " + ms);
    final Random random = new Random(ms);

    final AtomicOperationsManager manager = storage.getAtomicOperationsManager();
    while (keys.size() < KEYS_COUNT) {
      final int key = random.nextInt();

      for (int k = 0; k < 2; k++) {
        final AtomicOperation atomicOperation = manager.startAtomicOperation(null);
        localHashTable.put(atomicOperation, key, String.valueOf(key));
        manager.endAtomicOperation(k == 0 ? new CommandInterruptedException("") : null);
      }

      keys.add(key);
      Assert.assertEquals(localHashTable.get(key), String.valueOf(key));
    }

    for (final int key : keys) {
      if (key % 3 == 0) {
        for (int k = 0; k < 2; k++) {
          final AtomicOperation atomicOperation = manager.startAtomicOperation(null);
          localHashTable.remove(atomicOperation, key);
          manager.endAtomicOperation(k == 0 ? new CommandInterruptedException("") : null);
        }
      }
    }

    for (final int key : keys) {
      if (key % 3 == 0) {
        Assert.assertNull(localHashTable.get(key));
      } else {
        Assert.assertEquals(localHashTable.get(key), String.valueOf(key));
      }
    }
  }

  @Test
  public void testKeyDelete() throws IOException {
    doInRollbackLoop(
        0,
        KEYS_COUNT,
        100,
        (value, rollback, atomicOperation) ->
            localHashTable.put(atomicOperation, value, String.valueOf(value)));

    doInRollbackLoop(
        0,
        KEYS_COUNT,
        100,
        (value, rollback, atomicOperation) -> {
          if (value % 3 == 0) {
            Assert.assertEquals(
                localHashTable.remove(atomicOperation, value), String.valueOf(value));
          }
        });

    for (int i = 0; i < KEYS_COUNT; i++) {
      if (i % 3 == 0) {
        Assert.assertNull(localHashTable.get(i));
      } else {
        Assert.assertEquals(localHashTable.get(i), String.valueOf(i));
      }
    }
  }

  @Test
  public void testKeyAddDelete() throws IOException {
    doInRollbackLoop(
        0,
        KEYS_COUNT,
        100,
        (value, rollback, atomicOperation) ->
            localHashTable.put(atomicOperation, value, String.valueOf(value)));

    doInRollbackLoop(
        0,
        KEYS_COUNT,
        100,
        (value, rollback, atomicOperation) -> {
          if (value % 3 == 0) {
            Assert.assertEquals(
                localHashTable.remove(atomicOperation, value), String.valueOf(value));
          }

          if (value % 2 == 0) {
            localHashTable.put(
                atomicOperation, KEYS_COUNT + value, String.valueOf(KEYS_COUNT + value));
          }
        });

    for (int i = 0; i < KEYS_COUNT; i++) {
      if (i % 3 == 0) {
        Assert.assertNull(localHashTable.get(i));
      } else {
        Assert.assertEquals(localHashTable.get(i), String.valueOf(i));
      }

      if (i % 2 == 0) {
        Assert.assertEquals(localHashTable.get(KEYS_COUNT + i), String.valueOf(KEYS_COUNT + i));
      }
    }
  }

  @Test
  public void testKeyPutRemoveNullKey() throws IOException {
    doInRollbackLoop(
        0,
        10,
        1,
        (value, rollback, atomicOperation) ->
            localHashTable.put(atomicOperation, value, String.valueOf(value)));

    final AtomicOperationsManager manager = storage.getAtomicOperationsManager();

    for (int k = 0; k < 2; k++) {
      final AtomicOperation atomicOperation = manager.startAtomicOperation(null);
      localHashTable.put(atomicOperation, null, "null");
      manager.endAtomicOperation(k == 0 ? new CommandInterruptedException("") : null);
    }

    for (int i = 0; i < 10; i++) {
      Assert.assertEquals(localHashTable.get(i), String.valueOf(i));
    }

    Assert.assertEquals(localHashTable.get(null), "null");

    doInRollbackLoop(
        0,
        5,
        1,
        (value, rollback, atomicOperation) ->
            Assert.assertEquals(
                localHashTable.remove(atomicOperation, value), String.valueOf(value)));

    for (int k = 0; k < 2; k++) {
      final AtomicOperation atomicOperation = manager.startAtomicOperation(null);
      Assert.assertEquals(localHashTable.remove(atomicOperation, null), "null");
      manager.endAtomicOperation(k == 0 ? new CommandInterruptedException("") : null);
    }

    for (int i = 0; i < 5; i++) {
      final int key = i;
      manager.executeInsideAtomicOperation(
          null, atomicOperation -> Assert.assertNull(localHashTable.remove(atomicOperation, key)));
    }

    manager.executeInsideAtomicOperation(
        null, atomicOperation -> Assert.assertNull(localHashTable.remove(atomicOperation, null)));

    for (int i = 0; i < 5; i++) {
      Assert.assertNull(localHashTable.get(i));
    }

    Assert.assertNull(localHashTable.get(null));

    for (int i = 5; i < 10; i++) {
      Assert.assertEquals(localHashTable.get(i), String.valueOf(i));
    }
  }

  @Test
  public void testKeyDeleteRandomGaussian() throws IOException {
    HashSet<Integer> keys = new HashSet<>();

    final AtomicOperationsManager manager = storage.getAtomicOperationsManager();
    final Random random = new Random();
    while (keys.size() < KEYS_COUNT) {
      final int key = (int) (random.nextGaussian() * Integer.MAX_VALUE / 2 + Integer.MAX_VALUE);

      for (int k = 0; k < 2; k++) {
        final AtomicOperation atomicOperation = manager.startAtomicOperation(null);
        localHashTable.put(atomicOperation, key, String.valueOf(key));
        manager.endAtomicOperation(k == 0 ? new CommandInterruptedException("") : null);
      }

      keys.add(key);
    }

    for (final int key : keys) {
      if (key % 3 == 0) {
        for (int k = 0; k < 2; k++) {
          final AtomicOperation atomicOperation = manager.startAtomicOperation(null);
          localHashTable.remove(atomicOperation, key);
          manager.endAtomicOperation(k == 0 ? new CommandInterruptedException("") : null);
        }
      }
    }

    for (final int key : keys) {
      if (key % 3 == 0) {
        Assert.assertNull(localHashTable.get(key));
      } else {
        Assert.assertEquals(localHashTable.get(key), String.valueOf(key));
      }
    }
  }

  @SuppressWarnings("SameParameterValue")
  private void doInRollbackLoop(
      final int start, final int end, final int rollbackSlice, final TxCode code)
      throws IOException {
    final AtomicOperationsManager atomicOperationsManager = storage.getAtomicOperationsManager();

    for (int i = start; i < end; i += rollbackSlice) {
      for (int k = 0; k < 2; k++) {
        final AtomicOperation atomicOperation = atomicOperationsManager.startAtomicOperation(null);

        int counter = 0;
        while (counter < rollbackSlice && i + counter < end) {
          code.execute(i + counter, k == 0, atomicOperation);

          counter++;
        }

        atomicOperationsManager.endAtomicOperation(
            k == 0 ? new CommandInterruptedException("") : null);
      }
    }
  }

  private interface TxCode {

    void execute(int value, boolean rollback, AtomicOperation atomicOperation) throws IOException;
  }
}
