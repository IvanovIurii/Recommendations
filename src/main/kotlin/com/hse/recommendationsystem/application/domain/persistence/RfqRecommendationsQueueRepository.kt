package com.hse.recommendationsystem.application.domain.persistence

import com.hse.recommendationsystem.application.domain.model.RfqRecommendationQueueEntry
import com.hse.recommendationsystem.application.domain.model.RfqRecommendationQueueType
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
