package org.dungeon.prototype.async.metrics;

import org.dungeon.prototype.async.TaskType;

/**
 * Context data for level map cluster generation subtasks that require clusterId.
 */
public record ClusterTaskContextData(long chatId, long clusterId, TaskType taskType) implements TaskContextData {
}
