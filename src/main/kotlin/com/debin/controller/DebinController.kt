package com.debin.controller

import com.debin.dto.ApiResponse
import com.debin.dto.FundAvailabilityRequest
import com.debin.dto.FundAvailabilityResponse
import com.debin.dto.TransferResponse
import com.debin.dto.WithdrawRequest
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

        return ResponseEntity.ok(response)
    }

    @PostMapping("/withdraw-from-main")
    fun withdrawFromMainApi(@Valid @RequestBody request: WithdrawRequest): ResponseEntity<ApiResponse<TransferResponse>> {
        logger.info("Received withdraw from main API request for user: ${request.email}, amount: ${request.amount}")

        val response = debinService.withdrawFromMainApi(request)

        return if (response.success) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.badRequest().body(response)
        }
    }
}
