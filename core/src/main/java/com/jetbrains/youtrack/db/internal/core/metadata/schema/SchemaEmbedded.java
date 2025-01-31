package com.jetbrains.youtrack.db.internal.core.metadata.schema;

import com.jetbrains.youtrack.db.api.exception.BaseException;
import com.jetbrains.youtrack.db.api.exception.SchemaException;
import com.jetbrains.youtrack.db.api.record.DBRecord;
import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import com.jetbrains.youtrack.db.api.session.SessionListener;
import com.jetbrains.youtrack.db.internal.core.YouTrackDBEnginesManager;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseLifecycleListener;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.index.Index;
import com.jetbrains.youtrack.db.internal.core.index.IndexManagerAbstract;
import com.jetbrains.youtrack.db.internal.core.iterator.RecordIteratorCluster;
import com.jetbrains.youtrack.db.internal.core.metadata.security.Role;
import com.jetbrains.youtrack.db.internal.core.metadata.security.Rule;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class SchemaEmbedded extends SchemaShared {

  public SchemaEmbedded() {
    super();
  }

  public SchemaClass createClass(
      DatabaseSessionInternal database,
      final String className,
      int[] clusterIds,
      SchemaClass... superClasses) {
    final var wrongCharacter = SchemaShared.checkClassNameIfValid(className);
    //noinspection ConstantValue
    if (wrongCharacter != null) {
      throw new SchemaException(
          "Invalid class name found. Character '"
              + wrongCharacter
              + "' cannot be used in class name '"
              + className
              + "'");
    }

    SchemaClass result;
    var retry = 0;

    while (true) {
      try {
        result = doCreateClass(database, className, clusterIds, retry, superClasses);
        break;
      } catch (ClusterIdsAreEmptyException ignore) {
        classes.remove(className.toLowerCase(Locale.ENGLISH));
        clusterIds = createClusters(database, className);
        retry++;
      }
    }
    return result;
  }

  public SchemaClass createClass(
      DatabaseSessionInternal database,
      final String className,
      int clusters,
      SchemaClass... superClasses) {
    final var wrongCharacter = SchemaShared.checkClassNameIfValid(className);
    //noinspection ConstantValue
    if (wrongCharacter != null) {
      throw new SchemaException(
          "Invalid class name found. Character '"
              + wrongCharacter
              + "' cannot be used in class name '"
              + className
              + "'");
    }

    return doCreateClass(database, className, clusters, superClasses);
  }

  private SchemaClass doCreateClass(
      DatabaseSessionInternal database,
      final String className,
      final int clusters,
      SchemaClass... superClasses) {
    SchemaClass result;

    database.checkSecurity(Rule.ResourceGeneric.SCHEMA, Role.PERMISSION_CREATE);
    if (superClasses != null) {
      SchemaClassImpl.checkParametersConflict(Arrays.asList(superClasses));
    }
    acquireSchemaWriteLock(database);
    try {

      final var key = className.toLowerCase(Locale.ENGLISH);
      if (classes.containsKey(key)) {
        throw new SchemaException("Class '" + className + "' already exists in current database");
      }
      List<SchemaClass> superClassesList = new ArrayList<>();
      if (superClasses != null) {
        for (var superClass : superClasses) {
          // Filtering for null
          if (superClass != null) {
            superClassesList.add(superClass);
          }
        }
      }

      final int[] clusterIds;
      if (clusters > 0) {
        clusterIds = createClusters(database, className, clusters);
      } else {
        // ABSTRACT
        clusterIds = new int[]{-1};
      }

      doRealCreateClass(database, className, superClassesList, clusterIds);

      result = classes.get(className.toLowerCase(Locale.ENGLISH));
      // WAKE UP DB LIFECYCLE LISTENER
      for (var it = YouTrackDBEnginesManager.instance()
          .getDbLifecycleListeners();
          it.hasNext(); ) {
        //noinspection deprecation
        it.next().onCreateClass(database, result);
      }

      for (var oSessionListener : database.getListeners()) {
        oSessionListener.onCreateClass(database, result);
      }

    } catch (ClusterIdsAreEmptyException e) {
      throw BaseException.wrapException(
          new SchemaException("Cannot create class '" + className + "'"), e);
    } finally {
      releaseSchemaWriteLock(database);
    }

    return result;
  }

  protected void doRealCreateClass(
      DatabaseSessionInternal database,
      String className,
      List<SchemaClass> superClassesList,
      int[] clusterIds)
      throws ClusterIdsAreEmptyException {
    createClassInternal(database, className, clusterIds, superClassesList);
  }

  protected void createClassInternal(
      DatabaseSessionInternal database,
      final String className,
      final int[] clusterIdsToAdd,
      final List<SchemaClass> superClasses)
      throws ClusterIdsAreEmptyException {
    acquireSchemaWriteLock(database);
    try {
      if (className == null || className.isEmpty()) {
        throw new SchemaException("Found class name null or empty");
      }

      checkEmbedded();

      checkClustersAreAbsent(clusterIdsToAdd);

      final int[] clusterIds;
      if (clusterIdsToAdd == null || clusterIdsToAdd.length == 0) {
        throw new ClusterIdsAreEmptyException();

      } else {
        clusterIds = clusterIdsToAdd;
      }

      database.checkSecurity(Rule.ResourceGeneric.SCHEMA, Role.PERMISSION_CREATE);

      final var key = className.toLowerCase(Locale.ENGLISH);

      if (classes.containsKey(key)) {
        throw new SchemaException("Class '" + className + "' already exists in current database");
      }

      var cls = createClassInstance(className, clusterIds);

      classes.put(key, cls);

      if (superClasses != null && !superClasses.isEmpty()) {
        cls.setSuperClassesInternal(database, superClasses);
        for (var superClass : superClasses) {
          // UPDATE INDEXES
          final var clustersToIndex = superClass.getPolymorphicClusterIds();
          final var clusterNames = new String[clustersToIndex.length];
          for (var i = 0; i < clustersToIndex.length; i++) {
            clusterNames[i] = database.getClusterNameById(clustersToIndex[i]);
          }

          for (var index : ((SchemaClassInternal) superClass).getIndexesInternal(database)) {
            for (var clusterName : clusterNames) {
              if (clusterName != null) {
                database
                    .getMetadata()
                    .getIndexManagerInternal()
                    .addClusterToIndex(database, clusterName, index.getName());
              }
            }
          }
        }
      }

      addClusterClassMap(cls);

    } finally {
      releaseSchemaWriteLock(database);
    }
  }

  protected SchemaClassImpl createClassInstance(String className, int[] clusterIds) {
    return new SchemaClassEmbedded(this, className, clusterIds);
  }

  public SchemaClass getOrCreateClass(
      DatabaseSessionInternal database, final String iClassName,
      final SchemaClass... superClasses) {
    if (iClassName == null) {
      return null;
    }

    acquireSchemaReadLock();
    try {
      SchemaClass cls = classes.get(iClassName.toLowerCase(Locale.ENGLISH));
      if (cls != null) {
        return cls;
      }
    } finally {
      releaseSchemaReadLock();
    }

    SchemaClass cls;

    int[] clusterIds = null;
    var retry = 0;

    while (true) {
      try {
        acquireSchemaWriteLock(database);
        try {
          cls = classes.get(iClassName.toLowerCase(Locale.ENGLISH));
          if (cls != null) {
            return cls;
          }

          cls = doCreateClass(database, iClassName, clusterIds, retry, superClasses);
          addClusterClassMap(cls);
        } finally {
          releaseSchemaWriteLock(database);
        }
        break;
      } catch (ClusterIdsAreEmptyException ignore) {
        clusterIds = createClusters(database, iClassName);
        retry++;
      }
    }

    return cls;
  }

  protected SchemaClass doCreateClass(
      DatabaseSessionInternal database,
      final String className,
      int[] clusterIds,
      int retry,
      SchemaClass... superClasses)
      throws ClusterIdsAreEmptyException {
    SchemaClass result;
    database.checkSecurity(Rule.ResourceGeneric.SCHEMA, Role.PERMISSION_CREATE);
    if (superClasses != null) {
      SchemaClassImpl.checkParametersConflict(Arrays.asList(superClasses));
    }

    acquireSchemaWriteLock(database);
    try {

      final var key = className.toLowerCase(Locale.ENGLISH);
      if (classes.containsKey(key) && retry == 0) {
        throw new SchemaException("Class '" + className + "' already exists in current database");
      }

      checkClustersAreAbsent(clusterIds);

      if (clusterIds == null || clusterIds.length == 0) {
        clusterIds =
            createClusters(
                database,
                className,
                database.getStorageInfo().getConfiguration().getMinimumClusters());
      }
      List<SchemaClass> superClassesList = new ArrayList<>();
      if (superClasses != null) {
        for (var superClass : superClasses) {
          if (superClass != null) {
            superClassesList.add(superClass);
          }
        }
      }

      doRealCreateClass(database, className, superClassesList, clusterIds);

      result = classes.get(className.toLowerCase(Locale.ENGLISH));

      // WAKE UP DB LIFECYCLE LISTENER
      for (var it = YouTrackDBEnginesManager.instance()
          .getDbLifecycleListeners();
          it.hasNext(); ) {
        //noinspection deprecation
        it.next().onCreateClass(database, result);
      }

      for (var oSessionListener : database.getListeners()) {
        oSessionListener.onCreateClass(database, result);
      }

    } finally {
      releaseSchemaWriteLock(database);
    }

    return result;
  }

  private int[] createClusters(DatabaseSessionInternal database, final String iClassName) {
    return createClusters(
        database, iClassName, database.getStorageInfo().getConfiguration().getMinimumClusters());
  }

  protected int[] createClusters(
      DatabaseSessionInternal database, String className, int minimumClusters) {
    className = className.toLowerCase(Locale.ENGLISH);

    int[] clusterIds;

    if (internalClasses.contains(className.toLowerCase(Locale.ENGLISH))) {
      // INTERNAL CLASS, SET TO 1
      minimumClusters = 1;
    }

    clusterIds = new int[minimumClusters];
    clusterIds[0] = database.getClusterIdByName(className);
    if (clusterIds[0] > -1) {
      // CHECK THE CLUSTER HAS NOT BEEN ALREADY ASSIGNED
      final var cls = clustersToClasses.get(clusterIds[0]);
      if (cls != null) {
        clusterIds[0] = database.addCluster(getNextAvailableClusterName(database, className));
      }
    } else
    // JUST KEEP THE CLASS NAME. THIS IS FOR LEGACY REASONS
    {
      clusterIds[0] = database.addCluster(className);
    }

    for (var i = 1; i < minimumClusters; ++i) {
      clusterIds[i] = database.addCluster(getNextAvailableClusterName(database, className));
    }

    return clusterIds;
  }

  private static String getNextAvailableClusterName(
      DatabaseSessionInternal database, final String className) {
    for (var i = 1; ; ++i) {
      final var clusterName = className + "_" + i;
      if (database.getClusterIdByName(clusterName) < 0)
      // FREE NAME
      {
        return clusterName;
      }
    }
  }

  protected void checkClustersAreAbsent(final int[] iClusterIds) {
    if (iClusterIds == null) {
      return;
    }

    for (var clusterId : iClusterIds) {
      if (clusterId < 0) {
        continue;
      }

      if (clustersToClasses.containsKey(clusterId)) {
        throw new SchemaException(
            "Cluster with id "
                + clusterId
                + " already belongs to class "
                + clustersToClasses.get(clusterId));
      }
    }
  }

  public void dropClass(DatabaseSessionInternal database, final String className) {
    acquireSchemaWriteLock(database);
    try {
      if (database.getTransaction().isActive()) {
        throw new IllegalStateException("Cannot drop a class inside a transaction");
      }

      if (className == null) {
        throw new IllegalArgumentException("Class name is null");
      }

      database.checkSecurity(Rule.ResourceGeneric.SCHEMA, Role.PERMISSION_DELETE);

      final var key = className.toLowerCase(Locale.ENGLISH);

      SchemaClass cls = classes.get(key);

      if (cls == null) {
        throw new SchemaException("Class '" + className + "' was not found in current database");
      }

      if (!cls.getSubclasses().isEmpty()) {
        throw new SchemaException(
            "Class '"
                + className
                + "' cannot be dropped because it has sub classes "
                + cls.getSubclasses()
                + ". Remove the dependencies before trying to drop it again");
      }

      doDropClass(database, className);

      var localCache = database.getLocalCache();
      for (var clusterId : cls.getClusterIds()) {
        localCache.freeCluster(clusterId);
      }
    } finally {
      releaseSchemaWriteLock(database);
    }
  }

  protected void doDropClass(DatabaseSessionInternal database, String className) {
    dropClassInternal(database, className);
  }

  protected void dropClassInternal(DatabaseSessionInternal database, final String className) {
    acquireSchemaWriteLock(database);
    try {
      if (database.getTransaction().isActive()) {
        throw new IllegalStateException("Cannot drop a class inside a transaction");
      }

      if (className == null) {
        throw new IllegalArgumentException("Class name is null");
      }

      database.checkSecurity(Rule.ResourceGeneric.SCHEMA, Role.PERMISSION_DELETE);

      final var key = className.toLowerCase(Locale.ENGLISH);

      final SchemaClass cls = classes.get(key);
      if (cls == null) {
        throw new SchemaException("Class '" + className + "' was not found in current database");
      }

      if (!cls.getSubclasses().isEmpty()) {
        throw new SchemaException(
            "Class '"
                + className
                + "' cannot be dropped because it has sub classes "
                + cls.getSubclasses()
                + ". Remove the dependencies before trying to drop it again");
      }

      checkEmbedded();

      for (var superClass : cls.getSuperClasses()) {
        // REMOVE DEPENDENCY FROM SUPERCLASS
        ((SchemaClassImpl) superClass).removeBaseClassInternal(database, cls);
      }
      for (var id : cls.getClusterIds()) {
        if (id != -1) {
          deleteCluster(database, id);
        }
      }

      dropClassIndexes(database, cls);

      classes.remove(key);

      if (cls.getShortName() != null)
      // REMOVE THE ALIAS TOO
      {
        classes.remove(cls.getShortName().toLowerCase(Locale.ENGLISH));
      }

      removeClusterClassMap(cls);

      // WAKE UP DB LIFECYCLE LISTENER
      for (var it = YouTrackDBEnginesManager.instance()
          .getDbLifecycleListeners();
          it.hasNext(); ) {
        //noinspection deprecation
        it.next().onDropClass(database, cls);
      }

      for (var oSessionListener : database.getListeners()) {
        oSessionListener.onDropClass(database, cls);
      }
    } finally {
      releaseSchemaWriteLock(database);
    }
  }

  protected SchemaClassImpl createClassInstance(String name) {
    return new SchemaClassEmbedded(this, name);
  }


  private static void dropClassIndexes(DatabaseSessionInternal database, final SchemaClass cls) {
    final var indexManager = database.getMetadata().getIndexManagerInternal();

    for (final var index : indexManager.getClassIndexes(database, cls.getName())) {
      indexManager.dropIndex(database, index.getName());
    }
  }

  private static void deleteCluster(final DatabaseSessionInternal db, final int clusterId) {
    final var clusterName = db.getClusterNameById(clusterId);
    if (clusterName != null) {
      final var iteratorCluster = db.browseCluster(clusterName);
      if (iteratorCluster != null) {
        db.executeInTxBatches(
            (Iterable<DBRecord>) iteratorCluster, (session, record) -> record.delete());
        db.dropClusterInternal(clusterId);
      }
    }

    db.getLocalCache().freeCluster(clusterId);
  }

  private void removeClusterClassMap(final SchemaClass cls) {
    for (var clusterId : cls.getClusterIds()) {
      if (clusterId < 0) {
        continue;
      }

      clustersToClasses.remove(clusterId);
    }
  }

  public void checkEmbedded() {
  }

  void addClusterForClass(
      DatabaseSessionInternal database, final int clusterId, final SchemaClass cls) {
    acquireSchemaWriteLock(database);
    try {
      if (clusterId < 0) {
        return;
      }

      checkEmbedded();

      final var existingCls = clustersToClasses.get(clusterId);
      if (existingCls != null && !cls.equals(existingCls)) {
        throw new SchemaException(
            "Cluster with id "
                + clusterId
                + " already belongs to class "
                + clustersToClasses.get(clusterId));
      }

      clustersToClasses.put(clusterId, cls);
    } finally {
      releaseSchemaWriteLock(database);
    }
  }


  void removeClusterForClass(DatabaseSessionInternal database, int clusterId) {
    acquireSchemaWriteLock(database);
    try {
      if (clusterId < 0) {
        return;
      }

      checkEmbedded();

      clustersToClasses.remove(clusterId);
    } finally {
      releaseSchemaWriteLock(database);
    }
  }
}
