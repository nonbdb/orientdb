package com.jetbrains.youtrack.db.internal.core.index.engine;

import com.jetbrains.youtrack.db.api.exception.RecordDuplicatedException;
import com.jetbrains.youtrack.db.api.record.RID;
import com.jetbrains.youtrack.db.internal.core.index.IndexInternal;
import com.jetbrains.youtrack.db.internal.core.index.IndexUnique;

public class UniqueIndexEngineValidator implements IndexEngineValidator<Object, RID> {

  /**
   *
   */
  private final IndexUnique indexUnique;

  /**
   * @param oIndexUnique
   */
  public UniqueIndexEngineValidator(IndexUnique oIndexUnique) {
    indexUnique = oIndexUnique;
  }

  @Override
  public Object validate(Object key, RID oldValue, RID newValue) {
    if (oldValue != null) {
      var metadata = indexUnique.getMetadata();
      // CHECK IF THE ID IS THE SAME OF CURRENT: THIS IS THE UPDATE CASE
      if (!oldValue.equals(newValue)) {
        final var mergeSameKey =
            metadata != null ? (Boolean) metadata.get(IndexInternal.MERGE_KEYS) : Boolean.FALSE;
        if (mergeSameKey == null || !mergeSameKey) {
          throw new RecordDuplicatedException(
              String.format(
                  "Cannot index record %s: found duplicated key '%s' in index '%s' previously"
                      + " assigned to the record %s",
                  newValue.getIdentity(), key, indexUnique.getName(), oldValue.getIdentity()),
              indexUnique.getName(),
              oldValue.getIdentity(),
              key);
        }
      } else {
        return IndexEngineValidator.IGNORE;
      }
    }

    assert newValue.isPersistent();
    return newValue.getIdentity();
  }
}
