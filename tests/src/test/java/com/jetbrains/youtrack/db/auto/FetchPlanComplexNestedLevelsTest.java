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

import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

@Test
public class FetchPlanComplexNestedLevelsTest extends BaseDBTest {

  @Parameters(value = "remote")
  public FetchPlanComplexNestedLevelsTest(@Optional Boolean remote) {
    super(remote != null && remote);
  }

  @BeforeClass
  public void beforeClass() throws Exception {
    super.beforeClass();

    final var personTest = db.getMetadata().getSchema().getClass("PersonTest");
    if (personTest == null) {
      db
          .getMetadata()
          .getSchema()
          .createClass("PersonTest", db.getMetadata().getSchema().getClass("V"));
    } else if (personTest.getSuperClass() == null) {
      personTest.setSuperClass(db, db.getMetadata().getSchema().getClass("V"));
    }

    final var followTest = db.getMetadata().getSchema().getClass("FollowTest");
    if (followTest == null) {
      db
          .getMetadata()
          .getSchema()
          .createClass("FollowTest", db.getMetadata().getSchema().getClass("E"));
    } else if (followTest.getSuperClass() == null) {
      followTest.setSuperClass(db, db.getMetadata().getSchema().getClass("E"));
    }

    db.begin();
    db.command("create vertex PersonTest set name = 'A'").close();
    db.command("create vertex PersonTest set name = 'B'").close();
    db.command("create vertex PersonTest set name = 'C'").close();

    db
        .command(
            "create edge FollowTest from (select from PersonTest where name = 'A') to (select from"
                + " PersonTest where name = 'B')")
        .close();
    db
        .command(
            "create edge FollowTest from (select from PersonTest where name = 'B') to (select from"
                + " PersonTest where name = 'C')")
        .close();
    db.commit();
  }

  @Test
  public void queryAll2() {
    var resultSet =
        executeQuery(
            "select @this.toJSON('fetchPlan:*:2') as json from (select from PersonTest where"
                + " name='A')");

    Assert.assertEquals(resultSet.size(), 1);
    String json = resultSet.getFirst().getProperty("json");

    Assert.assertNotNull(json);

    final var parsed = ((EntityImpl) db.newEntity());
    parsed.updateFromJSON(json);

    Assert.assertNotNull(parsed.rawField("out_FollowTest.in.out_FollowTest"));
  }

  @Test
  public void queryOutWildcard2() {
    var resultSet =
        executeQuery("select @this.toJSON('fetchPlan:out_*:2') as json from (select from PersonTest"
            + " where name='A')");

    Assert.assertEquals(resultSet.size(), 1);
    String json = resultSet.getFirst().getProperty("json");

    Assert.assertNotNull(json);

    final var parsed = ((EntityImpl) db.newEntity());
    parsed.updateFromJSON(json);

    Assert.assertNotNull(parsed.rawField("out_FollowTest.in.out_FollowTest"));
  }

  @Test
  public void queryOutOneLevelOnly() {
    var resultSet =
        executeQuery(
            "select @this.toJSON('fetchPlan:[0]out_*:0') as json from (select from PersonTest"
                + " where name='A')");

    Assert.assertEquals(resultSet.size(), 1);
    String json = resultSet.getFirst().getProperty("json");

    Assert.assertNotNull(json);

    var pos = json.indexOf("\"in\":\"");
    Assert.assertTrue(pos > -1);

    Assert.assertEquals(json.charAt(pos) + 1, '#');
  }

  @Test
  public void startZeroGetOutStar2() {
    var resultSet =
        executeQuery(
            "select @this.toJSON('fetchPlan:[0]out_*:2') as json from (select from PersonTest"
                + " where name='A')");

    Assert.assertEquals(resultSet.size(), 1);
    String json = resultSet.getFirst().getProperty("json");

    Assert.assertNotNull(json);

    var pos = json.indexOf("\"in\":{");
    Assert.assertTrue(pos > -1);
  }

  @Test
  public void start2GetOutStar2() {
    var resultSet =
        executeQuery(
            "select @this.toJSON('fetchPlan:[2]out_*:2') as json from (select from PersonTest"
                + " where name='A')");

    Assert.assertEquals(resultSet.size(), 1);
    String json = resultSet.getFirst().getProperty("json");

    Assert.assertNotNull(json);

    var pos = json.indexOf("\"in\":\"");
    Assert.assertFalse(pos > -1);
  }
}
