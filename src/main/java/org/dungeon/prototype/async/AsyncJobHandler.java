package org.dungeon.prototype.async;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.async.metrics.TaskContext;
import org.dungeon.prototype.async.metrics.TaskContextData;
import org.dungeon.prototype.async.metrics.TaskMetrics;
import org.dungeon.prototype.exception.DungeonPrototypeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.Phaser;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.TimeUnit;

@Service
@EnableAsync
@Slf4j
public class AsyncJobHandler {

    @Qualifier(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME)
    private AsyncTaskExecutor asyncTaskExecutor;
    private static final Map<Long, Phaser> phasersByChat = new ConcurrentHashMap<>();

    @Autowired
    private TaskMetrics taskMetrics;


    public Future<?> submitTask(Runnable job, TaskType taskType, long chatId, Optional<Long> clusterId) {
        val phaser = getPhaser(chatId);
        phaser.register();

        try (var taskScope = new StructuredTaskScope.ShutdownOnFailure()) {
            return asyncTaskExecutor.submit(() -> {
                var context = new TaskContextData(chatId, clusterId.orElse(-1L), taskType);
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
                                phaser.arriveAndDeregister();
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
                    phaser.arriveAndDeregister();
                    taskMetrics.removeCompletedTask(context);
                }
            });
        }
    }

    public void awaitPhaser(long chatId) {
        phasersByChat.get(chatId).arriveAndAwaitAdvance();
    }

    public void deregisterPhaser(long chatId) {
        phasersByChat.get(chatId).arriveAndDeregister();
    }

    private Phaser getPhaser(long chatId) {
        return phasersByChat.computeIfAbsent(chatId, id -> new Phaser(1));
    }
}
