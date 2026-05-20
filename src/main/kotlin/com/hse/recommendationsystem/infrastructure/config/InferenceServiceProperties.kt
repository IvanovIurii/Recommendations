package com.hse.recommendationsystem.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.api-client.inference-service")
data class InferenceServiceProperties(
    val url: String,
)
