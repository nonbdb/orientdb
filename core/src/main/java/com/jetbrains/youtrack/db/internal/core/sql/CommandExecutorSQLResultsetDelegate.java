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
package com.jetbrains.youtrack.db.internal.core.sql;

import com.jetbrains.youtrack.db.internal.core.db.DatabaseRecordThreadLocal;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import java.util.Iterator;
import java.util.Map;

/**
 * SQL UPDATE command.
 */
@SuppressWarnings("unchecked")
public class CommandExecutorSQLResultsetDelegate extends CommandExecutorSQLDelegate
    implements IterableRecordSource, Iterable<Identifiable> {

  @Override
  public Iterator<Identifiable> iterator() {
    return ((IterableRecordSource) delegate).iterator(DatabaseRecordThreadLocal.instance().get(),
        null);
  }

  @Override
  public Iterator<Identifiable> iterator(DatabaseSessionInternal querySession,
      final Map<Object, Object> iArgs) {
    return ((IterableRecordSource) delegate).iterator(querySession, iArgs);
  }
}
