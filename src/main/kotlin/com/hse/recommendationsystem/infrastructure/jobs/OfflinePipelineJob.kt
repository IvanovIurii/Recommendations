package com.hse.recommendationsystem.infrastructure.jobs

import com.hse.recommendationsystem.application.OfflinePipelineService
import com.hse.recommendationsystem.infrastructure.config.OfflinePipelineProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["app.offline.enabled"], havingValue = "true")
class OfflinePipelineJob(
    private val offlinePipelineService: OfflinePipelineService,
    private val offlinePipelineProperties: OfflinePipelineProperties,
) {

    @Scheduled(fixedDelayString = "\${app.offline.schedule-delay-ms:300000}", initialDelay = 15_000)
    fun runOfflinePipeline() {
        logger.info("=== OFFLINE PIPELINE JOB TRIGGERED ===")
        try {
            offlinePipelineService.runOfflinePipeline()
        } catch (e: Exception) {
            logger.error("Offline pipeline failed, will retry on next schedule", e)
        }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(OfflinePipelineJob::class.java)
    }
}
