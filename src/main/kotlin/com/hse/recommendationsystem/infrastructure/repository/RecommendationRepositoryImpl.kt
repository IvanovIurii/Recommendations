package com.hse.recommendationsystem.infrastructure.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.hse.recommendationsystem.domain.model.DecisionType
import com.hse.recommendationsystem.domain.model.MatchType
import com.hse.recommendationsystem.domain.model.Recommendation
import com.hse.recommendationsystem.domain.model.RecommendationWithSnapshot
import com.hse.recommendationsystem.domain.model.SupplierDecision
import com.hse.recommendationsystem.domain.model.SupplierProfileSnapshot
import com.hse.recommendationsystem.domain.repository.RecommendationRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class RecommendationRepositoryImpl(
    private val jdbcTemplate: JdbcTemplate,
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
    private val objectMapper: ObjectMapper,
) : RecommendationRepository {
    override fun save(recommendation: Recommendation) {
        jdbcTemplate.update(
            """
            INSERT INTO recommendation (
                rfq_id, supplier_id, unified_supplier_id, match_type, model_version,
                customer_in_need, is_customer, raw_recommendation_json, recommended_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
        ) { ps ->
            var i = 1
            ps.setObject(i++, recommendation.rfqId)
            ps.setObject(i++, recommendation.supplierId)
            ps.setObject(i++, recommendation.unifiedSupplierId)
            ps.setString(i++, recommendation.matchType?.databaseValue)
            ps.setString(i++, recommendation.modelVersion)
            ps.setObject(i++, recommendation.customerInNeed)
            ps.setObject(i++, recommendation.isCustomer)
            val json = JsonbSupport.toPgObject(objectMapper, recommendation.rawRecommendationJson)
            ps.setObject(i++, json)
            ps.setTimestamp(i, java.sql.Timestamp.from(recommendation.recommendedAt))
        }
    }

    override fun deleteByRfqId(rfqId: UUID) {
        namedParameterJdbcTemplate.update(
            "DELETE FROM recommendation WHERE rfq_id = :rfqId",
            MapSqlParameterSource().addValue("rfqId", rfqId),
        )
    }

    override fun deleteAllDataForRfq(rfqId: UUID) {
        val params = MapSqlParameterSource().addValue("rfqId", rfqId)
        namedParameterJdbcTemplate.update(
            "DELETE FROM recommendations_notifications WHERE rfq_id = :rfqId",
            params,
        )
        namedParameterJdbcTemplate.update(
            "DELETE FROM decision WHERE rfq_id = :rfqId",
            params,
        )
        namedParameterJdbcTemplate.update(
            "DELETE FROM supplier_profile_snapshot WHERE rfq_id = :rfqId",
            params,
        )
        namedParameterJdbcTemplate.update(
            "DELETE FROM recommendation WHERE rfq_id = :rfqId",
            params,
        )
    }

    override fun findWithSnapshotsByRfqId(
        rfqId: UUID,
        page: Int,
        pageSize: Int,
    ): Pair<List<RecommendationWithSnapshot>, Int> {
        val total =
            (
                namedParameterJdbcTemplate.queryForObject(
                    """
                SELECT COUNT(*) FROM recommendation r WHERE r.rfq_id = :rfqId
                """.trimIndent(),
                    MapSqlParameterSource().addValue("rfqId", rfqId),
                    Long::class.java,
                ) ?: 0L
            ).toInt()

        val offset = page.coerceAtLeast(0) * pageSize.coerceAtLeast(1)
        val limit = pageSize.coerceAtLeast(1)

        val rows =
            namedParameterJdbcTemplate.query(
                """
                SELECT
                  r.rfq_id, r.supplier_id, r.unified_supplier_id, r.match_type, r.model_version,
                  r.customer_in_need, r.is_customer, r.raw_recommendation_json::text, r.recommended_at,
                  s.name, s.website, s.profile_url, s.country, s.distribution_area,
                  s.description, s.description_de, s.description_en,
                  s.supplier_types, s.products, s.keywords, s.product_categories, s.snapshot_at,
                  d.decision_type, d.reason, d.decided_at
                FROM recommendation r
                INNER JOIN supplier_profile_snapshot s
                  ON s.rfq_id = r.rfq_id AND s.supplier_id = r.supplier_id
                LEFT JOIN decision d
                  ON d.rfq_id = r.rfq_id AND d.supplier_id = r.supplier_id
                WHERE r.rfq_id = :rfqId
                ORDER BY r.recommended_at ASC, r.supplier_id
                LIMIT :limit OFFSET :offset
                """.trimIndent(),
                MapSqlParameterSource()
                    .addValue("rfqId", rfqId)
                    .addValue("limit", limit)
                    .addValue("offset", offset),
                ROW_MAPPER(objectMapper),
            )
        return rows to total
    }

    override fun existsByRfqAndSupplier(
        rfqId: UUID,
        supplierId: UUID,
    ): Boolean {
        val count =
            namedParameterJdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM recommendation WHERE rfq_id = :rfqId AND supplier_id = :supplierId
                """.trimIndent(),
                MapSqlParameterSource()
                    .addValue("rfqId", rfqId)
                    .addValue("supplierId", supplierId),
                Long::class.java,
            ) ?: 0L
        return count > 0
    }

    private class ROW_MAPPER(
        private val objectMapper: ObjectMapper,
    ) : RowMapper<RecommendationWithSnapshot> {
        override fun mapRow(
            rs: java.sql.ResultSet,
            rowNum: Int,
        ): RecommendationWithSnapshot {
            val rfqId = rs.getObject("rfq_id", UUID::class.java)
            val supplierId = rs.getObject("supplier_id", UUID::class.java)
            val rawJson = rs.getString("raw_recommendation_json")
            val recommendation =
                Recommendation(
                    rfqId = rfqId,
                    supplierId = supplierId,
                    unifiedSupplierId = rs.getObject("unified_supplier_id", UUID::class.java),
                    matchType = MatchType.fromDatabase(rs.getString("match_type")),
                    modelVersion = rs.getString("model_version"),
                    customerInNeed = rs.getObject("customer_in_need") as Boolean?,
                    isCustomer = rs.getObject("is_customer") as Boolean?,
                    rawRecommendationJson = JsonbSupport.parseJson(objectMapper, rawJson),
                    recommendedAt = rs.getTimestamp("recommended_at").toInstant(),
                )
            val snapshot =
                SupplierProfileSnapshot(
                    rfqId = rfqId,
                    supplierId = supplierId,
                    name = rs.getString("name"),
                    website = rs.getString("website"),
                    profileUrl = rs.getString("profile_url"),
                    country = rs.getString("country"),
                    distributionArea = rs.getString("distribution_area"),
                    description = rs.getString("description"),
                    descriptionDe = rs.getString("description_de"),
                    descriptionEn = rs.getString("description_en"),
                    supplierTypes = readTextArray(rs, "supplier_types"),
                    products = readTextArray(rs, "products"),
                    keywords = readTextArray(rs, "keywords"),
                    productCategories = readTextArray(rs, "product_categories"),
                    snapshotAt = rs.getTimestamp("snapshot_at").toInstant(),
                )
            val decisionTypeStr = rs.getString("decision_type")
            val decision =
                if (decisionTypeStr != null) {
                    SupplierDecision(
                        rfqId = rfqId,
                        supplierId = supplierId,
                        decisionType = DecisionType.valueOf(decisionTypeStr),
                        reason = rs.getString("reason"),
                        decidedAt = rs.getTimestamp("decided_at").toInstant(),
                    )
                } else {
                    null
                }
            return RecommendationWithSnapshot(
                recommendation = recommendation,
                snapshot = snapshot,
                decision = decision,
            )
        }

        private fun readTextArray(
            rs: java.sql.ResultSet,
            column: String,
        ): List<String>? {
            val arr = rs.getArray(column) ?: return null
            return (arr.array as Array<*>).mapNotNull { it as? String }
        }
    }
}
