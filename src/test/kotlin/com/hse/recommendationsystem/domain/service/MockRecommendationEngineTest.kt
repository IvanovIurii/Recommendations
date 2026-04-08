package com.hse.recommendationsystem.domain.service

import com.hse.recommendationsystem.domain.model.MatchType
import com.hse.recommendationsystem.domain.model.RfqCore
import com.hse.recommendationsystem.domain.model.RfqStatus
import com.hse.recommendationsystem.domain.service.stubs.MockRecommendationEngine
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class MockRecommendationEngineTest {
    private val engine = MockRecommendationEngine()

    @Test
    fun `returns eight customer suppliers with varied match types`() {
        val rfq =
            RfqCore(
                rfqId = UUID.fromString("cafe0000-0000-4000-8000-000000000001"),
                senderId = 1L,
                title = "Bolts",
                description = "Need M8 bolts",
                deliveryLocation = "Berlin",
                quantity = "1000",
                supplierTypes = listOf("Manufacturer"),
                status = RfqStatus.ACCEPTED,
                buyerCountry = "DE",
                categoryId = 42L,
                createdAt = Instant.parse("2024-01-01T00:00:00Z"),
                updatedAt = Instant.parse("2024-01-01T00:00:00Z"),
            )

        val result = engine.recommendForAcceptedRfq(rfq)

        assertThat(result).hasSize(8)
        assertThat(result.groupingBy { it.matchType }.eachCount())
            .isEqualTo(
                mapOf(
                    MatchType.MATCH to 2,
                    MatchType.WEAK_MATCH to 2,
                    MatchType.RELATED to 2,
                    MatchType.NO_MATCH to 2,
                ),
            )
        assertThat(result).allMatch { it.isCustomer }
        assertThat(result.first().description).contains(rfq.rfqId.toString())
    }
}
