package com.hse.recommendationsystem.infrastructure.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["app.messaging.enabled"], havingValue = "true")
class ModelSyncEventListener(
    private val objectMapper: ObjectMapper,
) {
    @SqsListener("\${app.messaging.model-sync-queue-name}")
    fun onModelSyncEvent(rawBody: String) {
        logger.info("=== MODEL SYNC EVENT RECEIVED === raw message length={}", rawBody.length)

        try {
            val root = objectMapper.readTree(rawBody)

            val messageNode = root.get("Message")
            val payload = when {
                messageNode != null && messageNode.isTextual -> objectMapper.readTree(messageNode.asText())
                messageNode != null && messageNode.isObject -> messageNode
                else -> root
            }

            val eventType = payload.get("eventType")?.asText() ?: "unknown"
            val modelVersion = payload.get("modelVersion")?.asText() ?: "unknown"
            val status = payload.get("status")?.asText() ?: "unknown"
            val syncedAt = payload.get("syncedAt")?.asText() ?: "unknown"

            logger.info(
                "=== NEW MODEL IN USE === eventType={}, modelVersion={}, status={}, syncedAt={}",
                eventType,
                modelVersion,
                status,
                syncedAt,
            )
            logger.info(
                "Model {} is now ACTIVE in the inference service (RLAB). " +
                    "All future RFQ recommendations will use this model version.",
                modelVersion,
            )
        } catch (e: Exception) {
            logger.error("Failed to parse model sync event", e)
        }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(ModelSyncEventListener::class.java)
    }
}
