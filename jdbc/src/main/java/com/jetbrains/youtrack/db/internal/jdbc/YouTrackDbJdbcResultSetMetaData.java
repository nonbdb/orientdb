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

import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.internal.core.db.record.LinkList;
import com.jetbrains.youtrack.db.api.schema.SchemaProperty;
import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.api.record.Blob;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.api.query.Result;
import java.math.BigDecimal;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 *
 */
public class YouTrackDbJdbcResultSetMetaData implements ResultSetMetaData {

  private static final Map<PropertyType, Integer> typesSqlTypes = new HashMap<>();

  static {
    typesSqlTypes.put(PropertyType.STRING, Types.VARCHAR);
    typesSqlTypes.put(PropertyType.INTEGER, Types.INTEGER);
    typesSqlTypes.put(PropertyType.FLOAT, Types.FLOAT);
    typesSqlTypes.put(PropertyType.SHORT, Types.SMALLINT);
    typesSqlTypes.put(PropertyType.BOOLEAN, Types.BOOLEAN);
    typesSqlTypes.put(PropertyType.LONG, Types.BIGINT);
    typesSqlTypes.put(PropertyType.DOUBLE, Types.DOUBLE);
    typesSqlTypes.put(PropertyType.DECIMAL, Types.DECIMAL);
    typesSqlTypes.put(PropertyType.DATE, Types.DATE);
    typesSqlTypes.put(PropertyType.DATETIME, Types.TIMESTAMP);
    typesSqlTypes.put(PropertyType.BYTE, Types.TINYINT);
    typesSqlTypes.put(PropertyType.SHORT, Types.SMALLINT);

    // NOT SURE ABOUT THE FOLLOWING MAPPINGS
    typesSqlTypes.put(PropertyType.BINARY, Types.BINARY);
    typesSqlTypes.put(PropertyType.EMBEDDED, Types.JAVA_OBJECT);
    typesSqlTypes.put(PropertyType.EMBEDDEDLIST, Types.ARRAY);
    typesSqlTypes.put(PropertyType.EMBEDDEDMAP, Types.JAVA_OBJECT);
    typesSqlTypes.put(PropertyType.EMBEDDEDSET, Types.ARRAY);
    typesSqlTypes.put(PropertyType.LINK, Types.JAVA_OBJECT);
    typesSqlTypes.put(PropertyType.LINKLIST, Types.ARRAY);
    typesSqlTypes.put(PropertyType.LINKMAP, Types.JAVA_OBJECT);
    typesSqlTypes.put(PropertyType.LINKSET, Types.ARRAY);
    typesSqlTypes.put(PropertyType.TRANSIENT, Types.NULL);
  }

  private final String[] fieldNames;
  private final YouTrackDbJdbcResultSet resultSet;

  public YouTrackDbJdbcResultSetMetaData(
      YouTrackDbJdbcResultSet youTrackDbJdbcResultSet, List<String> fieldNames) {
    resultSet = youTrackDbJdbcResultSet;
    this.fieldNames = fieldNames.toArray(new String[]{});
  }

  public static Integer getSqlType(final PropertyType iType) {
    return typesSqlTypes.get(iType);
  }

  public int getColumnCount() throws SQLException {

    return fieldNames.length;
  }

  @Override
  public String getCatalogName(final int column) throws SQLException {
    // return an empty String according to the method's documentation
    return "";
  }

  @Override
  public String getColumnClassName(final int column) throws SQLException {
    Object value = this.resultSet.getObject(column);
    if (value == null) {
      return null;
    }
    return value.getClass().getCanonicalName();
  }

  @Override
  public int getColumnDisplaySize(final int column) throws SQLException {
    return 0;
  }

  @Override
  public String getColumnLabel(final int column) throws SQLException {
    return getColumnName(column);
  }

  @Override
  public String getColumnName(final int column) throws SQLException {
    return fieldNames[column - 1];
  }

  @Override
  public int getColumnType(final int column) throws SQLException {
    final Result currentRecord = getCurrentRecord();

    if (column > fieldNames.length) {
      return Types.NULL;
    }

    String fieldName = fieldNames[column - 1];

    PropertyType otype =
        currentRecord
            .toEntity()
            .getSchemaType()
            .map(st -> st.getProperty(fieldName))
            .map(op -> op.getType())
            .orElse(null);

    if (otype == null) {
      Object value = currentRecord.getProperty(fieldName);

      if (value == null) {
        return Types.NULL;
      } else if (value instanceof Blob) {
        // Check if the type is a binary record or a collection of binary
        // records
        return Types.BINARY;
      } else if (value instanceof LinkList list) {
        // check if all the list items are instances of RecordBytes
        ListIterator<Identifiable> iterator = list.listIterator();
        Identifiable listElement;
        boolean stop = false;
        while (iterator.hasNext() && !stop) {
          listElement = iterator.next();
          if (!(listElement instanceof Blob)) {
            stop = true;
          }
        }
        if (!stop) {
          return Types.BLOB;
        }
      }
      return getSQLTypeFromJavaClass(value);
    } else {
      if (otype == PropertyType.EMBEDDED || otype == PropertyType.LINK) {
        Object value = currentRecord.getProperty(fieldName);
        if (value == null) {
          return Types.NULL;
        }
        // 1. Check if the type is another record or a collection of records
        if (value instanceof Blob) {
          return Types.BINARY;
        }
      } else {
        if (otype == PropertyType.EMBEDDEDLIST || otype == PropertyType.LINKLIST) {
          Object value = currentRecord.getProperty(fieldName);
          if (value == null) {
            return Types.NULL;
          }
          if (value instanceof LinkList list) {
            // check if all the list items are instances of RecordBytes
            ListIterator<Identifiable> iterator = list.listIterator();
            Identifiable listElement;
            boolean stop = false;
            while (iterator.hasNext() && !stop) {
              listElement = iterator.next();
              if (!(listElement instanceof Blob)) {
                stop = true;
              }
            }
            if (stop) {
              return typesSqlTypes.get(otype);
            } else {
              return Types.BLOB;
            }
          }
        }
      }
    }
    return typesSqlTypes.get(otype);
  }

  protected Result getCurrentRecord() throws SQLException {
    final Result currentRecord = resultSet.unwrap(Result.class);
    if (currentRecord == null) {
      throw new SQLException("No current record");
    }
    return currentRecord;
  }

  private int getSQLTypeFromJavaClass(final Object value) {
    if (value instanceof Boolean) {
      return typesSqlTypes.get(PropertyType.BOOLEAN);
    } else if (value instanceof Byte) {
      return typesSqlTypes.get(PropertyType.BYTE);
    } else if (value instanceof Date) {
      return typesSqlTypes.get(PropertyType.DATETIME);
    } else if (value instanceof Double) {
      return typesSqlTypes.get(PropertyType.DOUBLE);
    } else if (value instanceof BigDecimal) {
      return typesSqlTypes.get(PropertyType.DECIMAL);
    } else if (value instanceof Float) {
      return typesSqlTypes.get(PropertyType.FLOAT);
    } else if (value instanceof Integer) {
      return typesSqlTypes.get(PropertyType.INTEGER);
    } else if (value instanceof Long) {
      return typesSqlTypes.get(PropertyType.LONG);
    } else if (value instanceof Short) {
      return typesSqlTypes.get(PropertyType.SHORT);
    } else if (value instanceof String) {
      return typesSqlTypes.get(PropertyType.STRING);
    } else if (value instanceof List) {
      return typesSqlTypes.get(PropertyType.EMBEDDEDLIST);
    } else {
      return Types.JAVA_OBJECT;
    }
  }

  @Override
  public String getColumnTypeName(final int column) throws SQLException {
    final Result currentRecord = getCurrentRecord();

    String columnLabel = fieldNames[column - 1];

    return currentRecord
        .toEntity()
        .getSchemaType()
        .map(st -> st.getProperty(columnLabel))
        .map(p -> p.getType())
        .map(t -> t.toString())
        .orElse(null);
  }

  public int getPrecision(final int column) throws SQLException {
    return 0;
  }

  public int getScale(final int column) throws SQLException {
    return 0;
  }

  public String getSchemaName(final int column) throws SQLException {
    final Result currentRecord = getCurrentRecord();
    if (currentRecord == null) {
      return "";
    } else {
      return ((EntityImpl) currentRecord.toEntity()).getSession().getName();
    }
  }

  public String getTableName(final int column) throws SQLException {
    final SchemaProperty p = getProperty(column);
    return p != null ? p.getOwnerClass().getName() : null;
  }

  public boolean isAutoIncrement(final int column) throws SQLException {
    return false;
  }

  public boolean isCaseSensitive(final int column) throws SQLException {
    final SchemaProperty p = getProperty(column);
    return p != null && p.getCollate().getName().equalsIgnoreCase("ci");
  }

  public boolean isCurrency(final int column) throws SQLException {

    return false;
  }

  public boolean isDefinitelyWritable(final int column) throws SQLException {

    return false;
  }

  public int isNullable(final int column) throws SQLException {
    return columnNullableUnknown;
  }

  public boolean isReadOnly(final int column) throws SQLException {
    final SchemaProperty p = getProperty(column);
    return p != null && p.isReadonly();
  }

  public boolean isSearchable(int column) throws SQLException {
    return true;
  }

  public boolean isSigned(final int column) throws SQLException {
    final Result currentRecord = getCurrentRecord();
    PropertyType otype =
        currentRecord
            .toEntity()
            .getSchemaType()
            .map(st -> st.getProperty(fieldNames[column - 1]).getType())
            .orElse(null);

    return this.isANumericColumn(otype);
  }

  public boolean isWritable(final int column) throws SQLException {
    return !isReadOnly(column);
  }

  public boolean isWrapperFor(final Class<?> iface) throws SQLException {
    return false;
  }

  public <T> T unwrap(final Class<T> iface) throws SQLException {
    return null;
  }

  private boolean isANumericColumn(final PropertyType type) {
    return type == PropertyType.BYTE
        || type == PropertyType.DOUBLE
        || type == PropertyType.FLOAT
        || type == PropertyType.INTEGER
        || type == PropertyType.LONG
        || type == PropertyType.SHORT;
  }

  protected SchemaProperty getProperty(final int column) throws SQLException {

    String fieldName = getColumnName(column);

    return getCurrentRecord()
        .toEntity()
        .getSchemaType()
        .map(st -> st.getProperty(fieldName))
        .orElse(null);
  }
}
