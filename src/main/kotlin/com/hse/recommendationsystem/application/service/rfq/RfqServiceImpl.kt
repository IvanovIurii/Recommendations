package com.hse.recommendationsystem.application.service.rfq

import com.hse.recommendationsystem.application.RecommendationService
import com.hse.recommendationsystem.application.RfqService
import com.hse.recommendationsystem.application.domain.exception.InvalidRfqStatusException
import com.hse.recommendationsystem.application.domain.exception.RfqNotFoundException
import com.hse.recommendationsystem.application.domain.model.RfqCore
import com.hse.recommendationsystem.application.domain.model.RfqStatus
import com.hse.recommendationsystem.application.domain.model.RfqUser
import com.hse.recommendationsystem.application.domain.persistence.RfqCoreRepository
import com.hse.recommendationsystem.application.domain.persistence.RfqUserRepository
import com.hse.recommendationsystem.infrastructure.pipeline.OnlinePipelineEventTracker
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class RfqServiceImpl(
    private val rfqUserRepository: RfqUserRepository,
    private val rfqCoreRepository: RfqCoreRepository,
    private val recommendationService: RecommendationService,
    private val onlinePipelineEventTracker: OnlinePipelineEventTracker,
) : RfqService {

    @Transactional
    override fun createRfq(
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
        val now = Instant.now()
        val user = rfqUserRepository.save(
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
        val rfq = RfqCore(
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
        val saved = rfqCoreRepository.save(rfq)
        onlinePipelineEventTracker.startSession(rfqId)
        onlinePipelineEventTracker.addEvent(rfqId, "CREATED", "RFQ created: ${title ?: rfqId}", data = mapOf("rfqId" to rfqId.toString(), "title" to title))
        return saved
    }

    @Transactional
    override fun acceptRfq(rfqId: UUID) {
        val rfq = rfqCoreRepository.findById(rfqId)
            ?: throw RfqNotFoundException(rfqId)
        if (rfq.status !in ALLOWED_ACCEPT_STATUSES) {
            throw InvalidRfqStatusException("RFQ cannot be accepted from status ${rfq.status}")
        }
        rfqCoreRepository.updateStatus(rfqId, RfqStatus.ACCEPTED)
        onlinePipelineEventTracker.addEvent(rfqId, "ACCEPTED", "RFQ accepted — triggering recommendation pipeline")
        recommendationService.initiateRecommendationsTasks(rfqId)
    }

    @Transactional(readOnly = true)
    override fun getRfq(rfqId: UUID): RfqCore =
        rfqCoreRepository.findById(rfqId) ?: throw RfqNotFoundException(rfqId)

    companion object {
        private val ALLOWED_ACCEPT_STATUSES = setOf(RfqStatus.CREATED, RfqStatus.READY_FOR_QA)
    }
}
