package org.dungeon.prototype.config;

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

import static java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor;


@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME)
    public AsyncTaskExecutor asyncTaskExecutor(MeterRegistry meterRegistry) {
        ExecutorService executorService = newVirtualThreadPerTaskExecutor();
        ExecutorServiceMetrics.monitor(meterRegistry, executorService, "dungeon_task_executor");
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
}
