/*
 *
 *  *  Copyright YouTrackDB
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
package com.jetbrains.youtrack.db.internal.core.metadata.sequence;

import com.jetbrains.youtrack.db.api.exception.DatabaseException;
import com.jetbrains.youtrack.db.api.exception.SequenceLimitReachedException;
import com.jetbrains.youtrack.db.internal.common.log.LogManager;
import com.jetbrains.youtrack.db.internal.core.metadata.security.Role;
import com.jetbrains.youtrack.db.internal.core.metadata.security.Rule;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import javax.annotation.Nonnull;

/**
 * @since 3/3/2015
 */
public class SequenceCached extends DBSequence {

  private static final String FIELD_CACHE = "cache";
  private long cacheStart;
  private long cacheEnd;
  private volatile boolean firstCache;

  public SequenceCached(final EntityImpl entity) {
    super(entity);

    firstCache = true;
    cacheStart = cacheEnd = getValue(entity);
  }

  public SequenceCached(DBSequence.CreateParams params, @Nonnull String name) {
    super(params, name);
    var entity = (EntityImpl) entityRid.getRecord();

    if (params == null) {
      params = new CreateParams().setDefaults();
    }

    var db = getDatabase();
    var currentParams = params;
    db.executeInTx(
        () -> {
          EntityImpl boundEntity;

          if (entity.isNotBound(db)) {
            boundEntity = db.bindToSession(entity);
          } else {
            boundEntity = entity;
          }

          setCacheSize(boundEntity, currentParams.cacheSize);
          cacheStart = cacheEnd = 0L;
          allocateCache(
              boundEntity,
              currentParams.cacheSize,
              getOrderType(boundEntity),
              getLimitValue(boundEntity));
          boundEntity.save();
        });
  }

  @Override
  boolean updateParams(
      EntityImpl entity, DBSequence.CreateParams params, boolean executeViaDistributed)
      throws DatabaseException {
    boolean any = super.updateParams(entity, params, executeViaDistributed);
    if (params.cacheSize != null && this.getCacheSize(entity) != params.cacheSize) {
      this.setCacheSize(entity, params.cacheSize);
      any = true;
    }

    firstCache = true;

    return any;
  }

  private void doRecycle(
      EntityImpl entity,
      long start,
      int cacheSize,
      boolean recyclable,
      SequenceOrderType orderType,
      Long limitValue) {
    if (recyclable) {
      setValue(entity, start);
      allocateCache(entity, cacheSize, orderType, limitValue);
    } else {
      throw new SequenceLimitReachedException("Limit reached");
    }
  }

  private boolean signalToAllocateCache(
      SequenceOrderType orderType, int increment, Long limitValue) {
    if (orderType == SequenceOrderType.ORDER_POSITIVE) {
      return cacheStart + increment > cacheEnd
          && !(limitValue != null && cacheStart + increment > limitValue);
    } else {
      return cacheStart - increment < cacheEnd
          && !(limitValue != null && cacheStart - increment < limitValue);
    }
  }

  @Override
  public long next() throws SequenceLimitReachedException, DatabaseException {
    checkSecurity();
    return nextWork();
  }

  private void checkSecurity() {
    getDatabase()
        .checkSecurity(
            Rule.ResourceGeneric.CLASS,
            Role.PERMISSION_UPDATE,
            this.entityRid.<EntityImpl>getRecord().getClassName());
  }

  @Override
  public long nextWork() throws SequenceLimitReachedException {
    return callRetry(
        (db, entity) -> {
          var orderType = getOrderType(entity);
          var limitValue = getLimitValue(entity);
          var increment = getIncrement(entity);
          var cacheSize = getCacheSize(entity);
          var recyble = getRecyclable(entity);
          var start = getStart(entity);

          if (orderType == SequenceOrderType.ORDER_POSITIVE) {
            if (signalToAllocateCache(orderType, increment, limitValue)) {
              boolean cachedbefore = !firstCache;
              allocateCache(entity, cacheSize, orderType, limitValue);
              if (!cachedbefore) {
                if (limitValue != null && cacheStart + increment > limitValue) {
                  doRecycle(entity, start, cacheSize, recyble, orderType, limitValue);
                } else {
                  cacheStart = cacheStart + increment;
                }
              }
            } else if (limitValue != null && cacheStart + increment > limitValue) {
              doRecycle(entity, start, cacheSize, recyble, orderType, limitValue);
            } else {
              cacheStart = cacheStart + increment;
            }
          } else {
            if (signalToAllocateCache(orderType, increment, limitValue)) {
              boolean cachedbefore = !firstCache;
              allocateCache(entity, cacheSize, orderType, limitValue);
              if (!cachedbefore) {
                if (limitValue != null && cacheStart - increment < limitValue) {
                  doRecycle(entity, start, cacheSize, recyble, orderType, limitValue);
                } else {
                  cacheStart = cacheStart - increment;
                }
              }
            } else if (limitValue != null && cacheStart - increment < limitValue) {
              doRecycle(entity, start, cacheSize, recyble, orderType, limitValue);
            } else {
              cacheStart = cacheStart - increment;
            }
          }

          if (limitValue != null && !recyble) {
            float tillEnd = Math.abs(limitValue - cacheStart) / (float) increment;
            float delta = Math.abs(limitValue - start) / (float) increment;
            // warning on 1%
            if (tillEnd <= (delta / 100.f) || tillEnd <= 1) {
              String warningMessage =
                  "Non-recyclable sequence: "
                      + getSequenceName(entity)
                      + " reaching limt, current value: "
                      + cacheStart
                      + " limit value: "
                      + limitValue
                      + " with step: "
                      + increment;
              LogManager.instance().warn(this, warningMessage);
            }
          }

          firstCache = false;
          return cacheStart;
        },
        "next");
  }

  @Override
  protected long currentWork() {
    return this.cacheStart;
  }

  @Override
  public long resetWork() {
    return callRetry(
        (db, entity) -> {
          long newValue = getStart(entity);
          setValue(entity, newValue);
          firstCache = true;
          allocateCache(entity, getCacheSize(entity), getOrderType(entity), getLimitValue(entity));
          return newValue;
        },
        "reset");
  }

  @Override
  public SEQUENCE_TYPE getSequenceType() {
    return SEQUENCE_TYPE.CACHED;
  }

  private int getCacheSize(EntityImpl entity) {
    return entity.getProperty(FIELD_CACHE);
  }

  public final void setCacheSize(EntityImpl entity, int cacheSize) {
    entity.setProperty(FIELD_CACHE, cacheSize);
  }

  private void allocateCache(
      EntityImpl entity, int cacheSize, SequenceOrderType orderType, Long limitValue) {
    long value = getValue(entity);
    long newValue;
    if (orderType == SequenceOrderType.ORDER_POSITIVE) {
      newValue = value + ((long) getIncrement(entity) * cacheSize);
      if (limitValue != null && newValue > limitValue) {
        newValue = limitValue;
      }
    } else {
      newValue = value - ((long) getIncrement(entity) * cacheSize);
      if (limitValue != null && newValue < limitValue) {
        newValue = limitValue;
      }
    }
    setValue(entity, newValue);
    this.cacheStart = value;
    if (orderType == SequenceOrderType.ORDER_POSITIVE) {
      this.cacheEnd = newValue - 1;
    } else {
      this.cacheEnd = newValue + 1;
    }
    firstCache = false;
  }
}
