package com.hse.recommendationsystem.testconfigs

import com.hse.recommendationsystem.api.dto.CreateRfqRequest
import com.hse.recommendationsystem.application.domain.model.RfqCore
import com.hse.recommendationsystem.application.domain.model.RfqStatus
import com.hse.recommendationsystem.application.domain.model.RfqUser
import java.time.Instant
import java.util.UUID

class Fixtures {
    companion object {
        fun getRfqUser(
            userProfileId: UUID? = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
            email: String = "buyer@example.com",
            fullName: String = "Test User",
            countryCode: String = "DE",
            createdAt: Instant = Instant.parse("2025-01-15T10:00:00Z"),
        ): RfqUser = RfqUser(
            rfqUserId = null,
            userProfileId = userProfileId,
            email = email,
            fullName = fullName,
            countryCode = countryCode,
            createdAt = createdAt,
            updatedAt = createdAt,
        )

        fun getRfqCore(
            rfqId: UUID = UUID.fromString("cafe0000-0000-4000-8000-000000000001"),
            senderId: Long = 1L,
            title: String = "Test RFQ",
            description: String = "details",
            deliveryLocation: String = "Berlin",
            quantity: String = "100",
            supplierTypes: List<String>? = listOf("Manufacturer"),
            status: RfqStatus = RfqStatus.CREATED,
            buyerCountry: String? = "DE",
            categoryId: Long? = 1L,
            createdAt: Instant = Instant.parse("2025-01-15T10:00:00Z"),
        ): RfqCore = RfqCore(
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
            updatedAt = createdAt,
        )

        fun getCreateRfqRequest(
            email: String = "flow@example.com",
            fullName: String = "Flow Buyer",
            countryCode: String = "DE",
            userProfileId: UUID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
            title: String = "Steel brackets",
            description: String = "Need custom brackets",
            deliveryLocation: String = "Hamburg",
            quantity: String = "500 kg",
            supplierTypes: List<String> = listOf("Manufacturer"),
            buyerCountry: String = "DE",
            categoryId: Long = 99L,
        ): CreateRfqRequest = CreateRfqRequest(
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
    }
}
