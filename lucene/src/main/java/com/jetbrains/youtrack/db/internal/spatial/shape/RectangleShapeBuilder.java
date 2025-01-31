/**
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * <p>*
 */
package com.jetbrains.youtrack.db.internal.spatial.shape;

import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.api.schema.Schema;
import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import com.jetbrains.youtrack.db.api.schema.SchemaProperty;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import java.util.ArrayList;
import java.util.List;
import org.locationtech.spatial4j.shape.Point;
import org.locationtech.spatial4j.shape.Rectangle;

public class RectangleShapeBuilder extends ShapeBuilder<Rectangle> {

  public static final String NAME = "ORectangle";

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public OShapeType getType() {
    return OShapeType.RECTANGLE;
  }

  @Override
  public void initClazz(DatabaseSessionInternal db) {

    Schema schema = db.getMetadata().getSchema();
    SchemaClass rectangle = schema.createAbstractClass(NAME, superClass(db));
    SchemaProperty coordinates = rectangle.createProperty(db, COORDINATES,
        PropertyType.EMBEDDEDLIST,
        PropertyType.DOUBLE);
    coordinates.setMin(db, "4");
    coordinates.setMin(db, "4");
  }

  @Override
  public Rectangle fromDoc(EntityImpl document) {
    validate(document);
    List<Number> coordinates = document.field(COORDINATES);

    Point topLeft =
        SPATIAL_CONTEXT.makePoint(
            coordinates.get(0).doubleValue(), coordinates.get(1).doubleValue());
    Point bottomRight =
        SPATIAL_CONTEXT.makePoint(
            coordinates.get(2).doubleValue(), coordinates.get(3).doubleValue());
    Rectangle rectangle = SPATIAL_CONTEXT.makeRectangle(topLeft, bottomRight);
    return rectangle;
  }

  @Override
  public EntityImpl toEntitty(final Rectangle shape) {
    EntityImpl doc = new EntityImpl(null, NAME);
    doc.field(
        COORDINATES,
        new ArrayList<Double>() {
          {
            add(shape.getMinX());
            add(shape.getMinY());
            add(shape.getMaxX());
            add(shape.getMaxY());
          }
        });

    return doc;
  }
}
