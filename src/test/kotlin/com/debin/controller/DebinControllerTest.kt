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
    fun `test receiveMoney success`() {
        // Given
        val request = ReceiveMoneyRequest(
            accountIdentifier = "123456789",
            amount = BigDecimal("100.00"),
            description = "Test money reception",
            senderName = "John Doe",
            senderAccount = "987654321"
        )

        val transferResponse = TransferResponse(
            transactionId = "DEB-123456",
            amount = request.amount,
            status = "COMPLETED",
            accountIdentifier = request.accountIdentifier
        )

        val apiResponse = ApiResponse(
            success = true,
            message = "Money received successfully",
            data = transferResponse,
            timestamp = LocalDateTime.now(),
            transactionId = "DEB-123456"
        )

        `when`(debinService.receiveMoney(request)).thenReturn(apiResponse)

        // When & Then
        mockMvc.perform(
            post("/api/receive")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Money received successfully"))
            .andExpect(jsonPath("$.data.transactionId").value("DEB-123456"))
            .andExpect(jsonPath("$.data.amount").value(100.00))
            .andExpect(jsonPath("$.data.status").value("COMPLETED"))
            .andExpect(jsonPath("$.data.accountIdentifier").value("123456789"))
    }

    @Test
    fun `test requestMoney success`() {
        // Given
        val request = RequestMoneyRequest(
            accountIdentifier = "123456789",
            amount = BigDecimal("100.00"),
            description = "Test money request",
            requesterName = "John Doe",
            requesterAccount = "987654321"
        )

        val apiResponse = ApiResponse<Any>(
            success = true,
            message = "Money request processed successfully",
            data = mapOf(
                "status" to "PENDING",
                "transactionId" to "TEST-123456"
            ),
            timestamp = LocalDateTime.now()
        )

        `when`(debinService.requestMoney(request)).thenReturn(apiResponse)

        // When & Then
        mockMvc.perform(
            post("/api/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Money request processed successfully"))
            .andExpect(jsonPath("$.data.status").value("PENDING"))
            .andExpect(jsonPath("$.data.transactionId").value("TEST-123456"))
    }

    @Test
    fun `test receiveMoney validation error`() {
        // Given - invalid request with missing required fields
        val invalidRequest = mapOf(
            "amount" to 100.00
            // Missing accountIdentifier and description
        )

        // When & Then
        mockMvc.perform(
            post("/api/receive")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `test requestMoney validation error`() {
        // Given - invalid request with missing required fields
        val invalidRequest = mapOf(
            "amount" to 100.00
            // Missing accountIdentifier and description
        )

        // When & Then
        mockMvc.perform(
            post("/api/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
        )
            .andExpect(status().isBadRequest)
    }
}
