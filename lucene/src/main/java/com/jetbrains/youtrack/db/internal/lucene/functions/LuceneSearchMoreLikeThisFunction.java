package com.jetbrains.youtrack.db.internal.lucene.functions;

import com.jetbrains.youtrack.db.api.DatabaseSession;
import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.api.record.Entity;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.api.record.RID;
import com.jetbrains.youtrack.db.api.record.Record;
import com.jetbrains.youtrack.db.internal.common.log.LogManager;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.id.ChangeableRecordId;
import com.jetbrains.youtrack.db.internal.core.id.RecordId;
import com.jetbrains.youtrack.db.internal.core.metadata.MetadataInternal;
import com.jetbrains.youtrack.db.internal.core.record.RecordAbstract;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.core.sql.functions.IndexableSQLFunction;
import com.jetbrains.youtrack.db.internal.core.sql.functions.SQLFunctionAbstract;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLBinaryCompareOperator;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLExpression;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLFromClause;
import com.jetbrains.youtrack.db.internal.core.sql.parser.SQLFromItem;
import com.jetbrains.youtrack.db.internal.lucene.collections.LuceneCompositeKey;
import com.jetbrains.youtrack.db.internal.lucene.exception.LuceneIndexException;
import com.jetbrains.youtrack.db.internal.lucene.index.LuceneFullTextIndex;
import com.jetbrains.youtrack.db.internal.lucene.query.LuceneKeyAndMetadata;
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
public class LuceneSearchMoreLikeThisFunction extends SQLFunctionAbstract
    implements IndexableSQLFunction {

  public static final String NAME = "search_more";

  public LuceneSearchMoreLikeThisFunction() {
    super(LuceneSearchMoreLikeThisFunction.NAME, 1, 2);
  }

  @Override
  public String getName(DatabaseSession session) {
    return LuceneSearchMoreLikeThisFunction.NAME;
  }

  @Override
  public Object execute(
      Object iThis,
      Identifiable iCurrentRecord,
      Object iCurrentResult,
      Object[] params,
      CommandContext ctx) {

    throw new LuceneIndexException("SEARCH_MORE can't be executed by entity");
  }

  @Override
  public String getSyntax(DatabaseSession session) {
    return "SEARCH_MORE( [rids], [ metdatada {} ] )";
  }

  @Override
  public Iterable<Identifiable> searchFromTarget(
      SQLFromClause target,
      SQLBinaryCompareOperator operator,
      Object rightValue,
      CommandContext ctx,
      SQLExpression... args) {

    LuceneFullTextIndex index = this.searchForIndex(target, ctx);

    if (index == null) {
      return Collections.emptySet();
    }

    IndexSearcher searcher = index.searcher();

    SQLExpression expression = args[0];

    var metadata = parseMetadata(args);

    List<String> ridsAsString = parseRids(ctx, expression);

    var db = ctx.getDatabase();
    List<Record> others =
        ridsAsString.stream()
            .map(
                rid -> {
                  RecordId recordId = new ChangeableRecordId();

                  recordId.fromString(rid);
                  recordId = recordId.copy();
                  return recordId;
                })
            .<Record>map(recordId1 -> recordId1.getRecord(db))
            .toList();

    MoreLikeThis mlt = buildMoreLikeThis(index, searcher, metadata);

    Builder queryBuilder = new Builder();

    excludeOtherFromResults(ridsAsString, queryBuilder);

    addLikeQueries(others, mlt, queryBuilder);

    Query mltQuery = queryBuilder.build();

    Set<Identifiable> luceneResultSet;
    try (Stream<RID> rids =
        index
            .getInternal()
            .getRids(ctx.getDatabase(),
                new LuceneKeyAndMetadata(
                    new LuceneCompositeKey(Collections.singletonList(mltQuery.toString()))
                        .setContext(ctx),
                    metadata))) {
      luceneResultSet = rids.collect(Collectors.toSet());
    }

    return luceneResultSet;
  }

  private List<String> parseRids(CommandContext ctx, SQLExpression expression) {

    Object expResult = expression.execute((Identifiable) null, ctx);

    // single rind
    if (expResult instanceof Identifiable) {
      return Collections.singletonList(((Identifiable) expResult).getIdentity().toString());
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
      if (item instanceof Result) {
        if (((Result) item).isEntity()) {
          rids.add(((Result) item).getIdentity().get().toString());
        } else {
          var properties = ((Result) item).getPropertyNames();
          if (properties.size() == 1) {
            Object val = ((Result) item).getProperty(properties.iterator().next());
            if (val instanceof Identifiable) {
              rids.add(((Identifiable) val).getIdentity().toString());
            }
          }
        }
      } else if (item instanceof Identifiable) {
        rids.add(((Identifiable) item).getIdentity().toString());
      }
    }
    return rids;
  }

  private static Map<String, ?> parseMetadata(SQLExpression[] args) {
    EntityImpl metadata = new EntityImpl(null);
    if (args.length == 2) {
      metadata.updateFromJSON(args[1].toString());
    }
    return metadata.toMap();
  }

  private MoreLikeThis buildMoreLikeThis(
      LuceneFullTextIndex index, IndexSearcher searcher, Map<String, ?> metadata) {

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

  private void addLikeQueries(List<Record> others, MoreLikeThis mlt, Builder queryBuilder) {
    others.stream()
        .map(or -> ((RecordAbstract) or).getSession().<Entity>load(or.getIdentity()))
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
                            LogManager.instance()
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

  private LuceneFullTextIndex searchForIndex(SQLFromClause target, CommandContext ctx) {
    SQLFromItem item = target.getItem();

    String className = item.getIdentifier().getStringValue();

    return searchForIndex(ctx, className);
  }

  private LuceneFullTextIndex searchForIndex(CommandContext ctx, String className) {
    var db = ctx.getDatabase();
    db.activateOnCurrentThread();
    MetadataInternal dbMetadata = db.getMetadata();

    List<LuceneFullTextIndex> indices =
        dbMetadata.getImmutableSchemaSnapshot().getClassInternal(
                className).getIndexesInternal(db).stream()
            .filter(idx -> idx instanceof LuceneFullTextIndex)
            .map(idx -> (LuceneFullTextIndex) idx)
            .toList();

    if (indices.size() > 1) {
      throw new IllegalArgumentException("too many full-text indices on given class: " + className);
    }

    return indices.isEmpty() ? null : indices.get(0);
  }

  @Override
  public long estimate(
      SQLFromClause target,
      SQLBinaryCompareOperator operator,
      Object rightValue,
      CommandContext ctx,
      SQLExpression... args) {
    LuceneFullTextIndex index = this.searchForIndex(target, ctx);
    if (index != null) {
      return index.size(ctx.getDatabase());
    }
    return 0;
  }

  @Override
  public boolean canExecuteInline(
      SQLFromClause target,
      SQLBinaryCompareOperator operator,
      Object rightValue,
      CommandContext ctx,
      SQLExpression... args) {
    return false;
  }

  @Override
  public boolean allowsIndexedExecution(
      SQLFromClause target,
      SQLBinaryCompareOperator operator,
      Object rightValue,
      CommandContext ctx,
      SQLExpression... args) {

    LuceneFullTextIndex index = this.searchForIndex(target, ctx);

    return index != null;
  }

  @Override
  public boolean shouldExecuteAfterSearch(
      SQLFromClause target,
      SQLBinaryCompareOperator operator,
      Object rightValue,
      CommandContext ctx,
      SQLExpression... args) {
    return false;
  }
}
