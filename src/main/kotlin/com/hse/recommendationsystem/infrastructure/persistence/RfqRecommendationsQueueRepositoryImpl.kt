package com.hse.recommendationsystem.infrastructure.persistence

import com.hse.recommendationsystem.application.domain.model.RfqRecommendationQueueEntry
import com.hse.recommendationsystem.application.domain.model.RfqRecommendationQueueType
import com.hse.recommendationsystem.application.domain.persistence.RfqRecommendationsQueueRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
class RfqRecommendationsQueueRepositoryImpl(
    private val entityRepository: RfqRecommendationsQueueEntityRepository,
) : RfqRecommendationsQueueRepository {

    override fun enqueue(rfqId: UUID, queueType: RfqRecommendationQueueType) {
        entityRepository.enqueue(rfqId, queueType.name, Instant.now())
    }

    override fun pollOldest(limit: Int): List<RfqRecommendationQueueEntry> =
        entityRepository.pollOldest(limit).map { it.toDomain() }

    override fun deleteById(id: Long) {
        entityRepository.deleteById(id)
    }

    override fun recordFailedProcessingAttempt(id: Long, processingAttempts: Int) {
        entityRepository.recordFailedProcessingAttempt(id, processingAttempts)
    }

    private fun com.hse.recommendationsystem.infrastructure.persistence.entities.RfqRecommendationsQueueEntity.toDomain() =
        RfqRecommendationQueueEntry(
            id = id!!,
            rfqId = rfqId,
            queueType = RfqRecommendationQueueType.valueOf(queueType),
            createdAt = createdAt,
            processingAttempts = processingAttempts,
        )
}
