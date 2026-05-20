package com.hse.recommendationsystem.infrastructure.messaging

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.hse.recommendationsystem.application.domain.model.NotificationStatus
import com.hse.recommendationsystem.application.domain.persistence.RecommendationNotificationRepository
import com.hse.recommendationsystem.application.dto.SupplierRecommendationNotificationPayload
import com.hse.recommendationsystem.infrastructure.pipeline.OnlinePipelineEventTracker
import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.Instant

@Component
@ConditionalOnProperty(name = ["app.messaging.enabled"], havingValue = "true")
class CnsNotificationFeedbackListener(
    private val objectMapper: ObjectMapper,
    private val recommendationNotificationRepository: RecommendationNotificationRepository,
    private val onlinePipelineEventTracker: OnlinePipelineEventTracker,
) {
    @SqsListener("\${app.messaging.cns-feedback-queue-name}")
    fun onMessage(rawBody: String) {
        logger.info("Received SQS message (length={})", rawBody.length)
        val payload = runCatching { parsePayload(rawBody) }.getOrElse { ex ->
            logger.error("Failed to parse notification payload", ex)
            return
        }
        val updated = recommendationNotificationRepository.updateStatusIfCurrent(
            notificationId = payload.notificationId,
            expectedCurrent = NotificationStatus.ON_WAIT,
            newStatus = NotificationStatus.SENT,
            modifiedAt = Instant.now(),
        )
        if (updated == 0) {
            logger.info(
                "No row updated for notificationId={} (already SENT or missing); idempotent skip",
                payload.notificationId,
            )
        } else {
            logger.info(
                "=== CNS FEEDBACK === Notification {} marked as SENT for rfqId={} supplierId={} — supplier received email recommendation",
                payload.notificationId,
                payload.rfqId,
                payload.supplierId,
            )
            onlinePipelineEventTracker.addEvent(
                payload.rfqId, "CNS_FEEDBACK",
                "Supplier ${payload.supplierId} received email recommendation (CNS confirmed delivery)",
            )
        }
    }

    private fun parsePayload(rawBody: String): SupplierRecommendationNotificationPayload {
        val root = runCatching { objectMapper.readTree(rawBody) }.getOrElse {
            return objectMapper.readValue(rawBody, SupplierRecommendationNotificationPayload::class.java)
        }
        val messageNode = root.get("Message")
        val payloadNode: JsonNode = when {
            messageNode == null || messageNode.isNull -> root
            messageNode.isObject -> messageNode
            messageNode.isTextual -> objectMapper.readTree(messageNode.asText())
            else -> error("Unexpected SNS Message JSON node type: ${messageNode.nodeType}")
        }
        return objectMapper.treeToValue(payloadNode, SupplierRecommendationNotificationPayload::class.java)
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(CnsNotificationFeedbackListener::class.java)
    }
}
