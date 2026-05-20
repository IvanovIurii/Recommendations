package com.hse.recommendationsystem.infrastructure.persistence

import com.hse.recommendationsystem.infrastructure.persistence.entities.RfqRecommendationsQueueEntity
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import java.time.Instant
import java.util.UUID

interface RfqRecommendationsQueueEntityRepository : CrudRepository<RfqRecommendationsQueueEntity, Long> {

    @Modifying
    @Query(
        """
        INSERT INTO rfq_recommendations_queue (rfq_id, queue_type, created_at, processing_attempts)
        VALUES (:rfqId, :queueType, :createdAt, 0)
        ON CONFLICT (rfq_id, queue_type) DO UPDATE SET
            created_at = EXCLUDED.created_at,
            processing_attempts = 0
        """,
    )
    fun enqueue(rfqId: UUID, queueType: String, createdAt: Instant)

    @Query(
        """
        SELECT id, rfq_id, queue_type, created_at, processing_attempts
        FROM rfq_recommendations_queue
        ORDER BY id ASC
        LIMIT :batchLimit
        """,
    )
    fun pollOldest(batchLimit: Int): List<RfqRecommendationsQueueEntity>

    @Modifying
    @Query(
        """
        UPDATE rfq_recommendations_queue
        SET processing_attempts = :processingAttempts
        WHERE id = :id
        """,
    )
    fun recordFailedProcessingAttempt(id: Long, processingAttempts: Int)
}
