package com.jetbrains.youtrack.db.internal.common.thread;

import com.jetbrains.youtrack.db.internal.common.log.LogManager;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

/**
 * The same as thread {@link ScheduledThreadPoolExecutor} but also logs all exceptions happened
 * inside of the tasks which caused tasks to stop.
 */
public class ScheduledThreadPoolExecutorWithLogging extends ScheduledThreadPoolExecutor {

  public ScheduledThreadPoolExecutorWithLogging(int corePoolSize, ThreadFactory threadFactory) {
    super(corePoolSize, threadFactory);
  }

  @SuppressWarnings("unused")
  public ScheduledThreadPoolExecutorWithLogging(
      int corePoolSize, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
    super(corePoolSize, threadFactory, handler);
  }

  @Override
  protected void afterExecute(Runnable r, Throwable t) {
    super.afterExecute(r, t);

    if ((t == null) && r instanceof Future<?> future) {
      // scheduled futures can block execution forever if they are not done
      if (future.isDone()) {
        try {
          future.get();
        } catch (CancellationException ce) {
          // ignore it we cancel tasks on shutdown that is normal
        } catch (ExecutionException ee) {
          t = ee.getCause();
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt(); // ignore/reset
        }
      }
    }

    if (t != null) {
      final Thread thread = Thread.currentThread();
      LogManager.instance().error(this, "Exception in thread '%s'", t, thread.getName());
    }
  }
}
