package com.hse.recommendationsystem.infrastructure.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient

@Configuration
class InferenceServiceClientConfig(
    private val inferenceServiceProperties: InferenceServiceProperties,
) {
    @Bean
    fun inferenceServiceRestClient(builder: RestClient.Builder): RestClient =
        builder
            .baseUrl(inferenceServiceProperties.url.trimEnd('/'))
            .requestFactory(SimpleClientHttpRequestFactory())
            .build()
}
