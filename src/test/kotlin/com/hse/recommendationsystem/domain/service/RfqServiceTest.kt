package com.hse.recommendationsystem.domain.service

import com.hse.recommendationsystem.domain.model.RfqCore
import com.hse.recommendationsystem.domain.model.RfqStatus
import com.hse.recommendationsystem.domain.model.RfqUser
import com.hse.recommendationsystem.domain.repository.RfqCoreRepository
import com.hse.recommendationsystem.domain.repository.RfqUserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class RfqServiceTest {
    private val clock: Clock = Clock.fixed(Instant.parse("2024-06-01T12:00:00Z"), ZoneOffset.UTC)
    private val rfqUserRepository = mockk<RfqUserRepository>()
    private val rfqCoreRepository = mockk<RfqCoreRepository>()
    private val recommendationService = mockk<RecommendationService>(relaxUnitFun = true)

    private lateinit var rfqService: RfqService

    @BeforeEach
    fun setUp() {
        rfqService =
            RfqService(
                rfqUserRepository = rfqUserRepository,
                rfqCoreRepository = rfqCoreRepository,
                recommendationService = recommendationService,
            )
    }

    @Test
    fun `createRfq saves user then core with CREATED status`() {
        every { rfqUserRepository.save(any()) } answers {
            firstArg<RfqUser>().copy(rfqUserId = 99L)
        }
        every { rfqCoreRepository.save(any()) } answers { firstArg() }

        val rfq =
            rfqService.createRfq(
                email = "buyer@example.com",
                fullName = "Buyer",
                countryCode = "DE",
                userProfileId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                title = "Parts",
                description = "Desc",
                deliveryLocation = "DE",
                quantity = "10",
                supplierTypes = listOf("Manufacturer"),
                buyerCountry = "DE",
                categoryId = 1L,
            )

        assertThat(rfq.status).isEqualTo(RfqStatus.CREATED)
        assertThat(rfq.senderId).isEqualTo(99L)
        verify(exactly = 1) { rfqUserRepository.save(any()) }
        verify(exactly = 1) { rfqCoreRepository.save(any()) }
        verify(exactly = 0) { recommendationService.initiateRecommendationsTasks(any()) }
    }

    @Test
    fun `acceptRfq updates status and initiates recommendation tasks`() {
        val rfqId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
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
        every { rfqCoreRepository.updateStatus(rfqId, RfqStatus.ACCEPTED) } returns 1

        rfqService.acceptRfq(rfqId)

        verify { rfqCoreRepository.updateStatus(rfqId, RfqStatus.ACCEPTED) }
        verify { recommendationService.initiateRecommendationsTasks(rfqId) }
    }

    @Test
    fun `acceptRfq rejects invalid status`() {
        val rfqId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc")
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

        assertThrows<IllegalStateException> { rfqService.acceptRfq(rfqId) }
        verify(exactly = 0) { recommendationService.initiateRecommendationsTasks(any()) }
    }
}
