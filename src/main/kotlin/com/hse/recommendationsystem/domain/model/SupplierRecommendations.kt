package com.hse.recommendationsystem.domain.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.util.UUID

/**
 * Response from the external recommendations HTTP API.
 * Shape aligned with rfq-service [FindRecommendedSuppliersResponseDto] plus optional profile fields for this service (no separate supplier-facts client).
 */
data class FindRecommendedSuppliersResponseDto(
    val data: FindRecommendedSuppliersResponseDtoData,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class FindRecommendedSuppliersResponseDtoData(
    val supplierIdsWithFlag: List<RecommendedSupplierWithFlagsDto>? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class RecommendedSupplierWithFlagsDto(
    val supplierId: UUID,
    val matchType: String,
    val customerInNeed: Boolean,
    val isCustomer: Boolean = true,
    val modelVersion: String? = null,
    val unifiedSupplierId: UUID? = null,
    val name: String? = null,
    val website: String? = null,
    val profileUrl: String? = null,
    val country: String? = null,
    val distributionArea: String? = null,
    val description: String? = null,
    val descriptionDe: String? = null,
    val descriptionEn: String? = null,
    val supplierTypes: List<String>? = null,
    val products: List<String>? = null,
    val keywords: List<String>? = null,
    val productCategories: List<String>? = null,
)

data class SupplierRecommendations(
    val supplierWithFlags: List<RecommendedSupplierWithFlagsDto>,
)
