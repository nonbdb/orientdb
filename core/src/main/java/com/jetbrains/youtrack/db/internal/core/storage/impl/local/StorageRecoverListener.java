package com.jetbrains.youtrack.db.internal.core.storage.impl.local;

/**
 * Allows listeners to be notified in case of recovering is started at storage open.
 */
public interface StorageRecoverListener {

  void onStorageRecover();
}
