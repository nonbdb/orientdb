package com.jetbrains.youtrack.db.internal.core.sql.executor;

import com.jetbrains.youtrack.db.api.query.ResultSet;

/**
 *
 */
public class ExecutionPlanPrintUtils {

  public static void printExecutionPlan(ResultSet result) {
    printExecutionPlan(null, result);
  }

  public static void printExecutionPlan(String query, ResultSet result) {
    //    if (query != null) {
    //      System.out.println(query);
    //    }
    //    result.getExecutionPlan().ifPresent(x -> System.out.println(x.prettyPrint(0, 3)));
    //    System.out.println();
  }
}