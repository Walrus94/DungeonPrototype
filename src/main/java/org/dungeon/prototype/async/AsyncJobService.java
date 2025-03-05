package org.dungeon.prototype.async;

import org.springframework.scheduling.annotation.Async;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public interface AsyncJobService {
    @Async
    <T> Future<T> submitTask(Callable<T> job, TaskType taskType, long chatId);

    @Async
    Future<?> submitTask(Runnable job, TaskType taskType, long chatId, long clusterId);

    @Async
    Future<?> submitTask(Runnable job, TaskType taskType, long chatId);
}
