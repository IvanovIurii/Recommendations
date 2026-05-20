package com.hse.recommendationsystem.application

import com.hse.recommendationsystem.application.domain.model.RfqCore
import java.util.UUID

interface RfqService {
    fun createRfq(
        email: String,
        fullName: String?,
        countryCode: String?,
        userProfileId: UUID?,
        title: String?,
        description: String?,
        deliveryLocation: String?,
        quantity: String?,
        supplierTypes: List<String>?,
        buyerCountry: String?,
        categoryId: Long?,
    ): RfqCore

    fun acceptRfq(rfqId: UUID)

    fun getRfq(rfqId: UUID): RfqCore
}
