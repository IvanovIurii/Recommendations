package com.hse.recommendationsystem.infrastructure.repository

import com.hse.recommendationsystem.domain.model.RfqCore
import com.hse.recommendationsystem.domain.model.RfqStatus
import com.hse.recommendationsystem.domain.repository.RfqCoreRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
class RfqCoreRepositoryImpl(
    private val jdbcTemplate: JdbcTemplate,
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
) : RfqCoreRepository {
    override fun save(rfq: RfqCore): RfqCore {
        jdbcTemplate.update(
            """
            INSERT INTO rfq_core (
                rfq_id, sender_id, title, description, delivery_location, quantity,
                supplier_types, status, buyer_country, category_id, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
        ) { ps ->
            var i = 1
            ps.setObject(i++, rfq.rfqId)
            ps.setLong(i++, rfq.senderId)
            ps.setString(i++, rfq.title)
            ps.setString(i++, rfq.description)
            ps.setString(i++, rfq.deliveryLocation)
            ps.setString(i++, rfq.quantity)
            val types = rfq.supplierTypes?.toTypedArray() ?: emptyArray()
            ps.setArray(i++, ps.connection.createArrayOf("text", types))
            ps.setString(i++, rfq.status.name)
            ps.setString(i++, rfq.buyerCountry)
            if (rfq.categoryId != null) {
                ps.setLong(i++, rfq.categoryId)
            } else {
                ps.setObject(i++, null)
            }
            ps.setTimestamp(i++, java.sql.Timestamp.from(rfq.createdAt))
            ps.setTimestamp(i, java.sql.Timestamp.from(rfq.updatedAt))
        }
        return rfq
    }

    override fun findById(rfqId: UUID): RfqCore? =
        namedParameterJdbcTemplate.query(
            """
            SELECT rfq_id, sender_id, title, description, delivery_location, quantity,
                   supplier_types, status, buyer_country, category_id, created_at, updated_at
            FROM rfq_core WHERE rfq_id = :rfqId
            """.trimIndent(),
            MapSqlParameterSource().addValue("rfqId", rfqId),
            ROW_MAPPER,
        ).firstOrNull()

    override fun updateStatus(
        rfqId: UUID,
        status: RfqStatus,
    ): Int =
        namedParameterJdbcTemplate.update(
            """
            UPDATE rfq_core SET status = :status, updated_at = :updatedAt WHERE rfq_id = :rfqId
            """.trimIndent(),
            MapSqlParameterSource()
                .addValue("rfqId", rfqId)
                .addValue("status", status.name)
                .addValue("updatedAt", java.sql.Timestamp.from(Instant.now())),
        )

    private companion object {
        private val ROW_MAPPER =
            RowMapper { rs, _ ->
                val typesArray = rs.getArray("supplier_types")
                val supplierTypes =
                    typesArray?.array?.let { arr ->
                        (arr as Array<*>).mapNotNull { it as? String }
                    }
                RfqCore(
                    rfqId = rs.getObject("rfq_id", UUID::class.java),
                    senderId = rs.getLong("sender_id"),
                    title = rs.getString("title"),
                    description = rs.getString("description"),
                    deliveryLocation = rs.getString("delivery_location"),
                    quantity = rs.getString("quantity"),
                    supplierTypes = supplierTypes,
                    status = RfqStatus.valueOf(rs.getString("status")),
                    buyerCountry = rs.getString("buyer_country"),
                    categoryId = rs.getObject("category_id") as Long?,
                    createdAt = rs.getTimestamp("created_at").toInstant(),
                    updatedAt = rs.getTimestamp("updated_at").toInstant(),
                )
            }
    }
}
