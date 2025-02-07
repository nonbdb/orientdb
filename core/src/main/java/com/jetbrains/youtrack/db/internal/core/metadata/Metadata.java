/*
 *
 *
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
package com.jetbrains.youtrack.db.internal.core.metadata;

import com.jetbrains.youtrack.db.internal.core.metadata.function.FunctionLibrary;
import com.jetbrains.youtrack.db.api.schema.Schema;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.SchemaInternal;
import com.jetbrains.youtrack.db.internal.core.metadata.security.Security;
import com.jetbrains.youtrack.db.internal.core.metadata.sequence.SequenceLibrary;
import com.jetbrains.youtrack.db.internal.core.schedule.Scheduler;

/**
 *
 */
public interface Metadata {

  SchemaInternal getSchema();

  Security getSecurity();

  FunctionLibrary getFunctionLibrary();

  SequenceLibrary getSequenceLibrary();

  Scheduler getScheduler();
}
