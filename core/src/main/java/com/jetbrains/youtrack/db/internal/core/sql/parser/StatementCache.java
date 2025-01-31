package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.internal.common.log.LogManager;
import com.jetbrains.youtrack.db.api.config.GlobalConfiguration;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseRecordThreadLocal;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.api.exception.CommandSQLParsingException;
import com.jetbrains.youtrack.db.internal.core.db.YouTrackDBInternal;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class is an LRU cache for already parsed SQL statement executors. It stores itself in the
 * storage as a resource. It also acts an an entry point for the SQL parser.
 */
public class StatementCache {

  private final Map<String, SQLStatement> map;
  private final int mapSize;

  /**
   * @param size the size of the cache
   */
  public StatementCache(int size) {
    this.mapSize = size;
    map =
        new LinkedHashMap<String, SQLStatement>(size) {
          protected boolean removeEldestEntry(final Map.Entry<String, SQLStatement> eldest) {
            return super.size() > mapSize;
          }
        };
  }

  /**
   * @param statement an SQL statement
   * @return true if the corresponding executor is present in the cache
   */
  public boolean contains(String statement) {
    if (GlobalConfiguration.STATEMENT_CACHE_SIZE.getValueAsInteger() == 0) {
      return false;
    }

    synchronized (map) {
      return map.containsKey(statement);
    }
  }

  /**
   * returns an already parsed SQL executor, taking it from the cache if it exists or creating a new
   * one (parsing and then putting it into the cache) if it doesn't
   *
   * @param statement the SQL statement
   * @param db        the current DB instance. If null, cache is ignored and a new executor is
   *                  created through statement parsing
   * @return a statement executor from the cache
   */
  public static SQLStatement get(String statement, DatabaseSessionInternal db) {
    if (db == null) {
      return parse(statement);
    }

    var resource = db.getSharedContext().getStatementCache();
    return resource.get(statement);
  }

  /**
   * returns an already parsed server-level SQL executor, taking it from the cache if it exists or
   * creating a new one (parsing and then putting it into the cache) if it doesn't
   *
   * @param statement the SQL statement
   * @param db        the current YouTrackDB instance. If null, cache is ignored and a new executor
   *                  is created through statement parsing
   * @return a statement executor from the cache
   */
  public static SQLServerStatement getServerStatement(String statement, YouTrackDBInternal db) {
    // TODO create a global cache!
    return parseServerStatement(statement);
  }

  /**
   * @param statement an SQL statement
   * @return the corresponding executor, taking it from the internal cache, if it exists
   */
  public SQLStatement get(String statement) {
    if (GlobalConfiguration.STATEMENT_CACHE_SIZE.getValueAsInteger() == 0) {
      return parse(statement);
    }

    SQLStatement result;
    synchronized (map) {
      // LRU
      result = map.remove(statement);
      if (result != null) {
        map.put(statement, result);
      }
    }
    if (result == null) {
      result = parse(statement);
      synchronized (map) {
        map.put(statement, result);
      }
    }
    return result;
  }

  /**
   * parses an SQL statement and returns the corresponding executor
   *
   * @param statement the SQL statement
   * @return the corresponding executor
   * @throws CommandSQLParsingException if the input parameter is not a valid SQL statement
   */
  protected static SQLStatement parse(String statement) throws CommandSQLParsingException {
    try {
      var db = DatabaseRecordThreadLocal.instance().getIfDefined();
      InputStream is;

      if (db == null) {
        is = new ByteArrayInputStream(statement.getBytes());
      } else {
        try {
          is =
              new ByteArrayInputStream(
                  statement.getBytes(db.getStorageInfo().getConfiguration().getCharset()));
        } catch (UnsupportedEncodingException e2) {
          LogManager.instance()
              .warn(
                  StatementCache.class,
                  "Unsupported charset for database "
                      + db
                      + " "
                      + db.getStorageInfo().getConfiguration().getCharset());
          is = new ByteArrayInputStream(statement.getBytes());
        }
      }

      YouTrackDBSql osql = null;
      if (db == null) {
        osql = new YouTrackDBSql(is);
      } else {
        try {
          osql = new YouTrackDBSql(is, db.getStorageInfo().getConfiguration().getCharset());
        } catch (UnsupportedEncodingException e2) {
          LogManager.instance()
              .warn(
                  StatementCache.class,
                  "Unsupported charset for database "
                      + db
                      + " "
                      + db.getStorageInfo().getConfiguration().getCharset());
          osql = new YouTrackDBSql(is);
        }
      }
      var result = osql.parse();
      result.originalStatement = statement;

      return result;
    } catch (ParseException e) {
      throwParsingException(e, statement);
    } catch (TokenMgrError e2) {
      throwParsingException(e2, statement);
    }
    return null;
  }

  /**
   * parses an SQL statement and returns the corresponding executor
   *
   * @param statement the SQL statement
   * @return the corresponding executor
   * @throws CommandSQLParsingException if the input parameter is not a valid SQL statement
   */
  protected static SQLServerStatement parseServerStatement(String statement)
      throws CommandSQLParsingException {
    try {
      InputStream is = new ByteArrayInputStream(statement.getBytes());
      var osql = new YouTrackDBSql(is);
      var result = osql.parseServerStatement();
      //      result.originalStatement = statement;

      return result;
    } catch (ParseException e) {
      throwParsingException(e, statement);
    } catch (TokenMgrError e2) {
      throwParsingException(e2, statement);
    }
    return null;
  }

  protected static void throwParsingException(ParseException e, String statement) {
    throw new CommandSQLParsingException(e, statement);
  }

  protected static void throwParsingException(TokenMgrError e, String statement) {
    throw new CommandSQLParsingException(e, statement);
  }

  public void clear() {
    if (GlobalConfiguration.STATEMENT_CACHE_SIZE.getValueAsInteger() == 0) {
      return;
    }

    synchronized (map) {
      map.clear();
    }
  }
}
