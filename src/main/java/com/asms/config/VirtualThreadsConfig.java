package com.asms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Virtual Threads configuration (Project Loom — Java 25).
 *
 * <p>Enables virtual threads for high-concurrency session and audit workloads
 * per the ticket requirement (AC14). Virtual threads are also activated via
 * JVM flag {@code -XX:+UseVirtualThreads} in the Dockerfile ENTRYPOINT.
 */
@Configuration
public class VirtualThreadsConfig {

    /**
     * Task executor using Java 25 virtual threads for async operations.
     * Used for audit log writes, CSV import processing, and export jobs.
     */
    @Bean(name = "virtualThreadExecutor")
    public Executor virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
