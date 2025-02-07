/*
 *
 *  *  Copyright YouTrackDB
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

import com.jetbrains.youtrack.db.internal.core.storage.cache.WriteCache;

/**
 * Defines per-page checksum modes supported by {@link WriteCache write caches}.
 */
public enum ChecksumMode {

  /**
   * Checksums are completely off.
   */
  Off,

  /**
   * Checksums are calculated and stored on page flushes, no verification is done on page loads,
   * stored checksums are verified only during user-initiated health checks.
   */
  Store,

  /**
   * Checksums are calculated and stored on page flushes, verification is performed on each page
   * load, errors are reported in the log.
   */
  StoreAndVerify,

  /**
   * Same as {@link ChecksumMode#StoreAndVerify} with addition of exceptions thrown on errors. This
   * mode is useful for debugging and testing, but should be avoided in a production environment.
   */
  StoreAndThrow,

  /**
   * Same as {@link ChecksumMode#StoreAndVerify} with addition that storage will be switched in read
   * only mode till it will not be repaired.
   */
  StoreAndSwitchReadOnlyMode
}
