package org.dungeon.prototype.async;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.async.metrics.TaskMetrics;
import org.dungeon.prototype.async.scoped.ChatTaskManager;
import org.dungeon.prototype.exception.DungeonPrototypeException;
import org.dungeon.prototype.exception.ItemGenerationException;
import org.dungeon.prototype.model.document.item.ItemType;
import org.dungeon.prototype.model.level.Level;
import org.dungeon.prototype.model.level.generation.GeneratedCluster;
import org.dungeon.prototype.model.level.ui.GridSection;
import org.dungeon.prototype.properties.CallbackType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.Async;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Service
@Slf4j
public class AsyncJobHandler {

    private static final int ITEM_GENERATION_RETRIES = 3;

    private final AsyncTaskExecutor asyncTaskExecutor;
    private final Map<Long, ChatConcurrentState> chatConcurrentStateMap;
    private final TaskMetrics taskMetrics;
    private final ChatTaskManager chatTaskManager;

    public AsyncJobHandler(@Qualifier(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME)
                           AsyncTaskExecutor asyncTaskExecutor,
                           TaskMetrics taskMetrics,
                           ChatTaskManager chatTaskManager) {
        this.asyncTaskExecutor = asyncTaskExecutor;
        this.chatConcurrentStateMap = new ConcurrentHashMap<>();
        this.taskMetrics = taskMetrics;
        this.chatTaskManager = chatTaskManager;
    }

    public ChatTaskManager.ChatTaskScope openClusterScope(long chatId) {
        return chatTaskManager.openScope(chatId).openSubScope();
    }


    @Async
    public void submitItemGenerationTask(Runnable job, TaskType taskType, long chatId) {
        log.debug("Submitting item generation {} task for chatId: {}", taskType, chatId);
        var scope = chatTaskManager.openScope(chatId);
        scope.forkTask(taskType, () -> {
            chatConcurrentStateMap.computeIfAbsent(chatId,
                    k -> new ChatConcurrentState(chatId, new CountDownLatch(ItemType.values().length)));
            int attempt = 1;
            Exception lastException = null;
            while (attempt <= ITEM_GENERATION_RETRIES) {
                try {
                    job.run();
                    log.info("Counting down ({}) latch for chatId: {}",
                            chatConcurrentStateMap.get(chatId).getLatch().getCount(), chatId);
                    chatConcurrentStateMap.get(chatId).getLatch().countDown();
                    return null;
                } catch (Exception e) {
                    lastException = e;
                    if (attempt < ITEM_GENERATION_RETRIES) {
                        log.warn(
                                "Item generation task {} failed for chatId {}, retrying ({} / {})",
                                taskType, chatId, attempt, ITEM_GENERATION_RETRIES);
                    } else {
                        log.error(
                                "Item generation task {} failed for chatId {} after {} attempts: {}",
                                taskType, chatId, ITEM_GENERATION_RETRIES, e.getMessage());
                    }
                    attempt++;
                }
            }
            throw new ItemGenerationException(chatId, lastException.getMessage(), CallbackType.MENU_BACK);
        });
    }

    @Async
    public void submitEffectGenerationTask(Runnable job, TaskType taskType, long chatId) {
        log.debug("Submitting effect generation {} task for chatId: {}", taskType, chatId);
        var scope = chatTaskManager.openScope(chatId);
        scope.forkTask(taskType, () -> {
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
                log.warn("Effect generation task {} failed for chatId {}: {}", taskType, chatId, e.getMessage());
            }
            return null;
        });
    }

    public ChatTaskManager.ChatTaskScope.Subtask<GeneratedCluster> submitClusterGenerationTask(
            ChatTaskManager.ChatTaskScope clusterScope,
            Callable<GridSection[][]> job,
            TaskType taskType,
            long chatId,
            long clusterId) {
        log.debug("Submitting map generation task of type {} for chatId: {}", taskType, chatId);
        return clusterScope.forkTask(taskType, clusterId,
                () -> new GeneratedCluster(chatId, clusterId, job.call()));
    }

    @Async
    public Future<Level> submitMapPopulationTask(Callable<Level> job, TaskType taskType, long chatId) {
        log.debug("Submitting task of type {} for chatId: {}", taskType, chatId);
        return asyncTaskExecutor.submit(() -> {
            var scope = chatTaskManager.openScope(chatId).openSubScope();
            var subtask = scope.forkTask(taskType, () -> {
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
            try {
                scope.join();
                return scope.getResult(subtask);
            } finally {
                scope.close();
            }
        });
    }

    @Async
    public void submitTask(Runnable job, TaskType taskType, long chatId) {
        log.debug("Submitting task of type {} for chatId: {}", taskType, chatId);
        var scope = chatTaskManager.openScope(chatId);
        scope.forkTask(taskType, () -> {
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
            return null;
        });
    }

    @Async
    public void submitCallbackTask(Runnable task) {
        //TODO: temporary unused
        log.debug("Submitting callback task");
        var scope = chatTaskManager.openScope(-1);
        scope.forkTask(TaskType.EFFECTS_GENERATION, () -> { //task type not used
            try {
                task.run();
            } catch (Exception e) {
                throw new DungeonPrototypeException(e.getMessage());
            }
            return null;
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
