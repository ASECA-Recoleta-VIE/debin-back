package com.debin.controller

import com.debin.dto.ApiResponse
import com.debin.dto.ReceiveMoneyRequest
import com.debin.dto.RequestMoneyRequest
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
    
    @PostMapping("/receive")
    fun receiveMoney(@Valid @RequestBody request: ReceiveMoneyRequest): ResponseEntity<ApiResponse<TransferResponse>> {
        logger.info("Received money reception request for account: ${request.accountIdentifier}")
        
        val response = debinService.receiveMoney(request)
        
        return if (response.success) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.badRequest().body(response)
        }
    }
    
    @PostMapping("/request")
    fun requestMoney(@Valid @RequestBody request: RequestMoneyRequest): ResponseEntity<ApiResponse<Any>> {
        logger.info("Received money request for account: ${request.accountIdentifier}")
        
        val response = debinService.requestMoney(request)
        
        return if (response.success) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.badRequest().body(response)
        }
    }
    
    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf(
            "status" to "UP",
            "service" to "Debin API",
            "timestamp" to java.time.LocalDateTime.now().toString()
        ))
    }
}