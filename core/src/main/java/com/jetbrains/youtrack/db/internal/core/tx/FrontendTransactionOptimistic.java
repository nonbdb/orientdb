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

package com.jetbrains.youtrack.db.internal.core.tx;

import com.jetbrains.youtrack.db.api.DatabaseSession;
import com.jetbrains.youtrack.db.api.exception.BaseException;
import com.jetbrains.youtrack.db.api.exception.DatabaseException;
import com.jetbrains.youtrack.db.api.exception.RecordNotFoundException;
import com.jetbrains.youtrack.db.api.exception.TransactionException;
import com.jetbrains.youtrack.db.api.exception.ValidationException;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.api.record.RID;
import com.jetbrains.youtrack.db.api.record.Record;
import com.jetbrains.youtrack.db.api.record.RecordHook.TYPE;
import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import com.jetbrains.youtrack.db.internal.common.log.LogManager;
import com.jetbrains.youtrack.db.internal.core.cache.LocalRecordCache;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.record.RecordOperation;
import com.jetbrains.youtrack.db.internal.core.exception.StorageException;
import com.jetbrains.youtrack.db.internal.core.id.ChangeableIdentity;
import com.jetbrains.youtrack.db.internal.core.id.RecordId;
import com.jetbrains.youtrack.db.internal.core.index.ClassIndexManager;
import com.jetbrains.youtrack.db.internal.core.index.CompositeKey;
import com.jetbrains.youtrack.db.internal.core.index.Index;
import com.jetbrains.youtrack.db.internal.core.index.IndexDefinition;
import com.jetbrains.youtrack.db.internal.core.index.IndexInternal;
import com.jetbrains.youtrack.db.internal.core.index.IndexManagerAbstract;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.SchemaImmutableClass;
import com.jetbrains.youtrack.db.internal.core.metadata.security.Role;
import com.jetbrains.youtrack.db.internal.core.metadata.security.Rule;
import com.jetbrains.youtrack.db.internal.core.record.RecordAbstract;
import com.jetbrains.youtrack.db.internal.core.record.RecordInternal;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityInternalUtils;
import com.jetbrains.youtrack.db.internal.core.storage.Storage;
import com.jetbrains.youtrack.db.internal.core.storage.StorageProxy;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.AbstractPaginatedStorage;
import com.jetbrains.youtrack.db.internal.core.tx.FrontendTransactionIndexChanges.OPERATION;
import com.jetbrains.youtrack.db.internal.core.tx.FrontendTransactionIndexChangesPerKey.TransactionIndexEntry;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FrontendTransactionOptimistic extends FrontendTransactionAbstract implements
    TransactionInternal {

  private static final AtomicLong txSerial = new AtomicLong();

  // order of updates is critical during synchronization of remote transactions
  protected LinkedHashMap<RecordId, RecordId> generatedOriginalRecordIdMap = new LinkedHashMap<>();
  protected LinkedHashMap<RecordId, RecordOperation> recordOperations = new LinkedHashMap<>();

  protected LinkedHashMap<String, FrontendTransactionIndexChanges> indexEntries = new LinkedHashMap<>();
  protected HashMap<RID, List<FrontendTransactionRecordIndexOperation>> recordIndexOperations =
      new HashMap<>();

  protected long id;
  protected int newRecordsPositionsGenerator = -2;
  private final HashMap<String, Object> userData = new HashMap<>();

  @Nullable
  private FrontendTransacationMetadataHolder metadata = null;

  @Nullable
  private List<byte[]> serializedOperations;

  protected boolean changed = true;
  private boolean isAlreadyStartedOnServer = false;
  protected int txStartCounter;
  private boolean sentToServer = false;

  public FrontendTransactionOptimistic(final DatabaseSessionInternal iDatabase) {
    super(iDatabase);
    this.id = txSerial.incrementAndGet();
  }

  protected FrontendTransactionOptimistic(final DatabaseSessionInternal iDatabase, long id) {
    super(iDatabase);
    this.id = id;
  }

  public int begin() {
    if (txStartCounter < 0) {
      throw new TransactionException("Invalid value of TX counter: " + txStartCounter);
    }

    if (txStartCounter == 0) {
      status = TXSTATUS.BEGUN;

      var localCache = database.getLocalCache();
      localCache.unloadNotModifiedRecords();
      localCache.clear();
    } else {
      if (status == TXSTATUS.ROLLED_BACK || status == TXSTATUS.ROLLBACKING) {
        throw new RollbackException(
            "Impossible to start a new transaction because the current was rolled back");
      }
    }

    txStartCounter++;
    return txStartCounter;
  }

  public void commit() {
    commit(false);
  }

  /**
   * The transaction is reentrant. If {@code begin()} has been called several times, the actual
   * commit happens only after the same amount of {@code commit()} calls
   *
   * @param force commit transaction even
   */
  @Override
  public void commit(final boolean force) {
    checkTransactionValid();
    if (txStartCounter < 0) {
      throw new StorageException("Invalid value of tx counter: " + txStartCounter);
    }
    if (force) {
      preProcessRecordsAndExecuteCallCallbacks();
      txStartCounter = 0;
    } else {
      if (txStartCounter == 1) {
        preProcessRecordsAndExecuteCallCallbacks();
      }
      txStartCounter--;
    }

    if (txStartCounter == 0) {
      doCommit();
    } else {
      if (txStartCounter < 0) {
        throw new TransactionException(
            "Transaction was committed more times than it was started.");
      }
    }
  }

  public RecordAbstract getRecord(final RID rid) {
    final RecordOperation e = getRecordEntry(rid);
    if (e != null) {
      if (e.type == RecordOperation.DELETED) {
        return FrontendTransactionAbstract.DELETED_RECORD;
      } else {
        assert e.record.getSession() == database;
        return e.record;
      }
    }
    return null;
  }

  /**
   * Called by class iterator.
   */
  public List<RecordOperation> getNewRecordEntriesByClass(
      final SchemaClass iClass, final boolean iPolymorphic) {
    final List<RecordOperation> result = new ArrayList<>();

    if (iClass == null)
    // RETURN ALL THE RECORDS
    {
      for (RecordOperation entry : recordOperations.values()) {
        if (entry.type == RecordOperation.CREATED) {
          result.add(entry);
        }
      }
    } else {
      // FILTER RECORDS BY CLASSNAME
      for (RecordOperation entry : recordOperations.values()) {
        if (entry.type == RecordOperation.CREATED) {
          if (entry.record != null) {
            if (entry.record instanceof EntityImpl) {
              if (iPolymorphic) {
                if (iClass.isSuperClassOf(
                    EntityInternalUtils.getImmutableSchemaClass(((EntityImpl) entry.record)))) {
                  result.add(entry);
                }
              } else {
                if (iClass.getName().equals(((EntityImpl) entry.record).getClassName())) {
                  result.add(entry);
                }
              }
            }
          }
        }
      }
    }

    return result;
  }

  /**
   * Called by cluster iterator.
   */
  public List<RecordOperation> getNewRecordEntriesByClusterIds(final int[] iIds) {
    final List<RecordOperation> result = new ArrayList<>();

    if (iIds == null)
    // RETURN ALL THE RECORDS
    {
      for (RecordOperation entry : recordOperations.values()) {
        if (entry.type == RecordOperation.CREATED) {
          result.add(entry);
        }
      }
    } else
    // FILTER RECORDS BY ID
    {
      for (RecordOperation entry : recordOperations.values()) {
        for (int id : iIds) {
          if (entry.record != null) {
            if (entry.record.getIdentity().getClusterId() == id
                && entry.type == RecordOperation.CREATED) {
              result.add(entry);
              break;
            }
          }
        }
      }
    }

    return result;
  }

  public void clearIndexEntries() {
    indexEntries.clear();
    recordIndexOperations.clear();
  }

  public List<String> getInvolvedIndexes() {
    List<String> list = null;
    for (String indexName : indexEntries.keySet()) {
      if (list == null) {
        list = new ArrayList<>();
      }
      list.add(indexName);
    }
    return list;
  }

  public Map<String, FrontendTransactionIndexChanges> getIndexOperations() {
    return indexEntries;
  }

  public FrontendTransactionIndexChanges getIndexChangesInternal(final String indexName) {
    if (getDatabase().isRemote()) {
      return null;
    }
    return getIndexChanges(indexName);
  }

  @Override
  public void addIndexEntry(
      final IndexInternal index,
      final String iIndexName,
      final OPERATION iOperation,
      final Object key,
      final Identifiable iValue) {
    // index changes are tracked on server in case of client-server deployment
    assert database.getStorage() instanceof AbstractPaginatedStorage;

    changed = true;
    try {
      FrontendTransactionIndexChanges indexEntry = indexEntries.get(iIndexName);
      if (indexEntry == null) {
        indexEntry = new FrontendTransactionIndexChanges(index);
        indexEntries.put(iIndexName, indexEntry);
      }

      if (iOperation == OPERATION.CLEAR) {
        indexEntry.setCleared();
      } else {
        FrontendTransactionIndexChangesPerKey changes = indexEntry.getChangesPerKey(key);
        changes.add(iValue, iOperation);

        if (changes.key == key
            && key instanceof ChangeableIdentity changeableIdentity
            && changeableIdentity.canChangeIdentity()) {
          changeableIdentity.addIdentityChangeListener(indexEntry);
        }

        if (iValue == null) {
          return;
        }

        List<FrontendTransactionRecordIndexOperation> transactionIndexOperations =
            recordIndexOperations.get(iValue.getIdentity());

        if (transactionIndexOperations == null) {
          transactionIndexOperations = new ArrayList<>();
          recordIndexOperations.put(((RecordId) iValue.getIdentity()).copy(),
              transactionIndexOperations);
        }

        transactionIndexOperations.add(
            new FrontendTransactionRecordIndexOperation(iIndexName, key, iOperation));
      }
    } catch (Exception e) {
      rollback(true, 0);
      throw e;
    }
  }

  /**
   * Buffer sizes index changes to be flushed at commit time.
   */
  public FrontendTransactionIndexChanges getIndexChanges(final String iIndexName) {
    return indexEntries.get(iIndexName);
  }

  @Override
  public int amountOfNestedTxs() {
    return txStartCounter;
  }

  public void rollback() {
    rollback(false, -1);
  }

  public void internalRollback() {
    status = TXSTATUS.ROLLBACKING;

    invalidateChangesInCache();

    close();
    status = TXSTATUS.ROLLED_BACK;
  }

  private void invalidateChangesInCache() {
    for (final RecordOperation v : recordOperations.values()) {
      final RecordAbstract rec = v.record;
      RecordInternal.unsetDirty(rec);
      rec.unload();
    }

    var localCache = database.getLocalCache();
    localCache.unloadRecords();
    localCache.clear();
  }

  @Override
  public void rollback(boolean force, int commitLevelDiff) {
    if (txStartCounter < 0) {
      throw new StorageException("Invalid value of TX counter");
    }
    checkTransactionValid();

    txStartCounter += commitLevelDiff;
    status = TXSTATUS.ROLLBACKING;

    if (!force && txStartCounter > 0) {
      return;
    }

    if (database.isRemote()) {
      final Storage storage = database.getStorage();
      ((StorageProxy) storage).rollback(FrontendTransactionOptimistic.this);
    }

    internalRollback();
  }

  @Override
  public boolean exists(RID rid) {
    checkTransactionValid();

    final Record txRecord = getRecord(rid);
    if (txRecord == FrontendTransactionAbstract.DELETED_RECORD) {
      return false;
    }

    if (txRecord != null) {
      return true;
    }

    return database.executeExists(rid);
  }

  @Override
  public @Nonnull Record loadRecord(RID rid) {

    checkTransactionValid();

    final RecordAbstract txRecord = getRecord(rid);
    if (txRecord == FrontendTransactionAbstract.DELETED_RECORD) {
      // DELETED IN TX
      throw new RecordNotFoundException(rid);
    }

    if (txRecord != null) {
      return txRecord;
    }

    if (rid.isTemporary()) {
      throw new RecordNotFoundException(rid);
    }

    // DELEGATE TO THE STORAGE, NO TOMBSTONES SUPPORT IN TX MODE
    return database.executeReadRecord((RecordId) rid);
  }

  public void deleteRecord(final RecordAbstract iRecord) {
    try {
      addRecordOperation(iRecord, RecordOperation.DELETED, null);
    } catch (Exception e) {
      rollback(true, 0);
      throw e;
    }
  }

  public Record saveRecord(RecordAbstract passedRecord, final String clusterName) {
    try {
      if (passedRecord == null) {
        return null;
      }
      if (passedRecord.isUnloaded()) {
        throw new DatabaseException(
            "Record "
                + passedRecord
                + " is not bound to session, please call "
                + DatabaseSession.class.getSimpleName()
                + ".bindToSession(record) before changing it");
      }

      // fetch primary record if the record is a proxy record.
      passedRecord = passedRecord.getRecord(database);
      final byte operation =
          passedRecord.getIdentity().isValid()
              ? RecordOperation.UPDATED
              : RecordOperation.CREATED;

      addRecordOperation(passedRecord, operation, clusterName);
      return passedRecord;
    } catch (Exception e) {
      rollback(true, 0);
      throw e;
    }
  }

  @Override
  public String toString() {
    return "FrontendTransactionOptimistic [id="
        + id
        + ", status="
        + status
        + ", recEntries="
        + recordOperations.size()
        + ", idxEntries="
        + indexEntries.size()
        + ']';
  }

  public void setStatus(final TXSTATUS iStatus) {
    status = iStatus;
  }

  public void addRecordOperation(RecordAbstract record, byte status, String clusterName) {
    try {
      validateState(record);
      var rid = record.getIdentity();

      if (clusterName == null) {
        clusterName = database.getClusterNameById(record.getIdentity().getClusterId());
      }

      if (!rid.isValid()) {
        database.assignAndCheckCluster(record, clusterName);
        rid.setClusterPosition(newRecordsPositionsGenerator--);
      }

      RecordOperation txEntry = getRecordEntry(rid);
      try {
        if (txEntry == null) {
          if (rid.isTemporary() && status == RecordOperation.UPDATED) {
            throw new IllegalStateException(
                "Temporary records can not be added to the transaction");
          }
          txEntry = new RecordOperation(record, status);

          recordOperations.put(rid.copy(), txEntry);
          changed = true;
        } else {
          if (txEntry.record != record) {
            throw new TransactionException(
                "Found record in transaction with the same RID but different instance");
          }

          switch (txEntry.type) {
            case RecordOperation.UPDATED:
              if (status == RecordOperation.DELETED) {
                txEntry.type = RecordOperation.DELETED;
                changed = true;
              } else if (status == RecordOperation.CREATED) {
                throw new IllegalStateException(
                    "Invalid operation, record can not be created as it is already updated");
              }
              break;
            case RecordOperation.DELETED:
              if (status == RecordOperation.UPDATED || status == RecordOperation.CREATED) {
                throw new IllegalStateException(
                    "Invalid operation, record can not be updated or created as it is already deleted");
              }
              break;
            case RecordOperation.CREATED:
              if (status == RecordOperation.DELETED) {
                recordOperations.remove(rid);
                changed = true;
              } else if (status == RecordOperation.CREATED) {
                throw new IllegalStateException(
                    "Invalid operation, record can not be created as it is already created");
              }
              break;
          }
        }

        switch (status) {
          case RecordOperation.CREATED: {
            if (record instanceof EntityImpl entity) {
              final SchemaImmutableClass clazz =
                  EntityInternalUtils.getImmutableSchemaClass(database, entity);
              if (clazz != null) {
                ClassIndexManager.checkIndexesAfterCreate(entity, database);
                txEntry.indexTrackingDirtyCounter = record.getDirtyCounter();
              }
            }
          }
          break;
          case RecordOperation.UPDATED: {
            if (record instanceof EntityImpl entity) {
              final SchemaImmutableClass clazz =
                  EntityInternalUtils.getImmutableSchemaClass(database, entity);
              if (clazz != null && record.getDirtyCounter() != txEntry.indexTrackingDirtyCounter) {
                ClassIndexManager.checkIndexesAfterUpdate(entity, database);
                txEntry.indexTrackingDirtyCounter = record.getDirtyCounter();
              }
            }
          }
          break;
          case RecordOperation.DELETED: {
            if (record instanceof EntityImpl entity) {
              final SchemaImmutableClass clazz =
                  EntityInternalUtils.getImmutableSchemaClass(database, entity);
              if (clazz != null) {
                ClassIndexManager.checkIndexesAfterDelete(entity, database);
              }
            }
          }
          break;
          default:
            throw new IllegalStateException(
                "Invalid transaction operation type " + status);
        }

        if (record instanceof EntityImpl && ((EntityImpl) record).isTrackingChanges()) {
          EntityInternalUtils.clearTrackData(((EntityImpl) record));
        }
      } catch (final Exception e) {
        throw BaseException.wrapException(
            new DatabaseException(
                "Error on execution of operation on record " + record.getIdentity()), e);
      }
    } catch (Exception e) {
      rollback(true, 0);
      throw e;
    }
  }

  private void validateState(RecordAbstract record) {
    if (record.isUnloaded()) {
      throw new DatabaseException(
          "Record "
              + record
              + " is not bound to session, please call "
              + DatabaseSession.class.getSimpleName()
              + ".bindToSession(record) before changing it");
    }
    checkTransactionValid();
  }

  public void deleteRecordOperation(RecordAbstract record) {
    var identity = record.getIdentity();

    if (generatedOriginalRecordIdMap.containsKey(identity)) {
      throw new TransactionException(
          "Cannot delete record operation for record with identity " + identity
              + " because it was updated during transaction");
    }

    recordOperations.remove(identity);
  }

  private void doCommit() {
    if (status == TXSTATUS.ROLLED_BACK || status == TXSTATUS.ROLLBACKING) {
      if (status == TXSTATUS.ROLLBACKING) {
        internalRollback();
      }

      throw new RollbackException(
          "Given transaction was rolled back, and thus cannot be committed.");
    }

    try {
      status = TXSTATUS.COMMITTING;
      if (sentToServer || !recordOperations.isEmpty() || !indexEntries.isEmpty()) {
        database.internalCommit(this);
        try {
          database.afterCommitOperations();
        } catch (Exception e) {
          LogManager.instance().error(this,
              "Error during after commit callback invocation", e);
        }
      }

    } catch (Exception e) {
      rollback(true, 0);
      throw e;
    }

    close();
    status = TXSTATUS.COMPLETED;
  }

  private void preProcessRecordsAndExecuteCallCallbacks() {
    var serializer = database.getSerializer();
    List<RecordAbstract> changedRecords = null;

    for (var recordOperation : recordOperations.values()) {
      var record = recordOperation.record;
      if (recordOperation.type == RecordOperation.CREATED
          || recordOperation.type == RecordOperation.UPDATED) {
        if (recordOperation.record instanceof EntityImpl entity) {
          EntityInternalUtils.checkClass(entity, database);
          try {
            entity.autoConvertValues();
          } catch (ValidationException e) {
            entity.undo();
            throw e;
          }

          EntityInternalUtils.convertAllMultiValuesToTrackedVersions(entity);

          var className = entity.getClassName();
          if (!entity.getIdentity().isValid()) {
            if (className != null) {
              database.checkSecurity(Rule.ResourceGeneric.CLASS, Role.PERMISSION_CREATE,
                  className);
            }
          } else {
            // UPDATE: CHECK ACCESS ON SCHEMA CLASS NAME (IF ANY)
            if (className != null) {
              database.checkSecurity(Rule.ResourceGeneric.CLASS, Role.PERMISSION_UPDATE,
                  className);
            }
          }

          entity.recordFormat = serializer;

          if (entity.getDirtyCounter() != recordOperation.indexTrackingDirtyCounter) {
            if (className != null) {
              ClassIndexManager.checkIndexesAfterUpdate(entity, database);
              recordOperation.indexTrackingDirtyCounter = record.getDirtyCounter();
            }
          }
        }

        if (recordOperation.type == RecordOperation.CREATED) {
          if (processRecordCreation(recordOperation, record)) {
            if (changedRecords == null) {
              changedRecords = new ArrayList<>(recordOperations.size());
            }

            changedRecords.add(record);
          }
        } else {
          if (processRecordUpdate(recordOperation, record)) {
            if (changedRecords == null) {
              changedRecords = new ArrayList<>(recordOperations.size());
            }

            changedRecords.add(record);
          }
        }
      } else if (recordOperation.type == RecordOperation.DELETED) {
        processRecordDeletion(recordOperation, record);
      } else {
        throw new IllegalStateException("Invalid record operation type " + recordOperation.type);
      }
    }

    if (changedRecords != null && !changedRecords.isEmpty()) {
      var recordsChangedInLoop = new ArrayList<RecordAbstract>(changedRecords.size());

      while (!changedRecords.isEmpty()) {
        for (var record : changedRecords) {
          var recordOperation = new RecordOperation(record, RecordOperation.UPDATED);
          recordOperation.recordCallBackDirtyCounter = record.getDirtyCounter();
          recordOperation.indexTrackingDirtyCounter = record.getDirtyCounter();

          if (record instanceof EntityImpl entity && entity.getClassName() != null) {
            ClassIndexManager.checkIndexesAfterUpdate(entity, database);
            recordOperation.indexTrackingDirtyCounter = record.getDirtyCounter();
          }

          if (processRecordUpdate(recordOperation, record)) {
            recordsChangedInLoop.add(record);
          }
        }

        changedRecords = recordsChangedInLoop;
        recordsChangedInLoop = new ArrayList<>();
      }
    }
  }

  private void processRecordDeletion(RecordOperation recordOperation, RecordAbstract record) {
    var clusterName = database.getClusterNameById(record.getIdentity().getClusterId());

    database.beforeDeleteOperations(record, clusterName);
    try {
      database.afterDeleteOperations(record);
      if (record instanceof EntityImpl && ((EntityImpl) record).isTrackingChanges()) {
        EntityInternalUtils.clearTrackData(((EntityImpl) record));
      }
    } catch (Exception e) {
      database.callbackHooks(TYPE.DELETE_FAILED, record);
      throw e;
    } finally {
      database.callbackHooks(TYPE.FINALIZE_DELETION, record);
    }
    recordOperation.recordCallBackDirtyCounter = record.getDirtyCounter();
  }

  private boolean processRecordUpdate(RecordOperation recordOperation, RecordAbstract record) {
    var dirtyCounter = record.getDirtyCounter();
    var clusterName = database.getClusterNameById(record.getIdentity().getClusterId());
    if (recordOperation.recordCallBackDirtyCounter != record.getDirtyCounter()) {
      recordOperation.recordCallBackDirtyCounter = dirtyCounter;
      database.beforeUpdateOperations(record, clusterName);
      try {
        database.afterUpdateOperations(record);
        if (record instanceof EntityImpl && ((EntityImpl) record).isTrackingChanges()) {
          EntityInternalUtils.clearTrackData(((EntityImpl) record));
        }
      } catch (Exception e) {
        database.callbackHooks(TYPE.UPDATE_FAILED, record);
        throw e;
      } finally {
        database.callbackHooks(TYPE.FINALIZE_UPDATE, record);
      }

      return record.getDirtyCounter() != recordOperation.recordCallBackDirtyCounter;
    }

    return false;
  }

  private boolean processRecordCreation(RecordOperation recordOperation, RecordAbstract record) {
    database.assignAndCheckCluster(recordOperation.record, null);
    var clusterName = database.getClusterNameById(record.getIdentity().getClusterId());

    recordOperation.recordCallBackDirtyCounter = record.getDirtyCounter();
    database.beforeCreateOperations(record, clusterName);
    try {
      database.afterCreateOperations(record);
      if (record instanceof EntityImpl && ((EntityImpl) record).isTrackingChanges()) {
        EntityInternalUtils.clearTrackData(((EntityImpl) record));
      }
    } catch (Exception e) {
      database.callbackHooks(TYPE.CREATE_FAILED, record);
      throw e;
    } finally {
      database.callbackHooks(TYPE.FINALIZE_CREATION, record);
    }

    return recordOperation.recordCallBackDirtyCounter != record.getDirtyCounter();
  }

  public void resetChangesTracking() {
    isAlreadyStartedOnServer = true;
    changed = false;
  }

  @Override
  public void close() {
    final LocalRecordCache dbCache = database.getLocalCache();
    for (RecordOperation txEntry : recordOperations.values()) {
      var record = txEntry.record;

      if (!record.isUnloaded()) {
        if (record instanceof EntityImpl entity) {
          EntityInternalUtils.clearTransactionTrackData(entity);
        }

        RecordInternal.unsetDirty(record);
        record.unload();
      }
    }

    dbCache.unloadRecords();
    dbCache.clear();

    clearUnfinishedChanges();

    status = TXSTATUS.INVALID;
  }

  private void clearUnfinishedChanges() {
    recordOperations.clear();
    indexEntries.clear();
    recordIndexOperations.clear();

    newRecordsPositionsGenerator = -2;

    database.setDefaultTransactionMode();
    userData.clear();
  }

  public void updateIdentityAfterCommit(final RecordId oldRid, final RecordId newRid) {
    if (oldRid.equals(newRid))
    // NO CHANGE, IGNORE IT
    {
      return;
    }

    // XXX: Identity update may mutate the index keys, so we have to identify and reinsert
    // potentially affected index keys to keep
    // the FrontendTransactionIndexChanges.changesPerKey in a consistent state.

    final List<KeyChangesUpdateRecord> keyRecordsToReinsert = new ArrayList<>();
    final DatabaseSessionInternal database = getDatabase();
    if (!database.isRemote()) {
      final IndexManagerAbstract indexManager = database.getMetadata().getIndexManagerInternal();
      for (Entry<String, FrontendTransactionIndexChanges> entry : indexEntries.entrySet()) {
        final Index index = indexManager.getIndex(database, entry.getKey());
        if (index == null) {
          throw new TransactionException(
              "Cannot find index '" + entry.getValue() + "' while committing transaction");
        }

        final Dependency[] fieldRidDependencies = getIndexFieldRidDependencies(index);
        if (!isIndexMayDependOnRids(fieldRidDependencies)) {
          continue;
        }

        final FrontendTransactionIndexChanges indexChanges = entry.getValue();
        for (final Iterator<FrontendTransactionIndexChangesPerKey> iterator =
            indexChanges.changesPerKey.values().iterator();
            iterator.hasNext(); ) {
          final FrontendTransactionIndexChangesPerKey keyChanges = iterator.next();
          if (isIndexKeyMayDependOnRid(keyChanges.key, oldRid, fieldRidDependencies)) {
            keyRecordsToReinsert.add(new KeyChangesUpdateRecord(keyChanges, indexChanges));
            iterator.remove();

            if (keyChanges.key instanceof ChangeableIdentity changeableIdentity) {
              changeableIdentity.removeIdentityChangeListener(indexChanges);
            }
          }
        }
      }
    }

    // Update the identity.

    final RecordOperation rec = getRecordEntry(oldRid);
    if (rec != null) {
      generatedOriginalRecordIdMap.put(newRid.copy(), oldRid.copy());

      if (!rec.record.getIdentity().equals(newRid)) {
        final RecordId recordId = rec.record.getIdentity();
        recordId.setClusterPosition(newRid.getClusterPosition());
        recordId.setClusterId(newRid.getClusterId());
      }
    }

    // Reinsert the potentially affected index keys.

    for (KeyChangesUpdateRecord record : keyRecordsToReinsert) {
      record.indexChanges.changesPerKey.put(record.keyChanges.key, record.keyChanges);
    }

    // Update the indexes.

    RecordOperation val = getRecordEntry(oldRid);
    final List<FrontendTransactionRecordIndexOperation> transactionIndexOperations =
        recordIndexOperations.get(val != null ? val.getRecordId() : null);
    if (transactionIndexOperations != null) {
      for (final FrontendTransactionRecordIndexOperation indexOperation : transactionIndexOperations) {
        FrontendTransactionIndexChanges indexEntryChanges = indexEntries.get(indexOperation.index);
        if (indexEntryChanges == null) {
          continue;
        }
        final FrontendTransactionIndexChangesPerKey keyChanges;
        if (indexOperation.key == null) {
          keyChanges = indexEntryChanges.nullKeyChanges;
        } else {
          keyChanges = indexEntryChanges.changesPerKey.get(indexOperation.key);
        }
        if (keyChanges != null) {
          updateChangesIdentity(oldRid, newRid, keyChanges);
        }
      }
    }
  }

  private void updateChangesIdentity(
      RID oldRid, RID newRid, FrontendTransactionIndexChangesPerKey changesPerKey) {
    if (changesPerKey == null) {
      return;
    }

    for (final TransactionIndexEntry indexEntry : changesPerKey.getEntriesAsList()) {
      if (indexEntry.getValue().getIdentity().equals(oldRid)) {
        indexEntry.setValue(newRid);
      }
    }
  }

  @Override
  public void setCustomData(String iName, Object iValue) {
    userData.put(iName, iValue);
  }

  @Override
  public Object getCustomData(String iName) {
    return userData.get(iName);
  }

  private static Dependency[] getIndexFieldRidDependencies(Index index) {
    final IndexDefinition definition = index.getDefinition();

    if (definition == null) { // type for untyped index is still not resolved
      return null;
    }

    final PropertyType[] types = definition.getTypes();
    final Dependency[] dependencies = new Dependency[types.length];

    for (int i = 0; i < types.length; ++i) {
      dependencies[i] = getTypeRidDependency(types[i]);
    }

    return dependencies;
  }

  private static boolean isIndexMayDependOnRids(Dependency[] fieldDependencies) {
    if (fieldDependencies == null) {
      return true;
    }

    for (Dependency dependency : fieldDependencies) {
      switch (dependency) {
        case Unknown:
        case Yes:
          return true;
        case No:
          break; // do nothing
      }
    }

    return false;
  }

  private static boolean isIndexKeyMayDependOnRid(
      Object key, RID rid, Dependency[] keyDependencies) {
    if (key instanceof CompositeKey) {
      final List<Object> subKeys = ((CompositeKey) key).getKeys();
      for (int i = 0; i < subKeys.size(); ++i) {
        if (isIndexKeyMayDependOnRid(
            subKeys.get(i), rid, keyDependencies == null ? null : keyDependencies[i])) {
          return true;
        }
      }
      return false;
    }

    return isIndexKeyMayDependOnRid(key, rid, keyDependencies == null ? null : keyDependencies[0]);
  }

  private static boolean isIndexKeyMayDependOnRid(Object key, RID rid, Dependency dependency) {
    if (dependency == Dependency.No) {
      return false;
    }

    if (key instanceof Identifiable) {
      return key.equals(rid);
    }

    return dependency == Dependency.Unknown || dependency == null;
  }

  private static Dependency getTypeRidDependency(PropertyType type) {
    // fallback to the safest variant, just in case
    return switch (type) {
      case CUSTOM, ANY -> Dependency.Unknown;
      case EMBEDDED, LINK -> Dependency.Yes;
      case LINKLIST, LINKSET, LINKMAP, LINKBAG, EMBEDDEDLIST, EMBEDDEDSET, EMBEDDEDMAP ->
        // under normal conditions, collection field type is already resolved to its
        // component type
          throw new IllegalStateException("Collection field type is not allowed here");
      default -> // all other primitive types which doesn't depend on rids
          Dependency.No;
    };
  }

  private enum Dependency {
    Unknown,
    Yes,
    No
  }

  private static class KeyChangesUpdateRecord {

    final FrontendTransactionIndexChangesPerKey keyChanges;
    final FrontendTransactionIndexChanges indexChanges;

    KeyChangesUpdateRecord(
        FrontendTransactionIndexChangesPerKey keyChanges,
        FrontendTransactionIndexChanges indexChanges) {
      this.keyChanges = keyChanges;
      this.indexChanges = indexChanges;
    }
  }

  protected void checkTransactionValid() {
    if (status == TXSTATUS.INVALID) {
      throw new TransactionException(
          "Invalid state of the transaction. The transaction must be begun.");
    }
  }

  public boolean isChanged() {
    return changed;
  }

  public boolean isStartedOnServer() {
    return isAlreadyStartedOnServer;
  }

  public void setSentToServer(boolean sentToServer) {
    this.sentToServer = sentToServer;
  }

  public long getId() {
    return id;
  }

  public void clearRecordEntries() {
  }

  public void restore() {
  }

  @Override
  public int getEntryCount() {
    return recordOperations.size();
  }

  public Collection<RecordOperation> getCurrentRecordEntries() {
    return recordOperations.values();
  }

  public Collection<RecordOperation> getRecordOperations() {
    return recordOperations.values();
  }

  public RecordOperation getRecordEntry(RID ridPar) {
    assert ridPar instanceof RecordId;

    RID rid = ridPar;
    RecordOperation entry;
    do {
      entry = recordOperations.get(rid);
      if (entry == null) {
        rid = generatedOriginalRecordIdMap.get(rid);
      }
    } while (entry == null && rid != null && !rid.equals(ridPar));

    return entry;
  }

  public Map<RecordId, RecordId> getGeneratedOriginalRecordIdMap() {
    return generatedOriginalRecordIdMap;
  }

  @Override
  @Nullable
  public byte[] getMetadata() {
    if (metadata != null) {
      return metadata.metadata();
    }
    return null;
  }

  @Override
  public void storageBegun() {
    if (metadata != null) {
      metadata.notifyMetadataRead();
    }
  }

  @Override
  public void setMetadataHolder(FrontendTransacationMetadataHolder metadata) {
    this.metadata = metadata;
  }

  @Override
  public void prepareSerializedOperations() throws IOException {
    List<byte[]> operations = new ArrayList<>();
    for (RecordOperation value : recordOperations.values()) {
      FrontendTransactionDataChange change = new FrontendTransactionDataChange(database, value);
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      change.serialize(new DataOutputStream(out));
      operations.add(out.toByteArray());
    }
    this.serializedOperations = operations;
  }

  public Iterator<byte[]> getSerializedOperations() {
    if (serializedOperations != null) {
      return serializedOperations.iterator();
    } else {
      return Collections.emptyIterator();
    }
  }

  @Override
  public void resetAllocatedIds() {
    for (Map.Entry<RecordId, RecordOperation> op : recordOperations.entrySet()) {
      if (op.getValue().type == RecordOperation.CREATED) {
        var lastCreateId = op.getValue().getRecordId().copy();
        RecordId oldNew =
            new RecordId(lastCreateId.getClusterId(), op.getKey().getClusterPosition());
        updateIdentityAfterCommit(lastCreateId, oldNew);
        generatedOriginalRecordIdMap.put(oldNew, op.getKey());
      }
    }
  }

  public int getTxStartCounter() {
    return txStartCounter;
  }
}