package com.hse.recommendationsystem.api.dto

import com.hse.recommendationsystem.application.domain.model.DecisionType
import com.hse.recommendationsystem.application.domain.model.MatchType
import com.hse.recommendationsystem.application.domain.model.NotificationStatus
import com.hse.recommendationsystem.application.domain.model.RecommendationWithSnapshot
import java.time.Instant
import java.util.UUID

data class RecommendationPageApiResponse(
    val result: List<RecommendationItemDto>,
    val total: Int,
    val page: Int,
    val pageSize: Int,
) {
    companion object {
        fun from(
            rows: List<RecommendationWithSnapshot>,
            total: Int,
            page: Int,
            pageSize: Int,
        ): RecommendationPageApiResponse = RecommendationPageApiResponse(
            result = rows.map { RecommendationItemDto.from(it) },
            total = total,
            page = page,
            pageSize = pageSize,
        )
    }
}

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
    val notificationStatus: NotificationStatus?,
) {
    companion object {
        fun from(item: RecommendationWithSnapshot): RecommendationItemDto = RecommendationItemDto(
            rfqId = item.recommendation.rfqId,
            supplierId = item.recommendation.supplierId,
            unifiedSupplierId = item.recommendation.unifiedSupplierId,
            matchType = item.recommendation.matchType,
            modelVersion = item.recommendation.modelVersion,
            customerInNeed = item.recommendation.customerInNeed,
            isCustomer = item.recommendation.isCustomer,
            name = item.snapshot.name,
            website = item.snapshot.website,
            profileUrl = item.snapshot.profileUrl,
            country = item.snapshot.country,
            distributionArea = item.snapshot.distributionArea,
            description = item.snapshot.description,
            descriptionDe = item.snapshot.descriptionDe,
            descriptionEn = item.snapshot.descriptionEn,
            supplierTypes = item.snapshot.supplierTypes,
            products = item.snapshot.products,
            keywords = item.snapshot.keywords,
            productCategories = item.snapshot.productCategories,
            recommendedAt = item.recommendation.recommendedAt,
            snapshotAt = item.snapshot.snapshotAt,
            decisionType = item.decision?.decisionType,
            decidedAt = item.decision?.decidedAt,
            notificationStatus = item.notificationStatus,
        )
    }
}

data class SelectSupplierRequest(
    val reason: String? = null,
)
