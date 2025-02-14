package com.jetbrains.youtrack.db.internal.core.record.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.jetbrains.youtrack.db.internal.DbTestBase;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.internal.core.db.record.LinkList;
import com.jetbrains.youtrack.db.internal.core.db.record.LinkMap;
import com.jetbrains.youtrack.db.internal.core.db.record.LinkSet;
import com.jetbrains.youtrack.db.api.schema.PropertyType;
import java.util.Set;
import org.apache.commons.collections4.SetUtils;
import org.junit.Test;

public class CollectionOfLinkInNestedDocumentTest extends DbTestBase {

  @Test
  public void nestedLinkSet() {
    EntityImpl doc1 = new EntityImpl();
    doc1.field("value", "item 1");
    EntityImpl doc2 = new EntityImpl();
    doc2.field("value", "item 2");
    EntityImpl nested = new EntityImpl();
    LinkSet set = new LinkSet(nested);
    set.add(doc1);
    set.add(doc2);

    nested.field("set", set);

    db.begin();
    EntityImpl base = new EntityImpl();
    base.field("nested", nested, PropertyType.EMBEDDED);
    Identifiable id = db.save(base);
    db.commit();

    EntityImpl base1 = db.load(id.getIdentity());
    EntityImpl nest1 = base1.field("nested");
    assertNotNull(nest1);

    assertEquals(SetUtils.hashSet(doc1.getIdentity(), doc2.getIdentity()),
        nest1.<Set<Identifiable>>field("set"));
  }

  @Test
  public void nestedLinkList() {
    EntityImpl doc1 = new EntityImpl();
    doc1.field("value", "item 1");
    EntityImpl doc2 = new EntityImpl();
    doc2.field("value", "item 2");
    EntityImpl nested = new EntityImpl();
    LinkList list = new LinkList(nested);
    list.add(doc1);
    list.add(doc2);

    nested.field("list", list);

    db.begin();
    EntityImpl base = new EntityImpl();
    base.field("nested", nested, PropertyType.EMBEDDED);
    Identifiable id = db.save(base, db.getClusterNameById(db.getDefaultClusterId()));
    db.commit();

    EntityImpl base1 = db.load(id.getIdentity());
    EntityImpl nest1 = base1.field("nested");
    assertNotNull(nest1);
    assertEquals(list, nest1.field("list"));
  }

  @Test
  public void nestedLinkMap() {
    EntityImpl doc1 = new EntityImpl();
    doc1.field("value", "item 1");
    EntityImpl doc2 = new EntityImpl();
    doc2.field("value", "item 2");
    EntityImpl nested = new EntityImpl();
    LinkMap map = new LinkMap(nested);
    map.put("record1", doc1);
    map.put("record2", doc2);

    nested.field("map", map);

    db.begin();
    EntityImpl base = new EntityImpl();
    base.field("nested", nested, PropertyType.EMBEDDED);
    Identifiable id = db.save(base, db.getClusterNameById(db.getDefaultClusterId()));
    db.commit();

    EntityImpl base1 = db.load(id.getIdentity());
    EntityImpl nest1 = base1.field("nested");
    assertNotNull(nest1);
    assertEquals(map, nest1.field("map"));
  }
}
