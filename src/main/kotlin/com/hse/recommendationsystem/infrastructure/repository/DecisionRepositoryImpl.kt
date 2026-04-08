package com.hse.recommendationsystem.infrastructure.repository

import com.hse.recommendationsystem.domain.model.DecisionType
import com.hse.recommendationsystem.domain.model.SupplierDecision
import com.hse.recommendationsystem.domain.repository.DecisionRepository
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class DecisionRepositoryImpl(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
) : DecisionRepository {
    override fun save(decision: SupplierDecision) {
        namedParameterJdbcTemplate.update(
            """
            INSERT INTO decision (rfq_id, supplier_id, decision_type, reason, decided_at)
            VALUES (:rfqId, :supplierId, :decisionType, :reason, :decidedAt)
            ON CONFLICT (rfq_id, supplier_id) DO UPDATE SET
                decision_type = EXCLUDED.decision_type,
                reason = EXCLUDED.reason,
                decided_at = EXCLUDED.decided_at
            """.trimIndent(),
            MapSqlParameterSource()
                .addValue("rfqId", decision.rfqId)
                .addValue("supplierId", decision.supplierId)
                .addValue("decisionType", decision.decisionType.name)
                .addValue("reason", decision.reason)
                .addValue("decidedAt", java.sql.Timestamp.from(decision.decidedAt)),
        )
    }

    override fun findByRfqAndSupplier(
        rfqId: UUID,
        supplierId: UUID,
    ): SupplierDecision? =
        namedParameterJdbcTemplate.query(
            """
            SELECT rfq_id, supplier_id, decision_type, reason, decided_at
            FROM decision WHERE rfq_id = :rfqId AND supplier_id = :supplierId
            """.trimIndent(),
            MapSqlParameterSource()
                .addValue("rfqId", rfqId)
                .addValue("supplierId", supplierId),
            ROW_MAPPER,
        ).firstOrNull()

    private companion object {
        private val ROW_MAPPER =
            RowMapper { rs, _ ->
                SupplierDecision(
                    rfqId = rs.getObject("rfq_id", UUID::class.java),
                    supplierId = rs.getObject("supplier_id", UUID::class.java),
                    decisionType = DecisionType.valueOf(rs.getString("decision_type")),
                    reason = rs.getString("reason"),
                    decidedAt = rs.getTimestamp("decided_at").toInstant(),
                )
            }
    }
}
