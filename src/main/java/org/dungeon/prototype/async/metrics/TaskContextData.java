package org.dungeon.prototype.async.metrics;

import org.dungeon.prototype.async.TaskType;

/**
 * Basic context data for any chat related task.
 */
public interface TaskContextData {

    long chatId();

    TaskType taskType();
}
