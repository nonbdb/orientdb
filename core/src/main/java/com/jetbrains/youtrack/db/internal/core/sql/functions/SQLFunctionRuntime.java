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
package com.jetbrains.youtrack.db.internal.core.sql.functions;

import com.jetbrains.youtrack.db.api.DatabaseSession;
import com.jetbrains.youtrack.db.api.exception.CommandExecutionException;
import com.jetbrains.youtrack.db.api.exception.CommandSQLParsingException;
import com.jetbrains.youtrack.db.api.exception.RecordNotFoundException;
import com.jetbrains.youtrack.db.api.record.DBRecord;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.internal.common.collection.MultiValue;
import com.jetbrains.youtrack.db.internal.common.io.IOUtils;
import com.jetbrains.youtrack.db.internal.common.parser.BaseParser;
import com.jetbrains.youtrack.db.internal.core.command.BasicCommandContext;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.command.CommandExecutorNotFoundException;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.StringSerializerHelper;
import com.jetbrains.youtrack.db.internal.core.sql.CommandSQL;
import com.jetbrains.youtrack.db.internal.core.sql.SQLEngine;
import com.jetbrains.youtrack.db.internal.core.sql.SQLHelper;
import com.jetbrains.youtrack.db.internal.core.sql.filter.SQLFilterItemAbstract;
import com.jetbrains.youtrack.db.internal.core.sql.filter.SQLFilterItemField;
import com.jetbrains.youtrack.db.internal.core.sql.filter.SQLFilterItemVariable;
import com.jetbrains.youtrack.db.internal.core.sql.filter.SQLPredicate;
import javax.annotation.Nonnull;

/**
 * Wraps function managing the binding of parameters.
 */
public class SQLFunctionRuntime extends SQLFilterItemAbstract {

  public SQLFunction function;
  public Object[] configuredParameters;
  public Object[] runtimeParameters;

  public SQLFunctionRuntime(DatabaseSessionInternal session, final BaseParser iQueryToParse,
      final String iText) {
    super(session, iQueryToParse, iText);
  }

  public SQLFunctionRuntime(final SQLFunction iFunction) {
    function = iFunction;
  }

  public boolean aggregateResults() {
    return function.aggregateResults();
  }

  public boolean filterResult() {
    return function.filterResult();
  }

  /**
   * Execute a function.
   *
   * @param iCurrentRecord Current record
   * @param iCurrentResult TODO
   * @param iContext
   * @return
   */
  public Object execute(
      final Object iThis,
      final Identifiable iCurrentRecord,
      final Object iCurrentResult,
      @Nonnull final CommandContext iContext) {
    var session = iContext.getDatabaseSession();
    // RESOLVE VALUES USING THE CURRENT RECORD
    for (var i = 0; i < configuredParameters.length; ++i) {
      runtimeParameters[i] = configuredParameters[i];

      if (configuredParameters[i] instanceof SQLFilterItemField) {
        runtimeParameters[i] =
            ((SQLFilterItemField) configuredParameters[i])
                .getValue(iCurrentRecord, iCurrentResult, iContext);
      } else if (configuredParameters[i] instanceof SQLFunctionRuntime) {
        runtimeParameters[i] =
            ((SQLFunctionRuntime) configuredParameters[i])
                .execute(iThis, iCurrentRecord, iCurrentResult, iContext);
      } else if (configuredParameters[i] instanceof SQLFilterItemVariable) {
        runtimeParameters[i] =
            ((SQLFilterItemVariable) configuredParameters[i])
                .getValue(iCurrentRecord, iCurrentResult, iContext);
      } else if (configuredParameters[i] instanceof CommandSQL) {
        try {
          runtimeParameters[i] =
              ((CommandSQL) configuredParameters[i]).setContext(iContext)
                  .execute(iContext.getDatabaseSession());
        } catch (CommandExecutorNotFoundException ignore) {
          // TRY WITH SIMPLE CONDITION
          final var text = ((CommandSQL) configuredParameters[i]).getText();
          final var pred = new SQLPredicate(iContext, text);
          runtimeParameters[i] =
              pred.evaluate(
                  iCurrentRecord instanceof DBRecord ? iCurrentRecord : null,
                  (EntityImpl) iCurrentResult,
                  iContext);
          // REPLACE ORIGINAL PARAM
          configuredParameters[i] = pred;
        }
      } else if (configuredParameters[i] instanceof SQLPredicate) {
        runtimeParameters[i] =
            ((SQLPredicate) configuredParameters[i])
                .evaluate(
                    iCurrentRecord.getRecord(session),
                    (iCurrentRecord instanceof EntityImpl ? (EntityImpl) iCurrentResult : null),
                    iContext);
      } else if (configuredParameters[i] instanceof String) {
        if (configuredParameters[i].toString().startsWith("\"")
            || configuredParameters[i].toString().startsWith("'")) {
          runtimeParameters[i] = IOUtils.getStringContent(configuredParameters[i]);
        }
      }
    }

    if (function.getMaxParams(session) == -1 || function.getMaxParams(session) > 0) {
      if (runtimeParameters.length < function.getMinParams()
          || (function.getMaxParams(session) > -1
          && runtimeParameters.length > function.getMaxParams(
          session))) {
        String params;
        if (function.getMinParams() == function.getMaxParams(session)) {
          params = "" + function.getMinParams();
        } else {
          params = function.getMinParams() + "-" + function.getMaxParams(session);
        }
        throw new CommandExecutionException(session,
            "Syntax error: function '"
                + function.getName(session)
                + "' needs "
                + params
                + " argument(s) while has been received "
                + runtimeParameters.length);
      }
    }

    final var functionResult =
        function.execute(iThis, iCurrentRecord, iCurrentResult, runtimeParameters, iContext);

    return transformValue(iCurrentRecord, iContext, functionResult);
  }

  public Object getResult(DatabaseSessionInternal session) {
    var context = new BasicCommandContext();
    context.setDatabaseSession(session);

    return transformValue(null, context, function.getResult());
  }

  public void setResult(final Object iValue) {
    function.setResult(iValue);
  }

  @Override
  public Object getValue(
      final Identifiable iRecord, Object iCurrentResult, CommandContext iContext) {
    try {
      final var current =
          iRecord != null ? (EntityImpl) iRecord.getRecord(iContext.getDatabaseSession()) : null;
      return execute(current, current, null, iContext);
    } catch (RecordNotFoundException rnf) {
      return null;
    }
  }

  @Override
  public String getRoot(DatabaseSession session) {
    return function.getName(session);
  }

  public SQLFunctionRuntime setParameters(@Nonnull CommandContext context,
      final Object[] iParameters, final boolean iEvaluate) {
    this.configuredParameters = new Object[iParameters.length];

    for (var i = 0; i < iParameters.length; ++i) {
      this.configuredParameters[i] = iParameters[i];

      if (iEvaluate) {
        if (iParameters[i] != null) {
          if (iParameters[i] instanceof String) {
            final var v = SQLHelper.parseValue(null, null, iParameters[i].toString(), context);
            if (v == SQLHelper.VALUE_NOT_PARSED
                || (MultiValue.isMultiValue(v)
                && MultiValue.getFirstValue(v) == SQLHelper.VALUE_NOT_PARSED)) {
              continue;
            }

            configuredParameters[i] = v;
          }
        } else {
          this.configuredParameters[i] = null;
        }
      }
    }

    function.config(configuredParameters);

    // COPY STATIC VALUES
    this.runtimeParameters = new Object[configuredParameters.length];
    for (var i = 0; i < configuredParameters.length; ++i) {
      if (!(configuredParameters[i] instanceof SQLFilterItemField)
          && !(configuredParameters[i] instanceof SQLFunctionRuntime)) {
        runtimeParameters[i] = configuredParameters[i];
      }
    }

    return this;
  }

  public SQLFunction getFunction() {
    return function;
  }

  public Object[] getConfiguredParameters() {
    return configuredParameters;
  }

  public Object[] getRuntimeParameters() {
    return runtimeParameters;
  }

  @Override
  protected void setRoot(DatabaseSessionInternal session, final BaseParser iQueryToParse,
      final String iText) {
    final var beginParenthesis = iText.indexOf('(');

    // SEARCH FOR THE FUNCTION
    final var funcName = iText.substring(0, beginParenthesis);

    final var funcParamsText = StringSerializerHelper.getParameters(iText);

    function = SQLEngine.getInstance().getFunction(session, funcName);
    if (function == null) {
      throw new CommandSQLParsingException(session.getDatabaseName(),
          "Unknown function " + funcName + "()");
    }

    // PARSE PARAMETERS
    this.configuredParameters = new Object[funcParamsText.size()];
    for (var i = 0; i < funcParamsText.size(); ++i) {
      this.configuredParameters[i] = funcParamsText.get(i);
    }

    var context = new BasicCommandContext();
    context.setDatabaseSession(session);

    setParameters(context, configuredParameters, true);
  }
}
