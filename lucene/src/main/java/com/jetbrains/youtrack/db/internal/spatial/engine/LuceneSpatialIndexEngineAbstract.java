/*
 *
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 * <p>
 * *
 */
package com.jetbrains.youtrack.db.internal.spatial.engine;

import com.jetbrains.youtrack.db.internal.common.log.LogManager;
import com.jetbrains.youtrack.db.internal.common.util.RawPair;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.internal.core.index.IndexDefinition;
import com.jetbrains.youtrack.db.internal.core.index.IndexMetadata;
import com.jetbrains.youtrack.db.internal.core.index.engine.IndexEngineValuesTransformer;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.storage.Storage;
import com.jetbrains.youtrack.db.internal.core.storage.impl.local.paginated.atomicoperations.AtomicOperation;
import com.jetbrains.youtrack.db.internal.lucene.builder.LuceneIndexType;
import com.jetbrains.youtrack.db.internal.lucene.engine.LuceneIndexEngineAbstract;
import com.jetbrains.youtrack.db.internal.lucene.engine.LuceneIndexWriterFactory;
import com.jetbrains.youtrack.db.internal.spatial.factory.SpatialStrategyFactory;
import com.jetbrains.youtrack.db.internal.spatial.shape.ShapeBuilder;
import com.jetbrains.youtrack.db.internal.spatial.strategy.SpatialQueryBuilder;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.Query;
import org.apache.lucene.spatial.SpatialStrategy;
import org.apache.lucene.store.Directory;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.shape.Shape;

/**
 *
 */
public abstract class LuceneSpatialIndexEngineAbstract extends LuceneIndexEngineAbstract
    implements LuceneSpatialIndexContainer {

  protected final ShapeBuilder factory;
  protected SpatialContext ctx;
  protected SpatialStrategy strategy;

  protected SpatialStrategyFactory strategyFactory;
  protected SpatialQueryBuilder queryStrategy;

  public LuceneSpatialIndexEngineAbstract(
      Storage storage, String indexName, int id, ShapeBuilder factory) {
    super(id, storage, indexName);
    this.ctx = factory.context();
    this.factory = factory;
    strategyFactory = new SpatialStrategyFactory(factory);
    this.queryStrategy = new SpatialQueryBuilder(this, factory);
  }

  @Override
  public void init(DatabaseSessionInternal db, IndexMetadata im) {
    super.init(db, im);
    strategy = createSpatialStrategy(db, im.getIndexDefinition(), im.getMetadata());
  }

  protected abstract SpatialStrategy createSpatialStrategy(
      DatabaseSessionInternal db, IndexDefinition indexDefinition, Map<String, ?> metadata);

  @Override
  public IndexWriter createIndexWriter(Directory directory) throws IOException {
    var fc = new LuceneIndexWriterFactory();

    LogManager.instance().debug(this, "Creating Lucene index in '%s'...", directory);

    return fc.createIndexWriter(directory, metadata, indexAnalyzer());
  }

  @Override
  public boolean remove(Storage storage, AtomicOperation atomicOperation, Object key) {
    return false;
  }

  @Override
  public Stream<RawPair<Object, com.jetbrains.youtrack.db.api.record.RID>> iterateEntriesBetween(
      DatabaseSessionInternal db, Object rangeFrom,
      boolean fromInclusive,
      Object rangeTo,
      boolean toInclusive,
      boolean ascSortOrder,
      IndexEngineValuesTransformer transformer) {
    return null;
  }

  @Override
  public Stream<RawPair<Object, com.jetbrains.youtrack.db.api.record.RID>> iterateEntriesMajor(
      Object fromKey,
      boolean isInclusive,
      boolean ascSortOrder,
      IndexEngineValuesTransformer transformer) {
    return null;
  }

  @Override
  public Stream<RawPair<Object, com.jetbrains.youtrack.db.api.record.RID>> iterateEntriesMinor(
      Object toKey,
      boolean isInclusive,
      boolean ascSortOrder,
      IndexEngineValuesTransformer transformer) {
    return null;
  }

  @Override
  public Stream<RawPair<Object, com.jetbrains.youtrack.db.api.record.RID>> stream(
      IndexEngineValuesTransformer valuesTransformer) {
    return null;
  }

  @Override
  public Stream<Object> keyStream() {
    return null;
  }

  @Override
  public boolean hasRangeQuerySupport() {
    return false;
  }

  protected Document newGeoDocument(Identifiable oIdentifiable, Shape shape,
      EntityImpl shapeDoc) {

    var ft = new FieldType();
    ft.setIndexOptions(IndexOptions.DOCS);
    ft.setStored(true);

    var doc = new Document();
    doc.add(LuceneIndexType.createOldIdField(oIdentifiable));
    doc.add(LuceneIndexType.createIdField(oIdentifiable, shapeDoc));

    for (IndexableField f : strategy.createIndexableFields(shape)) {
      doc.add(f);
    }

    //noinspection deprecation
    doc.add(new StoredField(strategy.getFieldName(), ctx.toString(shape)));
    doc.add(
        new StringField(
            strategy.getFieldName() + "__orient_key_hash",
            LuceneIndexType.hashKey(shapeDoc),
            Field.Store.YES));
    return doc;
  }

  @Override
  public Document buildDocument(DatabaseSessionInternal db, Object key,
      Identifiable value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Query buildQuery(Object query) {
    throw new UnsupportedOperationException();
  }

  @Override
  public SpatialStrategy strategy() {
    return strategy;
  }
}
