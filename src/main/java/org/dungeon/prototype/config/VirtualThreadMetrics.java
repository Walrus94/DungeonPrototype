package org.dungeon.prototype.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class VirtualThreadMetrics {

    public VirtualThreadMetrics(MeterRegistry meterRegistry) {
        // Register a gauge to track total number of threads
        Gauge.builder("jvm.threads.total", Thread::activeCount)
                .description("Total number of active threads, including virtual threads")
                .register(meterRegistry);

        // Register a gauge to track the number of virtual threads
        Gauge.builder("jvm.threads.virtual", this::countVirtualThreads)
                .description("Total number of active virtual threads")
                .register(meterRegistry);

        // Gauge for carrier threads
        Gauge.builder("jvm.threads.carrier", this::countCarrierThreads)
                .description("The number of carrier threads used by virtual threads")
                .register(meterRegistry);
    }

    /**
     * Counts the number of virtual threads currently active in the JVM.
     */
    private long countVirtualThreads() {
        return Thread.getAllStackTraces().keySet().stream()
                .filter(Thread::isVirtual) // Check if the thread is virtual
                .count();
    }
    /**
     * Counts the number of carrier threads currently active in the JVM.
     */
    private long countCarrierThreads() {
        return Thread.getAllStackTraces().keySet().stream()
                .filter(thread -> !thread.isVirtual()) // Exclude virtual threads
                .count();
    }
}


