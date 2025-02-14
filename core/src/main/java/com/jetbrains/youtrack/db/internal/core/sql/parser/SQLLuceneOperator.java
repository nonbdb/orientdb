/* Generated By:JJTree: Do not edit this line. SQLLuceneOperator.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.internal.core.sql.executor.metadata.IndexFinder.Operation;
import java.util.Map;

public class SQLLuceneOperator extends SimpleNode implements SQLBinaryCompareOperator {

  public SQLLuceneOperator(int id) {
    super(id);
  }

  public SQLLuceneOperator(YouTrackDBSql p, int id) {
    super(p, id);
  }

  @Override
  public boolean execute(Object left, Object right) {
    throw new UnsupportedOperationException(this + " operator cannot be evaluated in this context");
  }

  @Override
  public String toString() {
    return "LUCENE";
  }

  @Override
  public void toGenericStatement(StringBuilder builder) {
    builder.append("LUCENE");
  }

  @Override
  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("LUCENE");
  }

  @Override
  public boolean supportsBasicCalculation() {
    return true;
  }

  @Override
  public Operation getOperation() {
    return Operation.FuzzyEq;
  }

  @Override
  public SQLLuceneOperator copy() {
    return this;
  }

  @Override
  public boolean equals(Object obj) {
    return obj != null && obj.getClass().equals(this.getClass());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
/* JavaCC - OriginalChecksum=bda1e010e6ba48c815829b22ce458b9d (do not edit this line) */
