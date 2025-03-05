package org.dungeon.prototype.async;

import org.springframework.scheduling.annotation.Async;

import java.util.Optional;
import java.util.concurrent.Future;

public interface AsyncJobService {
    @Async
    Future<?> submitTask(Runnable job, TaskType taskType, long chatId, Optional<Long> clusterId);

    @Async
    void awaitPhaser(long chatId);

    @Async
    void deregisterPhaser(long chatId);
}
