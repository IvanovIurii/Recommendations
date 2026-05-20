package com.hse.recommendationsystem.application.domain.persistence

import com.hse.recommendationsystem.application.domain.model.RfqUser

interface RfqUserRepository {
    fun save(user: RfqUser): RfqUser

    fun findById(rfqUserId: Long): RfqUser?
}
