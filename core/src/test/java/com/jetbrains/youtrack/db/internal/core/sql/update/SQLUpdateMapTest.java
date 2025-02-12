package com.jetbrains.youtrack.db.internal.core.sql.update;

import static org.junit.Assert.assertEquals;

import com.jetbrains.youtrack.db.internal.DbTestBase;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import java.util.Map;
import org.junit.Test;

public class SQLUpdateMapTest extends DbTestBase {

  @Test
  public void testMapPut() {

    EntityImpl ret;
    EntityImpl ret1;
    session.command("create class vRecord").close();
    session.command("create property vRecord.attrs EMBEDDEDMAP ").close();

    session.begin();
    try (var rs = session.command("insert into vRecord (title) values('first record')")) {
      ret = (EntityImpl) rs.next().getRecord().get();
    }

    try (var rs = session.command("insert into vRecord (title) values('second record')")) {
      ret1 = (EntityImpl) rs.next().getRecord().get();
    }
    session.commit();

    session.begin();
    session.command(
            "update " + ret.getIdentity() + " set attrs =  {'test1':" + ret1.getIdentity() + " }")
        .close();
    session.commit();
    reOpen("admin", "adminpwd");

    session.begin();
    session.command("update " + ret.getIdentity() + " set attrs['test'] = 'test value' ").close();
    session.commit();

    ret = session.bindToSession(ret);
    assertEquals(2, ((Map) ret.field("attrs")).size());
    assertEquals("test value", ((Map) ret.field("attrs")).get("test"));
    assertEquals(ret1.getIdentity(), ((Map) ret.field("attrs")).get("test1"));
  }
}
