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
package com.orientechnologies.orient.core.sql.functions;

import com.orientechnologies.common.collection.OMultiValue;
import com.orientechnologies.common.io.OIOUtils;
import com.orientechnologies.common.parser.OBaseParser;
import com.orientechnologies.orient.core.command.OBasicCommandContext;
import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.command.OCommandExecutorNotFoundException;
import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.db.ODatabaseSessionInternal;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.exception.OCommandExecutionException;
import com.orientechnologies.orient.core.exception.ORecordNotFoundException;
import com.orientechnologies.orient.core.record.ORecord;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.serialization.serializer.OStringSerializerHelper;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.OCommandSQLParsingException;
import com.orientechnologies.orient.core.sql.OSQLEngine;
import com.orientechnologies.orient.core.sql.OSQLHelper;
import com.orientechnologies.orient.core.sql.filter.OSQLFilterItemAbstract;
import com.orientechnologies.orient.core.sql.filter.OSQLFilterItemField;
import com.orientechnologies.orient.core.sql.filter.OSQLFilterItemVariable;
import com.orientechnologies.orient.core.sql.filter.OSQLPredicate;
import java.util.List;
import javax.annotation.Nonnull;

/**
 * Wraps function managing the binding of parameters.
 */
public class OSQLFunctionRuntime extends OSQLFilterItemAbstract {

  public OSQLFunction function;
  public Object[] configuredParameters;
  public Object[] runtimeParameters;

  public OSQLFunctionRuntime(ODatabaseSessionInternal session, final OBaseParser iQueryToParse,
      final String iText) {
    super(session, iQueryToParse, iText);
  }

  public OSQLFunctionRuntime(final OSQLFunction iFunction) {
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
      final OIdentifiable iCurrentRecord,
      final Object iCurrentResult,
      @Nonnull final OCommandContext iContext) {
    // RESOLVE VALUES USING THE CURRENT RECORD
    for (int i = 0; i < configuredParameters.length; ++i) {
      runtimeParameters[i] = configuredParameters[i];

      if (configuredParameters[i] instanceof OSQLFilterItemField) {
        runtimeParameters[i] =
            ((OSQLFilterItemField) configuredParameters[i])
                .getValue(iCurrentRecord, iCurrentResult, iContext);
      } else if (configuredParameters[i] instanceof OSQLFunctionRuntime) {
        runtimeParameters[i] =
            ((OSQLFunctionRuntime) configuredParameters[i])
                .execute(iThis, iCurrentRecord, iCurrentResult, iContext);
      } else if (configuredParameters[i] instanceof OSQLFilterItemVariable) {
        runtimeParameters[i] =
            ((OSQLFilterItemVariable) configuredParameters[i])
                .getValue(iCurrentRecord, iCurrentResult, iContext);
      } else if (configuredParameters[i] instanceof OCommandSQL) {
        try {
          runtimeParameters[i] =
              ((OCommandSQL) configuredParameters[i]).setContext(iContext)
                  .execute(iContext.getDatabase());
        } catch (OCommandExecutorNotFoundException ignore) {
          // TRY WITH SIMPLE CONDITION
          final String text = ((OCommandSQL) configuredParameters[i]).getText();
          final OSQLPredicate pred = new OSQLPredicate(iContext, text);
          runtimeParameters[i] =
              pred.evaluate(
                  iCurrentRecord instanceof ORecord ? iCurrentRecord : null,
                  (ODocument) iCurrentResult,
                  iContext);
          // REPLACE ORIGINAL PARAM
          configuredParameters[i] = pred;
        }
      } else if (configuredParameters[i] instanceof OSQLPredicate) {
        runtimeParameters[i] =
            ((OSQLPredicate) configuredParameters[i])
                .evaluate(
                    iCurrentRecord.getRecord(),
                    (iCurrentRecord instanceof ODocument ? (ODocument) iCurrentResult : null),
                    iContext);
      } else if (configuredParameters[i] instanceof String) {
        if (configuredParameters[i].toString().startsWith("\"")
            || configuredParameters[i].toString().startsWith("'")) {
          runtimeParameters[i] = OIOUtils.getStringContent(configuredParameters[i]);
        }
      }
    }

    var db = iContext.getDatabase();
    if (function.getMaxParams(db) == -1 || function.getMaxParams(db) > 0) {
      if (runtimeParameters.length < function.getMinParams()
          || (function.getMaxParams(db) > -1 && runtimeParameters.length > function.getMaxParams(
          db))) {
        String params;
        if (function.getMinParams() == function.getMaxParams(db)) {
          params = "" + function.getMinParams();
        } else {
          params = function.getMinParams() + "-" + function.getMaxParams(db);
        }
        throw new OCommandExecutionException(
            "Syntax error: function '"
                + function.getName(db)
                + "' needs "
                + params
                + " argument(s) while has been received "
                + runtimeParameters.length);
      }
    }

    final Object functionResult =
        function.execute(iThis, iCurrentRecord, iCurrentResult, runtimeParameters, iContext);

    return transformValue(iCurrentRecord, iContext, functionResult);
  }

  public Object getResult(ODatabaseSessionInternal session) {
    var context = new OBasicCommandContext();
    context.setDatabase(session);

    return transformValue(null, context, function.getResult());
  }

  public void setResult(final Object iValue) {
    function.setResult(iValue);
  }

  @Override
  public Object getValue(
      final OIdentifiable iRecord, Object iCurrentResult, OCommandContext iContext) {
    try {
      final ODocument current = iRecord != null ? (ODocument) iRecord.getRecord() : null;
      return execute(current, current, null, iContext);
    } catch (ORecordNotFoundException rnf) {
      return null;
    }
  }

  @Override
  public String getRoot(ODatabaseSession session) {
    return function.getName(session);
  }

  public OSQLFunctionRuntime setParameters(@Nonnull OCommandContext context,
      final Object[] iParameters, final boolean iEvaluate) {
    this.configuredParameters = new Object[iParameters.length];

    for (int i = 0; i < iParameters.length; ++i) {
      this.configuredParameters[i] = iParameters[i];

      if (iEvaluate) {
        if (iParameters[i] != null) {
          if (iParameters[i] instanceof String) {
            final Object v = OSQLHelper.parseValue(null, null, iParameters[i].toString(), context);
            if (v == OSQLHelper.VALUE_NOT_PARSED
                || (OMultiValue.isMultiValue(v)
                && OMultiValue.getFirstValue(v) == OSQLHelper.VALUE_NOT_PARSED)) {
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
    for (int i = 0; i < configuredParameters.length; ++i) {
      if (!(configuredParameters[i] instanceof OSQLFilterItemField)
          && !(configuredParameters[i] instanceof OSQLFunctionRuntime)) {
        runtimeParameters[i] = configuredParameters[i];
      }
    }

    return this;
  }

  public OSQLFunction getFunction() {
    return function;
  }

  public Object[] getConfiguredParameters() {
    return configuredParameters;
  }

  public Object[] getRuntimeParameters() {
    return runtimeParameters;
  }

  @Override
  protected void setRoot(ODatabaseSessionInternal session, final OBaseParser iQueryToParse,
      final String iText) {
    final int beginParenthesis = iText.indexOf('(');

    // SEARCH FOR THE FUNCTION
    final String funcName = iText.substring(0, beginParenthesis);

    final List<String> funcParamsText = OStringSerializerHelper.getParameters(iText);

    function = OSQLEngine.getInstance().getFunction(funcName);
    if (function == null) {
      throw new OCommandSQLParsingException("Unknown function " + funcName + "()");
    }

    // PARSE PARAMETERS
    this.configuredParameters = new Object[funcParamsText.size()];
    for (int i = 0; i < funcParamsText.size(); ++i) {
      this.configuredParameters[i] = funcParamsText.get(i);
    }

    var context = new OBasicCommandContext();
    context.setDatabase(session);

    setParameters(context, configuredParameters, true);
  }
}
