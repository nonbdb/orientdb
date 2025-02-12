package com.jetbrains.youtrack.db.internal.lucene.functions;

import static org.assertj.core.api.Assertions.assertThat;

import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.lucene.tests.LuceneBaseTest;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class LuceneSearchOnClassFunctionTest extends LuceneBaseTest {

  @Before
  public void setUp() throws Exception {
    final var stream = ClassLoader.getSystemResourceAsStream("testLuceneIndex.sql");
    session.execute("sql", getScriptFromStream(stream));
    session.command("create index Song.title on Song (title) FULLTEXT ENGINE LUCENE ");
  }

  @Test
  public void shouldSearchOnClass() throws Exception {

    var resultSet = session.query("SELECT from Song where SEARCH_Class('BELIEVE') = true");

    assertThat(resultSet).hasSize(2);

    resultSet.close();
  }

  @Test
  public void shouldSearchOnSingleFieldWithLeadingWildcard() throws Exception {

    var resultSet =
        session.query(
            "SELECT from Song where SEARCH_CLASS( '*EVE*', {'allowLeadingWildcard': true}) = true");

    assertThat(resultSet).hasSize(14);

    resultSet.close();
  }

  @Test
  public void shouldSearchInOr() throws Exception {

    var resultSet =
        session.query(
            "SELECT from Song where SEARCH_CLASS('BELIEVE') = true OR SEARCH_CLASS('GOODNIGHT') ="
                + " true ");

    assertThat(resultSet).hasSize(5);
    resultSet.close();
  }

  @Test
  public void shouldSearchInAnd() throws Exception {

    var resultSet =
        session.query(
            "SELECT from Song where SEARCH_CLASS('GOODNIGHT') = true AND SEARCH_CLASS( 'Irene',"
                + " {'allowLeadingWildcard': true}) = true ");

    assertThat(resultSet).hasSize(1);
    resultSet.close();
  }

  public void shouldThrowExceptionWithWrongClass() throws Exception {

    var resultSet =
        session.query(
            "SELECT from Author where SEARCH_CLASS('(description:happiness) (lyrics:sad)  ') = true"
                + " ");
    resultSet.close();
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowExceptionIfMoreIndexesAreDefined() {

    session.command("create index Song.author on Song (author) FULLTEXT ENGINE LUCENE ");

    var resultSet =
        session.query("SELECT from Song where SEARCH_CLASS('not important, will fail') = true ");
    resultSet.close();
  }

  @Test
  public void shouldHighlightTitle() throws Exception {

    var resultSet =
        session.query(
            "SELECT title, $title_hl from Song where SEARCH_CLASS('believe', {highlight: { fields:"
                + " ['title'], 'start': '<span>', 'end': '</span>' } }) = true ");

    resultSet.stream()
        .forEach(
            r ->
                assertThat(r.<String>getProperty("$title_hl"))
                    .containsIgnoringCase("<span>believe</span>"));
    resultSet.close();
  }

  @Test
  public void shouldHighlightWithNullValues() throws Exception {

    session.command("drop index Song.title");

    session.command(
        "create index Song.title_description on Song (title,description) FULLTEXT ENGINE LUCENE ");

    session.begin();
    session.command("insert into Song set description = 'shouldHighlightWithNullValues'");
    session.commit();

    var resultSet =
        session.query(
            "SELECT title, $title_hl,description, $description_hl  from Song where"
                + " SEARCH_CLASS('shouldHighlightWithNullValues', {highlight: { fields:"
                + " ['title','description'], 'start': '<span>', 'end': '</span>' } }) = true ");

    resultSet.stream()
        .forEach(
            r ->
                assertThat(r.<String>getProperty("$description_hl"))
                    .containsIgnoringCase("<span>shouldHighlightWithNullValues</span>"));
    resultSet.close();
  }

  @Test
  public void shouldSupportParameterizedMetadata() throws Exception {
    final var query = "SELECT from Song where SEARCH_CLASS('*EVE*', ?) = true";

    session.query(query, "{'allowLeadingWildcard': true}").close();
    session.query(query, new EntityImpl(session, "allowLeadingWildcard", Boolean.TRUE)).close();

    Map<String, Object> mdMap = new HashMap();
    mdMap.put("allowLeadingWildcard", true);
    session.query(query, new Object[]{mdMap}).close();
  }
}
