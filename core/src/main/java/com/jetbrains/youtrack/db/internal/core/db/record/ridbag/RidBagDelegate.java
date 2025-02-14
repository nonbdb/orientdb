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

package com.jetbrains.youtrack.db.internal.core.db.record.ridbag;

import com.jetbrains.youtrack.db.internal.common.util.Sizeable;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.internal.core.db.record.TrackedMultiValue;
import com.jetbrains.youtrack.db.internal.core.db.record.RecordElement;
import com.jetbrains.youtrack.db.internal.core.record.impl.SimpleMultiValueTracker;
import com.jetbrains.youtrack.db.internal.core.storage.ridbag.sbtree.Change;
import java.util.Collection;
import java.util.NavigableMap;
import java.util.UUID;

public interface RidBagDelegate
    extends Iterable<Identifiable>,
    Sizeable,
    TrackedMultiValue<Identifiable, Identifiable>,
    RecordElement {

  void addAll(Collection<Identifiable> values);

  void add(Identifiable identifiable);

  void remove(Identifiable identifiable);

  boolean isEmpty();

  int getSerializedSize();

  /**
   * Writes content of bag to stream.
   *
   * <p>OwnerUuid is needed to notify db about changes of collection pointer if some happens during
   * serialization.
   *
   * @param stream    to write content
   * @param offset    in stream where start to write content
   * @param ownerUuid id of delegate owner
   * @return offset where content of stream is ended
   */
  int serialize(byte[] stream, int offset, UUID ownerUuid);

  int deserialize(byte[] stream, int offset);

  void requestDelete();

  /**
   * THIS IS VERY EXPENSIVE METHOD AND CAN NOT BE CALLED IN REMOTE STORAGE.
   *
   * @param identifiable Object to check.
   * @return true if ridbag contains at leas one instance with the same rid as passed in
   * identifiable.
   */
  boolean contains(Identifiable identifiable);

  void setOwner(RecordElement owner);

  RecordElement getOwner();

  String toString();

  NavigableMap<Identifiable, Change> getChanges();

  void setSize(int size);

  SimpleMultiValueTracker<Identifiable, Identifiable> getTracker();

  void setTracker(SimpleMultiValueTracker<Identifiable, Identifiable> tracker);

  void setTransactionModified(boolean transactionModified);
}
