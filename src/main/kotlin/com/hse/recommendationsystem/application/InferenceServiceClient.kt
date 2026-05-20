package com.hse.recommendationsystem.application

import com.hse.recommendationsystem.application.domain.model.MatchType

data class InferencePrediction(
    val matchType: MatchType,
    val probabilities: Map<String, Double>,
)

interface InferenceServiceClient {
    fun predict(
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
    ): InferencePrediction
}
