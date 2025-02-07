package com.jetbrains.youtrack.db.internal.core.sql.executor;

import com.jetbrains.youtrack.db.api.exception.CommandExecutionException;
import com.jetbrains.youtrack.db.api.schema.SchemaClass;
import com.jetbrains.youtrack.db.internal.core.command.BasicCommandContext;
import com.jetbrains.youtrack.db.internal.core.sql.executor.resultset.ExecutionStream;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class CheckClassTypeStepTest extends TestUtilsFixture {

  @Test
  public void shouldCheckSubclasses() {
    BasicCommandContext context = new BasicCommandContext();
    context.setDatabase(db);
    SchemaClass parentClass = createClassInstance();
    SchemaClass childClass = createChildClassInstance(parentClass);
    CheckClassTypeStep step =
        new CheckClassTypeStep(childClass.getName(), parentClass.getName(), context, false);

    ExecutionStream result = step.start(context);
    Assert.assertEquals(0, result.stream(context).count());
  }

  @Test
  public void shouldCheckOneType() {
    BasicCommandContext context = new BasicCommandContext();
    context.setDatabase(db);
    String className = createClassInstance().getName();
    CheckClassTypeStep step = new CheckClassTypeStep(className, className, context, false);

    ExecutionStream result = step.start(context);
    Assert.assertEquals(0, result.stream(context).count());
  }

  @Test(expected = CommandExecutionException.class)
  public void shouldThrowExceptionWhenClassIsNotParent() {
    BasicCommandContext context = new BasicCommandContext();
    context.setDatabase(db);
    CheckClassTypeStep step =
        new CheckClassTypeStep(
            createClassInstance().getName(), createClassInstance().getName(), context, false);

    step.start(context);
  }
}
