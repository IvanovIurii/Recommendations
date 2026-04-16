package com.hse.recommendationsystem.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.api-client.recommendations-api")
data class RecommendationsApiProperties(
    /** Base URL of the recommendations API, e.g. http://localhost:8081/recommendations */
    val url: String,
)
