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

import com.jetbrains.youtrack.db.api.record.Identifiable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Presentation of YouTrackDB index cursor for point and range queries. Cursor may iterate by several
 * elements even if you do point query (query by single key). It is possible if you use not unique
 * index.
 *
 * <p>Contract of cursor is simple it iterates in some subset of index data till it reaches it's
 * borders in such case {@link #nextEntry()} returns <code>null</code>.
 *
 * <p>Cursor is created as result of index query method such as {@link
 * Index#streamEntriesBetween(Object, boolean, Object,
 * boolean, boolean)} cursor instance cannot be used at several threads simultaneously.
 *
 * @since 4/4/14
 */
public interface IndexCursor extends Iterator<Identifiable> {

  /**
   * Returns nextEntry element in subset of index data which should be iterated by given cursor.
   *
   * @return nextEntry element in subset of index data which should be iterated by given cursor or
   * <code>null</code> if all data are iterated.
   */
  Map.Entry<Object, Identifiable> nextEntry();

  /**
   * Accumulates and returns all values of index inside of data subset of cursor.
   *
   * @return all values of index inside of data subset of cursor.
   */
  Set<Identifiable> toValues();

  /**
   * Accumulates and returns all entries of index inside of data subset of cursor.
   *
   * @return all entries of index inside of data subset of cursor.
   */
  Set<Map.Entry<Object, Identifiable>> toEntries();

  /**
   * Accumulates and returns all keys of index inside of data subset of cursor.
   *
   * @return all keys of index inside of data subset of cursor.
   */
  Set<Object> toKeys();

  /**
   * Set number of records to fetch for the next call to next() or nextEntry().
   *
   * @param prefetchSize Number of records to prefetch. -1 = prefetch using default settings.
   */
  void setPrefetchSize(int prefetchSize);
}
