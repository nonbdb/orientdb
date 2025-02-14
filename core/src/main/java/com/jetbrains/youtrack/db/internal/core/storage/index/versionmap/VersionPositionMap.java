package com.jetbrains.youtrack.db.internal.core.storage.index.versionmap;

import com.jetbrains.youtrack.db.internal.core.storage.impl.local.AbstractPaginatedStorage;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.atomicoperations.AtomicOperation;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.base.DurableComponent;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.base.DurablePage;
import java.io.IOException;

public abstract class VersionPositionMap extends DurableComponent {

  public static final String DEF_EXTENSION = ".vpm";

  public static final int MAX_CONCURRENT_DISTRIBUTED_TRANSACTIONS = 1000;
  public static final int MAGIC_SAFETY_FILL_FACTOR = 10;
  public static final int DEFAULT_VERSION_ARRAY_SIZE =
      Math.min(
          MAX_CONCURRENT_DISTRIBUTED_TRANSACTIONS * MAGIC_SAFETY_FILL_FACTOR,
          (DurablePage.MAX_PAGE_SIZE_BYTES - DurablePage.NEXT_FREE_POSITION) / Integer.BYTES);

  public VersionPositionMap(
      AbstractPaginatedStorage storage, String name, String extension, String lockName) {
    super(storage, name, extension, lockName);
  }

  // Lifecycle similar to SQLCluster (e.g. PaginatedClusterV2)
  public abstract void create(AtomicOperation atomicOperation);

  public abstract void open() throws IOException;

  public abstract void delete(AtomicOperation atomicOperation) throws IOException;

  // VPM only stores an array of type integer for versions
  public abstract void updateVersion(int versionHash);

  public abstract int getVersion(int versionHash);

  public abstract int getKeyHash(Object key);
}
