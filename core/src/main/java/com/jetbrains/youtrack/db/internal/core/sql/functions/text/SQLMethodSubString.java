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
package com.jetbrains.youtrack.db.internal.core.sql.functions.text;

import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.internal.core.sql.method.misc.AbstractSQLMethod;

/**
 * Extracts a sub string from the original.
 */
public class SQLMethodSubString extends AbstractSQLMethod {

  public static final String NAME = "substring";

  public SQLMethodSubString() {
    super(NAME, 1, 2);
  }

  @Override
  public String getSyntax() {
    return "subString(<from-index> [,<to-index>])";
  }

  @Override
  public Object execute(
      Object iThis,
      Identifiable iCurrentRecord,
      CommandContext iContext,
      Object ioResult,
      Object[] iParams) {
    if (iThis == null || iParams[0] == null) {
      return null;
    }

    if (iParams.length > 1) {
      var from = Integer.parseInt(iParams[0].toString());
      var to = Integer.parseInt(iParams[1].toString());
      var thisString = iThis.toString();
      if (from < 0) {
        from = 0;
      }
      if (from >= thisString.length()) {
        return "";
      }
      if (to > thisString.length()) {
        to = thisString.length();
      }
      if (to <= from) {
        return "";
      }

      return thisString.substring(from, to);
    } else {
      var from = Integer.parseInt(iParams[0].toString());
      var thisString = iThis.toString();
      if (from < 0) {
        from = 0;
      }
      if (from >= thisString.length()) {
        return "";
      }
      return thisString.substring(from);
    }
  }
}
