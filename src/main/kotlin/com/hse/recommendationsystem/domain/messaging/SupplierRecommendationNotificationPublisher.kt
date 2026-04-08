package com.hse.recommendationsystem.domain.messaging

fun interface SupplierRecommendationNotificationPublisher {
    fun publish(payload: SupplierRecommendationNotificationPayload)
}
