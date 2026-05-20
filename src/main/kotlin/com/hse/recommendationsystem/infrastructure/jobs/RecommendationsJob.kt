package com.hse.recommendationsystem.infrastructure.jobs

import com.hse.recommendationsystem.application.RecommendationService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class RecommendationsJob(
    private val recommendationService: RecommendationService,
    private val rfqRecommendationsQueueProcessor: RecommendationQueueProcessor,
) {
    @Scheduled(fixedDelayString = "\${app.recommendations.queue-processing-delay-ms:1000}")
    fun processRecommendationsQueue() {
        rfqRecommendationsQueueProcessor.processBatch()
    }

    @Scheduled(fixedDelayString = "\${app.recommendations.notification-processing-delay-ms:60000}")
    fun recommendationNotificationsJob() {
        recommendationService.triggerRecommendationNotificationsToSuppliers()
    }
}
