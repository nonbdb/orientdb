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

import com.jetbrains.youtrack.db.api.exception.CommandSQLParsingException;
import com.jetbrains.youtrack.db.internal.core.command.CommandRequest;
import com.jetbrains.youtrack.db.internal.core.command.CommandRequestText;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.api.exception.CommandExecutionException;
import com.jetbrains.youtrack.db.internal.core.metadata.security.Role;
import java.util.Map;

/**
 * SQL REVOKE command: Revoke a privilege to a database role.
 */
public class CommandExecutorSQLRevoke extends CommandExecutorSQLPermissionAbstract {

  public static final String KEYWORD_REVOKE = "REVOKE";
  private static final String KEYWORD_FROM = "FROM";

  @SuppressWarnings("unchecked")
  public CommandExecutorSQLRevoke parse(final CommandRequest iRequest) {
    final CommandRequestText textRequest = (CommandRequestText) iRequest;

    String queryText = textRequest.getText();
    String originalQuery = queryText;
    try {
      queryText = preParse(queryText, iRequest);
      textRequest.setText(queryText);

      final var database = getDatabase();

      init((CommandRequestText) iRequest);

      privilege = Role.PERMISSION_NONE;
      resource = null;
      role = null;

      StringBuilder word = new StringBuilder();

      int oldPos = 0;
      int pos = nextWord(parserText, parserTextUpperCase, oldPos, word, true);
      if (pos == -1 || !word.toString().equals(KEYWORD_REVOKE)) {
        throw new CommandSQLParsingException(
            "Keyword " + KEYWORD_REVOKE + " not found. Use " + getSyntax(), parserText, oldPos);
      }

      pos = nextWord(parserText, parserTextUpperCase, pos, word, true);
      if (pos == -1) {
        throw new CommandSQLParsingException("Invalid privilege", parserText, oldPos);
      }

      parsePrivilege(word, oldPos);

      pos = nextWord(parserText, parserTextUpperCase, pos, word, true);
      if (pos == -1 || !word.toString().equals(KEYWORD_ON)) {
        throw new CommandSQLParsingException(
            "Keyword " + KEYWORD_ON + " not found. Use " + getSyntax(), parserText, oldPos);
      }

      pos = nextWord(parserText, parserText, pos, word, true);
      if (pos == -1) {
        throw new CommandSQLParsingException("Invalid resource", parserText, oldPos);
      }

      resource = word.toString();

      pos = nextWord(parserText, parserTextUpperCase, pos, word, true);
      if (pos == -1 || !word.toString().equals(KEYWORD_FROM)) {
        throw new CommandSQLParsingException(
            "Keyword " + KEYWORD_FROM + " not found. Use " + getSyntax(), parserText, oldPos);
      }

      pos = nextWord(parserText, parserText, pos, word, true);
      if (pos == -1) {
        throw new CommandSQLParsingException("Invalid role", parserText, oldPos);
      }

      final String roleName = word.toString();
      role = database.getMetadata().getSecurity().getRole(roleName);
      if (role == null) {
        throw new CommandSQLParsingException("Invalid role: " + roleName);
      }
    } finally {
      textRequest.setText(originalQuery);
    }

    return this;
  }

  /**
   * Execute the command.
   */
  public Object execute(final Map<Object, Object> iArgs, DatabaseSessionInternal querySession) {
    if (role == null) {
      throw new CommandExecutionException(
          "Cannot execute the command because it has not yet been parsed");
    }

    var db = getDatabase();
    role.revoke(db, resource, privilege);
    role.save(db);

    return role;
  }

  public String getSyntax() {
    return "REVOKE <permission> ON <resource> FROM <role>";
  }
}
