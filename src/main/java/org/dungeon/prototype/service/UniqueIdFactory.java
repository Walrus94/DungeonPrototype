package org.dungeon.prototype.service;

import java.util.concurrent.atomic.AtomicLong;

public class UniqueIdFactory {
    private static final UniqueIdFactory instance = new UniqueIdFactory();
    private final AtomicLong counter;

    private UniqueIdFactory() {
        counter = new AtomicLong();
    }

    public static UniqueIdFactory getInstance() {
        return instance;
    }

    public long getNextId() {
        return counter.incrementAndGet();
    }
}
