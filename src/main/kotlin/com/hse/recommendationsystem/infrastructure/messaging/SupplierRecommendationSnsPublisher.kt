package com.hse.recommendationsystem.infrastructure.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import com.hse.recommendationsystem.domain.messaging.SupplierRecommendationNotificationPayload
import com.hse.recommendationsystem.domain.messaging.SupplierRecommendationNotificationPublisher
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

    companion object {
        private val logger = LoggerFactory.getLogger(SupplierRecommendationSnsPublisher::class.java)
        private const val SNS_SUBJECT = "SupplierRecommendationNotificationRequested"
    }

    // todo: use event type instead of subject
    // enum class NotificationEventType {
    //    RFQ_CREATED,
    //    RFQ_READY_FOR_QA,
    //    RFQ_ACCEPTED,
    //    RFQ_REJECTED,
    //    RFQ_EDITED,
    //    RFQ_PROCESSED, // when supplier(s) added
    //    RFQ_DELETED,
    //    RFQ_CLOSED,
    //    RFQ_EXPIRED,
    //    RFQ_REACHED_LIMIT,
    //    RFQ_TRANSLATED,
    //    RFQ_CATEGORIZED,
    //    SKIP_LIST,
    //    SUPPLIER_RECOMMENDED,
    //    SUPPLIER_RFQ_APPLIED
    //}
}
