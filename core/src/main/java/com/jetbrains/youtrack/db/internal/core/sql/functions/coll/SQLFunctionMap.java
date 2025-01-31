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
package com.jetbrains.youtrack.db.internal.core.sql.functions.coll;

import com.jetbrains.youtrack.db.api.DatabaseSession;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import java.util.HashMap;
import java.util.Map;

/**
 * This operator add an entry in a map. The entry is composed by a key and a value.
 */
public class SQLFunctionMap extends SQLFunctionMultiValueAbstract<Map<Object, Object>> {

  public static final String NAME = "map";

  public SQLFunctionMap() {
    super(NAME, 1, -1);
  }

  @SuppressWarnings("unchecked")
  public Object execute(
      Object iThis,
      final Identifiable iCurrentRecord,
      Object iCurrentResult,
      final Object[] iParams,
      CommandContext iContext) {

    if (iParams.length > 2)
    // IN LINE MODE
    {
      context = new HashMap<Object, Object>();
    }

    if (iParams.length == 1) {
      if (iParams[0] == null) {
        return null;
      }

      if (iParams[0] instanceof Map<?, ?>) {
        if (context == null)
        // AGGREGATION MODE (STATEFULL)
        {
          context = new HashMap<Object, Object>();
        }

        // INSERT EVERY SINGLE COLLECTION ITEM
        context.putAll((Map<Object, Object>) iParams[0]);
      } else {
        throw new IllegalArgumentException(
            "Map function: expected a map or pairs of parameters as key, value");
      }
    } else if (iParams.length % 2 != 0) {
      throw new IllegalArgumentException(
          "Map function: expected a map or pairs of parameters as key, value");
    } else {
      for (var i = 0; i < iParams.length; i += 2) {
        final var key = iParams[i];
        final var value = iParams[i + 1];

        if (value != null) {
          if (iParams.length <= 2 && context == null)
          // AGGREGATION MODE (STATEFULL)
          {
            context = new HashMap<Object, Object>();
          }

          context.put(key, value);
        }
      }
    }

    return prepareResult(context);
  }

  public String getSyntax(DatabaseSession session) {
    return "map(<map>|[<key>,<value>]*)";
  }

  public boolean aggregateResults(final Object[] configuredParameters) {
    return configuredParameters.length <= 2;
  }

  @Override
  public boolean aggregateResults() {
    return configuredParameters.length <= 2;
  }

  @Override
  public Map<Object, Object> getResult() {
    final var res = context;
    context = null;
    return prepareResult(res);
  }

  protected Map<Object, Object> prepareResult(final Map<Object, Object> res) {
    return res;
  }
}
