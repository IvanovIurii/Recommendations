package com.hse.recommendationsystem.domain.model

import java.util.UUID

/**
 * Request to the external recommendations HTTP API (WireMock in tests).
 * Mirrors the intent of rfq-service's [com.visable.rfqservice.matching.clients.SupplierRecommendationsRequest].
 */
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
