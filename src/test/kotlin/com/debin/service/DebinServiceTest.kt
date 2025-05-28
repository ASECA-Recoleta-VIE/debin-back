package com.debin.service

import com.debin.dto.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class DebinServiceTest {

    @Mock
    private lateinit var restTemplate: RestTemplate

    private lateinit var debinService: DebinService

    private val mainApiBaseUrl = "http://localhost:8080"
    private val transferEndpoint = "/api/transfer"

    @BeforeEach
    fun setup() {
        // Create the service instance manually with the mocked RestTemplate and test values
        debinService = DebinService(restTemplate, mainApiBaseUrl, transferEndpoint)
    }

    @Test
    fun `test receiveMoney success or failure`() {
        // Given
        val request = ReceiveMoneyRequest(
            accountIdentifier = "123456789",
            amount = BigDecimal("100.00"),
            description = "Test money reception",
            senderName = "John Doe",
            senderAccount = "987654321"
        )

        // When
        val response = debinService.receiveMoney(request)

        // Then
        if (response.success) {
            // Success case
            assertEquals("Money received successfully", response.message)
            assertNotNull(response.data)
            assertEquals(request.accountIdentifier, response.data?.accountIdentifier)
            assertEquals(request.amount, response.data?.amount)
            assertEquals("COMPLETED", response.data?.status)
            assertNotNull(response.transactionId)
        } else {
            // Failure case
            assertEquals("Failed to process money reception. Please try again later.", response.message)
            assertEquals(null, response.data)
        }
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

        val mockResponseBody = mapOf(
            "status" to "PENDING",
            "transactionId" to "TEST-123456"
        )

        val mockResponse = ResponseEntity(mockResponseBody, HttpStatus.OK)

        `when`(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity::class.java),
            eq(Map::class.java)
        )).thenReturn(mockResponse as ResponseEntity<Map<*, *>>)

        // When
        val response = debinService.requestMoney(request)

        // Then
        assertTrue(response.success)
        assertEquals("Money request processed successfully", response.message)
        assertEquals(mockResponseBody, response.data)

        // Verify that restTemplate.exchange was called with the correct URL
        verify(restTemplate).exchange(
            eq("$mainApiBaseUrl$transferEndpoint"),
            eq(HttpMethod.POST),
            any(HttpEntity::class.java),
            eq(Map::class.java)
        )
    }
}
