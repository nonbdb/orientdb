/*
 *
 * Copyright 2013 Geomatys.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.youtrack.db.internal.core.sql.method.sequence;

import com.jetbrains.youtrack.db.api.exception.CommandExecutionException;
import com.jetbrains.youtrack.db.api.exception.CommandSQLParsingException;
import com.jetbrains.youtrack.db.api.exception.DatabaseException;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.internal.common.log.LogManager;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.metadata.sequence.DBSequence;
import com.jetbrains.youtrack.db.internal.core.sql.method.misc.AbstractSQLMethod;

/**
 * Returns the current number of a sequence.
 */
public class SQLMethodCurrent extends AbstractSQLMethod {

  public static final String NAME = "current";

  public SQLMethodCurrent() {
    super(NAME, 0, 0);
  }

  @Override
  public String getSyntax() {
    return "current()";
  }

  @Override
  public Object execute(
      Object iThis,
      Identifiable iCurrentRecord,
      CommandContext iContext,
      Object ioResult,
      Object[] iParams) {
    if (iThis == null) {
      throw new CommandSQLParsingException(
          "Method 'current()' can be invoked only on OSequence instances, while NULL was found");
    }

    if (!(iThis instanceof DBSequence)) {
      throw new CommandSQLParsingException(
          "Method 'current()' can be invoked only on OSequence instances, while '"
              + iThis.getClass()
              + "' was found");
    }

    try {
      return ((DBSequence) iThis).current(iContext.getDatabase());
    } catch (DatabaseException exc) {
      String message = "Unable to execute command: " + exc.getMessage();
      LogManager.instance().error(this, message, exc, (Object) null);
      throw new CommandExecutionException(message);
    }
  }
}
