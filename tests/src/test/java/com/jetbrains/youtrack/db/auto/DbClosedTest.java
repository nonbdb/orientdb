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
package com.jetbrains.youtrack.db.auto;

import com.jetbrains.youtrack.db.api.DatabaseSession;
import com.jetbrains.youtrack.db.api.config.GlobalConfiguration;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

@Test(groups = "db")
public class DbClosedTest extends BaseDBTest {

  @Parameters(value = {"remote"})
  public DbClosedTest(boolean remote) {
    super(remote, "db-closed-test");
  }

  public void testDoubleDb() {
    DatabaseSession db = acquireSession();

    // now I am getting another db instance
    DatabaseSession dbAnother = acquireSession();
    dbAnother.close();

    db.activateOnCurrentThread();
    db.close();
  }

  public void testDoubleDbWindowsPath() {
    DatabaseSession db = acquireSession();

    // now I am getting another db instance
    DatabaseSession dbAnother = acquireSession();
    dbAnother.close();

    db.activateOnCurrentThread();
    db.close();
  }

  @Test
  public void testRemoteConns() {
    if (remoteDB) {
      return;
    }

    final var max = GlobalConfiguration.NETWORK_MAX_CONCURRENT_SESSIONS.getValueAsInteger();
    for (var i = 0; i < max * 2; ++i) {
      final DatabaseSession db = acquireSession();
      db.close();
    }
  }
}
