package com.hse.recommendationsystem.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.recommendations")
data class RecommendationsProperties(
    val queueProcessingDelayMs: Long = 1000,
    val queueBatchSize: Int = 10,
    val queueMaxProcessingAttempts: Int = 3,
)
