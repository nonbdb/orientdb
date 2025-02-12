/*
 *
 *  *  Copyright YouTrackDB
 *  *
 *  *  Licensed under the Apache License, Version 2.0 (the "License");
 *  *  you may not use this file except in compliance with the License.
 *  *  You may obtain a copy of the License at
 *  *
 *  *       http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *  Unless required by applicable law or agreed to in writing, software
 *  *  distributed under the License is distributed on an "AS IS" BASIS,
 *  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  See the License for the specific language governing permissions and
 *  *  limitations under the License.
 *  *
 *
 *
 */
package com.jetbrains.youtrack.db.internal.core.sql;

import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.api.schema.Schema;
import com.jetbrains.youtrack.db.internal.DbTestBase;
import org.junit.Assert;
import org.junit.Test;

public class CommandExecutorSQLDropSchemaPropertyTest extends DbTestBase {

  @Test
  public void test() {
    Schema schema = session.getMetadata().getSchema();
    var foo = schema.createClass("Foo");

    foo.createProperty(session, "name", PropertyType.STRING);
    Assert.assertTrue(schema.getClass("Foo").existsProperty(session, "name"));
    session.command("DROP PROPERTY Foo.name").close();
    Assert.assertFalse(schema.getClass("Foo").existsProperty(session, "name"));

    foo.createProperty(session, "name", PropertyType.STRING);
    Assert.assertTrue(schema.getClass("Foo").existsProperty(session, "name"));
    session.command("DROP PROPERTY `Foo`.name").close();
    Assert.assertFalse(schema.getClass("Foo").existsProperty(session, "name"));

    foo.createProperty(session, "name", PropertyType.STRING);
    Assert.assertTrue(schema.getClass("Foo").existsProperty(session, "name"));
    session.command("DROP PROPERTY Foo.`name`").close();
    Assert.assertFalse(schema.getClass("Foo").existsProperty(session, "name"));

    foo.createProperty(session, "name", PropertyType.STRING);
    Assert.assertTrue(schema.getClass("Foo").existsProperty(session, "name"));
    session.command("DROP PROPERTY `Foo`.`name`").close();
    Assert.assertFalse(schema.getClass("Foo").existsProperty(session, "name"));
  }

  @Test
  public void testIfExists() {
    Schema schema = session.getMetadata().getSchema();
    var testIfExistsClass = schema.createClass("testIfExists");

    testIfExistsClass.createProperty(session, "name", PropertyType.STRING);
    Assert.assertTrue(schema.getClass("testIfExists").existsProperty(session, "name"));
    session.command("DROP PROPERTY testIfExists.name if exists").close();
    Assert.assertFalse(schema.getClass("testIfExists").existsProperty(session, "name"));

    session.command("DROP PROPERTY testIfExists.name if exists").close();
    Assert.assertFalse(schema.getClass("testIfExists").existsProperty(session, "name"));
  }
}
