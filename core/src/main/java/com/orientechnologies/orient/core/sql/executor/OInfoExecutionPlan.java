package com.orientechnologies.orient.core.sql.executor;

import com.orientechnologies.orient.core.db.ODatabaseSessionInternal;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class OInfoExecutionPlan implements OExecutionPlan {

  private List<OExecutionStep> steps = new ArrayList<>();
  private String prettyPrint;
  private String type;
  private String javaType;
  private Integer cost;
  private String stmText;

  @Override
  public List<OExecutionStep> getSteps() {
    return steps;
  }

  @Override
  public String prettyPrint(int depth, int indent) {
    return prettyPrint;
  }

  @Override
  public OResult toResult(ODatabaseSessionInternal db) {
    return null;
  }

  public void setSteps(List<OExecutionStep> steps) {
    this.steps = steps;
  }

  public String getPrettyPrint() {
    return prettyPrint;
  }

  public void setPrettyPrint(String prettyPrint) {
    this.prettyPrint = prettyPrint;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getJavaType() {
    return javaType;
  }

  public void setJavaType(String javaType) {
    this.javaType = javaType;
  }

  public Integer getCost() {
    return cost;
  }

  public void setCost(Integer cost) {
    this.cost = cost;
  }

  public String getStmText() {
    return stmText;
  }

  public void setStmText(String stmText) {
    this.stmText = stmText;
  }

  @Override
  public String toString() {
    return prettyPrint;
  }
}
