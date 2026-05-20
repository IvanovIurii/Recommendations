package com.hse.recommendationsystem.application.domain.model

import java.time.Instant
import java.util.UUID

data class RfqRecommendationQueueEntry(
    val id: Long,
    val rfqId: UUID,
    val queueType: RfqRecommendationQueueType,
    val createdAt: Instant,
    val processingAttempts: Int,
)
