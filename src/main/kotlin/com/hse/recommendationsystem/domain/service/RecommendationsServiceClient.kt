package com.hse.recommendationsystem.domain.service

import com.hse.recommendationsystem.domain.model.SupplierRecommendations
import com.hse.recommendationsystem.domain.model.SupplierRecommendationsRequest

fun interface RecommendationsServiceClient {
    fun getRecommendations(request: SupplierRecommendationsRequest): SupplierRecommendations?
}
