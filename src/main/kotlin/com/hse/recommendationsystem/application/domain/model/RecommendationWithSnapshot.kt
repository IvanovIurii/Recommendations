package com.hse.recommendationsystem.application.domain.model

data class RecommendationWithSnapshot(
    val recommendation: Recommendation,
    val snapshot: SupplierProfileSnapshot,
    val decision: SupplierDecision?,
    val notificationStatus: NotificationStatus?,
)
