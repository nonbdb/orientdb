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

import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import org.testng.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

@Test
public class SQLCreateLinkTest extends BaseDBTest {

  @Parameters(value = "remote")
  public SQLCreateLinkTest(boolean remote) {
    super(remote);
  }

  @Test
  public void createLinktest() {
    session.command("CREATE CLASS POST").close();
    session.command("CREATE PROPERTY POST.comments LINKSET").close();

    session.begin();
    session.command("INSERT INTO POST (id, title) VALUES ( 10, 'NoSQL movement' )").close();
    session.command("INSERT INTO POST (id, title) VALUES ( 20, 'New YouTrackDB' )").close();

    session.command("INSERT INTO POST (id, title) VALUES ( 30, '(')").close();

    session.command("INSERT INTO POST (id, title) VALUES ( 40, ')')").close();
    session.commit();

    session.command("CREATE CLASS COMMENT").close();

    session.begin();
    session.command("INSERT INTO COMMENT (id, postId, text) VALUES ( 0, 10, 'First' )").close();
    session.command("INSERT INTO COMMENT (id, postId, text) VALUES ( 1, 10, 'Second' )").close();
    session.command("INSERT INTO COMMENT (id, postId, text) VALUES ( 21, 10, 'Another' )").close();
    session
        .command("INSERT INTO COMMENT (id, postId, text) VALUES ( 41, 20, 'First again' )")
        .close();
    session
        .command("INSERT INTO COMMENT (id, postId, text) VALUES ( 82, 20, 'Second Again' )")
        .close();

    Assert.assertEquals(
        ((Number)
            session
                .command(
                    "CREATE LINK comments TYPE LINKSET FROM comment.postId TO post.id"
                        + " INVERSE")
                .next()
                .getProperty("count"))
            .intValue(),
        5);
    session.commit();

    session.begin();
    Assert.assertEquals(
        ((Number) session.command("UPDATE comment REMOVE postId").next().getProperty("count"))
            .intValue(),
        5);
    session.commit();
  }

  @Test
  public void createRIDLinktest() {

    session.command("CREATE CLASS POST2").close();
    session.command("CREATE PROPERTY POST2.comments LINKSET").close();

    session.begin();
    Object p1 =
        session
            .command("INSERT INTO POST2 (id, title) VALUES ( 10, 'NoSQL movement' )")
            .next()
            .asEntity();
    Assert.assertTrue(p1 instanceof EntityImpl);
    Object p2 =
        session
            .command("INSERT INTO POST2 (id, title) VALUES ( 20, 'New YouTrackDB' )")
            .next()
            .asEntity();
    Assert.assertTrue(p2 instanceof EntityImpl);

    Object p3 =
        session.command("INSERT INTO POST2 (id, title) VALUES ( 30, '(')").next().asEntity();
    Assert.assertTrue(p3 instanceof EntityImpl);

    Object p4 =
        session.command("INSERT INTO POST2 (id, title) VALUES ( 40, ')')").next().asEntity();
    Assert.assertTrue(p4 instanceof EntityImpl);
    session.commit();

    session.command("CREATE CLASS COMMENT2");

    session.begin();
    session
        .command(
            "INSERT INTO COMMENT2 (id, postId, text) VALUES ( 0, '"
                + ((EntityImpl) p1).getIdentity()
                + "', 'First' )")
        .close();
    session
        .command(
            "INSERT INTO COMMENT2 (id, postId, text) VALUES ( 1, '"
                + ((EntityImpl) p1).getIdentity()
                + "', 'Second' )")
        .close();
    session
        .command(
            "INSERT INTO COMMENT2 (id, postId, text) VALUES ( 21, '"
                + ((EntityImpl) p1).getIdentity()
                + "', 'Another' )")
        .close();
    session
        .command(
            "INSERT INTO COMMENT2 (id, postId, text) VALUES ( 41, '"
                + ((EntityImpl) p2).getIdentity()
                + "', 'First again' )")
        .close();
    session
        .command(
            "INSERT INTO COMMENT2 (id, postId, text) VALUES ( 82, '"
                + ((EntityImpl) p2).getIdentity()
                + "', 'Second Again' )")
        .close();
    session.commit();

    session.begin();
    Assert.assertEquals(
        ((Number)
            session
                .command(
                    "CREATE LINK comments TYPE LINKSET FROM comment2.postId TO post2.id"
                        + " INVERSE")
                .next()
                .getProperty("count"))
            .intValue(),
        5);
    session.commit();

    session.begin();
    Assert.assertEquals(
        ((Number) session.command("UPDATE comment2 REMOVE postId").next().getProperty("count"))
            .intValue(),
        5);
    session.commit();
  }
}
