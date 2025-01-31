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

import com.jetbrains.youtrack.db.api.exception.BaseException;
import com.jetbrains.youtrack.db.api.exception.CommandExecutionException;
import com.jetbrains.youtrack.db.internal.core.command.CommandRequest;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import java.util.Map;

/**
 * Acts as a delegate to the real command inserting the execution of the command inside a new
 * transaction if not yet begun.
 */
public class CommandExecutorSQLTransactional extends CommandExecutorSQLDelegate {

  public static final String KEYWORD_TRANSACTIONAL = "TRANSACTIONAL";

  @SuppressWarnings("unchecked")
  @Override
  public CommandExecutorSQLTransactional parse(DatabaseSessionInternal db,
      CommandRequest iCommand) {
    var cmd = ((CommandSQL) iCommand).getText();
    super.parse(db, new CommandSQL(cmd.substring(KEYWORD_TRANSACTIONAL.length())));
    return this;
  }

  @Override
  public Object execute(DatabaseSessionInternal db, Map<Object, Object> iArgs) {
    var database = getDatabase();
    var txbegun = database.getTransaction() == null || !database.getTransaction().isActive();

    if (txbegun) {
      database.begin();
    }

    try {
      final var result = super.execute(db, iArgs);

      if (txbegun) {
        database.commit();
      }

      return result;
    } catch (Exception e) {
      if (txbegun) {
        database.rollback();
      }
      throw BaseException.wrapException(
          new CommandExecutionException("Transactional command failed"), e);
    }
  }
}
