package com.hse.recommendationsystem.api

import com.hse.recommendationsystem.api.dto.RecommendationPageResponse
import com.hse.recommendationsystem.api.dto.SelectSupplierRequest
import com.hse.recommendationsystem.api.mapping.toPage
import com.hse.recommendationsystem.domain.service.RecommendationService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/rfq")
class RecommendationController(
    private val recommendationService: RecommendationService,
) {
    @GetMapping("/{rfqId}/recommendations")
    fun getRecommendations(
        @PathVariable rfqId: UUID,
        @RequestParam(name = "page", defaultValue = "0") page: Int,
        @RequestParam(name = "pageSize", defaultValue = "10") pageSize: Int,
    ): RecommendationPageResponse {
        val (rows, total) = recommendationService.getRecommendations(rfqId, page, pageSize)
        return rows.toPage(total, page, pageSize)
    }

    @PostMapping("/{rfqId}/recommendations/{supplierId}/select")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun selectSupplier(
        @PathVariable rfqId: UUID,
        @PathVariable supplierId: UUID,
        @RequestBody(required = false) body: SelectSupplierRequest?,
    ) {
        recommendationService.recordSupplierSelection(
            rfqId = rfqId,
            supplierId = supplierId,
            reason = body?.reason,
        )
    }
}
