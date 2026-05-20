package com.hse.recommendationsystem.application.domain.model

import java.time.Instant
import java.util.UUID

data class RfqCore(
    val rfqId: UUID,
    val senderId: Long,
    val title: String?,
    val description: String?,
    val deliveryLocation: String?,
    val quantity: String?,
    val supplierTypes: List<String>?,
    val status: RfqStatus,
    val buyerCountry: String?,
    val categoryId: Long?,
    val createdAt: Instant,
    val updatedAt: Instant,
)
