package com.hse.recommendationsystem.infrastructure.persistence

import com.hse.recommendationsystem.infrastructure.persistence.entities.RecommendationNotificationEntity
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import java.time.Instant

interface RecommendationNotificationEntityRepository : CrudRepository<RecommendationNotificationEntity, Long> {

    @Query(
        """
        SELECT id, rfq_id, supplier_id, unified_supplier_id, status, created_at, modified_at
        FROM recommendations_notifications
        WHERE status = 'ON_WAIT'
        ORDER BY id ASC
        LIMIT :limit
        """,
    )
    fun findOnWait(limit: Int): List<RecommendationNotificationEntity>

    @Modifying
    @Query(
        """
        UPDATE recommendations_notifications
        SET status = :newStatus, modified_at = :modifiedAt
        WHERE id = :id AND status = :expectedStatus
        """,
    )
    fun updateStatusIfCurrent(
        id: Long,
        expectedStatus: String,
        newStatus: String,
        modifiedAt: Instant,
    ): Int
}
