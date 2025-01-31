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
package com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated;

import com.jetbrains.youtrack.db.api.exception.BaseException;
import com.jetbrains.youtrack.db.api.exception.DatabaseException;
import com.jetbrains.youtrack.db.api.record.RID;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseRecordThreadLocal;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.AbstractPaginatedStorage;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.atomicoperations.AtomicOperation;
import com.jetbrains.youtrack.db.internal.core.storage.ridbag.BTreeCollectionManager;
import com.jetbrains.youtrack.db.internal.core.storage.ridbag.BonsaiCollectionPointer;
import com.jetbrains.youtrack.db.internal.core.storage.ridbag.Change;
import com.jetbrains.youtrack.db.internal.core.storage.ridbag.ridbagbtree.EdgeBTree;
import java.io.IOException;
import java.util.Map;
import java.util.NavigableMap;

/**
 * @since 11/26/13
 */
public class RidBagUpdateSerializationOperation implements RecordSerializationOperation {

  private final NavigableMap<RID, Change> changedValues;

  private final BonsaiCollectionPointer collectionPointer;

  private final BTreeCollectionManager collectionManager;

  public RidBagUpdateSerializationOperation(
      final NavigableMap<RID, Change> changedValues,
      BonsaiCollectionPointer collectionPointer) {
    this.changedValues = changedValues;
    this.collectionPointer = collectionPointer;

    collectionManager = DatabaseRecordThreadLocal.instance().get().getSbTreeCollectionManager();
  }

  @Override
  public void execute(
      AtomicOperation atomicOperation, AbstractPaginatedStorage paginatedStorage) {
    if (changedValues.isEmpty()) {
      return;
    }

    var tree = loadTree();
    try {
      for (var entry : changedValues.entrySet()) {
        var storedCounter = tree.get(entry.getKey());

        storedCounter = entry.getValue().applyTo(storedCounter);
        if (storedCounter <= 0) {
          tree.remove(atomicOperation, entry.getKey());
        } else {
          tree.put(atomicOperation, entry.getKey(), storedCounter);
        }
      }
    } catch (IOException e) {
      throw BaseException.wrapException(new DatabaseException("Error during ridbag update"), e);
    } finally {
      releaseTree();
    }

    changedValues.clear();
  }

  private EdgeBTree<RID, Integer> loadTree() {
    return collectionManager.loadSBTree(collectionPointer);
  }

  private void releaseTree() {
    collectionManager.releaseSBTree(collectionPointer);
  }
}
