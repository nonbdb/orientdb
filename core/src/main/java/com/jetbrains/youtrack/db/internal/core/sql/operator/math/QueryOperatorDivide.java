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
package com.jetbrains.youtrack.db.internal.core.sql.operator.math;

import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.api.DatabaseSession;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.api.record.RID;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.binary.EntitySerializer;
import com.jetbrains.youtrack.db.internal.core.sql.filter.SQLFilterCondition;
import com.jetbrains.youtrack.db.internal.core.sql.operator.IndexReuseType;
import com.jetbrains.youtrack.db.internal.core.sql.operator.QueryOperator;
import java.math.BigDecimal;
import java.util.Date;

/**
 * DIVIDE "/" operator.
 */
public class QueryOperatorDivide extends QueryOperator {

  public QueryOperatorDivide() {
    super("/", 10, false);
  }

  @Override
  public Object evaluateRecord(
      final Identifiable iRecord,
      EntityImpl iCurrentResult,
      final SQLFilterCondition iCondition,
      Object iLeft,
      Object iRight,
      CommandContext iContext,
      final EntitySerializer serializer) {
    if (iRight == null || iLeft == null) {
      return null;
    }

    if (iLeft instanceof Date) {
      iLeft = ((Date) iLeft).getTime();
    }
    if (iRight instanceof Date) {
      iRight = ((Date) iRight).getTime();
    }

    if (iLeft instanceof Number l && iRight instanceof Number r) {
      var maxPrecisionClass = QueryOperatorMultiply.getMaxPrecisionClass(l, r);
      if (Integer.class.equals(maxPrecisionClass)) {
        return l.intValue() / r.intValue();
      } else if (Long.class.equals(maxPrecisionClass)) {
        return l.longValue() / r.longValue();
      } else if (Short.class.equals(maxPrecisionClass)) {
        return l.shortValue() / r.shortValue();
      } else if (Float.class.equals(maxPrecisionClass)) {
        return l.floatValue() / r.floatValue();
      } else if (Double.class.equals(maxPrecisionClass)) {
        return l.doubleValue() / r.doubleValue();
      } else if (BigDecimal.class.equals(maxPrecisionClass)) {
        return (QueryOperatorMultiply.toBigDecimal(l))
            .divide(QueryOperatorMultiply.toBigDecimal(r));
      }
    }

    return null;
  }

  @Override
  public IndexReuseType getIndexReuseType(Object iLeft, Object iRight) {
    return IndexReuseType.NO_INDEX;
  }

  @Override
  public RID getBeginRidRange(DatabaseSession session, Object iLeft, Object iRight) {
    return null;
  }

  @Override
  public RID getEndRidRange(DatabaseSession session, Object iLeft, Object iRight) {
    return null;
  }
}
