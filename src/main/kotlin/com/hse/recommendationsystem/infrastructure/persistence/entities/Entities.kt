package com.hse.recommendationsystem.infrastructure.persistence.entities

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table("rfq_user")
data class RfqUserEntity(
    @Id
    val rfqUserId: Long? = null,
    val userProfileId: UUID?,
    val email: String,
    val fullName: String?,
    val countryCode: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Table("recommendations_notifications")
data class RecommendationNotificationEntity(
    @Id
    val id: Long? = null,
    val rfqId: UUID?,
    val supplierId: UUID?,
    val unifiedSupplierId: UUID?,
    val status: String?,
    val createdAt: Instant?,
    val modifiedAt: Instant?,
)

@Table("rfq_recommendations_queue")
data class RfqRecommendationsQueueEntity(
    @Id
    val id: Long? = null,
    val rfqId: UUID,
    val queueType: String,
    val createdAt: Instant,
    val processingAttempts: Int = 0,
)
