package com.jetbrains.youtrack.db.internal.client.remote.db;

import com.jetbrains.youtrack.db.api.config.GlobalConfiguration;
import com.jetbrains.youtrack.db.internal.client.remote.YouTrackDBRemote;
import com.jetbrains.youtrack.db.internal.client.remote.metadata.schema.SchemaRemote;
import com.jetbrains.youtrack.db.internal.client.remote.metadata.security.SecurityRemote;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.SharedContext;
import com.jetbrains.youtrack.db.internal.core.db.StringCache;
import com.jetbrains.youtrack.db.internal.core.index.IndexManagerRemote;
import com.jetbrains.youtrack.db.internal.core.metadata.function.FunctionLibraryImpl;
import com.jetbrains.youtrack.db.internal.core.metadata.sequence.SequenceLibraryImpl;
import com.jetbrains.youtrack.db.internal.core.schedule.SchedulerImpl;
import com.jetbrains.youtrack.db.internal.core.storage.StorageInfo;

/**
 *
 */
public class SharedContextRemote extends SharedContext {

  public SharedContextRemote(StorageInfo storage, YouTrackDBRemote youTrackDbRemote) {
    stringCache =
        new StringCache(
            youTrackDbRemote
                .getContextConfiguration()
                .getValueAsInteger(GlobalConfiguration.DB_STRING_CAHCE_SIZE));
    this.youtrackDB = youTrackDbRemote;
    this.storage = storage;
    schema = new SchemaRemote();
    security = new SecurityRemote();
    indexManager = new IndexManagerRemote(storage);
    functionLibrary = new FunctionLibraryImpl();
    scheduler = new SchedulerImpl(youtrackDB);
    sequenceLibrary = new SequenceLibraryImpl();
  }

  public synchronized void load(DatabaseSessionInternal database) {
    if (loaded) {
      return;
    }

    schema.load(database);
    indexManager.load(database);
    // The Immutable snapshot should be after index and schema that require and before
    // everything else that use it
    schema.forceSnapshot(database);
    security.load(database);
    sequenceLibrary.load(database);
    schema.onPostIndexManagement(database);
    loaded = true;
  }

  @Override
  public synchronized void close() {
    stringCache.close();
    schema.close();
    security.close();
    indexManager.close();
    sequenceLibrary.close();
    loaded = false;
  }

  public synchronized void reload(DatabaseSessionInternal database) {
    schema.reload(database);
    indexManager.reload(database);
    // The Immutable snapshot should be after index and schema that require and before everything
    // else that use it
    schema.forceSnapshot(database);
    security.load(database);
    scheduler.load(database);
    sequenceLibrary.load(database);
    functionLibrary.load(database);
  }
}
