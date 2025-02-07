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
package com.jetbrains.youtrack.db.api.schema;

import com.jetbrains.youtrack.db.internal.common.comparator.DefaultComparator;
import com.jetbrains.youtrack.db.internal.core.collate.CaseInsensitiveCollate;
import com.jetbrains.youtrack.db.internal.core.collate.DefaultCollate;
import java.io.Serializable;

/**
 * Specify the Collating strategy when comparison in SQL statement is required.
 */
public interface Collate extends Serializable {

  static Collate caseInsensitiveCollate() {
    return new CaseInsensitiveCollate();
  }
  static Collate defaultCollate() {
    return new DefaultCollate();
  }

  String getName();

  Object transform(Object obj);

  default int compareForOrderBy(Object o1, Object o2) {
    return new DefaultComparator().compare(transform(o1), transform(o2));
  }
}
