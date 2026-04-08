package com.hse.recommendationsystem.api.mapping

import com.hse.recommendationsystem.api.dto.CreateRfqRequest
import com.hse.recommendationsystem.api.dto.RecommendationItemDto
import com.hse.recommendationsystem.api.dto.RecommendationPageResponse
import com.hse.recommendationsystem.api.dto.RfqResponse
import com.hse.recommendationsystem.domain.model.RecommendationWithSnapshot
import com.hse.recommendationsystem.domain.model.RfqCore

fun RfqCore.toResponse(): RfqResponse =
    RfqResponse(
        rfqId = rfqId,
        senderId = senderId,
        title = title,
        description = description,
        deliveryLocation = deliveryLocation,
        quantity = quantity,
        supplierTypes = supplierTypes,
        status = status,
        buyerCountry = buyerCountry,
        categoryId = categoryId,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun CreateRfqRequest.toDomainParams(): CreateRfqParams =
    CreateRfqParams(
        email = email,
        fullName = fullName,
        countryCode = countryCode,
        userProfileId = userProfileId,
        title = title,
        description = description,
        deliveryLocation = deliveryLocation,
        quantity = quantity,
        supplierTypes = supplierTypes,
        buyerCountry = buyerCountry,
        categoryId = categoryId,
    )

data class CreateRfqParams(
    val email: String,
    val fullName: String?,
    val countryCode: String?,
    val userProfileId: java.util.UUID?,
    val title: String?,
    val description: String?,
    val deliveryLocation: String?,
    val quantity: String?,
    val supplierTypes: List<String>?,
    val buyerCountry: String?,
    val categoryId: Long?,
)

fun RecommendationWithSnapshot.toDto(): RecommendationItemDto =
    RecommendationItemDto(
        rfqId = recommendation.rfqId,
        supplierId = recommendation.supplierId,
        unifiedSupplierId = recommendation.unifiedSupplierId,
        matchType = recommendation.matchType,
        modelVersion = recommendation.modelVersion,
        customerInNeed = recommendation.customerInNeed,
        isCustomer = recommendation.isCustomer,
        name = snapshot.name,
        website = snapshot.website,
        profileUrl = snapshot.profileUrl,
        country = snapshot.country,
        distributionArea = snapshot.distributionArea,
        description = snapshot.description,
        descriptionDe = snapshot.descriptionDe,
        descriptionEn = snapshot.descriptionEn,
        supplierTypes = snapshot.supplierTypes,
        products = snapshot.products,
        keywords = snapshot.keywords,
        productCategories = snapshot.productCategories,
        recommendedAt = recommendation.recommendedAt,
        snapshotAt = snapshot.snapshotAt,
        decisionType = decision?.decisionType,
        decidedAt = decision?.decidedAt,
    )

fun List<RecommendationWithSnapshot>.toPage(
    total: Int,
    page: Int,
    pageSize: Int,
): RecommendationPageResponse =
    RecommendationPageResponse(
        result = map { it.toDto() },
        total = total,
        page = page,
        pageSize = pageSize,
    )
