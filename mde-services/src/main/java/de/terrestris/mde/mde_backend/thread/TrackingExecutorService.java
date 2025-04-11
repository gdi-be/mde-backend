package de.terrestris.mde.mde_backend.thread;

import lombok.extern.log4j.Log4j2;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An {@link ExecutorService} that tracks the number of running tasks and provides
 * methods to retrieve the count and the set of currently running tasks.
 *
 * @see ExecutorService
 */
@Log4j2
public class TrackingExecutorService implements ExecutorService {

  private final ExecutorService delegate;
  private final AtomicInteger runningTasks = new AtomicInteger(0);
  private final Set<Runnable> currentTasks = ConcurrentHashMap.newKeySet();

  public TrackingExecutorService(ExecutorService delegate) {
    this.delegate = delegate;
  }

  public int getRunningTaskCount() {
    return runningTasks.get();
  }

  public Set<Runnable> getRunningTasks() {
    return Collections.unmodifiableSet(currentTasks);
  }

  private Runnable wrap(Runnable task) {
    return () -> {
      currentTasks.add(task);
      int current = runningTasks.incrementAndGet();
      log.debug("Task started. Running tasks: {}", current);
      try {
        task.run();
      } finally {
        int remaining = runningTasks.decrementAndGet();
        currentTasks.remove(task);
        log.debug("Task finished. Remaining tasks: {}", remaining);
      }
    };
  }

  @Override
  public void execute(Runnable command) {
    delegate.execute(wrap(command));
  }

  @Override
  public Future<?> submit(Runnable task) {
    return delegate.submit(wrap(task));
  }

  @Override public <T> Future<T> submit(Callable<T> task) {
    return delegate.submit(() -> {
      Runnable wrapped = wrap(() -> {
        try {
          task.call();
        } catch (Exception e) {
          throw new CompletionException(e);
        }
      });
      wrapped.run();
      return null;
    });
  }

  @Override
  public <T> Future<T> submit(Runnable task, T result) {
    return delegate.submit(wrap(task), result);
  }

  @Override
  public void shutdown() {
    delegate.shutdown();
  }

  @Override
  public List<Runnable> shutdownNow() {
    return delegate.shutdownNow();
  }

  @Override
  public boolean isShutdown() {
    return delegate.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return delegate.isTerminated();
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return delegate.awaitTermination(timeout, unit);
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
    return delegate.invokeAll(tasks);
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
    throws InterruptedException {
    return delegate.invokeAll(tasks, timeout, unit);
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
    return delegate.invokeAny(tasks);
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
    throws InterruptedException, ExecutionException, TimeoutException {
    return delegate.invokeAny(tasks, timeout, unit);
  }
}
