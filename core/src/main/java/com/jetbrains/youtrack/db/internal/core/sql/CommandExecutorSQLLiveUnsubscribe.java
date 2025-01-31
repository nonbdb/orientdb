/*
 *
 *  *  Copyright YouTrackDB
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
import com.jetbrains.youtrack.db.internal.common.log.LogManager;
import com.jetbrains.youtrack.db.internal.core.command.CommandDistributedReplicateRequest;
import com.jetbrains.youtrack.db.internal.core.command.CommandRequest;
import com.jetbrains.youtrack.db.internal.core.command.CommandRequestText;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.exception.QueryParsingException;
import com.jetbrains.youtrack.db.internal.core.query.live.LiveQueryHook;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import java.util.Locale;
import java.util.Map;

/**
 *
 */
public class CommandExecutorSQLLiveUnsubscribe extends CommandExecutorSQLAbstract
    implements CommandDistributedReplicateRequest {

  public static final String KEYWORD_LIVE_UNSUBSCRIBE = "LIVE UNSUBSCRIBE";

  protected String unsubscribeToken;

  public CommandExecutorSQLLiveUnsubscribe() {
  }

  private Object executeUnsubscribe() {
    try {

      LiveQueryHook.unsubscribe(Integer.parseInt(unsubscribeToken), getDatabase());
      var result = new EntityImpl(null);
      result.field("unsubscribed", unsubscribeToken);
      result.field("unsubscribe", true);
      result.field("token", unsubscribeToken);

      return result;
    } catch (Exception e) {
      LogManager.instance()
          .warn(
              this,
              "error unsubscribing token "
                  + unsubscribeToken
                  + ": "
                  + e.getClass().getName()
                  + " - "
                  + e.getMessage());
      var result = new EntityImpl(null);
      result.field("error-unsubscribe", unsubscribeToken);
      result.field("error-description", e.getMessage());
      result.field("error-type", e.getClass().getName());

      return result;
    }
  }

  @Override
  public long getDistributedTimeout() {
    return getDatabase()
        .getConfiguration()
        .getValueAsLong(GlobalConfiguration.DISTRIBUTED_COMMAND_QUICK_TASK_SYNCH_TIMEOUT);
  }

  public Object execute(DatabaseSessionInternal db, final Map<Object, Object> iArgs) {
    if (this.unsubscribeToken != null) {
      return executeUnsubscribe();
    }
    var result = new EntityImpl(null);
    result.field("error-unsubscribe", "no token");
    return result;
  }

  @Override
  public CommandExecutorSQLLiveUnsubscribe parse(DatabaseSessionInternal db,
      CommandRequest iRequest) {
    var requestText = (CommandRequestText) iRequest;
    var originalText = requestText.getText();
    var remainingText = requestText.getText().trim().substring(5).trim();
    requestText.setText(remainingText);
    try {
      if (remainingText.toLowerCase(Locale.ENGLISH).startsWith("unsubscribe")) {
        remainingText = remainingText.substring("unsubscribe".length()).trim();
        if (remainingText.contains(" ")) {
          throw new QueryParsingException(
              "invalid unsubscribe token for live query: " + remainingText);
        }
        this.unsubscribeToken = remainingText;
      }
    } finally {
      requestText.setText(originalText);
    }
    return this;
  }

  @Override
  public QUORUM_TYPE getQuorumType() {
    return QUORUM_TYPE.NONE;
  }
}
