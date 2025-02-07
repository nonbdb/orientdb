package com.jetbrains.youtrack.db.internal.core.sql.method.misc;

import static org.junit.Assert.assertEquals;

import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the "asList()" method implemented by the SQLMethodAsList class. Note that the only input to
 * the execute() method from the SQLMethod interface that is used is the ioResult argument (the 4th
 * argument).
 */
public class SQLMethodAsListTest {

  private SQLMethodAsList function;

  @Before
  public void setup() {
    function = new SQLMethodAsList();
  }

  @Test
  public void testList() {
    // The expected behavior is to return the list itself.
    ArrayList<Object> aList = new ArrayList<Object>();
    aList.add(1);
    aList.add("2");
    Object result = function.execute(null, null, null, aList, null);
    assertEquals(result, aList);
  }

  @Test
  public void testNull() {
    // The expected behavior is to return an empty list.
    Object result = function.execute(null, null, null, null, null);
    assertEquals(result, new ArrayList<Object>());
  }

  @Test
  public void testCollection() {
    // The expected behavior is to return a list with all of the elements
    // of the collection in it.
    Set<Object> aCollection = new LinkedHashSet<Object>();
    aCollection.add(1);
    aCollection.add("2");
    Object result = function.execute(null, null, null, aCollection, null);

    ArrayList<Object> expected = new ArrayList<Object>();
    expected.add(1);
    expected.add("2");
    assertEquals(result, expected);
  }

  public void testIterable() {
    // The expected behavior is to return a list with all of the elements
    // of the iterable in it, in order of the collecitons iterator.
    ArrayList<Object> expected = new ArrayList<Object>();
    expected.add(1);
    expected.add("2");

    TestIterable<Object> anIterable = new TestIterable<Object>(expected);
    Object result = function.execute(null, null, null, anIterable, null);

    assertEquals(result, expected);
  }

  public void testIterator() {
    // The expected behavior is to return a list with all of the elements
    // of the iterator in it, in order of the iterator.
    ArrayList<Object> expected = new ArrayList<Object>();
    expected.add(1);
    expected.add("2");

    TestIterable<Object> anIterable = new TestIterable<Object>(expected);
    Object result = function.execute(null, null, null, anIterable.iterator(), null);

    assertEquals(result, expected);
  }

  public void testODocument() {
    // The expected behavior is to return a list with only the single
    // EntityImpl in it.
    EntityImpl doc = new EntityImpl();
    doc.field("f1", 1);
    doc.field("f2", 2);

    Object result = function.execute(null, null, null, doc, null);

    ArrayList<Object> expected = new ArrayList<Object>();
    expected.add(doc);

    assertEquals(result, expected);
  }

  public void testOtherSingleValue() {
    // The expected behavior is to return a list with only the single
    // element in it.

    Object result = function.execute(null, null, null, Integer.valueOf(4), null);
    ArrayList<Object> expected = new ArrayList<Object>();
    expected.add(Integer.valueOf(4));
    assertEquals(result, expected);
  }
}
