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

import com.jetbrains.youtrack.db.internal.core.id.RecordId;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Types;
import java.util.Calendar;
import java.util.TimeZone;
import org.assertj.db.api.Assertions;
import org.assertj.db.type.Request;
import org.assertj.db.type.ValueType;
import org.junit.Test;

public class YouTrackDbJdbcResultSetMetaDataTest extends YouTrackDbJdbcDbPerClassTemplateTest {

  @Test
  public void shouldMapYouTrackDbTypesToJavaSQLTypes() throws Exception {

    var rs =
        conn.createStatement()
            .executeQuery("SELECT stringKey, intKey, text, length, date, score FROM Item");

    var metaData = rs.getMetaData();
    assertThat(metaData).isNotNull();
    assertThat(metaData.getColumnCount()).isEqualTo(6);

    assertThat(metaData.getColumnType(1)).isEqualTo(Types.VARCHAR);
    assertThat(metaData.getColumnClassName(1)).isEqualTo(String.class.getName());

    assertThat(metaData.getColumnType(2)).isEqualTo(Types.INTEGER);

    assertThat(metaData.getColumnType(3)).isEqualTo(Types.VARCHAR);
    assertThat(rs.getObject(3)).isInstanceOf(String.class);

    assertThat(metaData.getColumnType(4)).isEqualTo(Types.INTEGER);
    assertThat(metaData.getColumnType(5)).isEqualTo(Types.TIMESTAMP);

    assertThat(metaData.getColumnType(6)).isEqualTo(Types.DECIMAL);
  }

  @Test
  public void shouldTestWithAssertJDb() throws Exception {

    var req = new Request(ds, "SELECT stringKey, intKey, text, length, date, score FROM Item");

    Assertions.assertThat(req)
        .hasNumberOfRows(20)
        .hasNumberOfColumns(6)
        .column()
        .isOfType(ValueType.TEXT, false)
        .containsValues(
            "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16",
            "17", "18", "19", "20")
        .column(1)
        .isOfType(ValueType.NUMBER, false)
        .column("date")
        .isOfType(ValueType.DATE_TIME, false);
  }

  @Test
  public void shouldMapReturnTypes() throws Exception {

    assertThat(conn.isClosed()).isFalse();

    var stmt = conn.createStatement();
    var rs =
        stmt.executeQuery(
            "SELECT stringKey, intKey, text, length, date, score FROM Item order by stringKey");

    assertThat(rs.getString(1)).isEqualTo("1");
    assertThat(rs.getString("stringKey")).isEqualTo("1");
    assertThat(rs.findColumn("stringKey")).isEqualTo(1);

    assertThat(rs.getInt(2)).isEqualTo(1);
    assertThat(rs.getInt("intKey")).isEqualTo(1);

    assertThat(rs.getString("text")).hasSize(rs.getInt("length"));

    var cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    cal.add(Calendar.HOUR_OF_DAY, -1);
    var date = new Date(cal.getTimeInMillis());

    assertThat(rs.getDate("date").toString()).isEqualTo(date.toString());
    assertThat(rs.getDate(5).toString()).isEqualTo(date.toString());

    // DECIMAL
    assertThat(rs.getBigDecimal("score")).isEqualTo(BigDecimal.valueOf(965));
  }

  @Test
  public void shouldMapRatingToDouble() throws Exception {

    var rs = conn.createStatement().executeQuery("SELECT * FROM Author limit 10");
    var size = 0;
    while (rs.next()) {
      assertThat(rs.getDouble("rating")).isNotNull().isInstanceOf(Double.class);

      size++;
    }
    assertThat(size).isEqualTo(10);
  }

  @Test
  public void shouldConvertUUIDToDouble() throws Exception {

    var rs = conn.createStatement().executeQuery("SELECT * FROM Author limit 10");
    var count = 0;
    while (rs.next()) {
      assertThat(rs.getLong("uuid")).isNotNull().isInstanceOf(Long.class);
      count++;
    }
    assertThat(count).isEqualTo(10);
  }

  @Test
  public void shouldNavigateResultSetByMetadata() throws Exception {

    assertThat(conn.isClosed()).isFalse();
    var stmt = conn.createStatement();
    var rs =
        stmt.executeQuery("SELECT @rid, @class, stringKey, intKey, text, length, date FROM Item");

    rs.next();
    var metaData = rs.getMetaData();
    assertThat(metaData.getColumnCount()).isEqualTo(7);

    assertThat(metaData.getColumnName(1)).isEqualTo("@rid");
    assertThat(new RecordId(rs.getString(1)).isPersistent()).isEqualTo(true);
    assertThat(rs.getObject(1)).isInstanceOf(String.class);

    assertThat(metaData.getColumnName(2)).isEqualTo("@class");
    assertThat(rs.getString(2)).isEqualTo("Item");
    assertThat(rs.getObject(2)).isInstanceOf(String.class);

    assertThat(metaData.getColumnName(3)).isEqualTo("stringKey");
    assertThat(rs.getObject(3)).isInstanceOf(String.class);

    assertThat(metaData.getColumnName(4)).isEqualTo("intKey");

    assertThat(metaData.getColumnName(5)).isEqualTo("text");
    assertThat(rs.getObject(5)).isInstanceOf(String.class);

    assertThat(metaData.getColumnName(6)).isEqualTo("length");

    assertThat(metaData.getColumnName(7)).isEqualTo("date");
  }

  @Test
  public void shouldMapMissingFieldsToNull() throws Exception {

    var stmt = conn.createStatement();

    var rs =
        stmt.executeQuery(
            "select uuid, posts.* as post_ from (\n"
                + " select uuid, out('Writes') as posts from writer  unwind posts) order by uuid");

    var metaData = rs.getMetaData();
    while (rs.next()) {
      if (rs.getMetaData().getColumnCount() == 6) {
        // record with all attributes
        assertThat(rs.getTimestamp("post_date")).isNotNull();
        assertThat(rs.getTime("post_date")).isNotNull();
        assertThat(rs.getDate("post_date")).isNotNull();
      } else {
        // record missing date; only 5 column
        assertThat(rs.getTimestamp("post_date")).isNull();
        assertThat(rs.getTime("post_date")).isNull();
        assertThat(rs.getDate("post_date")).isNull();
      }
    }
  }

  @Test
  public void shouldFetchMetadataTheSparkStyle() throws Exception {

    // set spark "profile"

    conn.getInfo().setProperty("spark", "true");
    var stmt = conn.createStatement();

    var rs = stmt.executeQuery("select * from (select * from item) WHERE 1=0");

    var metaData = rs.getMetaData();

    assertThat(metaData.getColumnName(1)).isEqualTo("stringKey");
    assertThat(metaData.getColumnTypeName(1)).isEqualTo("STRING");
    assertThat(rs.getObject(1)).isInstanceOf(String.class);
  }

  @Test
  public void shouldReadBoolean() throws Exception {

    var stmt = conn.createStatement();
    var rs = stmt.executeQuery("SELECT  isActive, is_active FROM Writer");

    while (rs.next()) {
      assertThat(rs.getBoolean(1)).isTrue();
      assertThat(rs.getBoolean(2)).isTrue();
    }
  }
}
