package com.hse.recommendationsystem.domain.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.hse.recommendationsystem.domain.messaging.SupplierRecommendationNotificationPayload
import com.hse.recommendationsystem.domain.messaging.SupplierRecommendationNotificationPublisher
import com.hse.recommendationsystem.domain.model.DecisionType
import com.hse.recommendationsystem.domain.model.NotificationStatus
import com.hse.recommendationsystem.domain.model.Recommendation
import com.hse.recommendationsystem.domain.model.RecommendationNotification
import com.hse.recommendationsystem.domain.model.RecommendationWithSnapshot
import com.hse.recommendationsystem.domain.model.RfqRecommendationQueueType
import com.hse.recommendationsystem.domain.model.RfqStatus
import com.hse.recommendationsystem.domain.model.SupplierDecision
import com.hse.recommendationsystem.domain.model.SupplierProfileSnapshot
import com.hse.recommendationsystem.domain.repository.DecisionRepository
import com.hse.recommendationsystem.domain.repository.RecommendationNotificationRepository
import com.hse.recommendationsystem.domain.repository.RecommendationRepository
import com.hse.recommendationsystem.domain.repository.RfqCoreRepository
import com.hse.recommendationsystem.domain.repository.RfqRecommendationsQueueRepository
import com.hse.recommendationsystem.domain.repository.RfqUserRepository
import com.hse.recommendationsystem.domain.repository.SupplierProfileSnapshotRepository
import com.hse.recommendationsystem.infrastructure.config.MessagingProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class RecommendationService(
    private val recommendationsServiceClient: RecommendationsServiceClient,
    private val recommendationsServiceSupport: RecommendationsServiceSupport,
    private val recommendationRepository: RecommendationRepository,
    private val supplierProfileSnapshotRepository: SupplierProfileSnapshotRepository,
    private val recommendationNotificationRepository: RecommendationNotificationRepository,
    private val rfqCoreRepository: RfqCoreRepository,
    private val rfqUserRepository: RfqUserRepository,
    private val rfqRecommendationsQueueRepository: RfqRecommendationsQueueRepository,
    private val decisionRepository: DecisionRepository,
    private val objectMapper: ObjectMapper,
    private val messagingProperties: MessagingProperties,
    private val supplierRecommendationNotificationPublisher: SupplierRecommendationNotificationPublisher,
) {
    fun initiateRecommendationsTasks(rfqId: UUID) {
        rfqRecommendationsQueueRepository.enqueue(rfqId, RfqRecommendationQueueType.CUSTOMER)
    }

    @Transactional
    fun findAndStoreRecommendations(rfqId: UUID) {
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

        val senderProfileId =
            rfqUserRepository.findById(rfq.senderId)?.userProfileId

        val request =
            recommendationsServiceSupport.createSupplierRecommendationsRequest(
                rfq,
                senderProfileId,
            )
        val raw = recommendationsServiceClient.getRecommendations(request)
        if (raw == null || raw.supplierWithFlags.isEmpty()) {
            logger.warn("No recommendations returned for rfqId={}", rfqId)
            return
        }

        val candidates =
            raw.supplierWithFlags.map { dto ->
                recommendationsServiceSupport.toSupplierRecommendationCandidate(dto)
            }

        recommendationRepository.deleteAllDataForRfq(rfqId)
        for (candidate in candidates) {
            val rawNode: JsonNode =
                candidate.rawRecommendationJson
                    ?: objectMapper.createObjectNode().apply {
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
                    rawRecommendationJson = rawNode,
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
        }

        rfqCoreRepository.updateStatus(rfqId, RfqStatus.PROCESSED)
    }

    /**
     * Publishes SNS notifications for rows still in ON_WAIT (typically invoked by [com.hse.recommendationsystem.infrastructure.scheduling.RecommendationsJob]).
     */
    fun triggerRecommendationNotificationsToSuppliers() {
        if (!messagingProperties.enabled) return
        val pending = recommendationNotificationRepository.listOnWait(limit = 500)
        for (notification in pending) {
            val notificationId = notification.id ?: continue
            val payload =
                SupplierRecommendationNotificationPayload(
                    notificationId = notificationId,
                    rfqId = notification.rfqId,
                    supplierId = notification.supplierId,
                )
            runCatching { supplierRecommendationNotificationPublisher.publish(payload) }
                .onFailure { ex ->
                    logger.error(
                        "SNS publish failed for notificationId={} rfqId={}",
                        notificationId,
                        notification.rfqId,
                        ex,
                    )
                }
        }
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
