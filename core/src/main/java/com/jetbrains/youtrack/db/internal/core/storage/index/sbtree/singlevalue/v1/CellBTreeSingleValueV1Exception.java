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

package com.jetbrains.youtrack.db.internal.core.storage.index.sbtree.singlevalue.v1;

import com.jetbrains.youtrack.db.internal.core.exception.DurableComponentException;

/**
 * @since 8/30/13
 */
public final class CellBTreeSingleValueV1Exception extends DurableComponentException {

  @SuppressWarnings("unused")
  public CellBTreeSingleValueV1Exception(final CellBTreeSingleValueV1Exception exception) {
    super(exception);
  }

  CellBTreeSingleValueV1Exception(final String message, final CellBTreeSingleValueV1 component) {
    super(message, component);
  }
}
