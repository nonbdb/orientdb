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
package com.orientechnologies.orient.core.metadata.schema;

import com.orientechnologies.orient.core.collate.OCollate;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.record.impl.ODocument;
import java.util.Collection;
import java.util.Set;

/**
 * Contains the description of a persistent class property.
 *
 * @author Luca Garulli (l.garulli--(at)--orientdb.com)
 */
public interface OProperty extends Comparable<OProperty> {

  enum ATTRIBUTES {
    LINKEDTYPE,
    LINKEDCLASS,
    MIN,
    MAX,
    MANDATORY,
    NAME,
    NOTNULL,
    REGEXP,
    TYPE,
    CUSTOM,
    READONLY,
    COLLATE,
    DEFAULT,
    DESCRIPTION
  }

  String getName();

  /**
   * Returns the full name as <class>.<property>
   */
  String getFullName();

  OProperty setName(String iName);

  void set(ATTRIBUTES attribute, Object iValue);

  OType getType();

  /**
   * Returns the linked class in lazy mode because while unmarshalling the class could be not loaded
   * yet.
   *
   * @return
   */
  OClass getLinkedClass();

  OProperty setLinkedClass(OClass oClass);

  OType getLinkedType();

  OProperty setLinkedType(OType type);

  boolean isNotNull();

  OProperty setNotNull(boolean iNotNull);

  OCollate getCollate();

  OProperty setCollate(String iCollateName);

  OProperty setCollate(OCollate collate);

  boolean isMandatory();

  OProperty setMandatory(boolean mandatory);

  boolean isReadonly();

  OProperty setReadonly(boolean iReadonly);

  /**
   * Min behavior depends on the Property OType.
   *
   * <p>
   *
   * <ul>
   *   <li>String : minimum length
   *   <li>Number : minimum value
   *   <li>date and time : minimum time in millisecond, date must be written in the storage date
   *       format
   *   <li>binary : minimum size of the byte array
   *   <li>List,Set,Collection : minimum size of the collection
   * </ul>
   *
   * @return String, can be null
   */
  String getMin();

  /**
   * @param min can be null
   * @return this property
   * @see OProperty#getMin()
   */
  OProperty setMin(String min);

  /**
   * Max behavior depends on the Property OType.
   *
   * <p>
   *
   * <ul>
   *   <li>String : maximum length
   *   <li>Number : maximum value
   *   <li>date and time : maximum time in millisecond, date must be written in the storage date
   *       format
   *   <li>binary : maximum size of the byte array
   *   <li>List,Set,Collection : maximum size of the collection
   * </ul>
   *
   * @return String, can be null
   */
  String getMax();

  /**
   * @param max can be null
   * @return this property
   * @see OProperty#getMax()
   */
  OProperty setMax(String max);

  /**
   * Default value for the property; can be function
   *
   * @return String, can be null
   */
  String getDefaultValue();

  /**
   * @param defaultValue can be null
   * @return this property
   * @see OProperty#getDefaultValue()
   */
  OProperty setDefaultValue(String defaultValue);

  /**
   * Creates an index on this property. Indexes speed up queries but slow down insert and update
   * operations. For massive inserts we suggest to remove the index, make the massive insert and
   * recreate it.
   *
   * @param iType One of types supported.
   *              <ul>
   *                <li>UNIQUE: Doesn't allow duplicates
   *                <li>NOTUNIQUE: Allow duplicates
   *                <li>FULLTEXT: Indexes single word for full text search
   *              </ul>
   * @return see {@link OClass#createIndex(String, OClass.INDEX_TYPE, String...)}.
   */
  OIndex createIndex(final OClass.INDEX_TYPE iType);

  /**
   * Creates an index on this property. Indexes speed up queries but slow down insert and update
   * operations. For massive inserts we suggest to remove the index, make the massive insert and
   * recreate it.
   *
   * @param iType
   * @return see {@link OClass#createIndex(String, OClass.INDEX_TYPE, String...)}.
   */
  OIndex createIndex(final String iType);

  /**
   * Creates an index on this property. Indexes speed up queries but slow down insert and update
   * operations. For massive inserts we suggest to remove the index, make the massive insert and
   * recreate it.
   *
   * @param iType    One of types supported.
   *                 <ul>
   *                   <li>UNIQUE: Doesn't allow duplicates
   *                   <li>NOTUNIQUE: Allow duplicates
   *                   <li>FULLTEXT: Indexes single word for full text search
   *                 </ul>
   * @param metadata the index metadata
   * @return see {@link OClass#createIndex(String, OClass.INDEX_TYPE, String...)}.
   */
  OIndex createIndex(String iType, ODocument metadata);

  /**
   * Creates an index on this property. Indexes speed up queries but slow down insert and update
   * operations. For massive inserts we suggest to remove the index, make the massive insert and
   * recreate it.
   *
   * @param iType    One of types supported.
   *                 <ul>
   *                   <li>UNIQUE: Doesn't allow duplicates
   *                   <li>NOTUNIQUE: Allow duplicates
   *                   <li>FULLTEXT: Indexes single word for full text search
   *                 </ul>
   * @param metadata the index metadata
   * @return see {@link OClass#createIndex(String, OClass.INDEX_TYPE, String...)}.
   */
  OIndex createIndex(OClass.INDEX_TYPE iType, ODocument metadata);

  /**
   * Remove the index on property
   *
   * @return
   * @deprecated Use SQL command instead.
   */
  @Deprecated
  OProperty dropIndexes();

  /**
   * @return All indexes in which this property participates as first key item.
   * @deprecated Use {@link OClass#getInvolvedIndexes(String...)} instead.
   */
  @Deprecated
  Set<OIndex> getIndexes();

  /**
   * @return The first index in which this property participates as first key item.
   * @deprecated Use {@link OClass#getInvolvedIndexes(String...)} instead.
   */
  @Deprecated
  OIndex getIndex();

  /**
   * @return All indexes in which this property participates.
   */
  Collection<OIndex> getAllIndexes();

  /**
   * Indicates whether property is contained in indexes as its first key item. If you would like to
   * fetch all indexes or check property presence in other indexes use {@link #getAllIndexes()}
   * instead.
   *
   * @return <code>true</code> if and only if this property is contained in indexes as its first key
   * item.
   * @deprecated Use {@link OClass#areIndexed(String...)} instead.
   */
  @Deprecated
  boolean isIndexed();

  String getRegexp();

  OProperty setRegexp(String regexp);

  /**
   * Change the type. It checks for compatibility between the change of type.
   *
   * @param iType
   */
  OProperty setType(final OType iType);

  String getCustom(final String iName);

  OProperty setCustom(final String iName, final String iValue);

  void removeCustom(final String iName);

  void clearCustom();

  Set<String> getCustomKeys();

  OClass getOwnerClass();

  Object get(ATTRIBUTES iAttribute);

  Integer getId();

  String getDescription();

  OProperty setDescription(String iDescription);
}
