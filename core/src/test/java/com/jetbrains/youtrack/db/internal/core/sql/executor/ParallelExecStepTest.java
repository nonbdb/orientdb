package com.jetbrains.youtrack.db.internal.core.sql.executor;

import com.jetbrains.youtrack.db.internal.core.command.BasicCommandContext;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.id.RecordId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

/**
 *
 */
public class ParallelExecStepTest {

  @Test
  public void test() {
    CommandContext ctx = new BasicCommandContext();
    List<InternalExecutionPlan> subPlans = new ArrayList<>();
    for (int i = 0; i < 4; i++) {
      FetchFromRidsStep step0 =
          new FetchFromRidsStep(Collections.singleton(new RecordId(12, i)), ctx, false);
      FetchFromRidsStep step1 =
          new FetchFromRidsStep(Collections.singleton(new RecordId(12, i)), ctx, false);
      InternalExecutionPlan plan = new SelectExecutionPlan(ctx);
      plan.getSteps().add(step0);
      plan.getSteps().add(step1);
      subPlans.add(plan);
    }

    ParallelExecStep step = new ParallelExecStep(subPlans, ctx, false);

    SelectExecutionPlan plan = new SelectExecutionPlan(ctx);
    plan.getSteps()
        .add(new FetchFromRidsStep(Collections.singleton(new RecordId(12, 100)), ctx, false));
    plan.getSteps().add(step);
    plan.getSteps()
        .add(new FetchFromRidsStep(Collections.singleton(new RecordId(12, 100)), ctx, false));
  }
}
