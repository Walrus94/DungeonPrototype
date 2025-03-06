package org.dungeon.prototype.async;

import lombok.extern.slf4j.Slf4j;
import org.dungeon.prototype.async.metrics.TaskContext;
import org.dungeon.prototype.async.metrics.TaskContextData;
import org.dungeon.prototype.async.metrics.TaskMetrics;
import org.dungeon.prototype.exception.DungeonPrototypeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class AsyncJobHandler implements AsyncJobService {

    @Autowired
    private AsyncTaskExecutor asyncTaskExecutor;
    private static final Map<Long, CountDownLatch> chatLatches = new ConcurrentHashMap<>();

    @Autowired
    private TaskMetrics taskMetrics;

    @Async
    @Override
    public Future<?> submitItemGenerationTask(Runnable job, TaskType taskType, long chatId) {
        CountDownLatch latch = chatLatches.computeIfAbsent(chatId, k -> new CountDownLatch(2));//TODO: increment when Usable items generation is implemented
        return asyncTaskExecutor.submit(() -> {
            try {
                executeTask(job, taskType, chatId);
            } finally {
                log.info("Counting down latch for chatId: {}", chatId);
                latch.countDown();
            }
        });
    }

    @Async
    @Override
    public <T> Future<T> submitTask(Callable<T> job, TaskType taskType, long chatId) {
        return (Future<T>) asyncTaskExecutor.submit(() -> {
            try {
                chatLatches.get(chatId).await();
                executeTask(job, taskType, chatId);
            } catch (InterruptedException e) {
                throw new DungeonPrototypeException(e.getMessage());
            } finally {
                if (taskType == TaskType.GET_DEFAULT_INVENTORY) {
                    chatLatches.remove(chatId);
                }
            }
        });
    }

    @Async
    @Override
    public Future<?> submitMapPopulationTask(Runnable job, TaskType taskType, long chatId, long clusterId) {
        return asyncTaskExecutor.submit(() -> {
            try {
                chatLatches.get(chatId).await();
                executeTask(job, taskType, chatId, clusterId);
            } catch (InterruptedException e) {
                throw new DungeonPrototypeException(e.getMessage());
            } finally {
                log.info("Task completed for chatId: {}", chatId);
            }
        });
    }

    private <T> Future<T> executeTask(Callable<T> job, TaskType taskType, long chatId) {
        try (var taskScope = new StructuredTaskScope.ShutdownOnFailure()) {
            return (Future<T>) asyncTaskExecutor.submit(() -> {
                var context = new TaskContextData(chatId, 0, taskType);
                taskScope.fork(() -> ScopedValue
                        .where(TaskContext.CONTEXT, context)
                        .call(() -> {
                            long start = System.currentTimeMillis();
                            taskMetrics.addActiveTask(context);
                            try {
                                return job.call();
                            } catch (Exception e) {
                                taskMetrics.getTaskFailureCounter(taskType.name()).increment();
                                throw e;
                            } finally {
                                long elapsed = System.currentTimeMillis() - start;
                                taskMetrics.removeCompletedTask(context);
                                taskMetrics.getTaskTimer(taskType.name()).record(elapsed, TimeUnit.MILLISECONDS);
                            }
                        }));
                try {
                    while (!chatLatches.get(chatId).await(1, TimeUnit.SECONDS)) {
                        log.info("Waiting for chatId map and items generation: {}", chatId);
                    }
                    taskScope.join();
                    taskScope.throwIfFailed();
                } catch (InterruptedException | ExecutionException e) {
                    throw new DungeonPrototypeException("Task execution interrupted:" + e.getMessage());
                } finally {
                    taskMetrics.removeCompletedTask(context);
                }
            });
        }
    }

    private void executeTask(Runnable job, TaskType taskType, long chatId) {
        try (var taskScope = new StructuredTaskScope.ShutdownOnFailure()) {
            asyncTaskExecutor.submit(() -> {
                var context = new TaskContextData(chatId, 0, taskType);
                taskScope.fork(() -> ScopedValue
                        .where(TaskContext.CONTEXT, context)
                        .call(() -> {
                            long start = System.currentTimeMillis();
                            taskMetrics.addActiveTask(context);
                            try {
                                job.run();
                            } catch (Exception e) {
                                taskMetrics.getTaskFailureCounter(taskType.name()).increment();
                                throw e;
                            } finally {
                                long elapsed = System.currentTimeMillis() - start;
                                taskMetrics.removeCompletedTask(context);
                                taskMetrics.getTaskTimer(taskType.name()).record(elapsed, TimeUnit.MILLISECONDS);
                            }
                            return null;
                        }));
                try {
                    while (!chatLatches.get(chatId).await(1, TimeUnit.SECONDS)) {
                        log.info("Waiting for chatId map and items generation: {}", chatId);
                    }
                    taskScope.join();
                    taskScope.throwIfFailed();
                } catch (InterruptedException | ExecutionException e) {
                    throw new DungeonPrototypeException("Task execution interrupted:" + e.getMessage());
                } finally {
                    taskMetrics.removeCompletedTask(context);
                }
            });
        }
    }

    private void executeTask(Runnable job, TaskType taskType, long chatId, long clusterId) {
        try (var taskScope = new StructuredTaskScope.ShutdownOnFailure()) {
            asyncTaskExecutor.submit(() -> {
                var context = new TaskContextData(chatId, clusterId, taskType);
                taskScope.fork(() -> ScopedValue
                        .where(TaskContext.CONTEXT, context)
                        .call(() -> {
                            long start = System.currentTimeMillis();
                            taskMetrics.addActiveTask(context);
                            try {
                                job.run();
                            } catch (Exception e) {
                                taskMetrics.getTaskFailureCounter(taskType.name()).increment();
                                throw e;
                            } finally {
                                long elapsed = System.currentTimeMillis() - start;
                                taskMetrics.removeCompletedTask(context);
                                taskMetrics.getTaskTimer(taskType.name()).record(elapsed, TimeUnit.MILLISECONDS);
                            }
                            return null;
                        }));
                try {
                    taskScope.join();
                    taskScope.throwIfFailed();
                } catch (InterruptedException | ExecutionException e) {
                    throw new DungeonPrototypeException("Task execution interrupted:" + e.getMessage());
                } finally {
                    taskMetrics.removeCompletedTask(context);
                }
            });
        }
    }
}
