/*
 *
 *  *  Copyright 2010-2016 OrientDB LTD (http://orientdb.com)
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
 *  * For more information: http://orientdb.com
 *
 */
package com.orientechnologies.orient.core.index;

import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.serialization.serializer.stream.OMixedIndexRIDContainerSerializer;
import com.orientechnologies.orient.core.serialization.serializer.stream.OStreamSerializerRID;
import com.orientechnologies.orient.core.serialization.serializer.stream.OStreamSerializerSBTreeIndexRIDContainer;
import com.orientechnologies.orient.core.sharding.auto.OAutoShardingIndexFactory;
import com.orientechnologies.orient.core.storage.index.hashindex.local.OHashIndexFactory;
import java.util.Set;
import javax.annotation.Nonnull;

/**
 * Contains the index metadata.
 *
 * @author Luca Garulli (l.garulli--(at)--orientdb.com)
 */
public class OIndexMetadata {

  @Nonnull private final String name;
  private final OIndexDefinition indexDefinition;
  private final Set<String> clustersToIndex;
  private final String type;
  private final String algorithm;
  private final String valueContainerAlgorithm;
  private int version;
  private ODocument metadata;

  public OIndexMetadata(
      @Nonnull String name,
      OIndexDefinition indexDefinition,
      Set<String> clustersToIndex,
      String type,
      String algorithm,
      String valueContainerAlgorithm,
      int version,
      ODocument metadata) {
    this.name = name;
    this.indexDefinition = indexDefinition;
    this.clustersToIndex = clustersToIndex;
    this.type = type;
    if (type.equalsIgnoreCase(OClass.INDEX_TYPE.UNIQUE_HASH_INDEX.name())
        || type.equalsIgnoreCase(OClass.INDEX_TYPE.NOTUNIQUE_HASH_INDEX.name())
        || type.equalsIgnoreCase(OClass.INDEX_TYPE.DICTIONARY_HASH_INDEX.name())) {
      if (!algorithm.equalsIgnoreCase("autosharding")) {
        algorithm = OHashIndexFactory.HASH_INDEX_ALGORITHM;
      }
    }
    this.algorithm = algorithm;
    if (valueContainerAlgorithm != null) {
      this.valueContainerAlgorithm = valueContainerAlgorithm;
    } else {
      this.valueContainerAlgorithm = OAutoShardingIndexFactory.NONE_VALUE_CONTAINER;
    }
    this.version = version;
    this.metadata = metadata;
  }

  @Nonnull
  public String getName() {
    return name;
  }

  public OIndexDefinition getIndexDefinition() {
    return indexDefinition;
  }

  public Set<String> getClustersToIndex() {
    return clustersToIndex;
  }

  public String getType() {
    return type;
  }

  public String getAlgorithm() {
    return algorithm;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final OIndexMetadata that = (OIndexMetadata) o;

    if (algorithm != null ? !algorithm.equals(that.algorithm) : that.algorithm != null) {
      return false;
    }
    if (!clustersToIndex.equals(that.clustersToIndex)) {
      return false;
    }
    if (indexDefinition != null
        ? !indexDefinition.equals(that.indexDefinition)
        : that.indexDefinition != null) {
      return false;
    }
    if (!name.equals(that.name)) {
      return false;
    }
    if (!type.equals(that.type)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = name.hashCode();
    result = 31 * result + (indexDefinition != null ? indexDefinition.hashCode() : 0);
    result = 31 * result + clustersToIndex.hashCode();
    result = 31 * result + type.hashCode();
    result = 31 * result + (algorithm != null ? algorithm.hashCode() : 0);
    return result;
  }

  public String getValueContainerAlgorithm() {
    return valueContainerAlgorithm;
  }

  public boolean isMultivalue() {
    String t = type.toUpperCase();
    if (OClass.INDEX_TYPE.NOTUNIQUE_HASH_INDEX.toString().equals(t)
        || OClass.INDEX_TYPE.NOTUNIQUE.toString().equals(t)
        || OClass.INDEX_TYPE.FULLTEXT.toString().equals(t)) {
      return true;
    } else {
      return false;
    }
  }

  public byte getValueSerializerId(int binaryFormatVersion) {
    String t = type.toUpperCase();
    if (OClass.INDEX_TYPE.NOTUNIQUE_HASH_INDEX.toString().equals(t)
        || OClass.INDEX_TYPE.NOTUNIQUE.toString().equals(t)
        || OClass.INDEX_TYPE.FULLTEXT.toString().equals(t)
        || OClass.INDEX_TYPE.SPATIAL.toString().equals(t)) {
      // TODO: Hard Coded Lucene maybe fix
      if (binaryFormatVersion >= 13 && !"LUCENE".equalsIgnoreCase(algorithm)) {
        return OMixedIndexRIDContainerSerializer.ID;
      }

      return OStreamSerializerSBTreeIndexRIDContainer.ID;
    } else {
      return OStreamSerializerRID.INSTANCE.getId();
    }
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  public ODocument getMetadata() {
    return metadata;
  }

  public void setMetadata(ODocument metadata) {
    this.metadata = metadata;
  }
}
