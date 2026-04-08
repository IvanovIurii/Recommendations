package com.hse.recommendationsystem.domain.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.hse.recommendationsystem.domain.model.DecisionType
import com.hse.recommendationsystem.domain.model.RfqCore
import com.hse.recommendationsystem.domain.model.RfqStatus
import com.hse.recommendationsystem.domain.model.SupplierDecision
import com.hse.recommendationsystem.domain.repository.DecisionRepository
import com.hse.recommendationsystem.domain.repository.RecommendationNotificationRepository
import com.hse.recommendationsystem.domain.messaging.SupplierRecommendationNotificationPublisher
import com.hse.recommendationsystem.domain.repository.RecommendationRepository
import com.hse.recommendationsystem.domain.repository.RfqCoreRepository
import com.hse.recommendationsystem.domain.repository.SupplierProfileSnapshotRepository
import com.hse.recommendationsystem.domain.service.stubs.MockRecommendationEngine
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
    private val engine = MockRecommendationEngine()
    private val recommendationRepository = mockk<RecommendationRepository>(relaxed = true)
    private val supplierProfileSnapshotRepository = mockk<SupplierProfileSnapshotRepository>(relaxed = true)
    private val recommendationNotificationRepository = mockk<RecommendationNotificationRepository>(relaxed = true)
    private val rfqCoreRepository = mockk<RfqCoreRepository>(relaxed = true)
    private val decisionRepository = mockk<DecisionRepository>(relaxed = true)
    private val objectMapper = ObjectMapper().findAndRegisterModules()
    private val messagingProperties = MessagingProperties(enabled = false, snsTopicArn = "supplier-recommendation-events", cnsFeedbackQueueName = "cns-recommendation-feedback")
    private val noopPublisher = SupplierRecommendationNotificationPublisher { }

    private lateinit var recommendationService: RecommendationService

    @BeforeEach
    fun setUp() {
        var notificationSeq = 0L
        every { recommendationNotificationRepository.save(any()) } answers {
            notificationSeq++
            firstArg<com.hse.recommendationsystem.domain.model.RecommendationNotification>().copy(id = notificationSeq)
        }
        recommendationService =
            RecommendationService(
                recommendationEngine = engine,
                recommendationRepository = recommendationRepository,
                supplierProfileSnapshotRepository = supplierProfileSnapshotRepository,
                recommendationNotificationRepository = recommendationNotificationRepository,
                rfqCoreRepository = rfqCoreRepository,
                decisionRepository = decisionRepository,
                objectMapper = objectMapper,
                messagingProperties = messagingProperties,
                supplierRecommendationNotificationPublisher = noopPublisher,
            )
    }

    @Test
    fun `processRecommendationsForRfq persists rows and marks PROCESSED`() {
        val rfqId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd")
        val rfq =
            RfqCore(
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

        recommendationService.processRecommendationsForRfq(rfqId)

        verify { recommendationRepository.deleteAllDataForRfq(rfqId) }
        verify(exactly = 8) { recommendationRepository.save(any()) }
        verify(exactly = 8) { supplierProfileSnapshotRepository.save(any()) }
        verify(exactly = 8) { recommendationNotificationRepository.save(any()) }
        verify { rfqCoreRepository.updateStatus(rfqId, RfqStatus.PROCESSED) }
    }

    @Test
    fun `processRecommendationsForRfq skips when RFQ not ACCEPTED`() {
        val rfqId = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee")
        val rfq =
            RfqCore(
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

        recommendationService.processRecommendationsForRfq(rfqId)

        verify(exactly = 0) { recommendationRepository.deleteAllDataForRfq(any()) }
        verify(exactly = 0) { recommendationRepository.save(any()) }
    }

    @Test
    fun `recordSupplierSelection saves decision when supplier was recommended`() {
        val rfqId = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff")
        val supplierId = UUID.fromString("a1000000-0000-4000-8000-000000000001")
        val rfq =
            RfqCore(
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
        val rfq =
            RfqCore(
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

        assertThrows<IllegalStateException> {
            recommendationService.recordSupplierSelection(rfqId, supplierId, null)
        }
        verify(exactly = 0) { decisionRepository.save(any()) }
    }
}
