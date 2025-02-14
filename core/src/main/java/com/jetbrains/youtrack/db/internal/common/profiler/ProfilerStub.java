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

package com.jetbrains.youtrack.db.internal.common.profiler;

import static com.jetbrains.youtrack.db.api.config.GlobalConfiguration.PROFILER_MAXVALUES;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ProfilerStub extends AbstractProfiler {

  protected ConcurrentMap<String, Long> counters;
  private ConcurrentLinkedHashMap<String, AtomicInteger> tips;
  private ConcurrentLinkedHashMap<String, Long> tipsTimestamp;

  public ProfilerStub() {
  }

  public ProfilerStub(boolean registerListener) {
    super(registerListener);
  }

  public ProfilerStub(final AbstractProfiler profiler) {
    super(profiler);
  }

  @Override
  public void startup() {
    counters =
        new ConcurrentLinkedHashMap.Builder()
            .maximumWeightedCapacity(PROFILER_MAXVALUES.getValueAsInteger())
            .build();
    tips =
        new ConcurrentLinkedHashMap.Builder()
            .maximumWeightedCapacity(PROFILER_MAXVALUES.getValueAsInteger())
            .build();
    tipsTimestamp =
        new ConcurrentLinkedHashMap.Builder()
            .maximumWeightedCapacity(PROFILER_MAXVALUES.getValueAsInteger())
            .build();
    super.startup();
  }

  @Override
  public void shutdown() {

    if (counters != null) {
      counters.clear();
    }
    if (tips != null) {
      tips.clear();
    }
    if (tipsTimestamp != null) {
      tipsTimestamp.clear();
    }
    super.shutdown();
  }

  @Override
  protected void setTip(final String iMessage, final AtomicInteger counter) {
    if (!isRecording()) {
      // profiler is not started
      return;
    }

    tips.put(iMessage, counter);
    tipsTimestamp.put(iMessage, System.currentTimeMillis());
  }

  @Override
  protected AtomicInteger getTip(final String iMessage) {
    if (!isRecording()) {
      // profiler is not started.
      return null;
    }

    if (iMessage == null) {
      return null;
    }

    return tips.get(iMessage);
  }

  @Override
  public boolean isEnterpriseEdition() {
    return false;
  }

  public void configure(final String iConfiguration) {
    if (iConfiguration == null || iConfiguration.length() == 0) {
      return;
    }

    if (isRecording()) {
      stopRecording();
    }

    startRecording();
  }

  public boolean startRecording() {
    counters =
        new ConcurrentLinkedHashMap.Builder()
            .maximumWeightedCapacity(PROFILER_MAXVALUES.getValueAsInteger())
            .build();
    tips =
        new ConcurrentLinkedHashMap.Builder()
            .maximumWeightedCapacity(PROFILER_MAXVALUES.getValueAsInteger())
            .build();
    tipsTimestamp =
        new ConcurrentLinkedHashMap.Builder()
            .maximumWeightedCapacity(PROFILER_MAXVALUES.getValueAsInteger())
            .build();

    if (super.startRecording()) {
      counters.clear();
      return true;
    }
    return false;
  }

  public boolean stopRecording() {
    if (super.stopRecording()) {
      counters.clear();
      return true;
    }
    return false;
  }

  @Override
  public String dump() {
    if (recordingFrom < 0) {
      return "<no recording>";
    }

    final StringBuilder buffer = new StringBuilder(super.dump());

    if (tips.size() == 0) {
      return "";
    }

    buffer.append("TIPS:");

    buffer.append(String.format("\n%100s +------------+", ""));
    buffer.append(String.format("\n%100s | Value      |", "Name"));
    buffer.append(String.format("\n%100s +------------+", ""));

    final List<String> names = new ArrayList<String>(tips.keySet());
    Collections.sort(names);

    for (String n : names) {
      final AtomicInteger v = tips.get(n);
      buffer.append(String.format("\n%-100s | %10d |", n, v.intValue()));
    }

    buffer.append(String.format("\n%100s +------------+", ""));
    return buffer.toString();
  }

  public void updateCounter(
      final String statName, final String description, final long plus, final String metadata) {
    if (statName == null || !isRecording()) {
      return;
    }

    Long oldValue;
    Long newValue;
    do {
      oldValue = counters.get(statName);

      if (oldValue == null) {
        counters.putIfAbsent(statName, 0L);
        oldValue = counters.get(statName);
      }

      newValue = oldValue + plus;
    } while (!counters.replace(statName, oldValue, newValue));
  }

  public long getCounter(final String statName) {
    if (statName == null || !isRecording()) {
      return -1;
    }

    final Long stat = counters.get(statName);
    if (stat == null) {
      return -1;
    }

    return stat;
  }

  @Override
  public String dumpCounters() {
    return null;
  }

  @Override
  public ProfilerEntry getChrono(String string) {
    return null;
  }

  @Override
  public long startChrono() {
    return 0;
  }

  @Override
  public long stopChrono(String iName, String iDescription, long iStartTime) {
    return 0;
  }

  @Override
  public long stopChrono(String iName, String iDescription, long iStartTime, String iDictionary) {
    return 0;
  }

  @Override
  public long stopChrono(
      String iName, String iDescription, long iStartTime, String iDictionary, String payload) {
    return 0;
  }

  @Override
  public long stopChrono(
      String iName,
      String iDescription,
      long iStartTime,
      String iDictionary,
      String payload,
      String user) {
    return 0;
  }

  @Override
  public String dumpChronos() {
    return null;
  }

  @Override
  public String[] getCountersAsString() {
    final List<String> keys = new ArrayList<String>(counters.keySet());
    final String[] result = new String[keys.size()];
    return keys.toArray(result);
  }

  @Override
  public List<String> getChronos() {
    return Collections.emptyList();
  }

  @Override
  public Date getLastReset() {
    return null;
  }

  @Override
  public String metadataToJSON() {
    return null;
  }

  @Override
  public Object getHookValue(final String iName) {
    return null;
  }

  @Override
  public String toJSON(String command, final String iPar1) {
    return null;
  }

  @Override
  public void resetRealtime(String iText) {
  }

  @Override
  public String getStatsAsJson() {
    return null;
  }

  /**
   * Updates the metric metadata.
   */
  protected void updateMetadata(
      final String iName, final String iDescription, final METRIC_TYPE iType) {
    if (iDescription != null && dictionary.putIfAbsent(iName, iDescription) == null) {
      types.put(iName, iType);
    }
  }
}
