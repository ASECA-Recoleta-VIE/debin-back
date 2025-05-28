package com.debin.dto

import java.time.LocalDateTime

data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val transactionId: String? = null
)

data class TransferResponse(
    val transactionId: String,
    val amount: java.math.BigDecimal,
    val status: String,
    val accountIdentifier: String
)