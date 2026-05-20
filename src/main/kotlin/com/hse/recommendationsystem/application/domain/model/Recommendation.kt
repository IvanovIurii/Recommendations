package com.hse.recommendationsystem.application.domain.model

import com.fasterxml.jackson.databind.JsonNode
import java.time.Instant
import java.util.UUID

data class Recommendation(
    val rfqId: UUID,
    val supplierId: UUID,
    val unifiedSupplierId: UUID?,
    val matchType: MatchType?,
    val modelVersion: String?,
    val customerInNeed: Boolean?,
    val isCustomer: Boolean?,
    val rawRecommendationJson: JsonNode?,
    val recommendedAt: Instant,
)
