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

import com.jetbrains.youtrack.db.api.record.Entity;
import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

@Test
public class CRUDInheritanceTest extends BaseDBTest {

  @Parameters(value = "remote")
  public CRUDInheritanceTest(@Optional Boolean remote) {
    super(remote != null && remote);
  }

  @BeforeClass
  @Override
  public void beforeClass() throws Exception {
    super.beforeClass();

    createCompanyClass();
  }

  @Test
  public void create() {
    db.command("delete from Company").close();

    generateCompanyData();
  }

  @Test(dependsOnMethods = "create")
  public void testCreate() {
    Assert.assertEquals(db.countClass("Company"), TOT_COMPANY_RECORDS);
  }

  @Test(dependsOnMethods = "testCreate")
  public void queryByBaseType() {
    var resultSet = executeQuery("select from Company where name.length() > 0");

    Assert.assertFalse(resultSet.isEmpty());
    Assert.assertEquals(resultSet.size(), TOT_COMPANY_RECORDS);

    var companyRecords = 0;
    Entity account;
    for (var entries : resultSet) {
      account = entries.asEntity();

      if ("Company".equals(account.getClassName())) {
        companyRecords++;
      }

      Assert.assertNotSame(account.<String>getProperty("name").length(), 0);
    }

    Assert.assertEquals(companyRecords, TOT_COMPANY_RECORDS);
  }

  @Test(dependsOnMethods = "queryByBaseType")
  public void queryPerSuperType() {
    var resultSet = executeQuery("select * from Company where name.length() > 0");

    Assert.assertEquals(resultSet.size(), TOT_COMPANY_RECORDS);

    Entity account;
    for (var entries : resultSet) {
      account = entries.asEntity();
      Assert.assertNotSame(account.<String>getProperty("name").length(), 0);
    }
  }

  @Test(dependsOnMethods = {"queryPerSuperType", "testCreate"})
  public void deleteFirst() {
    // DELETE ALL THE RECORD IN THE CLUSTER
    db.begin();
    var companyClusterIterator = db.browseClass("Company");
    for (Entity obj : companyClusterIterator) {
      if (obj.<Integer>getProperty("id") == 1) {
        db.delete(obj);
        break;
      }
    }
    db.commit();

    Assert.assertEquals(db.countClass("Company"), TOT_COMPANY_RECORDS - 1);
  }

  @Test(dependsOnMethods = "deleteFirst")
  public void testSuperclassInheritanceCreation() {
    db.close();
    db = createSessionInstance();

    createInheritanceTestClass();

    var abstractClass =
        db.getMetadata().getSchema().getClass("InheritanceTestAbstractClass");
    var baseClass = db.getMetadata().getSchema().getClass("InheritanceTestBaseClass");
    var testClass = db.getMetadata().getSchema().getClass("InheritanceTestClass");

    Assert.assertTrue(baseClass.getSuperClasses().contains(abstractClass));
    Assert.assertTrue(testClass.getSuperClasses().contains(baseClass));
  }

  @Test
  public void testIdFieldInheritanceFirstSubClass() {
    createInheritanceTestClass();

    var a = db.newInstance("InheritanceTestBaseClass");
    var b = db.newInstance("InheritanceTestClass");

    db.begin();
    db.save(a);
    db.save(b);
    db.commit();

    var resultSet = executeQuery("select from InheritanceTestBaseClass");
    Assert.assertEquals(resultSet.size(), 2);
  }

  @Test
  public void testKeywordClass() {
    var klass = db.getMetadata().getSchema().createClass("Not");

    var klass1 = db.getMetadata().getSchema().createClass("Extends_Not", klass);
    Assert.assertEquals(klass1.getSuperClasses().size(), 1, 1);
    Assert.assertEquals(klass1.getSuperClasses().getFirst().getName(), "Not");
  }

  @Test
  public void testSchemaGeneration() {
    var schema = db.getMetadata().getSchema();
    var testSchemaClass = schema.createClass("JavaTestSchemaGeneration");
    var childClass = schema.createClass("TestSchemaGenerationChild");

    testSchemaClass.createProperty(db, "text", PropertyType.STRING);
    testSchemaClass.createProperty(db, "enumeration", PropertyType.STRING);
    testSchemaClass.createProperty(db, "numberSimple", PropertyType.INTEGER);
    testSchemaClass.createProperty(db, "longSimple", PropertyType.LONG);
    testSchemaClass.createProperty(db, "doubleSimple", PropertyType.DOUBLE);
    testSchemaClass.createProperty(db, "floatSimple", PropertyType.FLOAT);
    testSchemaClass.createProperty(db, "byteSimple", PropertyType.BYTE);
    testSchemaClass.createProperty(db, "flagSimple", PropertyType.BOOLEAN);
    testSchemaClass.createProperty(db, "dateField", PropertyType.DATETIME);

    testSchemaClass.createProperty(db, "stringListMap", PropertyType.EMBEDDEDMAP,
        PropertyType.EMBEDDEDLIST);
    testSchemaClass.createProperty(db, "enumList", PropertyType.EMBEDDEDLIST,
        PropertyType.STRING);
    testSchemaClass.createProperty(db, "enumSet", PropertyType.EMBEDDEDSET,
        PropertyType.STRING);
    testSchemaClass.createProperty(db, "stringSet", PropertyType.EMBEDDEDSET,
        PropertyType.STRING);
    testSchemaClass.createProperty(db, "stringMap", PropertyType.EMBEDDEDMAP,
        PropertyType.STRING);

    testSchemaClass.createProperty(db, "list", PropertyType.LINKLIST, childClass);
    testSchemaClass.createProperty(db, "set", PropertyType.LINKSET, childClass);
    testSchemaClass.createProperty(db, "children", PropertyType.LINKMAP, childClass);
    testSchemaClass.createProperty(db, "child", PropertyType.LINK, childClass);

    testSchemaClass.createProperty(db, "embeddedSet", PropertyType.EMBEDDEDSET, childClass);
    testSchemaClass.createProperty(db, "embeddedChildren", PropertyType.EMBEDDEDMAP,
        childClass);
    testSchemaClass.createProperty(db, "embeddedChild", PropertyType.EMBEDDED, childClass);
    testSchemaClass.createProperty(db, "embeddedList", PropertyType.EMBEDDEDLIST, childClass);

    // Test simple types
    checkProperty(testSchemaClass, "text", PropertyType.STRING);
    checkProperty(testSchemaClass, "enumeration", PropertyType.STRING);
    checkProperty(testSchemaClass, "numberSimple", PropertyType.INTEGER);
    checkProperty(testSchemaClass, "longSimple", PropertyType.LONG);
    checkProperty(testSchemaClass, "doubleSimple", PropertyType.DOUBLE);
    checkProperty(testSchemaClass, "floatSimple", PropertyType.FLOAT);
    checkProperty(testSchemaClass, "byteSimple", PropertyType.BYTE);
    checkProperty(testSchemaClass, "flagSimple", PropertyType.BOOLEAN);
    checkProperty(testSchemaClass, "dateField", PropertyType.DATETIME);

    // Test complex types
    checkProperty(testSchemaClass, "stringListMap", PropertyType.EMBEDDEDMAP,
        PropertyType.EMBEDDEDLIST);
    checkProperty(testSchemaClass, "enumList", PropertyType.EMBEDDEDLIST, PropertyType.STRING);
    checkProperty(testSchemaClass, "enumSet", PropertyType.EMBEDDEDSET, PropertyType.STRING);
    checkProperty(testSchemaClass, "stringSet", PropertyType.EMBEDDEDSET, PropertyType.STRING);
    checkProperty(testSchemaClass, "stringMap", PropertyType.EMBEDDEDMAP, PropertyType.STRING);

    // Test linked types
    checkProperty(testSchemaClass, "list", PropertyType.LINKLIST, childClass);
    checkProperty(testSchemaClass, "set", PropertyType.LINKSET, childClass);
    checkProperty(testSchemaClass, "children", PropertyType.LINKMAP, childClass);
    checkProperty(testSchemaClass, "child", PropertyType.LINK, childClass);

    // Test embedded types
    checkProperty(testSchemaClass, "embeddedSet", PropertyType.EMBEDDEDSET, childClass);
    checkProperty(testSchemaClass, "embeddedChildren", PropertyType.EMBEDDEDMAP, childClass);
    checkProperty(testSchemaClass, "embeddedChild", PropertyType.EMBEDDED, childClass);
    checkProperty(testSchemaClass, "embeddedList", PropertyType.EMBEDDEDLIST, childClass);
  }

  protected static void checkProperty(SchemaClass iClass, String iPropertyName,
      PropertyType iType) {
    var prop = iClass.getProperty(iPropertyName);
    Assert.assertNotNull(prop);
    Assert.assertEquals(prop.getType(), iType);
  }

  protected static void checkProperty(
      SchemaClass iClass, String iPropertyName, PropertyType iType, SchemaClass iLinkedClass) {
    var prop = iClass.getProperty(iPropertyName);
    Assert.assertNotNull(prop);
    Assert.assertEquals(prop.getType(), iType);
    Assert.assertEquals(prop.getLinkedClass(), iLinkedClass);
  }

  protected static void checkProperty(
      SchemaClass iClass, String iPropertyName, PropertyType iType, PropertyType iLinkedType) {
    var prop = iClass.getProperty(iPropertyName);
    Assert.assertNotNull(prop);
    Assert.assertEquals(prop.getType(), iType);
    Assert.assertEquals(prop.getLinkedType(), iLinkedType);
  }
}
