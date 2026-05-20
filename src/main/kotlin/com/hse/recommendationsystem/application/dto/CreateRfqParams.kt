package com.hse.recommendationsystem.application.dto

import java.util.UUID

data class CreateRfqParams(
    val email: String,
    val fullName: String?,
    val countryCode: String?,
    val userProfileId: UUID?,
    val title: String?,
    val description: String?,
    val deliveryLocation: String?,
    val quantity: String?,
    val supplierTypes: List<String>?,
    val buyerCountry: String?,
    val categoryId: Long?,
)
