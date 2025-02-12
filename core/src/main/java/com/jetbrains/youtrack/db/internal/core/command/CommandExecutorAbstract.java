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
package com.jetbrains.youtrack.db.internal.core.command;

import com.jetbrains.youtrack.db.internal.common.listener.ProgressListener;
import com.jetbrains.youtrack.db.internal.common.parser.BaseParser;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.ExecutionThreadLocal;
import com.jetbrains.youtrack.db.internal.core.exception.CommandInterruptedException;
import com.jetbrains.youtrack.db.internal.core.metadata.security.Role;
import com.jetbrains.youtrack.db.internal.core.metadata.security.Rule;
import com.jetbrains.youtrack.db.internal.core.sql.filter.SQLPredicate;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Abstract implementation of Executor Command interface.
 */
@SuppressWarnings("unchecked")
public abstract class CommandExecutorAbstract extends BaseParser implements CommandExecutor {

  protected ProgressListener progressListener;
  protected int limit = -1;
  protected Map<Object, Object> parameters;
  protected CommandContext context;

  public CommandExecutorAbstract init(DatabaseSessionInternal db,
      final CommandRequestText iRequest) {
    db.checkSecurity(Rule.ResourceGeneric.COMMAND, Role.PERMISSION_READ);
    parserText = iRequest.getText().trim();
    parserTextUpperCase = SQLPredicate.upperCase(parserText);
    return this;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + " [text=" + parserText + "]";
  }

  public ProgressListener getProgressListener() {
    return progressListener;
  }

  public <RET extends CommandExecutor> RET setProgressListener(
      final ProgressListener progressListener) {
    this.progressListener = progressListener;
    return (RET) this;
  }

  public int getLimit() {
    return limit;
  }

  public <RET extends CommandExecutor> RET setLimit(final int iLimit) {
    this.limit = iLimit;
    return (RET) this;
  }

  public Map<Object, Object> getParameters() {
    return parameters;
  }

  @Override
  public String getFetchPlan() {
    return null;
  }

  public CommandContext getContext() {
    return context;
  }

  public void setContext(final CommandContext iContext) {
    context = iContext;
  }

  @Override
  public Set<String> getInvolvedClusters(DatabaseSessionInternal session) {
    return Collections.EMPTY_SET;
  }

  @Override
  public int getSecurityOperationType() {
    return Role.PERMISSION_READ;
  }

  protected boolean checkInterruption() {
    return checkInterruption(this.context);
  }

  public static boolean checkInterruption(final CommandContext iContext) {
    if (ExecutionThreadLocal.isInterruptCurrentOperation()) {
      throw new CommandInterruptedException(iContext.getDatabaseSession().getDatabaseName(),
          "The command has been interrupted");
    }

    return iContext == null || iContext.checkTimeout();
  }

  @Override
  public boolean isCacheable() {
    return false;
  }

}
