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
package com.jetbrains.youtrack.db.internal.core.sql.filter;

import com.jetbrains.youtrack.db.api.DatabaseSession;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.api.schema.Collate;
import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import com.jetbrains.youtrack.db.api.schema.SchemaProperty;
import com.jetbrains.youtrack.db.internal.common.parser.BaseParser;
import com.jetbrains.youtrack.db.internal.common.util.CommonConst;
import com.jetbrains.youtrack.db.internal.common.util.Pair;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseRecordThreadLocal;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.exception.QueryParsingException;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.StringSerializerHelper;
import com.jetbrains.youtrack.db.internal.core.sql.SQLEngine;
import com.jetbrains.youtrack.db.internal.core.sql.functions.SQLFunction;
import com.jetbrains.youtrack.db.internal.core.sql.functions.coll.SQLMethodMultiValue;
import com.jetbrains.youtrack.db.internal.core.sql.method.SQLMethod;
import com.jetbrains.youtrack.db.internal.core.sql.method.SQLMethodRuntime;
import com.jetbrains.youtrack.db.internal.core.sql.method.misc.SQLMethodField;
import com.jetbrains.youtrack.db.internal.core.sql.method.misc.SQLMethodFunctionDelegate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nonnull;

/**
 * Represents an object field as value in the query condition.
 */
public abstract class SQLFilterItemAbstract implements SQLFilterItem {

  protected List<Pair<SQLMethodRuntime, Object[]>> operationsChain = null;

  protected SQLFilterItemAbstract() {
  }

  public SQLFilterItemAbstract(DatabaseSessionInternal session, final BaseParser iQueryToParse,
      final String iText) {
    final List<String> parts =
        StringSerializerHelper.smartSplit(
            iText,
            new char[]{'.', '[', ']'},
            new boolean[]{false, false, true},
            new boolean[]{false, true, false},
            0,
            -1,
            false,
            true,
            false,
            false,
            CommonConst.EMPTY_CHAR_ARRAY);

    setRoot(session, iQueryToParse, parts.get(0));

    if (parts.size() > 1) {
      operationsChain = new ArrayList<Pair<SQLMethodRuntime, Object[]>>();

      // GET ALL SPECIAL OPERATIONS
      for (int i = 1; i < parts.size(); ++i) {
        final String part = parts.get(i);

        final int pindex = part.indexOf('(');
        if (part.charAt(0) == '[') {
          operationsChain.add(
              new Pair<SQLMethodRuntime, Object[]>(
                  new SQLMethodRuntime(SQLEngine.getMethod(SQLMethodMultiValue.NAME)),
                  new Object[]{part}));
        } else if (pindex > -1) {
          final String methodName = part.substring(0, pindex).trim().toLowerCase(Locale.ENGLISH);

          SQLMethod method = SQLEngine.getMethod(methodName);
          final Object[] arguments;
          if (method != null) {
            if (method.getMaxParams(session) == -1 || method.getMaxParams(session) > 0) {
              arguments = StringSerializerHelper.getParameters(part).toArray();
              if (arguments.length < method.getMinParams()
                  || (method.getMaxParams(session) > -1 && arguments.length > method.getMaxParams(
                  session))) {
                String params;
                if (method.getMinParams() == method.getMaxParams(session)) {
                  params = "" + method.getMinParams();
                } else {
                  params = method.getMinParams() + "-" + method.getMaxParams(session);
                }
                throw new QueryParsingException(
                    iQueryToParse.parserText,
                    "Syntax error: field operator '"
                        + method.getName()
                        + "' needs "
                        + params
                        + " argument(s) while has been received "
                        + arguments.length,
                    0);
              }
            } else {
              arguments = null;
            }

          } else {
            // LOOK FOR FUNCTION
            final SQLFunction f = SQLEngine.getInstance().getFunction(session, methodName);

            if (f == null)
            // ERROR: METHOD/FUNCTION NOT FOUND OR MISPELLED
            {
              throw new QueryParsingException(
                  iQueryToParse.parserText,
                  "Syntax error: function or field operator not recognized between the supported"
                      + " ones: "
                      + SQLEngine.getMethodNames(),
                  0);
            }

            if (f.getMaxParams(session) == -1 || f.getMaxParams(session) > 0) {
              arguments = StringSerializerHelper.getParameters(part).toArray();
              if (arguments.length + 1 < f.getMinParams()
                  || (f.getMaxParams(session) > -1 && arguments.length + 1 > f.getMaxParams(
                  session))) {
                String params;
                if (f.getMinParams() == f.getMaxParams(session)) {
                  params = "" + f.getMinParams();
                } else {
                  params = f.getMinParams() + "-" + f.getMaxParams(session);
                }
                throw new QueryParsingException(
                    iQueryToParse.parserText,
                    "Syntax error: function '"
                        + f.getName(session)
                        + "' needs "
                        + params
                        + " argument(s) while has been received "
                        + arguments.length,
                    0);
              }
            } else {
              arguments = null;
            }

            method = new SQLMethodFunctionDelegate(f);
          }

          final SQLMethodRuntime runtimeMethod = new SQLMethodRuntime(method);

          // SPECIAL OPERATION FOUND: ADD IT IN TO THE CHAIN
          operationsChain.add(new Pair<SQLMethodRuntime, Object[]>(runtimeMethod, arguments));

        } else {
          operationsChain.add(
              new Pair<SQLMethodRuntime, Object[]>(
                  new SQLMethodRuntime(SQLEngine.getMethod(SQLMethodField.NAME)),
                  new Object[]{part}));
        }
      }
    }
  }

  public abstract String getRoot(DatabaseSession session);

  public Object transformValue(
      final Identifiable iRecord, @Nonnull final CommandContext iContext, Object ioResult) {
    if (ioResult != null && operationsChain != null) {
      // APPLY OPERATIONS FOLLOWING THE STACK ORDER
      SQLMethodRuntime method = null;

      for (Pair<SQLMethodRuntime, Object[]> op : operationsChain) {
        method = op.getKey();

        // DON'T PASS THE CURRENT RECORD TO FORCE EVALUATING TEMPORARY RESULT
        method.setParameters(iContext.getDatabase(), op.getValue(), true);

        ioResult = method.execute(ioResult, iRecord, ioResult, iContext);
      }
    }

    return ioResult;
  }

  public boolean hasChainOperators() {
    return operationsChain != null;
  }

  public Pair<SQLMethodRuntime, Object[]> getLastChainOperator() {
    if (operationsChain != null) {
      return operationsChain.get(operationsChain.size() - 1);
    }

    return null;
  }

  @Override
  public String toString() {
    var db = DatabaseRecordThreadLocal.instance().getIfDefined();
    if (db != null) {
      final StringBuilder buffer = new StringBuilder(128);
      final String root = getRoot(db);
      if (root != null) {
        buffer.append(root);
      }
      if (operationsChain != null) {
        for (Pair<SQLMethodRuntime, Object[]> op : operationsChain) {
          buffer.append('.');
          buffer.append(op.getKey());
          if (op.getValue() != null) {
            final Object[] values = op.getValue();
            buffer.append('(');
            int i = 0;
            for (Object v : values) {
              if (i++ > 0) {
                buffer.append(',');
              }
              buffer.append(v);
            }
            buffer.append(')');
          }
        }
      }
      return buffer.toString();
    }

    return super.toString();
  }

  protected abstract void setRoot(DatabaseSessionInternal session, BaseParser iQueryToParse,
      final String iRoot);

  protected Collate getCollateForField(final SchemaClass iClass, final String iFieldName) {
    if (iClass != null) {
      final SchemaProperty p = iClass.getProperty(iFieldName);
      if (p != null) {
        return p.getCollate();
      }
    }
    return null;
  }
}
