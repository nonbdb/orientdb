package com.jetbrains.youtrack.db.internal.core.sql.executor;

import static com.jetbrains.youtrack.db.internal.core.sql.executor.ExecutionPlanPrintUtils.printExecutionPlan;

import com.jetbrains.youtrack.db.api.DatabaseSession;
import com.jetbrains.youtrack.db.api.config.GlobalConfiguration;
import com.jetbrains.youtrack.db.api.exception.CommandExecutionException;
import com.jetbrains.youtrack.db.api.query.ExecutionPlan;
import com.jetbrains.youtrack.db.api.query.ExecutionStep;
import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.api.query.ResultSet;
import com.jetbrains.youtrack.db.api.record.DBRecord;
import com.jetbrains.youtrack.db.api.record.Entity;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.api.record.RID;
import com.jetbrains.youtrack.db.api.record.Vertex;
import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.api.schema.Schema;
import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import com.jetbrains.youtrack.db.api.schema.SchemaProperty;
import com.jetbrains.youtrack.db.internal.DbTestBase;
import com.jetbrains.youtrack.db.internal.common.concur.TimeoutException;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.id.RecordId;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.sql.SQLEngine;
import com.jetbrains.youtrack.db.internal.core.sql.functions.SQLFunction;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 */
public class SelectStatementExecutionTest extends DbTestBase {

  @Test
  public void testSelectNoTarget() {
    var result = db.query("select 1 as one, 2 as two, 2+3");
    Assert.assertTrue(result.hasNext());
    var item = result.next();
    Assert.assertNotNull(item);
    Assert.assertEquals(1, item.<Object>getProperty("one"));
    Assert.assertEquals(2, item.<Object>getProperty("two"));
    Assert.assertEquals(5, item.<Object>getProperty("2 + 3"));
    printExecutionPlan(result);

    result.close();
  }

  @Test
  public void testGroupByCount() {
    db.getMetadata().getSchema().createClass("InputTx");

    for (var i = 0; i < 100; i++) {
      db.begin();
      final var hash = UUID.randomUUID().toString();
      db.command("insert into InputTx set address = '" + hash + "'");

      // CREATE RANDOM NUMBER OF COPIES final int random = new Random().nextInt(10);
      final var random = new Random().nextInt(10);
      for (var j = 0; j < random; j++) {
        db.command("insert into InputTx set address = '" + hash + "'");
      }
      db.commit();
    }

    final var result =
        db.query(
            "select address, count(*) as occurrencies from InputTx where address is not null group"
                + " by address limit 10");
    while (result.hasNext()) {
      final var row = result.next();
      Assert.assertNotNull(row.getProperty("address")); // <== FALSE!
      Assert.assertNotNull(row.getProperty("occurrencies"));
    }
    result.close();
  }

  @Test
  public void testSelectNoTargetSkip() {
    var result = db.query("select 1 as one, 2 as two, 2+3 skip 1");
    Assert.assertFalse(result.hasNext());
    printExecutionPlan(result);

    result.close();
  }

  @Test
  public void testSelectNoTargetSkipZero() {
    var result = db.query("select 1 as one, 2 as two, 2+3 skip 0");
    Assert.assertTrue(result.hasNext());
    var item = result.next();
    Assert.assertNotNull(item);
    Assert.assertEquals(1, item.<Object>getProperty("one"));
    Assert.assertEquals(2, item.<Object>getProperty("two"));
    Assert.assertEquals(5, item.<Object>getProperty("2 + 3"));
    printExecutionPlan(result);

    result.close();
  }

  @Test
  public void testSelectNoTargetLimit0() {
    var result = db.query("select 1 as one, 2 as two, 2+3 limit 0");
    Assert.assertFalse(result.hasNext());
    printExecutionPlan(result);

    result.close();
  }

  @Test
  public void testSelectNoTargetLimit1() {
    var result = db.query("select 1 as one, 2 as two, 2+3 limit 1");
    Assert.assertTrue(result.hasNext());
    var item = result.next();
    Assert.assertNotNull(item);
    Assert.assertEquals(1, item.<Object>getProperty("one"));
    Assert.assertEquals(2, item.<Object>getProperty("two"));
    Assert.assertEquals(5, item.<Object>getProperty("2 + 3"));
    printExecutionPlan(result);

    result.close();
  }

  @Test
  public void testSelectNoTargetLimitx() {
    var result = db.query("select 1 as one, 2 as two, 2+3 skip 0 limit 0");
    printExecutionPlan(result);
    result.close();
  }

  @Test
  public void testSelectFullScan1() {
    var className = "TestSelectFullScan1";
    db.getMetadata().getSchema().createClass(className);
    for (var i = 0; i < 100000; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i);
      doc.setProperty("surname", "surname" + i);
      doc.save();
      db.commit();
    }

    var result = db.query("select from " + className);
    for (var i = 0; i < 100000; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertNotNull(item);
      Assert.assertTrue(("" + item.getProperty("name")).startsWith("name"));
    }
    Assert.assertFalse(result.hasNext());
    printExecutionPlan(result);
    result.close();
  }

  @Test
  public void testSelectFullScanOrderByRidAsc() {
    var className = "testSelectFullScanOrderByRidAsc";
    db.getMetadata().getSchema().createClass(className);
    for (var i = 0; i < 100000; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i);
      doc.setProperty("surname", "surname" + i);
      doc.save();
      db.commit();
    }
    var result = db.query("select from " + className + " ORDER BY @rid ASC");
    printExecutionPlan(result);
    Identifiable lastItem = null;
    for (var i = 0; i < 100000; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertNotNull(item);
      Assert.assertTrue(("" + item.getProperty("name")).startsWith("name"));
      if (lastItem != null) {
        Assert.assertTrue(
            lastItem.getIdentity().compareTo(item.getEntity().get().getIdentity()) < 0);
      }
      lastItem = item.getEntity().get();
    }
    Assert.assertFalse(result.hasNext());

    result.close();
  }

  @Test
  public void testSelectFullScanOrderByRidDesc() {
    var className = "testSelectFullScanOrderByRidDesc";
    db.getMetadata().getSchema().createClass(className);
    for (var i = 0; i < 100000; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i);
      doc.setProperty("surname", "surname" + i);
      doc.save();
      db.commit();
    }

    var result = db.query("select from " + className + " ORDER BY @rid DESC");
    printExecutionPlan(result);
    Identifiable lastItem = null;
    for (var i = 0; i < 100000; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertNotNull(item);
      Assert.assertTrue(("" + item.getProperty("name")).startsWith("name"));
      if (lastItem != null) {
        Assert.assertTrue(
            lastItem.getIdentity().compareTo(item.getEntity().get().getIdentity()) > 0);
      }
      lastItem = item.getEntity().get();
    }
    Assert.assertFalse(result.hasNext());

    result.close();
  }

  @Test
  public void testSelectFullScanLimit1() {
    var className = "testSelectFullScanLimit1";
    db.getMetadata().getSchema().createClass(className);
    for (var i = 0; i < 300; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i);
      doc.setProperty("surname", "surname" + i);
      doc.save();
      db.commit();
    }
    var result = db.query("select from " + className + " limit 10");
    printExecutionPlan(result);

    for (var i = 0; i < 10; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertNotNull(item);
      Assert.assertTrue(("" + item.getProperty("name")).startsWith("name"));
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testSelectFullScanSkipLimit1() {
    var className = "testSelectFullScanSkipLimit1";
    db.getMetadata().getSchema().createClass(className);
    for (var i = 0; i < 300; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i);
      doc.setProperty("surname", "surname" + i);
      doc.save();
      db.commit();
    }
    var result = db.query("select from " + className + " skip 100 limit 10");
    printExecutionPlan(result);

    for (var i = 0; i < 10; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertNotNull(item);
      Assert.assertTrue(("" + item.getProperty("name")).startsWith("name"));
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testSelectOrderByDesc() {
    var className = "testSelectOrderByDesc";
    db.getMetadata().getSchema().createClass(className);
    for (var i = 0; i < 30; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i);
      doc.setProperty("surname", "surname" + i);
      doc.save();
      db.commit();
    }
    var result = db.query("select from " + className + " order by surname desc");
    printExecutionPlan(result);

    String lastSurname = null;
    for (var i = 0; i < 30; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertNotNull(item);
      String thisSurname = item.getProperty("surname");
      if (lastSurname != null) {
        Assert.assertTrue(lastSurname.compareTo(thisSurname) >= 0);
      }
      lastSurname = thisSurname;
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testSelectOrderByAsc() {
    var className = "testSelectOrderByAsc";
    db.getMetadata().getSchema().createClass(className);
    for (var i = 0; i < 30; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i);
      doc.setProperty("surname", "surname" + i);
      doc.save();
      db.commit();
    }
    var result = db.query("select from " + className + " order by surname asc");
    printExecutionPlan(result);

    String lastSurname = null;
    for (var i = 0; i < 30; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertNotNull(item);
      String thisSurname = item.getProperty("surname");
      if (lastSurname != null) {
        Assert.assertTrue(lastSurname.compareTo(thisSurname) <= 0);
      }
      lastSurname = thisSurname;
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testSelectOrderByMassiveAsc() {
    var className = "testSelectOrderByMassiveAsc";
    db.getMetadata().getSchema().createClass(className);
    for (var i = 0; i < 100000; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i);
      doc.setProperty("surname", "surname" + i % 100);
      doc.save();
      db.commit();
    }

    var result = db.query("select from " + className + " order by surname asc limit 100");
    printExecutionPlan(result);

    for (var i = 0; i < 100; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertNotNull(item);
      Assert.assertEquals("surname0", item.getProperty("surname"));
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testSelectOrderWithProjections() {
    var className = "testSelectOrderWithProjections";
    db.getMetadata().getSchema().createClass(className);
    for (var i = 0; i < 100; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i % 10);
      doc.setProperty("surname", "surname" + i % 10);
      doc.save();
      db.commit();
    }

    var result = db.query("select name from " + className + " order by surname asc");
    printExecutionPlan(result);

    String lastName = null;
    for (var i = 0; i < 100; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertNotNull(item);
      String name = item.getProperty("name");
      Assert.assertNotNull(name);
      if (i > 0) {
        Assert.assertTrue(name.compareTo(lastName) >= 0);
      }
      lastName = name;
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testSelectOrderWithProjections2() {
    var className = "testSelectOrderWithProjections2";
    db.getMetadata().getSchema().createClass(className);
    for (var i = 0; i < 100; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i % 10);
      doc.setProperty("surname", "surname" + i % 10);
      doc.save();
      db.commit();
    }

    var result =
        db.query("select name from " + className + " order by name asc, surname asc");
    printExecutionPlan(result);

    String lastName = null;
    for (var i = 0; i < 100; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertNotNull(item);
      String name = item.getProperty("name");
      Assert.assertNotNull(name);
      if (i > 0) {
        Assert.assertTrue(name.compareTo(lastName) >= 0);
      }
      lastName = name;
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testSelectFullScanWithFilter1() {
    var className = "testSelectFullScanWithFilter1";
    db.getMetadata().getSchema().createClass(className);

    for (var i = 0; i < 300; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i);
      doc.setProperty("surname", "surname" + i);
      doc.save();
      db.commit();
    }

    var result =
        db.query("select from " + className + " where name = 'name1' or name = 'name7' ");
    printExecutionPlan(result);

    for (var i = 0; i < 2; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertNotNull(item);
      var name = item.getProperty("name");
      Assert.assertTrue("name1".equals(name) || "name7".equals(name));
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testSelectFullScanWithFilter2() {
    var className = "testSelectFullScanWithFilter2";
    db.getMetadata().getSchema().createClass(className);
    for (var i = 0; i < 300; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i);
      doc.setProperty("surname", "surname" + i);
      doc.save();
      db.commit();
    }
    var result = db.query("select from " + className + " where name <> 'name1' ");
    printExecutionPlan(result);

    for (var i = 0; i < 299; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertNotNull(item);
      var name = item.getProperty("name");
      Assert.assertNotEquals("name1", name);
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testProjections() {
    var className = "testProjections";
    db.getMetadata().getSchema().createClass(className);
    for (var i = 0; i < 300; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i);
      doc.setProperty("surname", "surname" + i);
      doc.save();
      db.commit();
    }
    var result = db.query("select name from " + className);
    printExecutionPlan(result);

    for (var i = 0; i < 300; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertNotNull(item);
      String name = item.getProperty("name");
      String surname = item.getProperty("surname");
      Assert.assertNotNull(name);
      Assert.assertTrue(name.startsWith("name"));
      Assert.assertNull(surname);
      Assert.assertFalse(item.getEntity().isPresent());
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testCountStar() {
    var className = "testCountStar";
    db.getMetadata().getSchema().createClass(className);

    for (var i = 0; i < 7; i++) {
      db.begin();
      var doc = (EntityImpl) db.newEntity(className);
      doc.save();
      db.commit();
    }

    try {
      var result = db.query("select count(*) from " + className);
      printExecutionPlan(result);
      Assert.assertNotNull(result);
      Assert.assertTrue(result.hasNext());
      var next = result.next();
      Assert.assertNotNull(next);
      Assert.assertEquals(7L, (Object) next.getProperty("count(*)"));
      Assert.assertFalse(result.hasNext());
      result.close();
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail();
    }
  }

  @Test
  public void testCountStar2() {
    var className = "testCountStar2";
    db.getMetadata().getSchema().createClass(className);

    for (var i = 0; i < 10; i++) {
      db.begin();
      var doc = (EntityImpl) db.newEntity(className);
      doc.setProperty("name", "name" + (i % 5));
      doc.save();
      db.commit();
    }
    try {
      var result = db.query("select count(*), name from " + className + " group by name");
      printExecutionPlan(result);
      Assert.assertNotNull(result);
      for (var i = 0; i < 5; i++) {
        Assert.assertTrue(result.hasNext());
        var next = result.next();
        Assert.assertNotNull(next);
        Assert.assertEquals(2L, (Object) next.getProperty("count(*)"));
      }
      Assert.assertFalse(result.hasNext());
      result.close();
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail();
    }
  }

  @Test
  public void testCountStarEmptyNoIndex() {
    var className = "testCountStarEmptyNoIndex";
    db.getMetadata().getSchema().createClass(className);

    db.begin();
    var elem = db.newEntity(className);
    elem.setProperty("name", "bar");
    elem.save();
    db.commit();

    try {
      var result = db.query("select count(*) from " + className + " where name = 'foo'");
      printExecutionPlan(result);
      Assert.assertNotNull(result);
      Assert.assertTrue(result.hasNext());
      var next = result.next();
      Assert.assertNotNull(next);
      Assert.assertEquals(0L, (Object) next.getProperty("count(*)"));
      Assert.assertFalse(result.hasNext());
      result.close();
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail();
    }
  }

  @Test
  public void testCountStarEmptyNoIndexWithAlias() {
    var className = "testCountStarEmptyNoIndexWithAlias";
    db.getMetadata().getSchema().createClass(className);

    db.begin();
    var elem = db.newEntity(className);
    elem.setProperty("name", "bar");
    elem.save();
    db.commit();

    try {
      var result =
          db.query("select count(*) as a from " + className + " where name = 'foo'");
      printExecutionPlan(result);
      Assert.assertNotNull(result);
      Assert.assertTrue(result.hasNext());
      var next = result.next();
      Assert.assertNotNull(next);
      Assert.assertEquals(0L, (Object) next.getProperty("a"));
      Assert.assertFalse(result.hasNext());
      result.close();
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail();
    }
  }

  @Test
  public void testAggretateMixedWithNonAggregate() {
    var className = "testAggretateMixedWithNonAggregate";
    db.getMetadata().getSchema().createClass(className);

    try {
      db.query(
              "select max(a) + max(b) + pippo + pluto as foo, max(d) + max(e), f from " + className)
          .close();
      Assert.fail();
    } catch (CommandExecutionException x) {

    } catch (Exception e) {
      Assert.fail();
    }
  }

  @Test
  public void testAggretateMixedWithNonAggregateInCollection() {
    var className = "testAggretateMixedWithNonAggregateInCollection";
    db.getMetadata().getSchema().createClass(className);

    try {
      db.query("select [max(a), max(b), foo] from " + className).close();
      Assert.fail();
    } catch (CommandExecutionException x) {

    } catch (Exception e) {
      Assert.fail();
    }
  }

  @Test
  public void testAggretateInCollection() {
    var className = "testAggretateInCollection";
    db.getMetadata().getSchema().createClass(className);

    try {
      var query = "select [max(a), max(b)] from " + className;
      var result = db.query(query);
      printExecutionPlan(query, result);
      result.close();
    } catch (Exception x) {
      Assert.fail();
    }
  }

  @Test
  public void testAggretateMixedWithNonAggregateConstants() {
    var className = "testAggretateMixedWithNonAggregateConstants";
    db.getMetadata().getSchema().createClass(className);

    try {
      var result =
          db.query(
              "select max(a + b) + (max(b + c * 2) + 1 + 2) * 3 as foo, max(d) + max(e), f from "
                  + className);
      printExecutionPlan(result);
      result.close();
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail();
    }
  }

  @Test
  public void testAggregateSum() {
    var className = "testAggregateSum";
    db.getMetadata().getSchema().createClass(className);

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i);
      doc.setProperty("val", i);
      doc.save();
      db.commit();
    }

    var result = db.query("select sum(val) from " + className);
    printExecutionPlan(result);
    Assert.assertTrue(result.hasNext());
    var item = result.next();
    Assert.assertNotNull(item);
    Assert.assertEquals(45, (Object) item.getProperty("sum(val)"));

    result.close();
  }

  @Test
  public void testAggregateSumGroupBy() {
    var className = "testAggregateSumGroupBy";
    db.getMetadata().getSchema().createClass(className);
    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("type", i % 2 == 0 ? "even" : "odd");
      doc.setProperty("val", i);
      doc.save();
      db.commit();
    }
    var result = db.query("select sum(val), type from " + className + " group by type");
    printExecutionPlan(result);
    var evenFound = false;
    var oddFound = false;
    for (var i = 0; i < 2; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertNotNull(item);
      if ("even".equals(item.getProperty("type"))) {
        Assert.assertEquals(20, item.<Object>getProperty("sum(val)"));
        evenFound = true;
      } else if ("odd".equals(item.getProperty("type"))) {
        Assert.assertEquals(25, item.<Object>getProperty("sum(val)"));
        oddFound = true;
      }
    }
    Assert.assertFalse(result.hasNext());
    Assert.assertTrue(evenFound);
    Assert.assertTrue(oddFound);
    result.close();
  }

  @Test
  public void testAggregateSumMaxMinGroupBy() {
    var className = "testAggregateSumMaxMinGroupBy";
    db.getMetadata().getSchema().createClass(className);
    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("type", i % 2 == 0 ? "even" : "odd");
      doc.setProperty("val", i);
      doc.save();
      db.commit();
    }
    var result =
        db.query("select sum(val), max(val), min(val), type from " + className + " group by type");
    printExecutionPlan(result);
    var evenFound = false;
    var oddFound = false;
    for (var i = 0; i < 2; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertNotNull(item);
      if ("even".equals(item.getProperty("type"))) {
        Assert.assertEquals(20, item.<Object>getProperty("sum(val)"));
        Assert.assertEquals(8, item.<Object>getProperty("max(val)"));
        Assert.assertEquals(0, item.<Object>getProperty("min(val)"));
        evenFound = true;
      } else if ("odd".equals(item.getProperty("type"))) {
        Assert.assertEquals(25, item.<Object>getProperty("sum(val)"));
        Assert.assertEquals(9, item.<Object>getProperty("max(val)"));
        Assert.assertEquals(1, item.<Object>getProperty("min(val)"));
        oddFound = true;
      }
    }
    Assert.assertFalse(result.hasNext());
    Assert.assertTrue(evenFound);
    Assert.assertTrue(oddFound);
    result.close();
  }

  @Test
  public void testAggregateSumNoGroupByInProjection() {
    var className = "testAggregateSumNoGroupByInProjection";
    db.getMetadata().getSchema().createClass(className);
    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("type", i % 2 == 0 ? "even" : "odd");
      doc.setProperty("val", i);
      doc.save();
      db.commit();
    }
    var result = db.query("select sum(val) from " + className + " group by type");
    printExecutionPlan(result);
    var evenFound = false;
    var oddFound = false;
    for (var i = 0; i < 2; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertNotNull(item);
      var sum = item.getProperty("sum(val)");
      if (sum.equals(20)) {
        evenFound = true;
      } else if (sum.equals(25)) {
        oddFound = true;
      }
    }
    Assert.assertFalse(result.hasNext());
    Assert.assertTrue(evenFound);
    Assert.assertTrue(oddFound);
    result.close();
  }

  @Test
  public void testAggregateSumNoGroupByInProjection2() {
    var className = "testAggregateSumNoGroupByInProjection2";
    db.getMetadata().getSchema().createClass(className);
    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("type", i % 2 == 0 ? "dd1" : "dd2");
      doc.setProperty("val", i);
      doc.save();
      db.commit();
    }
    var result =
        db.query("select sum(val) from " + className + " group by type.substring(0,1)");
    printExecutionPlan(result);
    for (var i = 0; i < 1; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertNotNull(item);
      var sum = item.getProperty("sum(val)");
      Assert.assertEquals(45, sum);
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testFetchFromClusterNumber() {
    var className = "testFetchFromClusterNumber";
    Schema schema = db.getMetadata().getSchema();
    var clazz = schema.createClass(className);
    var targetCluster = clazz.getClusterIds()[0];
    var targetClusterName = db.getClusterNameById(targetCluster);

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("val", i);
      doc.save(targetClusterName);
      db.commit();
    }

    var result = db.query("select from cluster:" + targetClusterName);
    printExecutionPlan(result);
    var sum = 0;
    for (var i = 0; i < 10; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Integer val = item.getProperty("val");
      Assert.assertNotNull(val);
      sum += val;
    }
    Assert.assertEquals(45, sum);
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testFetchFromClusterNumberOrderByRidDesc() {
    var className = "testFetchFromClusterNumberOrderByRidDesc";
    Schema schema = db.getMetadata().getSchema();
    var clazz = schema.createClass(className);
    var targetCluster = clazz.getClusterIds()[0];
    var targetClusterName = db.getClusterNameById(targetCluster);

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("val", i);
      doc.save(targetClusterName);
      db.commit();
    }
    var result =
        db.query("select from cluster:" + targetClusterName + " order by @rid desc");
    printExecutionPlan(result);
    var sum = 0;
    for (var i = 0; i < 10; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Integer val = item.getProperty("val");
      Assert.assertEquals(i, 9 - val);
    }

    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testFetchFromClusterNumberOrderByRidAsc() {
    var className = "testFetchFromClusterNumberOrderByRidAsc";
    Schema schema = db.getMetadata().getSchema();
    var clazz = schema.createClass(className);
    var targetCluster = clazz.getClusterIds()[0];
    var targetClusterName = db.getClusterNameById(targetCluster);

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("val", i);
      doc.save(targetClusterName);
      db.commit();
    }
    var result = db.query(
        "select from cluster:" + targetClusterName + " order by @rid asc");
    printExecutionPlan(result);
    var sum = 0;
    for (var i = 0; i < 10; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Integer val = item.getProperty("val");
      Assert.assertEquals((Object) i, val);
    }

    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testFetchFromClustersNumberOrderByRidAsc() {
    var className = "testFetchFromClustersNumberOrderByRidAsc";
    Schema schema = db.getMetadata().getSchema();
    var clazz = schema.createClass(className);
    if (clazz.getClusterIds().length < 2) {
      clazz.addCluster(db, "testFetchFromClustersNumberOrderByRidAsc_2");
    }
    var targetCluster = clazz.getClusterIds()[0];
    var targetClusterName = db.getClusterNameById(targetCluster);

    var targetCluster2 = clazz.getClusterIds()[1];
    var targetClusterName2 = db.getClusterNameById(targetCluster2);

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("val", i);
      doc.save(targetClusterName);
      db.commit();
    }

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("val", i);
      doc.save(targetClusterName2);
      db.commit();
    }

    var result =
        db.query(
            "select from cluster:["
                + targetClusterName
                + ", "
                + targetClusterName2
                + "] order by @rid asc");
    printExecutionPlan(result);

    for (var i = 0; i < 20; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Integer val = item.getProperty("val");
      Assert.assertEquals((Object) (i % 10), val);
    }

    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testQueryAsTarget() {
    var className = "testQueryAsTarget";
    Schema schema = db.getMetadata().getSchema();
    schema.createClass(className);

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("val", i);
      doc.save();
      db.commit();
    }

    var result =
        db.query("select from (select from " + className + " where val > 2)  where val < 8");
    printExecutionPlan(result);

    for (var i = 0; i < 5; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Integer val = item.getProperty("val");
      Assert.assertTrue(val > 2);
      Assert.assertTrue(val < 8);
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testQuerySchema() {
    var result = db.query("select from metadata:schema");
    printExecutionPlan(result);

    for (var i = 0; i < 1; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertNotNull(item.getProperty("classes"));
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testQueryMetadataIndexManager() {
    var result = db.query("select from metadata:indexmanager");
    printExecutionPlan(result);
    for (var i = 0; i < 1; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertNotNull(item.getProperty("indexes"));
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testQueryMetadataIndexManager2() {
    var result = db.query("select expand(indexes) from metadata:indexmanager");
    printExecutionPlan(result);
    Assert.assertTrue(result.hasNext());
    result.close();
  }

  @Test
  public void testQueryMetadataDatabase() {
    var result = db.query("select from metadata:database");
    printExecutionPlan(result);

    Assert.assertTrue(result.hasNext());
    var item = result.next();
    Assert.assertEquals("testQueryMetadataDatabase", item.getProperty("name"));
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testQueryMetadataStorage() {
    var result = db.query("select from metadata:storage");
    printExecutionPlan(result);

    Assert.assertTrue(result.hasNext());
    var item = result.next();
    Assert.assertEquals("testQueryMetadataStorage", item.getProperty("name"));
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testNonExistingRids() {
    var result = db.query("select from #0:100000000");
    printExecutionPlan(result);
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testFetchFromSingleRid() {
    var result = db.query("select from #0:1");
    printExecutionPlan(result);
    Assert.assertTrue(result.hasNext());
    Assert.assertNotNull(result.next());
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testFetchFromSingleRid2() {
    var result = db.query("select from [#0:1]");
    printExecutionPlan(result);
    Assert.assertTrue(result.hasNext());
    Assert.assertNotNull(result.next());
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testFetchFromSingleRidParam() {
    var result = db.query("select from ?", new RecordId(0, 1));
    printExecutionPlan(result);
    Assert.assertTrue(result.hasNext());
    Assert.assertNotNull(result.next());
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testFetchFromSingleRid3() {
    db.begin();
    var document = (EntityImpl) db.newEntity();
    document.save();
    db.commit();

    var result = db.query("select from [#0:1, #0:2]");
    printExecutionPlan(result);
    Assert.assertTrue(result.hasNext());
    Assert.assertNotNull(result.next());
    Assert.assertTrue(result.hasNext());
    Assert.assertNotNull(result.next());
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testFetchFromSingleRid4() {
    db.begin();
    var document = (EntityImpl) db.newEntity();
    document.save();
    db.commit();

    var result = db.query("select from [#0:1, #0:2, #0:100000]");
    printExecutionPlan(result);
    Assert.assertTrue(result.hasNext());
    Assert.assertNotNull(result.next());
    Assert.assertTrue(result.hasNext());
    Assert.assertNotNull(result.next());
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testFetchFromClassWithIndex() {
    var className = "testFetchFromClassWithIndex";
    var clazz = db.getMetadata().getSchema().createClass(className);
    clazz.createProperty(db, "name", PropertyType.STRING);
    clazz.createIndex(db, className + ".name", SchemaClass.INDEX_TYPE.NOTUNIQUE, "name");

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i);
      doc.save();
      db.commit();
    }

    var result = db.query("select from " + className + " where name = 'name2'");
    printExecutionPlan(result);

    Assert.assertTrue(result.hasNext());
    var next = result.next();
    Assert.assertNotNull(next);
    Assert.assertEquals("name2", next.getProperty("name"));

    Assert.assertFalse(result.hasNext());

    var p = result.getExecutionPlan();
    Assert.assertTrue(p.isPresent());
    var p2 = p.get();
    Assert.assertTrue(p2 instanceof SelectExecutionPlan);
    var plan = (SelectExecutionPlan) p2;
    Assert.assertEquals(FetchFromIndexStep.class, plan.getSteps().get(0).getClass());
    result.close();
  }

  @Test
  public void testFetchFromIndex() {
    var oldAllowManual = GlobalConfiguration.INDEX_ALLOW_MANUAL_INDEXES.getValueAsBoolean();
    GlobalConfiguration.INDEX_ALLOW_MANUAL_INDEXES.setValue(true);
    var className = "testFetchFromIndex";
    var clazz = db.getMetadata().getSchema().createClass(className);
    clazz.createProperty(db, "name", PropertyType.STRING);
    var indexName = className + ".name";
    clazz.createIndex(db, indexName, SchemaClass.INDEX_TYPE.NOTUNIQUE, "name");

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i);
      doc.save();
      db.commit();
    }

    var result = db.query("select from index:" + indexName + " where key = 'name2'");
    printExecutionPlan(result);

    Assert.assertTrue(result.hasNext());
    var next = result.next();
    Assert.assertNotNull(next);

    Assert.assertFalse(result.hasNext());

    var p = result.getExecutionPlan();
    Assert.assertTrue(p.isPresent());
    var p2 = p.get();
    Assert.assertTrue(p2 instanceof SelectExecutionPlan);
    var plan = (SelectExecutionPlan) p2;
    Assert.assertEquals(FetchFromIndexStep.class, plan.getSteps().get(0).getClass());
    result.close();
    GlobalConfiguration.INDEX_ALLOW_MANUAL_INDEXES.setValue(oldAllowManual);
  }

  @Test
  public void testFetchFromIndexHierarchy() {
    var className = "testFetchFromIndexHierarchy";
    var clazz = db.getMetadata().getSchema().createClass(className);
    clazz.createProperty(db, "name", PropertyType.STRING);
    clazz.createIndex(db, className + ".name", SchemaClass.INDEX_TYPE.NOTUNIQUE, "name");

    var classNameExt = "testFetchFromIndexHierarchyExt";
    var clazzExt = db.getMetadata().getSchema().createClass(classNameExt, clazz);
    clazzExt.createIndex(db, classNameExt + ".name", SchemaClass.INDEX_TYPE.NOTUNIQUE, "name");

    for (var i = 0; i < 5; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i);
      doc.save();
      db.commit();
    }

    for (var i = 5; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(classNameExt);
      doc.setProperty("name", "name" + i);
      doc.save();
      db.commit();
    }

    var result = db.query("select from " + classNameExt + " where name = 'name6'");
    printExecutionPlan(result);

    Assert.assertTrue(result.hasNext());
    var next = result.next();
    Assert.assertNotNull(next);

    Assert.assertFalse(result.hasNext());

    var p = result.getExecutionPlan();
    Assert.assertTrue(p.isPresent());
    var p2 = p.get();
    Assert.assertTrue(p2 instanceof SelectExecutionPlan);
    var plan = (SelectExecutionPlan) p2;
    Assert.assertEquals(FetchFromIndexStep.class, plan.getSteps().get(0).getClass());

    Assert.assertEquals(
        ((FetchFromIndexStep) plan.getSteps().get(0)).getIndexName(), classNameExt + ".name");
    result.close();
  }

  @Test
  public void testFetchFromClassWithIndexes() {
    var className = "testFetchFromClassWithIndexes";
    var clazz = db.getMetadata().getSchema().createClass(className);
    clazz.createProperty(db, "name", PropertyType.STRING);
    clazz.createProperty(db, "surname", PropertyType.STRING);
    clazz.createIndex(db, className + ".name", SchemaClass.INDEX_TYPE.NOTUNIQUE, "name");
    clazz.createIndex(db, className + ".surname", SchemaClass.INDEX_TYPE.NOTUNIQUE, "surname");

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i);
      doc.setProperty("surname", "surname" + i);
      doc.save();
      db.commit();
    }

    var result =
        db.query("select from " + className + " where name = 'name2' or surname = 'surname3'");
    printExecutionPlan(result);

    Assert.assertTrue(result.hasNext());
    for (var i = 0; i < 2; i++) {
      var next = result.next();
      Assert.assertNotNull(next);
      Assert.assertTrue(
          "name2".equals(next.getProperty("name"))
              || ("surname3".equals(next.getProperty("surname"))));
    }

    Assert.assertFalse(result.hasNext());

    var p = result.getExecutionPlan();
    Assert.assertTrue(p.isPresent());
    var p2 = p.get();
    Assert.assertTrue(p2 instanceof SelectExecutionPlan);
    var plan = (SelectExecutionPlan) p2;
    Assert.assertEquals(ParallelExecStep.class, plan.getSteps().get(0).getClass());
    var parallel = (ParallelExecStep) plan.getSteps().get(0);
    Assert.assertEquals(2, parallel.getSubExecutionPlans().size());
    result.close();
  }

  @Test
  public void testFetchFromClassWithIndexes2() {
    var className = "testFetchFromClassWithIndexes2";
    var clazz = db.getMetadata().getSchema().createClass(className);
    clazz.createProperty(db, "name", PropertyType.STRING);
    clazz.createProperty(db, "surname", PropertyType.STRING);
    clazz.createIndex(db, className + ".name", SchemaClass.INDEX_TYPE.NOTUNIQUE, "name");
    clazz.createIndex(db, className + ".surname", SchemaClass.INDEX_TYPE.NOTUNIQUE, "surname");

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i);
      doc.setProperty("surname", "surname" + i);
      doc.save();
      db.commit();
    }

    var result =
        db.query(
            "select from "
                + className
                + " where foo is not null and (name = 'name2' or surname = 'surname3')");
    printExecutionPlan(result);

    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testFetchFromClassWithIndexes3() {
    var className = "testFetchFromClassWithIndexes3";
    var clazz = db.getMetadata().getSchema().createClass(className);
    clazz.createProperty(db, "name", PropertyType.STRING);
    clazz.createProperty(db, "surname", PropertyType.STRING);
    clazz.createIndex(db, className + ".name", SchemaClass.INDEX_TYPE.NOTUNIQUE, "name");
    clazz.createIndex(db, className + ".surname", SchemaClass.INDEX_TYPE.NOTUNIQUE, "surname");

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i);
      doc.setProperty("surname", "surname" + i);
      doc.setProperty("foo", i);
      doc.save();
      db.commit();
    }

    var result =
        db.query(
            "select from "
                + className
                + " where foo < 100 and (name = 'name2' or surname = 'surname3')");
    printExecutionPlan(result);

    Assert.assertTrue(result.hasNext());
    for (var i = 0; i < 2; i++) {
      var next = result.next();
      Assert.assertNotNull(next);
      Assert.assertTrue(
          "name2".equals(next.getProperty("name"))
              || ("surname3".equals(next.getProperty("surname"))));
    }

    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testFetchFromClassWithIndexes4() {
    var className = "testFetchFromClassWithIndexes4";
    var clazz = db.getMetadata().getSchema().createClass(className);
    clazz.createProperty(db, "name", PropertyType.STRING);
    clazz.createProperty(db, "surname", PropertyType.STRING);
    clazz.createIndex(db, className + ".name", SchemaClass.INDEX_TYPE.NOTUNIQUE, "name");
    clazz.createIndex(db, className + ".surname", SchemaClass.INDEX_TYPE.NOTUNIQUE, "surname");

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i);
      doc.setProperty("surname", "surname" + i);
      doc.setProperty("foo", i);
      doc.save();
      db.commit();
    }

    var result =
        db.query(
            "select from "
                + className
                + " where foo < 100 and ((name = 'name2' and foo < 20) or surname = 'surname3') and"
                + " ( 4<5 and foo < 50)");
    printExecutionPlan(result);

    Assert.assertTrue(result.hasNext());
    for (var i = 0; i < 2; i++) {
      var next = result.next();
      Assert.assertNotNull(next);
      Assert.assertTrue(
          "name2".equals(next.getProperty("name"))
              || ("surname3".equals(next.getProperty("surname"))));
    }

    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testFetchFromClassWithIndexes5() {
    var className = "testFetchFromClassWithIndexes5";
    var clazz = db.getMetadata().getSchema().createClass(className);
    clazz.createProperty(db, "name", PropertyType.STRING);
    clazz.createProperty(db, "surname", PropertyType.STRING);
    clazz.createIndex(db, className + ".name_surname", SchemaClass.INDEX_TYPE.NOTUNIQUE, "name",
        "surname");

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i);
      doc.setProperty("surname", "surname" + i);
      doc.setProperty("foo", i);
      doc.save();
      db.commit();
    }

    var result =
        db.query("select from " + className + " where name = 'name3' and surname >= 'surname1'");
    printExecutionPlan(result);

    Assert.assertTrue(result.hasNext());
    for (var i = 0; i < 1; i++) {
      var next = result.next();
      Assert.assertNotNull(next);
      Assert.assertEquals("name3", next.getProperty("name"));
    }

    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testFetchFromClassWithIndexes6() {
    var className = "testFetchFromClassWithIndexes6";
    var clazz = db.getMetadata().getSchema().createClass(className);
    clazz.createProperty(db, "name", PropertyType.STRING);
    clazz.createProperty(db, "surname", PropertyType.STRING);
    clazz.createIndex(db, className + ".name_surname", SchemaClass.INDEX_TYPE.NOTUNIQUE, "name",
        "surname");

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i);
      doc.setProperty("surname", "surname" + i);
      doc.setProperty("foo", i);
      doc.save();
      db.commit();
    }

    var result =
        db.query("select from " + className + " where name = 'name3' and surname > 'surname3'");
    printExecutionPlan(result);

    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testFetchFromClassWithIndexes7() {
    var className = "testFetchFromClassWithIndexes7";
    var clazz = db.getMetadata().getSchema().createClass(className);
    clazz.createProperty(db, "name", PropertyType.STRING);
    clazz.createProperty(db, "surname", PropertyType.STRING);
    clazz.createIndex(db, className + ".name_surname", SchemaClass.INDEX_TYPE.NOTUNIQUE, "name",
        "surname");

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i);
      doc.setProperty("surname", "surname" + i);
      doc.setProperty("foo", i);
      doc.save();
      db.commit();
    }

    var result =
        db.query("select from " + className + " where name = 'name3' and surname >= 'surname3'");
    printExecutionPlan(result);
    for (var i = 0; i < 1; i++) {
      var next = result.next();
      Assert.assertNotNull(next);
      Assert.assertEquals("name3", next.getProperty("name"));
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testFetchFromClassWithIndexes8() {
    var className = "testFetchFromClassWithIndexes8";
    var clazz = db.getMetadata().getSchema().createClass(className);
    clazz.createProperty(db, "name", PropertyType.STRING);
    clazz.createProperty(db, "surname", PropertyType.STRING);
    clazz.createIndex(db, className + ".name_surname", SchemaClass.INDEX_TYPE.NOTUNIQUE, "name",
        "surname");

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i);
      doc.setProperty("surname", "surname" + i);
      doc.setProperty("foo", i);
      doc.save();
      db.commit();
    }

    var result =
        db.query("select from " + className + " where name = 'name3' and surname < 'surname3'");
    printExecutionPlan(result);

    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testFetchFromClassWithIndexes9() {
    var className = "testFetchFromClassWithIndexes9";
    var clazz = db.getMetadata().getSchema().createClass(className);
    clazz.createProperty(db, "name", PropertyType.STRING);
    clazz.createProperty(db, "surname", PropertyType.STRING);
    clazz.createIndex(db, className + ".name_surname", SchemaClass.INDEX_TYPE.NOTUNIQUE, "name",
        "surname");

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i);
      doc.setProperty("surname", "surname" + i);
      doc.setProperty("foo", i);
      doc.save();
      db.commit();
    }

    var result =
        db.query("select from " + className + " where name = 'name3' and surname <= 'surname3'");
    printExecutionPlan(result);
    for (var i = 0; i < 1; i++) {
      var next = result.next();
      Assert.assertNotNull(next);
      Assert.assertEquals("name3", next.getProperty("name"));
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testFetchFromClassWithIndexes10() {
    var className = "testFetchFromClassWithIndexes10";
    var clazz = db.getMetadata().getSchema().createClass(className);
    clazz.createProperty(db, "name", PropertyType.STRING);
    clazz.createProperty(db, "surname", PropertyType.STRING);
    clazz.createIndex(db, className + ".name_surname", SchemaClass.INDEX_TYPE.NOTUNIQUE, "name",
        "surname");

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i);
      doc.setProperty("surname", "surname" + i);
      doc.setProperty("foo", i);
      doc.save();
      db.commit();
    }

    var result = db.query("select from " + className + " where name > 'name3' ");
    printExecutionPlan(result);
    for (var i = 0; i < 6; i++) {
      Assert.assertTrue(result.hasNext());
      var next = result.next();
      Assert.assertNotNull(next);
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testFetchFromClassWithIndexes11() {
    var className = "testFetchFromClassWithIndexes11";
    var clazz = db.getMetadata().getSchema().createClass(className);
    clazz.createProperty(db, "name", PropertyType.STRING);
    clazz.createProperty(db, "surname", PropertyType.STRING);
    clazz.createIndex(db, className + ".name_surname", SchemaClass.INDEX_TYPE.NOTUNIQUE, "name",
        "surname");

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i);
      doc.setProperty("surname", "surname" + i);
      doc.setProperty("foo", i);
      doc.save();
      db.commit();
    }

    var result = db.query("select from " + className + " where name >= 'name3' ");
    printExecutionPlan(result);
    for (var i = 0; i < 7; i++) {
      var next = result.next();
      Assert.assertNotNull(next);
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testFetchFromClassWithIndexes12() {
    var className = "testFetchFromClassWithIndexes12";
    var clazz = db.getMetadata().getSchema().createClass(className);
    clazz.createProperty(db, "name", PropertyType.STRING);
    clazz.createProperty(db, "surname", PropertyType.STRING);
    clazz.createIndex(db, className + ".name_surname", SchemaClass.INDEX_TYPE.NOTUNIQUE, "name",
        "surname");

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i);
      doc.setProperty("surname", "surname" + i);
      doc.setProperty("foo", i);
      doc.save();
      db.commit();
    }

    var result = db.query("select from " + className + " where name < 'name3' ");
    printExecutionPlan(result);
    for (var i = 0; i < 3; i++) {
      var next = result.next();
      Assert.assertNotNull(next);
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testFetchFromClassWithIndexes13() {
    var className = "testFetchFromClassWithIndexes13";
    var clazz = db.getMetadata().getSchema().createClass(className);
    clazz.createProperty(db, "name", PropertyType.STRING);
    clazz.createProperty(db, "surname", PropertyType.STRING);
    clazz.createIndex(db, className + ".name_surname", SchemaClass.INDEX_TYPE.NOTUNIQUE, "name",
        "surname");

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i);
      doc.setProperty("surname", "surname" + i);
      doc.setProperty("foo", i);
      doc.save();
      db.commit();
    }

    var result = db.query("select from " + className + " where name <= 'name3' ");
    printExecutionPlan(result);
    for (var i = 0; i < 4; i++) {
      var next = result.next();
      Assert.assertNotNull(next);
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testFetchFromClassWithIndexes14() {
    var className = "testFetchFromClassWithIndexes14";
    var clazz = db.getMetadata().getSchema().createClass(className);
    clazz.createProperty(db, "name", PropertyType.STRING);
    clazz.createProperty(db, "surname", PropertyType.STRING);
    clazz.createIndex(db, className + ".name_surname", SchemaClass.INDEX_TYPE.NOTUNIQUE, "name",
        "surname");

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i);
      doc.setProperty("surname", "surname" + i);
      doc.setProperty("foo", i);
      doc.save();
      db.commit();
    }

    var result =
        db.query("select from " + className + " where name > 'name3' and name < 'name5'");
    printExecutionPlan(result);
    for (var i = 0; i < 1; i++) {
      var next = result.next();
      Assert.assertNotNull(next);
    }
    Assert.assertFalse(result.hasNext());
    var plan = (SelectExecutionPlan) result.getExecutionPlan().get();
    Assert.assertEquals(
        1, plan.getSteps().stream().filter(step -> step instanceof FetchFromIndexStep).count());
    result.close();
  }

  @Test
  public void testFetchFromClassWithIndexes15() {
    var className = "testFetchFromClassWithIndexes15";
    var clazz = db.getMetadata().getSchema().createClass(className);
    clazz.createProperty(db, "name", PropertyType.STRING);
    clazz.createProperty(db, "surname", PropertyType.STRING);
    clazz.createIndex(db, className + ".name_surname", SchemaClass.INDEX_TYPE.NOTUNIQUE, "name",
        "surname");

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i);
      doc.setProperty("surname", "surname" + i);
      doc.setProperty("foo", i);
      doc.save();
      db.commit();
    }

    var result =
        db.query(
            "select from "
                + className
                + " where name > 'name6' and name = 'name3' and surname > 'surname2' and surname <"
                + " 'surname5' ");
    printExecutionPlan(result);
    Assert.assertFalse(result.hasNext());
    var plan = (SelectExecutionPlan) result.getExecutionPlan().get();
    Assert.assertEquals(
        1, plan.getSteps().stream().filter(step -> step instanceof FetchFromIndexStep).count());
    result.close();
  }

  @Test
  public void testFetchFromClassWithHashIndexes1() {
    var className = "testFetchFromClassWithHashIndexes1";
    var clazz = db.getMetadata().getSchema().createClass(className);
    clazz.createProperty(db, "name", PropertyType.STRING);
    clazz.createProperty(db, "surname", PropertyType.STRING);
    clazz.createIndex(db,
        className + ".name_surname", SchemaClass.INDEX_TYPE.NOTUNIQUE, "name",
        "surname");

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i);
      doc.setProperty("surname", "surname" + i);
      doc.setProperty("foo", i);
      doc.save();
      db.commit();
    }

    var result =
        db.query("select from " + className + " where name = 'name6' and surname = 'surname6' ");
    printExecutionPlan(result);

    for (var i = 0; i < 1; i++) {
      Assert.assertTrue(result.hasNext());
      var next = result.next();
      Assert.assertNotNull(next);
    }
    Assert.assertFalse(result.hasNext());
    var plan = (SelectExecutionPlan) result.getExecutionPlan().get();
    Assert.assertEquals(
        1, plan.getSteps().stream().filter(step -> step instanceof FetchFromIndexStep).count());
    result.close();
  }

  @Test
  public void testFetchFromClassWithHashIndexes2() {
    var className = "testFetchFromClassWithHashIndexes2";
    var clazz = db.getMetadata().getSchema().createClass(className);
    clazz.createProperty(db, "name", PropertyType.STRING);
    clazz.createProperty(db, "surname", PropertyType.STRING);
    clazz.createIndex(db,
        className + ".name_surname", SchemaClass.INDEX_TYPE.NOTUNIQUE, "name",
        "surname");

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i);
      doc.setProperty("surname", "surname" + i);
      doc.setProperty("foo", i);
      doc.save();
      db.commit();
    }

    var result =
        db.query("select from " + className + " where name = 'name6' and surname >= 'surname6' ");
    printExecutionPlan(result);

    for (var i = 0; i < 1; i++) {
      Assert.assertTrue(result.hasNext());
      var next = result.next();
      Assert.assertNotNull(next);
    }
    Assert.assertFalse(result.hasNext());
    var plan = (SelectExecutionPlan) result.getExecutionPlan().get();
    Assert.assertEquals(
        FetchFromIndexStep.class, plan.getSteps().getFirst().getClass()); // index not used
    result.close();
  }

  @Test
  public void testExpand1() {
    var childClassName = "testExpand1_child";
    var parentClassName = "testExpand1_parent";
    var childClass = db.getMetadata().getSchema().createClass(childClassName);
    var parentClass = db.getMetadata().getSchema().createClass(parentClassName);

    var count = 10;
    for (var i = 0; i < count; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(childClassName);
      doc.setProperty("name", "name" + i);
      doc.setProperty("surname", "surname" + i);
      doc.setProperty("foo", i);
      doc.save();

      var parent = (EntityImpl) db.newEntity(parentClassName);
      parent.setProperty("linked", doc);
      parent.save();
      db.commit();
    }

    var result = db.query("select expand(linked) from " + parentClassName);
    printExecutionPlan(result);

    for (var i = 0; i < count; i++) {
      Assert.assertTrue(result.hasNext());
      var next = result.next();
      Assert.assertNotNull(next);
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testExpand2() {
    var childClassName = "testExpand2_child";
    var parentClassName = "testExpand2_parent";
    var childClass = db.getMetadata().getSchema().createClass(childClassName);
    var parentClass = db.getMetadata().getSchema().createClass(parentClassName);

    db.begin();
    var count = 10;
    var collSize = 11;
    for (var i = 0; i < count; i++) {
      List coll = new ArrayList<>();
      for (var j = 0; j < collSize; j++) {
        EntityImpl doc = db.newInstance(childClassName);
        doc.setProperty("name", "name" + i);
        doc.save();
        coll.add(doc);
      }

      var parent = (EntityImpl) db.newEntity(parentClassName);
      parent.setProperty("linked", coll);
      parent.save();
    }
    db.commit();

    var result = db.query("select expand(linked) from " + parentClassName);
    printExecutionPlan(result);

    for (var i = 0; i < count * collSize; i++) {
      Assert.assertTrue(result.hasNext());
      var next = result.next();
      Assert.assertNotNull(next);
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testExpand3() {
    var childClassName = "testExpand3_child";
    var parentClassName = "testExpand3_parent";
    var childClass = db.getMetadata().getSchema().createClass(childClassName);
    var parentClass = db.getMetadata().getSchema().createClass(parentClassName);

    db.begin();
    var count = 30;
    var collSize = 7;
    for (var i = 0; i < count; i++) {
      List coll = new ArrayList<>();
      for (var j = 0; j < collSize; j++) {
        EntityImpl doc = db.newInstance(childClassName);
        doc.setProperty("name", "name" + j);
        doc.save();
        coll.add(doc);
      }

      var parent = (EntityImpl) db.newEntity(parentClassName);
      parent.setProperty("linked", coll);
      parent.save();
    }
    db.commit();

    var result =
        db.query("select expand(linked) from " + parentClassName + " order by name");
    printExecutionPlan(result);

    String last = null;
    for (var i = 0; i < count * collSize; i++) {
      Assert.assertTrue(result.hasNext());
      var next = result.next();
      if (i > 0) {
        Assert.assertTrue(last.compareTo(next.getProperty("name")) <= 0);
      }
      last = next.getProperty("name");
      Assert.assertNotNull(next);
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testDistinct1() {
    var className = "testDistinct1";
    var clazz = db.getMetadata().getSchema().createClass(className);
    clazz.createProperty(db, "name", PropertyType.STRING);
    clazz.createProperty(db, "surname", PropertyType.STRING);

    for (var i = 0; i < 30; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i % 10);
      doc.setProperty("surname", "surname" + i % 10);
      doc.save();
      db.commit();
    }

    var result = db.query("select distinct name, surname from " + className);
    printExecutionPlan(result);

    for (var i = 0; i < 10; i++) {
      Assert.assertTrue(result.hasNext());
      var next = result.next();
      Assert.assertNotNull(next);
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testDistinct2() {
    var className = "testDistinct2";
    var clazz = db.getMetadata().getSchema().createClass(className);
    clazz.createProperty(db, "name", PropertyType.STRING);
    clazz.createProperty(db, "surname", PropertyType.STRING);

    for (var i = 0; i < 30; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i % 10);
      doc.setProperty("surname", "surname" + i % 10);
      doc.save();
      db.commit();
    }

    var result = db.query("select distinct(name) from " + className);
    printExecutionPlan(result);

    for (var i = 0; i < 10; i++) {
      Assert.assertTrue(result.hasNext());
      var next = result.next();
      Assert.assertNotNull(next);
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testLet1() {
    var result = db.query("select $a as one, $b as two let $a = 1, $b = 1+1");
    Assert.assertTrue(result.hasNext());
    var item = result.next();
    Assert.assertNotNull(item);
    Assert.assertEquals(1, item.<Object>getProperty("one"));
    Assert.assertEquals(2, item.<Object>getProperty("two"));
    printExecutionPlan(result);
    result.close();
  }

  @Test
  public void testLet1Long() {
    var result = db.query("select $a as one, $b as two let $a = 1L, $b = 1L+1");
    Assert.assertTrue(result.hasNext());
    var item = result.next();
    Assert.assertNotNull(item);
    Assert.assertEquals(1L, item.<Object>getProperty("one"));
    Assert.assertEquals(2L, item.<Object>getProperty("two"));
    printExecutionPlan(result);
    result.close();
  }

  @Test
  public void testLet2() {
    var result = db.query("select $a as one let $a = (select 1 as a)");
    printExecutionPlan(result);
    Assert.assertTrue(result.hasNext());
    var item = result.next();
    Assert.assertNotNull(item);
    var one = item.getProperty("one");
    Assert.assertTrue(one instanceof List);
    Assert.assertEquals(1, ((List) one).size());
    var x = ((List) one).get(0);
    Assert.assertTrue(x instanceof Result);
    Assert.assertEquals(1, (Object) ((Result) x).getProperty("a"));
    result.close();
  }

  @Test
  public void testLet3() {
    var result = db.query("select $a[0].foo as one let $a = (select 1 as foo)");
    printExecutionPlan(result);
    Assert.assertTrue(result.hasNext());
    var item = result.next();
    Assert.assertNotNull(item);
    var one = item.getProperty("one");
    Assert.assertEquals(1, one);
    result.close();
  }

  @Test
  public void testLet4() {
    var className = "testLet4";
    db.getMetadata().getSchema().createClass(className);

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i);
      doc.setProperty("surname", "surname" + i);
      doc.save();
      db.commit();
    }

    var result =
        db.query(
            "select name, surname, $nameAndSurname as fullname from "
                + className
                + " let $nameAndSurname = name + ' ' + surname");
    printExecutionPlan(result);
    for (var i = 0; i < 10; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertNotNull(item);
      Assert.assertEquals(
          item.getProperty("fullname"),
          item.getProperty("name") + " " + item.getProperty("surname"));
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testLet5() {
    var className = "testLet5";
    db.getMetadata().getSchema().createClass(className);

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i);
      doc.setProperty("surname", "surname" + i);
      doc.save();
      db.commit();
    }

    var result =
        db.query(
            "select from "
                + className
                + " where name in (select name from "
                + className
                + " where name = 'name1')");
    printExecutionPlan(result);
    for (var i = 0; i < 1; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertNotNull(item);
      Assert.assertEquals("name1", item.getProperty("name"));
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testLet6() {
    var className = "testLet6";
    db.getMetadata().getSchema().createClass(className);

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i);
      doc.setProperty("surname", "surname" + i);
      doc.save();
      db.commit();
    }

    var result =
        db.query(
            "select $foo as name from "
                + className
                + " let $foo = (select name from "
                + className
                + " where name = $parent.$current.name)");
    printExecutionPlan(result);
    for (var i = 0; i < 10; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertNotNull(item);
      Assert.assertNotNull(item.getProperty("name"));
      Assert.assertTrue(item.getProperty("name") instanceof Collection);
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testLet7() {
    var className = "testLet7";
    db.getMetadata().getSchema().createClass(className);

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i);
      doc.setProperty("surname", "surname" + i);
      doc.save();
      db.commit();
    }

    var result =
        db.query(
            "select $bar as name from "
                + className
                + " "
                + "let $foo = (select name from "
                + className
                + " where name = $parent.$current.name),"
                + "$bar = $foo[0].name");
    printExecutionPlan(result);
    for (var i = 0; i < 10; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertNotNull(item);
      Assert.assertNotNull(item.getProperty("name"));
      Assert.assertTrue(item.getProperty("name") instanceof String);
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testLetWithTraverseFunction() {
    var vertexClassName = "testLetWithTraverseFunction";
    var edgeClassName = "testLetWithTraverseFunctioEdge";

    var vertexClass = db.createVertexClass(vertexClassName);

    db.begin();
    var doc1 = db.newVertex(vertexClass);
    doc1.setProperty("name", "A");
    doc1.save();

    var doc2 = db.newVertex(vertexClass);
    doc2.setProperty("name", "B");
    doc2.save();
    db.commit();

    var doc2Id = doc2.getIdentity();

    var edgeClass = db.createEdgeClass(edgeClassName);

    db.begin();
    doc1 = db.bindToSession(doc1);
    doc2 = db.bindToSession(doc2);

    db.newRegularEdge(doc1, doc2, edgeClass).save();
    db.commit();

    var queryString =
        "SELECT $x, name FROM " + vertexClassName + " let $x = out(\"" + edgeClassName + "\")";
    var resultSet = db.query(queryString);
    var counter = 0;
    while (resultSet.hasNext()) {
      var result = resultSet.next();
      Iterable edge = result.getProperty("$x");
      Iterator<Identifiable> iter = edge.iterator();
      while (iter.hasNext()) {
        Vertex toVertex = db.load(iter.next().getIdentity());
        if (doc2Id.equals(toVertex.getIdentity())) {
          ++counter;
        }
      }
    }
    Assert.assertEquals(1, counter);
    resultSet.close();
  }

  @Test
  public void testLetVariableSubqueryProjectionFetchFromClassTarget_9695() {
    var className = "testLetVariableSubqueryProjectionFetchFromClassTarget_9695";
    db.getMetadata().getSchema().createClass(className);

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("i", i);
      doc.setProperty("iSeq", new int[]{i, 2 * i, 4 * i});
      doc.save();
      db.commit();
    }

    var result =
        db.query(
            "select $current.*, $b.*, $b.@class from (select 1 as sqa, @class as sqc from "
                + className
                + " LIMIT 2)\n"
                + "let $b = $current");
    printExecutionPlan(result);
    Assert.assertTrue(result.hasNext());
    var item = result.next();
    Assert.assertNotNull(item);
    var currentProperty = item.getProperty("$current.*");
    Assert.assertTrue(currentProperty instanceof Result);
    final var currentResult = (Result) currentProperty;
    Assert.assertTrue(currentResult.isProjection());
    Assert.assertEquals(Integer.valueOf(1), currentResult.<Integer>getProperty("sqa"));
    Assert.assertEquals(className, currentResult.getProperty("sqc"));
    var bProperty = item.getProperty("$b.*");
    Assert.assertTrue(bProperty instanceof Result);
    final var bResult = (Result) bProperty;
    Assert.assertTrue(bResult.isProjection());
    Assert.assertEquals(Integer.valueOf(1), bResult.<Integer>getProperty("sqa"));
    Assert.assertEquals(className, bResult.getProperty("sqc"));
    result.close();
  }

  @Test
  public void testUnwind1() {
    var className = "testUnwind1";
    db.getMetadata().getSchema().createClass(className);

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("i", i);
      doc.setProperty("iSeq", new int[]{i, 2 * i, 4 * i});
      doc.save();
      db.commit();
    }

    var result = db.query("select i, iSeq from " + className + " unwind iSeq");
    printExecutionPlan(result);
    for (var i = 0; i < 30; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertNotNull(item);
      Assert.assertNotNull(item.getProperty("i"));
      Assert.assertNotNull(item.getProperty("iSeq"));
      Integer first = item.getProperty("i");
      Integer second = item.getProperty("iSeq");
      Assert.assertTrue(first + second == 0 || second.intValue() % first.intValue() == 0);
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testUnwind2() {
    var className = "testUnwind2";
    db.getMetadata().getSchema().createClass(className);

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("i", i);
      List<Integer> iSeq = new ArrayList<>();
      iSeq.add(i);
      iSeq.add(i * 2);
      iSeq.add(i * 4);
      doc.setProperty("iSeq", iSeq);
      doc.save();
      db.commit();
    }

    var result = db.query("select i, iSeq from " + className + " unwind iSeq");
    printExecutionPlan(result);
    for (var i = 0; i < 30; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertNotNull(item);
      Assert.assertNotNull(item.getProperty("i"));
      Assert.assertNotNull(item.getProperty("iSeq"));
      Integer first = item.getProperty("i");
      Integer second = item.getProperty("iSeq");
      Assert.assertTrue(first + second == 0 || second.intValue() % first.intValue() == 0);
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testFetchFromSubclassIndexes1() {
    var parent = "testFetchFromSubclassIndexes1_parent";
    var child1 = "testFetchFromSubclassIndexes1_child1";
    var child2 = "testFetchFromSubclassIndexes1_child2";
    var parentClass = db.getMetadata().getSchema().createClass(parent);
    var childClass1 = db.getMetadata().getSchema().createClass(child1, parentClass);
    var childClass2 = db.getMetadata().getSchema().createClass(child2, parentClass);

    parentClass.createProperty(db, "name", PropertyType.STRING);
    childClass1.createIndex(db, child1 + ".name", SchemaClass.INDEX_TYPE.NOTUNIQUE, "name");
    childClass2.createIndex(db, child2 + ".name", SchemaClass.INDEX_TYPE.NOTUNIQUE, "name");

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(child1);
      doc.setProperty("name", "name" + i);
      doc.save();
      db.commit();
    }

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(child2);
      doc.setProperty("name", "name" + i);
      doc.save();
      db.commit();
    }

    var result = db.query("select from " + parent + " where name = 'name1'");
    printExecutionPlan(result);
    var plan = (InternalExecutionPlan) result.getExecutionPlan().get();
    Assert.assertTrue(plan.getSteps().get(0) instanceof ParallelExecStep);
    for (var i = 0; i < 2; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertNotNull(item);
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testFetchFromSubclassIndexes2() {
    var parent = "testFetchFromSubclassIndexes2_parent";
    var child1 = "testFetchFromSubclassIndexes2_child1";
    var child2 = "testFetchFromSubclassIndexes2_child2";
    var parentClass = db.getMetadata().getSchema().createClass(parent);
    var childClass1 = db.getMetadata().getSchema().createClass(child1, parentClass);
    var childClass2 = db.getMetadata().getSchema().createClass(child2, parentClass);

    parentClass.createProperty(db, "name", PropertyType.STRING);
    childClass1.createIndex(db, child1 + ".name", SchemaClass.INDEX_TYPE.NOTUNIQUE, "name");
    childClass2.createIndex(db, child2 + ".name", SchemaClass.INDEX_TYPE.NOTUNIQUE, "name");

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(child1);
      doc.setProperty("name", "name" + i);
      doc.setProperty("surname", "surname" + i);
      doc.save();
      db.commit();
    }

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(child2);
      doc.setProperty("name", "name" + i);
      doc.setProperty("surname", "surname" + i);
      doc.save();
      db.commit();
    }

    var result =
        db.query("select from " + parent + " where name = 'name1' and surname = 'surname1'");
    printExecutionPlan(result);
    var plan = (InternalExecutionPlan) result.getExecutionPlan().get();
    Assert.assertTrue(plan.getSteps().get(0) instanceof ParallelExecStep);
    for (var i = 0; i < 2; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertNotNull(item);
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testFetchFromSubclassIndexes3() {
    var parent = "testFetchFromSubclassIndexes3_parent";
    var child1 = "testFetchFromSubclassIndexes3_child1";
    var child2 = "testFetchFromSubclassIndexes3_child2";
    var parentClass = db.getMetadata().getSchema().createClass(parent);
    var childClass1 = db.getMetadata().getSchema().createClass(child1, parentClass);
    var childClass2 = db.getMetadata().getSchema().createClass(child2, parentClass);

    parentClass.createProperty(db, "name", PropertyType.STRING);
    childClass1.createIndex(db, child1 + ".name", SchemaClass.INDEX_TYPE.NOTUNIQUE, "name");

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(child1);
      doc.setProperty("name", "name" + i);
      doc.setProperty("surname", "surname" + i);
      doc.save();
      db.commit();
    }

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(child2);
      doc.setProperty("name", "name" + i);
      doc.setProperty("surname", "surname" + i);
      doc.save();
      db.commit();
    }

    var result =
        db.query("select from " + parent + " where name = 'name1' and surname = 'surname1'");
    printExecutionPlan(result);
    var plan = (InternalExecutionPlan) result.getExecutionPlan().get();
    Assert.assertTrue(
        plan.getSteps().get(0) instanceof FetchFromClassExecutionStep); // no index used
    for (var i = 0; i < 2; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertNotNull(item);
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testFetchFromSubclassIndexes4() {
    var parent = "testFetchFromSubclassIndexes4_parent";
    var child1 = "testFetchFromSubclassIndexes4_child1";
    var child2 = "testFetchFromSubclassIndexes4_child2";
    var parentClass = db.getMetadata().getSchema().createClass(parent);
    var childClass1 = db.getMetadata().getSchema().createClass(child1, parentClass);
    var childClass2 = db.getMetadata().getSchema().createClass(child2, parentClass);

    parentClass.createProperty(db, "name", PropertyType.STRING);
    childClass1.createIndex(db, child1 + ".name", SchemaClass.INDEX_TYPE.NOTUNIQUE, "name");
    childClass2.createIndex(db, child2 + ".name", SchemaClass.INDEX_TYPE.NOTUNIQUE, "name");

    db.begin();
    EntityImpl parentdoc = db.newInstance(parent);
    parentdoc.setProperty("name", "foo");
    parentdoc.save();
    db.commit();

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(child1);
      doc.setProperty("name", "name" + i);
      doc.setProperty("surname", "surname" + i);
      doc.save();
      db.commit();
    }

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(child2);
      doc.setProperty("name", "name" + i);
      doc.setProperty("surname", "surname" + i);
      doc.save();
      db.commit();
    }

    var result =
        db.query("select from " + parent + " where name = 'name1' and surname = 'surname1'");
    printExecutionPlan(result);
    var plan = (InternalExecutionPlan) result.getExecutionPlan().get();
    Assert.assertTrue(
        plan.getSteps().get(0)
            instanceof
            FetchFromClassExecutionStep); // no index, because the superclass is not empty
    for (var i = 0; i < 2; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertNotNull(item);
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testFetchFromSubSubclassIndexes() {
    var parent = "testFetchFromSubSubclassIndexes_parent";
    var child1 = "testFetchFromSubSubclassIndexes_child1";
    var child2 = "testFetchFromSubSubclassIndexes_child2";
    var child2_1 = "testFetchFromSubSubclassIndexes_child2_1";
    var child2_2 = "testFetchFromSubSubclassIndexes_child2_2";
    var parentClass = db.getMetadata().getSchema().createClass(parent);
    var childClass1 = db.getMetadata().getSchema().createClass(child1, parentClass);
    var childClass2 = db.getMetadata().getSchema().createClass(child2, parentClass);
    var childClass2_1 = db.getMetadata().getSchema().createClass(child2_1, childClass2);
    var childClass2_2 = db.getMetadata().getSchema().createClass(child2_2, childClass2);

    parentClass.createProperty(db, "name", PropertyType.STRING);
    childClass1.createIndex(db, child1 + ".name", SchemaClass.INDEX_TYPE.NOTUNIQUE, "name");
    childClass2_1.createIndex(db, child2_1 + ".name", SchemaClass.INDEX_TYPE.NOTUNIQUE, "name");
    childClass2_2.createIndex(db, child2_2 + ".name", SchemaClass.INDEX_TYPE.NOTUNIQUE, "name");

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(child1);
      doc.setProperty("name", "name" + i);
      doc.setProperty("surname", "surname" + i);
      doc.save();
      db.commit();
    }

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(child2_1);
      doc.setProperty("name", "name" + i);
      doc.setProperty("surname", "surname" + i);
      doc.save();
      db.commit();
    }

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(child2_2);
      doc.setProperty("name", "name" + i);
      doc.setProperty("surname", "surname" + i);
      doc.save();
      db.commit();
    }

    var result =
        db.query("select from " + parent + " where name = 'name1' and surname = 'surname1'");
    printExecutionPlan(result);
    var plan = (InternalExecutionPlan) result.getExecutionPlan().get();
    Assert.assertTrue(plan.getSteps().get(0) instanceof ParallelExecStep);
    for (var i = 0; i < 3; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertNotNull(item);
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testFetchFromSubSubclassIndexesWithDiamond() {
    var parent = "testFetchFromSubSubclassIndexesWithDiamond_parent";
    var child1 = "testFetchFromSubSubclassIndexesWithDiamond_child1";
    var child2 = "testFetchFromSubSubclassIndexesWithDiamond_child2";
    var child12 = "testFetchFromSubSubclassIndexesWithDiamond_child12";

    var parentClass = db.getMetadata().getSchema().createClass(parent);
    var childClass1 = db.getMetadata().getSchema().createClass(child1, parentClass);
    var childClass2 = db.getMetadata().getSchema().createClass(child2, parentClass);
    var childClass12 =
        db.getMetadata().getSchema().createClass(child12, childClass1, childClass2);

    parentClass.createProperty(db, "name", PropertyType.STRING);
    childClass1.createIndex(db, child1 + ".name", SchemaClass.INDEX_TYPE.NOTUNIQUE, "name");
    childClass2.createIndex(db, child2 + ".name", SchemaClass.INDEX_TYPE.NOTUNIQUE, "name");

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(child1);
      doc.setProperty("name", "name" + i);
      doc.setProperty("surname", "surname" + i);
      doc.save();
      db.commit();
    }

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(child2);
      doc.setProperty("name", "name" + i);
      doc.setProperty("surname", "surname" + i);
      doc.save();
      db.commit();
    }

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(child12);
      doc.setProperty("name", "name" + i);
      doc.setProperty("surname", "surname" + i);
      doc.save();
      db.commit();
    }

    var result =
        db.query("select from " + parent + " where name = 'name1' and surname = 'surname1'");
    printExecutionPlan(result);
    var plan = (InternalExecutionPlan) result.getExecutionPlan().get();
    Assert.assertTrue(plan.getSteps().get(0) instanceof FetchFromClassExecutionStep);
    for (var i = 0; i < 3; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertNotNull(item);
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testIndexPlusSort1() {
    var className = "testIndexPlusSort1";
    var clazz = db.getMetadata().getSchema().createClass(className);
    clazz.createProperty(db, "name", PropertyType.STRING);
    clazz.createProperty(db, "surname", PropertyType.STRING);
    db.command(
            "create index "
                + className
                + ".name_surname on "
                + className
                + " (name, surname) NOTUNIQUE")
        .close();

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i % 3);
      doc.setProperty("surname", "surname" + i);
      doc.save();
      db.commit();
    }

    var result =
        db.query("select from " + className + " where name = 'name1' order by surname ASC");
    printExecutionPlan(result);
    String lastSurname = null;
    for (var i = 0; i < 3; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertNotNull(item);
      Assert.assertNotNull(item.getProperty("surname"));

      String surname = item.getProperty("surname");
      if (i > 0) {
        Assert.assertTrue(surname.compareTo(lastSurname) > 0);
      }
      lastSurname = surname;
    }
    Assert.assertFalse(result.hasNext());
    var plan = result.getExecutionPlan().get();
    Assert.assertEquals(
        1, plan.getSteps().stream().filter(step -> step instanceof FetchFromIndexStep).count());
    Assert.assertEquals(
        0, plan.getSteps().stream().filter(step -> step instanceof OrderByStep).count());
    result.close();
  }

  @Test
  public void testIndexPlusSort2() {
    var className = "testIndexPlusSort2";
    var clazz = db.getMetadata().getSchema().createClass(className);
    clazz.createProperty(db, "name", PropertyType.STRING);
    clazz.createProperty(db, "surname", PropertyType.STRING);
    db.command(
            "create index "
                + className
                + ".name_surname on "
                + className
                + " (name, surname) NOTUNIQUE")
        .close();

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i % 3);
      doc.setProperty("surname", "surname" + i);
      doc.save();
      db.commit();
    }

    var result =
        db.query("select from " + className + " where name = 'name1' order by surname DESC");
    printExecutionPlan(result);
    String lastSurname = null;
    for (var i = 0; i < 3; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertNotNull(item);
      Assert.assertNotNull(item.getProperty("surname"));

      String surname = item.getProperty("surname");
      if (i > 0) {
        Assert.assertTrue(surname.compareTo(lastSurname) < 0);
      }
      lastSurname = surname;
    }
    Assert.assertFalse(result.hasNext());
    var plan = result.getExecutionPlan().get();
    Assert.assertEquals(
        1, plan.getSteps().stream().filter(step -> step instanceof FetchFromIndexStep).count());
    Assert.assertEquals(
        0, plan.getSteps().stream().filter(step -> step instanceof OrderByStep).count());
    result.close();
  }

  @Test
  public void testIndexPlusSort3() {
    var className = "testIndexPlusSort3";
    var clazz = db.getMetadata().getSchema().createClass(className);
    clazz.createProperty(db, "name", PropertyType.STRING);
    clazz.createProperty(db, "surname", PropertyType.STRING);
    db.command(
            "create index "
                + className
                + ".name_surname on "
                + className
                + " (name, surname) NOTUNIQUE")
        .close();

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i % 3);
      doc.setProperty("surname", "surname" + i);
      doc.save();
      db.commit();
    }

    var result =
        db.query(
            "select from " + className + " where name = 'name1' order by name DESC, surname DESC");
    printExecutionPlan(result);
    String lastSurname = null;
    for (var i = 0; i < 3; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertNotNull(item);
      Assert.assertNotNull(item.getProperty("surname"));

      String surname = item.getProperty("surname");
      if (i > 0) {
        Assert.assertTrue(((String) item.getProperty("surname")).compareTo(lastSurname) < 0);
      }
      lastSurname = surname;
    }
    Assert.assertFalse(result.hasNext());
    var plan = result.getExecutionPlan().get();
    Assert.assertEquals(
        1, plan.getSteps().stream().filter(step -> step instanceof FetchFromIndexStep).count());
    Assert.assertEquals(
        0, plan.getSteps().stream().filter(step -> step instanceof OrderByStep).count());
    result.close();
  }

  @Test
  public void testIndexPlusSort4() {
    var className = "testIndexPlusSort4";
    var clazz = db.getMetadata().getSchema().createClass(className);
    clazz.createProperty(db, "name", PropertyType.STRING);
    clazz.createProperty(db, "surname", PropertyType.STRING);
    db.command(
            "create index "
                + className
                + ".name_surname on "
                + className
                + " (name, surname) NOTUNIQUE")
        .close();

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i % 3);
      doc.setProperty("surname", "surname" + i);
      doc.save();
      db.commit();
    }

    var result =
        db.query(
            "select from " + className + " where name = 'name1' order by name ASC, surname ASC");
    printExecutionPlan(result);
    String lastSurname = null;
    for (var i = 0; i < 3; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertNotNull(item);
      Assert.assertNotNull(item.getProperty("surname"));

      String surname = item.getProperty("surname");
      if (i > 0) {
        Assert.assertTrue(surname.compareTo(lastSurname) > 0);
      }
      lastSurname = surname;
    }
    Assert.assertFalse(result.hasNext());
    var plan = result.getExecutionPlan().get();
    Assert.assertEquals(
        1, plan.getSteps().stream().filter(step -> step instanceof FetchFromIndexStep).count());
    Assert.assertEquals(
        0, plan.getSteps().stream().filter(step -> step instanceof OrderByStep).count());
    result.close();
  }

  @Test
  public void testIndexPlusSort5() {
    var className = "testIndexPlusSort5";
    var clazz = db.getMetadata().getSchema().createClass(className);
    clazz.createProperty(db, "name", PropertyType.STRING);
    clazz.createProperty(db, "surname", PropertyType.STRING);
    clazz.createProperty(db, "address", PropertyType.STRING);
    db.command(
            "create index "
                + className
                + ".name_surname on "
                + className
                + " (name, surname, address) NOTUNIQUE")
        .close();

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i % 3);
      doc.setProperty("surname", "surname" + i);
      doc.save();
      db.commit();
    }

    var result =
        db.query("select from " + className + " where name = 'name1' order by surname ASC");
    printExecutionPlan(result);
    String lastSurname = null;
    for (var i = 0; i < 3; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertNotNull(item);
      Assert.assertNotNull(item.getProperty("surname"));
      String surname = item.getProperty("surname");
      if (i > 0) {
        Assert.assertTrue(surname.compareTo(lastSurname) > 0);
      }
      lastSurname = surname;
    }
    Assert.assertFalse(result.hasNext());
    var plan = result.getExecutionPlan().get();
    Assert.assertEquals(
        1, plan.getSteps().stream().filter(step -> step instanceof FetchFromIndexStep).count());
    Assert.assertEquals(
        0, plan.getSteps().stream().filter(step -> step instanceof OrderByStep).count());
    result.close();
  }

  @Test
  public void testIndexPlusSort6() {
    var className = "testIndexPlusSort6";
    var clazz = db.getMetadata().getSchema().createClass(className);
    clazz.createProperty(db, "name", PropertyType.STRING);
    clazz.createProperty(db, "surname", PropertyType.STRING);
    clazz.createProperty(db, "address", PropertyType.STRING);
    db.command(
            "create index "
                + className
                + ".name_surname on "
                + className
                + " (name, surname, address) NOTUNIQUE")
        .close();

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i % 3);
      doc.setProperty("surname", "surname" + i);
      doc.save();
      db.commit();
    }

    var result =
        db.query("select from " + className + " where name = 'name1' order by surname DESC");
    printExecutionPlan(result);
    String lastSurname = null;
    for (var i = 0; i < 3; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertNotNull(item);
      Assert.assertNotNull(item.getProperty("surname"));
      String surname = item.getProperty("surname");
      if (i > 0) {
        Assert.assertTrue(surname.compareTo(lastSurname) < 0);
      }
      lastSurname = surname;
    }
    Assert.assertFalse(result.hasNext());
    var plan = result.getExecutionPlan().get();
    Assert.assertEquals(
        1, plan.getSteps().stream().filter(step -> step instanceof FetchFromIndexStep).count());
    Assert.assertEquals(
        0, plan.getSteps().stream().filter(step -> step instanceof OrderByStep).count());
    result.close();
  }

  @Test
  public void testIndexPlusSort7() {
    var className = "testIndexPlusSort7";
    var clazz = db.getMetadata().getSchema().createClass(className);
    clazz.createProperty(db, "name", PropertyType.STRING);
    clazz.createProperty(db, "surname", PropertyType.STRING);
    clazz.createProperty(db, "address", PropertyType.STRING);
    db.command(
            "create index "
                + className
                + ".name_surname on "
                + className
                + " (name, surname, address) NOTUNIQUE")
        .close();

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i % 3);
      doc.setProperty("surname", "surname" + i);
      doc.save();
      db.commit();
    }

    var result =
        db.query("select from " + className + " where name = 'name1' order by address DESC");
    printExecutionPlan(result);

    for (var i = 0; i < 3; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertNotNull(item);
      Assert.assertNotNull(item.getProperty("surname"));
    }
    Assert.assertFalse(result.hasNext());
    var orderStepFound = false;
    for (var step : result.getExecutionPlan().get().getSteps()) {
      if (step instanceof OrderByStep) {
        orderStepFound = true;
        break;
      }
    }
    Assert.assertTrue(orderStepFound);
    result.close();
  }

  @Test
  public void testIndexPlusSort8() {
    var className = "testIndexPlusSort8";
    var clazz = db.getMetadata().getSchema().createClass(className);
    clazz.createProperty(db, "name", PropertyType.STRING);
    clazz.createProperty(db, "surname", PropertyType.STRING);
    db.command(
            "create index "
                + className
                + ".name_surname on "
                + className
                + " (name, surname) NOTUNIQUE")
        .close();

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i % 3);
      doc.setProperty("surname", "surname" + i);
      doc.save();
      db.commit();
    }

    var result =
        db.query(
            "select from " + className + " where name = 'name1' order by name ASC, surname DESC");
    printExecutionPlan(result);
    for (var i = 0; i < 3; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertNotNull(item);
      Assert.assertNotNull(item.getProperty("surname"));
    }
    Assert.assertFalse(result.hasNext());
    Assert.assertFalse(result.hasNext());
    var orderStepFound = false;
    for (var step : result.getExecutionPlan().get().getSteps()) {
      if (step instanceof OrderByStep) {
        orderStepFound = true;
        break;
      }
    }
    Assert.assertTrue(orderStepFound);
    result.close();
  }

  @Test
  public void testIndexPlusSort9() {
    var className = "testIndexPlusSort9";
    var clazz = db.getMetadata().getSchema().createClass(className);
    clazz.createProperty(db, "name", PropertyType.STRING);
    clazz.createProperty(db, "surname", PropertyType.STRING);
    db.command(
            "create index "
                + className
                + ".name_surname on "
                + className
                + " (name, surname) NOTUNIQUE")
        .close();

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i % 3);
      doc.setProperty("surname", "surname" + i);
      doc.save();
      db.commit();
    }

    var result = db.query("select from " + className + " order by name , surname ASC");
    printExecutionPlan(result);
    for (var i = 0; i < 10; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertNotNull(item);
      Assert.assertNotNull(item.getProperty("surname"));
    }
    Assert.assertFalse(result.hasNext());
    Assert.assertFalse(result.hasNext());
    var orderStepFound = false;
    for (var step : result.getExecutionPlan().get().getSteps()) {
      if (step instanceof OrderByStep) {
        orderStepFound = true;
        break;
      }
    }
    Assert.assertFalse(orderStepFound);
    result.close();
  }

  @Test
  public void testIndexPlusSort10() {
    var className = "testIndexPlusSort10";
    var clazz = db.getMetadata().getSchema().createClass(className);
    clazz.createProperty(db, "name", PropertyType.STRING);
    clazz.createProperty(db, "surname", PropertyType.STRING);
    db.command(
            "create index "
                + className
                + ".name_surname on "
                + className
                + " (name, surname) NOTUNIQUE")
        .close();

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i % 3);
      doc.setProperty("surname", "surname" + i);
      doc.save();
      db.commit();
    }

    var result = db.query("select from " + className + " order by name desc, surname desc");
    printExecutionPlan(result);
    for (var i = 0; i < 10; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertNotNull(item);
      Assert.assertNotNull(item.getProperty("surname"));
    }
    Assert.assertFalse(result.hasNext());
    Assert.assertFalse(result.hasNext());
    var orderStepFound = false;
    for (var step : result.getExecutionPlan().get().getSteps()) {
      if (step instanceof OrderByStep) {
        orderStepFound = true;
        break;
      }
    }
    Assert.assertFalse(orderStepFound);
    result.close();
  }

  @Test
  public void testIndexPlusSort11() {
    var className = "testIndexPlusSort11";
    var clazz = db.getMetadata().getSchema().createClass(className);
    clazz.createProperty(db, "name", PropertyType.STRING);
    clazz.createProperty(db, "surname", PropertyType.STRING);
    db.command(
            "create index "
                + className
                + ".name_surname on "
                + className
                + " (name, surname) NOTUNIQUE")
        .close();

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i % 3);
      doc.setProperty("surname", "surname" + i);
      doc.save();
      db.commit();
    }

    var result = db.query("select from " + className + " order by name asc, surname desc");
    printExecutionPlan(result);
    for (var i = 0; i < 10; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertNotNull(item);
      Assert.assertNotNull(item.getProperty("surname"));
    }
    Assert.assertFalse(result.hasNext());
    Assert.assertFalse(result.hasNext());
    var orderStepFound = false;
    for (var step : result.getExecutionPlan().get().getSteps()) {
      if (step instanceof OrderByStep) {
        orderStepFound = true;
        break;
      }
    }
    Assert.assertTrue(orderStepFound);
    result.close();
  }

  @Test
  public void testIndexPlusSort12() {
    var className = "testIndexPlusSort12";
    var clazz = db.getMetadata().getSchema().createClass(className);
    clazz.createProperty(db, "name", PropertyType.STRING);
    clazz.createProperty(db, "surname", PropertyType.STRING);
    db.command(
            "create index "
                + className
                + ".name_surname on "
                + className
                + " (name, surname) NOTUNIQUE")
        .close();

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i % 3);
      doc.setProperty("surname", "surname" + i);
      doc.save();
      db.commit();
    }

    var result = db.query("select from " + className + " order by name");
    printExecutionPlan(result);
    String last = null;
    for (var i = 0; i < 10; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertNotNull(item);
      Assert.assertNotNull(item.getProperty("name"));
      String name = item.getProperty("name");
      if (i > 0) {
        Assert.assertTrue(name.compareTo(last) >= 0);
      }
      last = name;
    }
    Assert.assertFalse(result.hasNext());
    Assert.assertFalse(result.hasNext());
    var orderStepFound = false;
    for (var step : result.getExecutionPlan().get().getSteps()) {
      if (step instanceof OrderByStep) {
        orderStepFound = true;
        break;
      }
    }
    Assert.assertFalse(orderStepFound);
    result.close();
  }

  @Test
  public void testSelectFromStringParam() {
    var className = "testSelectFromStringParam";
    db.getMetadata().getSchema().createClass(className);
    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i);
      doc.save();
      db.commit();
    }
    var result = db.query("select from ?", className);
    printExecutionPlan(result);

    for (var i = 0; i < 10; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertNotNull(item);
      Assert.assertTrue(("" + item.getProperty("name")).startsWith("name"));
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testSelectFromStringNamedParam() {
    var className = "testSelectFromStringNamedParam";
    db.getMetadata().getSchema().createClass(className);
    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i);
      doc.save();
      db.commit();
    }
    Map<Object, Object> params = new HashMap<>();
    params.put("target", className);
    var result = db.query("select from :target", params);
    printExecutionPlan(result);

    for (var i = 0; i < 10; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertNotNull(item);
      Assert.assertTrue(("" + item.getProperty("name")).startsWith("name"));
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testMatches() {
    var className = "testMatches";
    db.getMetadata().getSchema().createClass(className);

    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i);
      doc.save();
      db.commit();
    }

    var result = db.query("select from " + className + " where name matches 'name1'");
    printExecutionPlan(result);

    for (var i = 0; i < 1; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertNotNull(item);
      Assert.assertEquals(item.getProperty("name"), "name1");
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testRange() {
    var className = "testRange";
    db.getMetadata().getSchema().createClass(className);

    db.begin();
    EntityImpl doc = db.newInstance(className);
    doc.setProperty("name", new String[]{"a", "b", "c", "d"});
    doc.save();
    db.commit();

    var result = db.query("select name[0..3] as names from " + className);
    printExecutionPlan(result);

    for (var i = 0; i < 1; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertNotNull(item);
      var names = item.getProperty("names");
      if (names == null) {
        Assert.fail();
      }
      if (names instanceof Collection) {
        Assert.assertEquals(3, ((Collection) names).size());
        var iter = ((Collection) names).iterator();
        Assert.assertEquals("a", iter.next());
        Assert.assertEquals("b", iter.next());
        Assert.assertEquals("c", iter.next());
      } else if (names.getClass().isArray()) {
        Assert.assertEquals(3, Array.getLength(names));
      } else {
        Assert.fail();
      }
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testRangeParams1() {
    var className = "testRangeParams1";
    db.getMetadata().getSchema().createClass(className);

    db.begin();
    EntityImpl doc = db.newInstance(className);
    doc.setProperty("name", new String[]{"a", "b", "c", "d"});
    doc.save();
    db.commit();

    var result = db.query("select name[?..?] as names from " + className, 0, 3);
    printExecutionPlan(result);

    for (var i = 0; i < 1; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertNotNull(item);
      var names = item.getProperty("names");
      if (names == null) {
        Assert.fail();
      }
      if (names instanceof Collection) {
        Assert.assertEquals(3, ((Collection) names).size());
        var iter = ((Collection) names).iterator();
        Assert.assertEquals("a", iter.next());
        Assert.assertEquals("b", iter.next());
        Assert.assertEquals("c", iter.next());
      } else if (names.getClass().isArray()) {
        Assert.assertEquals(3, Array.getLength(names));
      } else {
        Assert.fail();
      }
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testRangeParams2() {
    var className = "testRangeParams2";
    db.getMetadata().getSchema().createClass(className);

    db.begin();
    EntityImpl doc = db.newInstance(className);
    doc.setProperty("name", new String[]{"a", "b", "c", "d"});
    doc.save();
    db.commit();

    Map<String, Object> params = new HashMap<>();
    params.put("a", 0);
    params.put("b", 3);
    var result = db.query("select name[:a..:b] as names from " + className, params);
    printExecutionPlan(result);

    for (var i = 0; i < 1; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertNotNull(item);
      var names = item.getProperty("names");
      if (names == null) {
        Assert.fail();
      }
      if (names instanceof Collection) {
        Assert.assertEquals(3, ((Collection) names).size());
        var iter = ((Collection) names).iterator();
        Assert.assertEquals("a", iter.next());
        Assert.assertEquals("b", iter.next());
        Assert.assertEquals("c", iter.next());
      } else if (names.getClass().isArray()) {
        Assert.assertEquals(3, Array.getLength(names));
      } else {
        Assert.fail();
      }
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testEllipsis() {
    var className = "testEllipsis";
    db.getMetadata().getSchema().createClass(className);

    db.begin();
    EntityImpl doc = db.newInstance(className);
    doc.setProperty("name", new String[]{"a", "b", "c", "d"});
    doc.save();
    db.commit();

    var result = db.query("select name[0...2] as names from " + className);
    printExecutionPlan(result);

    for (var i = 0; i < 1; i++) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertNotNull(item);
      var names = item.getProperty("names");
      if (names == null) {
        Assert.fail();
      }
      if (names instanceof Collection) {
        Assert.assertEquals(3, ((Collection) names).size());
        var iter = ((Collection) names).iterator();
        Assert.assertEquals("a", iter.next());
        Assert.assertEquals("b", iter.next());
        Assert.assertEquals("c", iter.next());
      } else if (names.getClass().isArray()) {
        Assert.assertEquals(3, Array.getLength(names));
        Assert.assertEquals("a", Array.get(names, 0));
        Assert.assertEquals("b", Array.get(names, 1));
        Assert.assertEquals("c", Array.get(names, 2));
      } else {
        Assert.fail();
      }
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testNewRid() {
    var result = db.query("select {\"@rid\":\"#12:0\"} as theRid ");
    Assert.assertTrue(result.hasNext());
    var item = result.next();
    var rid = item.getProperty("theRid");
    Assert.assertTrue(rid instanceof Identifiable);
    var id = (Identifiable) rid;
    Assert.assertEquals(12, id.getIdentity().getClusterId());
    Assert.assertEquals(0L, id.getIdentity().getClusterPosition());
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testNestedProjections1() {
    var className = "testNestedProjections1";
    db.command("create class " + className).close();

    db.begin();
    var elem1 = db.newEntity(className);
    elem1.setProperty("name", "a");
    elem1.save();

    var elem2 = db.newEntity(className);
    elem2.setProperty("name", "b");
    elem2.setProperty("surname", "lkj");
    elem2.save();

    var elem3 = db.newEntity(className);
    elem3.setProperty("name", "c");
    elem3.save();

    var elem4 = db.newEntity(className);
    elem4.setProperty("name", "d");
    elem4.setProperty("elem1", elem1);
    elem4.setProperty("elem2", elem2);
    elem4.setProperty("elem3", elem3);
    elem4.save();

    db.commit();

    var result =
        db.query(
            "select name, elem1:{*}, elem2:{!surname} from " + className + " where name = 'd'");
    Assert.assertTrue(result.hasNext());
    var item = result.next();
    Assert.assertNotNull(item);
    // TODO refine this!
    Assert.assertTrue(item.getProperty("elem1") instanceof Result);
    Assert.assertEquals("a", ((Result) item.getProperty("elem1")).getProperty("name"));
    printExecutionPlan(result);

    result.close();
  }

  @Test
  public void testSimpleCollectionFiltering() {
    var className = "testSimpleCollectionFiltering";
    db.command("create class " + className).close();

    db.begin();
    var elem1 = db.newEntity(className);
    List<String> coll = new ArrayList<>();
    coll.add("foo");
    coll.add("bar");
    coll.add("baz");
    elem1.setProperty("coll", coll);
    elem1.save();
    db.commit();

    var result = db.query("select coll[='foo'] as filtered from " + className);
    Assert.assertTrue(result.hasNext());
    var item = result.next();
    List res = item.getProperty("filtered");
    Assert.assertEquals(1, res.size());
    Assert.assertEquals("foo", res.get(0));
    result.close();

    result = db.query("select coll[<'ccc'] as filtered from " + className);
    Assert.assertTrue(result.hasNext());
    item = result.next();
    res = item.getProperty("filtered");
    Assert.assertEquals(2, res.size());
    result.close();

    result = db.query("select coll[LIKE 'ba%'] as filtered from " + className);
    Assert.assertTrue(result.hasNext());
    item = result.next();
    res = item.getProperty("filtered");
    Assert.assertEquals(2, res.size());
    result.close();

    result = db.query("select coll[in ['bar']] as filtered from " + className);
    Assert.assertTrue(result.hasNext());
    item = result.next();
    res = item.getProperty("filtered");
    Assert.assertEquals(1, res.size());
    Assert.assertEquals("bar", res.get(0));
    result.close();
  }

  @Test
  public void testContaninsWithConversion() {
    var className = "testContaninsWithConversion";
    db.command("create class " + className).close();

    db.begin();
    var elem1 = db.newEntity(className);
    List<Long> coll = new ArrayList<>();
    coll.add(1L);
    coll.add(3L);
    coll.add(5L);
    elem1.setProperty("coll", coll);
    elem1.save();

    var elem2 = db.newEntity(className);
    coll = new ArrayList<>();
    coll.add(2L);
    coll.add(4L);
    coll.add(6L);
    elem2.setProperty("coll", coll);
    elem2.save();
    db.commit();

    var result = db.query("select from " + className + " where coll contains 1");
    Assert.assertTrue(result.hasNext());
    result.next();
    Assert.assertFalse(result.hasNext());
    result.close();

    result = db.query("select from " + className + " where coll contains 1L");
    Assert.assertTrue(result.hasNext());
    result.next();
    Assert.assertFalse(result.hasNext());
    result.close();

    result = db.query("select from " + className + " where coll contains 12L");
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testIndexPrefixUsage() {
    // issue #7636
    var className = "testIndexPrefixUsage";
    db.command("create class " + className).close();
    db.command("create property " + className + ".id LONG").close();
    db.command("create property " + className + ".name STRING").close();
    db.command("create index " + className + ".id_name on " + className + "(id, name) UNIQUE")
        .close();

    db.begin();
    db.command("insert into " + className + " set id = 1 , name = 'Bar'").close();
    db.commit();

    var result = db.query("select from " + className + " where name = 'Bar'");
    Assert.assertTrue(result.hasNext());
    result.next();
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testNamedParams() {
    var className = "testNamedParams";
    db.command("create class " + className).close();

    db.begin();
    db.command("insert into " + className + " set name = 'Foo', surname = 'Fox'").close();
    db.command("insert into " + className + " set name = 'Bar', surname = 'Bax'").close();
    db.commit();

    Map<String, Object> params = new HashMap<>();
    params.put("p1", "Foo");
    params.put("p2", "Fox");
    var result =
        db.query("select from " + className + " where name = :p1 and surname = :p2", params);
    Assert.assertTrue(result.hasNext());
    result.next();
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testNamedParamsWithIndex() {
    var className = "testNamedParamsWithIndex";
    db.command("create class " + className).close();
    db.command("create property " + className + ".name STRING").close();
    db.command("create index " + className + ".name ON " + className + " (name) NOTUNIQUE").close();

    db.begin();
    db.command("insert into " + className + " set name = 'Foo'").close();
    db.command("insert into " + className + " set name = 'Bar'").close();
    db.commit();

    Map<String, Object> params = new HashMap<>();
    params.put("p1", "Foo");
    var result = db.query("select from " + className + " where name = :p1", params);
    Assert.assertTrue(result.hasNext());
    result.next();
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testIsDefined() {
    var className = "testIsDefined";
    db.command("create class " + className).close();

    db.begin();
    db.command("insert into " + className + " set name = 'Foo'").close();
    db.command("insert into " + className + " set sur = 'Bar'").close();
    db.command("insert into " + className + " set sur = 'Barz'").close();
    db.commit();

    var result = db.query("select from " + className + " where name is defined");
    Assert.assertTrue(result.hasNext());
    result.next();
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testIsNotDefined() {
    var className = "testIsNotDefined";
    db.command("create class " + className).close();

    db.begin();
    db.command("insert into " + className + " set name = 'Foo'").close();
    db.command("insert into " + className + " set name = null, sur = 'Bar'").close();
    db.command("insert into " + className + " set sur = 'Barz'").close();
    db.commit();

    var result = db.query("select from " + className + " where name is not defined");
    Assert.assertTrue(result.hasNext());
    result.next();
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testContainsWithSubquery() {
    var className = "testContainsWithSubquery";
    db.createClassIfNotExist(className + 1);
    var clazz2 = db.createClassIfNotExist(className + 2);
    clazz2.createProperty(db, "tags", PropertyType.EMBEDDEDLIST);

    db.begin();
    db.command("insert into " + className + 1 + "  set name = 'foo'");

    db.command("insert into " + className + 2 + "  set tags = ['foo', 'bar']");
    db.command("insert into " + className + 2 + "  set tags = ['baz', 'bar']");
    db.command("insert into " + className + 2 + "  set tags = ['foo']");
    db.commit();

    try (var result =
        db.query(
            "select from "
                + className
                + 2
                + " where tags contains (select from "
                + className
                + 1
                + " where name = 'foo')")) {

      Assert.assertTrue(result.hasNext());
      result.next();
      Assert.assertTrue(result.hasNext());
      result.next();
      Assert.assertFalse(result.hasNext());
    }
  }

  @Test
  public void testInWithSubquery() {
    var className = "testInWithSubquery";
    db.createClassIfNotExist(className + 1);
    var clazz2 = db.createClassIfNotExist(className + 2);
    clazz2.createProperty(db, "tags", PropertyType.EMBEDDEDLIST);

    db.begin();
    db.command("insert into " + className + 1 + "  set name = 'foo'");

    db.command("insert into " + className + 2 + "  set tags = ['foo', 'bar']");
    db.command("insert into " + className + 2 + "  set tags = ['baz', 'bar']");
    db.command("insert into " + className + 2 + "  set tags = ['foo']");
    db.commit();

    try (var result =
        db.query(
            "select from "
                + className
                + 2
                + " where (select from "
                + className
                + 1
                + " where name = 'foo') in tags")) {

      Assert.assertTrue(result.hasNext());
      result.next();
      Assert.assertTrue(result.hasNext());
      result.next();
      Assert.assertFalse(result.hasNext());
    }
  }

  @Test
  public void testContainsAny() {
    var className = "testContainsAny";
    var clazz = db.createClassIfNotExist(className);
    clazz.createProperty(db, "tags", PropertyType.EMBEDDEDLIST, PropertyType.STRING);

    db.begin();
    db.command("insert into " + className + "  set tags = ['foo', 'bar']");
    db.command("insert into " + className + "  set tags = ['bbb', 'FFF']");
    db.commit();

    try (var result =
        db.query("select from " + className + " where tags containsany ['foo','baz']")) {
      Assert.assertTrue(result.hasNext());
      result.next();
      Assert.assertFalse(result.hasNext());
    }

    try (var result =
        db.query("select from " + className + " where tags containsany ['foo','bar']")) {
      Assert.assertTrue(result.hasNext());
      result.next();
      Assert.assertFalse(result.hasNext());
    }

    try (var result =
        db.query("select from " + className + " where tags containsany ['foo','bbb']")) {
      Assert.assertTrue(result.hasNext());
      result.next();
      Assert.assertTrue(result.hasNext());
      result.next();
      Assert.assertFalse(result.hasNext());
    }

    try (var result =
        db.query("select from " + className + " where tags containsany ['xx','baz']")) {
      Assert.assertFalse(result.hasNext());
    }

    try (var result = db.query("select from " + className + " where tags containsany []")) {
      Assert.assertFalse(result.hasNext());
    }
  }

  @Test
  public void testContainsAnyWithIndex() {
    var className = "testContainsAnyWithIndex";
    var clazz = db.createClassIfNotExist(className);
    var prop = clazz.createProperty(db, "tags", PropertyType.EMBEDDEDLIST,
        PropertyType.STRING);
    prop.createIndex(db, SchemaClass.INDEX_TYPE.NOTUNIQUE);

    db.begin();
    db.command("insert into " + className + "  set tags = ['foo', 'bar']");
    db.command("insert into " + className + "  set tags = ['bbb', 'FFF']");
    db.commit();

    try (var result =
        db.query("select from " + className + " where tags containsany ['foo','baz']")) {
      Assert.assertTrue(result.hasNext());
      result.next();
      Assert.assertFalse(result.hasNext());
      Assert.assertTrue(
          result.getExecutionPlan().get().getSteps().stream()
              .anyMatch(x -> x instanceof FetchFromIndexStep));
    }

    try (var result =
        db.query("select from " + className + " where tags containsany ['foo','bar']")) {
      Assert.assertTrue(result.hasNext());
      result.next();
      Assert.assertFalse(result.hasNext());
      Assert.assertTrue(
          result.getExecutionPlan().get().getSteps().stream()
              .anyMatch(x -> x instanceof FetchFromIndexStep));
    }

    try (var result =
        db.query("select from " + className + " where tags containsany ['foo','bbb']")) {
      Assert.assertTrue(result.hasNext());
      result.next();
      Assert.assertTrue(result.hasNext());
      result.next();
      Assert.assertFalse(result.hasNext());
      Assert.assertTrue(
          result.getExecutionPlan().get().getSteps().stream()
              .anyMatch(x -> x instanceof FetchFromIndexStep));
    }

    try (var result =
        db.query("select from " + className + " where tags containsany ['xx','baz']")) {
      Assert.assertFalse(result.hasNext());
      Assert.assertTrue(
          result.getExecutionPlan().get().getSteps().stream()
              .anyMatch(x -> x instanceof FetchFromIndexStep));
    }

    try (var result = db.query("select from " + className + " where tags containsany []")) {
      Assert.assertFalse(result.hasNext());
      Assert.assertTrue(
          result.getExecutionPlan().get().getSteps().stream()
              .anyMatch(x -> x instanceof FetchFromIndexStep));
    }
  }

  @Test
  public void testContainsAll() {
    var className = "testContainsAll";
    var clazz = db.createClassIfNotExist(className);
    clazz.createProperty(db, "tags", PropertyType.EMBEDDEDLIST, PropertyType.STRING);

    db.begin();
    db.command("insert into " + className + "  set tags = ['foo', 'bar']");
    db.command("insert into " + className + "  set tags = ['foo', 'FFF']");
    db.commit();

    try (var result =
        db.query("select from " + className + " where tags containsall ['foo','bar']")) {
      Assert.assertTrue(result.hasNext());
      result.next();
      Assert.assertFalse(result.hasNext());
    }

    try (var result =
        db.query("select from " + className + " where tags containsall ['foo']")) {
      Assert.assertTrue(result.hasNext());
      result.next();
      Assert.assertTrue(result.hasNext());
      result.next();
      Assert.assertFalse(result.hasNext());
    }
  }

  @Test
  public void testBetween() {
    var className = "testBetween";
    db.createClassIfNotExist(className);

    db.begin();
    db.command("insert into " + className + "  set name = 'foo1', val = 1");
    db.command("insert into " + className + "  set name = 'foo2', val = 2");
    db.command("insert into " + className + "  set name = 'foo3', val = 3");
    db.command("insert into " + className + "  set name = 'foo4', val = 4");
    db.commit();

    try (var result = db.query("select from " + className + " where val between 2 and 3")) {
      Assert.assertTrue(result.hasNext());
      result.next();
      Assert.assertTrue(result.hasNext());
      result.next();
      Assert.assertFalse(result.hasNext());
    }
  }

  @Test
  public void testInWithIndex() {
    var className = "testInWithIndex";
    var clazz = db.createClassIfNotExist(className);
    var prop = clazz.createProperty(db, "tag", PropertyType.STRING);
    prop.createIndex(db, SchemaClass.INDEX_TYPE.NOTUNIQUE);

    db.begin();
    db.command("insert into " + className + "  set tag = 'foo'");
    db.command("insert into " + className + "  set tag = 'bar'");
    db.commit();

    try (var result = db.query(
        "select from " + className + " where tag in ['foo','baz']")) {
      Assert.assertTrue(result.hasNext());
      result.next();
      Assert.assertFalse(result.hasNext());
      Assert.assertTrue(
          result.getExecutionPlan().get().getSteps().stream()
              .anyMatch(x -> x instanceof FetchFromIndexStep));
    }

    try (var result = db.query(
        "select from " + className + " where tag in ['foo','bar']")) {
      Assert.assertTrue(result.hasNext());
      result.next();
      Assert.assertTrue(result.hasNext());
      result.next();
      Assert.assertFalse(result.hasNext());
      Assert.assertTrue(
          result.getExecutionPlan().get().getSteps().stream()
              .anyMatch(x -> x instanceof FetchFromIndexStep));
    }

    try (var result = db.query("select from " + className + " where tag in []")) {
      Assert.assertFalse(result.hasNext());
      Assert.assertTrue(
          result.getExecutionPlan().get().getSteps().stream()
              .anyMatch(x -> x instanceof FetchFromIndexStep));
    }

    List<String> params = new ArrayList<>();
    params.add("foo");
    params.add("bar");
    try (var result = db.query("select from " + className + " where tag in (?)", params)) {
      Assert.assertTrue(result.hasNext());
      result.next();
      Assert.assertTrue(result.hasNext());
      result.next();
      Assert.assertFalse(result.hasNext());
      Assert.assertTrue(
          result.getExecutionPlan().get().getSteps().stream()
              .anyMatch(x -> x instanceof FetchFromIndexStep));
    }
  }

  @Test
  public void testIndexChain() {
    var className1 = "testIndexChain1";
    var className2 = "testIndexChain2";
    var className3 = "testIndexChain3";

    var clazz3 = db.createClassIfNotExist(className3);
    var prop = clazz3.createProperty(db, "name", PropertyType.STRING);
    prop.createIndex(db, SchemaClass.INDEX_TYPE.NOTUNIQUE);

    var clazz2 = db.createClassIfNotExist(className2);
    prop = clazz2.createProperty(db, "next", PropertyType.LINK, clazz3);
    prop.createIndex(db, SchemaClass.INDEX_TYPE.NOTUNIQUE);

    var clazz1 = db.createClassIfNotExist(className1);
    prop = clazz1.createProperty(db, "next", PropertyType.LINK, clazz2);
    prop.createIndex(db, SchemaClass.INDEX_TYPE.NOTUNIQUE);

    db.begin();
    var elem3 = db.newEntity(className3);
    elem3.setProperty("name", "John");
    elem3.save();

    var elem2 = db.newEntity(className2);
    elem2.setProperty("next", elem3);
    elem2.save();

    var elem1 = db.newEntity(className1);
    elem1.setProperty("next", elem2);
    elem1.setProperty("name", "right");
    elem1.save();

    elem1 = db.newEntity(className1);
    elem1.setProperty("name", "wrong");
    elem1.save();
    db.commit();

    try (var result =
        db.query("select from " + className1 + " where next.next.name = ?", "John")) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertEquals("right", item.getProperty("name"));
      Assert.assertFalse(result.hasNext());
      Assert.assertTrue(
          result.getExecutionPlan().get().getSteps().stream()
              .anyMatch(x -> x instanceof FetchFromIndexStep));
    }
  }

  @Test
  public void testIndexChainWithContainsAny() {
    var className1 = "testIndexChainWithContainsAny1";
    var className2 = "testIndexChainWithContainsAny2";
    var className3 = "testIndexChainWithContainsAny3";

    var clazz3 = db.createClassIfNotExist(className3);
    var prop = clazz3.createProperty(db, "name", PropertyType.STRING);
    prop.createIndex(db, SchemaClass.INDEX_TYPE.NOTUNIQUE);

    var clazz2 = db.createClassIfNotExist(className2);
    prop = clazz2.createProperty(db, "next", PropertyType.LINKSET, clazz3);
    prop.createIndex(db, SchemaClass.INDEX_TYPE.NOTUNIQUE);

    var clazz1 = db.createClassIfNotExist(className1);
    prop = clazz1.createProperty(db, "next", PropertyType.LINKSET, clazz2);
    prop.createIndex(db, SchemaClass.INDEX_TYPE.NOTUNIQUE);

    db.begin();
    var elem3 = db.newEntity(className3);
    elem3.setProperty("name", "John");
    elem3.save();

    var elemFoo = db.newEntity(className3);
    elemFoo.setProperty("foo", "bar");
    elemFoo.save();

    var elem2 = db.newEntity(className2);
    List<Entity> elems3 = new ArrayList<>();
    elems3.add(elem3);
    elems3.add(elemFoo);
    elem2.setProperty("next", elems3);
    elem2.save();

    var elem1 = db.newEntity(className1);
    List<Entity> elems2 = new ArrayList<>();
    elems2.add(elem2);
    elem1.setProperty("next", elems2);
    elem1.setProperty("name", "right");
    elem1.save();

    elem1 = db.newEntity(className1);
    elem1.setProperty("name", "wrong");
    elem1.save();
    db.commit();

    try (var result =
        db.query("select from " + className1 + " where next.next.name CONTAINSANY ['John']")) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertEquals("right", item.getProperty("name"));
      Assert.assertFalse(result.hasNext());
      Assert.assertTrue(
          result.getExecutionPlan().get().getSteps().stream()
              .anyMatch(x -> x instanceof FetchFromIndexStep));
    }
  }

  @Test
  public void testMapByKeyIndex() {
    var className = "testMapByKeyIndex";

    var clazz1 = db.createClassIfNotExist(className);
    var prop = clazz1.createProperty(db, "themap", PropertyType.EMBEDDEDMAP);

    db.command(
        "CREATE INDEX " + className + ".themap ON " + className + "(themap by key) NOTUNIQUE");

    for (var i = 0; i < 100; i++) {
      db.begin();
      Map<String, Object> theMap = new HashMap<>();
      theMap.put("key" + i, "val" + i);
      var elem1 = db.newEntity(className);
      elem1.setProperty("themap", theMap);
      elem1.save();
      db.commit();
    }

    try (var result =
        db.query("select from " + className + " where themap CONTAINSKEY ?", "key10")) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Map<String, Object> map = item.getProperty("themap");
      Assert.assertEquals("key10", map.keySet().iterator().next());
      Assert.assertFalse(result.hasNext());
      Assert.assertTrue(
          result.getExecutionPlan().get().getSteps().stream()
              .anyMatch(x -> x instanceof FetchFromIndexStep));
    }
  }

  @Test
  public void testMapByKeyIndexMultiple() {
    var className = "testMapByKeyIndexMultiple";

    var clazz1 = db.createClassIfNotExist(className);
    clazz1.createProperty(db, "themap", PropertyType.EMBEDDEDMAP);
    clazz1.createProperty(db, "thestring", PropertyType.STRING);

    db.command(
        "CREATE INDEX "
            + className
            + ".themap_thestring ON "
            + className
            + "(themap by key, thestring) NOTUNIQUE");

    for (var i = 0; i < 100; i++) {
      db.begin();
      Map<String, Object> theMap = new HashMap<>();
      theMap.put("key" + i, "val" + i);
      var elem1 = db.newEntity(className);
      elem1.setProperty("themap", theMap);
      elem1.setProperty("thestring", "thestring" + i);
      elem1.save();
      db.commit();
    }

    try (var result =
        db.query(
            "select from " + className + " where themap CONTAINSKEY ? AND thestring = ?",
            "key10",
            "thestring10")) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Map<String, Object> map = item.getProperty("themap");
      Assert.assertEquals("key10", map.keySet().iterator().next());
      Assert.assertFalse(result.hasNext());
      Assert.assertTrue(
          result.getExecutionPlan().get().getSteps().stream()
              .anyMatch(x -> x instanceof FetchFromIndexStep));
    }
  }

  @Test
  public void testMapByValueIndex() {
    var className = "testMapByValueIndex";

    var clazz1 = db.createClassIfNotExist(className);
    var prop = clazz1.createProperty(db, "themap", PropertyType.EMBEDDEDMAP,
        PropertyType.STRING);

    db.command(
        "CREATE INDEX " + className + ".themap ON " + className + "(themap by value) NOTUNIQUE");

    for (var i = 0; i < 100; i++) {
      db.begin();
      Map<String, Object> theMap = new HashMap<>();
      theMap.put("key" + i, "val" + i);
      var elem1 = db.newEntity(className);
      elem1.setProperty("themap", theMap);
      elem1.save();
      db.commit();
    }

    try (var result =
        db.query("select from " + className + " where themap CONTAINSVALUE ?", "val10")) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Map<String, Object> map = item.getProperty("themap");
      Assert.assertEquals("key10", map.keySet().iterator().next());
      Assert.assertFalse(result.hasNext());
      Assert.assertTrue(
          result.getExecutionPlan().get().getSteps().stream()
              .anyMatch(x -> x instanceof FetchFromIndexStep));
    }
  }

  @Test
  public void testListOfMapsContains() {
    var className = "testListOfMapsContains";

    var clazz1 = db.createClassIfNotExist(className);
    clazz1.createProperty(db, "thelist", PropertyType.EMBEDDEDLIST, PropertyType.EMBEDDEDMAP);

    db.begin();
    db.command("INSERT INTO " + className + " SET thelist = [{name:\"Jack\"}]").close();
    db.command("INSERT INTO " + className + " SET thelist = [{name:\"Joe\"}]").close();
    db.commit();

    try (var result =
        db.query("select from " + className + " where thelist CONTAINS ( name = ?)", "Jack")) {
      Assert.assertTrue(result.hasNext());
      result.next();
      Assert.assertFalse(result.hasNext());
    }
  }

  @Test
  public void testOrderByWithCollate() {
    var className = "testOrderByWithCollate";

    db.createClassIfNotExist(className);

    db.begin();
    db.command("INSERT INTO " + className + " SET name = 'A', idx = 0").close();
    db.command("INSERT INTO " + className + " SET name = 'C', idx = 2").close();
    db.command("INSERT INTO " + className + " SET name = 'E', idx = 4").close();
    db.command("INSERT INTO " + className + " SET name = 'b', idx = 1").close();
    db.command("INSERT INTO " + className + " SET name = 'd', idx = 3").close();
    db.commit();

    try (var result =
        db.query("select from " + className + " order by name asc collate ci")) {
      for (var i = 0; i < 5; i++) {
        Assert.assertTrue(result.hasNext());
        var item = result.next();
        int val = item.getProperty("idx");
        Assert.assertEquals(i, val);
      }
      Assert.assertFalse(result.hasNext());
    }
  }

  @Test
  public void testContainsEmptyCollection() {
    var className = "testContainsEmptyCollection";

    db.createClassIfNotExist(className);

    db.begin();
    db.command("INSERT INTO " + className + " content {\"name\": \"jack\", \"age\": 22}").close();
    db.command(
            "INSERT INTO "
                + className
                + " content {\"name\": \"rose\", \"age\": 22, \"test\": [[]]}")
        .close();
    db.command(
            "INSERT INTO "
                + className
                + " content {\"name\": \"rose\", \"age\": 22, \"test\": [[1]]}")
        .close();
    db.command(
            "INSERT INTO "
                + className
                + " content {\"name\": \"pete\", \"age\": 22, \"test\": [{}]}")
        .close();
    db.command(
            "INSERT INTO "
                + className
                + " content {\"name\": \"david\", \"age\": 22, \"test\": [\"hello\"]}")
        .close();
    db.commit();

    try (var result = db.query("select from " + className + " where test contains []")) {
      Assert.assertTrue(result.hasNext());
      result.next();
      Assert.assertFalse(result.hasNext());
    }
  }

  @Test
  public void testContainsCollection() {
    var className = "testContainsCollection";

    db.createClassIfNotExist(className);

    db.begin();
    db.command("INSERT INTO " + className + " content {\"name\": \"jack\", \"age\": 22}").close();
    db.command(
            "INSERT INTO "
                + className
                + " content {\"name\": \"rose\", \"age\": 22, \"test\": [[]]}")
        .close();
    db.command(
            "INSERT INTO "
                + className
                + " content {\"name\": \"rose\", \"age\": 22, \"test\": [[1]]}")
        .close();
    db.command(
            "INSERT INTO "
                + className
                + " content {\"name\": \"pete\", \"age\": 22, \"test\": [{}]}")
        .close();
    db.command(
            "INSERT INTO "
                + className
                + " content {\"name\": \"david\", \"age\": 22, \"test\": [\"hello\"]}")
        .close();
    db.commit();

    try (var result = db.query("select from " + className + " where test contains [1]")) {
      Assert.assertTrue(result.hasNext());
      result.next();
      Assert.assertFalse(result.hasNext());
    }
  }

  @Test
  public void testHeapLimitForOrderBy() {
    Long oldValue = GlobalConfiguration.QUERY_MAX_HEAP_ELEMENTS_ALLOWED_PER_OP.getValueAsLong();
    try {
      GlobalConfiguration.QUERY_MAX_HEAP_ELEMENTS_ALLOWED_PER_OP.setValue(3);

      var className = "testHeapLimitForOrderBy";

      db.createClassIfNotExist(className);

      db.begin();
      db.command("INSERT INTO " + className + " set name = 'a'").close();
      db.command("INSERT INTO " + className + " set name = 'b'").close();
      db.command("INSERT INTO " + className + " set name = 'c'").close();
      db.command("INSERT INTO " + className + " set name = 'd'").close();
      db.commit();

      try {
        try (var result = db.query("select from " + className + " ORDER BY name")) {
          result.forEachRemaining(x -> x.getProperty("name"));
        }
        Assert.fail();
      } catch (CommandExecutionException ex) {
      }
    } finally {
      GlobalConfiguration.QUERY_MAX_HEAP_ELEMENTS_ALLOWED_PER_OP.setValue(oldValue);
    }
  }

  @Test
  public void testXor() {
    try (var result = db.query("select 15 ^ 4 as foo")) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertEquals(11, (int) item.getProperty("foo"));
      Assert.assertFalse(result.hasNext());
    }
  }

  @Test
  public void testLike() {
    var className = "testLike";

    db.createClassIfNotExist(className);

    db.begin();
    db.command("INSERT INTO " + className + " content {\"name\": \"foobarbaz\"}").close();
    db.command("INSERT INTO " + className + " content {\"name\": \"test[]{}()|*^.test\"}").close();
    db.commit();

    try (var result = db.query("select from " + className + " where name LIKE 'foo%'")) {
      Assert.assertTrue(result.hasNext());
      result.next();
      Assert.assertFalse(result.hasNext());
    }
    try (var result =
        db.query("select from " + className + " where name LIKE '%foo%baz%'")) {
      Assert.assertTrue(result.hasNext());
      result.next();
      Assert.assertFalse(result.hasNext());
    }
    try (var result = db.query("select from " + className + " where name LIKE '%bar%'")) {
      Assert.assertTrue(result.hasNext());
      result.next();
      Assert.assertFalse(result.hasNext());
    }

    try (var result = db.query("select from " + className + " where name LIKE 'bar%'")) {
      Assert.assertFalse(result.hasNext());
    }

    try (var result = db.query("select from " + className + " where name LIKE '%bar'")) {
      Assert.assertFalse(result.hasNext());
    }

    var specialChars = "[]{}()|*^.";
    for (var c : specialChars.toCharArray()) {
      try (var result =
          db.query("select from " + className + " where name LIKE '%" + c + "%'")) {
        Assert.assertTrue(result.hasNext());
        result.next();
        Assert.assertFalse(result.hasNext());
      }
    }
  }

  @Test
  public void testCountGroupBy() {
    // issue #9288
    var className = "testCountGroupBy";
    db.getMetadata().getSchema().createClass(className);
    for (var i = 0; i < 10; i++) {
      db.begin();
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("type", i % 2 == 0 ? "even" : "odd");
      doc.setProperty("val", i);
      doc.save();
      db.commit();
    }
    var result = db.query("select count(val) as count from " + className + " limit 3");
    printExecutionPlan(result);

    Assert.assertTrue(result.hasNext());
    var item = result.next();
    Assert.assertEquals(10L, (long) item.getProperty("count"));
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  @Ignore
  public void testTimeout() {
    var className = "testTimeout";
    final var funcitonName = getClass().getSimpleName() + "_sleep";
    db.getMetadata().getSchema().createClass(className);

    SQLEngine.getInstance()
        .registerFunction(
            funcitonName,
            new SQLFunction() {

              @Override
              public Object execute(
                  Object iThis,
                  Identifiable iCurrentRecord,
                  Object iCurrentResult,
                  Object[] iParams,
                  CommandContext iContext) {
                try {
                  Thread.sleep(5);
                } catch (InterruptedException e) {
                }
                return null;
              }

              @Override
              public void config(Object[] configuredParameters) {
              }

              @Override
              public boolean aggregateResults() {
                return false;
              }

              @Override
              public boolean filterResult() {
                return false;
              }

              @Override
              public String getName(DatabaseSession session) {
                return funcitonName;
              }

              @Override
              public int getMinParams() {
                return 0;
              }

              @Override
              public int getMaxParams(DatabaseSession session) {
                return 0;
              }

              @Override
              public String getSyntax(DatabaseSession session) {
                return "";
              }

              @Override
              public Object getResult() {
                return null;
              }

              @Override
              public void setResult(Object iResult) {
              }
            });
    for (var i = 0; i < 3; i++) {
      EntityImpl doc = db.newInstance(className);
      doc.setProperty("type", i % 2 == 0 ? "even" : "odd");
      doc.setProperty("val", i);
      doc.save();
    }
    try (var result =
        db.query("select " + funcitonName + "(), * from " + className + " timeout 1")) {
      while (result.hasNext()) {
        result.next();
      }
      Assert.fail();
    } catch (TimeoutException ex) {

    }

    try (var result =
        db.query("select " + funcitonName + "(), * from " + className + " timeout 1000")) {
      while (result.hasNext()) {
        result.next();
      }
    } catch (TimeoutException ex) {
      Assert.fail();
    }
  }

  @Test
  public void testSimpleRangeQueryWithIndexGTE() {
    final var className = "testSimpleRangeQueryWithIndexGTE";
    final var clazz = db.getMetadata().getSchema().getOrCreateClass(className);
    final var prop = clazz.createProperty(db, "name", PropertyType.STRING);
    prop.createIndex(db, SchemaClass.INDEX_TYPE.NOTUNIQUE);

    for (var i = 0; i < 10; i++) {
      db.begin();
      final EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i);
      doc.save();
      db.commit();
    }
    final var result = db.query("select from " + className + " WHERE name >= 'name5'");
    printExecutionPlan(result);

    for (var i = 0; i < 5; i++) {
      Assert.assertTrue(result.hasNext());
      result.next();
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testSimpleRangeQueryWithIndexLTE() {
    final var className = "testSimpleRangeQueryWithIndexLTE";
    final var clazz = db.getMetadata().getSchema().getOrCreateClass(className);
    final var prop = clazz.createProperty(db, "name", PropertyType.STRING);
    prop.createIndex(db, SchemaClass.INDEX_TYPE.NOTUNIQUE);

    for (var i = 0; i < 10; i++) {
      db.begin();
      final EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i);
      doc.save();
      db.commit();
    }
    final var result = db.query("select from " + className + " WHERE name <= 'name5'");
    printExecutionPlan(result);

    for (var i = 0; i < 6; i++) {
      Assert.assertTrue(result.hasNext());
      result.next();
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testSimpleRangeQueryWithOutIndex() {
    final var className = "testSimpleRangeQueryWithOutIndex";
    final var clazz = db.getMetadata().getSchema().getOrCreateClass(className);
    final var prop = clazz.createProperty(db, "name", PropertyType.STRING);
    // Hash Index skipped for range query
    prop.createIndex(db, SchemaClass.INDEX_TYPE.NOTUNIQUE);

    for (var i = 0; i < 10; i++) {
      db.begin();
      final EntityImpl doc = db.newInstance(className);
      doc.setProperty("name", "name" + i);
      doc.save();
      db.commit();
    }

    final var result = db.query("select from " + className + " WHERE name >= 'name5'");
    printExecutionPlan(result);

    for (var i = 0; i < 5; i++) {
      Assert.assertTrue(result.hasNext());
      result.next();
    }
    Assert.assertFalse(result.hasNext());
    result.close();
  }

  @Test
  public void testComplexIndexChain() {

    // A -b-> B -c-> C -d-> D.name
    //               C.name

    var classNamePrefix = "testComplexIndexChain_";
    var a = db.getMetadata().getSchema().createClass(classNamePrefix + "A");
    var b = db.getMetadata().getSchema().createClass(classNamePrefix + "C");
    var c = db.getMetadata().getSchema().createClass(classNamePrefix + "B");
    var d = db.getMetadata().getSchema().createClass(classNamePrefix + "D");

    a.createProperty(db, "b", PropertyType.LINK, b)
        .createIndex(db, SchemaClass.INDEX_TYPE.NOTUNIQUE);
    b.createProperty(db, "c", PropertyType.LINK, c)
        .createIndex(db, SchemaClass.INDEX_TYPE.NOTUNIQUE);
    c.createProperty(db, "d", PropertyType.LINK, d)
        .createIndex(db, SchemaClass.INDEX_TYPE.NOTUNIQUE);
    c.createProperty(db, "name", PropertyType.STRING)
        .createIndex(db, SchemaClass.INDEX_TYPE.NOTUNIQUE);
    d.createProperty(db, "name", PropertyType.STRING)
        .createIndex(db, SchemaClass.INDEX_TYPE.NOTUNIQUE);

    db.begin();
    var dDoc = db.newEntity(d.getName());
    dDoc.setProperty("name", "foo");
    dDoc.save();

    var cDoc = db.newEntity(c.getName());
    cDoc.setProperty("name", "foo");
    cDoc.setProperty("d", dDoc);
    cDoc.save();

    var bDoc = db.newEntity(b.getName());
    bDoc.setProperty("c", cDoc);
    bDoc.save();

    var aDoc = db.newEntity(a.getName());
    aDoc.setProperty("b", bDoc);
    aDoc.save();

    db.commit();

    try (var rs =
        db.query(
            "SELECT FROM "
                + classNamePrefix
                + "A WHERE b.c.name IN ['foo'] AND b.c.d.name IN ['foo']")) {
      Assert.assertTrue(rs.hasNext());
    }

    try (var rs =
        db.query(
            "SELECT FROM " + classNamePrefix + "A WHERE b.c.name = 'foo' AND b.c.d.name = 'foo'")) {
      Assert.assertTrue(rs.hasNext());
      Assert.assertTrue(
          rs.getExecutionPlan().get().getSteps().stream()
              .anyMatch(x -> x instanceof FetchFromIndexStep));
    }
  }

  @Test
  public void testIndexWithSubquery() {
    var classNamePrefix = "testIndexWithSubquery_";
    db.command("create class " + classNamePrefix + "Ownership extends V abstract;").close();
    db.command("create class " + classNamePrefix + "User extends V;").close();
    db.command("create property " + classNamePrefix + "User.id String;").close();
    db.command(
            "create index "
                + classNamePrefix
                + "User.id ON "
                + classNamePrefix
                + "User(id) unique;")
        .close();
    db.command(
            "create class " + classNamePrefix + "Report extends " + classNamePrefix + "Ownership;")
        .close();
    db.command("create property " + classNamePrefix + "Report.id String;").close();
    db.command("create property " + classNamePrefix + "Report.label String;").close();
    db.command("create property " + classNamePrefix + "Report.format String;").close();
    db.command("create property " + classNamePrefix + "Report.source String;").close();
    db.command("create class " + classNamePrefix + "hasOwnership extends E;").close();

    db.begin();
    db.command("insert into " + classNamePrefix + "User content {id:\"admin\"};");
    db.command(
            "insert into "
                + classNamePrefix
                + "Report content {format:\"PDF\", id:\"rep1\", label:\"Report 1\","
                + " source:\"Report1.src\"};")
        .close();
    db.command(
            "insert into "
                + classNamePrefix
                + "Report content {format:\"CSV\", id:\"rep2\", label:\"Report 2\","
                + " source:\"Report2.src\"};")
        .close();
    db.command(
            "create edge "
                + classNamePrefix
                + "hasOwnership from (select from "
                + classNamePrefix
                + "User) to (select from "
                + classNamePrefix
                + "Report);")
        .close();
    db.commit();

    try (var rs =
        db.query(
            "select from "
                + classNamePrefix
                + "Report where id in (select out('"
                + classNamePrefix
                + "hasOwnership').id from "
                + classNamePrefix
                + "User where id = 'admin');")) {
      Assert.assertTrue(rs.hasNext());
      rs.next();
      Assert.assertTrue(rs.hasNext());
      rs.next();
      Assert.assertFalse(rs.hasNext());
    }

    db.command(
            "create index "
                + classNamePrefix
                + "Report.id ON "
                + classNamePrefix
                + "Report(id) unique;")
        .close();

    try (var rs =
        db.query(
            "select from "
                + classNamePrefix
                + "Report where id in (select out('"
                + classNamePrefix
                + "hasOwnership').id from "
                + classNamePrefix
                + "User where id = 'admin');")) {
      Assert.assertTrue(rs.hasNext());
      rs.next();
      Assert.assertTrue(rs.hasNext());
      rs.next();
      Assert.assertFalse(rs.hasNext());
    }
  }

  @Test
  public void testExclude() {
    var className = "TestExclude";
    db.getMetadata().getSchema().createClass(className);
    db.begin();
    EntityImpl doc = db.newInstance(className);
    doc.setProperty("name", "foo");
    doc.setProperty("surname", "bar");
    doc.save();
    db.commit();

    var result = db.query("select *, !surname from " + className);
    Assert.assertTrue(result.hasNext());
    var item = result.next();
    Assert.assertNotNull(item);
    Assert.assertEquals("foo", item.getProperty("name"));
    Assert.assertNull(item.getProperty("surname"));

    printExecutionPlan(result);
    result.close();
  }

  @Test
  public void testOrderByLet() {
    var className = "testOrderByLet";
    db.getMetadata().getSchema().createClass(className);

    db.begin();
    EntityImpl doc = db.newInstance(className);
    doc.setProperty("name", "abbb");
    doc.save();

    doc = db.newInstance(className);
    doc.setProperty("name", "baaa");
    doc.save();
    db.commit();

    try (var result =
        db.query(
            "select from "
                + className
                + " LET $order = name.substring(1) ORDER BY $order ASC LIMIT 1")) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertNotNull(item);
      Assert.assertEquals("baaa", item.getProperty("name"));
    }
    try (var result =
        db.query(
            "select from "
                + className
                + " LET $order = name.substring(1) ORDER BY $order DESC LIMIT 1")) {
      Assert.assertTrue(result.hasNext());
      var item = result.next();
      Assert.assertNotNull(item);
      Assert.assertEquals("abbb", item.getProperty("name"));
    }
  }

  @Test
  public void testMapToJson() {
    var className = "testMapToJson";
    db.command("create class " + className).close();
    db.command("create property " + className + ".themap embeddedmap").close();

    db.begin();
    db.command(
            "insert into "
                + className
                + " set name = 'foo', themap = {\"foo bar\":\"baz\", \"riz\":\"faz\"}")
        .close();
    db.commit();

    try (var rs = db.query("select themap.tojson() as x from " + className)) {
      Assert.assertTrue(rs.hasNext());
      var item = rs.next();
      Assert.assertTrue(((String) item.getProperty("x")).contains("foo bar"));
    }
  }

  @Test
  public void testOptimizedCountQuery() {
    var className = "testOptimizedCountQuery";
    db.command("create class " + className).close();
    db.command("create property " + className + ".field boolean").close();
    db.command("create index " + className + ".field on " + className + "(field) NOTUNIQUE")
        .close();

    db.begin();
    db.command("insert into " + className + " set field=true").close();
    db.commit();

    try (var rs =
        db.query("select count(*) as count from " + className + " where field=true")) {
      Assert.assertTrue(rs.hasNext());
      var item = rs.next();
      Assert.assertEquals((long) item.getProperty("count"), 1L);
      Assert.assertFalse(rs.hasNext());
    }
  }

  @Test
  public void testAsSetKeepsOrderWithExpand() {
    // init classes
    db.activateOnCurrentThread();

    var car = db.createVertexClass("Car");
    var engine = db.createVertexClass("Engine");
    var body = db.createVertexClass("BodyType");
    var eng = db.createEdgeClass("eng");
    var bt = db.createEdgeClass("bt");
    db.begin();

    var diesel = db.newVertex(engine);
    diesel.setProperty("name", "diesel");
    var gasoline = db.newVertex(engine);
    gasoline.setProperty("name", "gasoline");
    var microwave = db.newVertex(engine);
    microwave.setProperty("name", "EV");

    var coupe = db.newVertex(body);
    coupe.setProperty("name", "coupe");
    var suv = db.newVertex(body);
    suv.setProperty("name", "suv");
    db.commit();
    db.begin();
    // fill data
    var coupe1 = db.newVertex(car);
    coupe1.setProperty("name", "car1");
    coupe1.addEdge(gasoline, eng);
    coupe1.addEdge(coupe, bt);
    coupe1.save();

    var coupe2 = db.newVertex(car);
    coupe2.setProperty("name", "car2");
    coupe2.addEdge(diesel, eng);
    coupe2.addEdge(coupe, bt);
    coupe2.save();

    var mw1 = db.newVertex(car);
    mw1.setProperty("name", "microwave1");
    mw1.addEdge(microwave, eng);
    mw1.addEdge(suv, bt);
    mw1.save();

    var mw2 = db.newVertex(car);
    mw2.setProperty("name", "microwave2");
    mw2.addEdge(microwave, eng);
    mw2.addEdge(suv, bt);
    mw2.save();

    var hatch1 = db.newVertex(car);
    hatch1.setProperty("name", "hatch1");
    hatch1.addEdge(diesel, eng);
    hatch1.addEdge(suv, bt);
    hatch1.save();
    db.commit();

    gasoline = db.bindToSession(gasoline);
    diesel = db.bindToSession(diesel);
    microwave = db.bindToSession(microwave);

    var identities =
        String.join(
            ",",
            Stream.of(coupe1, coupe2, mw1, mw2, hatch1)
                .map(DBRecord::getIdentity)
                .map(Object::toString)
                .toList());
    var unionAllEnginesQuery =
        "SELECT expand(unionAll($a, $a).asSet()) LET $a=(SELECT expand(out('eng')) FROM ["
            + identities
            + "])";

    var engineNames =
        db.query(unionAllEnginesQuery)
            .vertexStream()
            .map(oVertex -> oVertex.getProperty("name"))
            .toArray();
    Assert.assertArrayEquals(
        Arrays.asList(
                gasoline.getProperty("name"),
                diesel.getProperty("name"),
                microwave.getProperty("name"))
            .toArray(),
        engineNames);
  }
}
