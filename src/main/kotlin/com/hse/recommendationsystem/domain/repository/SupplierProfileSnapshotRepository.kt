package com.hse.recommendationsystem.domain.repository

import com.hse.recommendationsystem.domain.model.SupplierProfileSnapshot

interface SupplierProfileSnapshotRepository {
    fun save(snapshot: SupplierProfileSnapshot)
}
