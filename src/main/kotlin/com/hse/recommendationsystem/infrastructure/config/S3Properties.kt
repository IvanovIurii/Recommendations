package com.hse.recommendationsystem.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.s3")
data class S3Properties(
    val endpoint: String,
    val accessKey: String,
    val secretKey: String,
    val region: String = "us-east-1",
    val datasetBucket: String = "datasets",
    val modelBucket: String = "models",
)
