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

package com.jetbrains.youtrack.db.internal.core.storage;

import com.jetbrains.youtrack.db.api.record.RID;

/**
 * @since 18.12.12
 */
public final class RecordMetadata {

  private final RID recordId;
  private final int recordVersion;

  public RecordMetadata(RID recordId, int recordVersion) {
    this.recordId = recordId;
    this.recordVersion = recordVersion;
  }

  public RID getRecordId() {
    return recordId;
  }

  public int getVersion() {
    return recordVersion;
  }
}
