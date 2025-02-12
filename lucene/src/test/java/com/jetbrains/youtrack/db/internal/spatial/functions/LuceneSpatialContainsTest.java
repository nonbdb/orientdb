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

import com.jetbrains.youtrack.db.internal.spatial.BaseSpatialLuceneTest;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class LuceneSpatialContainsTest extends BaseSpatialLuceneTest {

  @Test
  public void testContainsNoIndex() {

    var execute =
        session.command(
            "select ST_Contains(smallc,smallc) as smallinsmall,ST_Contains(smallc, bigc) As"
                + " smallinbig, ST_Contains(bigc,smallc) As biginsmall from (SELECT"
                + " ST_Buffer(ST_GeomFromText('POINT(50 50)'), 20) As"
                + " smallc,ST_Buffer(ST_GeomFromText('POINT(50 50)'), 40) As bigc)");
    var next = execute.next();

    Assert.assertTrue(next.getProperty("smallinsmall"));
    Assert.assertFalse(next.getProperty("smallinbig"));
    Assert.assertTrue(next.getProperty("biginsmall"));
  }

  @Test
  public void testContainsIndex() {

    session.command("create class Polygon extends v").close();
    session.command("create property Polygon.geometry EMBEDDED OPolygon").close();

    session.begin();
    session.command(
            "insert into Polygon set geometry = ST_Buffer(ST_GeomFromText('POINT(50 50)'), 20)")
        .close();
    session.command(
            "insert into Polygon set geometry = ST_Buffer(ST_GeomFromText('POINT(50 50)'), 40)")
        .close();
    session.commit();

    session.command("create index Polygon.g on Polygon (geometry) SPATIAL engine lucene").close();
    var execute =
        session.command("SELECT from Polygon where ST_Contains(geometry, 'POINT(50 50)') = true");

    Assert.assertEquals(2, execute.stream().count());

    execute =
        session.command(
            "SELECT from Polygon where ST_Contains(geometry, ST_Buffer(ST_GeomFromText('POINT(50"
                + " 50)'), 30)) = true");

    Assert.assertEquals(1, execute.stream().count());
  }

  @Test
  public void testContainsIndex_GeometryCollection() {

    session.command("create class TestInsert extends v").close();
    session.command("create property TestInsert.geometry EMBEDDED OGeometryCollection").close();

    session.begin();
    session.command(
            "insert into TestInsert set geometry ="
                + " {'@type':'d','@class':'OGeometryCollection','geometries':[{'@type':'d','@class':'OPolygon','coordinates':[[[0,0],[10,0],[10,10],[0,10],[0,0]]]}]}")
        .close();
    session.command(
            "insert into TestInsert set geometry ="
                + " {'@type':'d','@class':'OGeometryCollection','geometries':[{'@type':'d','@class':'OPolygon','coordinates':[[[11,11],[21,11],[21,21],[11,21],[11,11]]]}]}")
        .close();
    session.commit();

    session.command(
            "create index TestInsert.geometry on TestInsert (geometry) SPATIAL engine lucene")
        .close();

    var testGeometry =
        "{'@type':'d','@class':'OGeometryCollection','geometries':[{'@type':'d','@class':'OPolygon','coordinates':[[[1,1],[2,1],[2,2],[1,2],[1,1]]]}]}";
    var execute =
        session.command(
            "SELECT from TestInsert where ST_Contains(geometry, " + testGeometry + ") = true");

    Assert.assertEquals(1, execute.stream().count());
  }
}
