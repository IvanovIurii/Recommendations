package com.hse.recommendationsystem.infrastructure.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import com.hse.recommendationsystem.application.SupplierRecommendationNotificationPublisher
import com.hse.recommendationsystem.application.dto.SupplierRecommendationNotificationPayload
import com.hse.recommendationsystem.infrastructure.config.MessagingProperties
import io.awspring.cloud.sns.core.SnsTemplate
import org.slf4j.LoggerFactory

class SupplierRecommendationSnsPublisher(
    private val snsTemplate: SnsTemplate,
    private val objectMapper: ObjectMapper,
    private val messagingProperties: MessagingProperties,
) : SupplierRecommendationNotificationPublisher {

    override fun publish(payload: SupplierRecommendationNotificationPayload) {
        val topicArn = messagingProperties.snsTopicArn
        if (topicArn.isBlank()) {
            logger.error("app.messaging.sns-topic-arn is not set; skip SNS publish for notificationId={}", payload.notificationId)
            return
        }
        val json = objectMapper.writeValueAsString(payload)
        logger.info("Publishing SupplierRecommendation event to SNS topicArn={} notificationId={}", topicArn, payload.notificationId)
        snsTemplate.sendNotification(topicArn, json, SNS_SUBJECT)
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(SupplierRecommendationSnsPublisher::class.java)
        private const val SNS_SUBJECT = "SupplierRecommendationNotificationRequested"
    }
}
