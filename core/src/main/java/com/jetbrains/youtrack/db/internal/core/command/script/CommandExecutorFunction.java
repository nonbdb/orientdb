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
package com.jetbrains.youtrack.db.internal.core.command.script;

import com.jetbrains.youtrack.db.api.exception.CommandScriptException;
import com.jetbrains.youtrack.db.api.exception.BaseException;
import com.jetbrains.youtrack.db.internal.common.util.CommonConst;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.command.CommandExecutorAbstract;
import com.jetbrains.youtrack.db.internal.core.command.CommandRequest;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.metadata.function.Function;
import com.jetbrains.youtrack.db.internal.core.metadata.security.Role;
import com.jetbrains.youtrack.db.internal.core.metadata.security.Rule;
import java.util.Map;
import java.util.Map.Entry;
import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

/**
 * Executes Script Commands.
 *
 * @see CommandScript
 */
public class CommandExecutorFunction extends CommandExecutorAbstract {

  protected CommandFunction request;

  public CommandExecutorFunction() {
  }

  @SuppressWarnings("unchecked")
  public CommandExecutorFunction parse(final CommandRequest iRequest) {
    request = (CommandFunction) iRequest;
    return this;
  }

  public Object execute(final Map<Object, Object> iArgs, DatabaseSessionInternal querySession) {
    return executeInContext(null, iArgs);
  }

  public Object executeInContext(final CommandContext iContext, final Map<Object, Object> iArgs) {

    parserText = request.getText();

    DatabaseSessionInternal db = iContext.getDatabase();
    final Function f = db.getMetadata().getFunctionLibrary().getFunction(parserText);

    db.checkSecurity(Rule.ResourceGeneric.FUNCTION, Role.PERMISSION_READ, f.getName(db));

    final ScriptManager scriptManager = db.getSharedContext().getYouTrackDB().getScriptManager();

    final ScriptEngine scriptEngine =
        scriptManager.acquireDatabaseEngine(db.getName(), f.getLanguage(db));
    try {
      final Bindings binding =
          scriptManager.bindContextVariables(
              scriptEngine,
              scriptEngine.getBindings(ScriptContext.ENGINE_SCOPE),
              db,
              iContext,
              iArgs);

      try {
        final Object result;

        if (scriptEngine instanceof Invocable invocableEngine) {
          // INVOKE AS FUNCTION. PARAMS ARE PASSED BY POSITION
          Object[] args = null;
          if (iArgs != null) {
            args = new Object[iArgs.size()];
            int i = 0;
            for (Entry<Object, Object> arg : iArgs.entrySet()) {
              args[i++] = arg.getValue();
            }
          } else {
            args = CommonConst.EMPTY_OBJECT_ARRAY;
          }
          result = invocableEngine.invokeFunction(parserText, args);

        } else {
          // INVOKE THE CODE SNIPPET
          final Object[] args = iArgs == null ? null : iArgs.values().toArray();
          result = scriptEngine.eval(scriptManager.getFunctionInvoke(db, f, args), binding);
        }
        return CommandExecutorUtility.transformResult(
            scriptManager.handleResult(f.getLanguage(db), result, scriptEngine, binding, db));

      } catch (ScriptException e) {
        throw BaseException.wrapException(
            new CommandScriptException(
                "Error on execution of the script", request.getText(), e.getColumnNumber()),
            e);
      } catch (NoSuchMethodException e) {
        throw BaseException.wrapException(
            new CommandScriptException("Error on execution of the script", request.getText(), 0),
            e);
      } catch (CommandScriptException e) {
        // PASS THROUGH
        throw e;

      } finally {
        scriptManager.unbind(scriptEngine, binding, iContext, iArgs);
      }
    } finally {
      scriptManager.releaseDatabaseEngine(f.getLanguage(db), db.getName(), scriptEngine);
    }
  }

  public boolean isIdempotent() {
    return false;
  }

  @Override
  protected void throwSyntaxErrorException(String iText) {
    throw new CommandScriptException(
        "Error on execution of the script: " + iText, request.getText(), 0);
  }
}
