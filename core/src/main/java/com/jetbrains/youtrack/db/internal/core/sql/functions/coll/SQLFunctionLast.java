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

import com.jetbrains.youtrack.db.internal.common.collection.MultiValue;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.api.DatabaseSession;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.internal.core.sql.filter.SQLFilterItem;
import com.jetbrains.youtrack.db.internal.core.sql.functions.SQLFunctionConfigurableAbstract;

/**
 * Extract the last item of multi values (arrays, collections and maps) or return the same value for
 * non multi-value types.
 */
public class SQLFunctionLast extends SQLFunctionConfigurableAbstract {

  public static final String NAME = "last";

  public SQLFunctionLast() {
    super(NAME, 1, 1);
  }

  public Object execute(
      Object iThis,
      final Identifiable iCurrentRecord,
      Object iCurrentResult,
      final Object[] iParams,
      final CommandContext iContext) {
    var value = iParams[0];

    if (value instanceof SQLFilterItem) {
      value = ((SQLFilterItem) value).getValue(iCurrentRecord, iCurrentResult, iContext);
    }

    if (MultiValue.isMultiValue(value)) {
      value = MultiValue.getLastValue(value);
    }

    return value;
  }

  public String getSyntax(DatabaseSession session) {
    return "last(<field>)";
  }
}
