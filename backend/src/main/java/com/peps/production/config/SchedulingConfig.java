package com.peps.production.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration for enabling scheduled tasks
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
    // Scheduling is enabled via @EnableScheduling annotation
}