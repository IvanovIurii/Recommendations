package com.hse.recommendationsystem.application

import com.hse.recommendationsystem.application.dto.SupplierRecommendationNotificationPayload

fun interface SupplierRecommendationNotificationPublisher {
    fun publish(payload: SupplierRecommendationNotificationPayload)
}
