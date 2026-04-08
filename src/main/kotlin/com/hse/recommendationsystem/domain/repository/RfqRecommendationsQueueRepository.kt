package com.hse.recommendationsystem.domain.repository

import com.hse.recommendationsystem.domain.model.RfqRecommendationQueueEntry
import com.hse.recommendationsystem.domain.model.RfqRecommendationQueueType
import java.util.UUID

interface RfqRecommendationsQueueRepository {
    fun enqueue(
        rfqId: UUID,
        queueType: RfqRecommendationQueueType,
    )

    fun pollOldest(limit: Int): List<RfqRecommendationQueueEntry>

    fun deleteById(id: Long)

    fun recordFailedProcessingAttempt(
        id: Long,
        processingAttempts: Int,
    )
}
