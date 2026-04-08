package com.hse.recommendationsystem.infrastructure.repository

import com.hse.recommendationsystem.domain.model.RfqUser
import com.hse.recommendationsystem.domain.repository.RfqUserRepository
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository

@Repository
class RfqUserRepositoryImpl(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
) : RfqUserRepository {
    override fun save(user: RfqUser): RfqUser {
        val keyHolder = GeneratedKeyHolder()
        namedParameterJdbcTemplate.update(
            """
            INSERT INTO rfq_user (user_profile_id, email, full_name, country_code, created_at, updated_at)
            VALUES (:userProfileId, :email, :fullName, :countryCode, :createdAt, :updatedAt)
            """.trimIndent(),
            MapSqlParameterSource()
                .addValue("userProfileId", user.userProfileId)
                .addValue("email", user.email)
                .addValue("fullName", user.fullName)
                .addValue("countryCode", user.countryCode)
                .addValue("createdAt", java.sql.Timestamp.from(user.createdAt))
                .addValue("updatedAt", java.sql.Timestamp.from(user.updatedAt)),
            keyHolder,
            arrayOf("rfq_user_id"),
        )
        val id = keyHolder.key?.toLong() ?: error("Failed to obtain rfq_user_id")
        return user.copy(rfqUserId = id)
    }

    override fun findById(rfqUserId: Long): RfqUser? =
        namedParameterJdbcTemplate.query(
            """
            SELECT rfq_user_id, user_profile_id, email, full_name, country_code, created_at, updated_at
            FROM rfq_user WHERE rfq_user_id = :id
            """.trimIndent(),
            MapSqlParameterSource().addValue("id", rfqUserId),
            ROW_MAPPER,
        ).firstOrNull()

    private companion object {
        private val ROW_MAPPER =
            RowMapper { rs, _ ->
                RfqUser(
                    rfqUserId = rs.getLong("rfq_user_id"),
                    userProfileId = rs.getObject("user_profile_id", java.util.UUID::class.java),
                    email = rs.getString("email"),
                    fullName = rs.getString("full_name"),
                    countryCode = rs.getString("country_code"),
                    createdAt = rs.getTimestamp("created_at").toInstant(),
                    updatedAt = rs.getTimestamp("updated_at").toInstant(),
                )
            }
    }
}
