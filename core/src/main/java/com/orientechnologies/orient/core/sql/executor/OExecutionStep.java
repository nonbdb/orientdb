package com.orientechnologies.orient.core.sql.executor;

import com.orientechnologies.orient.core.db.ODatabaseSessionInternal;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 */
public interface OExecutionStep {

  String getName();

  String getType();

  String getTargetNode();

  String getDescription();

  List<OExecutionStep> getSubSteps();

  /**
   * returns the absolute cost (in nanoseconds) of the execution of this step
   *
   * @return the absolute cost (in nanoseconds) of the execution of this step, -1 if not calculated
   */
  default long getCost() {
    return -1L;
  }

  default OResult toResult(ODatabaseSessionInternal db) {
    OResultInternal result = new OResultInternal(db);
    result.setProperty("name", getName());
    result.setProperty("type", getType());
    result.setProperty("targetNode", getType());
    result.setProperty(OInternalExecutionPlan.JAVA_TYPE, getClass().getName());
    result.setProperty("cost", getCost());
    result.setProperty(
        "subSteps",
        getSubSteps() == null
            ? null
            : getSubSteps().stream().map(x -> x.toResult(db)).collect(Collectors.toList()));
    result.setProperty("description", getDescription());
    return result;
  }
}
