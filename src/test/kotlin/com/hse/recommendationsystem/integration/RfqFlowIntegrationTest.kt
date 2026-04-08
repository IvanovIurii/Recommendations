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
import org.springframework.test.context.ActiveProfiles
import java.time.Duration
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class RfqFlowIntegrationTest(
    @param:Autowired private val restTemplate: TestRestTemplate,
) {
    @LocalServerPort
    private var port: Int = 0

    @Test
    fun `create accept wait for recommendations then select supplier`() {
        val base = "http://localhost:$port/api/v1/rfq"
        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }

        val createBody =
            CreateRfqRequest(
                email = "flow@example.com",
                fullName = "Flow Buyer",
                countryCode = "DE",
                userProfileId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                title = "Steel brackets",
                description = "Need custom brackets",
                deliveryLocation = "Hamburg",
                quantity = "500 kg",
                supplierTypes = listOf("Manufacturer"),
                buyerCountry = "DE",
                categoryId = 99L,
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

        await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(200)).until {
            val get =
                restTemplate.getForEntity(
                    "$base/$rfqId",
                    Map::class.java,
                )
            get.statusCode == HttpStatus.OK &&
                (get.body as Map<*, *>)["status"] == RfqStatus.PROCESSED.name
        }

        val recResponse =
            restTemplate.getForEntity(
                "$base/$rfqId/recommendations?page=0&pageSize=50",
                Map::class.java,
            )
        assertThat(recResponse.statusCode).isEqualTo(HttpStatus.OK)
        @Suppress("UNCHECKED_CAST")
        val recBody = recResponse.body as Map<String, Any>
        assertThat((recBody["total"] as Number).toInt()).isEqualTo(8)

        val supplierId = UUID.fromString("a1000000-0000-4000-8000-000000000001")
        val selectResponse =
            restTemplate.exchange(
                "$base/$rfqId/recommendations/$supplierId/select",
                HttpMethod.POST,
                HttpEntity("{}", headers),
                Void::class.java,
            )
        assertThat(selectResponse.statusCode).isEqualTo(HttpStatus.NO_CONTENT)

        val after =
            restTemplate.getForEntity(
                "$base/$rfqId/recommendations?page=0&pageSize=50",
                Map::class.java,
            )
        @Suppress("UNCHECKED_CAST")
        val list = (after.body as Map<String, Any>)["result"] as List<Map<String, Any>>
        val selected = list.first { it["supplierId"] == supplierId.toString() }
        assertThat(selected["decisionType"]).isEqualTo("SELECTED")
    }
}
