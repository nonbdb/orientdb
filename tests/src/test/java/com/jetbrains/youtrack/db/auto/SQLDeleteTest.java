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
package com.jetbrains.youtrack.db.auto;

import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.api.query.ResultSet;
import org.testng.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

@Test(groups = "sql-delete")
public class SQLDeleteTest extends BaseDBTest {

  @Parameters(value = "remote")
  public SQLDeleteTest(boolean remote) {
    super(remote);
  }

  @Test
  public void deleteWithWhereOperator() {
    db.begin();
    db.command("insert into Profile (sex, salary) values ('female', 2100)").close();
    db.commit();

    final Long total = db.countClass("Profile");

    var resultset =
        db.query("select from Profile where sex = 'female' and salary = 2100");
    var queryCount = resultset.stream().count();

    db.begin();
    var result =
        db.command("delete from Profile where sex = 'female' and salary = 2100");
    db.commit();
    long count = result.next().getProperty("count");

    Assert.assertEquals(count, queryCount);

    Assert.assertEquals(db.countClass("Profile"), total - count);
  }

  @Test
  public void deleteInPool() {
    var db = acquireSession();

    final Long total = db.countClass("Profile");

    var resultset =
        db.query("select from Profile where sex = 'male' and salary > 120 and salary <= 133");

    var queryCount = resultset.stream().count();

    db.begin();
    var records =
        db.command("delete from Profile where sex = 'male' and salary > 120 and salary <= 133");
    db.commit();

    long count = records.next().getProperty("count");
    Assert.assertEquals(count, queryCount);

    Assert.assertEquals(db.countClass("Profile"), total - count);

    db.close();
  }
}
