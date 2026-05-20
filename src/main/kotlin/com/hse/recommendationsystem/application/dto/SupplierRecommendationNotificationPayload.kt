package com.hse.recommendationsystem.application.dto

import java.util.UUID

data class SupplierRecommendationNotificationPayload(
    val version: String = "1.0",
    val eventType: String = "SupplierRecommendationNotificationRequested",
    val notificationId: Long,
    val rfqId: UUID,
    val supplierId: UUID,
)
