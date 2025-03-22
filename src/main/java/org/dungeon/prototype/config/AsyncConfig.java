package org.dungeon.prototype.config;

import com.github.marschall.micrometer.jfr.JfrMeterRegistry;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import org.dungeon.prototype.async.metrics.TaskMetrics;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor;


@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME)
    public AsyncTaskExecutor asyncTaskExecutor(MeterRegistry meterRegistry) {
        ExecutorService executorService = newVirtualThreadPerTaskExecutor();
        ExecutorServiceMetrics.monitor(meterRegistry, executorService, "virtual_threads");
        return new TaskExecutorAdapter(executorService);
    }

    @Bean
    public TomcatProtocolHandlerCustomizer<?> protocolHandlerVirtualThreadExecutorCustomizer() {
        return protocolHandler -> protocolHandler.setExecutor(newVirtualThreadPerTaskExecutor());
    }

    @Bean
    public TaskMetrics taskMetrics(MeterRegistry meterRegistry) {
        return new TaskMetrics(meterRegistry);
    }

    @Bean
    public MeterRegistry meterRegistry() {
        MeterRegistry registry = new JfrMeterRegistry();

        registry.config().commonTags("application", "dungeon-prototype");
        AtomicInteger activeThreads = new AtomicInteger(Thread.activeCount());
        Gauge.builder("jfr_virtual_threads", activeThreads, AtomicInteger::get)
                .description("The number of virtual threads in the JVM")
                .register(registry);
        return registry;
    }
}
