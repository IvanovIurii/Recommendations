package com.hse.recommendationsystem.application.domain.model

import java.time.Instant
import java.util.UUID

data class SupplierProfileSnapshot(
    val rfqId: UUID,
    val supplierId: UUID,
    val name: String?,
    val website: String?,
    val profileUrl: String?,
    val country: String?,
    val distributionArea: String?,
    val description: String?,
    val descriptionDe: String?,
    val descriptionEn: String?,
    val supplierTypes: List<String>?,
    val products: List<String>?,
    val keywords: List<String>?,
    val productCategories: List<String>?,
    val snapshotAt: Instant,
)
