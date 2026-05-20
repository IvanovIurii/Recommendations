package com.hse.recommendationsystem.application.domain.persistence

import com.hse.recommendationsystem.application.domain.model.NotificationStatus
import com.hse.recommendationsystem.application.domain.model.RecommendationNotification
import java.time.Instant

interface RecommendationNotificationRepository {
    fun save(notification: RecommendationNotification): RecommendationNotification

    fun listOnWait(limit: Int = 500): List<RecommendationNotification>

    fun updateStatusIfCurrent(
        notificationId: Long,
        expectedCurrent: NotificationStatus,
        newStatus: NotificationStatus,
        modifiedAt: Instant,
    ): Int
}
