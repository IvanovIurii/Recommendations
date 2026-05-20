package com.hse.recommendationsystem.integration

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.hse.recommendationsystem.api.dto.CreateRfqRequest
import com.hse.recommendationsystem.application.domain.model.RfqStatus
import com.hse.recommendationsystem.testconfigs.Fixtures
import com.hse.recommendationsystem.testconfigs.WiremockStubs
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import org.springframework.web.client.RestClient
import java.time.Duration
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWireMock(port = 0)
@Sql(scripts = ["classpath:clean.sql"])
class RfqRecommendationFlowIntegrationTest {
    @Autowired
    private lateinit var wireMockServer: WireMockServer

    @Autowired
    private lateinit var restClientBuilder: RestClient.Builder

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @LocalServerPort
    private var port: Int = 0

    @BeforeEach
    fun setupWireMock() {
        WiremockStubs.setupRecommendationsFind(wireMockServer)
    }

    @Test
    fun `create accept wait for recommendations then select supplier`() {
        val restClient = restClientBuilder.baseUrl("http://localhost:$port").build()
        val base = "/api/v1/rfq"
        val createBody = Fixtures.getCreateRfqRequest()

        val createJson = restClient
            .post()
            .uri(base)
            .contentType(MediaType.APPLICATION_JSON)
            .body(objectMapper.writeValueAsString(createBody))
            .retrieve()
            .body(String::class.java)!!
        val createMap = objectMapper.readValue(createJson, object : TypeReference<Map<String, Any>>() {})
        val rfqId = UUID.fromString(createMap["rfqId"] as String)

        restClient
            .post()
            .uri("$base/$rfqId/accept")
            .retrieve()
            .toBodilessEntity()

        await().atMost(Duration.ofSeconds(90)).pollInterval(Duration.ofMillis(200)).until {
            val getJson = restClient
                .get()
                .uri("$base/$rfqId")
                .retrieve()
                .body(String::class.java)!!
            val getMap = objectMapper.readValue(getJson, object : TypeReference<Map<String, Any>>() {})
            getMap["status"] == RfqStatus.PROCESSED.name
        }

        await().atMost(Duration.ofSeconds(90)).pollInterval(Duration.ofMillis(200)).until {
            countSentNotifications(rfqId) == 8
        }

        val recJson = restClient
            .get()
            .uri("$base/$rfqId/recommendations?page=0&pageSize=50")
            .retrieve()
            .body(String::class.java)!!
        val recBody = objectMapper.readValue(recJson, object : TypeReference<Map<String, Any>>() {})
        assertThat((recBody["total"] as Number).toInt()).isEqualTo(8)

        val supplierId = UUID.fromString("a1000000-0000-4000-8000-000000000001")
        restClient
            .post()
            .uri("$base/$rfqId/recommendations/$supplierId/select")
            .contentType(MediaType.APPLICATION_JSON)
            .body("{}")
            .retrieve()
            .toBodilessEntity()

        val afterJson = restClient
            .get()
            .uri("$base/$rfqId/recommendations?page=0&pageSize=50")
            .retrieve()
            .body(String::class.java)!!
        val afterBody = objectMapper.readValue(afterJson, object : TypeReference<Map<String, Any>>() {})
        @Suppress("UNCHECKED_CAST")
        val list = afterBody["result"] as List<Map<String, Any>>
        val selected = list.first { it["supplierId"] == supplierId.toString() }
        assertThat(selected["decisionType"]).isEqualTo("SELECTED")
    }

    @Test
    fun `full flow with SNS SQS and SENT notifications`() {
        val restClient = restClientBuilder.baseUrl("http://localhost:$port").build()
        val base = "/api/v1/rfq"

        val createBody = CreateRfqRequest(
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
        val createJson = restClient
            .post()
            .uri(base)
            .contentType(MediaType.APPLICATION_JSON)
            .body(objectMapper.writeValueAsString(createBody))
            .retrieve()
            .body(String::class.java)!!
        val createMap = objectMapper.readValue(createJson, object : TypeReference<Map<String, Any>>() {})
        val rfqId = UUID.fromString(createMap["rfqId"] as String)

        restClient
            .post()
            .uri("$base/$rfqId/accept")
            .retrieve()
            .toBodilessEntity()

        await().atMost(Duration.ofSeconds(90)).pollInterval(Duration.ofMillis(300)).until {
            val getJson = restClient
                .get()
                .uri("$base/$rfqId")
                .retrieve()
                .body(String::class.java)!!
            val getMap = objectMapper.readValue(getJson, object : TypeReference<Map<String, Any>>() {})
            getMap["status"] == RfqStatus.PROCESSED.name
        }

        await().atMost(Duration.ofSeconds(90)).pollInterval(Duration.ofMillis(300)).until {
            countSentNotifications(rfqId) == 8
        }

        val supplierId = UUID.fromString("a1000000-0000-4000-8000-000000000001")
        restClient
            .post()
            .uri("$base/$rfqId/recommendations/$supplierId/select")
            .contentType(MediaType.APPLICATION_JSON)
            .body("{}")
            .retrieve()
            .toBodilessEntity()

        val decisionCount = jdbcTemplate.queryForObject(
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
