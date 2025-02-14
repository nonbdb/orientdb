/*
 * Copyright 2010-2012 henryzhao81-at-gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jetbrains.youtrack.db.internal.core.schedule;

import com.jetbrains.youtrack.db.api.DatabaseSession;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.record.ProxedResource;
import java.util.Map;

/**
 * Proxy implementation of the Scheduler. <<<<<<< HEAD
 *
 * @since Mar 28, 2013
 */
public class SchedulerProxy extends ProxedResource<SchedulerImpl> implements Scheduler {

  public SchedulerProxy(final SchedulerImpl iDelegate,
      final DatabaseSessionInternal iDatabase) {
    super(iDelegate, iDatabase);
  }

  @Override
  public void scheduleEvent(DatabaseSession session, final ScheduledEvent scheduler) {
    delegate.scheduleEvent(session, scheduler);
  }

  @Override
  public void removeEvent(DatabaseSession session, final String eventName) {
    delegate.removeEvent((DatabaseSessionInternal) session, eventName);
  }

  @Override
  public void updateEvent(DatabaseSession session, final ScheduledEvent event) {
    delegate.updateEvent((DatabaseSessionInternal) session, event);
  }

  @Override
  public Map<String, ScheduledEvent> getEvents() {
    return delegate.getEvents();
  }

  @Override
  public ScheduledEvent getEvent(final String name) {
    return delegate.getEvent(name);
  }

  @Override
  public void load() {
    delegate.load(database);
  }

  @Override
  public void close() {
    // DO NOTHING THE DELEGATE CLOSE IS MANAGED IN A DIFFERENT CONTEXT
  }

  @Override
  public void create() {
    SchedulerImpl.create(database);
  }
}
