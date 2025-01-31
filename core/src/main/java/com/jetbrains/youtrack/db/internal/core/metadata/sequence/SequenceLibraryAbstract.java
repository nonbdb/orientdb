/*
 * Copyright 2018 YouTrackDB.
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
package com.jetbrains.youtrack.db.internal.core.metadata.sequence;

import com.jetbrains.youtrack.db.api.exception.DatabaseException;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.db.record.ProxedResource;

/**
 *
 */
public abstract class SequenceLibraryAbstract extends ProxedResource<SequenceLibraryImpl>
    implements SequenceLibrary {

  public SequenceLibraryAbstract(
      final SequenceLibraryImpl iDelegate, final DatabaseSessionInternal iDatabase) {
    super(iDelegate, iDatabase);
  }

  abstract void dropSequence(String iName, boolean executeViaDistributed)
      throws DatabaseException;

  abstract DBSequence createSequence(
      String iName,
      DBSequence.SEQUENCE_TYPE sequenceType,
      DBSequence.CreateParams params,
      boolean executeViaDistributed)
      throws DatabaseException;
}
