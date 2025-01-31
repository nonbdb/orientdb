package com.jetbrains.youtrack.db.internal.core.storage.cluster.v2;

import com.jetbrains.youtrack.db.api.config.YouTrackDBConfig;
import com.jetbrains.youtrack.db.internal.common.io.FileUtils;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.YouTrackDBImpl;
import com.jetbrains.youtrack.db.internal.core.storage.cluster.LocalPaginatedClusterAbstract;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.AbstractPaginatedStorage;
import java.io.File;
import java.io.IOException;
import org.junit.BeforeClass;

public class LocalPaginatedClusterV2TestIT extends LocalPaginatedClusterAbstract {

  @BeforeClass
  public static void beforeClass() throws IOException {
    buildDirectory = System.getProperty("buildDirectory");
    if (buildDirectory == null || buildDirectory.isEmpty()) {
      buildDirectory = ".";
    }

    buildDirectory += File.separator + LocalPaginatedClusterV2TestIT.class.getSimpleName();
    FileUtils.deleteRecursively(new File(buildDirectory));

    dbName = "clusterTest";

    final var config = YouTrackDBConfig.defaultConfig();
    youTrackDB = new YouTrackDBImpl("plocal:" + buildDirectory, config);
    youTrackDB.execute(
        "create database " + dbName + " plocal users ( admin identified by 'admin' role admin)");

    databaseDocumentTx = (DatabaseSessionInternal) youTrackDB.open(dbName, "admin", "admin");

    storage = (AbstractPaginatedStorage) databaseDocumentTx.getStorage();

    paginatedCluster = new PaginatedClusterV2("paginatedClusterTest", storage);
    paginatedCluster.configure(42, "paginatedClusterTest");
    storage
        .getAtomicOperationsManager()
        .executeInsideAtomicOperation(
            null, atomicOperation -> paginatedCluster.create(atomicOperation));
  }
}
