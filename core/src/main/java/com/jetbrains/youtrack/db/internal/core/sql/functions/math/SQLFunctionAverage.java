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

import com.jetbrains.youtrack.db.internal.common.collection.MultiValue;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.api.DatabaseSession;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.api.schema.PropertyType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Compute the average value for a field. Uses the context to save the last average number. When
 * different Number class are used, take the class with most precision.
 */
public class SQLFunctionAverage extends SQLFunctionMathAbstract {

  public static final String NAME = "avg";

  private Number sum;
  private int total = 0;

  public SQLFunctionAverage() {
    super(NAME, 1, -1);
  }

  public Object execute(
      Object iThis,
      Identifiable iCurrentRecord,
      Object iCurrentResult,
      final Object[] iParams,
      CommandContext iContext) {
    if (iParams.length == 1) {
      if (iParams[0] instanceof Number) {
        sum((Number) iParams[0]);
      } else if (MultiValue.isMultiValue(iParams[0])) {
        for (Object n : MultiValue.getMultiValueIterable(iParams[0])) {
          sum((Number) n);
        }
      }

    } else {
      sum = null;
      for (int i = 0; i < iParams.length; ++i) {
        sum((Number) iParams[i]);
      }
    }

    return getResult();
  }

  protected void sum(Number value) {
    if (value != null) {
      total++;
      if (sum == null)
      // FIRST TIME
      {
        sum = value;
      } else {
        sum = PropertyType.increment(sum, value);
      }
    }
  }

  public String getSyntax(DatabaseSession session) {
    return "avg(<field> [,<field>*])";
  }

  @Override
  public Object getResult() {
    if (returnDistributedResult()) {
      final Map<String, Object> map = new HashMap<String, Object>();
      map.put("sum", sum);
      map.put("total", total);
      return map;
    } else {
      return computeAverage(sum, total);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public Object mergeDistributedResult(final List<Object> resultsToMerge) {
    if (returnDistributedResult()) {
      Number dSum = null;
      int dTotal = 0;
      for (Object iParameter : resultsToMerge) {
        final Map<String, Object> item = (Map<String, Object>) iParameter;
        if (dSum == null) {
          dSum = (Number) item.get("sum");
        } else {
          dSum = PropertyType.increment(dSum, (Number) item.get("sum"));
        }

        dTotal += (Integer) item.get("total");
      }

      return computeAverage(dSum, dTotal);
    }

    if (!resultsToMerge.isEmpty()) {
      return resultsToMerge.get(0);
    }

    return null;
  }

  @Override
  public boolean aggregateResults() {
    return configuredParameters.length == 1;
  }

  private Object computeAverage(Number iSum, int iTotal) {
    if (iSum instanceof Integer) {
      return iSum.intValue() / iTotal;
    } else if (iSum instanceof Long) {
      return iSum.longValue() / iTotal;
    } else if (iSum instanceof Float) {
      return iSum.floatValue() / iTotal;
    } else if (iSum instanceof Double) {
      return iSum.doubleValue() / iTotal;
    } else if (iSum instanceof BigDecimal) {
      return ((BigDecimal) iSum).divide(new BigDecimal(iTotal), RoundingMode.HALF_UP);
    }

    return null;
  }
}
