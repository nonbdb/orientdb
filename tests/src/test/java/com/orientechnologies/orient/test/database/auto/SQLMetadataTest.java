/*
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.orientechnologies.orient.test.database.auto;

import com.orientechnologies.orient.core.exception.YTQueryParsingException;
import com.orientechnologies.orient.core.record.impl.YTDocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * SQL test against metadata.
 */
@Test
public class SQLMetadataTest extends DocumentDBBaseTest {

  @Parameters(value = "remote")
  public SQLMetadataTest(boolean remote) {
    super(remote);
  }

  @Test
  public void querySchemaClasses() {
    List<YTDocument> result =
        database
            .command(new OSQLSynchQuery<YTDocument>("select expand(classes) from metadata:schema"))
            .execute(database);

    Assert.assertTrue(result.size() != 0);
  }

  @Test
  public void querySchemaProperties() {
    List<YTDocument> result =
        database
            .command(
                new OSQLSynchQuery<YTDocument>(
                    "select expand(properties) from (select expand(classes) from metadata:schema)"
                        + " where name = 'OUser'"))
            .execute(database);

    Assert.assertTrue(result.size() != 0);
  }

  @Test
  public void queryIndexes() {
    List<YTDocument> result =
        database
            .command(
                new OSQLSynchQuery<YTDocument>("select expand(indexes) from metadata:indexmanager"))
            .execute(database);

    Assert.assertTrue(result.size() != 0);
  }

  @Test
  public void queryMetadataNotSupported() {
    try {
      database
          .command(new OSQLSynchQuery<YTDocument>("select expand(indexes) from metadata:blaaa"))
          .execute(database);
      Assert.fail();
    } catch (YTQueryParsingException e) {
    }
  }
}
