/**
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * <p>*
 */
package com.jetbrains.youtrack.db.internal.spatial.functions;

import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.api.DatabaseSession;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLBinaryCompareOperator;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLExpression;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLFromClause;
import com.jetbrains.youtrack.db.internal.spatial.strategy.SpatialQueryBuilderDWithin;
import java.util.Map;
import org.locationtech.spatial4j.shape.Shape;

/**
 *
 */
public class STDWithinFunction extends SpatialFunctionAbstractIndexable {

  public static final String NAME = "st_dwithin";

  public STDWithinFunction() {
    super(NAME, 3, 3);
  }

  @Override
  public Object execute(
      Object iThis,
      Identifiable iCurrentRecord,
      Object iCurrentResult,
      Object[] iParams,
      CommandContext iContext) {

    if (containsNull(iParams)) {
      return null;
    }
    var shape = factory.fromObject(iParams[0]);

    var shape1 = factory.fromObject(iParams[1]);

    var distance = (Number) iParams[2];

    return factory.operation().isWithInDistance(shape, shape1, distance.doubleValue());
  }

  @Override
  public String getSyntax(DatabaseSession session) {
    return null;
  }

  @Override
  public Iterable<Identifiable> searchFromTarget(
      SQLFromClause target,
      SQLBinaryCompareOperator operator,
      Object rightValue,
      CommandContext ctx,
      SQLExpression... args) {
    return results(target, args, ctx, rightValue);
  }

  @Override
  protected void onAfterParsing(
      Map<String, Object> params, SQLExpression[] args, CommandContext ctx, Object rightValue) {

    var number = args[2];

    var parsedNumber = (Number) number.execute((Identifiable) null, ctx);

    params.put("distance", parsedNumber.doubleValue());
  }

  @Override
  protected String operator() {
    return SpatialQueryBuilderDWithin.NAME;
  }
}
