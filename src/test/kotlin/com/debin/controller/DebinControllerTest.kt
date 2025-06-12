package com.debin.controller

import com.debin.dto.*
import com.debin.service.DebinService
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.math.BigDecimal
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class DebinControllerTest {

    private lateinit var mockMvc: MockMvc

    private lateinit var objectMapper: ObjectMapper

    @Mock
    private lateinit var debinService: DebinService

    @BeforeEach
    fun setup() {
        objectMapper = ObjectMapper()
        mockMvc = MockMvcBuilders.standaloneSetup(DebinController(debinService)).build()
    }

    @Test
    fun `test health endpoint`() {
        mockMvc.perform(get("/api/health"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.service").value("Debin API"))
            .andExpect(jsonPath("$.timestamp").exists())
    }

    @Test
    fun `test checkFundAvailability success`() {
        // Given
        val request = FundAvailabilityRequest(
            amount = BigDecimal("100.00"),
            accountId = "test-account",
            description = "Test fund check"
        )

        val fundAvailabilityResponse = FundAvailabilityResponse(
            available = true,
            amount = request.amount,
            accountId = request.accountId!!,
            currentBalance = BigDecimal("500.00")
        )

        val apiResponse = ApiResponse(
            success = true,
            message = "Funds are available",
            data = fundAvailabilityResponse,
            timestamp = LocalDateTime.now(),
            transactionId = "DEB-123456"
        )

        `when`(debinService.checkFundAvailability(request)).thenReturn(apiResponse)

        // When & Then
        mockMvc.perform(
            post("/api/check-funds")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Funds are available"))
            .andExpect(jsonPath("$.data.available").value(true))
            .andExpect(jsonPath("$.data.amount").value(100.00))
            .andExpect(jsonPath("$.data.accountId").value("test-account"))
            .andExpect(jsonPath("$.data.currentBalance").value(500.00))
    }

    @Test
    fun `test checkFundAvailability insufficient funds`() {
        // Given
        val request = FundAvailabilityRequest(
            amount = BigDecimal("1000.00"),
            accountId = "test-account",
            description = "Test fund check"
        )

        val fundAvailabilityResponse = FundAvailabilityResponse(
            available = false,
            amount = request.amount,
            accountId = request.accountId!!,
            currentBalance = BigDecimal("500.00")
        )

        val apiResponse = ApiResponse(
            success = true,
            message = "Insufficient funds",
            data = fundAvailabilityResponse,
            timestamp = LocalDateTime.now(),
            transactionId = "DEB-123456"
        )

        `when`(debinService.checkFundAvailability(request)).thenReturn(apiResponse)

        // When & Then
        mockMvc.perform(
            post("/api/check-funds")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Insufficient funds"))
            .andExpect(jsonPath("$.data.available").value(false))
            .andExpect(jsonPath("$.data.amount").value(1000.00))
            .andExpect(jsonPath("$.data.accountId").value("test-account"))
            .andExpect(jsonPath("$.data.currentBalance").value(500.00))
    }

    @Test
    fun `test checkFundAvailability account not found`() {
        // Given
        val request = FundAvailabilityRequest(
            amount = BigDecimal("100.00"),
            accountId = "non-existent-account",
            description = "Test fund check"
        )

        val fundAvailabilityResponse = FundAvailabilityResponse(
            available = false,
            amount = request.amount,
            accountId = request.accountId!!,
            currentBalance = BigDecimal.ZERO
        )

        val apiResponse = ApiResponse(
            success = true,
            message = "Account not found",
            data = fundAvailabilityResponse,
            timestamp = LocalDateTime.now(),
            transactionId = "DEB-123456"
        )

        `when`(debinService.checkFundAvailability(request)).thenReturn(apiResponse)

        // When & Then
        mockMvc.perform(
            post("/api/check-funds")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isNotFound)
    }

    // Test for removeFunds removed since this method doesn't exist in the current implementation

    @Test
    fun `test checkFundAvailability missing accountId`() {
        // Given
        val request = FundAvailabilityRequest(
            amount = BigDecimal("100.00"),
            accountId = null,
            description = "Test fund check"
        )

        val apiResponse = ApiResponse<FundAvailabilityResponse>(
            success = false,
            message = "Account ID is required",
            data = null,
            timestamp = LocalDateTime.now(),
            transactionId = "DEB-123456"
        )

        `when`(debinService.checkFundAvailability(request)).thenReturn(apiResponse)

        // When & Then
        mockMvc.perform(
            post("/api/check-funds")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Account ID is required"))
    }

    @Test
    fun `test depositToMainApi success`() {
        // Given
        val request = DepositRequest(
            email = "test@example.com",
            amount = BigDecimal("100.00"),
            description = "Test deposit",
            password = "password123",
            accountId = "test-account"
        )

        val transferResponse = TransferResponse(
            transactionId = "DEB-123456",
            amount = request.amount,
            status = "COMPLETED",
            accountIdentifier = "test-account"
        )

        val apiResponse = ApiResponse(
            success = true,
            message = "Funds deposited to main API successfully",
            data = transferResponse,
            timestamp = LocalDateTime.now(),
            transactionId = "DEB-123456"
        )

        `when`(debinService.depositToMainApi(request)).thenReturn(apiResponse)

        // When & Then
        mockMvc.perform(
            post("/api/deposit-to-main")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Funds deposited to main API successfully"))
            .andExpect(jsonPath("$.data.transactionId").value("DEB-123456"))
            .andExpect(jsonPath("$.data.amount").value(100.00))
            .andExpect(jsonPath("$.data.status").value("COMPLETED"))
            .andExpect(jsonPath("$.data.accountIdentifier").value("test-account"))
    }

    @Test
    fun `test depositToMainApi insufficient funds`() {
        // Given
        val request = DepositRequest(
            email = "test@example.com",
            amount = BigDecimal("1000.00"),
            description = "Test deposit",
            password = "password123",
            accountId = "test-account"
        )

        val apiResponse = ApiResponse<TransferResponse>(
            success = false,
            message = "Insufficient funds",
            data = null,
            timestamp = LocalDateTime.now(),
            transactionId = "DEB-123456"
        )

        `when`(debinService.depositToMainApi(request)).thenReturn(apiResponse)

        // When & Then
        mockMvc.perform(
            post("/api/deposit-to-main")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Insufficient funds"))
    }

    @Test
    fun `test depositToMainApi account not found`() {
        // Given
        val request = DepositRequest(
            email = "test@example.com",
            amount = BigDecimal("100.00"),
            description = "Test deposit",
            password = "password123",
            accountId = "test-account"
        )

        val apiResponse = ApiResponse<TransferResponse>(
            success = false,
            message = "Account not found",
            data = null,
            timestamp = LocalDateTime.now(),
            transactionId = "DEB-123456"
        )

        `when`(debinService.depositToMainApi(request)).thenReturn(apiResponse)

        // When & Then
        mockMvc.perform(
            post("/api/deposit-to-main")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Account not found"))
    }
}
