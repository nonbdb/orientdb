package com.jetbrains.youtrack.db.internal.lucene.test;

import com.jetbrains.youtrack.db.api.query.ResultSet;
import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.api.schema.Schema;
import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 */
public class LuceneFreezeReleaseTest extends BaseLuceneTest {
  @Test
  @Ignore
  public void freezeReleaseTest() {
    if (isWindows()) {
      return;
    }

    Schema schema = db.getMetadata().getSchema();
    var person = schema.createClass("Person");
    person.createProperty(db, "name", PropertyType.STRING);

    db.command("create index Person.name on Person (name) FULLTEXT ENGINE LUCENE").close();

    db.begin();
    db.save(((EntityImpl) db.newEntity("Person")).field("name", "John"));
    db.commit();

    var results = db.query("select from Person where name lucene 'John'");
    Assert.assertEquals(1, results.stream().count());
    db.freeze();

    results = db.query("select from Person where name lucene 'John'");
    Assert.assertEquals(1, results.stream().count());

    db.release();

    db.begin();
    db.save(((EntityImpl) db.newEntity("Person")).field("name", "John"));
    db.commit();

    results = db.query("select from Person where name lucene 'John'");
    Assert.assertEquals(2, results.stream().count());
  }

  // With double calling freeze/release
  @Test
  @Ignore
  public void freezeReleaseMisUsageTest() {
    if (isWindows()) {
      return;
    }

    Schema schema = db.getMetadata().getSchema();
    var person = schema.createClass("Person");
    person.createProperty(db, "name", PropertyType.STRING);

    db.command("create index Person.name on Person (name) FULLTEXT ENGINE LUCENE").close();

    db.begin();
    db.save(((EntityImpl) db.newEntity("Person")).field("name", "John"));
    db.commit();

    var results = db.query("select from Person where name lucene 'John'");
      Assert.assertEquals(1, results.stream().count());

      db.freeze();

      db.freeze();

      results = db.query("select from Person where name lucene 'John'");
      Assert.assertEquals(1, results.stream().count());

      db.release();
      db.release();

      db.begin();
    db.save(((EntityImpl) db.newEntity("Person")).field("name", "John"));
      db.commit();

      results = db.query("select from Person where name lucene 'John'");
      Assert.assertEquals(2, results.stream().count());
  }

  private static boolean isWindows() {
    final var osName = System.getProperty("os.name").toLowerCase();
    return osName.contains("win");
  }
}
