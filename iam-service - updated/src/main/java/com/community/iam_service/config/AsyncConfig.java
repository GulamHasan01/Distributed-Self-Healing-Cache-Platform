package com.community.iam_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 *  NEW: Enables @Async so AuditLogService writes don't block main request thread.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}