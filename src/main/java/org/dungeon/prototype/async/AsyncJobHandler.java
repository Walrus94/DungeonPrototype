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
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
                    k -> new ChatConcurrentState(chatId, new CountDownLatch(ItemType.values().length)));//TODO: increment when Usable items generation is implemented
            try {
                job.run();
            } catch (Exception e) {
                throw new DungeonPrototypeException(e.getMessage());
            } finally {
                log.info("Counting down ({}) latch for chatId: {}", chatConcurrentStateMap.get(chatId).getLatch().getCount(), chatId);
                chatConcurrentStateMap.get(chatId).getLatch().countDown();
            }
        });
    }

    @Async
    public void submitEffectGenerationTask(Runnable job, TaskType taskType, long chatId) {
        log.debug("Submitting effect generation {} task for chatId: {}", taskType, chatId);
        asyncTaskExecutor.submit(() -> {
            try {
                while (!chatConcurrentStateMap.containsKey(chatId)) {
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
                }
                job.run();
            } catch (Exception e) {
                throw new DungeonPrototypeException(e.getMessage());
            } finally {
                chatConcurrentStateMap.get(chatId).getLatch().countDown();
            }
        });
    }

    public void executeMapGenerationTask(Callable<GridSection[][]> job, TaskType taskType, long chatId, long clusterId) throws InterruptedException {
        asyncTaskCompletionService.submit(() -> {
            log.debug("Submitting map generation task of type {} for chatId: {}", taskType, chatId);
            return new GeneratedCluster(chatId, clusterId, job.call());
        });
    }

    @Async
    @Scheduled(fixedRate = 1000)
    public void updateMapGenerationResults() {
        try {
            val result = asyncTaskCompletionService.take().get(10, TimeUnit.SECONDS);
            if (chatConcurrentStateMap.containsKey(result.chatId())) {
                chatConcurrentStateMap.get(result.chatId()).getGridSectionsQueue().put(result);
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("Error while waiting for map generation: ", e);
            throw new DungeonPrototypeException(e.getMessage());
        }
    }

    @Async
    public Optional<GeneratedCluster> retrieveMapGenerationResults(long chatId) {
        if (chatConcurrentStateMap.containsKey(chatId)) {
            val queue = chatConcurrentStateMap.get(chatId).getGridSectionsQueue();
            try {
                return Optional.ofNullable(queue.poll(10, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                log.error("Error while retrieving map generation results: ", e);
                throw new DungeonPrototypeException(e.getMessage());
            }
        }
        return Optional.empty();
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
            } finally {
                chatConcurrentStateMap.remove(chatId);
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
            } finally {
                chatConcurrentStateMap.remove(chatId);
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

    public void initializeMapClusterQueue(long chatId, int size) {
        if (chatConcurrentStateMap.containsKey(chatId)) {
            chatConcurrentStateMap.get(chatId).setGridSectionsQueue(new LinkedBlockingQueue<>(size));
        }
    }
}
