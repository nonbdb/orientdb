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
package com.orientechnologies.orient.core.db.record;

import java.util.Iterator;

/**
 * Implementation of Iterator that keeps track of changes to the source record avoiding to call
 * setDirty() by hand.
 */
public class ORecordTrackedIterator implements Iterator<YTIdentifiable> {

  private final RecordElement sourceRecord;
  private final Iterator<?> underlying;

  public ORecordTrackedIterator(final RecordElement iSourceRecord, final Iterator<?> iIterator) {
    this.sourceRecord = iSourceRecord;
    this.underlying = iIterator;
  }

  public YTIdentifiable next() {
    return (YTIdentifiable) underlying.next();
  }

  public boolean hasNext() {
    return underlying.hasNext();
  }

  public void remove() {
    underlying.remove();
    if (sourceRecord != null) {
      sourceRecord.setDirty();
    }
  }
}
