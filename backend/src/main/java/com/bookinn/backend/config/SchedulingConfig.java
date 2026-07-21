package com.bookinn.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on Spring's scheduled-task support so {@code @Scheduled} jobs run. */
@Configuration
@EnableScheduling
public class SchedulingConfig {}
