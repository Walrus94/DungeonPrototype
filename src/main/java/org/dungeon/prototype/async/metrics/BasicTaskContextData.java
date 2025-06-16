package org.dungeon.prototype.async.metrics;

import org.dungeon.prototype.async.TaskType;

/**
 * Default task context without cluster information.
 */
public record BasicTaskContextData(long chatId, TaskType taskType) implements TaskContextData {
}
