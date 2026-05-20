package com.hse.recommendationsystem.application.domain.persistence

import com.hse.recommendationsystem.application.domain.model.Recommendation
import com.hse.recommendationsystem.application.domain.model.RecommendationWithSnapshot
import java.util.UUID

interface RecommendationRepository {
    fun save(recommendation: Recommendation)

    fun deleteByRfqId(rfqId: UUID)

    fun deleteAllDataForRfq(rfqId: UUID)

    fun findWithSnapshotsByRfqId(
        rfqId: UUID,
        page: Int,
        pageSize: Int,
    ): Pair<List<RecommendationWithSnapshot>, Int>

    fun existsByRfqAndSupplier(
        rfqId: UUID,
        supplierId: UUID,
    ): Boolean
}
