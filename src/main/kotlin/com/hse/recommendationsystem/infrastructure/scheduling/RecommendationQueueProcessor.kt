package com.hse.recommendationsystem.infrastructure.scheduling

import com.hse.recommendationsystem.domain.model.RfqRecommendationQueueType
import com.hse.recommendationsystem.domain.repository.RfqRecommendationsQueueRepository
import com.hse.recommendationsystem.domain.service.RecommendationService
import com.hse.recommendationsystem.infrastructure.config.RecommendationsProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class RecommendationQueueProcessor(
    private val rfqRecommendationsQueueRepository: RfqRecommendationsQueueRepository,
    private val recommendationService: RecommendationService,
    private val recommendationsProperties: RecommendationsProperties,
) {
    fun processBatch() {
        val batch = rfqRecommendationsQueueRepository.pollOldest(recommendationsProperties.queueBatchSize)
        if (batch.isEmpty()) return

        for (entry in batch) {
            try {
                when (entry.queueType) {
                    RfqRecommendationQueueType.CUSTOMER ->
                        recommendationService.findAndStoreRecommendations(entry.rfqId)
                }
                rfqRecommendationsQueueRepository.deleteById(entry.id)
            } catch (e: Exception) {
                val failedAttempts = entry.processingAttempts + 1
                if (failedAttempts >= recommendationsProperties.queueMaxProcessingAttempts) {
                    logger.error(
                        "Dropping recommendation queue entry after {} failed attempts: {}",
                        failedAttempts,
                        entry,
                        e,
                    )
                    rfqRecommendationsQueueRepository.deleteById(entry.id)
                } else {
                    logger.warn(
                        "Failed processing recommendation queue entry {}, attempt {}/{} — will retry",
                        entry,
                        failedAttempts,
                        recommendationsProperties.queueMaxProcessingAttempts,
                        e,
                    )
                    rfqRecommendationsQueueRepository.recordFailedProcessingAttempt(
                        entry.id,
                        failedAttempts,
                    )
                }
            }
        }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(RecommendationQueueProcessor::class.java)
    }
}
