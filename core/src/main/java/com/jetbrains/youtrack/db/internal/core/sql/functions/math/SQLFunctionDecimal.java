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
package com.jetbrains.youtrack.db.internal.core.sql.functions.math;

import com.jetbrains.youtrack.db.api.DatabaseSession;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Evaluates a complex expression.
 */
public class SQLFunctionDecimal extends SQLFunctionMathAbstract {

  public static final String NAME = "decimal";
  private Object result;

  public SQLFunctionDecimal() {
    super(NAME, 1, 1);
  }

  public Object execute(
      Object iThis,
      final Identifiable iRecord,
      final Object iCurrentResult,
      final Object[] iParams,
      CommandContext iContext) {
    var inputValue = iParams[0];
    if (inputValue == null) {
      result = null;
    }

    if (inputValue instanceof BigDecimal) {
      result = inputValue;
      return result;
    }
    if (inputValue instanceof BigInteger) {
      result = new BigDecimal((BigInteger) inputValue);
      return result;
    }
    if (inputValue instanceof Integer) {
      result = BigDecimal.valueOf(((Integer) inputValue));
      return result;
    }

    if (inputValue instanceof Long) {
      result = new BigDecimal(((Long) inputValue));
      return result;
    }

    if (inputValue instanceof Number) {
      result = BigDecimal.valueOf(((Number) inputValue).doubleValue());
      return result;
    }

    try {
      if (inputValue instanceof String) {
        result = new BigDecimal((String) inputValue);
      }

    } catch (Exception ignore) {
      result = null;
    }
    return result;
  }

  public boolean aggregateResults() {
    return false;
  }

  public String getSyntax(DatabaseSession session) {
    return "decimal(<val>)";
  }

  @Override
  public Object getResult() {
    return result;
  }
}
