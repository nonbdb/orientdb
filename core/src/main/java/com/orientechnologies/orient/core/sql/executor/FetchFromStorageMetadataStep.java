package com.orientechnologies.orient.core.sql.executor;

import com.orientechnologies.common.concur.OTimeoutException;
import com.orientechnologies.common.log.OLogManager;
import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.config.OStorageConfiguration;
import com.orientechnologies.orient.core.config.OStorageEntryConfiguration;
import com.orientechnologies.orient.core.db.ODatabaseSessionInternal;
import com.orientechnologies.orient.core.sql.executor.resultset.OExecutionStream;
import com.orientechnologies.orient.core.sql.executor.resultset.OProduceExecutionStream;
import com.orientechnologies.orient.core.storage.OCluster;
import com.orientechnologies.orient.core.storage.OStorage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Returns an OResult containing metadata regarding the storage
 */
public class FetchFromStorageMetadataStep extends AbstractExecutionStep {

  public FetchFromStorageMetadataStep(OCommandContext ctx, boolean profilingEnabled) {
    super(ctx, profilingEnabled);
  }

  @Override
  public OExecutionStream internalStart(OCommandContext ctx) throws OTimeoutException {
    if (prev != null) {
      prev.start(ctx).close(ctx);
    }

    return new OProduceExecutionStream(this::produce).limit(1);
  }

  private OResult produce(OCommandContext ctx) {
    ODatabaseSessionInternal db = ctx.getDatabase();
    OResultInternal result = new OResultInternal(db);

    OStorage storage = db.getStorage();
    result.setProperty("clusters", toResult(db, storage.getClusterInstances()));
    result.setProperty("defaultClusterId", storage.getDefaultClusterId());
    result.setProperty("totalClusters", storage.getClusters());
    result.setProperty("configuration", toResult(db, storage.getConfiguration()));
    result.setProperty(
        "conflictStrategy",
        storage.getRecordConflictStrategy() == null
            ? null
            : storage.getRecordConflictStrategy().getName());
    result.setProperty("name", storage.getName());
    result.setProperty("size", storage.getSize(ctx.getDatabase()));
    result.setProperty("type", storage.getType());
    result.setProperty("version", storage.getVersion());
    result.setProperty("createdAtVersion", storage.getCreatedAtVersion());
    return result;
  }

  private static Object toResult(ODatabaseSessionInternal db,
      OStorageConfiguration configuration) {
    OResultInternal result = new OResultInternal(db);
    result.setProperty("charset", configuration.getCharset());
    result.setProperty("clusterSelection", configuration.getClusterSelection());
    result.setProperty("conflictStrategy", configuration.getConflictStrategy());
    result.setProperty("dateFormat", configuration.getDateFormat());
    result.setProperty("dateTimeFormat", configuration.getDateTimeFormat());
    result.setProperty("localeCountry", configuration.getLocaleCountry());
    result.setProperty("localeLanguage", configuration.getLocaleLanguage());
    result.setProperty("recordSerializer", configuration.getRecordSerializer());
    result.setProperty("timezone", String.valueOf(configuration.getTimeZone()));
    result.setProperty("properties", toResult(db, configuration.getProperties()));
    return result;
  }

  private static List<OResult> toResult(ODatabaseSessionInternal db,
      List<OStorageEntryConfiguration> properties) {
    List<OResult> result = new ArrayList<>();
    if (properties != null) {
      for (OStorageEntryConfiguration entry : properties) {
        OResultInternal item = new OResultInternal(db);
        item.setProperty("name", entry.name);
        item.setProperty("value", entry.value);
        result.add(item);
      }
    }
    return result;
  }

  private List<OResult> toResult(ODatabaseSessionInternal db,
      Collection<? extends OCluster> clusterInstances) {
    List<OResult> result = new ArrayList<>();
    if (clusterInstances != null) {
      for (OCluster cluster : clusterInstances) {
        OResultInternal item = new OResultInternal(db);
        item.setProperty("name", cluster.getName());
        item.setProperty("fileName", cluster.getFileName());
        item.setProperty("id", cluster.getId());
        item.setProperty("entries", cluster.getEntries());
        item.setProperty(
            "conflictStrategy",
            cluster.getRecordConflictStrategy() == null
                ? null
                : cluster.getRecordConflictStrategy().getName());
        item.setProperty("tombstonesCount", cluster.getTombstonesCount());
        try {
          item.setProperty("encryption", cluster.encryption());
        } catch (Exception e) {
          OLogManager.instance().error(this, "Can not set value of encryption parameter", e);
        }
        result.add(item);
      }
    }
    return result;
  }

  @Override
  public String prettyPrint(int depth, int indent) {
    String spaces = OExecutionStepInternal.getIndent(depth, indent);
    String result = spaces + "+ FETCH STORAGE METADATA";
    if (profilingEnabled) {
      result += " (" + getCostFormatted() + ")";
    }
    return result;
  }
}
