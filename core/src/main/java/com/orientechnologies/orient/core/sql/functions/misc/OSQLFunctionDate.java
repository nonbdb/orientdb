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
package com.orientechnologies.orient.core.sql.functions.misc;

import com.orientechnologies.common.exception.YTException;
import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.orientechnologies.orient.core.db.YTDatabaseSession;
import com.orientechnologies.orient.core.db.record.YTIdentifiable;
import com.orientechnologies.orient.core.exception.YTQueryParsingException;
import com.orientechnologies.orient.core.sql.functions.OSQLFunctionAbstract;
import com.orientechnologies.orient.core.util.ODateHelper;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Builds a date object from the format passed. If no arguments are passed, than the system date is
 * built (like sysdate() function)
 *
 * @see OSQLFunctionSysdate
 */
public class OSQLFunctionDate extends OSQLFunctionAbstract {

  public static final String NAME = "date";

  private final Date date;
  private DateFormat format;

  /**
   * Get the date at construction to have the same date for all the iteration.
   */
  public OSQLFunctionDate() {
    super(NAME, 0, 3);
    date = new Date();
  }

  public Object execute(
      Object iThis,
      final YTIdentifiable iCurrentRecord,
      final Object iCurrentResult,
      final Object[] iParams,
      OCommandContext iContext) {
    if (iParams.length == 0) {
      return date;
    }

    if (iParams[0] == null) {
      return null;
    }

    if (iParams[0] instanceof Number) {
      return new Date(((Number) iParams[0]).longValue());
    }

    if (format == null) {
      if (iParams.length > 1) {
        format = new SimpleDateFormat((String) iParams[1]);
        format.setTimeZone(ODateHelper.getDatabaseTimeZone());
      } else {
        format = ODateHelper.getDateTimeFormatInstance(ODatabaseRecordThreadLocal.instance().get());
      }

      if (iParams.length == 3) {
        format.setTimeZone(TimeZone.getTimeZone(iParams[2].toString()));
      }
    }

    try {
      return format.parse((String) iParams[0]);
    } catch (ParseException e) {
      throw YTException.wrapException(
          new YTQueryParsingException(
              "Error on formatting date '"
                  + iParams[0]
                  + "' using the format: "
                  + ((SimpleDateFormat) format).toPattern()),
          e);
    }
  }

  public boolean aggregateResults(final Object[] configuredParameters) {
    return false;
  }

  public String getSyntax(YTDatabaseSession session) {
    return "date([<date-as-string>] [,<format>] [,<timezone>])";
  }

  @Override
  public Object getResult() {
    format = null;
    return null;
  }
}
