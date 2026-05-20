package com.hse.recommendationsystem.api.controller

import com.hse.recommendationsystem.api.dto.RecommendationPageApiResponse
import com.hse.recommendationsystem.api.dto.SelectSupplierRequest
import com.hse.recommendationsystem.application.RecommendationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/rfq")
@CrossOrigin(origins = ["http://localhost:5173"])
class RecommendationController(
    private val recommendationService: RecommendationService,
) {
    @GetMapping("/{rfqId}/recommendations")
    fun getRecommendations(
        @PathVariable rfqId: UUID,
        @RequestParam(name = "page", defaultValue = "0") page: Int,
        @RequestParam(name = "pageSize", defaultValue = "10") pageSize: Int,
    ): ResponseEntity<RecommendationPageApiResponse> {
        val (rows, total) = recommendationService.getRecommendations(rfqId, page, pageSize)
        return ResponseEntity.ok(RecommendationPageApiResponse.from(rows, total, page, pageSize))
    }

    @PostMapping("/{rfqId}/recommendations/{supplierId}/select")
    fun selectSupplier(
        @PathVariable rfqId: UUID,
        @PathVariable supplierId: UUID,
        @RequestBody(required = false) body: SelectSupplierRequest?,
    ): ResponseEntity<Void> {
        recommendationService.recordSupplierSelection(
            rfqId = rfqId,
            supplierId = supplierId,
            reason = body?.reason,
        )
        return ResponseEntity.noContent().build()
    }
}
