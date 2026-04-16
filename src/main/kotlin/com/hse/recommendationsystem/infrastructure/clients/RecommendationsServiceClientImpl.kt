package com.hse.recommendationsystem.infrastructure.clients

import com.fasterxml.jackson.databind.ObjectMapper
import com.hse.recommendationsystem.domain.model.FindRecommendedSuppliersResponseDto
import com.hse.recommendationsystem.domain.model.SupplierRecommendations
import com.hse.recommendationsystem.domain.model.SupplierRecommendationsRequest
import com.hse.recommendationsystem.domain.service.RecommendationsServiceClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class RecommendationsServiceClientImpl(
    @param:Qualifier("recommendationsServiceRestClient")
    private val recommendationsServiceRestClient: RestClient,
    private val objectMapper: ObjectMapper,
) : RecommendationsServiceClient {
    override fun getRecommendations(request: SupplierRecommendationsRequest): SupplierRecommendations? {
        logger.info("Getting recommendations for rfqId={}", request.rfqId)
        return try {
            val body =
                recommendationsServiceRestClient
                    .post()
                    .uri(FIND_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(request))
                    .retrieve()
                    .body(String::class.java)
                    ?: return null
            val dto = objectMapper.readValue(body, FindRecommendedSuppliersResponseDto::class.java)
            val flags = dto.data.supplierIdsWithFlag?.take(RECOMMENDATIONS_LIMIT) ?: emptyList()
            if (flags.isEmpty()) return null
            SupplierRecommendations(supplierWithFlags = flags)
        } catch (e: Exception) {
            logger.error("getRecommendations failed for rfqId={}", request.rfqId, e)
            null
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RecommendationsServiceClientImpl::class.java)
        private const val FIND_PATH = "/find"
        private const val RECOMMENDATIONS_LIMIT = 25
    }
}
