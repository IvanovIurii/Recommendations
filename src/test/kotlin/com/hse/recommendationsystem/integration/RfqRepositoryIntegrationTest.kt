package com.hse.recommendationsystem.integration

import com.hse.recommendationsystem.application.domain.model.RfqCore
import com.hse.recommendationsystem.application.domain.model.RfqStatus
import com.hse.recommendationsystem.application.domain.model.RfqUser
import com.hse.recommendationsystem.application.domain.persistence.RfqCoreRepository
import com.hse.recommendationsystem.application.domain.persistence.RfqUserRepository
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
class RfqRepositoryIntegrationTest {
    @Autowired
    private lateinit var rfqUserRepository: RfqUserRepository

    @Autowired
    private lateinit var rfqCoreRepository: RfqCoreRepository

    @Test
    fun `save and load rfq user and core`() {
        val now = Instant.parse("2025-01-15T10:00:00Z")
        val user = rfqUserRepository.save(
            RfqUser(
                userProfileId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                email = "u@example.com",
                fullName = "User",
                countryCode = "DE",
                createdAt = now,
                updatedAt = now,
            ),
        )
        val rfqId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
        val rfq = RfqCore(
            rfqId = rfqId,
            senderId = user.rfqUserId!!,
            title = "Title",
            description = "Desc",
            deliveryLocation = "Berlin",
            quantity = "100",
            supplierTypes = listOf("Manufacturer", "Distributor"),
            status = RfqStatus.CREATED,
            buyerCountry = "DE",
            categoryId = 5L,
            createdAt = now,
            updatedAt = now,
        )
        rfqCoreRepository.save(rfq)

        val loaded = rfqCoreRepository.findById(rfqId)!!
        assertThat(loaded.status).isEqualTo(RfqStatus.CREATED)
        assertThat(loaded.supplierTypes).containsExactly("Manufacturer", "Distributor")
        assertThat(loaded.senderId).isEqualTo(user.rfqUserId)
    }
}
