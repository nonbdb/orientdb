package com.jetbrains.youtrack.db.internal.core.sql.functions.sequence;

import com.jetbrains.youtrack.db.api.DatabaseSession;
import com.jetbrains.youtrack.db.api.exception.CommandExecutionException;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseRecordThreadLocal;
import com.jetbrains.youtrack.db.internal.core.metadata.sequence.DBSequence;
import com.jetbrains.youtrack.db.internal.core.sql.filter.SQLFilterItem;
import com.jetbrains.youtrack.db.internal.core.sql.functions.SQLFunctionConfigurableAbstract;

/**
 * Returns a sequence by name.
 */
public class SQLFunctionSequence extends SQLFunctionConfigurableAbstract {

  public static final String NAME = "sequence";

  public SQLFunctionSequence() {
    super(NAME, 1, 1);
  }

  @Override
  public Object execute(
      Object iThis,
      Identifiable iCurrentRecord,
      Object iCurrentResult,
      Object[] iParams,
      CommandContext iContext) {
    final String seqName;
    if (configuredParameters != null
        && configuredParameters.length > 0
        && configuredParameters[0] instanceof SQLFilterItem) // old stuff
    {
      seqName =
          (String)
              ((SQLFilterItem) configuredParameters[0])
                  .getValue(iCurrentRecord, iCurrentResult, iContext);
    } else {
      seqName = "" + iParams[0];
    }

    DBSequence result =
        DatabaseRecordThreadLocal.instance()
            .get()
            .getMetadata()
            .getSequenceLibrary()
            .getSequence(seqName);
    if (result == null) {
      throw new CommandExecutionException("Sequence not found: " + seqName);
    }
    return result;
  }

  @Override
  public Object getResult() {
    return null;
  }

  @Override
  public String getSyntax(DatabaseSession session) {
    return "sequence(<name>)";
  }

  @Override
  public boolean aggregateResults() {
    return false;
  }
}
