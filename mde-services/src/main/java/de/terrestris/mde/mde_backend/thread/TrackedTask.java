package de.terrestris.mde.mde_backend.thread;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class TrackedTask implements Runnable {
  private final String taskId;
  private final Runnable delegate;

  public TrackedTask(String taskId, Runnable delegate) {
    this.taskId = taskId;
    this.delegate = delegate;
  }

  @Override
  public void run() {
    delegate.run();
  }
}
