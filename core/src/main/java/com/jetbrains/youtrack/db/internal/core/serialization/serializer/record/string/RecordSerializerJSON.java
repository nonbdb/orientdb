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
package com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.string;

import com.jetbrains.youtrack.db.api.record.DBRecord;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.record.RecordAbstract;
import java.io.StringWriter;

public class RecordSerializerJSON extends RecordSerializerStringAbstract {

  public static final String NAME = "json";
  public static final char[] PARAMETER_SEPARATOR = new char[]{':', ','};

  @Override
  public <T extends DBRecord> T fromString(DatabaseSessionInternal db, String iContent,
      RecordAbstract iRecord, String[] iFields) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected StringWriter toString(DatabaseSessionInternal db, DBRecord iRecord,
      StringWriter iOutput, String iFormat, boolean autoDetectCollectionType) {
    throw new UnsupportedOperationException();
  }

  public static class FormatSettings {

    public boolean includeVer;
    public boolean includeType;
    public boolean includeId;
    public boolean includeClazz;
    public boolean attribSameRow;
    public boolean alwaysFetchEmbeddedDocuments;
    public int indentLevel;
    public String fetchPlan = null;
    public boolean keepTypes = true;
    public boolean dateAsLong = false;
    public boolean prettyPrint = false;

    public FormatSettings(final String stringFormat) {
      if (stringFormat == null) {
        includeType = true;
        includeVer = true;
        includeId = true;
        includeClazz = true;
        attribSameRow = true;
        indentLevel = 0;
        fetchPlan = "";
        alwaysFetchEmbeddedDocuments = true;
      } else {
        includeType = false;
        includeVer = false;
        includeId = false;
        includeClazz = false;
        attribSameRow = false;
        alwaysFetchEmbeddedDocuments = false;
        indentLevel = 0;
        keepTypes = false;

        if (!stringFormat.isEmpty()) {
          final String[] format = stringFormat.split(",");
          for (String f : format) {
            if (f.equals("type")) {
              includeType = true;
            } else {
              if (f.equals("rid")) {
                includeId = true;
              } else {
                if (f.equals("version")) {
                  includeVer = true;
                } else {
                  if (f.equals("class")) {
                    includeClazz = true;
                  } else {
                    if (f.equals("attribSameRow")) {
                      attribSameRow = true;
                    } else {
                      if (f.startsWith("indent")) {
                        indentLevel = Integer.parseInt(f.substring(f.indexOf(':') + 1));
                      } else {
                        if (f.startsWith("fetchPlan")) {
                          fetchPlan = f.substring(f.indexOf(':') + 1);
                        } else {
                          if (f.startsWith("keepTypes")) {
                            keepTypes = true;
                          } else {
                            if (f.startsWith("alwaysFetchEmbedded")) {
                              alwaysFetchEmbeddedDocuments = true;
                            } else {
                              if (f.startsWith("dateAsLong")) {
                                dateAsLong = true;
                              } else {
                                if (f.startsWith("prettyPrint")) {
                                  prettyPrint = true;
                                } else {
                                  if (f.startsWith("graph") || f.startsWith("shallow"))
                                    // SUPPORTED IN OTHER PARTS
                                    ;
                                  else {
                                    throw new IllegalArgumentException(
                                        "Unrecognized JSON formatting option: " + f);
                                  }
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  @Override
  public int getCurrentVersion() {
    return 0;
  }

  @Override
  public int getMinSupportedVersion() {
    return 0;
  }


  @Override
  public String toString() {
    return NAME;
  }


  @Override
  public String getName() {
    return NAME;
  }
}
