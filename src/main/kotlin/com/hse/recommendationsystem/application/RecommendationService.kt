package com.hse.recommendationsystem.application

import com.hse.recommendationsystem.application.domain.model.RecommendationWithSnapshot
import java.util.UUID

interface RecommendationService {
    fun initiateRecommendationsTasks(rfqId: UUID)

    fun findAndStoreRecommendations(rfqId: UUID)

    fun triggerRecommendationNotificationsToSuppliers()

    fun getRecommendations(rfqId: UUID, page: Int, pageSize: Int): Pair<List<RecommendationWithSnapshot>, Int>

    fun recordSupplierSelection(rfqId: UUID, supplierId: UUID, reason: String?)
}
