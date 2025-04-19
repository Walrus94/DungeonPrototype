package org.dungeon.prototype.async.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TaskMetrics {

    private final Set<TaskContextData> activeTasks = ConcurrentHashMap.newKeySet();

    private final MeterRegistry meterRegistry;

    public TaskMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        Gauge.builder("active_tasks", activeTasks, Set::size)
                .description("Number of active tasks")
                .register(meterRegistry);
    }

    // Timer to measure task execution time
    public Timer getTaskTimer(String taskType) {
        return meterRegistry.timer("game_task_execution_time", "taskType", taskType);
    }

    // Counter for completed tasks
    public Counter getTaskCounter(String taskType) {
        return meterRegistry.counter("game_task_completed", "taskType", taskType);
    }

    // Counter for failed tasks
    public Counter getTaskFailureCounter(String taskType) {
        return meterRegistry.counter("game_task_failed", "taskType", taskType);
    }

    // Track active tasks per chatId
    public void addActiveTask(TaskContextData context) {
        activeTasks.add(context);
    }

    public void removeCompletedTask(TaskContextData context) {
        activeTasks.remove(context);
    }

}
