package com.jetbrains.youtrack.db.internal.core.iterator;

import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.api.schema.Schema;
import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import com.jetbrains.youtrack.db.internal.DbTestBase;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.SchemaClassInternal;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.clusterselection.DefaultClusterSelectionStrategy;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import java.util.HashSet;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class ClassIteratorTest extends DbTestBase {

  private Set<String> names;

  private void createPerson(final String iClassName, final String first) {
    // Create Person document
    db.begin();
    final EntityImpl personDoc = db.newInstance(iClassName);
    personDoc.field("First", first);
    personDoc.save();
    db.commit();
  }

  public void beforeTest() throws Exception {
    super.beforeTest();

    final Schema schema = db.getMetadata().getSchema();

    // Create Person class
    final SchemaClass personClass = schema.createClass("Person");
    personClass
        .createProperty(db, "First", PropertyType.STRING)
        .setMandatory(db, true)
        .setNotNull(db, true)
        .setMin(db, "1");

    // Insert some data
    names = new HashSet<String>();
    names.add("Adam");
    names.add("Bob");
    names.add("Calvin");
    names.add("Daniel");

    for (String name : names) {
      createPerson("Person", name);
    }
  }

  @Test
  public void testDescendentOrderIteratorWithMultipleClusters() throws Exception {
    var personClass = (SchemaClassInternal) db.getMetadata().getSchema().getClass("Person");

    // empty old cluster but keep it attached
    personClass.truncate(db);

    // reload the data in a new 'test' cluster
    int testClusterId = db.addCluster("test");
    personClass.addClusterId(db, testClusterId);
    personClass.setClusterSelection(db, new DefaultClusterSelectionStrategy());

    for (String name : names) {
      createPerson("Person", name);
    }

    // Use descending class iterator.
    final RecordIteratorClass<EntityImpl> personIter =
        new RecordIteratorClassDescendentOrder<EntityImpl>(db, db, "Person", true);

    personIter.setRange(null, null); // open range

    int docNum = 0;
    // Explicit iterator loop.
    while (personIter.hasNext()) {
      final EntityImpl personDoc = personIter.next();
      Assert.assertTrue(names.contains(personDoc.field("First")));
      Assert.assertTrue(names.remove(personDoc.field("First")));
      System.out.printf("Doc %d: %s\n", docNum++, personDoc);
    }

    Assert.assertTrue(names.isEmpty());
  }

  @Test
  public void testMultipleClusters() throws Exception {
    final SchemaClass personClass =
        db.getMetadata().getSchema().createClass("PersonMultipleClusters", 4, null);
    for (String name : names) {
      createPerson("PersonMultipleClusters", name);
    }

    final RecordIteratorClass<EntityImpl> personIter =
        new RecordIteratorClass<EntityImpl>(db, "PersonMultipleClusters", true);

    int docNum = 0;

    while (personIter.hasNext()) {
      final EntityImpl personDoc = personIter.next();
      Assert.assertTrue(names.contains(personDoc.field("First")));
      Assert.assertTrue(names.remove(personDoc.field("First")));
      System.out.printf("Doc %d: %s\n", docNum++, personDoc);
    }

    Assert.assertTrue(names.isEmpty());
  }
}
