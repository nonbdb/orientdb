/* Generated By:JJTree: Do not edit this line. OContainsKeyOperator.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.orientechnologies.orient.core.sql.parser;

import com.orientechnologies.orient.core.sql.executor.metadata.OIndexFinder.Operation;
import java.util.Map;

public class OContainsKeyOperator extends SimpleNode implements OBinaryCompareOperator {

  public OContainsKeyOperator(int id) {
    super(id);
  }

  public OContainsKeyOperator(OrientSql p, int id) {
    super(p, id);
  }

  @Override
  public boolean execute(Object left, Object right) {
    if (left == null) {
      return false;
    }
    if (left instanceof Map<?, ?>) {
      final Map<String, ?> map = (Map<String, ?>) left;
      return map.containsKey(right);
    }
    return false;
  }

  @Override
  public String toString() {
    return "CONTAINSKEY";
  }

  @Override
  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("CONTAINSKEY");
  }

  @Override
  public void toGenericStatement(StringBuilder builder) {
    builder.append("CONTAINSKEY");
  }

  @Override
  public boolean supportsBasicCalculation() {
    return true;
  }

  public OContainsKeyOperator copy() {
    return this;
  }

  @Override
  public Operation getOperation() {
    return Operation.Eq;
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
/* JavaCC - OriginalChecksum=1a03daaa6712eb981b070e8e94960951 (do not edit this line) */
