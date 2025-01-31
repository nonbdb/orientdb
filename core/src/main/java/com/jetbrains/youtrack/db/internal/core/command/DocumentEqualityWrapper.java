package com.jetbrains.youtrack.db.internal.core.command;

import com.jetbrains.youtrack.db.internal.core.record.impl.EntityHelper;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;

/**
 * This class is designed to compare documents based on deep equality (to be used in Sets)
 */
public class DocumentEqualityWrapper {

  private final EntityImpl internal;

  DocumentEqualityWrapper(EntityImpl internal) {

    this.internal = internal;
  }

  public boolean equals(Object obj) {
    if (obj instanceof DocumentEqualityWrapper) {
      return EntityHelper.hasSameContentOf(
          internal, internal.getSession(), ((DocumentEqualityWrapper) obj).internal,
          internal.getSession(), null);
    }
    return false;
  }

  @Override
  public int hashCode() {
    var result = 0;
    for (var fieldName : internal.fieldNames()) {
      result += fieldName.hashCode();
      var value = internal.field(fieldName);
      if (value != null) {
        result += value.hashCode();
      }
    }
    return result;
  }
}
