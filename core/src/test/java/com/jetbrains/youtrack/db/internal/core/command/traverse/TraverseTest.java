package com.jetbrains.youtrack.db.internal.core.command.traverse;

import com.jetbrains.youtrack.db.internal.DbTestBase;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class TraverseTest extends DbTestBase {

  private EntityImpl rootDocument;
  private Traverse traverse;

  public void beforeTest() throws Exception {
    super.beforeTest();

    rootDocument = new EntityImpl();
    traverse = new Traverse(db);
    traverse.target(rootDocument).fields("*");
  }

  @Test
  public void testDepthTraverse() {

    final EntityImpl aa = new EntityImpl();
    final EntityImpl ab = new EntityImpl();
    final EntityImpl ba = new EntityImpl();
    final EntityImpl bb = new EntityImpl();
    final EntityImpl a = new EntityImpl();
    a.setProperty("aa", aa, PropertyType.LINK);
    a.setProperty("ab", ab, PropertyType.LINK);
    final EntityImpl b = new EntityImpl();
    b.setProperty("ba", ba, PropertyType.LINK);
    b.setProperty("bb", bb, PropertyType.LINK);

    rootDocument.setProperty("a", a, PropertyType.LINK);
    rootDocument.setProperty("b", b, PropertyType.LINK);

    final EntityImpl c1 = new EntityImpl();
    final EntityImpl c1a = new EntityImpl();
    c1.setProperty("c1a", c1a, PropertyType.LINK);
    final EntityImpl c1b = new EntityImpl();
    c1.setProperty("c1b", c1b, PropertyType.LINK);
    final EntityImpl c2 = new EntityImpl();
    final EntityImpl c2a = new EntityImpl();
    c2.setProperty("c2a", c2a, PropertyType.LINK);
    final EntityImpl c2b = new EntityImpl();
    c2.setProperty("c2b", c2b, PropertyType.LINK);
    final EntityImpl c3 = new EntityImpl();
    final EntityImpl c3a = new EntityImpl();
    c3.setProperty("c3a", c3a, PropertyType.LINK);
    final EntityImpl c3b = new EntityImpl();
    c3.setProperty("c3b", c3b, PropertyType.LINK);
    rootDocument.setProperty("c", new ArrayList<>(Arrays.asList(c1, c2, c3)),
        PropertyType.LINKLIST);

    db.executeInTx(() -> rootDocument.save(db.getClusterNameById(db.getDefaultClusterId())));

    rootDocument = db.bindToSession(rootDocument);
    final List<EntityImpl> expectedResult =
        Arrays.asList(
            rootDocument,
            db.bindToSession(a),
            db.bindToSession(aa),
            db.bindToSession(ab),
            db.bindToSession(b),
            db.bindToSession(ba),
            db.bindToSession(bb),
            db.bindToSession(c1),
            db.bindToSession(c1a),
            db.bindToSession(c1b),
            db.bindToSession(c2),
            db.bindToSession(c2a),
            db.bindToSession(c2b),
            db.bindToSession(c3),
            db.bindToSession(c3a),
            db.bindToSession(c3b));

    final List<Identifiable> results = traverse.execute(db);

    compareTraverseResults(expectedResult, results);
  }

  @Test
  public void testBreadthTraverse() throws Exception {
    traverse.setStrategy(Traverse.STRATEGY.BREADTH_FIRST);

    final EntityImpl aa = new EntityImpl();
    final EntityImpl ab = new EntityImpl();
    final EntityImpl ba = new EntityImpl();
    final EntityImpl bb = new EntityImpl();
    final EntityImpl a = new EntityImpl();
    a.setProperty("aa", aa, PropertyType.LINK);
    a.setProperty("ab", ab, PropertyType.LINK);
    final EntityImpl b = new EntityImpl();
    b.setProperty("ba", ba, PropertyType.LINK);
    b.setProperty("bb", bb, PropertyType.LINK);

    rootDocument.setProperty("a", a, PropertyType.LINK);
    rootDocument.setProperty("b", b, PropertyType.LINK);

    final EntityImpl c1 = new EntityImpl();
    final EntityImpl c1a = new EntityImpl();
    c1.setProperty("c1a", c1a, PropertyType.LINK);
    final EntityImpl c1b = new EntityImpl();
    c1.setProperty("c1b", c1b, PropertyType.LINK);
    final EntityImpl c2 = new EntityImpl();
    final EntityImpl c2a = new EntityImpl();
    c2.setProperty("c2a", c2a, PropertyType.LINK);
    final EntityImpl c2b = new EntityImpl();
    c2.setProperty("c2b", c2b, PropertyType.LINK);
    final EntityImpl c3 = new EntityImpl();
    final EntityImpl c3a = new EntityImpl();
    c3.setProperty("c3a", c3a, PropertyType.LINK);
    final EntityImpl c3b = new EntityImpl();
    c3.setProperty("c3b", c3b, PropertyType.LINK);
    rootDocument.setProperty("c", new ArrayList<>(Arrays.asList(c1, c2, c3)),
        PropertyType.LINKLIST);

    db.executeInTx(() -> rootDocument.save(db.getClusterNameById(db.getDefaultClusterId())));

    rootDocument = db.bindToSession(rootDocument);
    final List<EntityImpl> expectedResult =
        Arrays.asList(
            rootDocument,
            db.bindToSession(a),
            db.bindToSession(b),
            db.bindToSession(aa),
            db.bindToSession(ab),
            db.bindToSession(ba),
            db.bindToSession(bb),
            db.bindToSession(c1),
            db.bindToSession(c2),
            db.bindToSession(c3),
            db.bindToSession(c1a),
            db.bindToSession(c1b),
            db.bindToSession(c2a),
            db.bindToSession(c2b),
            db.bindToSession(c3a),
            db.bindToSession(c3b));

    final List<Identifiable> results = traverse.execute(db);

    compareTraverseResults(expectedResult, results);
  }

  private void compareTraverseResults(List<EntityImpl> expectedResult,
      List<Identifiable> results) {
    boolean equality = results.size() == expectedResult.size();
    for (int i = 0; i < expectedResult.size() && equality; i++) {
      equality &= results.get(i).equals(expectedResult.get(i));
    }
    System.out.println("Expected: " + expectedResult);
    System.out.println();
    System.out.println("Actual:   " + results);
    Assert.assertTrue(equality);
  }
}
