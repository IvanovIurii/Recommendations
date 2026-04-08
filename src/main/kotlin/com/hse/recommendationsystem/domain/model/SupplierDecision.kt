package com.hse.recommendationsystem.domain.model

import java.time.Instant
import java.util.UUID

data class SupplierDecision(
    val rfqId: UUID,
    val supplierId: UUID,
    val decisionType: DecisionType,
    val reason: String?,
    val decidedAt: Instant,
)
