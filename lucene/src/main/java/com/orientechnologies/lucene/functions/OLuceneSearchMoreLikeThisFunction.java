package com.orientechnologies.lucene.functions;

import com.orientechnologies.common.log.OLogManager;
import com.orientechnologies.lucene.collections.OLuceneCompositeKey;
import com.orientechnologies.lucene.exception.YTLuceneIndexException;
import com.orientechnologies.lucene.index.OLuceneFullTextIndex;
import com.orientechnologies.lucene.query.OLuceneKeyAndMetadata;
import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.YTDatabaseSession;
import com.orientechnologies.orient.core.db.record.YTIdentifiable;
import com.orientechnologies.orient.core.id.ChangeableRecordId;
import com.orientechnologies.orient.core.id.YTRID;
import com.orientechnologies.orient.core.id.YTRecordId;
import com.orientechnologies.orient.core.metadata.OMetadataInternal;
import com.orientechnologies.orient.core.record.YTEntity;
import com.orientechnologies.orient.core.record.YTRecord;
import com.orientechnologies.orient.core.record.YTRecordAbstract;
import com.orientechnologies.orient.core.record.impl.YTEntityImpl;
import com.orientechnologies.orient.core.sql.executor.YTResult;
import com.orientechnologies.orient.core.sql.functions.OIndexableSQLFunction;
import com.orientechnologies.orient.core.sql.functions.OSQLFunctionAbstract;
import com.orientechnologies.orient.core.sql.parser.OBinaryCompareOperator;
import com.orientechnologies.orient.core.sql.parser.OExpression;
import com.orientechnologies.orient.core.sql.parser.OFromClause;
import com.orientechnologies.orient.core.sql.parser.OFromItem;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.mlt.MoreLikeThis;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery.Builder;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

/**
 *
 */
public class OLuceneSearchMoreLikeThisFunction extends OSQLFunctionAbstract
    implements OIndexableSQLFunction {

  public static final String NAME = "search_more";

  public OLuceneSearchMoreLikeThisFunction() {
    super(OLuceneSearchMoreLikeThisFunction.NAME, 1, 2);
  }

  @Override
  public String getName(YTDatabaseSession session) {
    return OLuceneSearchMoreLikeThisFunction.NAME;
  }

  @Override
  public Object execute(
      Object iThis,
      YTIdentifiable iCurrentRecord,
      Object iCurrentResult,
      Object[] params,
      OCommandContext ctx) {

    throw new YTLuceneIndexException("SEARCH_MORE can't be executed by document");
  }

  @Override
  public String getSyntax(YTDatabaseSession session) {
    return "SEARCH_MORE( [rids], [ metdatada {} ] )";
  }

  @Override
  public Iterable<YTIdentifiable> searchFromTarget(
      OFromClause target,
      OBinaryCompareOperator operator,
      Object rightValue,
      OCommandContext ctx,
      OExpression... args) {

    OLuceneFullTextIndex index = this.searchForIndex(target, ctx);

    if (index == null) {
      return Collections.emptySet();
    }

    IndexSearcher searcher = index.searcher();

    OExpression expression = args[0];

    var metadata = parseMetadata(args);

    List<String> ridsAsString = parseRids(ctx, expression);

    List<YTRecord> others =
        ridsAsString.stream()
            .map(
                rid -> {
                  YTRecordId recordId = new ChangeableRecordId();

                  recordId.fromString(rid);
                  recordId = recordId.copy();
                  return recordId;
                })
            .<YTRecord>map(YTRecordId::getRecord)
            .toList();

    MoreLikeThis mlt = buildMoreLikeThis(index, searcher, metadata);

    Builder queryBuilder = new Builder();

    excludeOtherFromResults(ridsAsString, queryBuilder);

    addLikeQueries(others, mlt, queryBuilder);

    Query mltQuery = queryBuilder.build();

    Set<YTIdentifiable> luceneResultSet;
    try (Stream<YTRID> rids =
        index
            .getInternal()
            .getRids(ctx.getDatabase(),
                new OLuceneKeyAndMetadata(
                    new OLuceneCompositeKey(Collections.singletonList(mltQuery.toString()))
                        .setContext(ctx),
                    metadata))) {
      luceneResultSet = rids.collect(Collectors.toSet());
    }

    return luceneResultSet;
  }

  private List<String> parseRids(OCommandContext ctx, OExpression expression) {

    Object expResult = expression.execute((YTIdentifiable) null, ctx);

    // single rind
    if (expResult instanceof YTIdentifiable) {
      return Collections.singletonList(((YTIdentifiable) expResult).getIdentity().toString());
    }

    Iterator iter;
    if (expResult instanceof Iterable) {
      iter = ((Iterable) expResult).iterator();
    } else if (expResult instanceof Iterator) {
      iter = (Iterator) expResult;
    } else {
      return Collections.emptyList();
    }

    List<String> rids = new ArrayList<>();
    while (iter.hasNext()) {
      Object item = iter.next();
      if (item instanceof YTResult) {
        if (((YTResult) item).isEntity()) {
          rids.add(((YTResult) item).getIdentity().get().toString());
        } else {
          var properties = ((YTResult) item).getPropertyNames();
          if (properties.size() == 1) {
            Object val = ((YTResult) item).getProperty(properties.iterator().next());
            if (val instanceof YTIdentifiable) {
              rids.add(((YTIdentifiable) val).getIdentity().toString());
            }
          }
        }
      } else if (item instanceof YTIdentifiable) {
        rids.add(((YTIdentifiable) item).getIdentity().toString());
      }
    }
    return rids;
  }

  private static Map<String, ?> parseMetadata(OExpression[] args) {
    YTEntityImpl metadata = new YTEntityImpl();
    if (args.length == 2) {
      metadata.fromJSON(args[1].toString());
    }
    return metadata.toMap();
  }

  private MoreLikeThis buildMoreLikeThis(
      OLuceneFullTextIndex index, IndexSearcher searcher, Map<String, ?> metadata) {

    MoreLikeThis mlt = new MoreLikeThis(searcher.getIndexReader());

    mlt.setAnalyzer(index.queryAnalyzer());

    //noinspection unchecked
    mlt.setFieldNames(
        Optional.ofNullable((List<String>) metadata.get("fieldNames"))
            .orElse(index.getDefinition().getFields())
            .toArray(new String[]{}));

    mlt.setMaxQueryTerms(
        Optional.ofNullable((Integer) metadata.get("maxQueryTerms"))
            .orElse(MoreLikeThis.DEFAULT_MAX_QUERY_TERMS));

    mlt.setMinTermFreq(
        Optional.ofNullable((Integer) metadata.get("minTermFreq"))
            .orElse(MoreLikeThis.DEFAULT_MIN_TERM_FREQ));

    mlt.setMaxDocFreq(
        Optional.ofNullable((Integer) metadata.get("maxDocFreq"))
            .orElse(MoreLikeThis.DEFAULT_MAX_DOC_FREQ));

    mlt.setMinDocFreq(
        Optional.ofNullable((Integer) metadata.get("minDocFreq"))
            .orElse(MoreLikeThis.DEFAULT_MAX_DOC_FREQ));

    mlt.setBoost(
        Optional.ofNullable((Boolean) metadata.get("boost"))
            .orElse(MoreLikeThis.DEFAULT_BOOST));

    mlt.setBoostFactor(Optional.ofNullable((Float) metadata.get("boostFactor")).orElse(1f));

    mlt.setMaxWordLen(
        Optional.ofNullable((Integer) metadata.get("maxWordLen"))
            .orElse(MoreLikeThis.DEFAULT_MAX_WORD_LENGTH));

    mlt.setMinWordLen(
        Optional.ofNullable((Integer) metadata.get("minWordLen"))
            .orElse(MoreLikeThis.DEFAULT_MIN_WORD_LENGTH));

    mlt.setMaxNumTokensParsed(
        Optional.ofNullable((Integer) metadata.get("maxNumTokensParsed"))
            .orElse(MoreLikeThis.DEFAULT_MAX_NUM_TOKENS_PARSED));

    //noinspection rawtypes
    mlt.setStopWords(
        Optional.ofNullable((Set) metadata.get("stopWords"))
            .orElse(MoreLikeThis.DEFAULT_STOP_WORDS));

    return mlt;
  }

  private void addLikeQueries(List<YTRecord> others, MoreLikeThis mlt, Builder queryBuilder) {
    others.stream()
        .map(or -> ((YTRecordAbstract) or).getSession().<YTEntity>load(or.getIdentity()))
        .forEach(
            element ->
                Arrays.stream(mlt.getFieldNames())
                    .forEach(
                        fieldName -> {
                          String property = element.getProperty(fieldName);
                          try {
                            Query fieldQuery = mlt.like(fieldName, new StringReader(property));
                            if (!fieldQuery.toString().isEmpty()) {
                              queryBuilder.add(fieldQuery, Occur.SHOULD);
                            }
                          } catch (IOException e) {
                            // FIXME handle me!
                            OLogManager.instance()
                                .error(this, "Error during Lucene query generation", e);
                          }
                        }));
  }

  private void excludeOtherFromResults(List<String> ridsAsString, Builder queryBuilder) {
    ridsAsString.stream()
        .forEach(
            rid ->
                queryBuilder.add(
                    new TermQuery(new Term("RID", QueryParser.escape(rid))), Occur.MUST_NOT));
  }

  private OLuceneFullTextIndex searchForIndex(OFromClause target, OCommandContext ctx) {
    OFromItem item = target.getItem();

    String className = item.getIdentifier().getStringValue();

    return searchForIndex(ctx, className);
  }

  private OLuceneFullTextIndex searchForIndex(OCommandContext ctx, String className) {
    var db = ctx.getDatabase();
    db.activateOnCurrentThread();
    OMetadataInternal dbMetadata = db.getMetadata();

    List<OLuceneFullTextIndex> indices =
        dbMetadata.getImmutableSchemaSnapshot().getClass(className).getIndexes(db).stream()
            .filter(idx -> idx instanceof OLuceneFullTextIndex)
            .map(idx -> (OLuceneFullTextIndex) idx)
            .toList();

    if (indices.size() > 1) {
      throw new IllegalArgumentException("too many full-text indices on given class: " + className);
    }

    return indices.isEmpty() ? null : indices.get(0);
  }

  @Override
  public long estimate(
      OFromClause target,
      OBinaryCompareOperator operator,
      Object rightValue,
      OCommandContext ctx,
      OExpression... args) {
    OLuceneFullTextIndex index = this.searchForIndex(target, ctx);
    if (index != null) {
      return index.size(ctx.getDatabase());
    }
    return 0;
  }

  @Override
  public boolean canExecuteInline(
      OFromClause target,
      OBinaryCompareOperator operator,
      Object rightValue,
      OCommandContext ctx,
      OExpression... args) {
    return false;
  }

  @Override
  public boolean allowsIndexedExecution(
      OFromClause target,
      OBinaryCompareOperator operator,
      Object rightValue,
      OCommandContext ctx,
      OExpression... args) {

    OLuceneFullTextIndex index = this.searchForIndex(target, ctx);

    return index != null;
  }

  @Override
  public boolean shouldExecuteAfterSearch(
      OFromClause target,
      OBinaryCompareOperator operator,
      Object rightValue,
      OCommandContext ctx,
      OExpression... args) {
    return false;
  }
}
