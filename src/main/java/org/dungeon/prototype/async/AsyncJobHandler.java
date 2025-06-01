package org.dungeon.prototype.async;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.async.metrics.TaskMetrics;
import org.dungeon.prototype.exception.DungeonPrototypeException;
import org.dungeon.prototype.model.document.item.ItemType;
import org.dungeon.prototype.model.level.Level;
import org.dungeon.prototype.model.level.generation.GeneratedCluster;
import org.dungeon.prototype.model.level.ui.GridSection;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Service
@Slf4j
public class AsyncJobHandler {

    private final AsyncTaskExecutor asyncTaskExecutor;
    private final CompletionService<GeneratedCluster> asyncTaskCompletionService;
    private final Map<Long, ChatConcurrentState> chatConcurrentStateMap;
    private final TaskMetrics taskMetrics;

    public AsyncJobHandler(@Qualifier(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME)
                           AsyncTaskExecutor asyncTaskExecutor, TaskMetrics taskMetrics) {
        this.asyncTaskExecutor = asyncTaskExecutor;
        this.asyncTaskCompletionService = new ExecutorCompletionService<>(asyncTaskExecutor);
        this.chatConcurrentStateMap = new ConcurrentHashMap<>();
        this.taskMetrics = taskMetrics;
    }

    @Async
    public void submitItemGenerationTask(Runnable job, TaskType taskType, long chatId) {
        log.debug("Submitting item generation {} task for chatId: {}", taskType, chatId);
        asyncTaskExecutor.submit(() -> {
            chatConcurrentStateMap.computeIfAbsent(chatId,
                    k -> new ChatConcurrentState(chatId, new CountDownLatch(ItemType.values().length - 1)));
            try {
                job.run();
                log.info("Counting down ({}) latch for chatId: {}", chatConcurrentStateMap.get(chatId).getLatch().getCount(), chatId);
                chatConcurrentStateMap.get(chatId).getLatch().countDown();
            } catch (Exception e) {
                throw new DungeonPrototypeException(e.getMessage());
            }
        });
    }

    @Async
    public void submitEffectGenerationTask(Runnable job, TaskType taskType, long chatId) {
        log.debug("Submitting effect generation {} task for chatId: {}", taskType, chatId);
        asyncTaskExecutor.submit(() -> {
            try {
                while (!chatConcurrentStateMap.containsKey(chatId) || isNull(chatConcurrentStateMap.get(chatId).getLatch())) {
                    log.info("Waiting for latch to be created for chatId: {}", chatId);
                    Thread.sleep(1000);
                }
                while (chatConcurrentStateMap.get(chatId).getLatch().getCount() > 1) {
                    log.info("Waiting for vanilla items to generate for chatId: {}", chatId);
                    try {
                        chatConcurrentStateMap.get(chatId).getLatch().await();
                    } catch (InterruptedException e) {
                        throw new DungeonPrototypeException(e.getMessage());
                    }
                    log.info("Counting down ({}) latch for chatId: {}", chatConcurrentStateMap.get(chatId).getLatch().getCount(), chatId);
                    chatConcurrentStateMap.get(chatId).getLatch().countDown();
                }
                job.run();
            } catch (Exception e) {
                throw new DungeonPrototypeException(e.getMessage());
            } finally {
            }
        });
    }

    @Async
    public void executeMapGenerationTask(Callable<GridSection[][]> job, TaskType taskType, long chatId, long clusterId) {
        log.debug("Submitting map generation task of type {} for chatId: {}", taskType, chatId);
        asyncTaskCompletionService.submit(() -> new GeneratedCluster(chatId, clusterId, job.call()));
    }

    @Scheduled(fixedRate = 1000)
    public void updateMapGenerationResults() {
        updateMapGenerationResultsAsync();
    }

    @Async
    public void updateMapGenerationResultsAsync() {
        try {
            val result = asyncTaskCompletionService.take().get(10, TimeUnit.SECONDS);
            if (chatConcurrentStateMap.containsKey(result.chatId())) {
                chatConcurrentStateMap.get(result.chatId()).offerGridSection(result);
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.warn("Error while waiting for map generation: {}", e.getMessage());
        }
    }


    @Async
    public CompletableFuture<GeneratedCluster> retrieveMapGenerationResults(long chatId) {
        if (chatConcurrentStateMap.containsKey(chatId) && nonNull(chatConcurrentStateMap.get(chatId).getGridSectionsQueue()) &&
        !chatConcurrentStateMap.get(chatId).getGridSectionsQueue().isEmpty()) {
            val queue = chatConcurrentStateMap.get(chatId).getGridSectionsQueue();
            try {
                return CompletableFuture.completedFuture(queue.poll(10, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                log.warn("Unable while retrieving map generation results for chatId: {}: {} ", chatId, e.getMessage());
            }
        }
        log.debug("Map generation results not available for chatId: {}", chatId);
        return CompletableFuture.completedFuture(null);
    }

    @Async
    public Future<Level> submitMapPopulationTask(Callable<Level> job, TaskType taskType, long chatId) {
        log.debug("Submitting task of type {} for chatId: {}", taskType, chatId);
        return asyncTaskExecutor.submit(() -> {
            try {
                while (!chatConcurrentStateMap.containsKey(chatId)) {
                    log.info("Waiting for latch to be created for chatId: {}", chatId);
                    Thread.sleep(1000);
                }
                while (chatConcurrentStateMap.get(chatId).getLatch().getCount() > 0) {
                    chatConcurrentStateMap.get(chatId).getLatch().await();
                }
                return job.call();
            } catch (InterruptedException e) {
                throw new DungeonPrototypeException(e.getMessage());
            }
        });
    }

    @Async
    public void submitTask(Runnable job, TaskType taskType, long chatId) {
        log.debug("Submitting task of type {} for chatId: {}", taskType, chatId);
        asyncTaskExecutor.submit(() -> {
            try {
                while (!chatConcurrentStateMap.containsKey(chatId)) {
                    log.info("Waiting for latch to be created for chatId: {}", chatId);
                    Thread.sleep(1000);
                }
                while (chatConcurrentStateMap.get(chatId).getLatch().getCount() > 0) {
                    chatConcurrentStateMap.get(chatId).getLatch().await();
                }
                job.run();
            } catch (InterruptedException e) {
                throw new DungeonPrototypeException(e.getMessage());
            }
        });
    }

    @Async
    public void submitCallbackTask(Runnable task) {
        //TODO: temporary unused
        log.debug("Submitting callback task");
        asyncTaskExecutor.submit(() -> {
            try {
                task.run();
            } catch (Exception e) {
                throw new DungeonPrototypeException(e.getMessage());
            }
        });
    }

    @Async
    public void clearLatch(long chatId) {
        log.info("Clearing latch for chatId: {}", chatId);
        chatConcurrentStateMap.get(chatId).setLatch(null);
    }
}
