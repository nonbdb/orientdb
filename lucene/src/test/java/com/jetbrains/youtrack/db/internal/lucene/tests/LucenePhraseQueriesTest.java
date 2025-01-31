package com.jetbrains.youtrack.db.internal.lucene.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.jetbrains.youtrack.db.api.schema.PropertyType;
import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import com.jetbrains.youtrack.db.api.record.Vertex;
import com.jetbrains.youtrack.db.api.query.ResultSet;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class LucenePhraseQueriesTest extends LuceneBaseTest {

  @Before
  public void setUp() throws Exception {

    var type = db.createVertexClass("Role");
    type.createProperty(db, "name", PropertyType.STRING);

    db.command(
        "create index Role.name on Role (name) FULLTEXT ENGINE LUCENE "
            + "METADATA{"
            + "\"name_index\": \"org.apache.lucene.analysis.standard.StandardAnalyzer\","
            + "\"name_index_stopwords\": [],"
            + "\"name_query\": \"org.apache.lucene.analysis.standard.StandardAnalyzer\","
            + "\"name_query_stopwords\": []"
            //                + "\"name_query\":
            // \"org.apache.lucene.analysis.core.KeywordAnalyzer\""
            + "} ");

    db.begin();
    var role = db.newVertex("Role");
    role.setProperty("name", "System IT Owner");
    db.save(role);

    role = db.newVertex("Role");
    role.setProperty("name", "System Business Owner");
    db.save(role);

    role = db.newVertex("Role");
    role.setProperty("name", "System Business SME");
    db.save(role);

    role = db.newVertex("Role");
    role.setProperty("name", "System Technical SME");
    db.save(role);

    role = db.newVertex("Role");
    role.setProperty("name", "System");
    db.save(role);

    role = db.newVertex("Role");
    role.setProperty("name", "boat");
    db.save(role);

    role = db.newVertex("Role");
    role.setProperty("name", "moat");
    db.save(role);
    db.commit();
  }

  @Test
  public void testPhraseQueries() throws Exception {

    var vertexes =
        db.command("select from Role where search_class(' \"Business Owner\" ')=true  ");

    assertThat(vertexes).hasSize(1);

    vertexes = db.command("select from Role where search_class( ' \"Owner of Business\" ')=true  ");

    assertThat(vertexes).hasSize(0);

    vertexes = db.command("select from Role where search_class(' \"System Owner\" '  )=true  ");

    assertThat(vertexes).hasSize(0);

    vertexes = db.command("select from Role where search_class(' \"System SME\"~1 '  )=true  ");

    assertThat(vertexes).hasSize(2);

    vertexes =
        db.command("select from Role where search_class(' \"System Business\"~1 '  )=true  ");

    assertThat(vertexes).hasSize(2);

    vertexes = db.command("select from Role where search_class(' /[mb]oat/ '  )=true  ");

    assertThat(vertexes).hasSize(2);
  }

  @Test
  public void testComplexPhraseQueries() throws Exception {

    var vertexes =
        db.command("select from Role where search_class(?)=true", "\"System SME\"~1");

    assertThat(vertexes).allMatch(v -> v.<String>getProperty("name").contains("SME"));

    vertexes = db.command("select from Role where search_class(? )=true", "\"SME System\"~1");

    assertThat(vertexes).isEmpty();

    vertexes = db.command("select from Role where search_class(?) =true", "\"Owner Of Business\"");
    vertexes.stream().forEach(v -> System.out.println("v = " + v.getProperty("name")));

    assertThat(vertexes).isEmpty();

    vertexes =
        db.command("select from Role where search_class(? )=true", "\"System Business SME\"");

    assertThat(vertexes)
        .hasSize(1)
        .allMatch(v -> v.<String>getProperty("name").equalsIgnoreCase("System Business SME"));

    vertexes = db.command("select from Role where search_class(? )=true", "\"System Owner\"~1 -IT");
    assertThat(vertexes)
        .hasSize(1)
        .allMatch(v -> v.<String>getProperty("name").equalsIgnoreCase("System Business Owner"));

    vertexes = db.command("select from Role where search_class(? )=true", "+System +Own*~0.0 -IT");
    assertThat(vertexes)
        .hasSize(1)
        .allMatch(v -> v.<String>getProperty("name").equalsIgnoreCase("System Business Owner"));

    vertexes =
        db.command("select from Role where search_class(? )=true", "\"System Owner\"~1 -Business");
    assertThat(vertexes)
        .hasSize(1)
        .allMatch(v -> v.<String>getProperty("name").equalsIgnoreCase("System IT Owner"));
  }
}
