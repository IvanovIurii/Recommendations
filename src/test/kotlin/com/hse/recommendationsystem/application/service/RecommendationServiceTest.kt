package com.hse.recommendationsystem.application.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.hse.recommendationsystem.application.RecommendationsServiceClient
import com.hse.recommendationsystem.application.SupplierRecommendationNotificationPublisher
import com.hse.recommendationsystem.application.domain.exception.InvalidRfqStatusException
import com.hse.recommendationsystem.application.domain.model.DecisionType
import com.hse.recommendationsystem.application.domain.model.MatchType
import com.hse.recommendationsystem.application.domain.model.RecommendationNotification
import com.hse.recommendationsystem.application.domain.model.RfqCore
import com.hse.recommendationsystem.application.domain.model.RfqStatus
import com.hse.recommendationsystem.application.domain.model.SupplierDecision
import com.hse.recommendationsystem.application.domain.model.SupplierRecommendationCandidate
import com.hse.recommendationsystem.application.domain.persistence.DecisionRepository
import com.hse.recommendationsystem.application.domain.persistence.RecommendationNotificationRepository
import com.hse.recommendationsystem.application.domain.persistence.RecommendationRepository
import com.hse.recommendationsystem.application.domain.persistence.RfqCoreRepository
import com.hse.recommendationsystem.application.domain.persistence.RfqRecommendationsQueueRepository
import com.hse.recommendationsystem.application.domain.persistence.RfqUserRepository
import com.hse.recommendationsystem.application.domain.persistence.SupplierProfileSnapshotRepository
import com.hse.recommendationsystem.application.service.recommendation.RecommendationServiceImpl
import com.hse.recommendationsystem.application.service.recommendation.RecommendationsServiceSupport
import com.hse.recommendationsystem.infrastructure.config.MessagingProperties
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class RecommendationServiceTest {
    private val clock: Clock = Clock.fixed(Instant.parse("2024-07-01T09:00:00Z"), ZoneOffset.UTC)
    private val recommendationsServiceClient = mockk<RecommendationsServiceClient>()
    private val objectMapper = ObjectMapper().findAndRegisterModules()
    private val recommendationsServiceSupport = RecommendationsServiceSupport()
    private val recommendationRepository = mockk<RecommendationRepository>(relaxed = true)
    private val supplierProfileSnapshotRepository = mockk<SupplierProfileSnapshotRepository>(relaxed = true)
    private val recommendationNotificationRepository = mockk<RecommendationNotificationRepository>(relaxed = true)
    private val rfqCoreRepository = mockk<RfqCoreRepository>(relaxed = true)
    private val rfqUserRepository = mockk<RfqUserRepository>(relaxed = true)
    private val rfqRecommendationsQueueRepository = mockk<RfqRecommendationsQueueRepository>(relaxed = true)
    private val decisionRepository = mockk<DecisionRepository>(relaxed = true)
    private val messagingProperties = MessagingProperties(
        enabled = false,
        snsTopicArn = "arn:aws:sns:us-east-1:000000000000:supplier-recommendation-events",
        cnsFeedbackQueueName = "cns-recommendation-feedback",
    )
    private val noopPublisher = SupplierRecommendationNotificationPublisher { }

    private lateinit var recommendationService: RecommendationServiceImpl

    private fun buildCandidates(): List<SupplierRecommendationCandidate> =
        (1..8).map { i ->
            SupplierRecommendationCandidate(
                supplierId = UUID.fromString("a1000000-0000-4000-8000-${String.format("%012d", i)}"),
                unifiedSupplierId = null,
                matchType = MatchType.MATCH,
                modelVersion = "v1",
                customerInNeed = true,
                isCustomer = true,
                name = "Supplier $i",
                website = "https://example.com",
                profileUrl = "https://example.com/p",
                country = "DE",
                distributionArea = "EU",
                description = "Desc",
                descriptionDe = null,
                descriptionEn = "Desc",
                supplierTypes = listOf("Manufacturer"),
                products = listOf("P"),
                keywords = listOf("k"),
                productCategories = listOf("C"),
                rawRecommendationJson = null,
            )
        }

    @BeforeEach
    fun setUp() {
        var notificationSeq = 0L
        every { recommendationNotificationRepository.save(any()) } answers {
            notificationSeq++
            firstArg<RecommendationNotification>().copy(id = notificationSeq)
        }
        every { recommendationsServiceClient.getRecommendations(any()) } returns buildCandidates()
        recommendationService = RecommendationServiceImpl(
            recommendationsServiceClient = recommendationsServiceClient,
            recommendationsServiceSupport = recommendationsServiceSupport,
            recommendationRepository = recommendationRepository,
            supplierProfileSnapshotRepository = supplierProfileSnapshotRepository,
            recommendationNotificationRepository = recommendationNotificationRepository,
            rfqCoreRepository = rfqCoreRepository,
            rfqUserRepository = rfqUserRepository,
            rfqRecommendationsQueueRepository = rfqRecommendationsQueueRepository,
            decisionRepository = decisionRepository,
            objectMapper = objectMapper,
            messagingProperties = messagingProperties,
            supplierRecommendationNotificationPublisher = noopPublisher,
        )
    }

    @Test
    fun `findAndStoreRecommendations persists rows and marks PROCESSED`() {
        val rfqId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd")
        val rfq = RfqCore(
            rfqId = rfqId,
            senderId = 1L,
            title = "T",
            description = "D",
            deliveryLocation = "L",
            quantity = "1",
            supplierTypes = emptyList(),
            status = RfqStatus.ACCEPTED,
            buyerCountry = "DE",
            categoryId = null,
            createdAt = clock.instant(),
            updatedAt = clock.instant(),
        )
        every { rfqCoreRepository.findById(rfqId) } returns rfq
        every { rfqUserRepository.findById(1L) } returns null

        recommendationService.findAndStoreRecommendations(rfqId)

        verify { recommendationRepository.deleteAllDataForRfq(rfqId) }
        verify(exactly = 8) { recommendationRepository.save(any()) }
        verify(exactly = 8) { supplierProfileSnapshotRepository.save(any()) }
        verify(exactly = 8) { recommendationNotificationRepository.save(any()) }
        verify { rfqCoreRepository.updateStatus(rfqId, RfqStatus.PROCESSED) }
    }

    @Test
    fun `findAndStoreRecommendations skips when RFQ not ACCEPTED`() {
        val rfqId = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee")
        val rfq = RfqCore(
            rfqId = rfqId,
            senderId = 1L,
            title = "T",
            description = "D",
            deliveryLocation = "L",
            quantity = "1",
            supplierTypes = emptyList(),
            status = RfqStatus.CREATED,
            buyerCountry = "DE",
            categoryId = null,
            createdAt = clock.instant(),
            updatedAt = clock.instant(),
        )
        every { rfqCoreRepository.findById(rfqId) } returns rfq

        recommendationService.findAndStoreRecommendations(rfqId)

        verify(exactly = 0) { recommendationRepository.deleteAllDataForRfq(any()) }
        verify(exactly = 0) { recommendationRepository.save(any()) }
    }

    @Test
    fun `recordSupplierSelection saves decision when supplier was recommended`() {
        val rfqId = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff")
        val supplierId = UUID.fromString("a1000000-0000-4000-8000-000000000001")
        val rfq = RfqCore(
            rfqId = rfqId,
            senderId = 1L,
            title = "T",
            description = "D",
            deliveryLocation = "L",
            quantity = "1",
            supplierTypes = emptyList(),
            status = RfqStatus.PROCESSED,
            buyerCountry = "DE",
            categoryId = null,
            createdAt = clock.instant(),
            updatedAt = clock.instant(),
        )
        every { rfqCoreRepository.findById(rfqId) } returns rfq
        every { recommendationRepository.existsByRfqAndSupplier(rfqId, supplierId) } returns true
        val decisionSlot = slot<SupplierDecision>()
        every { decisionRepository.save(capture(decisionSlot)) } returns Unit

        recommendationService.recordSupplierSelection(rfqId, supplierId, "Interested")

        assertThat(decisionSlot.captured.decisionType).isEqualTo(DecisionType.SELECTED)
        assertThat(decisionSlot.captured.reason).isEqualTo("Interested")
        verify { decisionRepository.save(any()) }
    }

    @Test
    fun `recordSupplierSelection fails when RFQ not PROCESSED`() {
        val rfqId = UUID.fromString("11111111-2222-3333-4444-555555555555")
        val supplierId = UUID.fromString("a1000000-0000-4000-8000-000000000001")
        val rfq = RfqCore(
            rfqId = rfqId,
            senderId = 1L,
            title = "T",
            description = "D",
            deliveryLocation = "L",
            quantity = "1",
            supplierTypes = emptyList(),
            status = RfqStatus.ACCEPTED,
            buyerCountry = "DE",
            categoryId = null,
            createdAt = clock.instant(),
            updatedAt = clock.instant(),
        )
        every { rfqCoreRepository.findById(rfqId) } returns rfq

        assertThrows<InvalidRfqStatusException> {
            recommendationService.recordSupplierSelection(rfqId, supplierId, null)
        }
        verify(exactly = 0) { decisionRepository.save(any()) }
    }
}
