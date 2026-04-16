package com.hse.recommendationsystem.domain.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.hse.recommendationsystem.domain.model.MatchType
import com.hse.recommendationsystem.domain.model.RecommendedSupplierWithFlagsDto
import com.hse.recommendationsystem.domain.model.RfqCore
import com.hse.recommendationsystem.domain.model.SupplierRecommendationsRequest
import com.hse.recommendationsystem.domain.model.SupplierRecommendationCandidate
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class RecommendationsServiceSupport(
    private val objectMapper: ObjectMapper,
) {
    fun createSupplierRecommendationsRequest(
        rfq: RfqCore,
        senderProfileId: UUID?,
    ): SupplierRecommendationsRequest =
        SupplierRecommendationsRequest(
            rfqId = rfq.rfqId,
            title = rfq.title,
            description = rfq.description,
            deliveryLocation = rfq.deliveryLocation,
            quantity = rfq.quantity,
            supplierTypes = rfq.supplierTypes,
            buyerCountry = rfq.buyerCountry,
            categoryId = rfq.categoryId,
            senderProfileId = senderProfileId,
            recommendCustomers = true,
        )

    fun toSupplierRecommendationCandidate(
        dto: RecommendedSupplierWithFlagsDto,
    ): SupplierRecommendationCandidate {
        val matchType =
            MatchType.fromDatabase(dto.matchType)
                ?: MatchType.NO_MATCH
        return SupplierRecommendationCandidate(
            supplierId = dto.supplierId,
            unifiedSupplierId = dto.unifiedSupplierId,
            matchType = matchType,
            modelVersion = dto.modelVersion ?: DEFAULT_MODEL_VERSION,
            customerInNeed = dto.customerInNeed,
            isCustomer = dto.isCustomer,
            name = dto.name ?: "Unknown",
            website = dto.website ?: "",
            profileUrl = dto.profileUrl ?: "",
            country = dto.country ?: "",
            distributionArea = dto.distributionArea ?: "",
            description = dto.description ?: "",
            descriptionDe = dto.descriptionDe,
            descriptionEn = dto.descriptionEn,
            supplierTypes = dto.supplierTypes ?: emptyList(),
            products = dto.products ?: emptyList(),
            keywords = dto.keywords ?: emptyList(),
            productCategories = dto.productCategories ?: emptyList(),
            rawRecommendationJson = objectMapper.valueToTree(dto),
        )
    }

    companion object {
        private const val DEFAULT_MODEL_VERSION = "v1"
    }
}
