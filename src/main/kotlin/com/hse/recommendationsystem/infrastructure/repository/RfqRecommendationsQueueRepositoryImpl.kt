package com.hse.recommendationsystem.infrastructure.repository

import com.hse.recommendationsystem.domain.model.RfqRecommendationQueueEntry
import com.hse.recommendationsystem.domain.model.RfqRecommendationQueueType
import com.hse.recommendationsystem.domain.repository.RfqRecommendationsQueueRepository
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

@Repository
class RfqRecommendationsQueueRepositoryImpl(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
) : RfqRecommendationsQueueRepository {
    override fun enqueue(
        rfqId: UUID,
        queueType: RfqRecommendationQueueType,
    ) {
        namedParameterJdbcTemplate.update(
            """
            INSERT INTO rfq_recommendations_queue (rfq_id, queue_type, created_at, processing_attempts)
            VALUES (:rfqId, :queueType, :createdAt, 0)
            ON CONFLICT (rfq_id, queue_type) DO UPDATE SET
                created_at = EXCLUDED.created_at,
                processing_attempts = 0
            """.trimIndent(),
            MapSqlParameterSource()
                .addValue("rfqId", rfqId)
                .addValue("queueType", queueType.name)
                .addValue("createdAt", Timestamp.from(Instant.now())),
        )
    }

    override fun pollOldest(limit: Int): List<RfqRecommendationQueueEntry> =
        namedParameterJdbcTemplate.query(
            """
            SELECT id, rfq_id, queue_type, created_at, processing_attempts
            FROM rfq_recommendations_queue
            ORDER BY id ASC
            LIMIT :limit
            """.trimIndent(),
            MapSqlParameterSource().addValue("limit", limit),
        ) { rs, _ ->
            RfqRecommendationQueueEntry(
                id = rs.getLong("id"),
                rfqId = rs.getObject("rfq_id", UUID::class.java),
                queueType = RfqRecommendationQueueType.valueOf(rs.getString("queue_type")),
                createdAt = rs.getTimestamp("created_at").toInstant(),
                processingAttempts = rs.getInt("processing_attempts"),
            )
        }

    override fun deleteById(id: Long) {
        namedParameterJdbcTemplate.update(
            "DELETE FROM rfq_recommendations_queue WHERE id = :id",
            MapSqlParameterSource().addValue("id", id),
        )
    }

    override fun recordFailedProcessingAttempt(
        id: Long,
        processingAttempts: Int,
    ) {
        namedParameterJdbcTemplate.update(
            """
            UPDATE rfq_recommendations_queue
            SET processing_attempts = :processingAttempts
            WHERE id = :id
            """.trimIndent(),
            MapSqlParameterSource()
                .addValue("id", id)
                .addValue("processingAttempts", processingAttempts),
        )
    }
}
