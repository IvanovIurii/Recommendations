package com.hse.recommendationsystem.integration

import com.hse.recommendationsystem.api.dto.CreateRfqRequest
import com.hse.recommendationsystem.domain.model.RfqStatus
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import java.time.Duration
import java.util.UUID

/**
 * Postgres + LocalStack (both from docker-compose):
 * recommendations → SNS → SQS fan-out → notification SENT → supplier select → decision.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class RecommendationsMessagingFlowIntegrationTest(
    @param:Autowired private val restTemplate: TestRestTemplate,
    @param:Autowired private val jdbcTemplate: JdbcTemplate,
) {
    @LocalServerPort
    private var port: Int = 0

    @Test
    fun `full flow with SNS SQS and SENT notifications`() {
        val base = "http://localhost:$port/api/v1/rfq"
        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }

        val createBody =
            CreateRfqRequest(
                email = "messaging-flow@example.com",
                fullName = "Buyer",
                countryCode = "DE",
                userProfileId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                title = "Parts",
                description = "Need parts",
                deliveryLocation = "Berlin",
                quantity = "10",
                supplierTypes = listOf("Manufacturer"),
                buyerCountry = "DE",
                categoryId = 1L,
            )
        val createResponse =
            restTemplate.postForEntity(
                base,
                HttpEntity(createBody, headers),
                Map::class.java,
            )
        assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        @Suppress("UNCHECKED_CAST")
        val rfqId = UUID.fromString((createResponse.body as Map<String, Any>)["rfqId"] as String)

        restTemplate.postForEntity("$base/$rfqId/accept", HttpEntity<Void>(null, headers), Void::class.java)

        await().atMost(Duration.ofSeconds(45)).pollInterval(Duration.ofMillis(300)).until {
            val get =
                restTemplate.getForEntity(
                    "$base/$rfqId",
                    Map::class.java,
                )
            get.statusCode == HttpStatus.OK &&
                (get.body as Map<*, *>)["status"] == RfqStatus.PROCESSED.name
        }

        await().atMost(Duration.ofSeconds(45)).pollInterval(Duration.ofMillis(300)).until {
            countSentNotifications(rfqId) == 8
        }

        val supplierId = UUID.fromString("a1000000-0000-4000-8000-000000000001")
        val selectResponse =
            restTemplate.exchange(
                "$base/$rfqId/recommendations/$supplierId/select",
                HttpMethod.POST,
                HttpEntity("{}", headers),
                Void::class.java,
            )
        assertThat(selectResponse.statusCode).isEqualTo(HttpStatus.NO_CONTENT)

        val decisionCount =
            jdbcTemplate.queryForObject(
                """
                SELECT count(*) FROM decision WHERE rfq_id = ?::uuid AND supplier_id = ?::uuid
                """.trimIndent(),
                Int::class.java,
                rfqId.toString(),
                supplierId.toString(),
            ) ?: 0
        assertThat(decisionCount).isEqualTo(1)
    }

    private fun countSentNotifications(rfqId: UUID): Int =
        jdbcTemplate.queryForObject(
            """
            SELECT count(*) FROM recommendations_notifications
            WHERE rfq_id = ?::uuid AND status = 'SENT'
            """.trimIndent(),
            Int::class.java,
            rfqId.toString(),
        ) ?: 0
}
