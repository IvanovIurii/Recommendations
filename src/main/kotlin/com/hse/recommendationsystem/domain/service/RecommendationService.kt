package com.hse.recommendationsystem.domain.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.hse.recommendationsystem.domain.messaging.SupplierRecommendationNotificationPayload
import com.hse.recommendationsystem.domain.messaging.SupplierRecommendationNotificationPublisher
import com.hse.recommendationsystem.domain.model.DecisionType
import com.hse.recommendationsystem.domain.model.NotificationStatus
import com.hse.recommendationsystem.domain.model.Recommendation
import com.hse.recommendationsystem.domain.model.RecommendationNotification
import com.hse.recommendationsystem.domain.model.RecommendationWithSnapshot
import com.hse.recommendationsystem.domain.model.RfqStatus
import com.hse.recommendationsystem.domain.model.SupplierDecision
import com.hse.recommendationsystem.domain.model.SupplierProfileSnapshot
import com.hse.recommendationsystem.domain.model.SupplierRecommendationCandidate
import com.hse.recommendationsystem.domain.repository.DecisionRepository
import com.hse.recommendationsystem.domain.repository.RecommendationNotificationRepository
import com.hse.recommendationsystem.domain.repository.RecommendationRepository
import com.hse.recommendationsystem.domain.repository.RfqCoreRepository
import com.hse.recommendationsystem.domain.repository.SupplierProfileSnapshotRepository
import com.hse.recommendationsystem.infrastructure.config.MessagingProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Instant
import java.util.UUID

@Service
class RecommendationService(
    private val recommendationEngine: SupplierRecommendationEngine,
    private val recommendationRepository: RecommendationRepository,
    private val supplierProfileSnapshotRepository: SupplierProfileSnapshotRepository,
    private val recommendationNotificationRepository: RecommendationNotificationRepository,
    private val rfqCoreRepository: RfqCoreRepository,
    private val decisionRepository: DecisionRepository,
    private val objectMapper: ObjectMapper,
    private val messagingProperties: MessagingProperties,
    private val supplierRecommendationNotificationPublisher: SupplierRecommendationNotificationPublisher,
) {
    @Transactional
    fun processRecommendationsForRfq(rfqId: UUID) {
        val rfq =
            rfqCoreRepository.findById(rfqId)
                ?: run {
                    logger.warn("RFQ not found for recommendation processing: {}", rfqId)
                    return
                }
        if (rfq.status != RfqStatus.ACCEPTED) {
            logger.warn("Skipping recommendations for RFQ {} in status {}", rfqId, rfq.status)
            return
        }

        val candidates = recommendationEngine.recommendForAcceptedRfq(rfq)
        if (candidates.isEmpty()) {
            logger.warn("No recommendations produced for RFQ {}", rfqId)
            return
        }

        recommendationRepository.deleteAllDataForRfq(rfqId)
        for (candidate in candidates) {
            val raw: ObjectNode =
                objectMapper.createObjectNode().apply {
                    put("supplierId", candidate.supplierId.toString())
                    put("matchType", candidate.matchType.databaseValue)
                    put("modelVersion", candidate.modelVersion)
                    put("customerInNeed", candidate.customerInNeed)
                    put("isCustomer", candidate.isCustomer)
                }

            recommendationRepository.save(
                Recommendation(
                    rfqId = rfqId,
                    supplierId = candidate.supplierId,
                    unifiedSupplierId = candidate.unifiedSupplierId,
                    matchType = candidate.matchType,
                    modelVersion = candidate.modelVersion,
                    customerInNeed = candidate.customerInNeed,
                    isCustomer = candidate.isCustomer,
                    rawRecommendationJson = raw,
                    recommendedAt = Instant.now(),
                ),
            )
            supplierProfileSnapshotRepository.save(
                SupplierProfileSnapshot(
                    rfqId = rfqId,
                    supplierId = candidate.supplierId,
                    name = candidate.name,
                    website = candidate.website,
                    profileUrl = candidate.profileUrl,
                    country = candidate.country,
                    distributionArea = candidate.distributionArea,
                    description = candidate.description,
                    descriptionDe = candidate.descriptionDe,
                    descriptionEn = candidate.descriptionEn,
                    supplierTypes = candidate.supplierTypes,
                    products = candidate.products,
                    keywords = candidate.keywords,
                    productCategories = candidate.productCategories,
                    snapshotAt = Instant.now(),
                ),
            )
            val savedNotification =
                recommendationNotificationRepository.save(
                    RecommendationNotification(
                        rfqId = rfqId,
                        supplierId = candidate.supplierId,
                        unifiedSupplierId = candidate.unifiedSupplierId,
                        status = NotificationStatus.ON_WAIT,
                        createdAt = Instant.now(),
                        modifiedAt = null,
                    ),
                )
            schedulePublishSupplierRecommendationAfterCommit(savedNotification, candidate, rfqId)
        }

        rfqCoreRepository.updateStatus(rfqId, RfqStatus.PROCESSED)
    }

    private fun schedulePublishSupplierRecommendationAfterCommit(
        notification: RecommendationNotification,
        candidate: SupplierRecommendationCandidate,
        rfqId: UUID,
    ) {
        if (!messagingProperties.enabled) return
        val payload = buildNotificationPayload(notification, candidate, rfqId)
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            runCatching { supplierRecommendationNotificationPublisher.publish(payload) }
                .onFailure { logger.error("SNS publish failed (no active transaction)", it) }
            return
        }
        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
                override fun afterCommit() {
                    runCatching { supplierRecommendationNotificationPublisher.publish(payload) }
                        .onFailure {
                            logger.error(
                                "SNS publish failed for notificationId={}",
                                payload.notificationId,
                                it,
                            )
                        }
                }
            },
        )
    }

    private fun buildNotificationPayload(
        notification: RecommendationNotification,
        candidate: SupplierRecommendationCandidate,
        rfqId: UUID,
    ): SupplierRecommendationNotificationPayload {
        val notificationId = notification.id ?: error("recommendations_notifications.id must be set before SNS publish")
//        val base = messagingProperties.publicBaseUrl.trimEnd('/')

        val base = "BASE_URL" // todo: put it to application.yaml

        val deepLink = "$base/rfq/$rfqId/supplier/${candidate.supplierId}/react" // todo: add the CTA response_link
        return SupplierRecommendationNotificationPayload(
            notificationId = notificationId,
            rfqId = rfqId,
            supplierId = candidate.supplierId,
        )
    }

    @Transactional(readOnly = true)
    fun getRecommendations(
        rfqId: UUID,
        page: Int,
        pageSize: Int,
    ): Pair<List<RecommendationWithSnapshot>, Int> =
        recommendationRepository.findWithSnapshotsByRfqId(rfqId, page, pageSize)

    @Transactional
    fun recordSupplierSelection(
        rfqId: UUID,
        supplierId: UUID,
        reason: String?,
    ) {
        val rfq =
            rfqCoreRepository.findById(rfqId)
                ?: error("RFQ not found: $rfqId")
        if (rfq.status != RfqStatus.PROCESSED) {
            error("RFQ must be PROCESSED before recording supplier selection; current=${rfq.status}")
        }
        if (!recommendationRepository.existsByRfqAndSupplier(rfqId, supplierId)) {
            error("Supplier $supplierId is not among recommendations for RFQ $rfqId")
        }

        decisionRepository.save(
            SupplierDecision(
                rfqId = rfqId,
                supplierId = supplierId,
                decisionType = DecisionType.SELECTED,
                reason = reason,
                decidedAt = Instant.now(),
            ),
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RecommendationService::class.java)
    }
}
