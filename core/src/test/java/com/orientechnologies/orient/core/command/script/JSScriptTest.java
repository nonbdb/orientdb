package com.orientechnologies.orient.core.command.script;

import com.orientechnologies.DBTestBase;
import com.orientechnologies.common.io.OIOUtils;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.db.YouTrackDBInternal;
import com.orientechnologies.orient.core.sql.executor.OResult;
import com.orientechnologies.orient.core.sql.executor.OResultSet;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import javax.script.ScriptException;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class JSScriptTest extends DBTestBase {

  @Test
  public void jsSimpleTest() {
    OResultSet resultSet = db.execute("javascript", "'foo'");
    Assert.assertTrue(resultSet.hasNext());
    OResult result = resultSet.next();
    String ret = result.getProperty("value");
    Assert.assertEquals("foo", ret);
  }

  @Test
  public void jsQueryTest() {
    String script = "db.query('select from OUser')";
    OResultSet resultSet = db.execute("javascript", script);
    Assert.assertTrue(resultSet.hasNext());

    List<OResult> results = resultSet.stream().collect(Collectors.toList());
    Assert.assertEquals(1, results.size()); // no default users anymore, 'admin' created

    results.stream()
        .map(r -> r.getElement().get())
        .forEach(
            oElement -> {
              Assert.assertEquals("OUser", oElement.getSchemaType().get().getName());
            });

  }

  @Test
  public void jsScriptTest() throws IOException {
    InputStream stream = ClassLoader.getSystemResourceAsStream("fixtures/scriptTest.js");
    OResultSet resultSet = db.execute("javascript", OIOUtils.readStreamAsString(stream));
    Assert.assertTrue(resultSet.hasNext());

    List<OResult> results = resultSet.stream().collect(Collectors.toList());
    Assert.assertEquals(1, results.size());

    Object value = results.get(0).getProperty("value");
    Collection<OResult> values = (Collection<OResult>) value;
    values.stream()
        .map(r -> r.getElement().get())
        .forEach(
            oElement -> {
              Assert.assertEquals("OUser", oElement.getSchemaType().get().getName());
            });

  }

  @Test
  public void jsScriptCountTest() throws IOException {
    InputStream stream = ClassLoader.getSystemResourceAsStream("fixtures/scriptCountTest.js");
    OResultSet resultSet = db.execute("javascript", OIOUtils.readStreamAsString(stream));
    Assert.assertTrue(resultSet.hasNext());

    List<OResult> results = resultSet.stream().collect(Collectors.toList());
    Assert.assertEquals(1, results.size());

    Number value = results.get(0).getProperty("value");
    Assert.assertEquals(1, value.intValue()); // no default users anymore, 'admin' created
  }

  @Test
  public void jsSandboxTestWithJavaType() {
    try {
      final OResultSet result =
          db.execute(
              "javascript", "var File = Java.type(\"java.io.File\");\n  File.pathSeparator;");

      Assert.fail("It should receive a class not found exception");
    } catch (RuntimeException e) {
      Assert.assertEquals(
          OGlobalConfiguration.SCRIPT_POLYGLOT_USE_GRAAL.getValueAsBoolean()
              ? ScriptException.class
              : ClassNotFoundException.class,
          e.getCause().getClass());
    }
  }

  // @Test
  // THIS TEST WONT PASS WITH GRAALVM
  public void jsSandboxWithNativeTest() {
    OScriptManager scriptManager = YouTrackDBInternal.extract(context).getScriptManager();
    try {
      scriptManager.addAllowedPackages(new HashSet<>(List.of("java.lang.System")));

      OResultSet resultSet =
          db.execute(
              "javascript", "var System = Java.type('java.lang.System'); System.nanoTime();");
      Assert.assertEquals(0, resultSet.stream().count());
    } finally {
      scriptManager.removeAllowedPackages(new HashSet<>(List.of("java.lang.System")));
    }
  }

  @Test
  public void jsSandboxWithMathTest() {
    OResultSet resultSet = db.execute("javascript", "Math.random()");
    Assert.assertEquals(1, resultSet.stream().count());
    resultSet.close();
  }

  @Test
  public void jsSandboxWithDB() {
    OResultSet resultSet =
        db.execute(
            "javascript",
            """
                var rs = db.query("select from OUser");
                var elem = rs.next();
                var prop = elem.getProperty("name");
                rs.close();
                prop;
                """);
    Assert.assertEquals(1, resultSet.stream().count());
    resultSet.close();
  }

  @Test
  public void jsSandboxWithBigDecimal() {
    final OScriptManager scriptManager = YouTrackDBInternal.extract(context).getScriptManager();
    try {
      scriptManager.addAllowedPackages(new HashSet<>(List.of("java.math.BigDecimal")));

      try (OResultSet resultSet =
          db.execute(
              "javascript",
              "var BigDecimal = Java.type('java.math.BigDecimal'); new BigDecimal(1.0);")) {
        Assert.assertEquals(1, resultSet.stream().count());
      }
      scriptManager.removeAllowedPackages(new HashSet<>(List.of("java.math.BigDecimal")));
      scriptManager.closeAll();

      try {
        db.execute("javascript", "new java.math.BigDecimal(1.0);");
        Assert.fail("It should receive a class not found exception");
      } catch (RuntimeException e) {
        Assert.assertEquals(
            OGlobalConfiguration.SCRIPT_POLYGLOT_USE_GRAAL.getValueAsBoolean()
                ? ScriptException.class
                : ClassNotFoundException.class,
            e.getCause().getClass());
      }

      scriptManager.addAllowedPackages(new HashSet<>(List.of("java.math.*")));
      scriptManager.closeAll();

      try (OResultSet resultSet = db.execute("javascript", "new java.math.BigDecimal(1.0);")) {
        Assert.assertEquals(1, resultSet.stream().count());
      }

    } finally {
      scriptManager.removeAllowedPackages(
          new HashSet<>(Arrays.asList("java.math.BigDecimal", "java.math.*")));
    }
  }

  @Test
  public void jsSandboxWithOrient() {
    try (OResultSet resultSet =
        db.execute("javascript", "Orient.instance().getScriptManager().addAllowedPackages([])")) {
      Assert.assertEquals(1, resultSet.stream().count());
    } catch (Exception e) {
      Assert.assertEquals(ScriptException.class, e.getCause().getClass());
    }

    try (OResultSet resultSet =
        db.execute(
            "javascript",
            "com.orientechnologies.orient.core.Orient.instance().getScriptManager().addAllowedPackages([])")) {
      Assert.assertEquals(1, resultSet.stream().count());
    } catch (Exception e) {
      Assert.assertEquals(
          OGlobalConfiguration.SCRIPT_POLYGLOT_USE_GRAAL.getValueAsBoolean()
              ? ScriptException.class
              : ClassNotFoundException.class,
          e.getCause().getClass());
    }

    try (OResultSet resultSet =
        db.execute(
            "javascript",
            "Java.type('com.orientechnologies.orient.core.Orient').instance().getScriptManager().addAllowedPackages([])")) {
      Assert.assertEquals(1, resultSet.stream().count());
    } catch (Exception e) {
      Assert.assertEquals(
          OGlobalConfiguration.SCRIPT_POLYGLOT_USE_GRAAL.getValueAsBoolean()
              ? ScriptException.class
              : ClassNotFoundException.class,
          e.getCause().getClass());
    }
  }
}
