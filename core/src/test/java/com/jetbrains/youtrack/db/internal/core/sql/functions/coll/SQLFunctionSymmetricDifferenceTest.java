/*
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.youtrack.db.internal.core.sql.functions.coll;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.jetbrains.youtrack.db.internal.core.command.BasicCommandContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

/**
 * @since 11.10.12 14:40
 */
public class SQLFunctionSymmetricDifferenceTest {

  @Test
  public void testOperator() {
    final SQLFunctionSymmetricDifference differenceFunction =
        new SQLFunctionSymmetricDifference() {
          @Override
          protected boolean returnDistributedResult() {
            return false;
          }
        };

    final List<Object> income = Arrays.asList(1, 2, 3, 1, 4, 5, 2, 2, 1, 1);
    final Set<Object> expectedResult = new HashSet<Object>(Arrays.asList(3, 4, 5));

    for (Object i : income) {
      differenceFunction.execute(null, null, null, new Object[]{i}, null);
    }

    final Set<Object> actualResult = differenceFunction.getResult();

    assertSetEquals(actualResult, expectedResult);
  }

  @Test
  public void testOperatorMerge() {
    final SQLFunctionSymmetricDifference merger =
        new SQLFunctionSymmetricDifference() {
          @Override
          protected boolean returnDistributedResult() {
            return true;
          }
        };

    final List<SQLFunctionSymmetricDifference> differences =
        new ArrayList<SQLFunctionSymmetricDifference>(3);
    for (int i = 0; i < 3; i++) {
      differences.add(
          new SQLFunctionSymmetricDifference() {
            @Override
            protected boolean returnDistributedResult() {
              return true;
            }
          });
    }

    final List<List<Object>> incomes =
        Arrays.asList(
            Arrays.asList(1, 2, 3, 4, 5, 1),
            Arrays.asList(3, 5, 6, 7, 0, 1, 3, 3, 6),
            Arrays.asList(2, 2, 8, 9));

    final Set<Object> expectedResult = new HashSet<Object>(Arrays.<Object>asList(4, 7, 8, 9, 0));

    for (int j = 0; j < 3; j++) {
      for (Object i : incomes.get(j)) {
        differences.get(j).execute(null, null, null, new Object[]{i}, null);
      }
    }

    final Set<Object> actualResult =
        (Set<Object>)
            merger.mergeDistributedResult(
                Arrays.asList(
                    differences.get(0).getResult(),
                    differences.get(1).getResult(),
                    differences.get(2).getResult()));

    assertSetEquals(actualResult, expectedResult);
  }

  @Test
  public void testExecute() {
    final SQLFunctionSymmetricDifference function = new SQLFunctionSymmetricDifference();

    final List<List<Object>> incomes =
        Arrays.asList(
            Arrays.asList(1, 2, 3, 4, 5, 1),
            Arrays.asList(3, 5, 6, 7, 0, 1, 3, 3, 6),
            Arrays.asList(2, 2, 8, 9));

    final Set<Object> expectedResult = new HashSet<Object>(Arrays.<Object>asList(4, 7, 8, 9, 0));

    final Set<Object> actualResult =
        (Set<Object>)
            function.execute(null, null, null, incomes.toArray(), new BasicCommandContext());

    assertSetEquals(actualResult, expectedResult);
  }

  private void assertSetEquals(Set<Object> actualResult, Set<Object> expectedResult) {
    assertEquals(actualResult.size(), expectedResult.size());
    for (Object o : actualResult) {
      assertTrue(expectedResult.contains(o));
    }
  }
}