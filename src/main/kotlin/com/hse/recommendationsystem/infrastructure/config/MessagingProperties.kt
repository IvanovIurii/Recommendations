package com.hse.recommendationsystem.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.messaging")
data class MessagingProperties(
    val enabled: Boolean,
    val snsTopicArn: String,
    val cnsFeedbackQueueName: String,
    val modelSyncQueueName: String = "model-sync-events",
)
