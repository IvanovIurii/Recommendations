package com.hse.recommendationsystem.application

import com.hse.recommendationsystem.application.domain.model.SupplierRecommendationsRequest

data class TppSupplierCandidate(
    val supplierId: String,
    val unifiedSupplierId: String,
    val name: String,
    val website: String,
    val profileUrl: String,
    val country: String,
    val distributionArea: String,
    val description: String,
    val supplierTypes: List<String>,
    val products: List<String>,
    val keywords: List<String>,
    val productCategories: List<String>,
)

fun interface TppClient {
    fun getCandidates(request: SupplierRecommendationsRequest): List<TppSupplierCandidate>
}
