package com.hse.recommendationsystem.domain.service

import com.hse.recommendationsystem.domain.model.RfqCore
import com.hse.recommendationsystem.domain.model.RfqRecommendationQueueType
import com.hse.recommendationsystem.domain.model.RfqStatus
import com.hse.recommendationsystem.domain.model.RfqUser
import com.hse.recommendationsystem.domain.repository.RfqCoreRepository
import com.hse.recommendationsystem.domain.repository.RfqRecommendationsQueueRepository
import com.hse.recommendationsystem.domain.repository.RfqUserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class RfqService(
    private val rfqUserRepository: RfqUserRepository,
    private val rfqCoreRepository: RfqCoreRepository,
    private val rfqRecommendationsQueueRepository: RfqRecommendationsQueueRepository,
) {
    @Transactional
    fun createRfq(
        email: String,
        fullName: String?,
        countryCode: String?,
        userProfileId: UUID?,
        title: String?,
        description: String?,
        deliveryLocation: String?,
        quantity: String?,
        supplierTypes: List<String>?,
        buyerCountry: String?,
        categoryId: Long?,
    ): RfqCore {
        // todo: better use clock
        val now = Instant.now()
        val user =
            rfqUserRepository.save(
                RfqUser(
                    userProfileId = userProfileId,
                    email = email,
                    fullName = fullName,
                    countryCode = countryCode,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        val rfqId = UUID.randomUUID()
        val rfq =
            RfqCore(
                rfqId = rfqId,
                senderId = user.rfqUserId!!,
                title = title,
                description = description,
                deliveryLocation = deliveryLocation,
                quantity = quantity,
                supplierTypes = supplierTypes,
                status = RfqStatus.CREATED,
                buyerCountry = buyerCountry,
                categoryId = categoryId,
                createdAt = now,
                updatedAt = now,
            )
        return rfqCoreRepository.save(rfq)
    }

    @Transactional
    fun acceptRfq(rfqId: UUID) {
        val rfq =
            rfqCoreRepository.findById(rfqId)
                ?: error("RFQ not found: $rfqId")
        if (rfq.status !in ALLOWED_ACCEPT_STATUSES) {
            error("RFQ cannot be accepted from status ${rfq.status}")
        }
        rfqCoreRepository.updateStatus(rfqId, RfqStatus.ACCEPTED)
        rfqRecommendationsQueueRepository.enqueue(rfqId, RfqRecommendationQueueType.CUSTOMER)
    }

    @Transactional(readOnly = true)
    fun getRfq(rfqId: UUID): RfqCore =
        rfqCoreRepository.findById(rfqId) ?: error("RFQ not found: $rfqId")

    companion object {
        private val ALLOWED_ACCEPT_STATUSES = setOf(RfqStatus.CREATED, RfqStatus.READY_FOR_QA)
    }
}
