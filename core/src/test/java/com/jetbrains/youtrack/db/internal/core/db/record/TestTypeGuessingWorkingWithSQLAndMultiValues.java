package com.jetbrains.youtrack.db.internal.core.db.record;

import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.api.query.ResultSet;
import com.jetbrains.youtrack.db.internal.DbTestBase;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import java.util.Collection;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class TestTypeGuessingWorkingWithSQLAndMultiValues extends DbTestBase {

  @Before
  public void beforeTest() throws Exception {
    super.beforeTest();

    db.execute(
            "sql",
            """
                create class Address;
                create property Address.street String;
                create property Address.city String;
                create class Client;
                create property Client.name String;
                create property Client.phones embeddedSet String;
                create property Client.addresses embeddedList Address;""")
        .close();
  }

  @Test
  public void testLinkedValue() {

    try (var result =
        db.execute(
            "sql",
            "begin; let res = insert into client set name = 'James Bond', phones = ['1234',"
                + " '34567'], addresses = [{'@class':'Address','city':'Shanghai', 'zip':'3999'},"
                + " {'@class':'Address','city':'New York', 'street':'57th Ave'}]\n"
                + ";update client set addresses = addresses ||"
                + " [{'@type':'d','@class':'Address','city':'London', 'zip':'67373'}]; commit;"
                + " return $res")) {
      Assert.assertTrue(result.hasNext());
      var doc = result.next();

      Collection<EntityImpl> addresses = doc.getProperty("addresses");
      Assert.assertEquals(3, addresses.size());
      for (var a : addresses) {
        Assert.assertEquals("Address", a.getProperty("@class"));
      }
    }

    db.begin();
    try (var resultSet =
        db.command(
            "update client set addresses = addresses || [{'city':'London', 'zip':'67373'}] return"
                + " after")) {
      Assert.assertTrue(resultSet.hasNext());

      var result = resultSet.next();

      Collection<Result> addresses = result.getProperty("addresses");
      Assert.assertEquals(4, addresses.size());

      for (var a : addresses) {
        Assert.assertEquals("Address", a.getProperty("@class"));
      }
    }
    db.commit();
  }
}
