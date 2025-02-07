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
package com.jetbrains.youtrack.db.internal.common.comparator;

import java.util.Comparator;

/**
 * Comparator for byte arrays comparison. Bytes are compared like unsigned not like signed bytes.
 *
 * @since 03.07.12
 */
public class ByteArrayComparator implements Comparator<byte[]> {

  public static final ByteArrayComparator INSTANCE = new ByteArrayComparator();

  public int compare(final byte[] arrayOne, final byte[] arrayTwo) {
    final int lenDiff = arrayOne.length - arrayTwo.length;

    if (lenDiff != 0) {
      return lenDiff;
    }

    for (int i = 0; i < arrayOne.length; i++) {
      final int valOne = arrayOne[i] & 0xFF;
      final int valTwo = arrayTwo[i] & 0xFF;

      final int diff = valOne - valTwo;
      if (diff != 0) {
        return diff;
      }
    }

    return 0;
  }
}
