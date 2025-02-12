package com.jetbrains.youtrack.db.internal.core.sql.executor;

import com.jetbrains.youtrack.db.api.exception.CommandExecutionException;
import com.jetbrains.youtrack.db.api.exception.DatabaseException;
import com.jetbrains.youtrack.db.internal.DbTestBase;
import com.jetbrains.youtrack.db.internal.core.metadata.sequence.DBSequence;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class DropDBSequenceStatementExecutionTest extends DbTestBase {

  @Test
  public void testPlain() {
    var name = "testPlain";
    try {
      session.getMetadata()
          .getSequenceLibrary()
          .createSequence(name, DBSequence.SEQUENCE_TYPE.CACHED, new DBSequence.CreateParams());
    } catch (DatabaseException exc) {
      Assert.fail("Creating sequence failed");
    }

    Assert.assertNotNull(session.getMetadata().getSequenceLibrary().getSequence(name));
    session.begin();
    var result = session.command("drop sequence " + name);
    Assert.assertTrue(result.hasNext());
    var next = result.next();
    Assert.assertEquals("drop sequence", next.getProperty("operation"));
    Assert.assertFalse(result.hasNext());
    result.close();
    session.commit();

    Assert.assertNull(session.getMetadata().getSequenceLibrary().getSequence(name));
  }

  @Test
  public void testNonExisting() {
    var name = "testNonExisting";
    var lib = session.getMetadata().getSequenceLibrary();
    Assert.assertNull(lib.getSequence(name));
    try {
      var result = session.command("drop sequence " + name);
      Assert.fail();
    } catch (CommandExecutionException ex1) {

    } catch (Exception ex1) {
      Assert.fail();
    }
  }

  @Test
  public void testNonExistingWithIfExists() {
    var name = "testNonExistingWithIfExists";
    var lib = session.getMetadata().getSequenceLibrary();
    Assert.assertNull(lib.getSequence(name));

    var result = session.command("drop sequence " + name + " if exists");
    Assert.assertFalse(result.hasNext());

    try {
      session.getMetadata()
          .getSequenceLibrary()
          .createSequence(name, DBSequence.SEQUENCE_TYPE.CACHED, new DBSequence.CreateParams());
    } catch (DatabaseException exc) {
      Assert.fail("Creating sequence failed");
    }

    Assert.assertNotNull(session.getMetadata().getSequenceLibrary().getSequence(name));
    session.begin();
    result = session.command("drop sequence " + name + " if exists");
    Assert.assertTrue(result.hasNext());
    var next = result.next();
    Assert.assertEquals("drop sequence", next.getProperty("operation"));
    Assert.assertFalse(result.hasNext());
    result.close();
    session.commit();

    Assert.assertNull(session.getMetadata().getSequenceLibrary().getSequence(name));
  }
}
