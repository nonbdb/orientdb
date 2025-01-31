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

package com.jetbrains.youtrack.db.internal.common.util;

import com.jetbrains.youtrack.db.internal.common.log.LogManager;
import java.lang.reflect.Array;

@SuppressWarnings("unchecked")
public class ArrayUtils {

  public static <T> T[] copyOf(final T[] iSource, final int iNewSize) {
    return (T[]) copyOf(iSource, iNewSize, iSource.getClass());
  }

  public static <T, U> T[] copyOf(
      final U[] iSource, final int iNewSize, final Class<? extends T[]> iNewType) {
    final T[] copy;
    if (Object[].class.equals(iNewType)) {
      copy = (T[]) new Object[iNewSize];
    } else {
      copy = (T[]) Array.newInstance(iNewType.getComponentType(), iNewSize);
    }
    System.arraycopy(iSource, 0, copy, 0, Math.min(iSource.length, iNewSize));
    return copy;
  }

  public static <S> S[] copyOfRange(final S[] iSource, final int iBegin, final int iEnd) {
    return copyOfRange(iSource, iBegin, iEnd, (Class<S[]>) iSource.getClass());
  }

  public static <D, S> D[] copyOfRange(
      final S[] iSource, final int iBegin, final int iEnd, final Class<? extends D[]> iClass) {
    final var newLength = iEnd - iBegin;
    if (newLength < 0) {
      throw new IllegalArgumentException(iBegin + " > " + iEnd);
    }

    final D[] copy;
    if (Object[].class.equals(iClass)) {
      copy = (D[]) new Object[newLength];
    } else {
      copy = (D[]) Array.newInstance(iClass.getComponentType(), newLength);
    }

    System.arraycopy(iSource, iBegin, copy, 0, Math.min(iSource.length - iBegin, newLength));
    return copy;
  }

  public static byte[] copyOfRange(final byte[] iSource, final int iBegin, final int iEnd) {
    final var newLength = iEnd - iBegin;
    if (newLength < 0) {
      throw new IllegalArgumentException(iBegin + " > " + iEnd);
    }

    try {
      final var copy = new byte[newLength];
      System.arraycopy(iSource, iBegin, copy, 0, Math.min(iSource.length - iBegin, newLength));
      return copy;
    } catch (OutOfMemoryError e) {
      LogManager.instance()
          .error(ArrayUtils.class, "Error on copying buffer of size %d bytes", e, newLength);
      throw e;
    }
  }

  public static int[] copyOf(final int[] iSource, final int iNewSize) {
    final var copy = new int[iNewSize];
    System.arraycopy(iSource, 0, copy, 0, Math.min(iSource.length, iNewSize));
    return copy;
  }

  /**
   * Returns true if an arrays contains a value, otherwise false
   */
  public static boolean contains(final int[] iArray, final int iToFind) {
    if (iArray == null) {
      return false;
    }

    for (var e : iArray) {
      if (e == iToFind) {
        return true;
      }
    }

    return false;
  }

  /**
   * Returns true if an arrays contains a value, otherwise false
   */
  public static <T> boolean contains(final T[] iArray, final T iToFind) {
    if (iArray == null) {
      return false;
    }

    for (var e : iArray) {
      if (e != null && e.equals(iToFind)) {
        return true;
      }
    }

    return false;
  }

  public static int hash(final Object[] iArray) {
    var hash = 0;
    for (var o : iArray) {
      if (o != null) {
        hash += o.hashCode();
      }
    }
    return hash;
  }
}
