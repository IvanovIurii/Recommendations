package com.hse.recommendationsystem.api.dto

import com.hse.recommendationsystem.application.domain.model.RfqCore
import com.hse.recommendationsystem.application.domain.model.RfqStatus
import java.time.Instant
import java.util.UUID

data class RfqApiResponse(
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
) {
    companion object {
        fun from(rfq: RfqCore): RfqApiResponse = RfqApiResponse(
            rfqId = rfq.rfqId,
            senderId = rfq.senderId,
            title = rfq.title,
            description = rfq.description,
            deliveryLocation = rfq.deliveryLocation,
            quantity = rfq.quantity,
            supplierTypes = rfq.supplierTypes,
            status = rfq.status,
            buyerCountry = rfq.buyerCountry,
            categoryId = rfq.categoryId,
            createdAt = rfq.createdAt,
            updatedAt = rfq.updatedAt,
        )
    }
}
