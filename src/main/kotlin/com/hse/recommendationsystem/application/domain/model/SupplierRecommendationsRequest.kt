package com.hse.recommendationsystem.application.domain.model

import java.util.UUID

data class SupplierRecommendationsRequest(
    val rfqId: UUID,
    val title: String?,
    val description: String?,
    val deliveryLocation: String?,
    val quantity: String?,
    val supplierTypes: List<String>?,
    val buyerCountry: String?,
    val categoryId: Long?,
    val senderProfileId: UUID?,
    val recommendCustomers: Boolean = true,
)
