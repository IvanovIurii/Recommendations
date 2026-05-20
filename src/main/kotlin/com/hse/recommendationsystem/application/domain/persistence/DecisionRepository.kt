package com.hse.recommendationsystem.application.domain.persistence

import com.hse.recommendationsystem.application.domain.model.SupplierDecision
import java.util.UUID

interface DecisionRepository {
    fun save(decision: SupplierDecision)

    fun findByRfqAndSupplier(
        rfqId: UUID,
        supplierId: UUID,
    ): SupplierDecision?
}
