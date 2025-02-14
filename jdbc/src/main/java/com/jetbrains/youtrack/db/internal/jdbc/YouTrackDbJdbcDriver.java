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

import com.jetbrains.youtrack.db.internal.common.log.LogManager;
import com.jetbrains.youtrack.db.internal.core.YouTrackDBConstants;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Logger;

public class YouTrackDbJdbcDriver implements java.sql.Driver {

  static {
    try {
      DriverManager.registerDriver(new YouTrackDbJdbcDriver());
    } catch (SQLException e) {
      LogManager.instance()
          .error(YouTrackDbJdbcDriver.class, "Error while registering the JDBC Driver", e);
    }
  }

  public static String getVersion() {
    return "YouTrackDB " + YouTrackDBConstants.getVersion() + " JDBC Driver";
  }

  @Override
  public boolean acceptsURL(String url) throws SQLException {
    if (url == null) {
      return false;
    }
    return url.toLowerCase(Locale.ENGLISH).startsWith("jdbc:youtrackdb:");
  }

  @Override
  public Connection connect(String url, Properties info) throws SQLException {
    if (!acceptsURL(url)) {
      return null;
    }

    return new YouTrackDbJdbcConnection(url, info);
  }

  @Override
  public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
    return new DriverPropertyInfo[]{};
  }

  @Override
  public boolean jdbcCompliant() {
    return false;
  }

  @Override
  public int getMajorVersion() {
    return YouTrackDBConstants.getVersionMajor();
  }

  @Override
  public int getMinorVersion() {
    return YouTrackDBConstants.getVersionMinor();
  }

  @Override
  public Logger getParentLogger() throws SQLFeatureNotSupportedException {
    return null;
  }
}
