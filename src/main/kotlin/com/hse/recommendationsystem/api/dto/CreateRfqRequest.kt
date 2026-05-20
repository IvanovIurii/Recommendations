package com.hse.recommendationsystem.api.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class CreateRfqRequest(
    @field:NotBlank @field:Email
    val email: String,
    val fullName: String? = null,
    val countryCode: String? = null,
    val userProfileId: UUID? = null,
    val title: String? = null,
    val description: String? = null,
    val deliveryLocation: String? = null,
    val quantity: String? = null,
    val supplierTypes: List<String>? = null,
    val buyerCountry: String? = null,
    val categoryId: Long? = null,
)
