/*
 *
 *
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.jetbrains.youtrack.db.internal.lucene.tests;

import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import com.jetbrains.youtrack.db.api.schema.Schema;
import com.jetbrains.youtrack.db.api.record.Entity;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 */
public class LuceneFacetTest extends LuceneBaseTest {

  @Before
  public void init() {
    Schema schema = db.getMetadata().getSchema();
    SchemaClass oClass = schema.createClass("Item");

    oClass.createProperty(db, "name", PropertyType.STRING);
    oClass.createProperty(db, "category", PropertyType.STRING);

    db.command(
            "create index Item.name_category on Item (name,category) FULLTEXT ENGINE LUCENE"
                + " METADATA { 'facetFields' : ['category']}")
        .close();

    EntityImpl doc = new EntityImpl("Item");
    doc.field("name", "Pioneer");
    doc.field("category", "Electronic/HiFi");

    db.save(doc);

    doc = new EntityImpl("Item");
    doc.field("name", "Hitachi");
    doc.field("category", "Electronic/HiFi");

    db.save(doc);

    doc = new EntityImpl("Item");
    doc.field("name", "Philips");
    doc.field("category", "Electronic/HiFi");

    db.save(doc);

    doc = new EntityImpl("Item");
    doc.field("name", "HP");
    doc.field("category", "Electronic/Computer");

    db.save(doc);

    db.commit();
  }

  @Test
  @Ignore
  public void baseFacetTest() {

    List<Entity> result =
        db.command("select *,$facet from Item where name lucene '(name:P*)' limit 1 ").stream()
            .map((o) -> o.toEntity())
            .collect(Collectors.toList());

    Assert.assertEquals(result.size(), 1);

    List<EntityImpl> facets = result.get(0).getProperty("$facet");

    Assert.assertEquals(facets.size(), 1);

    EntityImpl facet = facets.get(0);
    Assert.assertEquals(facet.<Object>field("childCount"), 1);
    Assert.assertEquals(facet.<Object>field("value"), 2);
    Assert.assertEquals(facet.field("dim"), "category");

    List<EntityImpl> labelsValues = facet.field("labelsValue");

    Assert.assertEquals(labelsValues.size(), 1);

    EntityImpl labelValues = labelsValues.get(0);

    Assert.assertEquals(labelValues.<Object>field("value"), 2);
    Assert.assertEquals(labelValues.field("label"), "Electronic");

    result =
        db
            .command(
                "select *,$facet from Item where name lucene { 'q' : 'H*', 'drillDown' :"
                    + " 'category:Electronic' }  limit 1 ")
            .stream()
            .map((o) -> o.toEntity())
            .collect(Collectors.toList());

    Assert.assertEquals(result.size(), 1);

    facets = result.get(0).getProperty("$facet");

    Assert.assertEquals(facets.size(), 1);

    facet = facets.get(0);

    Assert.assertEquals(facet.<Object>field("childCount"), 2);
    Assert.assertEquals(facet.<Object>field("value"), 2);
    Assert.assertEquals(facet.field("dim"), "category");

    labelsValues = facet.field("labelsValue");

    Assert.assertEquals(labelsValues.size(), 2);

    labelValues = labelsValues.get(0);

    Assert.assertEquals(labelValues.<Object>field("value"), 1);
    Assert.assertEquals(labelValues.field("label"), "HiFi");

    labelValues = labelsValues.get(1);

    Assert.assertEquals(labelValues.<Object>field("value"), 1);
    Assert.assertEquals(labelValues.field("label"), "Computer");
  }
}
