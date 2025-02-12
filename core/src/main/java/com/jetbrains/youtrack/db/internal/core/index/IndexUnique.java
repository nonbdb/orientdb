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

import com.jetbrains.youtrack.db.api.record.RID;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.exception.InvalidIndexEngineIdException;
import com.jetbrains.youtrack.db.internal.core.index.engine.IndexEngineValidator;
import com.jetbrains.youtrack.db.internal.core.index.engine.UniqueIndexEngineValidator;
import com.jetbrains.youtrack.db.internal.core.storage.Storage;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.AbstractPaginatedStorage;
import com.jetbrains.youtrack.db.internal.core.tx.FrontendTransactionIndexChangesPerKey;
import com.jetbrains.youtrack.db.internal.core.tx.FrontendTransactionIndexChangesPerKey.TransactionIndexEntry;

/**
 * Index implementation that allows only one value for a key.
 */
public class IndexUnique extends IndexOneValue {

  private final IndexEngineValidator<Object, RID> uniqueValidator =
      new UniqueIndexEngineValidator(this);

  public IndexUnique(IndexMetadata im, final Storage storage) {
    super(im, storage);
  }

  @Override
  public boolean isNativeTxSupported() {
    return true;
  }

  @Override
  public void doPut(DatabaseSessionInternal session, AbstractPaginatedStorage storage,
      Object key,
      RID rid)
      throws InvalidIndexEngineIdException {
    storage.validatedPutIndexValue(indexId, key, rid, uniqueValidator);
  }

  @Override
  public boolean canBeUsedInEqualityOperators() {
    return true;
  }

  @Override
  public boolean supportsOrderedIterations() {
    while (true) {
      try {
        return storage.hasIndexRangeQuerySupport(indexId);
      } catch (InvalidIndexEngineIdException ignore) {
        doReloadIndexEngine();
      }
    }
  }

  @Override
  public Iterable<TransactionIndexEntry> interpretTxKeyChanges(
      FrontendTransactionIndexChangesPerKey changes) {
    return changes.interpret(FrontendTransactionIndexChangesPerKey.Interpretation.Unique);
  }
}
