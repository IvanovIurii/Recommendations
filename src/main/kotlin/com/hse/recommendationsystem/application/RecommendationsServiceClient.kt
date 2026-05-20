package com.hse.recommendationsystem.application

import com.hse.recommendationsystem.application.domain.model.SupplierRecommendationCandidate
import com.hse.recommendationsystem.application.domain.model.SupplierRecommendationsRequest

fun interface RecommendationsServiceClient {
    fun getRecommendations(request: SupplierRecommendationsRequest): List<SupplierRecommendationCandidate>?
}
