package com.hse.recommendationsystem.infrastructure.persistence

import com.hse.recommendationsystem.infrastructure.persistence.entities.RfqUserEntity
import org.springframework.data.repository.CrudRepository

interface RfqUserEntityRepository : CrudRepository<RfqUserEntity, Long>
