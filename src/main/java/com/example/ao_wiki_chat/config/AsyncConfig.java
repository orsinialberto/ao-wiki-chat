package com.example.ao_wiki_chat.config;

import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configuration for asynchronous processing of documents.
 * Enables async execution and configures thread pool for document processing pipeline.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Value("${spring.task.execution.pool.core-size:4}")
    private int corePoolSize;

    @Value("${spring.task.execution.pool.max-size:8}")
    private int maxPoolSize;

    @Value("${spring.task.execution.pool.queue-capacity:100}")
    private int queueCapacity;

    @Value("${spring.task.execution.thread-name-prefix:doc-processor-}")
    private String threadNamePrefix;

    /**
     * Configures thread pool executor for document processing tasks.
     * Uses values from application.yml or defaults.
     *
     * @return configured ThreadPoolTaskExecutor
     */
    @Bean(name = "documentProcessingExecutor")
    public Executor documentProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}

