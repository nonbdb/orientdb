package com.jetbrains.youtrack.db.internal.jdbc;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;
import java.util.Map;

/**
 *
 */
public class YouTrackDbJdbcArray implements Array {

  private final Collection<? extends Object> values;

  public YouTrackDbJdbcArray(Collection<? extends Object> values) {
    this.values = values;
  }

  @Override
  public String getBaseTypeName() throws SQLException {
    return String.class.getTypeName();
  }

  @Override
  public int getBaseType() throws SQLException {
    return Types.VARCHAR;
  }

  @Override
  public Object getArray() throws SQLException {

    return values.toArray();
  }

  @Override
  public Object getArray(Map<String, Class<?>> map) throws SQLException {
    return null;
  }

  @Override
  public Object getArray(long index, int count) throws SQLException {
    return null;
  }

  @Override
  public Object getArray(long index, int count, Map<String, Class<?>> map) throws SQLException {
    return null;
  }

  @Override
  public ResultSet getResultSet() throws SQLException {
    return null;
  }

  @Override
  public ResultSet getResultSet(Map<String, Class<?>> map) throws SQLException {
    return null;
  }

  @Override
  public ResultSet getResultSet(long index, int count) throws SQLException {
    return null;
  }

  @Override
  public ResultSet getResultSet(long index, int count, Map<String, Class<?>> map)
      throws SQLException {
    return null;
  }

  @Override
  public void free() throws SQLException {
  }
}
