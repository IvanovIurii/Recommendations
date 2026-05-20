package com.hse.recommendationsystem.application.domain.persistence

import com.hse.recommendationsystem.application.domain.model.RfqCore
import com.hse.recommendationsystem.application.domain.model.RfqStatus
import java.util.UUID

interface RfqCoreRepository {
    fun save(rfq: RfqCore): RfqCore

    fun findById(rfqId: UUID): RfqCore?

    fun updateStatus(rfqId: UUID, status: RfqStatus): Int
}
