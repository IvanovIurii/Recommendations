package com.hse.recommendationsystem.infrastructure.clients

import com.hse.recommendationsystem.infrastructure.config.RecommendationsApiProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
@EnableConfigurationProperties(RecommendationsApiProperties::class)
class RecommendationsServiceClientConfig(
    private val recommendationsApiProperties: RecommendationsApiProperties,
) {
    @Bean
    fun recommendationsServiceRestClient(builder: RestClient.Builder): RestClient =
        builder
            .baseUrl(recommendationsApiProperties.url.trimEnd('/'))
            .build()
}
