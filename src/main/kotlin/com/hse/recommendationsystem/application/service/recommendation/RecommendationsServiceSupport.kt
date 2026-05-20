package com.hse.recommendationsystem.application.service.recommendation

import com.hse.recommendationsystem.application.domain.model.RfqCore
import com.hse.recommendationsystem.application.domain.model.SupplierRecommendationsRequest
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class RecommendationsServiceSupport {

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
}
