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
package com.jetbrains.youtrack.db.internal.server.network.protocol.http.command.post;

import com.jetbrains.youtrack.db.api.DatabaseSession;
import com.jetbrains.youtrack.db.api.config.GlobalConfiguration;
import com.jetbrains.youtrack.db.api.query.ResultSet;
import com.jetbrains.youtrack.db.internal.core.command.BasicCommandContext;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseStats;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.sql.SQLEngine;
import com.jetbrains.youtrack.db.internal.core.sql.parser.DDLStatement;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLFetchPlan;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLLimit;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLMatchStatement;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLSelectStatement;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLStatement;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLTraverseStatement;
import com.jetbrains.youtrack.db.internal.server.network.protocol.http.HttpRequest;
import com.jetbrains.youtrack.db.internal.server.network.protocol.http.HttpResponse;
import com.jetbrains.youtrack.db.internal.server.network.protocol.http.command.ServerCommandAuthenticatedDbAbstract;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

public class ServerCommandPostCommand extends ServerCommandAuthenticatedDbAbstract {

  private static final String[] NAMES = {"GET|command/*", "POST|command/*"};

  @Override
  public boolean execute(final HttpRequest iRequest, HttpResponse iResponse) throws Exception {
    final String[] urlParts =
        checkSyntax(
            iRequest.getUrl(),
            3,
            "Syntax error: command/<database>/<language>/<command-text>[/limit][/<fetchPlan>]");

    // TRY TO GET THE COMMAND FROM THE URL, THEN FROM THE CONTENT
    final String language = urlParts.length > 2 ? urlParts[2].trim() : "sql";
    String text = urlParts.length > 3 ? urlParts[3].trim() : iRequest.getContent();
    int limit = urlParts.length > 4 ? Integer.parseInt(urlParts[4].trim()) : -1;
    String fetchPlan = urlParts.length > 5 ? urlParts[5] : null;
    final String accept = iRequest.getHeader("accept");

    Object params = null;
    String mode = "resultset";

    boolean returnExecutionPlan = true;

    long begin = System.currentTimeMillis();
    if (iRequest.getContent() != null && !iRequest.getContent().isEmpty()) {
      // CONTENT REPLACES TEXT
      if (iRequest.getContent().startsWith("{")) {
        // JSON PAYLOAD
        final EntityImpl entity = new EntityImpl(null);
        entity.updateFromJSON(iRequest.getContent());
        text = entity.field("command");
        params = entity.field("parameters");
        if (entity.containsField("mode")) {
          mode = entity.field("mode");
        }

        if ("false".equalsIgnoreCase("" + entity.field("returnExecutionPlan"))) {
          returnExecutionPlan = false;
        }

        if (params instanceof Collection) {
          final Object[] paramArray = new Object[((Collection) params).size()];
          ((Collection) params).toArray(paramArray);
          params = paramArray;
        }
      } else {
        text = iRequest.getContent();
      }
    }

    if ("false".equalsIgnoreCase(iRequest.getHeader("return-execution-plan"))) {
      returnExecutionPlan = false;
    }

    if (text == null) {
      throw new IllegalArgumentException("text cannot be null");
    }

    iRequest.getData().commandInfo = "Command";
    iRequest.getData().commandDetail = text;

    DatabaseSessionInternal db = null;

    boolean ok = false;
    boolean txBegun = false;
    try {
      db = getProfiledDatabaseInstance(iRequest);
      db.resetRecordLoadStats();
      SQLStatement stm = parseStatement(language, text, db);

      if (stm != null && !(stm instanceof DDLStatement)) {
        db.begin();
        txBegun = true;
      }

      ResultSet result = executeStatement(language, text, params, db);
      limit = getLimitFromStatement(stm, limit);
      String localFetchPlan = getFetchPlanFromStatement(stm);
      if (localFetchPlan != null) {
        fetchPlan = localFetchPlan;
      }
      int i = 0;
      List response = new ArrayList();
      TimerTask commandInterruptTimer = null;
      if (db.getConfiguration().getValueAsLong(GlobalConfiguration.COMMAND_TIMEOUT) > 0
          && !language.equalsIgnoreCase("sql")) {
      }
      try {
        while (result.hasNext()) {
          if (limit >= 0 && i >= limit) {
            break;
          }
          response.add(result.next());
          i++;
        }
      } finally {
        if (commandInterruptTimer != null) {
          commandInterruptTimer.cancel();
        }
      }
      Map<String, Object> additionalContent = new HashMap<>();
      if (returnExecutionPlan) {
        var dbRef = db;
        result
            .getExecutionPlan()
            .ifPresent(x -> additionalContent.put("executionPlan", x.toResult(dbRef).toMap()));
      }

      result.close();
      long elapsedMs = System.currentTimeMillis() - begin;

      String format = null;
      if (fetchPlan != null) {
        format = "fetchPlan:" + fetchPlan;
      }

      if (iRequest.getHeader("TE") != null) {
        iResponse.setStreaming(true);
      }

      additionalContent.put("elapsedMs", elapsedMs);
      DatabaseStats dbStats = db.getStats();
      additionalContent.put("dbStats", dbStats.toResult(db).toMap());

      iResponse.writeResult(response, format, accept, additionalContent, mode, db);
      ok = true;
    } finally {
      if (db != null) {
        db.activateOnCurrentThread();

        if (txBegun && db.getTransaction().isActive()) {
          if (ok) {
            db.commit();
          } else {
            db.rollback();
          }
        }
        db.close();
      }
    }

    return false;
  }

  public static String getFetchPlanFromStatement(SQLStatement statement) {
    if (statement instanceof SQLSelectStatement) {
      SQLFetchPlan fp = ((SQLSelectStatement) statement).getFetchPlan();
      if (fp != null) {
        return fp.toString().substring("FETCHPLAN ".length());
      }
    }

    return null;
  }

  public static SQLStatement parseStatement(String language, String text, DatabaseSession db) {
    try {
      if (language != null && language.equalsIgnoreCase("sql")) {
        return SQLEngine.parse(text, (DatabaseSessionInternal) db);
      }
    } catch (Exception e) {
    }
    return null;
  }

  public static int getLimitFromStatement(SQLStatement statement, int previousLimit) {
    try {
      SQLLimit limit = null;
      if (statement instanceof SQLSelectStatement) {
        limit = ((SQLSelectStatement) statement).getLimit();
      } else if (statement instanceof SQLMatchStatement) {
        limit = ((SQLMatchStatement) statement).getLimit();
      } else if (statement instanceof SQLTraverseStatement) {
        limit = ((SQLTraverseStatement) statement).getLimit();
      }
      if (limit != null) {
        return limit.getValue(new BasicCommandContext());
      }

    } catch (Exception e) {
    }
    return previousLimit;
  }

  protected ResultSet executeStatement(
      String language, String text, Object params, DatabaseSession db) {
    ResultSet result;
    if ("sql".equalsIgnoreCase(language)) {
      if (params instanceof Map) {
        result = db.command(text, (Map) params);
      } else if (params instanceof Object[]) {
        result = db.command(text, (Object[]) params);
      } else {
        result = db.command(text, params);
      }
    } else {
      if (params instanceof Map) {
        result = db.execute(language, text, (Map) params);
      } else if (params instanceof Object[]) {
        result = db.execute(language, text, (Object[]) params);
      } else {
        result = db.execute(language, text, params);
      }
    }
    return result;
  }

  @Override
  public String[] getNames() {
    return NAMES;
  }
}
