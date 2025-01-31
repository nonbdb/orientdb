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

import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.internal.core.sql.executor.ResultInternal;
import com.jetbrains.youtrack.db.internal.core.sql.functions.SQLFunctionRuntime;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Handles runtime results.
 */
public class RuntimeResult {

  private final Object fieldValue;
  private final Map<String, Object> projections;
  private final ResultInternal value;
  private final CommandContext context;

  public RuntimeResult(
      final Object iFieldValue,
      final Map<String, Object> iProjections,
      final int iProgressive,
      final CommandContext iContext) {
    fieldValue = iFieldValue;
    projections = iProjections;
    context = iContext;
    value = new ResultInternal(iContext.getDatabase());
  }


  private static boolean entriesPersistent(Collection<Identifiable> projectionValue) {
    for (var rec : projectionValue) {
      if (rec != null && !rec.getIdentity().isPersistent()) {
        return false;
      }
    }

    return true;
  }

  public static ResultInternal getResult(
      DatabaseSessionInternal session, final ResultInternal iValue,
      final Map<String, Object> iProjections) {
    if (iValue != null) {
      var canExcludeResult = false;

      for (var projection : iProjections.entrySet()) {
        if (!iValue.hasProperty(projection.getKey())) {
          // ONLY IF NOT ALREADY CONTAINS A VALUE, OTHERWISE HAS BEEN SET MANUALLY (INDEX?)
          final var v = projection.getValue();
          if (v instanceof SQLFunctionRuntime f) {
            canExcludeResult = f.filterResult();
            var fieldValue = f.getResult(session);
            if (fieldValue != null) {
              iValue.setProperty(projection.getKey(), fieldValue);
            }
          }
        }
      }

      if (canExcludeResult && iValue.getPropertyNames().isEmpty()) {
        // RESULT EXCLUDED FOR EMPTY RECORD
        return null;
      }
    }

    return iValue;
  }

  /**
   * Set a single value. This is useful in case of query optimization like with indexes
   *
   * @param iName  Field name
   * @param iValue Field value
   */
  public void applyValue(final String iName, final Object iValue) {
    value.setProperty(iName, iValue);
  }

  public ResultInternal getResult(DatabaseSessionInternal session) {
    return getResult(session, value, projections);
  }

  public Object getFieldValue() {
    return fieldValue;
  }
}
