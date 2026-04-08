package com.hse.recommendationsystem.domain.repository

import com.hse.recommendationsystem.domain.model.NotificationStatus
import com.hse.recommendationsystem.domain.model.RecommendationNotification
import java.time.Instant

interface RecommendationNotificationRepository {
    fun save(notification: RecommendationNotification): RecommendationNotification

    fun updateStatusIfCurrent(
        notificationId: Long,
        expectedCurrent: NotificationStatus,
        newStatus: NotificationStatus,
        modifiedAt: Instant,
    ): Int
}
