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

    // Test for removeFunds removed since this method doesn't exist in the current implementation

    @Test
    fun `test withdrawFromMainApi success`() {
        // Given
        val request = WithdrawRequest(
            email = "test@example.com",
            amount = BigDecimal("100.00"),
            description = "Test withdrawal",
            password = "password123"
        )

        val transferResponse = TransferResponse(
            transactionId = "DEB-123456",
            amount = request.amount,
            status = "COMPLETED",
            accountIdentifier = "test-account"
        )

        val apiResponse = ApiResponse(
            success = true,
            message = "Funds withdrawn from main API and deposited to fake API successfully",
            data = transferResponse,
            timestamp = LocalDateTime.now(),
            transactionId = "DEB-123456"
        )

        `when`(debinService.withdrawFromMainApi(request)).thenReturn(apiResponse)

        // When & Then
        mockMvc.perform(
            post("/api/withdraw-from-main")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Funds withdrawn from main API and deposited to fake API successfully"))
            .andExpect(jsonPath("$.data.transactionId").value("DEB-123456"))
            .andExpect(jsonPath("$.data.amount").value(100.00))
            .andExpect(jsonPath("$.data.status").value("COMPLETED"))
            .andExpect(jsonPath("$.data.accountIdentifier").value("test-account"))
    }
}
