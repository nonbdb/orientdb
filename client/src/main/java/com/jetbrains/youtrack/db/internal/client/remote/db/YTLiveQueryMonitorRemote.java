package com.jetbrains.youtrack.db.internal.client.remote.db;

import com.jetbrains.youtrack.db.api.query.LiveQueryMonitor;

/**
 *
 */
public class YTLiveQueryMonitorRemote implements LiveQueryMonitor {

  private final DatabaseSessionRemote database;
  private final int monitorId;

  public YTLiveQueryMonitorRemote(DatabaseSessionRemote database, int monitorId) {
    this.database = database;
    this.monitorId = monitorId;
  }

  @Override
  public void unSubscribe() {
    database.getStorageRemote().unsubscribeLive(database, this.monitorId);
  }

  @Override
  public int getMonitorId() {
    return monitorId;
  }
}
