/* Generated By:JJTree: Do not edit this line. OOptimizeDatabaseStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.orientechnologies.orient.core.sql.parser;

import com.orientechnologies.common.log.OLogManager;
import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.YTDatabaseSessionInternal;
import com.orientechnologies.orient.core.db.record.YTIdentifiable;
import com.orientechnologies.orient.core.db.record.ridbag.RidBag;
import com.orientechnologies.orient.core.id.YTRID;
import com.orientechnologies.orient.core.record.impl.YTEntityImpl;
import com.orientechnologies.orient.core.sql.executor.YTResultInternal;
import com.orientechnologies.orient.core.sql.executor.resultset.OExecutionStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class OOptimizeDatabaseStatement extends OSimpleExecStatement {

  protected List<OCommandLineOption> options = new ArrayList<OCommandLineOption>();
  private final int batch = 1000;

  public OOptimizeDatabaseStatement(int id) {
    super(id);
  }

  public OOptimizeDatabaseStatement(OrientSql p, int id) {
    super(p, id);
  }

  public void addOption(OCommandLineOption option) {
    this.options.add(option);
  }

  @Override
  public OExecutionStream executeSimple(OCommandContext ctx) {
    var db = ctx.getDatabase();
    YTResultInternal result = new YTResultInternal(db);
    result.setProperty("operation", "optimize databae");

    if (isOptimizeEdges()) {
      String edges = optimizeEdges(db);
      result.setProperty("optimizeEdges", edges);
    }

    return OExecutionStream.singleton(result);
  }

  @Override
  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("OPTIMIZE DATABASE");
    for (OCommandLineOption option : options) {
      builder.append(" ");
      option.toString(params, builder);
    }
  }

  @Override
  public void toGenericStatement(StringBuilder builder) {
    builder.append("OPTIMIZE DATABASE");
    for (OCommandLineOption option : options) {
      builder.append(" ");
      option.toGenericStatement(builder);
    }
  }

  @Override
  public OOptimizeDatabaseStatement copy() {
    OOptimizeDatabaseStatement result = new OOptimizeDatabaseStatement(-1);
    result.options =
        options == null
            ? null
            : options.stream().map(OCommandLineOption::copy).collect(Collectors.toList());
    return result;
  }

  private String optimizeEdges(YTDatabaseSessionInternal db) {
    long transformed = 0;
    final long totalEdges = db.countClass("E");
    long browsedEdges = 0;
    long lastLapBrowsed = 0;
    long lastLapTime = System.currentTimeMillis();

    for (YTEntityImpl doc : db.browseClass("E")) {
      if (Thread.currentThread().isInterrupted()) {
        break;
      }

      browsedEdges++;

      if (doc != null) {
        if (doc.fields() == 2) {
          final YTRID edgeIdentity = doc.getIdentity();

          final YTEntityImpl outV = doc.getPropertyInternal("out");
          final YTEntityImpl inV = doc.getPropertyInternal("in");

          // OUTGOING
          final Object outField = outV.getPropertyInternal("out_" + doc.getClassName());
          if (outField instanceof RidBag) {
            final Iterator<YTIdentifiable> it = ((RidBag) outField).iterator();
            while (it.hasNext()) {
              YTIdentifiable v = it.next();
              if (edgeIdentity.equals(v)) {
                // REPLACE EDGE RID WITH IN-VERTEX RID
                it.remove();
                ((RidBag) outField).add(inV.getIdentity());
                break;
              }
            }
          }

          outV.save();

          // INCOMING
          final Object inField = inV.getPropertyInternal("in_" + doc.getClassName());
          if (outField instanceof RidBag) {
            final Iterator<YTIdentifiable> it = ((RidBag) inField).iterator();
            while (it.hasNext()) {
              YTIdentifiable v = it.next();
              if (edgeIdentity.equals(v)) {
                // REPLACE EDGE RID WITH IN-VERTEX RID
                it.remove();
                ((RidBag) inField).add(outV.getIdentity());
                break;
              }
            }
          }

          inV.save();

          doc.delete();

          final long now = System.currentTimeMillis();

          if (verbose() && (now - lastLapTime > 2000)) {
            final long elapsed = now - lastLapTime;

            OLogManager.instance()
                .info(
                    this,
                    "Browsed %,d of %,d edges, transformed %,d so far (%,d edges/sec)",
                    browsedEdges,
                    totalEdges,
                    transformed,
                    (((browsedEdges - lastLapBrowsed) * 1000 / elapsed)));

            lastLapTime = System.currentTimeMillis();
            lastLapBrowsed = browsedEdges;
          }
        }
      }
    }

    return "Transformed " + transformed + " regular edges in lightweight edges";
  }

  private boolean isOptimizeEdges() {
    for (OCommandLineOption option : options) {
      if (option.name.getStringValue().equalsIgnoreCase("LWEDGES")) {
        return true;
      }
    }
    return false;
  }

  private boolean verbose() {
    for (OCommandLineOption option : options) {
      if (option.name.getStringValue().equalsIgnoreCase("NOVERBOSE")) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    OOptimizeDatabaseStatement that = (OOptimizeDatabaseStatement) o;

    return Objects.equals(options, that.options);
  }

  @Override
  public int hashCode() {
    return options != null ? options.hashCode() : 0;
  }
}
/* JavaCC - OriginalChecksum=b85d66f84bbae92224565361df9d0c91 (do not edit this line) */
