package com.hse.recommendationsystem.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.offline")
data class OfflinePipelineProperties(
    val enabled: Boolean = false,
    val scheduleDelayMs: Long = 300_000,
)
