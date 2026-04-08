package com.hse.recommendationsystem.domain.repository

import com.hse.recommendationsystem.domain.model.Recommendation
import com.hse.recommendationsystem.domain.model.RecommendationWithSnapshot
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
