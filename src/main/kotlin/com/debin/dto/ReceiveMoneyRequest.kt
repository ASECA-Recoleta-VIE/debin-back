package com.debin.dto

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

data class ReceiveMoneyRequest(
    @field:NotBlank(message = "Account identifier is required")
    val accountIdentifier: String,
    
    @field:NotNull(message = "Amount is required")
    @field:DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    val amount: BigDecimal,
    
    @field:NotBlank(message = "Description is required")
    val description: String,
    
    val senderName: String? = null,
    val senderAccount: String? = null
)