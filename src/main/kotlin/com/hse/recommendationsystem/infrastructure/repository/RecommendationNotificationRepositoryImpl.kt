package com.hse.recommendationsystem.infrastructure.repository

import com.hse.recommendationsystem.domain.model.NotificationStatus
import com.hse.recommendationsystem.domain.model.RecommendationNotification
import com.hse.recommendationsystem.domain.repository.RecommendationNotificationRepository
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class RecommendationNotificationRepositoryImpl(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
) : RecommendationNotificationRepository {
    override fun save(notification: RecommendationNotification): RecommendationNotification {
        val keyHolder = GeneratedKeyHolder()
        namedParameterJdbcTemplate.update(
            """
            INSERT INTO recommendations_notifications (
                rfq_id, supplier_id, unified_supplier_id, status, created_at, modified_at
            ) VALUES (:rfqId, :supplierId, :unifiedSupplierId, :status, :createdAt, :modifiedAt)
            """.trimIndent(),
            MapSqlParameterSource()
                .addValue("rfqId", notification.rfqId)
                .addValue("supplierId", notification.supplierId)
                .addValue("unifiedSupplierId", notification.unifiedSupplierId)
                .addValue("status", notification.status.name)
                .addValue("createdAt", java.sql.Timestamp.from(notification.createdAt))
                .addValue(
                    "modifiedAt",
                    notification.modifiedAt?.let { java.sql.Timestamp.from(it) },
                ),
            keyHolder,
            arrayOf("id"),
        )
        val id = keyHolder.key?.toLong() ?: error("Failed to obtain recommendations_notifications id")
        return notification.copy(id = id)
    }

    override fun updateStatusIfCurrent(
        notificationId: Long,
        expectedCurrent: NotificationStatus,
        newStatus: NotificationStatus,
        modifiedAt: Instant,
    ): Int =
        namedParameterJdbcTemplate.update(
            """
            UPDATE recommendations_notifications
            SET status = :newStatus, modified_at = :modifiedAt
            WHERE id = :id AND status = :expectedStatus
            """.trimIndent(),
            MapSqlParameterSource()
                .addValue("id", notificationId)
                .addValue("expectedStatus", expectedCurrent.name)
                .addValue("newStatus", newStatus.name)
                .addValue("modifiedAt", java.sql.Timestamp.from(modifiedAt)),
        )
}
