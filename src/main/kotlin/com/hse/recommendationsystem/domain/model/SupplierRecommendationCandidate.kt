package com.hse.recommendationsystem.domain.model

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID

data class SupplierRecommendationCandidate(
    val supplierId: UUID,
    val unifiedSupplierId: UUID?,
    val matchType: MatchType,
    val modelVersion: String,
    val customerInNeed: Boolean,
    val isCustomer: Boolean,
    val name: String,
    val website: String,
    val profileUrl: String,
    val country: String,
    val distributionArea: String,
    val description: String,
    val descriptionDe: String?,
    val descriptionEn: String?,
    val supplierTypes: List<String>,
    val products: List<String>,
    val keywords: List<String>,
    val productCategories: List<String>,
    val rawRecommendationJson: JsonNode? = null,
)
