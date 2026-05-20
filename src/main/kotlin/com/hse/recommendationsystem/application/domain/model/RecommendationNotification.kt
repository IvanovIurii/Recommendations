package com.hse.recommendationsystem.application.domain.model

import java.time.Instant
import java.util.UUID

data class RecommendationNotification(
    val id: Long? = null,
    val rfqId: UUID,
    val supplierId: UUID,
    val unifiedSupplierId: UUID?,
    val status: NotificationStatus,
    val createdAt: Instant,
    val modifiedAt: Instant?,
)
