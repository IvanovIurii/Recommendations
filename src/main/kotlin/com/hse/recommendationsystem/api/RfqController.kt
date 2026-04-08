package com.hse.recommendationsystem.api

import com.hse.recommendationsystem.api.dto.CreateRfqRequest
import com.hse.recommendationsystem.api.dto.RfqResponse
import com.hse.recommendationsystem.api.mapping.toDomainParams
import com.hse.recommendationsystem.api.mapping.toResponse
import com.hse.recommendationsystem.domain.service.RfqService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/rfq")
class RfqController(
    private val rfqService: RfqService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createRfq(
        @Valid @RequestBody request: CreateRfqRequest,
    ): RfqResponse {
        val rfq = request.toDomainParams()
        return rfqService
            .createRfq(
                email = rfq.email,
                countryCode = rfq.countryCode,
                userProfileId = rfq.userProfileId,
                title = rfq.title,
                description = rfq.description,
                deliveryLocation = rfq.deliveryLocation,
                quantity = rfq.quantity,
                supplierTypes = rfq.supplierTypes,
                buyerCountry = rfq.buyerCountry,
                categoryId = rfq.categoryId,
                fullName = rfq.fullName,
            ).toResponse()
    }

    @PostMapping("/{rfqId}/accept")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun acceptRfq(
        @PathVariable rfqId: UUID,
    ) {
        rfqService.acceptRfq(rfqId)
    }

    @GetMapping("/{rfqId}")
    fun getRfq(
        @PathVariable rfqId: UUID,
    ): RfqResponse = rfqService.getRfq(rfqId).toResponse()
}
