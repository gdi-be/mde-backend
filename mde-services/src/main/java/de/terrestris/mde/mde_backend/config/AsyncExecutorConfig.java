package de.terrestris.mde.mde_backend.config;

import de.terrestris.mde.mde_backend.thread.TrackingExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@Log4j2
public class AsyncExecutorConfig {

  @Bean
  public TrackingExecutorService trackingTaskExecutor() {
    int corePoolSize = 5;
    int maxPoolSize = 5;
    int queueCapacity = 10;

    RejectedExecutionHandler rejectionHandler =
        (r, executor) -> {
          try {
            log.warn("Queue full, blocking task submission…");
            executor.getQueue().put(r);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RejectedExecutionException("Task submission interrupted", e);
          }
        };

    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(corePoolSize);
    executor.setMaxPoolSize(maxPoolSize);
    executor.setQueueCapacity(queueCapacity);
    executor.setThreadNamePrefix("MDE-Thread-Executor");
    executor.setAllowCoreThreadTimeOut(true);
    executor.setKeepAliveSeconds(60);
    executor.setRejectedExecutionHandler(rejectionHandler);

    executor.initialize();

    return new TrackingExecutorService(executor.getThreadPoolExecutor());
  }
}
