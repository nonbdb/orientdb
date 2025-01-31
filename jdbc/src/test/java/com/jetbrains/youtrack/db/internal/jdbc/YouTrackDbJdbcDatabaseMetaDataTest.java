/**
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * <p>*
 */
package com.jetbrains.youtrack.db.internal.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.jetbrains.youtrack.db.internal.core.YouTrackDBConstants;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;

public class YouTrackDbJdbcDatabaseMetaDataTest extends YouTrackDbJdbcDbPerClassTemplateTest {

  private DatabaseMetaData metaData;

  @Before
  public void setup() throws SQLException {
    metaData = conn.getMetaData();
  }

  @Test
  public void verifyDriverAndDatabaseVersions() throws SQLException {
    assertEquals("admin", metaData.getUserName());
    assertEquals("YouTrackDB", metaData.getDatabaseProductName());
    assertEquals(YouTrackDBConstants.getVersion(), metaData.getDatabaseProductVersion());
    assertEquals(1, metaData.getDatabaseMajorVersion());
    assertEquals(0, metaData.getDatabaseMinorVersion());

    assertEquals("YouTrackDB JDBC Driver", metaData.getDriverName());
    assertEquals(
        "YouTrackDB " + YouTrackDBConstants.getVersion() + " JDBC Driver",
        metaData.getDriverVersion());
    assertEquals(1, metaData.getDriverMajorVersion());
    assertEquals(0, metaData.getDriverMinorVersion());
  }

  @Test
  public void shouldRetrievePrimaryKeysMetadata() throws SQLException {
    ResultSet primaryKeys = metaData.getPrimaryKeys(null, null, "Item");
    assertTrue(primaryKeys.next());
    assertEquals("intKey", primaryKeys.getString(4));
    assertEquals("Item.intKey", primaryKeys.getString(6));
    assertEquals(1, primaryKeys.getInt(5));

    assertTrue(primaryKeys.next());
    assertEquals("stringKey", primaryKeys.getString("COLUMN_NAME"));
    assertEquals("Item.stringKey", primaryKeys.getString("PK_NAME"));
    assertEquals(1, primaryKeys.getInt("KEY_SEQ"));
  }

  @Test
  public void shouldRetrieveTableTypes() throws SQLException {

    ResultSet tableTypes = metaData.getTableTypes();

    //    Assertions.
    assertTrue(tableTypes.next());
    assertEquals("TABLE", tableTypes.getString(1));
    assertTrue(tableTypes.next());
    assertEquals("SYSTEM TABLE", tableTypes.getString(1));

    assertFalse(tableTypes.next());
  }

  @Test
  public void shouldRetrieveKeywords() throws SQLException {

    final String keywordsStr = metaData.getSQLKeywords();
    assertNotNull(keywordsStr);
    assertThat(Arrays.asList(keywordsStr.toUpperCase(Locale.ENGLISH).split(",\\s*")))
        .contains("TRAVERSE");
  }

  @Test
  public void shouldRetrieveUniqueIndexInfoForTable() throws Exception {

    ResultSet indexInfo =
        metaData.getIndexInfo(
            "YouTrackDBJdbcDatabaseMetaDataTest",
            "YouTrackDBJdbcDatabaseMetaDataTest",
            "Item",
            true,
            false);

    indexInfo.next();

    assertThat(indexInfo.getString("INDEX_NAME")).isEqualTo("Item.intKey");
    assertThat(indexInfo.getBoolean("NON_UNIQUE")).isFalse();

    indexInfo.next();

    assertThat(indexInfo.getString("INDEX_NAME")).isEqualTo("Item.stringKey");
    assertThat(indexInfo.getBoolean("NON_UNIQUE")).isFalse();
  }

  @Test
  public void getFields() throws SQLException {
    ResultSet rs = conn.createStatement().executeQuery("select from OUser");

    ResultSetMetaData rsMetaData = rs.getMetaData();

    int cc = rsMetaData.getColumnCount();
    Set<String> colset = new HashSet<>();
    List<Map<String, Object>> columns = new ArrayList<>(cc);
    for (int i = 1; i <= cc; i++) {
      String name = rsMetaData.getColumnLabel(i);
      //      if (colset.contains(name))
      //        continue;
      colset.add(name);
      Map<String, Object> field = new HashMap<>();
      field.put("name", name);

      try {
        String catalog = rsMetaData.getCatalogName(i);
        String schema = rsMetaData.getSchemaName(i);
        String table = rsMetaData.getTableName(i);
        ResultSet rsmc = conn.getMetaData().getColumns(catalog, schema, table, name);
        while (rsmc.next()) {
          field.put("description", rsmc.getString("REMARKS"));
          break;
        }
      } catch (SQLException se) {
        se.printStackTrace();
      }
      columns.add(field);
    }

    for (Map<String, Object> c : columns) {
      System.out.println(c);
    }
  }

  @Test
  public void shouldFetchAllTables() throws SQLException {
    ResultSet rs = metaData.getTables(null, null, null, null);
    int tableCount = sizeOf(rs);

    var database = (DatabaseSessionInternal) conn.getDatabase();
    assertThat(tableCount)
        .isEqualTo(
            database.getMetadata().getSchema().getClasses(database)
                .size());
  }

  @Test
  public void shouldFillSchemaAndCatalogWithDatabaseName() throws SQLException {
    ResultSet rs = metaData.getTables(null, null, null, null);

    while (rs.next()) {
      assertThat(rs.getString("TABLE_SCHEM")).isEqualTo("perClassTestDatabase");
      assertThat(rs.getString("TABLE_CAT")).isEqualTo("perClassTestDatabase");
    }
  }

  @Test
  public void shouldGetAllTablesFilteredByAllTypes() throws SQLException {
    ResultSet rs = metaData.getTableTypes();
    List<String> tableTypes = new ArrayList<>(2);
    while (rs.next()) {
      tableTypes.add(rs.getString(1));
    }
    rs = metaData.getTables(null, null, null, tableTypes.toArray(new String[2]));
    int tableCount = sizeOf(rs);
    var database = (DatabaseSessionInternal) conn.getDatabase();

    assertThat(tableCount)
        .isEqualTo(
            database.getMetadata().getSchema().getClasses(database)
                .size());
  }

  @Test
  public void getNoTablesFilteredByEmptySetOfTypes() throws SQLException {
    final ResultSet rs = metaData.getTables(null, null, null, new String[0]);
    int tableCount = sizeOf(rs);

    assertThat(tableCount).isEqualTo(0);
  }

  @Test
  public void getSingleTable() throws SQLException {
    ResultSet rs = metaData.getTables(null, null, "ouser", null);
    rs.next();
    assertThat(rs.getString("TABLE_NAME")).isEqualTo("OUser");
    assertThat(rs.getString("TABLE_CAT")).isEqualTo("perClassTestDatabase");
    assertThat(rs.getString("TABLE_SCHEM")).isEqualTo("perClassTestDatabase");
    assertThat(rs.getString("REMARKS")).isNull();
    assertThat(rs.getString("REF_GENERATION")).isNull();
    assertThat(rs.getString("TYPE_NAME")).isNull();

    assertThat(rs.next()).isFalse();
  }

  @Test
  public void shouldGetSingleColumnOfArticle() throws SQLException {
    ResultSet rs = metaData.getColumns(null, null, "Article", "uuid");
    rs.next();

    assertThat(rs.getString("TABLE_NAME")).isEqualTo("Article");
    assertThat(rs.getString("COLUMN_NAME")).isEqualTo("uuid");
    assertThat(rs.getString("TYPE_NAME")).isEqualTo("LONG");
    assertThat(rs.getInt("DATA_TYPE")).isEqualTo(-5);

    assertThat(rs.next()).isFalse();
  }

  @Test
  public void shouldGetAllColumnsOfArticle() throws SQLException {
    ResultSet rs = metaData.getColumns(null, null, "Article", null);

    while (rs.next()) {
      assertThat(rs.getString("TABLE_NAME")).isEqualTo("Article");
      assertThat(rs.getString("COLUMN_NAME")).isIn("date", "uuid", "author", "title", "content");

      //      System.out.println("rs = " + rs.getInt("DATA_TYPE"));
      assertThat(rs.getInt("DATA_TYPE")).isIn(-5, 12, 91, 2000);
      assertThat(rs.getString("TYPE_NAME")).isIn("LONG", "LINK", "DATE", "STRING", "INTEGER");
    }
  }

  @Test
  public void shouldGetAllIndexesOnArticle() throws Exception {
    ResultSet rs = metaData.getIndexInfo(null, null, "Article", true, true);

    rs.next();

    assertThat(rs.getString("COLUMN_NAME")).isEqualTo("uuid");
    assertThat(rs.getString("INDEX_NAME")).isEqualTo("Article.uuid");
    assertThat(rs.getBoolean("NON_UNIQUE")).isFalse();
  }

  @Test
  public void shouldGetPrimaryKeyOfArticle() throws Exception {
    ResultSet rs = metaData.getPrimaryKeys(null, null, "Article");

    rs.next();
    assertThat(rs.getString("TABLE_NAME")).isEqualTo("Article");
    assertThat(rs.getString("COLUMN_NAME")).isEqualTo("uuid");
    assertThat(rs.getString("PK_NAME")).isEqualTo("Article.uuid");
    assertThat(rs.getInt("KEY_SEQ")).isEqualTo(1);
  }

  private int sizeOf(ResultSet rs) throws SQLException {
    int tableCount = 0;

    while (rs.next()) {
      tableCount++;
    }
    return tableCount;
  }
}
