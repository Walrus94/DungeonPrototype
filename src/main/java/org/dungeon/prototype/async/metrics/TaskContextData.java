package org.dungeon.prototype.async.metrics;

import org.dungeon.prototype.async.TaskType;


public record TaskContextData (long chatId, long clusterId, TaskType taskType) {

}

