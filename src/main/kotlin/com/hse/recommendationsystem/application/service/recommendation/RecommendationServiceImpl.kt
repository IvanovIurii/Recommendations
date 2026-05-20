package com.hse.recommendationsystem.application.service.recommendation

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.hse.recommendationsystem.application.RecommendationService
import com.hse.recommendationsystem.application.RecommendationsServiceClient
import com.hse.recommendationsystem.application.SupplierRecommendationNotificationPublisher
import com.hse.recommendationsystem.application.domain.exception.InvalidRfqStatusException
import com.hse.recommendationsystem.application.domain.exception.RfqNotFoundException
import com.hse.recommendationsystem.application.domain.exception.SupplierNotRecommendedException
import com.hse.recommendationsystem.application.domain.model.DecisionType
import com.hse.recommendationsystem.application.domain.model.NotificationStatus
import com.hse.recommendationsystem.application.domain.model.Recommendation
import com.hse.recommendationsystem.application.domain.model.RecommendationNotification
import com.hse.recommendationsystem.application.domain.model.RecommendationWithSnapshot
import com.hse.recommendationsystem.application.domain.model.RfqRecommendationQueueType
import com.hse.recommendationsystem.application.domain.model.RfqStatus
import com.hse.recommendationsystem.application.domain.model.SupplierDecision
import com.hse.recommendationsystem.application.domain.model.SupplierProfileSnapshot
import com.hse.recommendationsystem.application.domain.persistence.DecisionRepository
import com.hse.recommendationsystem.application.domain.persistence.RecommendationNotificationRepository
import com.hse.recommendationsystem.application.domain.persistence.RecommendationRepository
import com.hse.recommendationsystem.application.domain.persistence.RfqCoreRepository
import com.hse.recommendationsystem.application.domain.persistence.RfqRecommendationsQueueRepository
import com.hse.recommendationsystem.application.domain.persistence.RfqUserRepository
import com.hse.recommendationsystem.application.domain.persistence.SupplierProfileSnapshotRepository
import com.hse.recommendationsystem.application.dto.SupplierRecommendationNotificationPayload
import com.hse.recommendationsystem.infrastructure.config.MessagingProperties
import com.hse.recommendationsystem.infrastructure.pipeline.OnlinePipelineEventTracker
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class RecommendationServiceImpl(
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
    private val onlinePipelineEventTracker: OnlinePipelineEventTracker,
) : RecommendationService {

    override fun initiateRecommendationsTasks(rfqId: UUID) {
        rfqRecommendationsQueueRepository.enqueue(rfqId, RfqRecommendationQueueType.CUSTOMER)
    }

    @Transactional
    override fun findAndStoreRecommendations(rfqId: UUID) {
        val rfq = rfqCoreRepository.findById(rfqId)
            ?: run {
                logger.warn("RFQ not found for recommendation processing: {}", rfqId)
                return
            }
        if (rfq.status != RfqStatus.ACCEPTED) {
            logger.warn("Skipping recommendations for RFQ {} in status {}", rfqId, rfq.status)
            return
        }

        onlinePipelineEventTracker.addEvent(rfqId, "TPP_RECALL", "Step 1 — Calling TPP recall service for supplier candidates")
        val senderProfileId = rfqUserRepository.findById(rfq.senderId)?.userProfileId
        val request = recommendationsServiceSupport.createSupplierRecommendationsRequest(rfq, senderProfileId)
        val candidates = recommendationsServiceClient.getRecommendations(request)
        if (candidates.isNullOrEmpty()) {
            onlinePipelineEventTracker.addEvent(rfqId, "TPP_RECALL", "No candidates returned from TPP", level = "WARN")
            logger.warn("No recommendations returned for rfqId={}", rfqId)
            return
        }
        onlinePipelineEventTracker.addEvent(rfqId, "TPP_RECALL", "TPP returned ${candidates.size} supplier candidates", data = mapOf("count" to candidates.size))

        onlinePipelineEventTracker.addEvent(rfqId, "INFERENCE", "Step 3 — RLAB inference scoring ${candidates.size} candidates")
        recommendationRepository.deleteAllDataForRfq(rfqId)
        for (candidate in candidates) {
            val rawNode: JsonNode = candidate.rawRecommendationJson
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
        onlinePipelineEventTracker.addEvent(rfqId, "RECOMMENDATIONS_STORED", "Recommendations stored: ${candidates.size} suppliers scored and saved", data = mapOf("count" to candidates.size))
    }

    override fun triggerRecommendationNotificationsToSuppliers() {
        if (!messagingProperties.enabled) return
        val pending = recommendationNotificationRepository.listOnWait(limit = 500)
        for (notification in pending) {
            val notificationId = notification.id ?: continue
            onlinePipelineEventTracker.addEvent(notification.rfqId, "CNS_NOTIFICATION", "Sending email notification via CNS for supplier ${notification.supplierId}")
            val payload = SupplierRecommendationNotificationPayload(
                notificationId = notificationId,
                rfqId = notification.rfqId,
                supplierId = notification.supplierId,
            )
            runCatching { supplierRecommendationNotificationPublisher.publish(payload) }
                .onSuccess {
                    onlinePipelineEventTracker.addEvent(notification.rfqId, "CNS_NOTIFICATION", "Email notification published to SNS for supplier ${notification.supplierId}")
                }
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
    override fun getRecommendations(
        rfqId: UUID,
        page: Int,
        pageSize: Int,
    ): Pair<List<RecommendationWithSnapshot>, Int> =
        recommendationRepository.findWithSnapshotsByRfqId(rfqId, page, pageSize)

    @Transactional
    override fun recordSupplierSelection(
        rfqId: UUID,
        supplierId: UUID,
        reason: String?,
    ) {
        val rfq = rfqCoreRepository.findById(rfqId)
            ?: throw RfqNotFoundException(rfqId)
        if (rfq.status != RfqStatus.PROCESSED) {
            throw InvalidRfqStatusException("RFQ must be PROCESSED before recording supplier selection; current=${rfq.status}")
        }
        if (!recommendationRepository.existsByRfqAndSupplier(rfqId, supplierId)) {
            throw SupplierNotRecommendedException(supplierId, rfqId)
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
        onlinePipelineEventTracker.addEvent(rfqId, "SUPPLIER_SELECTED", "Supplier $supplierId selected${if (reason != null) " — reason: $reason" else ""}")
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(RecommendationServiceImpl::class.java)
    }
}
