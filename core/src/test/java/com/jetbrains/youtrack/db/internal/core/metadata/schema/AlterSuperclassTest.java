package com.jetbrains.youtrack.db.internal.core.metadata.schema;

import static org.junit.Assert.assertEquals;

import com.jetbrains.youtrack.db.api.exception.SchemaException;
import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.api.schema.Schema;
import com.jetbrains.youtrack.db.internal.DbTestBase;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

/**
 *
 */
public class AlterSuperclassTest extends DbTestBase {

  @Test
  public void testSamePropertyCheck() {

    Schema schema = session.getMetadata().getSchema();
    var classA = schema.createClass("ParentClass");
    classA.setAbstract(session, true);
    var property = classA.createProperty(session, "RevNumberNine", PropertyType.INTEGER);
    var classChild = schema.createClass("ChildClass1", classA);
    assertEquals(classChild.getSuperClasses(session), List.of(classA));
    var classChild2 = schema.createClass("ChildClass2", classChild);
    assertEquals(classChild2.getSuperClasses(session), List.of(classChild));
    classChild2.setSuperClasses(session, List.of(classA));
    assertEquals(classChild2.getSuperClasses(session), List.of(classA));
  }

  @Test(expected = SchemaException.class)
  public void testPropertyNameConflict() {
    Schema schema = session.getMetadata().getSchema();
    var classA = schema.createClass("ParentClass");
    classA.setAbstract(session, true);
    var property = classA.createProperty(session, "RevNumberNine", PropertyType.INTEGER);
    var classChild = schema.createClass("ChildClass1", classA);
    assertEquals(classChild.getSuperClasses(session), List.of(classA));
    var classChild2 = schema.createClass("ChildClass2");
    classChild2.createProperty(session, "RevNumberNine", PropertyType.STRING);
    classChild2.setSuperClasses(session, List.of(classChild));
  }

  @Test(expected = SchemaException.class)
  public void testHasAlreadySuperclass() {
    Schema schema = session.getMetadata().getSchema();
    var classA = schema.createClass("ParentClass");
    var classChild = schema.createClass("ChildClass1", classA);
    assertEquals(classChild.getSuperClasses(session), Collections.singletonList(classA));
    classChild.addSuperClass(session, classA);
  }

  @Test(expected = SchemaException.class)
  public void testSetDuplicateSuperclasses() {
    Schema schema = session.getMetadata().getSchema();
    var classA = schema.createClass("ParentClass");
    var classChild = schema.createClass("ChildClass1", classA);
    assertEquals(classChild.getSuperClasses(session), Collections.singletonList(classA));
    classChild.setSuperClasses(session, Arrays.asList(classA, classA));
  }

  /**
   * This tests fixes a problem created in Issue #5586. It should not throw
   * ArrayIndexOutOfBoundsException
   */
  @Test
  public void testBrokenDbAlteringSuperClass() {
    Schema schema = session.getMetadata().getSchema();
    var classA = schema.createClass("BaseClass");
    var classChild = schema.createClass("ChildClass1", classA);
    var classChild2 = schema.createClass("ChildClass2", classA);

    classChild2.setSuperClass(session, classChild);

    schema.dropClass("ChildClass2");
  }
}
