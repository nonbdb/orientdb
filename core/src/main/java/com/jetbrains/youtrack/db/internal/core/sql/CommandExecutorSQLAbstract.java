/*
 *
 *
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

import com.jetbrains.youtrack.db.api.config.GlobalConfiguration;
import com.jetbrains.youtrack.db.api.exception.BaseException;
import com.jetbrains.youtrack.db.api.exception.CommandSQLParsingException;
import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext.TIMEOUT_STRATEGY;
import com.jetbrains.youtrack.db.internal.core.command.CommandDistributedReplicateRequest;
import com.jetbrains.youtrack.db.internal.core.command.CommandExecutorAbstract;
import com.jetbrains.youtrack.db.internal.core.command.CommandRequest;
import com.jetbrains.youtrack.db.internal.core.command.CommandRequestAbstract;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.index.Index;
import com.jetbrains.youtrack.db.internal.core.metadata.MetadataInternal;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.SchemaClassImpl;
import com.jetbrains.youtrack.db.internal.core.metadata.security.Rule;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLStatement;
import com.jetbrains.youtrack.db.internal.core.sql.parser.StatementCache;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * SQL abstract Command Executor implementation.
 */
public abstract class CommandExecutorSQLAbstract extends CommandExecutorAbstract {

  public static final String KEYWORD_FROM = "FROM";
  public static final String KEYWORD_LET = "LET";
  public static final String KEYWORD_WHERE = "WHERE";
  public static final String KEYWORD_LIMIT = "LIMIT";
  public static final String KEYWORD_SKIP = "SKIP";
  public static final String KEYWORD_OFFSET = "OFFSET";
  public static final String KEYWORD_TIMEOUT = "TIMEOUT";
  public static final String KEYWORD_RETURN = "RETURN";
  public static final String KEYWORD_KEY = "key";
  public static final String KEYWORD_RID = "rid";
  public static final String CLUSTER_PREFIX = "CLUSTER:";
  public static final String CLASS_PREFIX = "CLASS:";
  public static final String INDEX_PREFIX = "INDEX:";
  public static final String KEYWORD_UNSAFE = "UNSAFE";

  public static final String INDEX_VALUES_PREFIX = "INDEXVALUES:";
  public static final String INDEX_VALUES_ASC_PREFIX = "INDEXVALUESASC:";
  public static final String INDEX_VALUES_DESC_PREFIX = "INDEXVALUESDESC:";

  public static final String METADATA_PREFIX = "METADATA:";
  public static final String METADATA_SCHEMA = "SCHEMA";
  public static final String METADATA_INDEXMGR = "INDEXMANAGER";
  public static final String METADATA_STORAGE = "STORAGE";
  public static final String METADATA_DATABASE = "DATABASE";

  public static final String DEFAULT_PARAM_USER = "$user";

  protected long timeoutMs = GlobalConfiguration.COMMAND_TIMEOUT.getValueAsLong();
  protected TIMEOUT_STRATEGY timeoutStrategy = TIMEOUT_STRATEGY.EXCEPTION;
  protected SQLStatement preParsedStatement;

  /**
   * The command is replicated
   *
   * @return
   */
  public CommandDistributedReplicateRequest.DISTRIBUTED_EXECUTION_MODE
  getDistributedExecutionMode() {
    return CommandDistributedReplicateRequest.DISTRIBUTED_EXECUTION_MODE.REPLICATE;
  }

  public boolean isIdempotent() {
    return false;
  }

  protected void throwSyntaxErrorException(final String iText) {
    throw new CommandSQLParsingException(
        iText + ". Use " + getSyntax(), parserText, parserGetPreviousPosition());
  }

  protected void throwParsingException(final String iText) {
    throw new CommandSQLParsingException(iText, parserText, parserGetPreviousPosition());
  }

  protected void throwParsingException(final String iText, Exception e) {
    throw BaseException.wrapException(
        new CommandSQLParsingException(iText, parserText, parserGetPreviousPosition()), e);
  }

  /**
   * Parses the timeout keyword if found.
   */
  protected boolean parseTimeout(final String w) throws CommandSQLParsingException {
    if (!w.equals(KEYWORD_TIMEOUT)) {
      return false;
    }

    String word = parserNextWord(true);

    try {
      timeoutMs = Long.parseLong(word);
    } catch (NumberFormatException ignore) {
      throwParsingException(
          "Invalid "
              + KEYWORD_TIMEOUT
              + " value set to '"
              + word
              + "' but it should be a valid long. Example: "
              + KEYWORD_TIMEOUT
              + " 3000");
    }

    if (timeoutMs < 0) {
      throwParsingException(
          "Invalid "
              + KEYWORD_TIMEOUT
              + ": value set minor than ZERO. Example: "
              + KEYWORD_TIMEOUT
              + " 10000");
    }

    word = parserNextWord(true);

    if (word != null) {
      if (word.equals(TIMEOUT_STRATEGY.EXCEPTION.toString())) {
        timeoutStrategy = TIMEOUT_STRATEGY.EXCEPTION;
      } else if (word.equals(TIMEOUT_STRATEGY.RETURN.toString())) {
        timeoutStrategy = TIMEOUT_STRATEGY.RETURN;
      } else {
        parserGoBack();
      }
    }

    return true;
  }

  protected Set<String> getInvolvedClustersOfClasses(final Collection<String> iClassNames) {
    final var db = getDatabase();

    final Set<String> clusters = new HashSet<String>();

    for (String clazz : iClassNames) {
      final SchemaClass cls = db.getMetadata().getImmutableSchemaSnapshot().getClass(clazz);
      if (cls != null) {
        for (int clId : cls.getPolymorphicClusterIds()) {
          // FILTER THE CLUSTER WHERE THE USER HAS THE RIGHT ACCESS
          if (clId > -1 && checkClusterAccess(db, db.getClusterNameById(clId))) {
            clusters.add(db.getClusterNameById(clId).toLowerCase(Locale.ENGLISH));
          }
        }
      }
    }

    return clusters;
  }

  protected Set<String> getInvolvedClustersOfClusters(final Collection<String> iClusterNames) {
    var db = getDatabase();

    final Set<String> clusters = new HashSet<String>();

    for (String cluster : iClusterNames) {
      final String c = cluster.toLowerCase(Locale.ENGLISH);
      // FILTER THE CLUSTER WHERE THE USER HAS THE RIGHT ACCESS
      if (checkClusterAccess(db, c)) {
        clusters.add(c);
      }
    }

    return clusters;
  }

  protected Set<String> getInvolvedClustersOfIndex(final String iIndexName) {
    final DatabaseSessionInternal db = getDatabase();

    final Set<String> clusters = new HashSet<String>();

    final MetadataInternal metadata = db.getMetadata();
    final Index idx = metadata.getIndexManagerInternal().getIndex(db, iIndexName);
    if (idx != null && idx.getDefinition() != null) {
      final String clazz = idx.getDefinition().getClassName();

      if (clazz != null) {
        final SchemaClass cls = metadata.getImmutableSchemaSnapshot().getClass(clazz);
        if (cls != null) {
          for (int clId : cls.getClusterIds()) {
            final String clName = db.getClusterNameById(clId);
            if (clName != null) {
              clusters.add(clName.toLowerCase(Locale.ENGLISH));
            }
          }
        }
      }
    }

    return clusters;
  }

  protected boolean checkClusterAccess(final DatabaseSessionInternal db,
      final String iClusterName) {
    return db.geCurrentUser() == null
        || db.geCurrentUser()
        .checkIfAllowed(db,
            Rule.ResourceGeneric.CLUSTER, iClusterName, getSecurityOperationType())
        != null;
  }

  protected void bindDefaultContextVariables() {
    if (context != null) {
      if (getDatabase() != null && getDatabase().geCurrentUser() != null) {
        context.setVariable(DEFAULT_PARAM_USER,
            getDatabase().geCurrentUser().getIdentity());
      }
    }
  }

  protected String preParse(final String queryText, final CommandRequest iRequest) {
    final boolean strict = getDatabase().getStorageInfo().getConfiguration().isStrictSql();

    if (strict) {
      try {
        final SQLStatement result = StatementCache.get(queryText, getDatabase());
        preParsedStatement = result;

        if (iRequest instanceof CommandRequestAbstract) {
          final Map<Object, Object> params = ((CommandRequestAbstract) iRequest).getParameters();
          StringBuilder builder = new StringBuilder();
          result.toString(params, builder);
          return builder.toString();
        }
        return result.toString();
      } catch (CommandSQLParsingException sqlx) {
        throw sqlx;
      } catch (Exception e) {
        throwParsingException("Error parsing query: \n" + queryText + "\n" + e.getMessage(), e);
      }
    }
    return queryText;
  }

  protected String decodeClassName(String s) {
    return SchemaClassImpl.decodeClassName(s);
  }
}
