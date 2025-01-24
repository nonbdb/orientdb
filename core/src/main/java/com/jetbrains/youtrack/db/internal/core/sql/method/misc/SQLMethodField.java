/*
 *
 * Copyright 2013 Geomatys.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.youtrack.db.internal.core.sql.method.misc;

import com.jetbrains.youtrack.db.api.exception.RecordNotFoundException;
import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.internal.common.collection.MultiValue;
import com.jetbrains.youtrack.db.internal.common.log.LogManager;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.id.RecordId;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityHelper;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class SQLMethodField extends AbstractSQLMethod {

  public static final String NAME = "field";

  public SQLMethodField() {
    super(NAME, 1, 1);
  }

  @Override
  public Object execute(
      Object iThis,
      final Identifiable iCurrentRecord,
      final CommandContext iContext,
      Object ioResult,
      final Object[] iParams) {
    if (iParams[0] == null) {
      return null;
    }

    var db = iContext.getDatabase();
    final String paramAsString = iParams[0].toString();

    if (ioResult != null) {
      if (ioResult instanceof Result result && result.isEntity()) {
        ioResult = result.asEntity();
      }
      if (ioResult instanceof Iterable && !(ioResult instanceof EntityImpl)) {
        ioResult = ((Iterable) ioResult).iterator();
      }
      if (ioResult instanceof String) {
        try {
          ioResult = new RecordId((String) ioResult).getRecord(db);
        } catch (Exception e) {
          LogManager.instance().error(this, "Error on reading rid with value '%s'", e, ioResult);
          ioResult = null;
        }
      } else if (ioResult instanceof Identifiable) {
        try {
          ioResult = ((Identifiable) ioResult).getRecord(db);
        } catch (RecordNotFoundException rnf) {
          LogManager.instance()
              .error(this, "Error on reading rid with value '%s'", null, ioResult);
          ioResult = null;
        }
      } else if (ioResult instanceof Collection<?>
          || ioResult instanceof Iterator<?>
          || ioResult.getClass().isArray()) {
        final List<Object> result = new ArrayList<Object>(MultiValue.getSize(ioResult));
        for (Object o : MultiValue.getMultiValueIterable(ioResult)) {
          Object newlyAdded = EntityHelper.getFieldValue(db, o, paramAsString);
          if (MultiValue.isMultiValue(newlyAdded)) {
            if (newlyAdded instanceof Map || newlyAdded instanceof Identifiable) {
              result.add(newlyAdded);
            } else {
              for (Object item : MultiValue.getMultiValueIterable(newlyAdded)) {
                result.add(item);
              }
            }
          } else {
            result.add(newlyAdded);
          }
        }
        return result;
      }
    }

    if (!"*".equals(paramAsString) && ioResult != null) {
      if (ioResult instanceof CommandContext) {
        ioResult = ((CommandContext) ioResult).getVariable(paramAsString);
      } else {
        ioResult = EntityHelper.getFieldValue(db, ioResult, paramAsString, iContext);
      }
    }

    return ioResult;
  }

  @Override
  public boolean evaluateParameters() {
    return false;
  }
}
