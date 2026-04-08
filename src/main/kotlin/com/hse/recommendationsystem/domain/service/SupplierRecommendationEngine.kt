package com.hse.recommendationsystem.domain.service

import com.hse.recommendationsystem.domain.model.RfqCore
import com.hse.recommendationsystem.domain.model.SupplierRecommendationCandidate

fun interface SupplierRecommendationEngine {
    fun recommendForAcceptedRfq(rfq: RfqCore): List<SupplierRecommendationCandidate>
}
