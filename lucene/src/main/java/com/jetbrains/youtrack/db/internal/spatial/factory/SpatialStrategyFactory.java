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
package com.jetbrains.youtrack.db.internal.spatial.factory;

import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import com.jetbrains.youtrack.db.api.schema.SchemaProperty;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.index.IndexDefinition;
import com.jetbrains.youtrack.db.internal.spatial.shape.ShapeBuilder;
import org.apache.lucene.spatial.SpatialStrategy;
import org.apache.lucene.spatial.bbox.BBoxStrategy;
import org.apache.lucene.spatial.prefix.RecursivePrefixTreeStrategy;
import org.apache.lucene.spatial.prefix.tree.GeohashPrefixTree;
import org.locationtech.spatial4j.context.SpatialContext;

/**
 *
 */
public class SpatialStrategyFactory {

  private final ShapeBuilder factory;

  public SpatialStrategyFactory(ShapeBuilder factory) {
    this.factory = factory;
  }

  public static SpatialStrategy createStrategy(
      SpatialContext ctx,
      DatabaseSessionInternal db,
      IndexDefinition indexDefinition) {

    SchemaClass aClass =
        db.getMetadata().getImmutableSchemaSnapshot().getClass(indexDefinition.getClassName());

    SchemaProperty property = aClass.getProperty(indexDefinition.getFields().get(0));

    SchemaClass linkedClass = property.getLinkedClass();

    if ("OPoint".equalsIgnoreCase(linkedClass.getName())) {
      RecursivePrefixTreeStrategy strategy =
          new RecursivePrefixTreeStrategy(new GeohashPrefixTree(ctx, 11), "location");
      strategy.setDistErrPct(0);
      return strategy;
    }
    return BBoxStrategy.newInstance(ctx, "location");
  }
}
