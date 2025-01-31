package com.jetbrains.youtrack.db.internal.core.sql.executor;

import com.jetbrains.youtrack.db.api.exception.CommandExecutionException;
import com.jetbrains.youtrack.db.internal.core.command.BasicCommandContext;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class CheckClassTypeStepTest extends TestUtilsFixture {

  @Test
  public void shouldCheckSubclasses() {
    var context = new BasicCommandContext();
    context.setDatabase(db);
    var parentClass = createClassInstance();
    var childClass = createChildClassInstance(parentClass);
    var step =
        new CheckClassTypeStep(childClass.getName(), parentClass.getName(), context, false);

    var result = step.start(context);
    Assert.assertEquals(0, result.stream(context).count());
  }

  @Test
  public void shouldCheckOneType() {
    var context = new BasicCommandContext();
    context.setDatabase(db);
    var className = createClassInstance().getName();
    var step = new CheckClassTypeStep(className, className, context, false);

    var result = step.start(context);
    Assert.assertEquals(0, result.stream(context).count());
  }

  @Test(expected = CommandExecutionException.class)
  public void shouldThrowExceptionWhenClassIsNotParent() {
    var context = new BasicCommandContext();
    context.setDatabase(db);
    var step =
        new CheckClassTypeStep(
            createClassInstance().getName(), createClassInstance().getName(), context, false);

    step.start(context);
  }
}
