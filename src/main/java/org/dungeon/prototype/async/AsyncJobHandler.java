package org.dungeon.prototype.async;

import lombok.extern.slf4j.Slf4j;
import org.dungeon.prototype.async.metrics.TaskContext;
import org.dungeon.prototype.async.metrics.TaskContextData;
import org.dungeon.prototype.async.metrics.TaskMetrics;
import org.dungeon.prototype.exception.DungeonPrototypeException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
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
public class AsyncJobHandler {

    private final AsyncTaskExecutor asyncTaskExecutor;
    private final Map<Long, CountDownLatch> chatLatches;

    private final TaskMetrics taskMetrics;

    public AsyncJobHandler(@Qualifier(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME)
                           AsyncTaskExecutor asyncTaskExecutor, TaskMetrics taskMetrics) {
        this.asyncTaskExecutor = asyncTaskExecutor;
        this.chatLatches = new ConcurrentHashMap<>();
        this.taskMetrics = taskMetrics;
    }

    @Async
    public void submitItemGenerationTask(Runnable job, TaskType taskType, long chatId) {
        log.debug("Submitting item generation {} task for chatId: {}", taskType, chatId);
        CountDownLatch latch = chatLatches.computeIfAbsent(chatId, k -> new CountDownLatch(2));//TODO: increment when Usable items generation is implemented
        asyncTaskExecutor.submit(() -> {
            try {
                job.run();
            } finally {
                log.info("Counting down latch for chatId: {}", chatId);
                latch.countDown();
            }
        });
    }

    @Async
    public Future<?> submitTask(Callable<?> job, TaskType taskType, long chatId) {
        log.debug("Submitting task for of type {} chatId: {}", taskType, chatId);
        return asyncTaskExecutor.submit(() -> {
            try {
                if (chatLatches.containsKey(chatId) && chatLatches.get(chatId).getCount() > 0) {
                    chatLatches.get(chatId).await();
                    chatLatches.remove(chatId);
                }
                return job.call();
            } catch (InterruptedException e) {
                throw new DungeonPrototypeException(e.getMessage());
            }
        });
    }

    @Async
    public Future<?> submitMapPopulationTask(Callable<?> job, TaskType taskType, long chatId, long clusterId) {
        log.debug("Submitting map population task for chatId: {}, clusterId: {}", chatId, clusterId);
        return asyncTaskExecutor.submit(() -> {
            try {
                if (chatLatches.containsKey(chatId) && chatLatches.get(chatId).getCount() > 0) {
                    log.debug("Awaiting for chatId: {} ", chatId);
                    chatLatches.get(chatId).await();
                    chatLatches.remove(chatId);
                }
                executeTask(job, taskType, chatId, clusterId);
            } catch (InterruptedException e) {
                throw new DungeonPrototypeException(e.getMessage());
            }
        });
    }

    private <T> Future<T> executeTask(Callable<T> job, TaskType taskType, long chatId, long clusterId) {
        try (var taskScope = new StructuredTaskScope.ShutdownOnFailure()) {
            var context = new TaskContextData(chatId, clusterId, taskType);
            taskScope.fork(() -> ScopedValue
                    .where(TaskContext.CONTEXT, context)
                    .call(() -> {
                        long start = System.currentTimeMillis();
                        taskMetrics.addActiveTask(context);
                        try {
                            job.call();
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
        }
        return null;
    }
}
