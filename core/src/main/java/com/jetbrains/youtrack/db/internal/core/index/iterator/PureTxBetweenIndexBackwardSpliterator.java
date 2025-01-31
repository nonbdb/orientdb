package com.jetbrains.youtrack.db.internal.core.index.iterator;

import com.jetbrains.youtrack.db.internal.common.comparator.DefaultComparator;
import com.jetbrains.youtrack.db.internal.common.util.RawPair;
import com.jetbrains.youtrack.db.api.record.RID;
import com.jetbrains.youtrack.db.internal.core.index.IndexOneValue;
import com.jetbrains.youtrack.db.internal.core.tx.FrontendTransactionIndexChanges;
import java.util.Comparator;
import java.util.Spliterator;
import java.util.function.Consumer;

public class PureTxBetweenIndexBackwardSpliterator implements Spliterator<RawPair<Object, RID>> {

  /**
   *
   */
  private final IndexOneValue oIndexTxAwareOneValue;

  private final FrontendTransactionIndexChanges indexChanges;
  private Object firstKey;

  private Object nextKey;

  public PureTxBetweenIndexBackwardSpliterator(
      IndexOneValue oIndexTxAwareOneValue,
      Object fromKey,
      boolean fromInclusive,
      Object toKey,
      boolean toInclusive,
      FrontendTransactionIndexChanges indexChanges) {
    this.oIndexTxAwareOneValue = oIndexTxAwareOneValue;
    this.indexChanges = indexChanges;

    if (fromKey != null) {
      fromKey =
          this.oIndexTxAwareOneValue.enhanceFromCompositeKeyBetweenDesc(fromKey, fromInclusive);
    }
    if (toKey != null) {
      toKey = this.oIndexTxAwareOneValue.enhanceToCompositeKeyBetweenDesc(toKey, toInclusive);
    }

    final var keys = indexChanges.firstAndLastKeys(fromKey, fromInclusive, toKey, toInclusive);
    if (keys.length == 0) {
      nextKey = null;
    } else {
      firstKey = keys[0];
      nextKey = keys[1];
    }
  }

  @Override
  public boolean tryAdvance(Consumer<? super RawPair<Object, RID>> action) {
    if (nextKey == null) {
      return false;
    }

    RawPair<Object, RID> result;
    do {
      result = this.oIndexTxAwareOneValue.calculateTxIndexEntry(nextKey, null, indexChanges);
      nextKey = indexChanges.getLowerKey(nextKey);

      if (nextKey != null && DefaultComparator.INSTANCE.compare(nextKey, firstKey) < 0) {
        nextKey = null;
      }
    } while (result == null && nextKey != null);

    if (result == null) {
      return false;
    }

    action.accept(result);
    return true;
  }

  @Override
  public Spliterator<RawPair<Object, RID>> trySplit() {
    return null;
  }

  @Override
  public long estimateSize() {
    return Long.MAX_VALUE;
  }

  @Override
  public int characteristics() {
    return NONNULL | SORTED | ORDERED;
  }

  @Override
  public Comparator<? super RawPair<Object, RID>> getComparator() {
    return (entryOne, entryTwo) ->
        -DefaultComparator.INSTANCE.compare(entryOne.first, entryTwo.first);
  }
}
