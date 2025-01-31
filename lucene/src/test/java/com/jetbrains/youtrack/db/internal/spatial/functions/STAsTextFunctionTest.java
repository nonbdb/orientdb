package com.jetbrains.youtrack.db.internal.spatial.functions;

import com.jetbrains.youtrack.db.api.config.GlobalConfiguration;
import com.jetbrains.youtrack.db.internal.core.record.impl.EntityImpl;
import com.jetbrains.youtrack.db.internal.spatial.BaseSpatialLuceneTest;
import org.junit.Assert;
import org.junit.Test;
import org.locationtech.jts.io.WKTReader;

public class STAsTextFunctionTest extends BaseSpatialLuceneTest {

  protected static final WKTReader wktReader = new WKTReader();

  @Test
  public void test() {
    var prevValue = GlobalConfiguration.SPATIAL_ENABLE_DIRECT_WKT_READER.getValueAsBoolean();
    GlobalConfiguration.SPATIAL_ENABLE_DIRECT_WKT_READER.setValue(true);

    var values = new String[]{
        "POINT (100.1 80.2)",
        "POINT Z (100.1 80.2 0.3)",
        "LINESTRING (1 1, 1 2, 1 3, 2 2)",
        "LINESTRING Z (1 1 0, 1 2 0, 1 3 1, 2 2 0)",
        "POLYGON ((0 0, 0 1, 1 1, 1 0, 0 0))",
        "POLYGON Z ((0 0 1, 0 1 0, 1 1 0, 1 0 0, 0 0 0))",
        "MULTILINESTRING ((10 10, 20 20, 10 40), (40 40, 30 30, 40 20, 30 10))",
        "MULTILINESTRING Z((10 10 0, 20 20 1, 10 40 2), (40 40 3, 30 30 4, 40 20 5, 30 10 6))",
    };
    try {
      var func = new STGeomFromTextFunction();
      var func2 = new STAsTextFunction();

      for (var value : values) {
        var item = (EntityImpl) func.execute(null, null, null, new Object[]{value},
            null);

        var result = (String) func2.execute(null, null, null, new Object[]{item}, null);

        Assert.assertEquals(value, result);
      }

    } finally {
      GlobalConfiguration.SPATIAL_ENABLE_DIRECT_WKT_READER.setValue(prevValue);
    }
  }
}
