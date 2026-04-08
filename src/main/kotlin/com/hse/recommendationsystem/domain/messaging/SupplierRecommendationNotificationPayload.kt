package com.hse.recommendationsystem.domain.messaging

import java.util.UUID

data class SupplierRecommendationNotificationPayload(
    val version: String = "1.0",
    val eventType: String = "SupplierRecommendationNotificationRequested", // todo: define event types
    val notificationId: Long, // todo: we should save it, transaction outbox
    val rfqId: UUID,
    val supplierId: UUID,
)
