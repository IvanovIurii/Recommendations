package com.hse.recommendationsystem.infrastructure.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.hse.recommendationsystem.application.SupplierRecommendationNotificationPublisher
import com.hse.recommendationsystem.infrastructure.messaging.SupplierRecommendationSnsPublisher
import io.awspring.cloud.sns.core.SnsTemplate
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MessagingConfiguration {

    @Bean
    fun supplierRecommendationNotificationPublisher(
        messagingProperties: MessagingProperties,
        objectMapper: ObjectMapper,
        snsTemplate: ObjectProvider<SnsTemplate>,
    ): SupplierRecommendationNotificationPublisher {
        if (!messagingProperties.enabled) {
            return SupplierRecommendationNotificationPublisher { }
        }
        val template = snsTemplate.getIfAvailable()
            ?: throw IllegalStateException("SnsTemplate bean missing while app.messaging.enabled=true")
        return SupplierRecommendationSnsPublisher(template, objectMapper, messagingProperties)
    }
}
