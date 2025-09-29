package dev.andreasarf.websocket.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@TestConfiguration
public class AsyncTestConfig {

    @Bean
    public TaskScheduler virtualTaskScheduler() {
        final var scheduler = new ThreadPoolTaskScheduler();
        scheduler.setVirtualThreads(true);
        scheduler.setThreadNamePrefix("vt-scheduler-");
        return scheduler;
    }
}
