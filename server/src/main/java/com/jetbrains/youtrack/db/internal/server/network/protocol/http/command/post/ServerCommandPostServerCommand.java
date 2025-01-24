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

import com.jetbrains.youtrack.db.api.YouTrackDB;
import com.jetbrains.youtrack.db.api.query.ResultSet;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.server.network.protocol.http.HttpRequest;
import com.jetbrains.youtrack.db.internal.server.network.protocol.http.HttpResponse;
import com.jetbrains.youtrack.db.internal.server.network.protocol.http.command.ServerCommandAuthenticatedServerAbstract;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerCommandPostServerCommand extends ServerCommandAuthenticatedServerAbstract {

  private static final String[] NAMES = {"POST|servercommand"};

  public ServerCommandPostServerCommand() {
    super("server.command");
  }

  @Override
  public boolean execute(final HttpRequest iRequest, HttpResponse iResponse) throws Exception {
    final String[] urlParts = checkSyntax(iRequest.getUrl(), 1, "Syntax error: servercommand");

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
        entity.fromJSON(iRequest.getContent());
        text = entity.field("command");
        params = entity.field("parameters");

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

    ResultSet result = executeStatement(language, text, params);

    int i = 0;
    List response = new ArrayList();
    while (result.hasNext()) {
      if (limit >= 0 && i >= limit) {
        break;
      }
      response.add(result.next());
      i++;
    }

    Map<String, Object> additionalContent = new HashMap<>();
    if (returnExecutionPlan) {
      result
          .getExecutionPlan()
          .ifPresent(x -> additionalContent.put("executionPlan", x.toResult(null).toMap()));
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
    iResponse.writeResult(response, format, accept, additionalContent, mode, null);

    return false;
  }

  protected ResultSet executeStatement(String language, String text, Object params) {
    ResultSet result;

    YouTrackDB odb = this.server.getContext();
    if (params instanceof Map) {
      result = odb.execute(text, (Map) params);
    } else if (params instanceof Object[]) {
      result = odb.execute(text, (Object[]) params);
    } else {
      result = odb.execute(text, params);
    }
    return result;
  }

  @Override
  public String[] getNames() {
    return NAMES;
  }
}
