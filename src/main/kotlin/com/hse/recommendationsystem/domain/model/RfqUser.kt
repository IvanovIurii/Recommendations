package com.hse.recommendationsystem.domain.model

import java.time.Instant
import java.util.UUID

data class RfqUser(
    val rfqUserId: Long? = null,
    val userProfileId: UUID?,
    val email: String,
    val fullName: String?,
    val countryCode: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)
