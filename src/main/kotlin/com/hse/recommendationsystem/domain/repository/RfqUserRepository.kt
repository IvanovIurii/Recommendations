package com.hse.recommendationsystem.domain.repository

import com.hse.recommendationsystem.domain.model.RfqUser

interface RfqUserRepository {
    fun save(user: RfqUser): RfqUser

    fun findById(rfqUserId: Long): RfqUser?
}
