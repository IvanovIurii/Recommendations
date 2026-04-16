package com.hse.recommendationsystem.domain.repository

import com.hse.recommendationsystem.domain.model.NotificationStatus
import com.hse.recommendationsystem.domain.model.RecommendationNotification
import java.time.Instant

interface RecommendationNotificationRepository {
    fun save(notification: RecommendationNotification): RecommendationNotification

    /** Pending supplier notifications (for batch SNS publish). */
    fun listOnWait(limit: Int = 500): List<RecommendationNotification>

    fun updateStatusIfCurrent(
        notificationId: Long,
        expectedCurrent: NotificationStatus,
        newStatus: NotificationStatus,
        modifiedAt: Instant,
    ): Int
}
