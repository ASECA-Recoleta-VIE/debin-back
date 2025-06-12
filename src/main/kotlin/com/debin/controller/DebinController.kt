package com.debin.controller

import com.debin.dto.ApiResponse
import com.debin.dto.DepositRequest
import com.debin.dto.FundAvailabilityRequest
import com.debin.dto.FundAvailabilityResponse
import com.debin.dto.TransferResponse
import com.debin.service.DebinService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
@Validated
class DebinController(
    private val debinService: DebinService
) {

    private val logger = LoggerFactory.getLogger(DebinController::class.java)

    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf(
            "status" to "UP",
            "service" to "Debin API",
            "timestamp" to java.time.LocalDateTime.now().toString()
        ))
    }

    @PostMapping("/check-funds")
    fun checkFundAvailability(@Valid @RequestBody request: FundAvailabilityRequest): ResponseEntity<ApiResponse<FundAvailabilityResponse>> {
        logger.info("Received fund availability check request for amount: ${request.amount}")

        val response = debinService.checkFundAvailability(request)

        return when (response.message) {
            "Account not found" -> ResponseEntity.notFound().build()
            "Insufficient funds" -> ResponseEntity.status(403).body(response)
            "Account ID is required" -> ResponseEntity.status(400).body(response)
            else -> ResponseEntity.ok(response)
        }
    }

    @PostMapping("/deposit-to-main")
    fun depositToMainApi(@Valid @RequestBody request: DepositRequest): ResponseEntity<ApiResponse<TransferResponse>> {
        logger.info("Received deposit to main API request for user: ${request.email}, amount: ${request.amount}")

        val response = debinService.depositToMainApi(request)

        return when (response.message) {
            "Insufficient funds" -> ResponseEntity.status(403).body(response)
            "Account not found" -> ResponseEntity.status(404).body(response)
            else -> if (response.success) {
                ResponseEntity.ok(response)
            } else {
                ResponseEntity.badRequest().body(response)
            }
        }
    }
}
