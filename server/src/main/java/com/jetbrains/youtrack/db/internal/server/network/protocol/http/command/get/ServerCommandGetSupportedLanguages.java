/*
 *
 *
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */
package com.jetbrains.youtrack.db.internal.server.network.protocol.http.command.get;

import com.jetbrains.youtrack.db.internal.core.db.YouTrackDBInternal;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.server.network.protocol.http.HttpRequest;
import com.jetbrains.youtrack.db.internal.server.network.protocol.http.HttpResponse;
import com.jetbrains.youtrack.db.internal.server.network.protocol.http.command.ServerCommandAuthenticatedDbAbstract;
import java.util.HashSet;
import java.util.Set;

public class ServerCommandGetSupportedLanguages extends ServerCommandAuthenticatedDbAbstract {

  private static final String[] NAMES = {"GET|supportedLanguages/*"};

  @Override
  public boolean execute(final HttpRequest iRequest, HttpResponse iResponse) throws Exception {
    var urlParts =
        checkSyntax(iRequest.getUrl(), 2, "Syntax error: supportedLanguages/<database>");

    iRequest.getData().commandInfo = "Returns the supported languages";

    try (var db = getProfiledDatabaseSessionInstance(iRequest)) {
      var result = new EntityImpl(null);
      Set<String> languages = new HashSet<>();

      var scriptManager =
          YouTrackDBInternal.extract(server.getContext()).getScriptManager();
      for (var language : scriptManager.getSupportedLanguages()) {
        if (scriptManager.getFormatters() != null
            && scriptManager.getFormatters().get(language) != null) {
          languages.add(language);
        }
      }

      result.field("languages", languages);
      iResponse.writeRecord(result);
    }
    return false;
  }

  @Override
  public String[] getNames() {
    return NAMES;
  }
}
