/*
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.orientechnologies.orient.test.database.auto;

import com.orientechnologies.orient.core.command.script.OCommandScript;
import com.orientechnologies.orient.core.db.ODatabaseType;
import com.orientechnologies.orient.core.db.record.YTIdentifiable;
import com.orientechnologies.orient.core.metadata.schema.YTSchema;
import com.orientechnologies.orient.core.metadata.schema.YTType;
import com.orientechnologies.orient.core.record.impl.YTEntityImpl;
import com.orientechnologies.orient.core.storage.cache.local.OWOWCache;
import com.orientechnologies.orient.core.storage.cluster.OClusterPositionMap;
import com.orientechnologies.orient.core.storage.cluster.OPaginatedCluster;
import com.orientechnologies.orient.core.storage.disk.OLocalPaginatedStorage;
import java.io.File;
import java.util.Collection;
import java.util.Locale;
import org.testng.Assert;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

@Test
public class SQLCommandsTest extends DocumentDBBaseTest {

  @Parameters(value = "remote")
  public SQLCommandsTest(@Optional Boolean remote) {
    super(remote != null && remote);
  }

  public void createProperty() {
    YTSchema schema = database.getMetadata().getSchema();
    if (!schema.existsClass("account")) {
      schema.createClass("account");
    }

    database.command("create property account.timesheet string").close();

    Assert.assertEquals(
        database.getMetadata().getSchema().getClass("account").getProperty("timesheet").getType(),
        YTType.STRING);
  }

  @Test(dependsOnMethods = "createProperty")
  public void createLinkedClassProperty() {
    database.command("create property account.knows embeddedmap account").close();

    Assert.assertEquals(
        database.getMetadata().getSchema().getClass("account").getProperty("knows").getType(),
        YTType.EMBEDDEDMAP);
    Assert.assertEquals(
        database
            .getMetadata()
            .getSchema()
            .getClass("account")
            .getProperty("knows")
            .getLinkedClass(),
        database.getMetadata().getSchema().getClass("account"));
  }

  @Test(dependsOnMethods = "createLinkedClassProperty")
  public void createLinkedTypeProperty() {
    database.command("create property account.tags embeddedlist string").close();

    Assert.assertEquals(
        database.getMetadata().getSchema().getClass("account").getProperty("tags").getType(),
        YTType.EMBEDDEDLIST);
    Assert.assertEquals(
        database.getMetadata().getSchema().getClass("account").getProperty("tags").getLinkedType(),
        YTType.STRING);
  }

  @Test(dependsOnMethods = "createLinkedTypeProperty")
  public void removeProperty() {
    database.command("drop property account.timesheet").close();
    database.command("drop property account.tags").close();

    Assert.assertFalse(
        database.getMetadata().getSchema().getClass("account").existsProperty("timesheet"));
    Assert.assertFalse(
        database.getMetadata().getSchema().getClass("account").existsProperty("tags"));
  }

  @Test(dependsOnMethods = "removeProperty")
  public void testSQLScript() {
    String cmd = "";
    cmd += "select from ouser limit 1;begin;";
    cmd += "let a = create vertex set script = true\n";
    cmd += "let b = select from v limit 1;";
    cmd += "create edge from $a to $b;";
    cmd += "commit;";
    cmd += "return $a;";

    Object result = database.command(new OCommandScript("sql", cmd)).execute(database);

    Assert.assertTrue(result instanceof YTIdentifiable);
    Assert.assertTrue(((YTIdentifiable) result).getRecord() instanceof YTEntityImpl);
    Assert.assertTrue(
        database.bindToSession((YTEntityImpl) ((YTIdentifiable) result).getRecord())
            .field("script"));
  }

  public void testClusterRename() {
    if (database.getURL().startsWith("memory:")) {
      return;
    }

    Collection<String> names = database.getClusterNames();
    Assert.assertFalse(names.contains("testClusterRename".toLowerCase(Locale.ENGLISH)));

    database.command("create cluster testClusterRename").close();

    names = database.getClusterNames();
    Assert.assertTrue(names.contains("testClusterRename".toLowerCase(Locale.ENGLISH)));

    database.command("alter cluster testClusterRename name testClusterRename42").close();
    names = database.getClusterNames();

    Assert.assertTrue(names.contains("testClusterRename42".toLowerCase(Locale.ENGLISH)));
    Assert.assertFalse(names.contains("testClusterRename".toLowerCase(Locale.ENGLISH)));

    if (!remoteDB && databaseType.equals(ODatabaseType.PLOCAL)) {
      String storagePath = database.getStorage().getConfiguration().getDirectory();

      final OWOWCache wowCache =
          (OWOWCache) ((OLocalPaginatedStorage) database.getStorage()).getWriteCache();

      File dataFile =
          new File(
              storagePath,
              wowCache.nativeFileNameById(
                  wowCache.fileIdByName("testClusterRename42" + OPaginatedCluster.DEF_EXTENSION)));
      File mapFile =
          new File(
              storagePath,
              wowCache.nativeFileNameById(
                  wowCache.fileIdByName(
                      "testClusterRename42" + OClusterPositionMap.DEF_EXTENSION)));

      Assert.assertTrue(dataFile.exists());
      Assert.assertTrue(mapFile.exists());
    }
  }
}
