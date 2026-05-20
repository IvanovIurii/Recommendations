package com.hse.recommendationsystem.infrastructure.persistence

import com.hse.recommendationsystem.application.domain.model.RfqUser
import com.hse.recommendationsystem.application.domain.persistence.RfqUserRepository
import com.hse.recommendationsystem.infrastructure.persistence.entities.RfqUserEntity
import org.springframework.stereotype.Repository

@Repository
class RfqUserRepositoryImpl(
    private val rfqUserEntityRepository: RfqUserEntityRepository,
) : RfqUserRepository {

    override fun save(user: RfqUser): RfqUser {
        val saved = rfqUserEntityRepository.save(user.toEntity())
        return saved.toDomain()
    }

    override fun findById(rfqUserId: Long): RfqUser? =
        rfqUserEntityRepository.findById(rfqUserId).orElse(null)?.toDomain()

    private fun RfqUser.toEntity() = RfqUserEntity(
        rfqUserId = rfqUserId,
        userProfileId = userProfileId,
        email = email,
        fullName = fullName,
        countryCode = countryCode,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun RfqUserEntity.toDomain() = RfqUser(
        rfqUserId = rfqUserId,
        userProfileId = userProfileId,
        email = email,
        fullName = fullName,
        countryCode = countryCode,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
