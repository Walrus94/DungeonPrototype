package org.dungeon.prototype.async.scoped;

import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.dungeon.prototype.async.TaskType;
import org.dungeon.prototype.async.metrics.TaskContext;
import org.dungeon.prototype.async.metrics.TaskContextData;
import org.dungeon.prototype.async.metrics.BasicTaskContextData;
import org.dungeon.prototype.async.metrics.ClusterTaskContextData;
import org.dungeon.prototype.async.metrics.TaskMetrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.CountDownLatch;
import java.lang.ScopedValue;
import org.dungeon.prototype.async.ChatConcurrentState;

/**
 * Manages StructuredTaskScope instances per chat and
 * allows interrupting all tasks associated with a chat.
 */
@Slf4j
public class ChatTaskManager {

    private final Map<Long, ChatTaskScope> scopes = new ConcurrentHashMap<>();
    private final Map<Long, ChatConcurrentState> chatStates = new ConcurrentHashMap<>();
    private final TaskMetrics taskMetrics;

    public ChatTaskManager(TaskMetrics taskMetrics) {
        this.taskMetrics = taskMetrics;
    }

    /**
     * Opens a new scope for a chat. If a scope already exists it is returned.
     */
    public ChatTaskScope openScope(long chatId) {
        return scopes.computeIfAbsent(chatId, id -> new ChatTaskScope(id, taskMetrics));
    }

    /**
     * Cancel all running tasks for provided chat and close its scope.
     */
    public void cancelScope(long chatId) {
        var scope = scopes.remove(chatId);
        if (scope != null) {
            scope.shutdownRecursive();
            try {
                scope.joinRecursive();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            scope.closeRecursive();

            var state = chatStates.remove(chatId);
            if (state != null) {
                var latch = state.getLatch();
                if (latch != null) {
                    while (latch.getCount() > 0) {
                        latch.countDown();
                    }
                }
                if (state.getGridSectionsQueue() != null) {
                    state.getGridSectionsQueue().clear();
                }
            }
        }
        removeChatState(chatId);
    }

    public ChatConcurrentState getOrCreateChatState(long chatId, int latchCount) {
        return chatStates.computeIfAbsent(chatId,
                id -> new ChatConcurrentState(chatId, new CountDownLatch(latchCount)));
    }

    public ChatConcurrentState getChatState(long chatId) {
        return chatStates.get(chatId);
    }

    public void removeChatState(long chatId) {
        var state = chatStates.remove(chatId);
        if (state != null && state.getGridSectionsQueue() != null) {
            state.getGridSectionsQueue().clear();
        }
    }

    /**
     * StructuredTaskScope wrapper storing task information.
     */
    public static class ChatTaskScope extends StructuredTaskScope<Object> {
        private final long chatId;
        private final TaskMetrics metrics;
        private final Map<TaskType, List<Subtask<?>>> tasks = new ConcurrentHashMap<>();
        private final Map<Subtask<?>, SubtaskMeta> metadata = new ConcurrentHashMap<>();
        private final List<ChatTaskScope> childScopes = new CopyOnWriteArrayList<>();

        ChatTaskScope(long chatId, TaskMetrics metrics) {
            this.chatId = chatId;
            this.metrics = metrics;
        }

        public long chatId() {
            return chatId;
        }

        public ChatTaskScope openSubScope() {
            var child = new ChatTaskScope(chatId, metrics);
            childScopes.add(child);
            return child;
        }

        public void shutdownRecursive() {
            shutdown();
            childScopes.forEach(ChatTaskScope::shutdownRecursive);
        }

        public void joinRecursive() throws InterruptedException {
            join();
            for (ChatTaskScope child : childScopes) {
                child.joinRecursive();
            }
        }

        public void closeRecursive() {
            close();
            childScopes.forEach(ChatTaskScope::closeRecursive);
        }

        /**
         * Forks a task and registers it under provided task type.
         */
        public <T> Subtask<T> forkTask(TaskType type, Callable<T> callable) {
            var context = new BasicTaskContextData(chatId, type);
            return forkSubTask(type, callable, context);
        }

        public <T> Subtask<T> forkTask(TaskType type, long clusterId, Callable<T> callable) {
            var context = new ClusterTaskContextData(chatId, clusterId, type);
            return forkSubTask(type, callable, context);
        }

        public Map<TaskType, List<Subtask<?>>> tasks() {
            return tasks;
        }

        public <T> T getResult(Subtask<T> subtask){
            var meta = metadata.remove(subtask);
            try {
                var result = subtask.get();
                meta.sample().stop(metrics.getTaskTimer(meta.context().taskType().name()));
                metrics.getTaskCounter(meta.context().taskType().name()).increment();
                return result;
            } catch (Exception e) {
                meta.sample().stop(metrics.getTaskTimer(meta.context().taskType().name()));
                metrics.getTaskFailureCounter(meta.context().taskType().name()).increment();
                throw e;
            } finally {
                metrics.removeCompletedTask(meta.context());
            }
        }

        private <T> Subtask<T> forkSubTask(TaskType type, Callable<T> callable, TaskContextData context) {
            metrics.addActiveTask(context);
            var sample = Timer.start(metrics.getMeterRegistry());
            var subtask = super.fork(() ->
                    ScopedValue.where(TaskContext.CONTEXT, context).call(callable));
            tasks.computeIfAbsent(type, k -> new ArrayList<>()).add(subtask);
            metadata.put(subtask, new SubtaskMeta(context, sample));
            return subtask;
        }

        private record SubtaskMeta(TaskContextData context, Timer.Sample sample) {}
    }
}
