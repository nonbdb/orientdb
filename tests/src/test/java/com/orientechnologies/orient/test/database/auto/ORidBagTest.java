package com.orientechnologies.orient.test.database.auto;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import com.orientechnologies.orient.client.remote.OServerAdmin;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.db.record.ridbag.ORidBag;
import com.orientechnologies.orient.core.exception.OConcurrentModificationException;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.record.ORecordAbstract;
import com.orientechnologies.orient.core.record.ORecordInternal;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.record.impl.ODocumentHelper;
import com.orientechnologies.orient.core.storage.OStorageProxy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.testng.Assert;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

@Test
public abstract class ORidBagTest extends DocumentDBBaseTest {

  @Parameters(value = "remote")
  public ORidBagTest(@Optional Boolean remote) {
    super(remote != null && remote);
  }

  public void testAdd() throws Exception {
    ORidBag bag = new ORidBag(database);

    bag.add(new ORecordId("#77:1"));
    Assert.assertTrue(bag.contains(new ORecordId("#77:1")));
    Assert.assertFalse(bag.contains(new ORecordId("#78:2")));

    Iterator<OIdentifiable> iterator = bag.iterator();
    Assert.assertTrue(iterator.hasNext());

    OIdentifiable identifiable = iterator.next();
    Assert.assertEquals(identifiable, new ORecordId("#77:1"));

    Assert.assertFalse(iterator.hasNext());
    assertEmbedded(bag.isEmbedded());
  }

  public void testAdd2() throws Exception {
    ORidBag bag = new ORidBag(database);

    bag.add(new ORecordId("#77:2"));
    bag.add(new ORecordId("#77:2"));

    Assert.assertTrue(bag.contains(new ORecordId("#77:2")));
    Assert.assertFalse(bag.contains(new ORecordId("#77:3")));

    assertEquals(bag.size(), 2);
    assertEmbedded(bag.isEmbedded());
  }

  public void testAddRemoveInTheMiddleOfIteration() {
    ORidBag bag = new ORidBag(database);

    bag.add(new ORecordId("#77:2"));
    bag.add(new ORecordId("#77:2"));
    bag.add(new ORecordId("#77:3"));
    bag.add(new ORecordId("#77:4"));
    bag.add(new ORecordId("#77:4"));
    bag.add(new ORecordId("#77:4"));
    bag.add(new ORecordId("#77:5"));
    bag.add(new ORecordId("#77:6"));

    int counter = 0;
    Iterator<OIdentifiable> iterator = bag.iterator();

    bag.remove(new ORecordId("#77:2"));
    while (iterator.hasNext()) {
      counter++;
      if (counter == 1) {
        bag.remove(new ORecordId("#77:1"));
        bag.remove(new ORecordId("#77:2"));
      }

      if (counter == 3) {
        bag.remove(new ORecordId("#77:4"));
      }

      if (counter == 5) {
        bag.remove(new ORecordId("#77:6"));
      }

      iterator.next();
    }

    Assert.assertTrue(bag.contains(new ORecordId("#77:3")));
    Assert.assertTrue(bag.contains(new ORecordId("#77:4")));
    Assert.assertTrue(bag.contains(new ORecordId("#77:5")));

    Assert.assertFalse(bag.contains(new ORecordId("#77:2")));
    Assert.assertFalse(bag.contains(new ORecordId("#77:6")));
    Assert.assertFalse(bag.contains(new ORecordId("#77:1")));
    Assert.assertFalse(bag.contains(new ORecordId("#77:0")));

    assertEmbedded(bag.isEmbedded());

    final List<OIdentifiable> rids = new ArrayList<>();
    rids.add(new ORecordId("#77:3"));
    rids.add(new ORecordId("#77:4"));
    rids.add(new ORecordId("#77:4"));
    rids.add(new ORecordId("#77:5"));

    for (OIdentifiable identifiable : bag) {
      assertTrue(rids.remove(identifiable));
    }

    assertTrue(rids.isEmpty());

    for (OIdentifiable identifiable : bag) {
      rids.add(identifiable);
    }

    ODocument doc = new ODocument();
    doc.field("ridbag", bag);
    database.begin();
    doc.save(database.getClusterNameById(database.getDefaultClusterId()));
    database.commit();

    ORID rid = doc.getIdentity();

    doc = database.load(rid);
    doc.setLazyLoad(false);

    bag = doc.field("ridbag");
    assertEmbedded(bag.isEmbedded());

    Assert.assertTrue(bag.contains(new ORecordId("#77:3")));
    Assert.assertTrue(bag.contains(new ORecordId("#77:4")));
    Assert.assertTrue(bag.contains(new ORecordId("#77:5")));

    Assert.assertFalse(bag.contains(new ORecordId("#77:2")));
    Assert.assertFalse(bag.contains(new ORecordId("#77:6")));
    Assert.assertFalse(bag.contains(new ORecordId("#77:1")));
    Assert.assertFalse(bag.contains(new ORecordId("#77:0")));

    for (OIdentifiable identifiable : bag) {
      assertTrue(rids.remove(identifiable));
    }

    assertTrue(rids.isEmpty());
  }

  public void testAddRemove() {
    ORidBag bag = new ORidBag(database);

    bag.add(new ORecordId("#77:2"));
    bag.add(new ORecordId("#77:2"));
    bag.add(new ORecordId("#77:3"));
    bag.add(new ORecordId("#77:4"));
    bag.add(new ORecordId("#77:4"));
    bag.add(new ORecordId("#77:4"));
    bag.add(new ORecordId("#77:5"));
    bag.add(new ORecordId("#77:6"));

    bag.remove(new ORecordId("#77:1"));
    bag.remove(new ORecordId("#77:2"));
    bag.remove(new ORecordId("#77:2"));
    bag.remove(new ORecordId("#77:4"));
    bag.remove(new ORecordId("#77:6"));

    Assert.assertTrue(bag.contains(new ORecordId("#77:3")));
    Assert.assertTrue(bag.contains(new ORecordId("#77:4")));
    Assert.assertTrue(bag.contains(new ORecordId("#77:5")));

    Assert.assertFalse(bag.contains(new ORecordId("#77:2")));
    Assert.assertFalse(bag.contains(new ORecordId("#77:6")));
    Assert.assertFalse(bag.contains(new ORecordId("#77:1")));
    Assert.assertFalse(bag.contains(new ORecordId("#77:0")));

    assertEmbedded(bag.isEmbedded());

    final List<OIdentifiable> rids = new ArrayList<OIdentifiable>();
    rids.add(new ORecordId("#77:3"));
    rids.add(new ORecordId("#77:4"));
    rids.add(new ORecordId("#77:4"));
    rids.add(new ORecordId("#77:5"));

    for (OIdentifiable identifiable : bag) {
      assertTrue(rids.remove(identifiable));
    }

    assertTrue(rids.isEmpty());

    for (OIdentifiable identifiable : bag) {
      rids.add(identifiable);
    }

    ODocument doc = new ODocument();
    doc.field("ridbag", bag);
    database.begin();
    doc.save(database.getClusterNameById(database.getDefaultClusterId()));
    database.commit();

    ORID rid = doc.getIdentity();

    doc = database.load(rid);
    doc.setLazyLoad(false);

    bag = doc.field("ridbag");
    assertEmbedded(bag.isEmbedded());

    Assert.assertTrue(bag.contains(new ORecordId("#77:3")));
    Assert.assertTrue(bag.contains(new ORecordId("#77:4")));
    Assert.assertTrue(bag.contains(new ORecordId("#77:5")));

    Assert.assertFalse(bag.contains(new ORecordId("#77:2")));
    Assert.assertFalse(bag.contains(new ORecordId("#77:6")));
    Assert.assertFalse(bag.contains(new ORecordId("#77:1")));
    Assert.assertFalse(bag.contains(new ORecordId("#77:0")));

    for (OIdentifiable identifiable : bag) {
      assertTrue(rids.remove(identifiable));
    }

    assertTrue(rids.isEmpty());
  }

  public void testAddRemoveSBTreeContainsValues() {
    ORidBag bag = new ORidBag(database);

    bag.add(new ORecordId("#77:2"));
    bag.add(new ORecordId("#77:2"));
    bag.add(new ORecordId("#77:3"));
    bag.add(new ORecordId("#77:4"));
    bag.add(new ORecordId("#77:4"));
    bag.add(new ORecordId("#77:4"));
    bag.add(new ORecordId("#77:5"));
    bag.add(new ORecordId("#77:6"));

    assertEmbedded(bag.isEmbedded());

    ODocument doc = new ODocument();
    doc.field("ridbag", bag);
    database.begin();
    doc.save(database.getClusterNameById(database.getDefaultClusterId()));
    database.commit();

    ORID rid = doc.getIdentity();

    database.close();

    database = createSessionInstance();

    doc = database.load(rid);
    doc.setLazyLoad(false);

    bag = doc.field("ridbag");
    assertEmbedded(bag.isEmbedded());

    bag.remove(new ORecordId("#77:1"));
    bag.remove(new ORecordId("#77:2"));
    bag.remove(new ORecordId("#77:2"));
    bag.remove(new ORecordId("#77:4"));
    bag.remove(new ORecordId("#77:6"));

    final List<OIdentifiable> rids = new ArrayList<>();
    rids.add(new ORecordId("#77:3"));
    rids.add(new ORecordId("#77:4"));
    rids.add(new ORecordId("#77:4"));
    rids.add(new ORecordId("#77:5"));

    for (OIdentifiable identifiable : bag) {
      assertTrue(rids.remove(identifiable));
    }

    assertTrue(rids.isEmpty());

    for (OIdentifiable identifiable : bag) {
      rids.add(identifiable);
    }

    doc = new ODocument();
    ORidBag otherBag = new ORidBag(database);
    for (OIdentifiable id : bag) {
      otherBag.add(id);
    }

    assertEmbedded(otherBag.isEmbedded());
    doc.field("ridbag", otherBag);
    database.begin();
    doc.save(database.getClusterNameById(database.getDefaultClusterId()));
    database.commit();

    rid = doc.getIdentity();

    doc = database.load(rid);
    doc.setLazyLoad(false);

    bag = doc.field("ridbag");
    assertEmbedded(bag.isEmbedded());

    for (OIdentifiable identifiable : bag) {
      assertTrue(rids.remove(identifiable));
    }

    assertTrue(rids.isEmpty());
  }

  public void testAddRemoveDuringIterationSBTreeContainsValues() {
    database.begin();
    ORidBag bag = new ORidBag(database);
    assertEmbedded(bag.isEmbedded());

    bag.add(new ORecordId("#77:2"));
    bag.add(new ORecordId("#77:2"));
    bag.add(new ORecordId("#77:3"));
    bag.add(new ORecordId("#77:4"));
    bag.add(new ORecordId("#77:4"));
    bag.add(new ORecordId("#77:4"));
    bag.add(new ORecordId("#77:5"));
    bag.add(new ORecordId("#77:6"));
    assertEmbedded(bag.isEmbedded());

    ODocument doc = new ODocument();
    doc.field("ridbag", bag);

    doc.save(database.getClusterNameById(database.getDefaultClusterId()));
    database.commit();

    ORID rid = doc.getIdentity();
    database.close();

    database = createSessionInstance();
    doc = database.load(rid);
    doc.setLazyLoad(false);

    bag = doc.field("ridbag");
    assertEmbedded(bag.isEmbedded());

    bag.remove(new ORecordId("#77:1"));
    bag.remove(new ORecordId("#77:2"));
    bag.remove(new ORecordId("#77:2"));
    bag.remove(new ORecordId("#77:4"));
    bag.remove(new ORecordId("#77:6"));
    assertEmbedded(bag.isEmbedded());

    final List<OIdentifiable> rids = new ArrayList<OIdentifiable>();
    rids.add(new ORecordId("#77:3"));
    rids.add(new ORecordId("#77:4"));
    rids.add(new ORecordId("#77:4"));
    rids.add(new ORecordId("#77:5"));

    for (OIdentifiable identifiable : bag) {
      assertTrue(rids.remove(identifiable));
    }

    assertTrue(rids.isEmpty());

    for (OIdentifiable identifiable : bag) {
      rids.add(identifiable);
    }

    Iterator<OIdentifiable> iterator = bag.iterator();
    while (iterator.hasNext()) {
      final OIdentifiable identifiable = iterator.next();
      if (identifiable.equals(new ORecordId("#77:4"))) {
        iterator.remove();
        assertTrue(rids.remove(identifiable));
      }
    }

    for (OIdentifiable identifiable : bag) {
      assertTrue(rids.remove(identifiable));
    }

    for (OIdentifiable identifiable : bag) {
      rids.add(identifiable);
    }

    assertEmbedded(bag.isEmbedded());
    doc = new ODocument();

    final ORidBag otherBag = new ORidBag(database);
    for (OIdentifiable id : bag) {
      otherBag.add(id);
    }

    assertEmbedded(otherBag.isEmbedded());
    doc.field("ridbag", otherBag);

    database.begin();
    doc.save(database.getClusterNameById(database.getDefaultClusterId()));
    database.commit();

    rid = doc.getIdentity();

    doc = database.load(rid);
    doc.setLazyLoad(false);

    bag = doc.field("ridbag");
    assertEmbedded(bag.isEmbedded());

    for (OIdentifiable identifiable : bag) {
      assertTrue(rids.remove(identifiable));
    }

    assertTrue(rids.isEmpty());
  }

  public void testEmptyIterator() throws Exception {
    ORidBag bag = new ORidBag(database);
    assertEmbedded(bag.isEmbedded());
    assertEquals(bag.size(), 0);

    for (OIdentifiable id : bag) {
      Assert.fail();
    }
  }

  public void testAddRemoveNotExisting() {
    List<OIdentifiable> rids = new ArrayList<OIdentifiable>();

    ORidBag bag = new ORidBag(database);
    assertEmbedded(bag.isEmbedded());

    bag.add(new ORecordId("#77:2"));
    rids.add(new ORecordId("#77:2"));

    bag.add(new ORecordId("#77:2"));
    rids.add(new ORecordId("#77:2"));

    bag.add(new ORecordId("#77:3"));
    rids.add(new ORecordId("#77:3"));

    bag.add(new ORecordId("#77:4"));
    rids.add(new ORecordId("#77:4"));

    bag.add(new ORecordId("#77:4"));
    rids.add(new ORecordId("#77:4"));

    bag.add(new ORecordId("#77:4"));
    rids.add(new ORecordId("#77:4"));

    bag.add(new ORecordId("#77:5"));
    rids.add(new ORecordId("#77:5"));

    bag.add(new ORecordId("#77:6"));
    rids.add(new ORecordId("#77:6"));
    assertEmbedded(bag.isEmbedded());

    ODocument doc = new ODocument();
    doc.field("ridbag", bag);

    database.begin();
    doc.save(database.getClusterNameById(database.getDefaultClusterId()));
    database.commit();

    ORID rid = doc.getIdentity();

    database.close();

    database = createSessionInstance();

    doc = database.load(rid);
    doc.setLazyLoad(false);

    bag = doc.field("ridbag");
    assertEmbedded(bag.isEmbedded());

    bag.add(new ORecordId("#77:2"));
    rids.add(new ORecordId("#77:2"));

    bag.remove(new ORecordId("#77:4"));
    rids.remove(new ORecordId("#77:4"));

    bag.remove(new ORecordId("#77:4"));
    rids.remove(new ORecordId("#77:4"));

    bag.remove(new ORecordId("#77:2"));
    rids.remove(new ORecordId("#77:2"));

    bag.remove(new ORecordId("#77:2"));
    rids.remove(new ORecordId("#77:2"));

    bag.remove(new ORecordId("#77:7"));
    rids.remove(new ORecordId("#77:7"));

    bag.remove(new ORecordId("#77:8"));
    rids.remove(new ORecordId("#77:8"));

    bag.remove(new ORecordId("#77:8"));
    rids.remove(new ORecordId("#77:8"));

    bag.remove(new ORecordId("#77:8"));
    rids.remove(new ORecordId("#77:8"));

    assertEmbedded(bag.isEmbedded());

    for (OIdentifiable identifiable : bag) {
      assertTrue(rids.remove(identifiable));
    }

    assertTrue(rids.isEmpty());

    for (OIdentifiable identifiable : bag) {
      rids.add(identifiable);
    }

    database.begin();
    doc.save();
    database.commit();

    doc = database.load(rid);
    doc.setLazyLoad(false);

    bag = doc.field("ridbag");
    assertEmbedded(bag.isEmbedded());

    for (OIdentifiable identifiable : bag) {
      assertTrue(rids.remove(identifiable));
    }

    assertTrue(rids.isEmpty());
  }

  public void testContentChange() {
    ODocument document = new ODocument();
    ORidBag ridBag = new ORidBag(database);
    document.field("ridBag", ridBag);

    database.begin();
    document.save(database.getClusterNameById(database.getDefaultClusterId()));
    database.commit();

    document = database.bindToSession(document);
    ridBag = document.field("ridBag");
    ridBag.add(new ORecordId("#77:10"));
    Assert.assertTrue(document.isDirty());

    boolean expectCME = false;
    if (ORecordInternal.isContentChanged(document)) {
      assertEmbedded(true);
      expectCME = true;
    } else {
      assertEmbedded(false);
    }

    database.begin();
    document.save();
    database.commit();

    ODocument copy = new ODocument();
    ORecordInternal.unsetDirty(copy);
    document = database.bindToSession(document);
    ridBag = document.field("ridBag");
    copy.fromStream(document.toStream());
    ORecordInternal.setIdentity(copy, new ORecordId(document.getIdentity()));
    ORecordInternal.setVersion(copy, document.getVersion());

    ORidBag copyRidBag = copy.field("ridBag");
    Assert.assertNotSame(copyRidBag, ridBag);

    copyRidBag.add(new ORecordId("#77:11"));
    Assert.assertTrue(copy.isDirty());
    Assert.assertFalse(document.isDirty());

    ridBag.add(new ORecordId("#77:12"));
    Assert.assertTrue(document.isDirty());

    database.begin();
    document.save();
    database.commit();

    try {
      database.begin();
      copy.save();
      database.commit();
      Assert.assertFalse(expectCME);
    } catch (OConcurrentModificationException cme) {
      Assert.assertTrue(expectCME);
    }
  }

  public void testAddAllAndIterator() throws Exception {
    final Set<OIdentifiable> expected = new HashSet<OIdentifiable>(8);

    expected.add(new ORecordId("#77:12"));
    expected.add(new ORecordId("#77:13"));
    expected.add(new ORecordId("#77:14"));
    expected.add(new ORecordId("#77:15"));
    expected.add(new ORecordId("#77:16"));

    ORidBag bag = new ORidBag(database);

    bag.addAll(expected);
    assertEmbedded(bag.isEmbedded());

    assertEquals(bag.size(), 5);

    Set<OIdentifiable> actual = new HashSet<OIdentifiable>(8);
    for (OIdentifiable id : bag) {
      actual.add(id);
    }

    assertEquals(actual, expected);
  }

  public void testAddSBTreeAddInMemoryIterate() {
    List<OIdentifiable> rids = new ArrayList<OIdentifiable>();

    ORidBag bag = new ORidBag(database);
    assertEmbedded(bag.isEmbedded());

    bag.add(new ORecordId("#77:2"));
    rids.add(new ORecordId("#77:2"));

    bag.add(new ORecordId("#77:2"));
    rids.add(new ORecordId("#77:2"));

    bag.add(new ORecordId("#77:3"));
    rids.add(new ORecordId("#77:3"));

    bag.add(new ORecordId("#77:4"));
    rids.add(new ORecordId("#77:4"));

    bag.add(new ORecordId("#77:4"));
    rids.add(new ORecordId("#77:4"));
    assertEmbedded(bag.isEmbedded());

    ODocument doc = new ODocument();
    doc.field("ridbag", bag);

    database.begin();
    doc.save(database.getClusterNameById(database.getDefaultClusterId()));
    database.commit();

    ORID rid = doc.getIdentity();

    database.close();

    database = createSessionInstance();

    doc = database.load(rid);
    doc.setLazyLoad(false);

    bag = doc.field("ridbag");
    assertEmbedded(bag.isEmbedded());

    bag.add(new ORecordId("#77:0"));
    rids.add(new ORecordId("#77:0"));

    bag.add(new ORecordId("#77:1"));
    rids.add(new ORecordId("#77:1"));

    bag.add(new ORecordId("#77:2"));
    rids.add(new ORecordId("#77:2"));

    bag.add(new ORecordId("#77:3"));
    rids.add(new ORecordId("#77:3"));

    bag.add(new ORecordId("#77:5"));
    rids.add(new ORecordId("#77:5"));

    bag.add(new ORecordId("#77:6"));
    rids.add(new ORecordId("#77:6"));

    assertEmbedded(bag.isEmbedded());

    for (OIdentifiable identifiable : bag) {
      assertTrue(rids.remove(identifiable));
    }

    assertTrue(rids.isEmpty());

    for (OIdentifiable identifiable : bag) {
      rids.add(identifiable);
    }

    doc = new ODocument();
    final ORidBag otherBag = new ORidBag(database);
    for (OIdentifiable id : bag) {
      otherBag.add(id);
    }

    doc.field("ridbag", otherBag);

    database.begin();
    doc.save(database.getClusterNameById(database.getDefaultClusterId()));
    database.commit();

    rid = doc.getIdentity();

    doc = database.load(rid);
    doc.setLazyLoad(false);

    bag = doc.field("ridbag");
    assertEmbedded(bag.isEmbedded());

    for (OIdentifiable identifiable : bag) {
      assertTrue(rids.remove(identifiable));
    }

    assertTrue(rids.isEmpty());
  }

  public void testCycle() {
    ODocument docOne = new ODocument();
    ORidBag ridBagOne = new ORidBag(database);

    ODocument docTwo = new ODocument();
    ORidBag ridBagTwo = new ORidBag(database);

    docOne.field("ridBag", ridBagOne);
    docTwo.field("ridBag", ridBagTwo);

    database.begin();
    docOne.save(database.getClusterNameById(database.getDefaultClusterId()));
    docTwo.save(database.getClusterNameById(database.getDefaultClusterId()));
    database.commit();

    docOne = database.bindToSession(docOne);
    docTwo = database.bindToSession(docTwo);

    ridBagOne = docOne.field("ridBag");
    ridBagOne.add(docTwo);

    ridBagTwo = docTwo.field("ridBag");
    ridBagTwo.add(docOne);

    database.begin();
    docOne.save(database.getClusterNameById(database.getDefaultClusterId()));
    database.commit();

    docOne = database.load(docOne.getIdentity(), "*:-1", false);
    ridBagOne = docOne.field("ridBag");

    docTwo = database.load(docTwo.getIdentity(), "*:-1", false);
    ridBagTwo = docTwo.field("ridBag");

    Assert.assertEquals(ridBagOne.iterator().next(), docTwo);
    Assert.assertEquals(ridBagTwo.iterator().next(), docOne);
  }

  public void testAddSBTreeAddInMemoryIterateAndRemove() {
    List<OIdentifiable> rids = new ArrayList<OIdentifiable>();

    ORidBag bag = new ORidBag(database);
    assertEmbedded(bag.isEmbedded());

    bag.add(new ORecordId("#77:2"));
    rids.add(new ORecordId("#77:2"));

    bag.add(new ORecordId("#77:2"));
    rids.add(new ORecordId("#77:2"));

    bag.add(new ORecordId("#77:3"));
    rids.add(new ORecordId("#77:3"));

    bag.add(new ORecordId("#77:4"));
    rids.add(new ORecordId("#77:4"));

    bag.add(new ORecordId("#77:4"));
    rids.add(new ORecordId("#77:4"));

    bag.add(new ORecordId("#77:7"));
    rids.add(new ORecordId("#77:7"));

    bag.add(new ORecordId("#77:8"));
    rids.add(new ORecordId("#77:8"));

    assertEmbedded(bag.isEmbedded());

    ODocument doc = new ODocument();
    doc.field("ridbag", bag);

    database.begin();
    doc.save(database.getClusterNameById(database.getDefaultClusterId()));
    database.commit();

    ORID rid = doc.getIdentity();
    database.close();

    database = createSessionInstance();

    doc = database.load(rid);
    doc.setLazyLoad(false);

    bag = doc.field("ridbag");
    assertEmbedded(bag.isEmbedded());

    bag.add(new ORecordId("#77:0"));
    rids.add(new ORecordId("#77:0"));

    bag.add(new ORecordId("#77:1"));
    rids.add(new ORecordId("#77:1"));

    bag.add(new ORecordId("#77:2"));
    rids.add(new ORecordId("#77:2"));

    bag.add(new ORecordId("#77:3"));
    rids.add(new ORecordId("#77:3"));

    bag.add(new ORecordId("#77:3"));
    rids.add(new ORecordId("#77:3"));

    bag.add(new ORecordId("#77:5"));
    rids.add(new ORecordId("#77:5"));

    bag.add(new ORecordId("#77:6"));
    rids.add(new ORecordId("#77:6"));

    assertEmbedded(bag.isEmbedded());

    Iterator<OIdentifiable> iterator = bag.iterator();
    int r2c = 0;
    int r3c = 0;
    int r6c = 0;
    int r4c = 0;
    int r7c = 0;

    while (iterator.hasNext()) {
      OIdentifiable identifiable = iterator.next();
      if (identifiable.equals(new ORecordId("#77:2"))) {
        if (r2c < 2) {
          r2c++;
          iterator.remove();
          rids.remove(identifiable);
        }
      }

      if (identifiable.equals(new ORecordId("#77:3"))) {
        if (r3c < 1) {
          r3c++;
          iterator.remove();
          rids.remove(identifiable);
        }
      }

      if (identifiable.equals(new ORecordId("#77:6"))) {
        if (r6c < 1) {
          r6c++;
          iterator.remove();
          rids.remove(identifiable);
        }
      }

      if (identifiable.equals(new ORecordId("#77:4"))) {
        if (r4c < 1) {
          r4c++;
          iterator.remove();
          rids.remove(identifiable);
        }
      }

      if (identifiable.equals(new ORecordId("#77:7"))) {
        if (r7c < 1) {
          r7c++;
          iterator.remove();
          rids.remove(identifiable);
        }
      }
    }

    assertEquals(r2c, 2);
    assertEquals(r3c, 1);
    assertEquals(r6c, 1);
    assertEquals(r4c, 1);
    assertEquals(r7c, 1);

    for (OIdentifiable identifiable : bag) {
      assertTrue(rids.remove(identifiable));
    }

    assertTrue(rids.isEmpty());

    for (OIdentifiable identifiable : bag) {
      rids.add(identifiable);
    }

    doc = new ODocument();

    final ORidBag otherBag = new ORidBag(database);
    for (OIdentifiable id : bag) {
      otherBag.add(id);
    }

    assertEmbedded(otherBag.isEmbedded());

    doc.field("ridbag", otherBag);

    database.begin();
    doc.save(database.getClusterNameById(database.getDefaultClusterId()));
    database.commit();

    rid = doc.getIdentity();

    doc = database.load(rid);
    doc.setLazyLoad(false);

    bag = doc.field("ridbag");
    assertEmbedded(bag.isEmbedded());

    for (OIdentifiable identifiable : bag) {
      assertTrue(rids.remove(identifiable));
    }

    assertTrue(rids.isEmpty());
  }

  public void testRemove() {
    final Set<OIdentifiable> expected = new HashSet<OIdentifiable>(8);

    expected.add(new ORecordId("#77:12"));
    expected.add(new ORecordId("#77:13"));
    expected.add(new ORecordId("#77:14"));
    expected.add(new ORecordId("#77:15"));
    expected.add(new ORecordId("#77:16"));

    final ORidBag bag = new ORidBag(database);
    assertEmbedded(bag.isEmbedded());
    bag.addAll(expected);
    assertEmbedded(bag.isEmbedded());

    bag.remove(new ORecordId("#77:23"));
    assertEmbedded(bag.isEmbedded());

    final Set<OIdentifiable> expectedTwo = new HashSet<OIdentifiable>(8);
    expectedTwo.addAll(expected);

    for (OIdentifiable identifiable : bag) {
      assertTrue(expectedTwo.remove(identifiable));
    }

    Assert.assertTrue(expectedTwo.isEmpty());

    expected.remove(new ORecordId("#77:14"));
    bag.remove(new ORecordId("#77:14"));
    assertEmbedded(bag.isEmbedded());

    expectedTwo.addAll(expected);

    for (OIdentifiable identifiable : bag) {
      assertTrue(expectedTwo.remove(identifiable));
    }
  }

  public void testSaveLoad() throws Exception {
    Set<OIdentifiable> expected = new HashSet<OIdentifiable>(8);

    expected.add(new ORecordId("#77:12"));
    expected.add(new ORecordId("#77:13"));
    expected.add(new ORecordId("#77:14"));
    expected.add(new ORecordId("#77:15"));
    expected.add(new ORecordId("#77:16"));
    expected.add(new ORecordId("#77:17"));
    expected.add(new ORecordId("#77:18"));
    expected.add(new ORecordId("#77:19"));
    expected.add(new ORecordId("#77:20"));
    expected.add(new ORecordId("#77:21"));
    expected.add(new ORecordId("#77:22"));

    ODocument doc = new ODocument();

    final ORidBag bag = new ORidBag(database);
    bag.addAll(expected);

    doc.field("ridbag", bag);
    assertEmbedded(bag.isEmbedded());

    database.begin();
    doc.save(database.getClusterNameById(database.getDefaultClusterId()));
    database.commit();
    final ORID id = doc.getIdentity();

    database.close();

    database = createSessionInstance();

    doc = database.load(id);
    doc.setLazyLoad(false);

    final ORidBag loaded = doc.field("ridbag");
    assertEmbedded(loaded.isEmbedded());

    Assert.assertEquals(loaded.size(), expected.size());
    for (OIdentifiable identifiable : loaded) {
      Assert.assertTrue(expected.remove(identifiable));
    }

    Assert.assertTrue(expected.isEmpty());
  }

  public void testSaveInBackOrder() throws Exception {
    ODocument docA = new ODocument().field("name", "A");

    database.begin();
    ODocument docB =
        new ODocument()
            .field("name", "B");
    docB.save(database.getClusterNameById(database.getDefaultClusterId()));
    database.commit();

    ORidBag ridBag = new ORidBag(database);

    ridBag.add(docA);
    ridBag.add(docB);

    database.begin();
    docA.save(database.getClusterNameById(database.getDefaultClusterId()));
    database.commit();

    ridBag.remove(docB);

    assertEmbedded(ridBag.isEmbedded());

    HashSet<OIdentifiable> result = new HashSet<OIdentifiable>();

    for (OIdentifiable oIdentifiable : ridBag) {
      result.add(oIdentifiable);
    }

    Assert.assertTrue(result.contains(docA));
    Assert.assertFalse(result.contains(docB));
    Assert.assertEquals(1, result.size());
    Assert.assertEquals(1, ridBag.size());
  }

  public void testMassiveChanges() {
    ODocument document = new ODocument();
    ORidBag bag = new ORidBag(database);
    assertEmbedded(bag.isEmbedded());

    final long seed = System.nanoTime();
    System.out.println("testMassiveChanges seed: " + seed);

    Random random = new Random(seed);
    List<OIdentifiable> rids = new ArrayList<OIdentifiable>();
    document.field("bag", bag);

    database.begin();
    document.save(database.getClusterNameById(database.getDefaultClusterId()));
    database.commit();

    ORID rid = document.getIdentity();

    for (int i = 0; i < 10; i++) {
      document = database.load(rid);
      document.setLazyLoad(false);

      bag = document.field("bag");
      assertEmbedded(bag.isEmbedded());

      massiveInsertionIteration(random, rids, bag);
      assertEmbedded(bag.isEmbedded());

      database.begin();
      document.save();
      database.commit();
    }

    database.begin();
    database.bindToSession(document).delete();
    database.commit();
  }

  public void testSimultaneousIterationAndRemove() {
    database.begin();
    ORidBag ridBag = new ORidBag(database);
    ODocument document = new ODocument();
    document.field("ridBag", ridBag);
    assertEmbedded(ridBag.isEmbedded());

    for (int i = 0; i < 10; i++) {
      ODocument docToAdd = new ODocument();
      docToAdd.save();
      ridBag.add(docToAdd);
    }
    document.save();
    database.commit();

    database.begin();
    assertEmbedded(ridBag.isEmbedded());
    database.commit();

    database.begin();
    document = database.bindToSession(document);
    ridBag = document.field("ridBag");

    Set<OIdentifiable> docs = Collections.newSetFromMap(new IdentityHashMap<>());
    for (OIdentifiable id : ridBag) {
      // cache record inside session
      docs.add(id.getRecord());
    }

    ridBag = document.field("ridBag");
    assertEmbedded(ridBag.isEmbedded());

    for (int i = 0; i < 10; i++) {
      ODocument docToAdd = new ODocument();

      database.begin();
      docToAdd.save(database.getClusterNameById(database.getDefaultClusterId()));
      database.commit();

      docs.add(docToAdd);
      ridBag.add(docToAdd);
    }

    assertEmbedded(ridBag.isEmbedded());

    for (int i = 0; i < 10; i++) {
      ODocument docToAdd = new ODocument();

      database.begin();
      docToAdd.save(database.getClusterNameById(database.getDefaultClusterId()));
      database.commit();

      docs.add(docToAdd);
      ridBag.add(docToAdd);
    }

    assertEmbedded(ridBag.isEmbedded());
    for (OIdentifiable identifiable : ridBag) {
      Assert.assertTrue(docs.remove(identifiable.getRecord()));
      ridBag.remove(identifiable);
      Assert.assertEquals(ridBag.size(), docs.size());

      int counter = 0;
      for (OIdentifiable id : ridBag) {
        Assert.assertTrue(docs.contains(id.getRecord()));
        counter++;
      }

      Assert.assertEquals(counter, docs.size());
      assertEmbedded(ridBag.isEmbedded());
    }

    document.save();
    database.commit();

    database.begin();
    document = database.bindToSession(document);
    ridBag = document.field("ridBag");
    Assert.assertEquals(ridBag.size(), 0);
    Assert.assertEquals(docs.size(), 0);
    database.rollback();
  }

  public void testAddMixedValues() {
    database.begin();
    ORidBag ridBag = new ORidBag(database);
    ODocument document = new ODocument();
    document.field("ridBag", ridBag);
    assertEmbedded(ridBag.isEmbedded());

    List<OIdentifiable> itemsToAdd = new ArrayList<>();

    for (int i = 0; i < 10; i++) {
      ODocument docToAdd = new ODocument();
      ridBag = document.field("ridBag");
      docToAdd.save(database.getClusterNameById(database.getDefaultClusterId()));

      for (int k = 0; k < 2; k++) {
        ridBag.add(docToAdd);
        itemsToAdd.add(docToAdd);
      }
      document.save();
    }
    database.commit();

    assertEmbedded(ridBag.isEmbedded());

    for (int i = 0; i < 10; i++) {
      database.begin();
      ODocument docToAdd = new ODocument();

      docToAdd.save(database.getClusterNameById(database.getDefaultClusterId()));

      document = database.bindToSession(document);
      ridBag = document.field("ridBag");
      for (int k = 0; k < 2; k++) {
        ridBag.add(docToAdd);
        itemsToAdd.add(docToAdd);
      }
      document.save();

      database.commit();
    }

    for (int i = 0; i < 10; i++) {
      database.begin();
      ODocument docToAdd = new ODocument();
      docToAdd.save(database.getClusterNameById(database.getDefaultClusterId()));

      document = database.bindToSession(document);
      ridBag = document.field("ridBag");
      ridBag.add(docToAdd);
      itemsToAdd.add(docToAdd);

      document.save();
      database.commit();
    }

    assertEmbedded(ridBag.isEmbedded());

    database.begin();
    document = database.bindToSession(document);
    ridBag = document.field("ridBag");
    for (int i = 0; i < 10; i++) {
      ODocument docToAdd = new ODocument();
      docToAdd.save(database.getClusterNameById(database.getDefaultClusterId()));

      for (int k = 0; k < 2; k++) {
        ridBag.add(docToAdd);
        itemsToAdd.add(docToAdd);
      }
    }
    for (int i = 0; i < 10; i++) {
      ODocument docToAdd = new ODocument();
      docToAdd.save(database.getClusterNameById(database.getDefaultClusterId()));
      ridBag.add(docToAdd);
      itemsToAdd.add(docToAdd);
    }

    assertEmbedded(ridBag.isEmbedded());
    document.save();

    database.commit();

    database.begin();
    document = database.bindToSession(document);
    ridBag = document.field("ridBag");
    assertEmbedded(ridBag.isEmbedded());

    Assert.assertEquals(ridBag.size(), itemsToAdd.size());

    Assert.assertEquals(ridBag.size(), itemsToAdd.size());

    for (OIdentifiable id : ridBag) {
      Assert.assertTrue(itemsToAdd.remove(id));
    }

    Assert.assertTrue(itemsToAdd.isEmpty());
    database.rollback();
  }

  public void testFromEmbeddedToSBTreeAndBack() throws IOException {
    OGlobalConfiguration.RID_BAG_EMBEDDED_TO_SBTREEBONSAI_THRESHOLD.setValue(7);
    OGlobalConfiguration.RID_BAG_SBTREEBONSAI_TO_EMBEDDED_THRESHOLD.setValue(-1);

    if (database.getStorage() instanceof OStorageProxy) {
      OServerAdmin server = new OServerAdmin(database.getURL()).connect("root", SERVER_PASSWORD);
      server.setGlobalConfiguration(
          OGlobalConfiguration.RID_BAG_EMBEDDED_TO_SBTREEBONSAI_THRESHOLD, 7);
      server.setGlobalConfiguration(
          OGlobalConfiguration.RID_BAG_SBTREEBONSAI_TO_EMBEDDED_THRESHOLD, -1);
      server.close();
    }

    database.begin();
    ORidBag ridBag = new ORidBag(database);
    ODocument document = new ODocument();
    document.field("ridBag", ridBag);

    Assert.assertTrue(ridBag.isEmbedded());

    document.save(database.getClusterNameById(database.getDefaultClusterId()));
    database.commit();

    database.begin();
    document = database.bindToSession(document);
    ridBag = document.field("ridBag");
    Assert.assertTrue(ridBag.isEmbedded());

    List<OIdentifiable> addedItems = new ArrayList<>();
    for (int i = 0; i < 6; i++) {
      ODocument docToAdd = new ODocument();

      database.begin();
      docToAdd.save(database.getClusterNameById(database.getDefaultClusterId()));
      database.commit();

      ridBag = document.field("ridBag");
      ridBag.add(docToAdd);
      addedItems.add(docToAdd);
    }

    document.save();
    database.commit();

    database.begin();
    document = database.bindToSession(document);
    ridBag = document.field("ridBag");
    Assert.assertTrue(ridBag.isEmbedded());
    database.rollback();

    database.begin();
    ODocument docToAdd = new ODocument();
    docToAdd.save(database.getClusterNameById(database.getDefaultClusterId()));
    database.commit();
    database.begin();

    docToAdd = database.bindToSession(docToAdd);
    document = database.bindToSession(document);
    ridBag = document.field("ridBag");
    ridBag.add(docToAdd);
    addedItems.add(docToAdd);

    document.save();
    database.commit();

    document = database.bindToSession(document);
    ridBag = document.field("ridBag");
    Assert.assertFalse(ridBag.isEmbedded());

    List<OIdentifiable> addedItemsCopy = new ArrayList<>(addedItems);
    for (OIdentifiable id : ridBag) {
      Assert.assertTrue(addedItems.remove(id));
    }

    Assert.assertTrue(addedItems.isEmpty());

    database.begin();
    document = database.bindToSession(document);
    ridBag = document.field("ridBag");
    Assert.assertFalse(ridBag.isEmbedded());

    addedItems.addAll(addedItemsCopy);
    for (OIdentifiable id : ridBag) {
      Assert.assertTrue(addedItems.remove(id));
    }

    Assert.assertTrue(addedItems.isEmpty());

    addedItems.addAll(addedItemsCopy);

    for (int i = 0; i < 3; i++) {
      ridBag.remove(addedItems.remove(i));
    }

    addedItemsCopy.clear();
    addedItemsCopy.addAll(addedItems);

    document.save();
    database.commit();

    database.begin();
    document = database.bindToSession(document);
    ridBag = document.field("ridBag");
    Assert.assertFalse(ridBag.isEmbedded());

    for (OIdentifiable id : ridBag) {
      Assert.assertTrue(addedItems.remove(id));
    }

    Assert.assertTrue(addedItems.isEmpty());

    ridBag = document.field("ridBag");
    Assert.assertFalse(ridBag.isEmbedded());

    addedItems.addAll(addedItemsCopy);
    for (OIdentifiable id : ridBag) {
      Assert.assertTrue(addedItems.remove(id));
    }

    Assert.assertTrue(addedItems.isEmpty());
    database.rollback();
  }

  public void testFromEmbeddedToSBTreeAndBackTx() throws IOException {
    OGlobalConfiguration.RID_BAG_EMBEDDED_TO_SBTREEBONSAI_THRESHOLD.setValue(7);
    OGlobalConfiguration.RID_BAG_SBTREEBONSAI_TO_EMBEDDED_THRESHOLD.setValue(-1);

    if (database.isRemote()) {
      OServerAdmin server = new OServerAdmin(database.getURL()).connect("root", SERVER_PASSWORD);
      server.setGlobalConfiguration(
          OGlobalConfiguration.RID_BAG_EMBEDDED_TO_SBTREEBONSAI_THRESHOLD, 7);
      server.setGlobalConfiguration(
          OGlobalConfiguration.RID_BAG_SBTREEBONSAI_TO_EMBEDDED_THRESHOLD, -1);
      server.close();
    }

    ORidBag ridBag = new ORidBag(database);
    ODocument document = new ODocument();
    document.field("ridBag", ridBag);

    Assert.assertTrue(ridBag.isEmbedded());
    database.begin();
    document.save(database.getClusterNameById(database.getDefaultClusterId()));
    database.commit();

    database.begin();
    document = database.bindToSession(document);
    ridBag = document.field("ridBag");
    Assert.assertTrue(ridBag.isEmbedded());

    List<OIdentifiable> addedItems = new ArrayList<OIdentifiable>();

    ridBag = document.field("ridBag");
    for (int i = 0; i < 6; i++) {

      ODocument docToAdd = new ODocument();

      docToAdd.save(database.getClusterNameById(database.getDefaultClusterId()));
      ridBag.add(docToAdd);
      addedItems.add(docToAdd);
    }
    document.save();
    database.commit();

    database.begin();
    document = database.bindToSession(document);
    ridBag = document.field("ridBag");
    Assert.assertTrue(ridBag.isEmbedded());

    ODocument docToAdd = new ODocument();

    docToAdd.save(database.getClusterNameById(database.getDefaultClusterId()));
    database.commit();

    database.begin();
    document = database.bindToSession(document);
    docToAdd = database.bindToSession(docToAdd);

    ridBag = document.field("ridBag");
    ridBag.add(docToAdd);
    addedItems.add(docToAdd);

    document.save();
    database.commit();

    database.begin();
    document = database.bindToSession(document);
    ridBag = document.field("ridBag");
    Assert.assertFalse(ridBag.isEmbedded());

    List<OIdentifiable> addedItemsCopy = new ArrayList<OIdentifiable>(addedItems);
    for (OIdentifiable id : ridBag) {
      Assert.assertTrue(addedItems.remove(id));
    }

    Assert.assertTrue(addedItems.isEmpty());

    ridBag = document.field("ridBag");
    Assert.assertFalse(ridBag.isEmbedded());

    addedItems.addAll(addedItemsCopy);
    for (OIdentifiable id : ridBag) {
      Assert.assertTrue(addedItems.remove(id));
    }

    Assert.assertTrue(addedItems.isEmpty());

    addedItems.addAll(addedItemsCopy);

    for (int i = 0; i < 3; i++) {
      ridBag.remove(addedItems.remove(i));
    }

    addedItemsCopy.clear();
    addedItemsCopy.addAll(addedItems);

    document.save();
    database.commit();

    database.begin();
    document = database.bindToSession(document);
    ridBag = document.field("ridBag");
    Assert.assertFalse(ridBag.isEmbedded());

    for (OIdentifiable id : ridBag) {
      Assert.assertTrue(addedItems.remove(id));
    }

    Assert.assertTrue(addedItems.isEmpty());

    ridBag = document.field("ridBag");
    Assert.assertFalse(ridBag.isEmbedded());

    addedItems.addAll(addedItemsCopy);
    for (OIdentifiable id : ridBag) {
      Assert.assertTrue(addedItems.remove(id));
    }

    Assert.assertTrue(addedItems.isEmpty());
    database.rollback();
  }

  public void testRemoveSavedInCommit() {
    database.begin();
    List<OIdentifiable> docsToAdd = new ArrayList<OIdentifiable>();

    ORidBag ridBag = new ORidBag(database);
    ODocument document = new ODocument();
    document.field("ridBag", ridBag);

    for (int i = 0; i < 5; i++) {
      ODocument docToAdd = new ODocument();
      docToAdd.save(database.getClusterNameById(database.getDefaultClusterId()));
      ridBag = document.field("ridBag");
      ridBag.add(docToAdd);

      docsToAdd.add(docToAdd);
    }

    document.save(database.getClusterNameById(database.getDefaultClusterId()));
    database.commit();

    database.begin();
    document = database.bindToSession(document);
    ridBag = document.field("ridBag");
    assertEmbedded(ridBag.isEmbedded());

    ridBag = document.field("ridBag");
    assertEmbedded(ridBag.isEmbedded());

    ridBag = document.field("ridBag");
    for (int i = 0; i < 5; i++) {
      ODocument docToAdd = new ODocument();
      docToAdd.save(database.getClusterNameById(database.getDefaultClusterId()));
      ridBag.add(docToAdd);

      docsToAdd.add(docToAdd);
    }

    for (int i = 5; i < 10; i++) {
      ODocument docToAdd = docsToAdd.get(i).getRecord();
      docToAdd.save(database.getClusterNameById(database.getDefaultClusterId()));
    }

    Iterator<OIdentifiable> iterator = docsToAdd.listIterator(7);
    while (iterator.hasNext()) {
      OIdentifiable docToAdd = iterator.next();
      ridBag.remove(docToAdd);
      iterator.remove();
    }

    document.save();
    database.commit();

    database.begin();
    document = database.bindToSession(document);
    ridBag = document.field("ridBag");
    assertEmbedded(ridBag.isEmbedded());

    List<OIdentifiable> docsToAddCopy = new ArrayList<OIdentifiable>(docsToAdd);
    for (OIdentifiable id : ridBag) {
      Assert.assertTrue(docsToAdd.remove(id));
    }

    Assert.assertTrue(docsToAdd.isEmpty());

    docsToAdd.addAll(docsToAddCopy);

    ridBag = document.field("ridBag");

    for (OIdentifiable id : ridBag) {
      Assert.assertTrue(docsToAdd.remove(id));
    }

    Assert.assertTrue(docsToAdd.isEmpty());
    database.rollback();
  }

  @Test
  public void testSizeNotChangeAfterRemoveNotExistentElement() {
    final ODocument bob = new ODocument();

    database.begin();
    final ODocument fred = new ODocument();
    fred.save(database.getClusterNameById(database.getDefaultClusterId()));
    final ODocument jim =
        new ODocument();
    jim.save(database.getClusterNameById(database.getDefaultClusterId()));
    database.commit();

    ORidBag teamMates = new ORidBag(database);

    teamMates.add(bob);
    teamMates.add(fred);

    Assert.assertEquals(teamMates.size(), 2);

    teamMates.remove(jim);

    Assert.assertEquals(teamMates.size(), 2);
  }

  @Test
  public void testRemoveNotExistentElementAndAddIt() throws Exception {
    ORidBag teamMates = new ORidBag(database);

    database.begin();
    final ODocument bob = new ODocument();
    bob.save(database.getClusterNameById(database.getDefaultClusterId()));
    database.commit();

    teamMates.remove(bob);

    Assert.assertEquals(teamMates.size(), 0);

    teamMates.add(bob);

    Assert.assertEquals(teamMates.size(), 1);
    Assert.assertEquals(teamMates.iterator().next().getIdentity(), bob.getIdentity());
  }

  public void testAddNewItemsAndRemoveThem() {
    database.begin();
    final List<OIdentifiable> rids = new ArrayList<OIdentifiable>();
    ORidBag ridBag = new ORidBag(database);
    int size = 0;
    for (int i = 0; i < 10; i++) {
      ODocument docToAdd = new ODocument();
      docToAdd.save(database.getClusterNameById(database.getDefaultClusterId()));

      for (int k = 0; k < 2; k++) {
        ridBag.add(docToAdd);
        rids.add(docToAdd);
        size++;
      }
    }

    Assert.assertEquals(ridBag.size(), size);
    ODocument document = new ODocument();
    document.field("ridBag", ridBag);
    document.save(database.getClusterNameById(database.getDefaultClusterId()));
    database.commit();

    database.begin();
    document = database.load(document.getIdentity());
    ridBag = document.field("ridBag");
    Assert.assertEquals(ridBag.size(), size);

    final List<OIdentifiable> newDocs = new ArrayList<OIdentifiable>();
    for (int i = 0; i < 10; i++) {
      ODocument docToAdd = new ODocument();

      docToAdd.save(database.getClusterNameById(database.getDefaultClusterId()));
      for (int k = 0; k < 2; k++) {
        ridBag.add(docToAdd);
        rids.add(docToAdd);
        newDocs.add(docToAdd);
        size++;
      }
    }
    document.save();
    database.commit();

    document = database.bindToSession(document);
    ridBag = document.field("ridBag");
    Assert.assertEquals(ridBag.size(), size);

    Random rnd = new Random();

    for (int i = 0; i < newDocs.size(); i++) {
      if (rnd.nextBoolean()) {
        OIdentifiable newDoc = newDocs.get(i);
        rids.remove(newDoc);
        ridBag.remove(newDoc);
        newDocs.remove(newDoc);

        size--;
      }
    }

    for (OIdentifiable identifiable : ridBag) {
      if (newDocs.contains(identifiable) && rnd.nextBoolean()) {
        ridBag.remove(identifiable);
        rids.remove(identifiable);

        size--;
      }
    }

    Assert.assertEquals(ridBag.size(), size);
    List<OIdentifiable> ridsCopy = new ArrayList<OIdentifiable>(rids);

    for (OIdentifiable identifiable : ridBag) {
      Assert.assertTrue(rids.remove(identifiable));
    }

    Assert.assertTrue(rids.isEmpty());

    database.begin();
    document.save();
    database.commit();

    document = database.load(document.getIdentity(), "*:-1", false);
    ridBag = document.field("ridBag");

    rids.addAll(ridsCopy);
    for (OIdentifiable identifiable : ridBag) {
      Assert.assertTrue(rids.remove(identifiable));
    }

    Assert.assertTrue(rids.isEmpty());
    Assert.assertEquals(ridBag.size(), size);
  }

  @Test
  public void testJsonSerialization() {
    database.begin();
    final ODocument externalDoc = new ODocument();

    final ORidBag highLevelRidBag = new ORidBag(database);

    for (int i = 0; i < 10; i++) {
      var doc = new ODocument();
      doc.save(database.getClusterNameById(database.getDefaultClusterId()));

      highLevelRidBag.add(doc);
    }

    externalDoc.save(database.getClusterNameById(database.getDefaultClusterId()));

    ODocument testDocument = new ODocument();
    testDocument.field("type", "testDocument");
    testDocument.field("ridBag", highLevelRidBag);
    testDocument.field("externalDoc", externalDoc);

    testDocument.save(database.getClusterNameById(database.getDefaultClusterId()));
    testDocument.save(database.getClusterNameById(database.getDefaultClusterId()));
    database.commit();

    database.begin();

    testDocument = database.bindToSession(testDocument);
    final String json = testDocument.toJSON(ORecordAbstract.OLD_FORMAT_WITH_LATE_TYPES);

    final ODocument doc = new ODocument();
    doc.fromJSON(json);

    Assert.assertTrue(
        ODocumentHelper.hasSameContentOf(doc, database, testDocument, database, null));
    database.rollback();
  }

  protected abstract void assertEmbedded(boolean isEmbedded);

  private static void massiveInsertionIteration(Random rnd, List<OIdentifiable> rids, ORidBag bag) {
    Iterator<OIdentifiable> bagIterator = bag.iterator();

    while (bagIterator.hasNext()) {
      OIdentifiable bagValue = bagIterator.next();
      Assert.assertTrue(rids.contains(bagValue));
    }

    Assert.assertEquals(bag.size(), rids.size());

    for (int i = 0; i < 100; i++) {
      if (rnd.nextDouble() < 0.2 & rids.size() > 5) {
        final int index = rnd.nextInt(rids.size());
        final OIdentifiable rid = rids.remove(index);
        bag.remove(rid);
      } else {
        final long position;
        position = rnd.nextInt(300);

        final ORecordId recordId = new ORecordId(1, position);
        rids.add(recordId);
        bag.add(recordId);
      }
    }

    bagIterator = bag.iterator();

    while (bagIterator.hasNext()) {
      final OIdentifiable bagValue = bagIterator.next();
      Assert.assertTrue(rids.contains(bagValue));

      if (rnd.nextDouble() < 0.05) {
        bagIterator.remove();
        Assert.assertTrue(rids.remove(bagValue));
      }
    }

    Assert.assertEquals(bag.size(), rids.size());
    bagIterator = bag.iterator();

    while (bagIterator.hasNext()) {
      final OIdentifiable bagValue = bagIterator.next();
      Assert.assertTrue(rids.contains(bagValue));
    }
  }
}
