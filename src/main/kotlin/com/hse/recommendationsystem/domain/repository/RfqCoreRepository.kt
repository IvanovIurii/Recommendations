package com.hse.recommendationsystem.domain.repository

import com.hse.recommendationsystem.domain.model.RfqCore
import com.hse.recommendationsystem.domain.model.RfqStatus
import java.util.UUID

interface RfqCoreRepository {
    fun save(rfq: RfqCore): RfqCore

    fun findById(rfqId: UUID): RfqCore?

    fun updateStatus(rfqId: UUID, status: RfqStatus): Int
}
