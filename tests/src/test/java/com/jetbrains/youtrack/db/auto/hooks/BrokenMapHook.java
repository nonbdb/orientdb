package com.jetbrains.youtrack.db.auto.hooks;

import com.jetbrains.youtrack.db.api.exception.RecordNotFoundException;
import com.jetbrains.youtrack.db.api.record.DBRecord;
import com.jetbrains.youtrack.db.api.record.Entity;
import com.jetbrains.youtrack.db.api.record.RecordHook;
import com.jetbrains.youtrack.db.api.record.RecordHookAbstract;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class BrokenMapHook extends RecordHookAbstract implements RecordHook {

  public BrokenMapHook() {
  }

  @Override
  public DISTRIBUTED_EXECUTION_MODE getDistributedExecutionMode() {
    return DISTRIBUTED_EXECUTION_MODE.BOTH;
  }

  public RESULT onRecordBeforeCreate(DBRecord record) {
    var now = new Date();
    var element = (Entity) record;

    if (element.getProperty("myMap") != null) {
      var myMap = new HashMap<String, Object>(element.getProperty("myMap"));

      var newDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(now);

      myMap.replaceAll((k, v) -> newDate);

      element.setProperty("myMap", myMap);
    }

    return RESULT.RECORD_CHANGED;
  }

  public RESULT onRecordBeforeUpdate(DBRecord newRecord) {
    var newElement = (Entity) newRecord;
    try {
      var session = newElement.getBoundedToSession();
      Entity oldElement = session.load(newElement.getIdentity());

      var newPropertyNames = newElement.getPropertyNames();
      var oldPropertyNames = oldElement.getPropertyNames();

      if (newPropertyNames.contains("myMap") && oldPropertyNames.contains("myMap")) {
        HashMap<String, Object> newFieldValue = newElement.getProperty("myMap");
        var oldFieldValue = new HashMap<String, Object>(oldElement.getProperty("myMap"));

        var newDate =
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        Set<String> newKeys = new HashSet<>(newFieldValue.keySet());

        newKeys.forEach(
            k -> {
              newFieldValue.remove(k);
              newFieldValue.put(k, newDate);
            });

        oldFieldValue.forEach(
            (k, v) -> {
              if (!newFieldValue.containsKey(k)) {
                newFieldValue.put(k, v);
              }
            });
      }
      return RESULT.RECORD_CHANGED;
    } catch (RecordNotFoundException e) {
      return RESULT.RECORD_NOT_CHANGED;
    }
  }
}
