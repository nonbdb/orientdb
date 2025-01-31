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
package com.jetbrains.youtrack.db.internal.server.network.protocol.http.command.get;

import com.jetbrains.youtrack.db.api.exception.RecordNotFoundException;
import com.jetbrains.youtrack.db.api.record.DBRecord;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.id.RecordId;
import com.jetbrains.youtrack.db.internal.server.network.protocol.http.HttpRequest;
import com.jetbrains.youtrack.db.internal.server.network.protocol.http.HttpResponse;
import com.jetbrains.youtrack.db.internal.server.network.protocol.http.HttpUtils;
import com.jetbrains.youtrack.db.internal.server.network.protocol.http.command.ServerCommandAuthenticatedDbAbstract;

public class ServerCommandGetDocumentByClass extends ServerCommandAuthenticatedDbAbstract {

  private static final String[] NAMES = {"GET|documentbyclass/*", "HEAD|documentbyclass/*"};

  @Override
  public boolean execute(final HttpRequest iRequest, HttpResponse iResponse) throws Exception {
    final String[] urlParts =
        checkSyntax(
            iRequest.getUrl(),
            4,
            "Syntax error: documentbyclass/<database>/<class-name>/<record-position>[/fetchPlan]");

    final String fetchPlan = urlParts.length > 4 ? urlParts[4] : null;

    iRequest.getData().commandInfo = "Load entity";

    final DBRecord rec;
    try (DatabaseSessionInternal db = getProfiledDatabaseInstance(iRequest)) {
      if (db.getMetadata().getImmutableSchemaSnapshot().getClass(urlParts[2]) == null) {
        throw new IllegalArgumentException("Invalid class '" + urlParts[2] + "'");
      }
      final String rid = db.getClusterIdByName(urlParts[2]) + ":" + urlParts[3];
      try {
        rec = db.load(new RecordId(rid));
      } catch (RecordNotFoundException e) {
        iResponse.send(
            HttpUtils.STATUS_NOTFOUND_CODE,
            HttpUtils.STATUS_NOTFOUND_DESCRIPTION,
            HttpUtils.CONTENT_JSON,
            "Record with id '" + rid + "' was not found.",
            null);
        return false;
      }
      if (iRequest.getHttpMethod().equals("HEAD"))
      // JUST SEND HTTP CODE 200
      {
        iResponse.send(
            HttpUtils.STATUS_OK_CODE, HttpUtils.STATUS_OK_DESCRIPTION, null, null, null);
      } else
      // SEND THE DOCUMENT BACK
      {
        iResponse.writeRecord(rec, fetchPlan, null);
      }

    }

    return false;
  }

  @Override
  public String[] getNames() {
    return NAMES;
  }
}
