package com.debin.dto

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

data class FundAvailabilityRequest(
    @field:NotNull(message = "Amount is required")
    @field:DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    val amount: BigDecimal,
    
    val accountId: String? = null,
    
    val description: String? = null
)

data class FundAvailabilityResponse(
    val available: Boolean,
    val amount: BigDecimal,
    val accountId: String,
    val currentBalance: BigDecimal
)