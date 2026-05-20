package com.hse.recommendationsystem.api.controller

import com.hse.recommendationsystem.api.dto.CreateRfqRequest
import com.hse.recommendationsystem.api.dto.RfqApiResponse
import com.hse.recommendationsystem.application.RfqService
import com.hse.recommendationsystem.application.dto.CreateRfqParams
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/rfq")
@CrossOrigin(origins = ["http://localhost:5173"])
class RfqController(
    private val rfqService: RfqService,
) {
    @PostMapping
    fun createRfq(
        @Valid @RequestBody request: CreateRfqRequest,
    ): ResponseEntity<RfqApiResponse> {
        val params = request.toParams()
        val rfq = rfqService.createRfq(
            email = params.email,
            countryCode = params.countryCode,
            userProfileId = params.userProfileId,
            title = params.title,
            description = params.description,
            deliveryLocation = params.deliveryLocation,
            quantity = params.quantity,
            supplierTypes = params.supplierTypes,
            buyerCountry = params.buyerCountry,
            categoryId = params.categoryId,
            fullName = params.fullName,
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(RfqApiResponse.from(rfq))
    }

    @PostMapping("/{rfqId}/accept")
    fun acceptRfq(
        @PathVariable rfqId: UUID,
    ): ResponseEntity<Void> {
        rfqService.acceptRfq(rfqId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{rfqId}")
    fun getRfq(
        @PathVariable rfqId: UUID,
    ): ResponseEntity<RfqApiResponse> {
        val rfq = rfqService.getRfq(rfqId)
        return ResponseEntity.ok(RfqApiResponse.from(rfq))
    }

    private fun CreateRfqRequest.toParams() = CreateRfqParams(
        email = email,
        fullName = fullName,
        countryCode = countryCode,
        userProfileId = userProfileId,
        title = title,
        description = description,
        deliveryLocation = deliveryLocation,
        quantity = quantity,
        supplierTypes = supplierTypes,
        buyerCountry = buyerCountry,
        categoryId = categoryId,
    )
}
