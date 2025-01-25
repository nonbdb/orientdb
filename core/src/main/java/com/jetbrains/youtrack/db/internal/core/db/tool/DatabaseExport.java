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
package com.jetbrains.youtrack.db.internal.core.db.tool;

import com.jetbrains.youtrack.db.api.record.RID;
import com.jetbrains.youtrack.db.api.record.Record;
import com.jetbrains.youtrack.db.api.schema.Property;
import com.jetbrains.youtrack.db.api.schema.Schema;
import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import com.jetbrains.youtrack.db.internal.common.io.FileUtils;
import com.jetbrains.youtrack.db.internal.common.io.YTIOException;
import com.jetbrains.youtrack.db.internal.common.log.LogManager;
import com.jetbrains.youtrack.db.internal.core.YouTrackDBConstants;
import com.jetbrains.youtrack.db.internal.core.command.CommandOutputListener;
import com.jetbrains.youtrack.db.internal.core.config.StorageConfiguration;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.index.Index;
import com.jetbrains.youtrack.db.internal.core.index.IndexManagerAbstract;
import com.jetbrains.youtrack.db.internal.core.iterator.RecordIteratorCluster;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.SchemaShared;
import com.jetbrains.youtrack.db.internal.core.record.RecordAbstract;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.JSONWriter;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.string.RecordSerializerJackson;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;

/**
 * Export data from a database to a file.
 */
public class DatabaseExport extends DatabaseImpExpAbstract {

  public static final int EXPORTER_VERSION = 13;

  protected JSONWriter writer;
  protected long recordExported;
  protected int compressionLevel = Deflater.BEST_SPEED;
  protected int compressionBuffer = 16384; // 16Kb

  private final String tempFileName;

  public DatabaseExport(
      final DatabaseSessionInternal iDatabase,
      final String iFileName,
      final CommandOutputListener iListener)
      throws IOException {
    super(iDatabase, iFileName, iListener);
    if (iDatabase.isRemote()) {
      throw new DatabaseExportException("Database export can be done only in embedded environment");
    }

    if (fileName == null) {
      throw new IllegalArgumentException("file name missing");
    }

    if (!fileName.endsWith(".gz")) {
      fileName += ".gz";
    }
    FileUtils.prepareForFileCreationOrReplacement(Paths.get(fileName), this, "exporting");

    this.tempFileName = fileName + ".tmp";
    FileUtils.prepareForFileCreationOrReplacement(Paths.get(tempFileName), this, "exporting");

    final GZIPOutputStream gzipOS =
        new GZIPOutputStream(new FileOutputStream(tempFileName), compressionBuffer) {
          {
            def.setLevel(compressionLevel);
          }
        };

    writer = new JSONWriter(new OutputStreamWriter(gzipOS));
    writer.beginObject();
  }

  public DatabaseExport(
      final DatabaseSessionInternal iDatabase,
      final OutputStream iOutputStream,
      final CommandOutputListener iListener)
      throws IOException {
    super(iDatabase, "streaming", iListener);
    this.tempFileName = null;

    writer = new JSONWriter(new OutputStreamWriter(iOutputStream));
    writer.beginObject();
  }

  @Override
  public void run() {
    exportDatabase();
  }

  @Override
  public DatabaseExport setOptions(final String s) {
    super.setOptions(s);
    return this;
  }

  public DatabaseExport exportDatabase() {
    try {
      listener.onMessage(
          "\nStarted export of database '" + database.getName() + "' to " + fileName + "...");

      long time = System.nanoTime();

      exportInfo();
      exportClusters();
      exportSchema();
      exportRecords();
      exportIndexDefinitions();

      listener.onMessage(
          "\n\nDatabase export completed in " + ((System.nanoTime() - time) / 1000000) + "ms");

      writer.flush();
    } catch (Exception e) {
      LogManager.instance()
          .error(this, "Error on exporting database '%s' to: %s", e, database.getName(), fileName);
      throw new DatabaseExportException(
          "Error on exporting database '" + database.getName() + "' to: " + fileName, e);
    } finally {
      close();
    }
    return this;
  }

  private void exportRecords() throws IOException {
    long totalFoundRecords = 0;
    long totalExportedRecords = 0;

    int level = 1;
    listener.onMessage("\nExporting records...");

    final Set<RID> brokenRids = new HashSet<>();

    writer.beginCollection(database, level, true, "records");
    int exportedClusters = 0;
    int maxClusterId = getMaxClusterId();
    for (int i = 0; exportedClusters <= maxClusterId; ++i) {
      String clusterName = database.getClusterNameById(i);

      exportedClusters++;

      long clusterExportedRecordsTot = 0;
      if (clusterName != null) {
        // CHECK IF THE CLUSTER IS INCLUDED
        clusterExportedRecordsTot = database.countClusterElements(clusterName);
      }

      listener.onMessage(
          "\n- Cluster "
              + (clusterName != null ? "'" + clusterName + "'" : "NULL")
              + " (id="
              + i
              + ")...");

      long clusterExportedRecordsCurrent = 0;
      if (clusterName != null) {
        RecordAbstract rec = null;
        try {
          RecordIteratorCluster<Record> it = database.browseCluster(clusterName);

          while (it.hasNext()) {
            rec = (RecordAbstract) it.next();
            if (rec instanceof EntityImpl entity) {
              // CHECK IF THE CLASS OF THE DOCUMENT IS INCLUDED
              final String className =
                  entity.getClassName() != null
                      ? entity.getClassName().toUpperCase(Locale.ENGLISH)
                      : null;
            }

            if (exportRecord(
                clusterExportedRecordsTot, clusterExportedRecordsCurrent, rec, brokenRids)) {
              clusterExportedRecordsCurrent++;
            }
          }

          brokenRids.addAll(it.getBrokenRIDs());
        } catch (YTIOException e) {
          LogManager.instance()
              .error(
                  this,
                  "\nError on exporting record %s because of I/O problems",
                  e,
                  rec == null ? null : rec.getIdentity());
          // RE-THROW THE EXCEPTION UP
          throw e;
        } catch (Exception t) {
          if (rec != null) {
            final byte[] buffer = rec.toStream();

            LogManager.instance()
                .error(
                    this,
                    """
                        
                        Error on exporting record %s. It seems corrupted; size: %d bytes, raw\
                         content (as string):
                        ==========
                        %s
                        ==========""",
                    t,
                    rec.getIdentity(),
                    buffer.length,
                    new String(buffer));
          }
        }
      }

      listener.onMessage(
          "OK (records=" + clusterExportedRecordsCurrent + "/" + clusterExportedRecordsTot + ")");

      totalExportedRecords += clusterExportedRecordsCurrent;
      totalFoundRecords += clusterExportedRecordsTot;
    }
    writer.endCollection(level, true);

    listener.onMessage(
        "\n\nDone. Exported "
            + totalExportedRecords
            + " of total "
            + totalFoundRecords
            + " records. "
            + brokenRids.size()
            + " records were detected as broken\n");

    writer.beginCollection(database, level, true, "brokenRids");

    boolean firsBrokenRid = true;
    for (final RID rid : brokenRids) {
      if (firsBrokenRid) {
        firsBrokenRid = false;
      } else {
        writer.append(",");
      }
      writer.append(rid.toString());
    }
    writer.endCollection(level, true);
  }

  public void close() {

    if (writer == null) {
      return;
    }

    try {
      writer.endObject();
      writer.close();
      writer = null;
    } catch (IOException e) {
      LogManager.instance()
          .error(this, "Error on exporting database '%s' to: %s", e, database.getName(), fileName);
      throw new DatabaseExportException(
          "Error on exporting database '" + database.getName() + "' to: " + fileName, e);
    }

    if (tempFileName != null) // may be null if writing to an output stream w/o file
    {
      try {
        FileUtils.atomicMoveWithFallback(Paths.get(tempFileName), Paths.get(fileName), this);
      } catch (IOException e) {
        LogManager.instance()
            .error(
                this, "Error on exporting database '%s' to: %s", e, database.getName(), fileName);
        throw new DatabaseExportException(
            "Error on exporting database '" + database.getName() + "' to: " + fileName, e);
      }
    }
  }

  private int getMaxClusterId() {
    int totalCluster = -1;
    for (String clusterName : database.getClusterNames()) {
      if (database.getClusterIdByName(clusterName) > totalCluster) {
        totalCluster = database.getClusterIdByName(clusterName);
      }
    }
    return totalCluster;
  }

  @Override
  protected void parseSetting(final String option, final List<String> items) {
    if (option.equalsIgnoreCase("-compressionLevel")) {
      compressionLevel = Integer.parseInt(items.get(0));
    } else if (option.equalsIgnoreCase("-compressionBuffer")) {
      compressionBuffer = Integer.parseInt(items.get(0));
    } else {
      super.parseSetting(option, items);
    }
  }

  private void exportClusters() throws IOException {
    listener.onMessage("\nExporting clusters...");

    writer.beginCollection(database, 1, true, "clusters");
    int exportedClusters = 0;

    int maxClusterId = getMaxClusterId();

    for (int clusterId = 0; clusterId <= maxClusterId; ++clusterId) {

      final String clusterName = database.getClusterNameById(clusterId);

      // exclude removed clusters
      if (clusterName == null) {
        continue;
      }

      // CHECK IF THE CLUSTER IS INCLUDED
      writer.beginObject(2, true, null);

      writer.writeAttribute(database, 0, false, "name", clusterName);
      writer.writeAttribute(database, 0, false, "id", clusterId);

      exportedClusters++;
      writer.endObject(2, false);
    }

    listener.onMessage("OK (" + exportedClusters + " clusters)");

    writer.endCollection(1, true);
  }

  private void exportInfo() throws IOException {
    listener.onMessage("\nExporting database info...");

    writer.beginObject(1, true, "info");
    writer.writeAttribute(database, 2, true,
        "name", database.getName().replace('\\', '/'));
    writer.writeAttribute(database, 2, true,
        "default-cluster-id", database.getDefaultClusterId());
    writer.writeAttribute(database, 2, true,
        "exporter-version", EXPORTER_VERSION);
    writer.writeAttribute(database, 2, true,
        "engine-version", YouTrackDBConstants.getVersion());
    final String engineBuild = YouTrackDBConstants.getBuildNumber();
    if (engineBuild != null) {
      writer.writeAttribute(database, 2, true, "engine-build", engineBuild);
    }
    writer.writeAttribute(database, 2, true, "storage-config-version",
        StorageConfiguration.CURRENT_VERSION);
    writer.writeAttribute(database, 2, true, "schema-version", SchemaShared.CURRENT_VERSION_NUMBER);
    writer.writeAttribute(database,
        2,
        true,
        "schemaRecordId", database.getStorageInfo().getConfiguration().getSchemaRecordId());
    writer.writeAttribute(database,
        2,
        true,
        "indexMgrRecordId", database.getStorageInfo().getConfiguration().getIndexMgrRecordId());
    writer.endObject(1, true);

    listener.onMessage("OK");
  }

  private void exportIndexDefinitions() throws IOException {
    listener.onMessage("\nExporting index info...");
    writer.beginCollection(database, 1, true, "indexes");

    final IndexManagerAbstract indexManager = database.getMetadata().getIndexManagerInternal();
    indexManager.reload(database);

    final Collection<? extends Index> indexes = indexManager.getIndexes(database);

    for (Index index : indexes) {
      final String clsName =
          index.getDefinition() != null ? index.getDefinition().getClassName() : null;
      if (DatabaseImport.EXPORT_IMPORT_CLASS_NAME.equals(clsName)) {
        continue;
      }

      // CHECK TO FILTER CLASS
      listener.onMessage("\n- Index " + index.getName() + "...");
      writer.beginObject(2, true, null);
      writer.writeAttribute(database, 3, true, "name", index.getName());
      writer.writeAttribute(database, 3, true, "type", index.getType());
      if (index.getAlgorithm() != null) {
        writer.writeAttribute(database, 3, true, "algorithm", index.getAlgorithm());
      }

      if (!index.getClusters().isEmpty()) {
        writer.writeAttribute(database, 3, true, "clustersToIndex", index.getClusters());
      }

      if (index.getDefinition() != null) {
        writer.beginObject(4, true, "definition");

        writer.writeAttribute(database, 5, true, "defClass",
            index.getDefinition().getClass().getName());
        writer.writeAttribute(database, 5, true,
            "stream", index.getDefinition().toStream(database, new EntityImpl(database)));

        writer.endObject(4, true);
      }

      final var metadata = index.getMetadata();
      if (metadata != null) {
        var entity = new EntityImpl(database);
        entity.updateFromMap(metadata);

        writer.writeAttribute(database, 4, true, "metadata", entity);
      }

      final EntityImpl configuration = index.getConfiguration(database);
      if (configuration.field("blueprintsIndexClass") != null) {
        writer.writeAttribute(database,
            4, true, "blueprintsIndexClass", configuration.field("blueprintsIndexClass"));
      }

      writer.endObject(2, true);
      listener.onMessage("OK");
    }

    writer.endCollection(1, true);
    listener.onMessage("\nOK (" + indexes.size() + " indexes)");
  }

  private void exportSchema() throws IOException {
    listener.onMessage("\nExporting schema...");

    writer.beginObject(1, true, "schema");
    final Schema schema = (database.getMetadata()).getImmutableSchemaSnapshot();
    //noinspection deprecation
    writer.writeAttribute(database, 2, true, "version", schema.getVersion());
    writer.writeAttribute(database, 2, false, "blob-clusters", database.getBlobClusterIds());
    if (!schema.getClasses().isEmpty()) {
      writer.beginCollection(database, 2, true, "classes");

      final List<SchemaClass> classes = new ArrayList<>(schema.getClasses());
      Collections.sort(classes);

      for (SchemaClass cls : classes) {
        // CHECK TO FILTER CLASS
        writer.beginObject(3, true, null);
        writer.writeAttribute(database, 0, false, "name", cls.getName());
        writer.writeAttribute(database, 0, false, "cluster-ids", cls.getClusterIds());
        if (cls.isStrictMode()) {
          writer.writeAttribute(database, 0, false, "strictMode", cls.isStrictMode());
        }
        if (!cls.getSuperClasses().isEmpty()) {
          writer.writeAttribute(database, 0, false, "super-classes", cls.getSuperClassesNames());
        }
        if (cls.getShortName() != null) {
          writer.writeAttribute(database, 0, false, "short-name", cls.getShortName());
        }
        if (cls.isAbstract()) {
          writer.writeAttribute(database, 0, false, "abstract", cls.isAbstract());
        }
        writer.writeAttribute(database,
            0, false, "cluster-selection", cls.getClusterSelectionStrategyName()); // @SINCE 1.7

        if (!cls.properties(database).isEmpty()) {
          writer.beginCollection(database, 4, true, "properties");

          final List<Property> properties = new ArrayList<>(cls.declaredProperties());
          Collections.sort(properties);

          for (Property p : properties) {
            writer.beginObject(5, true, null);
            writer.writeAttribute(database, 0, false, "name", p.getName());
            writer.writeAttribute(database, 0, false, "type", p.getType().toString());
            if (p.isMandatory()) {
              writer.writeAttribute(database, 0, false, "mandatory", p.isMandatory());
            }
            if (p.isReadonly()) {
              writer.writeAttribute(database, 0, false, "readonly", p.isReadonly());
            }
            if (p.isNotNull()) {
              writer.writeAttribute(database, 0, false, "not-null", p.isNotNull());
            }
            if (p.getLinkedClass() != null) {
              writer.writeAttribute(database, 0, false, "linked-class",
                  p.getLinkedClass().getName());
            }
            if (p.getLinkedType() != null) {
              writer.writeAttribute(database, 0, false, "linked-type",
                  p.getLinkedType().toString());
            }
            if (p.getMin() != null) {
              writer.writeAttribute(database, 0, false, "min", p.getMin());
            }
            if (p.getMax() != null) {
              writer.writeAttribute(database, 0, false, "max", p.getMax());
            }
            if (p.getCollate() != null) {
              writer.writeAttribute(database, 0, false, "collate", p.getCollate().getName());
            }
            if (p.getDefaultValue() != null) {
              writer.writeAttribute(database, 0, false, "default-value", p.getDefaultValue());
            }
            if (p.getRegexp() != null) {
              writer.writeAttribute(database, 0, false, "regexp", p.getRegexp());
            }
            final Set<String> customKeys = p.getCustomKeys();
            final Map<String, String> custom = new HashMap<>();
            for (String key : customKeys) {
              custom.put(key, p.getCustom(key));
            }

            if (!custom.isEmpty()) {
              writer.writeAttribute(database, 0, false, "customFields", custom);
            }

            writer.endObject(0, false);
          }
          writer.endCollection(4, true);
        }
        final Set<String> customKeys = cls.getCustomKeys();
        final Map<String, String> custom = new HashMap<>();
        for (String key : customKeys) {
          custom.put(key, cls.getCustom(key));
        }

        if (!custom.isEmpty()) {
          writer.writeAttribute(database, 0, false, "customFields", custom);
        }
        writer.endObject(3, true);
      }
      writer.endCollection(2, true);
    }

    writer.endObject(1, true);

    listener.onMessage("OK (" + schema.getClasses().size() + " classes)");
  }

  private boolean exportRecord(
      long recordTot, long recordNum, RecordAbstract rec, Set<RID> brokenRids) {
    if (rec != null) {
      try {
        if (useLineFeedForRecords) {
          writer.append("\n");
        }
        if (recordExported > 0) {
          writer.append(",");
        }

        final String format = RecordAbstract.BASE_FORMAT + ",dateAsLong";
        RecordSerializerJackson.toString(database, rec, writer, format);

        recordExported++;
        recordNum++;

        if (recordTot > 10 && (recordNum + 1) % (recordTot / 10) == 0) {
          listener.onMessage(".");
        }

        return true;
      } catch (final Exception t) {
        final RID rid = rec.getIdentity().copy();

        if (rid != null) {
          brokenRids.add(rid);
        }

        final byte[] buffer = rec.toStream();

        LogManager.instance()
            .error(
                this,
                """
                    
                    Error on exporting record %s. It seems corrupted; size: %d bytes, raw\
                     content (as string):
                    ==========
                    %s
                    ==========""",
                t,
                rec.getIdentity(),
                buffer.length,
                new String(buffer));
      }
    }

    return false;
  }
}
