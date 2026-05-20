package com.hse.recommendationsystem.application.domain.persistence

import com.hse.recommendationsystem.application.domain.model.SupplierProfileSnapshot

interface SupplierProfileSnapshotRepository {
    fun save(snapshot: SupplierProfileSnapshot)
}
