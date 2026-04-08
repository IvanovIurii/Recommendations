package com.hse.recommendationsystem.domain.repository

import com.hse.recommendationsystem.domain.model.SupplierDecision
import java.util.UUID

interface DecisionRepository {
    fun save(decision: SupplierDecision)

    fun findByRfqAndSupplier(
        rfqId: UUID,
        supplierId: UUID,
    ): SupplierDecision?
}
