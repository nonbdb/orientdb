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

import com.jetbrains.youtrack.db.api.config.GlobalConfiguration;
import com.jetbrains.youtrack.db.api.config.YouTrackDBConfig;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.api.record.RID;
import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import com.jetbrains.youtrack.db.internal.core.db.YouTrackDBConfigBuilderImpl;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

@Test
public class EntityTreeTest extends BaseDBTest {

  @Parameters(value = "remote")
  public EntityTreeTest(@Optional Boolean remote) {
    super(remote != null && remote);
  }

  @BeforeClass
  public void init() {
    createComplexTestClass();
    createSimpleTestClass();
    createCascadeDeleteClass();
    createPlanetClasses();
    createRefClasses();
  }

  @Override
  protected YouTrackDBConfig createConfig(YouTrackDBConfigBuilderImpl builder) {
    builder.addGlobalConfigurationParameter(GlobalConfiguration.NON_TX_READS_WARNING_MODE,
        "EXCEPTION");
    return builder.build();
  }


  @Test
  public void testPersonSaving() {
    addGaribaldiAndBonaparte();

    database.begin();
    Assert.assertTrue(
        database.query("select from Profile where nick = 'NBonaparte'").stream()
            .findAny()
            .isPresent());
    database.commit();
  }

  @Test(dependsOnMethods = "testPersonSaving")
  public void testCityEquality() {
    database.begin();
    List<EntityImpl> resultset =
        executeQuery("select from profile where location.city.name = 'Rome'");
    Assert.assertEquals(resultset.size(), 2);

    var p1 = resultset.get(0);
    var p2 = resultset.get(1);

    Assert.assertNotSame(p1, p2);
    Assert.assertSame(
        p1.getElementProperty("location").getElementProperty("city"),
        p2.getElementProperty("location").getElementProperty("city"));
    database.commit();
  }

  @Test(dependsOnMethods = "testCityEquality")
  public void testSaveCircularLink() {
    database.begin();
    var winston = database.newInstance("Profile");

    winston.setProperty("nick", "WChurcill");
    winston.setProperty("name", "Winston");
    winston.setProperty("surname", "Churcill");

    var country = database.newInstance("Country");
    country.setProperty("name", "England");

    var city = database.newInstance("City");
    city.setProperty("name", "London");
    city.setProperty("country", country);

    var address = database.newInstance("Address");
    address.setProperty("type", "Residence");
    address.setProperty("city", city);
    address.setProperty("street", "unknown");

    winston.setProperty("location", address);

    var nicholas = database.newInstance("Profile");
    nicholas.setProperty("nick", "NChurcill");
    nicholas.setProperty("name", "Nicholas");
    nicholas.setProperty("surname", "Churcill");

    nicholas.setProperty("location", winston.getElementProperty("location"));

    nicholas.setProperty("invitedBy", winston);
    winston.setProperty("invitedBy", nicholas);

    database.save(nicholas);
    database.commit();
  }

  @Test(dependsOnMethods = "testSaveCircularLink")
  public void testSaveMultiCircular() {
    addBarackObamaAndFollowers();
  }

  @SuppressWarnings("unchecked")
  @Test(dependsOnMethods = "testSaveMultiCircular")
  public void testQueryMultiCircular() {
    database.begin();
    List<EntityImpl> result =
        executeQuery("select * from Profile where name = 'Barack' and surname = 'Obama'");

    Assert.assertEquals(result.size(), 1);
    for (EntityImpl profile : result) {
      final Collection<Identifiable> followers = profile.field("followers");
      if (followers != null) {
        for (Identifiable follower : followers) {
          Assert.assertTrue(
              ((Collection<Identifiable>)
                  Objects.requireNonNull(follower.getEntity().getProperty("followings")))
                  .contains(profile));
        }
      }
    }
    database.commit();
  }

  @Test
  public void testSetFieldSize() {
    database.begin();
    var test = database.newInstance("JavaComplexTestClass");
    test.setProperty("set", new HashSet<>());

    for (int i = 0; i < 100; i++) {
      var child = database.newInstance("Child");
      child.setProperty("name", String.valueOf(i));
      test.<Set<Identifiable>>getProperty("set").add(child);
    }
    Assert.assertNotNull(test.<Set<Identifiable>>getProperty("set"));
    Assert.assertEquals(test.<Set<Identifiable>>getProperty("set").size(), 100);

    database.save(test);
    database.commit();

    // Assert.assertEquals(test.<Set<Identifiable>>getProperty("set").size(), 100);
    RID rid = test.getIdentity();
    database.close();
    database = createSessionInstance();

    database.begin();
    test = database.load(rid);
    Assert.assertNotNull(test.<Set<Identifiable>>getProperty("set"));
    for (Identifiable identifiable : test.<Set<Identifiable>>getProperty("set")) {
      var child = identifiable.getEntity();
      Assert.assertNotNull(child.<String>getProperty("name"));
      Assert.assertTrue(Integer.parseInt(child.getProperty("name")) < 100);
      Assert.assertTrue(Integer.parseInt(child.getProperty("name")) >= 0);
    }
    Assert.assertEquals(test.<Set<Identifiable>>getProperty("set").size(), 100);
    database.delete(database.bindToSession(test));
    database.commit();
  }

  @Test(dependsOnMethods = "testQueryMultiCircular")
  public void testCollectionsRemove() {
    var a = database.newInstance("JavaComplexTestClass");

    // LIST TEST
    var first = database.newInstance("Child");
    first.setProperty("name", "1");
    var second = database.newInstance("Child");
    second.setProperty("name", "2");
    var third = database.newInstance("Child");
    third.setProperty("name", "3");
    var fourth = database.newInstance("Child");
    fourth.setProperty("name", "4");
    var fifth = database.newInstance("Child");
    fifth.setProperty("name", "5");

    var set = new HashSet<Identifiable>();
    set.add(first);
    set.add(second);
    set.add(third);
    set.add(fourth);
    set.add(fifth);

    a.setProperty("set", set);

    var list = new ArrayList<Identifiable>();
    list.add(first);
    list.add(second);
    list.add(third);
    list.add(fourth);
    list.add(fifth);

    a.setProperty("list", list);

    a.<Set<Identifiable>>getProperty("set").remove(third);
    a.<List<Identifiable>>getProperty("list").remove(fourth);

    Assert.assertEquals(a.<Set<Identifiable>>getProperty("set").size(), 4);
    Assert.assertEquals(a.<List<Identifiable>>getProperty("list").size(), 4);

    database.begin();
    a = database.save(a);
    database.commit();

    database.begin();
    a = database.bindToSession(a);
    RID rid = a.getIdentity();
    Assert.assertEquals(a.<Set<Identifiable>>getProperty("set").size(), 4);
    Assert.assertEquals(a.<List<Identifiable>>getProperty("list").size(), 4);
    database.commit();

    database.close();

    database = createSessionInstance();

    database.begin();
    var loadedObj = database.loadEntity(rid);

    Assert.assertEquals(loadedObj.<Set<Object>>getProperty("set").size(), 4);
    Assert.assertEquals(loadedObj.<Set<Identifiable>>getProperty("set").size(), 4);

    database.delete(rid);
    database.commit();
  }

  @Test
  public void childNLevelUpdateTest() {
    database.begin();
    var p = database.newInstance("Planet");
    var near = database.newInstance("Planet");
    var sat = database.newInstance("Satellite");
    var satNear = database.newInstance("Satellite");
    sat.setProperty("diameter", 50);
    sat.setProperty("near", near);
    satNear.setProperty("diameter", 10);

    near.setProperty("satellites", Collections.singletonList(satNear));
    p.setProperty("satellites", Collections.singletonList(sat));

    database.save(p);
    database.commit();

    database.begin();
    RID rid = p.getIdentity();
    p = database.load(rid);
    sat = p.<List<Identifiable>>getProperty("satellites").get(0).getEntity();
    near = sat.getElementProperty("near");
    satNear = near.<List<Identifiable>>getProperty("satellites").get(0).getEntity();
    Assert.assertEquals(satNear.<Long>getProperty("diameter"), 10);

    satNear.setProperty("diameter", 100);
    satNear.save();

    database.save(p);
    database.commit();

    database.begin();
    p = database.load(rid);
    sat = p.<List<Identifiable>>getProperty("satellites").get(0).getEntity();
    near = sat.getElementProperty("near");
    satNear = near.<List<Identifiable>>getProperty("satellites").get(0).getEntity();
    Assert.assertEquals(satNear.<Long>getProperty("diameter"), 100);
    database.commit();
  }

  @Test(dependsOnMethods = "childNLevelUpdateTest")
  public void childMapUpdateTest() {
    database.begin();
    var p = database.newInstance("Planet");
    p.setProperty("name", "Earth");
    p.setProperty("distanceSun", 1000);

    var sat = database.newInstance("Satellite");
    sat.setProperty("diameter", 50);
    sat.setProperty("name", "Moon");

    p.setProperty("satellitesMap", Collections.singletonMap(sat.<String>getProperty("name"), sat));
    database.save(p);
    database.commit();

    database.begin();
    p = database.bindToSession(p);
    Assert.assertEquals(p.<Integer>getProperty("distanceSun"), 1000);
    Assert.assertEquals(p.getProperty("name"), "Earth");
    RID rid = p.getIdentity();

    p = database.load(rid);
    sat = p.<Map<String, Identifiable>>getProperty("satellitesMap").get("Moon").getEntity();
    Assert.assertEquals(p.<Integer>getProperty("distanceSun"), 1000);
    Assert.assertEquals(p.getProperty("name"), "Earth");
    Assert.assertEquals(sat.<Long>getProperty("diameter"), 50);
    sat.setProperty("diameter", 500);

    database.save(p);
    database.commit();

    database.begin();
    p = database.load(rid);
    sat = p.<Map<String, Identifiable>>getProperty("satellitesMap").get("Moon").getEntity();
    Assert.assertEquals(sat.<Long>getProperty("diameter"), 500);
    Assert.assertEquals(p.<Integer>getProperty("distanceSun"), 1000);
    Assert.assertEquals(p.getProperty("name"), "Earth");
    database.commit();
  }

  @Test(dependsOnMethods = "childMapUpdateTest")
  public void childMapNLevelUpdateTest() {
    var jupiter = database.newInstance("Planet");
    jupiter.setProperty("name", "Jupiter");
    jupiter.setProperty("distanceSun", 3000);
    var mercury = database.newInstance("Planet");
    mercury.setProperty("name", "Mercury");
    mercury.setProperty("distanceSun", 5000);
    var jupiterMoon = database.newInstance("Satellite");
    var mercuryMoon = database.newInstance("Satellite");
    jupiterMoon.setProperty("diameter", 50);
    jupiterMoon.setProperty("near", mercury);
    jupiterMoon.setProperty("name", "JupiterMoon");
    mercuryMoon.setProperty("diameter", 10);
    mercuryMoon.setProperty("name", "MercuryMoon");

    mercury.setProperty(
        "satellitesMap",
        Collections.singletonMap(mercuryMoon.<String>getProperty("name"), mercuryMoon));
    jupiter.setProperty(
        "satellitesMap",
        Collections.singletonMap(jupiterMoon.<String>getProperty("name"), jupiterMoon));

    database.begin();
    database.save(jupiter);
    database.commit();

    database.begin();
    RID rid = jupiter.getIdentity();
    jupiter = database.load(rid);
    jupiterMoon =
        jupiter
            .<Map<String, Identifiable>>getProperty("satellitesMap")
            .get("JupiterMoon")
            .getEntity();
    mercury = jupiterMoon.getElementProperty("near");
    mercuryMoon =
        mercury
            .<Map<String, Identifiable>>getProperty("satellitesMap")
            .get("MercuryMoon")
            .getEntity();
    Assert.assertEquals(mercuryMoon.<Long>getProperty("diameter"), 10);
    Assert.assertEquals(mercuryMoon.getProperty("name"), "MercuryMoon");
    Assert.assertEquals(jupiterMoon.<Long>getProperty("diameter"), 50);
    Assert.assertEquals(jupiterMoon.getProperty("name"), "JupiterMoon");
    Assert.assertEquals(jupiter.getProperty("name"), "Jupiter");
    Assert.assertEquals(jupiter.<Integer>getProperty("distanceSun"), 3000);
    Assert.assertEquals(mercury.getProperty("name"), "Mercury");
    Assert.assertEquals(mercury.<Integer>getProperty("distanceSun"), 5000);
    mercuryMoon.setProperty("diameter", 100);
    database.save(jupiter);
    database.commit();

    database.close();
    database = createSessionInstance();

    database.begin();
    jupiter = database.load(rid);
    jupiterMoon =
        jupiter
            .<Map<String, Identifiable>>getProperty("satellitesMap")
            .get("JupiterMoon")
            .getEntity();
    mercury = jupiterMoon.getElementProperty("near");
    mercuryMoon =
        mercury
            .<Map<String, Identifiable>>getProperty("satellitesMap")
            .get("MercuryMoon")
            .getEntity();
    Assert.assertEquals(mercuryMoon.<Long>getProperty("diameter"), 100);
    Assert.assertEquals(mercuryMoon.getProperty("name"), "MercuryMoon");
    Assert.assertEquals(jupiterMoon.<Long>getProperty("diameter"), 50);
    Assert.assertEquals(jupiterMoon.getProperty("name"), "JupiterMoon");
    Assert.assertEquals(jupiter.getProperty("name"), "Jupiter");
    Assert.assertEquals(jupiter.<Integer>getProperty("distanceSun"), 3000);
    Assert.assertEquals(mercury.getProperty("name"), "Mercury");
    Assert.assertEquals(mercury.<Integer>getProperty("distanceSun"), 5000);
    database.commit();
    database.close();
  }

  @Test
  public void iteratorShouldTerminate() {
    database.begin();

    var person = database.newEntity("Profile");
    person.setProperty("nick", "Guy1");
    person.setProperty("name", "Guy");
    person.setProperty("surname", "Ritchie");

    person = database.save(person);
    database.commit();

    database.begin();
    database.delete(database.bindToSession(person));
    database.commit();

    database.begin();
    var person2 = database.newEntity("Profile");
    person2.setProperty("nick", "Guy2");
    person2.setProperty("name", "Guy");
    person2.setProperty("surname", "Brush");
    database.save(person2);

    var it = database.browseClass("Profile");
    while (it.hasNext()) {
      it.next();
    }

    database.commit();
  }

  @Test
  public void testSave() {
    database.begin();
    var parent1 = database.newEntity("RefParent");
    parent1 = database.save(parent1);
    var parent2 = database.newEntity("RefParent");
    parent2 = database.save(parent2);

    var child1 = database.newEntity("RefChild");
    parent1.setProperty("children", Collections.singleton(child1));
    parent1 = database.save(parent1);

    var child2 = database.newEntity("RefChild");
    parent2.setProperty("children", Collections.singleton(child2));
    database.save(parent2);
    database.commit();

    database.begin();
    parent1 = database.load(parent1.getIdentity());
    parent2 = database.load(parent2.getIdentity());

    var child3 = database.newEntity("RefChild");

    var otherThing = database.newEntity("OtherThing");
    child3.setProperty("otherThing", otherThing);

    otherThing.setProperty("relationToParent1", parent1);
    otherThing.setProperty("relationToParent2", parent2);

    parent1.<Set<Identifiable>>getProperty("children").add(child3);
    parent2.<Set<Identifiable>>getProperty("children").add(child3);

    database.save(parent1);
    database.save(parent2);

    database.commit();
  }

  private void createCascadeDeleteClass() {
    var schema = database.getSchema();
    if (schema.existsClass("JavaCascadeDeleteTestClass")) {
      schema.dropClass("JavaCascadeDeleteTestClass");
    }

    var child = schema.getClass("Child");
    SchemaClass clazz = schema.createClass("JavaCascadeDeleteTestClass");
    clazz.createProperty(database, "simpleClass", PropertyType.LINK,
        schema.getClass("JavaSimpleTestClass"));
    clazz.createProperty(database, "binary", PropertyType.LINK);
    clazz.createProperty(database, "name", PropertyType.STRING);
    clazz.createProperty(database, "set", PropertyType.LINKSET, child);
    clazz.createProperty(database, "children", PropertyType.LINKMAP, child);
    clazz.createProperty(database, "list", PropertyType.LINKLIST, child);
  }

  private void createPlanetClasses() {
    var schema = database.getSchema();
    var satellite = schema.createClass("Satellite");
    var planet = schema.createClass("Planet");

    planet.createProperty(database, "name", PropertyType.STRING);
    planet.createProperty(database, "distanceSun", PropertyType.INTEGER);
    planet.createProperty(database, "satellites", PropertyType.LINKLIST, satellite);
    planet.createProperty(database, "satellitesMap", PropertyType.LINKMAP, satellite);

    satellite.createProperty(database, "name", PropertyType.STRING);
    satellite.createProperty(database, "diameter", PropertyType.LONG);
    satellite.createProperty(database, "near", PropertyType.LINK, planet);
  }

  private void createRefClasses() {
    var schema = database.getSchema();
    var refParent = schema.createClass("RefParent");
    var refChild = schema.createClass("RefChild");
    var otherThing = schema.createClass("OtherThing");

    refParent.createProperty(database, "children", PropertyType.LINKSET, refChild);
    refChild.createProperty(database, "otherThing", PropertyType.LINK, otherThing);

    otherThing.createProperty(database, "relationToParent1", PropertyType.LINK, refParent);
    otherThing.createProperty(database, "relationToParent2", PropertyType.LINK, refParent);
  }
}
