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
 * Evaluates the absolute value for numeric types. The argument must be a BigDecimal, BigInteger,
 * Integer, Long, Double or a Float, or null. If null is passed in the result will be null.
 * Otherwise the result will be the mathematical absolute value of the argument passed in and will
 * be of the same type that was passed in.
 */
public class SQLFunctionAbsoluteValue extends SQLFunctionMathAbstract {

  public static final String NAME = "abs";
  private Object result;

  public SQLFunctionAbsoluteValue() {
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
    } else if (inputValue instanceof BigDecimal) {
      result = ((BigDecimal) inputValue).abs();
    } else if (inputValue instanceof BigInteger) {
      result = ((BigInteger) inputValue).abs();
    } else if (inputValue instanceof Integer) {
      result = Math.abs((Integer) inputValue);
    } else if (inputValue instanceof Long) {
      result = Math.abs((Long) inputValue);
    } else if (inputValue instanceof Short) {
      result = (short) Math.abs((Short) inputValue);
    } else if (inputValue instanceof Double) {
      result = Math.abs((Double) inputValue);
    } else if (inputValue instanceof Float) {
      result = Math.abs((Float) inputValue);
    } else {
      throw new IllegalArgumentException("Argument to absolute value must be a number.");
    }

    return result;
  }

  public boolean aggregateResults() {
    return false;
  }

  public String getSyntax(DatabaseSession session) {
    return "abs(<number>)";
  }

  @Override
  public Object getResult() {
    return result;
  }
}
