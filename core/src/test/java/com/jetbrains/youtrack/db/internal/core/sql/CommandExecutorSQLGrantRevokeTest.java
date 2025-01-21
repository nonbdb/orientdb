/*
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *   *
 */

package com.jetbrains.youtrack.db.internal.core.sql;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.jetbrains.youtrack.db.internal.DbTestBase;
import com.jetbrains.youtrack.db.internal.core.metadata.security.Role;
import com.jetbrains.youtrack.db.internal.core.metadata.security.Rule;
import org.junit.Test;

/**
 *
 */
public class CommandExecutorSQLGrantRevokeTest extends DbTestBase {

  @Test
  public void grantServerRemove() {
    db.begin();
    Role testRole =
        db.getMetadata()
            .getSecurity()
            .createRole("testRole");

    assertFalse(testRole.allow(Rule.ResourceGeneric.SERVER, "server", Role.PERMISSION_EXECUTE));
    db.commit();

    db.begin();
    db.command("GRANT execute on server.remove to testRole").close();
    db.commit();

    testRole = db.getMetadata().getSecurity().getRole("testRole");

    assertTrue(testRole.allow(Rule.ResourceGeneric.SERVER, "remove", Role.PERMISSION_EXECUTE));

    db.begin();
    db.command("REVOKE execute on server.remove from testRole").close();
    db.commit();

    testRole = db.getMetadata().getSecurity().getRole("testRole");

    assertFalse(testRole.allow(Rule.ResourceGeneric.SERVER, "remove", Role.PERMISSION_EXECUTE));
  }
}
