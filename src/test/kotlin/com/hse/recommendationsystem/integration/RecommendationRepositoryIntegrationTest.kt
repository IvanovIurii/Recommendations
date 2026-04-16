package com.hse.recommendationsystem.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.hse.recommendationsystem.domain.model.DecisionType
import com.hse.recommendationsystem.domain.model.MatchType
import com.hse.recommendationsystem.domain.model.NotificationStatus
import com.hse.recommendationsystem.domain.model.Recommendation
import com.hse.recommendationsystem.domain.model.RecommendationNotification
import com.hse.recommendationsystem.domain.model.RfqCore
import com.hse.recommendationsystem.domain.model.RfqStatus
import com.hse.recommendationsystem.domain.model.RfqUser
import com.hse.recommendationsystem.domain.model.SupplierDecision
import com.hse.recommendationsystem.domain.model.SupplierProfileSnapshot
import com.hse.recommendationsystem.domain.repository.DecisionRepository
import com.hse.recommendationsystem.domain.repository.RecommendationNotificationRepository
import com.hse.recommendationsystem.domain.repository.RecommendationRepository
import com.hse.recommendationsystem.domain.repository.RfqCoreRepository
import com.hse.recommendationsystem.domain.repository.RfqUserRepository
import com.hse.recommendationsystem.domain.repository.SupplierProfileSnapshotRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import java.time.Instant
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@AutoConfigureWireMock(port = 0)
@Sql(scripts = ["classpath:clean.sql"])
class RecommendationRepositoryIntegrationTest {
    @Autowired
    private lateinit var rfqUserRepository: RfqUserRepository

    @Autowired
    private lateinit var rfqCoreRepository: RfqCoreRepository

    @Autowired
    private lateinit var recommendationRepository: RecommendationRepository

    @Autowired
    private lateinit var supplierProfileSnapshotRepository: SupplierProfileSnapshotRepository

    @Autowired
    private lateinit var decisionRepository: DecisionRepository

    @Autowired
    private lateinit var recommendationNotificationRepository: RecommendationNotificationRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `recommendation snapshot decision and notification round trip`() {
        val now = Instant.parse("2025-02-01T12:00:00Z")
        val user =
            rfqUserRepository.save(
                RfqUser(
                    userProfileId = null,
                    email = "buyer@example.com",
                    fullName = "B",
                    countryCode = "DE",
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        val rfqId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc")
        rfqCoreRepository.save(
            RfqCore(
                rfqId = rfqId,
                senderId = user.rfqUserId!!,
                title = "T",
                description = "D",
                deliveryLocation = "L",
                quantity = "1",
                supplierTypes = listOf("M"),
                status = RfqStatus.PROCESSED,
                buyerCountry = "DE",
                categoryId = null,
                createdAt = now,
                updatedAt = now,
            ),
        )
        val supplierId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd")
        val raw: ObjectNode =
            objectMapper.createObjectNode().apply {
                put("k", "v")
            }
        recommendationRepository.save(
            Recommendation(
                rfqId = rfqId,
                supplierId = supplierId,
                unifiedSupplierId = null,
                matchType = MatchType.MATCH,
                modelVersion = "v1",
                customerInNeed = true,
                isCustomer = true,
                rawRecommendationJson = raw,
                recommendedAt = now,
            ),
        )
        supplierProfileSnapshotRepository.save(
            SupplierProfileSnapshot(
                rfqId = rfqId,
                supplierId = supplierId,
                name = "Co",
                website = "https://co.example",
                profileUrl = "https://p.example/co",
                country = "DE",
                distributionArea = "EU",
                description = "Hello",
                descriptionDe = "Hallo",
                descriptionEn = "Hello",
                supplierTypes = listOf("Manufacturer"),
                products = listOf("P"),
                keywords = listOf("k"),
                productCategories = listOf("C"),
                snapshotAt = now,
            ),
        )
        recommendationNotificationRepository.save(
            RecommendationNotification(
                rfqId = rfqId,
                supplierId = supplierId,
                unifiedSupplierId = null,
                status = NotificationStatus.ON_WAIT,
                createdAt = now,
                modifiedAt = null,
            ),
        )
        decisionRepository.save(
            SupplierDecision(
                rfqId = rfqId,
                supplierId = supplierId,
                decisionType = DecisionType.SELECTED,
                reason = "ok",
                decidedAt = now,
            ),
        )

        val (page, total) = recommendationRepository.findWithSnapshotsByRfqId(rfqId, page = 0, pageSize = 10)
        assertThat(total).isEqualTo(1)
        assertThat(page).hasSize(1)
        assertThat(page[0].recommendation.matchType).isEqualTo(MatchType.MATCH)
        assertThat(page[0].snapshot.name).isEqualTo("Co")
        assertThat(page[0].decision?.decisionType).isEqualTo(DecisionType.SELECTED)
        assertThat(recommendationRepository.existsByRfqAndSupplier(rfqId, supplierId)).isTrue()
    }

    @Test
    fun `deleteAllDataForRfq removes dependent rows`() {
        val now = Instant.parse("2025-03-01T08:00:00Z")
        val user =
            rfqUserRepository.save(
                RfqUser(
                    userProfileId = null,
                    email = "x@example.com",
                    fullName = "X",
                    countryCode = "DE",
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        val rfqId = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee")
        rfqCoreRepository.save(
            RfqCore(
                rfqId = rfqId,
                senderId = user.rfqUserId!!,
                title = "T",
                description = "D",
                deliveryLocation = "L",
                quantity = "1",
                supplierTypes = null,
                status = RfqStatus.PROCESSED,
                buyerCountry = "DE",
                categoryId = null,
                createdAt = now,
                updatedAt = now,
            ),
        )
        val supplierId = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff")
        recommendationRepository.save(
            Recommendation(
                rfqId = rfqId,
                supplierId = supplierId,
                unifiedSupplierId = null,
                matchType = MatchType.RELATED,
                modelVersion = "v1",
                customerInNeed = false,
                isCustomer = true,
                rawRecommendationJson = null,
                recommendedAt = now,
            ),
        )
        supplierProfileSnapshotRepository.save(
            SupplierProfileSnapshot(
                rfqId = rfqId,
                supplierId = supplierId,
                name = "N",
                website = "w",
                profileUrl = "p",
                country = "DE",
                distributionArea = "EU",
                description = "d",
                descriptionDe = null,
                descriptionEn = null,
                supplierTypes = emptyList(),
                products = emptyList(),
                keywords = emptyList(),
                productCategories = emptyList(),
                snapshotAt = now,
            ),
        )
        recommendationNotificationRepository.save(
            RecommendationNotification(
                rfqId = rfqId,
                supplierId = supplierId,
                unifiedSupplierId = null,
                status = NotificationStatus.ON_WAIT,
                createdAt = now,
                modifiedAt = null,
            ),
        )
        decisionRepository.save(
            SupplierDecision(
                rfqId = rfqId,
                supplierId = supplierId,
                decisionType = DecisionType.SELECTED,
                reason = null,
                decidedAt = now,
            ),
        )

        recommendationRepository.deleteAllDataForRfq(rfqId)

        assertThat(recommendationRepository.existsByRfqAndSupplier(rfqId, supplierId)).isFalse()
    }
}
