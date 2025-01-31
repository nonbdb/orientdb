/*
 *
 * Copyright 2013 Geomatys.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.youtrack.db.internal.core.sql.method.misc;

import com.jetbrains.youtrack.db.internal.common.util.Sizeable;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Transforms current value in a Set.
 */
public class SQLMethodAsSet extends AbstractSQLMethod {

  public static final String NAME = "asset";

  public SQLMethodAsSet() {
    super(NAME);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Object execute(
      Object iThis,
      Identifiable iCurrentRecord,
      CommandContext iContext,
      Object ioResult,
      Object[] iParams) {
    if (ioResult instanceof Set)
    // ALREADY A SET
    {
      return ioResult;
    }

    if (ioResult == null)
    // NULL VALUE, RETURN AN EMPTY SET
    {
      return Collections.EMPTY_SET;
    }

    if (ioResult instanceof Collection<?>) {
      return new LinkedHashSet<Object>((Collection<Object>) ioResult);
    } else if (!(ioResult instanceof EntityImpl) && ioResult instanceof Iterable<?>) {
      ioResult = ((Iterable<?>) ioResult).iterator();
    }

    if (ioResult instanceof Iterator<?>) {
      final Set<Object> set;
      if (ioResult instanceof Sizeable) {
        set = new LinkedHashSet<Object>(((Sizeable) ioResult).size());
      } else {
        set = new LinkedHashSet<Object>();
      }

      for (var iter = (Iterator<Object>) ioResult; iter.hasNext(); ) {
        set.add(iter.next());
      }
      return set;
    }

    // SINGLE ITEM: ADD IT AS UNIQUE ITEM
    return Collections.singleton(ioResult);
  }
}
