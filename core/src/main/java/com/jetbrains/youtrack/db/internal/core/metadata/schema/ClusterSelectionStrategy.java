/*
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.youtrack.db.internal.core.metadata.schema;

import com.jetbrains.youtrack.db.api.DatabaseSession;
import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;

/**
 * Strategy to select the cluster to use. Instances are stateful, so can't be reused on multiple
 * classes.
 */
public interface ClusterSelectionStrategy {

  int getCluster(DatabaseSession session, final SchemaClass iClass, final EntityImpl entity);

  int getCluster(DatabaseSession session, final SchemaClass iClass, int[] selection,
      final EntityImpl entity);

  String getName();
}
