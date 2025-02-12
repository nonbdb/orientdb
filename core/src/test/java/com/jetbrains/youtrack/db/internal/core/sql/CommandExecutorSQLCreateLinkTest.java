/*
 *
 *  *  Copyright YouTrackDB
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
package com.jetbrains.youtrack.db.internal.core.sql;

import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.internal.DbTestBase;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class CommandExecutorSQLCreateLinkTest extends DbTestBase {

  @Test
  public void testBasic() {
    var basic1 = session.getMetadata().getSchema().createClass("Basic1");
    basic1.createProperty(session, "theLink", PropertyType.LINK);

    var basic2 = session.getMetadata().getSchema().createClass("Basic2");
    basic2.createProperty(session, "theLink", PropertyType.LINK);

    session.begin();
    session.command("insert into Basic1 set pk = 'pkb1_1', fk = 'pkb2_1'").close();
    session.command("insert into Basic1 set pk = 'pkb1_2', fk = 'pkb2_2'").close();

    session.command("insert into Basic2 set pk = 'pkb2_1'").close();
    session.command("insert into Basic2 set pk = 'pkb2_2'").close();

    session.command("CREATE LINK theLink type link FROM Basic1.fk TO Basic2.pk ").close();
    session.commit();

    var result = session.query("select pk, theLink.pk as other from Basic1 order by pk");

    var otherKey = result.next().getProperty("other");
    Assert.assertNotNull(otherKey);

    Assert.assertEquals(otherKey, "pkb2_1");

    otherKey = result.next().getProperty("other");
    Assert.assertEquals(otherKey, "pkb2_2");
  }

  @Test
  public void testInverse() {
    var inverse1 = session.getMetadata().getSchema().createClass("Inverse1");
    inverse1.createProperty(session, "theLink", PropertyType.LINK);

    var inverse2 = session.getMetadata().getSchema().createClass("Inverse2");
    inverse2.createProperty(session, "theLink", PropertyType.LINKSET);

    session.begin();
    session.command("insert into Inverse1 set pk = 'pkb1_1', fk = 'pkb2_1'").close();
    session.command("insert into Inverse1 set pk = 'pkb1_2', fk = 'pkb2_2'").close();
    session.command("insert into Inverse1 set pk = 'pkb1_3', fk = 'pkb2_2'").close();

    session.command("insert into Inverse2 set pk = 'pkb2_1'").close();
    session.command("insert into Inverse2 set pk = 'pkb2_2'").close();

    session.command("CREATE LINK theLink TYPE LINKSET FROM Inverse1.fk TO Inverse2.pk INVERSE")
        .close();
    session.commit();

    var result = session.query("select pk, theLink.pk as other from Inverse2 order by pk");
    var first = result.next();
    var otherKeys = first.getProperty("other");
    Assert.assertNotNull(otherKeys);
    Assert.assertTrue(otherKeys instanceof List);
    Assert.assertEquals(((List) otherKeys).get(0), "pkb1_1");

    var second = result.next();
    otherKeys = second.getProperty("other");
    Assert.assertNotNull(otherKeys);
    Assert.assertTrue(otherKeys instanceof List);
    Assert.assertEquals(((List) otherKeys).size(), 2);
    Assert.assertTrue(((List) otherKeys).contains("pkb1_2"));
    Assert.assertTrue(((List) otherKeys).contains("pkb1_3"));
  }
}
