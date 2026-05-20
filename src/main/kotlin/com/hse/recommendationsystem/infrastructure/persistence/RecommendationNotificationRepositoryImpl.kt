package com.hse.recommendationsystem.infrastructure.persistence

import com.hse.recommendationsystem.application.domain.model.NotificationStatus
import com.hse.recommendationsystem.application.domain.model.RecommendationNotification
import com.hse.recommendationsystem.application.domain.persistence.RecommendationNotificationRepository
import com.hse.recommendationsystem.infrastructure.persistence.entities.RecommendationNotificationEntity
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class RecommendationNotificationRepositoryImpl(
    private val entityRepository: RecommendationNotificationEntityRepository,
) : RecommendationNotificationRepository {

    override fun save(notification: RecommendationNotification): RecommendationNotification {
        val saved = entityRepository.save(notification.toEntity())
        return saved.toDomain()
    }

    override fun listOnWait(limit: Int): List<RecommendationNotification> =
        entityRepository.findOnWait(limit).map { it.toDomain() }

    override fun updateStatusIfCurrent(
        notificationId: Long,
        expectedCurrent: NotificationStatus,
        newStatus: NotificationStatus,
        modifiedAt: Instant,
    ): Int = entityRepository.updateStatusIfCurrent(
        id = notificationId,
        expectedStatus = expectedCurrent.name,
        newStatus = newStatus.name,
        modifiedAt = modifiedAt,
    )

    private fun RecommendationNotification.toEntity() = RecommendationNotificationEntity(
        id = id,
        rfqId = rfqId,
        supplierId = supplierId,
        unifiedSupplierId = unifiedSupplierId,
        status = status.name,
        createdAt = createdAt,
        modifiedAt = modifiedAt,
    )

    private fun RecommendationNotificationEntity.toDomain() = RecommendationNotification(
        id = id,
        rfqId = rfqId!!,
        supplierId = supplierId!!,
        unifiedSupplierId = unifiedSupplierId,
        status = NotificationStatus.valueOf(status!!),
        createdAt = createdAt!!,
        modifiedAt = modifiedAt,
    )
}
