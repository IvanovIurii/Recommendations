package com.hse.recommendationsystem.domain.model

data class RecommendationWithSnapshot(
    val recommendation: Recommendation,
    val snapshot: SupplierProfileSnapshot,
    val decision: SupplierDecision?,
)
