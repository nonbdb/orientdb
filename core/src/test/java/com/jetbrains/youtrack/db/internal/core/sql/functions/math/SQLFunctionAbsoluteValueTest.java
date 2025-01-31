package com.jetbrains.youtrack.db.internal.core.sql.functions.math;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.jetbrains.youtrack.db.api.YouTrackDB;
import com.jetbrains.youtrack.db.api.config.YouTrackDBConfig;
import com.jetbrains.youtrack.db.api.query.ResultSet;
import com.jetbrains.youtrack.db.internal.DbTestBase;
import com.jetbrains.youtrack.db.internal.core.db.YouTrackDBImpl;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the absolute value function. The key is that the mathematical abs function is correctly
 * applied and that values retain their types.
 */
public class SQLFunctionAbsoluteValueTest {

  private SQLFunctionAbsoluteValue function;

  @Before
  public void setup() {
    function = new SQLFunctionAbsoluteValue();
  }

  @Test
  public void testEmpty() {
    var result = function.getResult();
    assertNull(result);
  }

  @Test
  public void testNull() {
    function.execute(null, null, null, new Object[]{null}, null);
    var result = function.getResult();
    assertNull(result);
  }

  @Test
  public void testPositiveInteger() {
    function.execute(null, null, null, new Object[]{10}, null);
    var result = function.getResult();
    assertTrue(result instanceof Integer);
    assertEquals(10, result);
  }

  @Test
  public void testNegativeInteger() {
    function.execute(null, null, null, new Object[]{-10}, null);
    var result = function.getResult();
    assertTrue(result instanceof Integer);
    assertEquals(10, result);
  }

  @Test
  public void testPositiveLong() {
    function.execute(null, null, null, new Object[]{10L}, null);
    var result = function.getResult();
    assertTrue(result instanceof Long);
    assertEquals(10L, result);
  }

  @Test
  public void testNegativeLong() {
    function.execute(null, null, null, new Object[]{-10L}, null);
    var result = function.getResult();
    assertTrue(result instanceof Long);
    assertEquals(10L, result);
  }

  @Test
  public void testPositiveShort() {
    function.execute(null, null, null, new Object[]{(short) 10}, null);
    var result = function.getResult();
    assertTrue(result instanceof Short);
    assertEquals((short) 10, result);
  }

  @Test
  public void testNegativeShort() {
    function.execute(null, null, null, new Object[]{(short) -10}, null);
    var result = function.getResult();
    assertTrue(result instanceof Short);
    assertEquals((short) 10, result);
  }

  @Test
  public void testPositiveDouble() {
    function.execute(null, null, null, new Object[]{10.5D}, null);
    var result = function.getResult();
    assertTrue(result instanceof Double);
    assertEquals(10.5D, result);
  }

  @Test
  public void testNegativeDouble() {
    function.execute(null, null, null, new Object[]{-10.5D}, null);
    var result = function.getResult();
    assertTrue(result instanceof Double);
    assertEquals(10.5D, result);
  }

  @Test
  public void testPositiveFloat() {
    function.execute(null, null, null, new Object[]{10.5F}, null);
    var result = function.getResult();
    assertTrue(result instanceof Float);
    assertEquals(10.5F, result);
  }

  @Test
  public void testNegativeFloat() {
    function.execute(null, null, null, new Object[]{-10.5F}, null);
    var result = function.getResult();
    assertTrue(result instanceof Float);
    assertEquals(10.5F, result);
  }

  @Test
  public void testPositiveBigDecimal() {
    function.execute(null, null, null, new Object[]{new BigDecimal("10.5")}, null);
    var result = function.getResult();
    assertTrue(result instanceof BigDecimal);
    assertEquals(new BigDecimal("10.5"), result);
  }

  @Test
  public void testNegativeBigDecimal() {
    function.execute(null, null, null, new Object[]{BigDecimal.valueOf(-10.5D)}, null);
    var result = function.getResult();
    assertTrue(result instanceof BigDecimal);
    assertEquals(new BigDecimal("10.5"), result);
  }

  @Test
  public void testPositiveBigInteger() {
    function.execute(null, null, null, new Object[]{new BigInteger("10")}, null);
    var result = function.getResult();
    assertTrue(result instanceof BigInteger);
    assertEquals(new BigInteger("10"), result);
  }

  @Test
  public void testNegativeBigInteger() {
    function.execute(null, null, null, new Object[]{new BigInteger("-10")}, null);
    var result = function.getResult();
    assertTrue(result instanceof BigInteger);
    assertEquals(new BigInteger("10"), result);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNonNumber() {
    function.execute(null, null, null, new Object[]{"abc"}, null);
  }

  @Test
  public void testFromQuery() {
    try (YouTrackDB ctx = new YouTrackDBImpl(DbTestBase.embeddedDBUrl(getClass()),
        YouTrackDBConfig.defaultConfig())) {
      ctx.execute("create database test memory users(admin identified by 'adminpwd' role admin)");
      try (var db = ctx.open("test", "admin", "adminpwd")) {
        try (var result = db.query("select abs(-45.4) as abs")) {
          assertThat(result.next().<Float>getProperty("abs")).isEqualTo(45.4f);
        }
      }
      ctx.drop("test");
    }
  }
}
