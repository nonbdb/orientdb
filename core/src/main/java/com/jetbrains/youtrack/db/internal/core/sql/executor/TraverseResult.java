package com.jetbrains.youtrack.db.internal.core.sql.executor;

import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.api.record.Identifiable;

/**
 *
 */
public class TraverseResult extends ResultInternal {

  protected Integer depth;

  public TraverseResult(DatabaseSessionInternal db) {
    super(db);
  }

  public TraverseResult(DatabaseSessionInternal db, Identifiable element) {
    super(db, element);
  }

  @Override
  public <T> T getProperty(String name) {
    if ("$depth".equalsIgnoreCase(name)) {
      return (T) depth;
    }
    return super.getProperty(name);
  }

  @Override
  public void setProperty(String name, Object value) {
    if ("$depth".equalsIgnoreCase(name)) {
      if (value instanceof Number) {
        depth = ((Number) value).intValue();
      }
    } else {
      super.setProperty(name, value);
    }
  }
}
