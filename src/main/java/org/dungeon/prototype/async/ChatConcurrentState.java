package org.dungeon.prototype.async;

import lombok.Data;
import org.dungeon.prototype.model.level.generation.GeneratedCluster;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

@Data
public class ChatConcurrentState {
    private long chatId;
    private CountDownLatch latch;
    private BlockingQueue<GeneratedCluster> gridSectionsQueue;

    public ChatConcurrentState(long chatId, CountDownLatch latch) {
        this.chatId = chatId;
        this.latch = latch;
        this.gridSectionsQueue = new LinkedBlockingQueue<>();
    }
}
