package org.dungeon.prototype.config;

import com.github.marschall.micrometer.jfr.JfrMeterRegistry;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import org.dungeon.prototype.async.metrics.TaskMetrics;
import org.dungeon.prototype.async.scoped.ChatTaskManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor;


@Configuration
@EnableScheduling
public class AsyncConfig {

    @Value("${spring.profiles.active}")
    private String env;


    @Bean
    public TomcatProtocolHandlerCustomizer<?> protocolHandlerVirtualThreadExecutorCustomizer() {
        return protocolHandler -> protocolHandler.setExecutor(newVirtualThreadPerTaskExecutor());
    }

    @Bean
    public TaskMetrics taskMetrics(MeterRegistry meterRegistry) {
        return new TaskMetrics(meterRegistry);
    }

    @Bean
    public ChatTaskManager chatTaskManager(TaskMetrics metrics) {
        return new ChatTaskManager(metrics);
    }

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> configureCommonTags() {
        return registry -> registry.config()
                .commonTags("environment", env)
                .meterFilter(MeterFilter.ignoreTags("code.function", "main.application.class", "code.namespace"));//TODO: remove when fixed
    }

    @Bean
    public MeterRegistry meterRegistry() {
        return new JfrMeterRegistry();
    }

    @Bean
    public Gauge activeVirtualThreadsGauge(MeterRegistry registry) {
        AtomicInteger activeThreads = new AtomicInteger(Thread.activeCount());
        return Gauge.builder("jfr_virtual_threads", activeThreads, AtomicInteger::get)
                .description("The number of virtual threads in the JVM")
                .register(registry);
    }
}
