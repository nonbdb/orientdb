package com.orientechnologies.orient.distributed.impl.structural.raft;

import com.orientechnologies.orient.core.db.config.ONodeIdentity;
import com.orientechnologies.orient.distributed.OrientDBDistributed;
import com.orientechnologies.orient.distributed.impl.coordinator.OLogId;
import com.orientechnologies.orient.distributed.impl.coordinator.OOperationLog;
import com.orientechnologies.orient.distributed.impl.structural.OStructuralDistributedMember;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

public class OStructuralMaster {
  private final ExecutorService                                  executor;
  private final OOperationLog                                    operationLog;
  private final ConcurrentMap<OLogId, ORaftRequestContext>       contexts = new ConcurrentHashMap<>();
  private final Map<ONodeIdentity, OStructuralDistributedMember> members  = new ConcurrentHashMap<>();
  private final Timer                                            timer;
  private final OrientDBDistributed                              context;
  private       int                                              quorum;
  private       int                                              timeout;

  public OStructuralMaster(ExecutorService executor, OOperationLog operationLog, OrientDBDistributed context, int quorum) {
    this.executor = executor;
    this.operationLog = operationLog;
    this.timer = new Timer(true);
    this.context = context;
    this.quorum = quorum;
    this.timeout = 0;
  }

  public void propagateAndApply(ORaftOperation operation) {
    executor.execute(() -> {
      OLogId id = operationLog.log(operation);
      contexts.put(id, new ORaftRequestContext(operation, quorum));
      timer.schedule(new ORaftOperationTimeoutTimerTask(this, id), timeout, timeout);
      for (OStructuralDistributedMember value : members.values()) {
        value.propagate(id, operation);
      }
    });
  }

  public void receiveAck(ONodeIdentity node, OLogId id) {
    executor.execute(() -> {
      ORaftRequestContext ctx = contexts.get(id);
      if (ctx != null && ctx.ack(node, this)) {
        for (OStructuralDistributedMember value : members.values()) {
          value.confirm(id);
        }
        contexts.remove(id);
      }
    });
  }

  public void operationTimeout(OLogId id, TimerTask tt) {
    executor.execute(() -> {
      ORaftRequestContext ctx = contexts.get(id);
      if (ctx != null) {
        if (ctx.timeout()) {
          contexts.remove(id);
          //TODO: if an operation timedout, is should stop everything following raft.
          tt.cancel();
        }
      } else {
        tt.cancel();
      }
    });
  }

  public OrientDBDistributed getContext() {
    return context;
  }

}