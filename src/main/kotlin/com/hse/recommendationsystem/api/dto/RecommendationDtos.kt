package com.hse.recommendationsystem.api.dto

import com.hse.recommendationsystem.domain.model.DecisionType
import com.hse.recommendationsystem.domain.model.MatchType
import java.time.Instant
import java.util.UUID

data class RecommendationPageResponse(
    val result: List<RecommendationItemDto>,
    val total: Int,
    val page: Int,
    val pageSize: Int,
)

data class RecommendationItemDto(
    val rfqId: UUID,
    val supplierId: UUID,
    val unifiedSupplierId: UUID?,
    val matchType: MatchType?,
    val modelVersion: String?,
    val customerInNeed: Boolean?,
    val isCustomer: Boolean?,
    val name: String?,
    val website: String?,
    val profileUrl: String?,
    val country: String?,
    val distributionArea: String?,
    val description: String?,
    val descriptionDe: String?,
    val descriptionEn: String?,
    val supplierTypes: List<String>?,
    val products: List<String>?,
    val keywords: List<String>?,
    val productCategories: List<String>?,
    val recommendedAt: Instant,
    val snapshotAt: Instant,
    val decisionType: DecisionType?,
    val decidedAt: Instant?,
)

data class SelectSupplierRequest(
    val reason: String? = null,
)
