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

package com.jetbrains.youtrack.db.internal.core.serialization;

import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;

/**
 * Interface for objects which are hold inside of entity as field values and can serialize
 * yourself into entity. In such way it is possible to serialize complex types and do not break
 * compatibility with non-Java binary drivers.
 *
 * <p>After serialization into entity additional field {@link #CLASS_NAME} will be added. This
 * field contains value of class of original object.
 *
 * <p>During deserialization of embedded object if embedded entity contains {@link #CLASS_NAME}
 * field we try to find class with given name and only if this class implements
 * {@link DocumentSerializable} interface it will be converted to the object. So it is pretty safe
 * to use field with {@link #CLASS_NAME} in ordinary documents if it is needed.
 *
 * <p>Class which implements this interface should have public no-arguments constructor.
 *
 * @since 3/27/14
 */
public interface DocumentSerializable {

  String CLASS_NAME = "__youtrackdb_serilized_class__ ";

  EntityImpl toDocument();

  void fromDocument(EntityImpl entity);
}
