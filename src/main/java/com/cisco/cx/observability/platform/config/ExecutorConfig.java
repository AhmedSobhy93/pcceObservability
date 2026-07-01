package com.cisco.cx.observability.platform.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ExecutorConfig {

    @Bean(name = "probeExecutor")
    Executor probeExecutor() {
        return executor("probe-", 4, 16, 200);
    }

    @Bean(name = "apiExecutor")
    Executor apiExecutor() {
        return executor("api-", 4, 12, 200);
    }

    @Bean(name = "reportingExecutor")
    Executor reportingExecutor() {
        return executor("reporting-", 2, 8, 100);
    }

    private ThreadPoolTaskExecutor executor(String prefix, int corePoolSize, int maxPoolSize, int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix(prefix);
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        executor.initialize();
        return executor;
    }
}
