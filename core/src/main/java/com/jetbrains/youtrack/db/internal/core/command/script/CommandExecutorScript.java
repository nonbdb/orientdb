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
import com.jetbrains.youtrack.db.internal.common.collection.MultiValue;
import com.jetbrains.youtrack.db.internal.common.concur.NeedRetryException;
import com.jetbrains.youtrack.db.api.exception.BaseException;
import com.jetbrains.youtrack.db.internal.common.io.IOUtils;
import com.jetbrains.youtrack.db.internal.common.log.LogManager;
import com.jetbrains.youtrack.db.internal.common.parser.ContextVariableResolver;
import com.jetbrains.youtrack.db.internal.core.command.BasicCommandContext;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.command.CommandExecutorAbstract;
import com.jetbrains.youtrack.db.internal.core.command.CommandRequest;
import com.jetbrains.youtrack.db.internal.core.command.CommandDistributedReplicateRequest;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseRecordThreadLocal;
import com.jetbrains.youtrack.db.api.DatabaseSession;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.api.exception.CommandExecutionException;
import com.jetbrains.youtrack.db.api.exception.RecordNotFoundException;
import com.jetbrains.youtrack.db.api.exception.TransactionException;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.StringSerializerHelper;
import com.jetbrains.youtrack.db.internal.core.sql.CommandSQL;
import com.jetbrains.youtrack.db.api.exception.CommandSQLParsingException;
import com.jetbrains.youtrack.db.internal.core.sql.SQLEngine;
import com.jetbrains.youtrack.db.internal.core.sql.TemporaryRidGenerator;
import com.jetbrains.youtrack.db.internal.core.sql.filter.SQLFilter;
import com.jetbrains.youtrack.db.internal.core.sql.filter.SQLPredicate;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLIfStatement;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLStatement;
import com.jetbrains.youtrack.db.internal.core.sql.parser.ParseException;
import com.jetbrains.youtrack.db.internal.core.sql.parser.YouTrackDBSql;
import com.jetbrains.youtrack.db.internal.core.sql.query.LegacyResultSet;
import com.jetbrains.youtrack.db.api.exception.RecordDuplicatedException;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

/**
 * Executes Script Commands.
 *
 * @see CommandScript
 */
public class CommandExecutorScript extends CommandExecutorAbstract
    implements CommandDistributedReplicateRequest, TemporaryRidGenerator {

  private static final int MAX_DELAY = 100;
  protected CommandScript request;
  protected DISTRIBUTED_EXECUTION_MODE executionMode = DISTRIBUTED_EXECUTION_MODE.LOCAL;
  protected AtomicInteger serialTempRID = new AtomicInteger(0);

  public CommandExecutorScript() {
  }

  @SuppressWarnings("unchecked")
  public CommandExecutorScript parse(final CommandRequest iRequest) {
    request = (CommandScript) iRequest;
    executionMode = ((CommandScript) iRequest).getExecutionMode();
    return this;
  }

  public CommandDistributedReplicateRequest.DISTRIBUTED_EXECUTION_MODE
  getDistributedExecutionMode() {
    return executionMode;
  }

  public Object execute(final Map<Object, Object> iArgs, DatabaseSessionInternal querySession) {
    if (context == null) {
      context = new BasicCommandContext();
    }
    return executeInContext(context, iArgs);
  }

  public Object executeInContext(final CommandContext iContext, final Map<Object, Object> iArgs) {
    final String language = request.getLanguage();
    parserText = request.getText();
    parameters = iArgs;

    parameters = iArgs;
    if (language.equalsIgnoreCase("SQL")) {
      // SPECIAL CASE: EXECUTE THE COMMANDS IN SEQUENCE
      try {
        parserText = preParse(parserText, iArgs);
      } catch (ParseException e) {
        throw BaseException.wrapException(
            new CommandExecutionException("Invalid script:" + e.getMessage()), e);
      }
      return executeSQL();
    } else {
      return executeJsr223Script(language, iContext, iArgs);
    }
  }

  private String preParse(String parserText, final Map<Object, Object> iArgs)
      throws ParseException {
    final boolean strict = getDatabase().getStorageInfo().getConfiguration().isStrictSql();
    if (strict) {
      parserText = addSemicolons(parserText);

      DatabaseSessionInternal db = getDatabase();

      byte[] bytes;
      try {
        if (db == null) {
          bytes = parserText.getBytes();
        } else {
          bytes =
              parserText.getBytes(getDatabase().getStorageInfo().getConfiguration().getCharset());
        }
      } catch (UnsupportedEncodingException e) {
        LogManager.instance()
            .warn(
                this,
                "Invalid charset for database "
                    + getDatabase()
                    + " "
                    + getDatabase().getStorageInfo().getConfiguration().getCharset());

        bytes = parserText.getBytes();
      }

      InputStream is = new ByteArrayInputStream(bytes);

      YouTrackDBSql osql = null;
      try {

        if (db == null) {
          osql = new YouTrackDBSql(is);
        } else {
          osql = new YouTrackDBSql(is, db.getStorageInfo().getConfiguration().getCharset());
        }
      } catch (UnsupportedEncodingException e) {
        LogManager.instance()
            .warn(
                this,
                "Invalid charset for database "
                    + getDatabase()
                    + " "
                    + getDatabase().getStorageInfo().getConfiguration().getCharset());
        osql = new YouTrackDBSql(is);
      }
      List<SQLStatement> statements = osql.parseScript();
      StringBuilder result = new StringBuilder();
      for (SQLStatement stm : statements) {
        stm.toString(iArgs, result);
        if (!(stm instanceof SQLIfStatement)) {
          result.append(";");
        }
        result.append("\n");
      }
      return result.toString();
    } else {
      return parserText;
    }
  }

  private String addSemicolons(String parserText) {
    String[] rows = parserText.split("\n");
    StringBuilder builder = new StringBuilder();
    for (String row : rows) {
      row = row.trim();
      builder.append(row);
      if (!(row.endsWith(";") || row.endsWith("{"))) {
        builder.append(";");
      }
      builder.append("\n");
    }
    return builder.toString();
  }

  public boolean isIdempotent() {
    return false;
  }

  protected Object executeJsr223Script(
      final String language, final CommandContext iContext, final Map<Object, Object> iArgs) {
    DatabaseSessionInternal db = iContext.getDatabase();

    final ScriptManager scriptManager = db.getSharedContext().getYouTrackDB().getScriptManager();
    CompiledScript compiledScript = request.getCompiledScript();

    final ScriptEngine scriptEngine = scriptManager.acquireDatabaseEngine(db.getName(), language);
    try {

      if (compiledScript == null) {
        if (!(scriptEngine instanceof Compilable c)) {
          throw new CommandExecutionException(
              "Language '" + language + "' does not support compilation");
        }

        try {
          compiledScript = c.compile(parserText);
        } catch (ScriptException e) {
          scriptManager.throwErrorMessage(e, parserText);
        }

        request.setCompiledScript(compiledScript);
      }

      final Bindings binding =
          scriptManager.bind(
              scriptEngine,
              compiledScript.getEngine().getBindings(ScriptContext.ENGINE_SCOPE),
              db,
              iContext,
              iArgs);

      try {
        final Object ob = compiledScript.eval(binding);

        return CommandExecutorUtility.transformResult(ob);
      } catch (ScriptException e) {
        throw BaseException.wrapException(
            new CommandScriptException(
                "Error on execution of the script", request.getText(), e.getColumnNumber()),
            e);

      } finally {
        scriptManager.unbind(scriptEngine, binding, iContext, iArgs);
      }
    } finally {
      scriptManager.releaseDatabaseEngine(language, db.getName(), scriptEngine);
    }
  }

  // TODO: CREATE A REGULAR JSR223 SCRIPT IMPL
  protected Object executeSQL() {
    var db = DatabaseRecordThreadLocal.instance().getIfDefined();
    try {

      return executeSQLScript(parserText, db);

    } catch (IOException e) {
      throw BaseException.wrapException(
          new CommandExecutionException("Error on executing command: " + parserText), e);
    }
  }

  @Override
  protected void throwSyntaxErrorException(String iText) {
    throw new CommandScriptException(
        "Error on execution of the script: " + iText, request.getText(), 0);
  }

  protected Object executeSQLScript(final String iText, final DatabaseSessionInternal db)
      throws IOException {
    Object lastResult = null;
    int maxRetry = 1;

    context.setVariable("transactionRetries", 0);
    context.setVariable("parentQuery", this);

    for (int retry = 1; retry <= maxRetry; retry++) {
      try {
        try {
          int txBegunAtLine = -1;
          int txBegunAtPart = -1;
          lastResult = null;
          int nestedLevel = 0;
          int skippingScriptsAtNestedLevel = -1;

          final BufferedReader reader = new BufferedReader(new StringReader(iText));

          int line = 0;
          int linePart = 0;
          String lastLine;
          boolean txBegun = false;

          for (; line < txBegunAtLine; ++line)
          // SKIP PREVIOUS COMMAND AND JUMP TO THE BEGIN IF ANY
          {
            reader.readLine();
          }

          for (; (lastLine = reader.readLine()) != null; ++line) {
            lastLine = lastLine.trim();

            // this block is here (and not below, with the other conditions)
            // just because of the smartSprit() that does not parse correctly a single bracket

            // final List<String> lineParts = StringSerializerHelper.smartSplit(lastLine, ';',
            // true);
            final List<String> lineParts = splitBySemicolon(lastLine);

            if (line == txBegunAtLine)
            // SKIP PREVIOUS COMMAND PART AND JUMP TO THE BEGIN IF ANY
            {
              linePart = txBegunAtPart;
            } else {
              linePart = 0;
            }

            boolean breakReturn = false;

            for (; linePart < lineParts.size(); ++linePart) {
              final String lastCommand = lineParts.get(linePart);

              if (isIfCondition(lastCommand)) {
                nestedLevel++;
                if (skippingScriptsAtNestedLevel >= 0) {
                  continue; // I'm in an (outer) IF that did not match the condition
                }
                boolean ifResult = evaluateIfCondition(lastCommand);
                if (!ifResult) {
                  // if does not match the condition, skip all the inner statements
                  skippingScriptsAtNestedLevel = nestedLevel;
                }
                continue;
              } else if (lastCommand.equals("}")) {
                nestedLevel--;
                if (skippingScriptsAtNestedLevel > nestedLevel) {
                  skippingScriptsAtNestedLevel = -1;
                }
                continue;
              } else if (skippingScriptsAtNestedLevel >= 0) {
                continue; // I'm in an IF that did not match the condition
              } else if (StringSerializerHelper.startsWithIgnoreCase(lastCommand, "let ")) {
                lastResult = executeLet(lastCommand, db);

              } else if (StringSerializerHelper.startsWithIgnoreCase(lastCommand, "begin")) {

                if (txBegun) {
                  throw new CommandSQLParsingException("Transaction already begun");
                }

                if (db.getTransaction().isActive())
                // COMMIT ANY ACTIVE TX
                {
                  db.commit();
                }

                txBegun = true;
                txBegunAtLine = line;
                txBegunAtPart = linePart;

                db.begin();
              } else if ("rollback".equalsIgnoreCase(lastCommand)) {

                if (!txBegun) {
                  throw new CommandSQLParsingException("Transaction not begun");
                }

                db.rollback();

                txBegun = false;
                txBegunAtLine = -1;
                txBegunAtPart = -1;

              } else if (StringSerializerHelper.startsWithIgnoreCase(lastCommand, "commit")) {
                if (txBegunAtLine < 0) {
                  throw new CommandSQLParsingException("Transaction not begun");
                }

                if (retry == 1 && lastCommand.length() > "commit ".length()) {
                  // FIRST CYCLE: PARSE RETRY TIMES OVERWRITING DEFAULT = 1
                  String next = lastCommand.substring("commit ".length()).trim();
                  if (StringSerializerHelper.startsWithIgnoreCase(next, "retry ")) {
                    next = next.substring("retry ".length()).trim();
                    maxRetry = Integer.parseInt(next);
                  }
                }

                db.commit();

                txBegun = false;
                txBegunAtLine = -1;
                txBegunAtPart = -1;

              } else if (StringSerializerHelper.startsWithIgnoreCase(lastCommand, "sleep ")) {
                executeSleep(lastCommand);

              } else if (StringSerializerHelper.startsWithIgnoreCase(
                  lastCommand, "console.log ")) {
                executeConsoleLog(lastCommand, db);

              } else if (StringSerializerHelper.startsWithIgnoreCase(
                  lastCommand, "console.output ")) {
                executeConsoleOutput(lastCommand, db);

              } else if (StringSerializerHelper.startsWithIgnoreCase(
                  lastCommand, "console.error ")) {
                executeConsoleError(lastCommand, db);

              } else if (StringSerializerHelper.startsWithIgnoreCase(lastCommand, "return ")) {
                lastResult = getValue(lastCommand.substring("return ".length()), db);

                // END OF SCRIPT
                breakReturn = true;
                break;

              } else if (lastCommand != null && lastCommand.length() > 0) {
                lastResult = executeCommand(lastCommand, db);
              }
            }
            if (breakReturn) {
              break;
            }
          }
        } catch (RuntimeException ex) {
          if (db.getTransaction().isActive()) {
            db.rollback();
          }
          throw ex;
        }

        // COMPLETED
        break;

      } catch (TransactionException e) {
        // THIS CASE IS ON UPSERT
        context.setVariable("retries", retry);
        if (retry >= maxRetry) {
          throw e;
        }

        waitForNextRetry();

      } catch (RecordDuplicatedException e) {
        // THIS CASE IS ON UPSERT
        context.setVariable("retries", retry);
        if (retry >= maxRetry) {
          throw e;
        }

        waitForNextRetry();

      } catch (RecordNotFoundException e) {
        // THIS CASE IS ON UPSERT
        context.setVariable("retries", retry);
        if (retry >= maxRetry) {
          throw e;
        }

      } catch (NeedRetryException e) {
        context.setVariable("retries", retry);
        if (retry >= maxRetry) {
          throw e;
        }

        waitForNextRetry();
      }
    }

    return lastResult;
  }

  private List<String> splitBySemicolon(String lastLine) {
    if (lastLine == null) {
      return Collections.EMPTY_LIST;
    }
    List<String> result = new ArrayList<String>();
    Character prev = null;
    Character lastQuote = null;
    StringBuilder buffer = new StringBuilder();
    for (char c : lastLine.toCharArray()) {
      if (c == ';' && lastQuote == null) {
        if (buffer.toString().trim().length() > 0) {
          result.add(buffer.toString().trim());
        }
        buffer = new StringBuilder();
        prev = null;
        continue;
      }
      if ((c == '"' || c == '\'') && (prev == null || !prev.equals('\\'))) {
        if (lastQuote != null && lastQuote.equals(c)) {
          lastQuote = null;
        } else if (lastQuote == null) {
          lastQuote = c;
        }
      }
      buffer.append(c);
      prev = c;
    }
    if (buffer.toString().trim().length() > 0) {
      result.add(buffer.toString().trim());
    }
    return result;
  }

  private boolean evaluateIfCondition(String lastCommand) {
    String cmd = lastCommand;
    cmd = cmd.trim().substring(2); // remove IF
    cmd = cmd.trim().substring(0, cmd.trim().length() - 1); // remove {
    SQLFilter condition = SQLEngine.parseCondition(cmd, getContext(), "IF");
    Object result = null;
    try {
      result = condition.evaluate(null, null, getContext());
    } catch (Exception e) {
      throw BaseException.wrapException(
          new CommandExecutionException(
              "Could not evaluate IF condition: " + cmd + " - " + e.getMessage()),
          e);
    }

    return Boolean.TRUE.equals(result);
  }

  private boolean isIfCondition(String iCommand) {
    if (iCommand == null) {
      return false;
    }
    String cmd = iCommand.trim();
    if (cmd.length() < 3) {
      return false;
    }
    if (!((StringSerializerHelper.startsWithIgnoreCase(cmd, "if "))
        || StringSerializerHelper.startsWithIgnoreCase(cmd, "if("))) {
      return false;
    }
    return cmd.endsWith("{");
  }

  /**
   * Wait before to retry
   */
  protected void waitForNextRetry() {
    try {
      Thread.sleep(new Random().nextInt(MAX_DELAY - 1) + 1);
    } catch (InterruptedException e) {
      LogManager.instance().error(this, "Wait was interrupted", e);
    }
  }

  private Object executeCommand(final String lastCommand, final DatabaseSession db) {
    final CommandSQL command = new CommandSQL(lastCommand);
    var database = (DatabaseSessionInternal) db;
    Object result =
        database
            .command(command.setContext(getContext()))
            .execute(database, toMap(parameters));
    request.setFetchPlan(command.getFetchPlan());
    return result;
  }

  private Object toMap(Object parameters) {
    if (parameters instanceof SimpleBindings) {
      HashMap<Object, Object> result = new LinkedHashMap<Object, Object>();
      result.putAll((SimpleBindings) parameters);
      return result;
    }
    return parameters;
  }

  private Object getValue(final String iValue, final DatabaseSessionInternal db) {
    Object lastResult = null;
    boolean recordResultSet = true;
    if (iValue.equalsIgnoreCase("NULL")) {
      lastResult = null;
    } else if (iValue.startsWith("[") && iValue.endsWith("]")) {
      // ARRAY - COLLECTION
      final List<String> items = new ArrayList<String>();

      StringSerializerHelper.getCollection(iValue, 0, items);
      final List<Object> result = new ArrayList<Object>(items.size());

      for (int i = 0; i < items.size(); ++i) {
        String item = items.get(i);

        result.add(getValue(item, db));
      }
      lastResult = result;
      checkIsRecordResultSet(lastResult);
    } else if (iValue.startsWith("{") && iValue.endsWith("}")) {
      // MAP
      final Map<String, String> map = StringSerializerHelper.getMap(db, iValue);
      final Map<Object, Object> result = new HashMap<Object, Object>(map.size());

      for (Map.Entry<String, String> entry : map.entrySet()) {
        // KEY
        String stringKey = entry.getKey();
        if (stringKey == null) {
          continue;
        }

        stringKey = stringKey.trim();

        Object key;
        if (stringKey.startsWith("$")) {
          key = getContext().getVariable(stringKey);
        } else {
          key = stringKey;
        }

        if (MultiValue.isMultiValue(key) && MultiValue.getSize(key) == 1) {
          key = MultiValue.getFirstValue(key);
        }

        // VALUE
        String stringValue = entry.getValue();
        if (stringValue == null) {
          continue;
        }

        stringValue = stringValue.trim();

        Object value;
        if (stringValue.startsWith("$")) {
          value = getContext().getVariable(stringValue);
        } else {
          value = stringValue;
        }

        result.put(key, value);
      }
      lastResult = result;
      checkIsRecordResultSet(lastResult);
    } else if (iValue.startsWith("\"") && iValue.endsWith("\"")
        || iValue.startsWith("'") && iValue.endsWith("'")) {
      lastResult = new ContextVariableResolver(context).parse(IOUtils.getStringContent(iValue));
      checkIsRecordResultSet(lastResult);
    } else if (iValue.startsWith("(") && iValue.endsWith(")")) {
      lastResult = executeCommand(iValue, db);
    } else {
      var context = getContext();
      lastResult = new SQLPredicate(getContext(), iValue).evaluate(context);
    }
    // END OF THE SCRIPT
    return lastResult;
  }

  private void checkIsRecordResultSet(Object result) {
    if (!(result instanceof Identifiable) && !(result instanceof LegacyResultSet)) {
      if (!MultiValue.isMultiValue(result)) {
        request.setRecordResultSet(false);
      } else {
        for (Object val : MultiValue.getMultiValueIterable(result)) {
          if (!(val instanceof Identifiable)) {
            request.setRecordResultSet(false);
          }
        }
      }
    }
  }

  private void executeSleep(String lastCommand) {
    final String sleepTimeInMs = lastCommand.substring("sleep ".length()).trim();
    try {
      Thread.sleep(Integer.parseInt(sleepTimeInMs));
    } catch (InterruptedException e) {
      LogManager.instance().debug(this, "Sleep was interrupted in SQL batch", e);
    }
  }

  private void executeConsoleLog(final String lastCommand, final DatabaseSessionInternal db) {
    final String value = lastCommand.substring("console.log ".length()).trim();
    LogManager.instance().info(this, "%s", getValue(IOUtils.wrapStringContent(value, '\''), db));
  }

  private void executeConsoleOutput(final String lastCommand, final DatabaseSessionInternal db) {
    final String value = lastCommand.substring("console.output ".length()).trim();
    System.out.println(getValue(IOUtils.wrapStringContent(value, '\''), db));
  }

  private void executeConsoleError(final String lastCommand, final DatabaseSessionInternal db) {
    final String value = lastCommand.substring("console.error ".length()).trim();
    System.err.println(getValue(IOUtils.wrapStringContent(value, '\''), db));
  }

  private Object executeLet(final String lastCommand, final DatabaseSessionInternal db) {
    final int equalsPos = lastCommand.indexOf('=');
    final String variable = lastCommand.substring("let ".length(), equalsPos).trim();
    final String cmd = lastCommand.substring(equalsPos + 1).trim();
    if (cmd == null) {
      return null;
    }

    Object lastResult = null;

    if (cmd.equalsIgnoreCase("NULL")
        || cmd.startsWith("$")
        || (cmd.startsWith("[") && cmd.endsWith("]"))
        || (cmd.startsWith("{") && cmd.endsWith("}"))
        || (cmd.startsWith("\"") && cmd.endsWith("\"") || cmd.startsWith("'") && cmd.endsWith("'"))
        || (cmd.startsWith("(") && cmd.endsWith(")"))
        || cmd.startsWith("#")) {
      lastResult = getValue(cmd, db);
    } else {
      lastResult = executeCommand(cmd, db);
    }

    // PUT THE RESULT INTO THE CONTEXT
    getContext().setVariable(variable, lastResult);
    return lastResult;
  }

  @Override
  public QUORUM_TYPE getQuorumType() {
    return QUORUM_TYPE.WRITE;
  }

  @Override
  public int getTemporaryRIDCounter(CommandContext iContext) {
    return serialTempRID.incrementAndGet();
  }
}