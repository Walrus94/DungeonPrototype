package org.dungeon.prototype.model.ui.level;

import java.util.concurrent.atomic.AtomicLong;

public class WalkerUniqueIdFactory {
    private static final WalkerUniqueIdFactory instance = new WalkerUniqueIdFactory();
    private final AtomicLong counter;

    private WalkerUniqueIdFactory() {
        counter = new AtomicLong();
    }

    public static WalkerUniqueIdFactory getInstance() {
        return instance;
    }

    public long getNextId() {
        return counter.incrementAndGet();
    }
}
