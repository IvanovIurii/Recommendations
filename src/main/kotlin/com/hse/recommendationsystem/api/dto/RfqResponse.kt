package com.hse.recommendationsystem.api.dto

import com.hse.recommendationsystem.domain.model.RfqStatus
import java.time.Instant
import java.util.UUID

data class RfqResponse(
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
