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
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Service
@Slf4j
public class AsyncJobHandler {

    private final AsyncTaskExecutor asyncTaskExecutor;
    private final CompletionService<GeneratedCluster> asyncTaskCompletionService;
    private final Map<Long, ChatConcurrentState> chatConcurrentStateMap;
    private final TaskMetrics taskMetrics;
    private Thread completionConsumerThread;
    private volatile boolean consumerRunning;

    public AsyncJobHandler(@Qualifier(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME)
                           AsyncTaskExecutor asyncTaskExecutor, TaskMetrics taskMetrics) {
        this.asyncTaskExecutor = asyncTaskExecutor;
        this.asyncTaskCompletionService = new ExecutorCompletionService<>(asyncTaskExecutor);
        this.chatConcurrentStateMap = new ConcurrentHashMap<>();
        this.taskMetrics = taskMetrics;
    }

    @PostConstruct
    private void startCompletionConsumer() {
        consumerRunning = true;
        completionConsumerThread = Thread.ofVirtual().name("map-generation-consumer").start(this::consumeCompletionResults);
    }

    @PreDestroy
    private void stopCompletionConsumer() {
        consumerRunning = false;
        if (completionConsumerThread != null) {
            completionConsumerThread.interrupt();
        }
    }

    @Async
    public void submitItemGenerationTask(Runnable job, TaskType taskType, long chatId) {
        log.debug("Submitting item generation {} task for chatId: {}", taskType, chatId);
        asyncTaskExecutor.submit(() -> {
            chatConcurrentStateMap.computeIfAbsent(chatId,
                    k -> new ChatConcurrentState(chatId, new CountDownLatch(ItemType.values().length)));
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
                while (!chatConcurrentStateMap.containsKey(chatId) ||
                        isNull(chatConcurrentStateMap.get(chatId).getLatch())) {
                    log.info("Waiting for latch to be created for chatId: {}", chatId);
                    Thread.sleep(1000);
                }
                while (chatConcurrentStateMap.get(chatId).getLatch().getCount() > 1) {
                    log.info("Waiting for vanilla items to generate for chatId: {}", chatId);
                    Thread.sleep(1000);
                }
                job.run();
                chatConcurrentStateMap.get(chatId).getLatch().countDown();
            } catch (Exception e) {
                throw new DungeonPrototypeException(e.getMessage());
            }
        });
    }

    @Async
    public void executeMapGenerationTask(Callable<GridSection[][]> job, TaskType taskType, long chatId, long clusterId) {
        log.debug("Submitting map generation task of type {} for chatId: {}", taskType, chatId);
        asyncTaskCompletionService.submit(() -> new GeneratedCluster(chatId, clusterId, job.call()));
    }

    private void consumeCompletionResults() {
        while (consumerRunning && !Thread.currentThread().isInterrupted()) {
            try {
                val result = asyncTaskCompletionService.take().get();
                if (chatConcurrentStateMap.containsKey(result.chatId())) {
                    chatConcurrentStateMap.get(result.chatId()).offerGridSection(result);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                log.warn("Error while waiting for map generation: {}", e.getMessage());
            }
        }
    }

    @Async
    public Future<Level> submitMapPopulationTask(Callable<Level> job, TaskType taskType, long chatId) {
        log.debug("Submitting task of type {} for chatId: {}", taskType, chatId);
        return asyncTaskExecutor.submit(() -> {
            try {
                while (!chatConcurrentStateMap.containsKey(chatId) ||
                        isNull(chatConcurrentStateMap.get(chatId).getLatch())) {
                    log.info("Waiting for latch to be created for chatId: {}", chatId);
                    Thread.sleep(1000);
                }
                chatConcurrentStateMap.get(chatId).getLatch().await();
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
                while (!chatConcurrentStateMap.containsKey(chatId) ||
                        isNull(chatConcurrentStateMap.get(chatId).getLatch())) {
                    log.info("Waiting for latch to be created for chatId: {}", chatId);
                    Thread.sleep(1000);
                }
                chatConcurrentStateMap.get(chatId).getLatch().await();
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

    public void removeChatState(long chatId) {
        log.info("Removing chat state for chatId: {}", chatId);
        if (chatConcurrentStateMap.containsKey(chatId)) {
            val state = chatConcurrentStateMap.remove(chatId);
            if (nonNull(state.getGridSectionsQueue())) {
                state.getGridSectionsQueue().clear();
            }
        }
    }
}
