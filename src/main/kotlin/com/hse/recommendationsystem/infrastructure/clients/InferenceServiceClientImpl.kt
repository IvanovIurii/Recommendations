package com.hse.recommendationsystem.infrastructure.clients

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.hse.recommendationsystem.application.InferencePrediction
import com.hse.recommendationsystem.application.InferenceServiceClient
import com.hse.recommendationsystem.application.domain.model.MatchType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@Service
class InferenceServiceClientImpl(
    @param:Qualifier("inferenceServiceRestClient")
    private val restClient: RestClient,
    private val objectMapper: ObjectMapper,
) : InferenceServiceClient {

    override fun predict(
        rfqTitle: String,
        rfqDescription: String,
        deliveryLocation: String,
        quantity: String,
        rfqSupplierTypes: String,
        supplierName: String,
        supplierCountry: String,
        distributionArea: String,
        supplierDescription: String,
        supplierTypes: String,
        products: String,
        productCategories: String,
        keywords: String,
    ): InferencePrediction {
        val requestBody = mapOf(
            "rfq_title" to rfqTitle,
            "rfq_description" to rfqDescription,
            "delivery_location" to deliveryLocation,
            "quantity" to quantity,
            "rfq_supplier_types" to rfqSupplierTypes,
            "supplier_name" to supplierName,
            "supplier_country" to supplierCountry,
            "distribution_area" to distributionArea,
            "supplier_description" to supplierDescription,
            "supplier_types" to supplierTypes,
            "products" to products,
            "product_categories" to productCategories,
            "keywords" to keywords,
        )

        val responseBody = restClient
            .post()
            .uri("/predict")
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBody)
            .retrieve()
            .body(String::class.java)
            ?: throw RuntimeException("Empty response from inference service")

        val dto = objectMapper.readValue(responseBody, PredictionResponseDto::class.java)
        val matchType = MatchType.fromDatabase(dto.matchType) ?: MatchType.NO_MATCH

        logger.info(
            "Inference result: supplier='{}' → matchType={}, probabilities={}",
            supplierName,
            matchType,
            dto.probabilities,
        )

        return InferencePrediction(
            matchType = matchType,
            probabilities = dto.probabilities,
        )
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class PredictionResponseDto(
        val match_type: String,
        val probabilities: Map<String, Double>,
    ) {
        val matchType: String get() = match_type
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(InferenceServiceClientImpl::class.java)
    }
}
