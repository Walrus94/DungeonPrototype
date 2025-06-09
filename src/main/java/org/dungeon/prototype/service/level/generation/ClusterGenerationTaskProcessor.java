package org.dungeon.prototype.service.level.generation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dungeon.prototype.async.TaskType;
import org.dungeon.prototype.async.scoped.ChatTaskManager;
import org.dungeon.prototype.model.level.generation.GeneratedCluster;
import org.dungeon.prototype.model.level.generation.LevelGridCluster;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.StructuredTaskScope;
import java.util.function.Function;

/**
 * Executes cluster generation tasks within a {@link ChatTaskManager.ChatTaskScope}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClusterGenerationTaskProcessor {

    private final ChatTaskManager chatTaskManager;

    /**
     * Processes provided clusters using {@link ChatTaskManager} scoped tasks and returns
     * generation results mapped by cluster id.
     */
    public Map<Long, GeneratedCluster> process(long chatId,
                                               Map<Long, LevelGridCluster> clusters,
                                               Function<LevelGridCluster, GeneratedCluster> task) {
        var scope = chatTaskManager.openScope(chatId);
        Map<Long, StructuredTaskScope.Subtask<GeneratedCluster>> tasks = new HashMap<>();
        clusters.values().forEach(cluster ->
                tasks.put(cluster.getId(), scope.forkTask(
                        TaskType.LEVEL_GENERATION,
                        cluster.getId(),
                        () -> task.apply(cluster))));

        try {
            scope.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Cluster generation interrupted for chatId: {}", chatId);
        }

        Map<Long, GeneratedCluster> results = new HashMap<>();
        tasks.forEach((id, subtask) -> {
            try {
                results.put(id, scope.getResult(subtask));
            } catch (Exception e) {
                log.warn("Error while generating cluster result: {}", e.getMessage());
            }
        });
        chatTaskManager.cancelScope(chatId);
        return results;
    }
}
