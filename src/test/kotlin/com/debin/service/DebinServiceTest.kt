package com.debin.service

import com.debin.dto.*
import com.debin.model.FakeApiAccount
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
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

    @Mock
    private lateinit var fakeApiAccountService: FakeApiAccountService

    private lateinit var debinService: DebinService

    private val mainApiBaseUrl = "http://localhost:8080"

    @BeforeEach
    fun setup() {
        // Create the service instance manually with the mocked dependencies and test values
        debinService = DebinService(
            restTemplate,
            fakeApiAccountService,
            mainApiBaseUrl,
            "/api/users/login"
        )


        // Set the depositEndpoint field using reflection
        val depositEndpointField = DebinService::class.java.getDeclaredField("depositEndpoint")
        depositEndpointField.isAccessible = true
        depositEndpointField.set(debinService, "/api/deposit")
    }

    @Test
    fun `test checkFundAvailability with sufficient funds`() {
        // Given
        val request = FundAvailabilityRequest(
            amount = BigDecimal("100.00"),
            accountId = "test-account",
            description = "Test fund check"
        )

        // We don't need to mock hasSufficientFunds since it's always true now
        // Use concrete value instead of matcher to avoid Mockito issues
        `when`(fakeApiAccountService.getBalance("test-account")).thenReturn(BigDecimal("500.00"))
        `when`(fakeApiAccountService.accountExists("test-account")).thenReturn(true)

        // When
        val response = debinService.checkFundAvailability(request)

        // Then
        assertTrue(response.success)
        assertEquals("Funds are available", response.message)
        assertNotNull(response.data)
        assertTrue(response.data!!.available)
        assertEquals(request.amount, response.data!!.amount)
        assertEquals(request.accountId, response.data!!.accountId)
        assertEquals(BigDecimal("500.00"), response.data!!.currentBalance)

        // Verify that the service methods were called
        // No need to verify hasSufficientFunds since we don't call it anymore
        // Use concrete value to match the mock setup
        verify(fakeApiAccountService).accountExists("test-account")
        verify(fakeApiAccountService).getBalance("test-account")
    }

    @Test
    fun `test checkFundAvailability with insufficient funds`() {
        // Given
        val request = FundAvailabilityRequest(
            amount = BigDecimal("1000.00"),
            accountId = "test-account",
            description = "Test fund check"
        )

        // Mock the balance to be less than the requested amount
        // Use concrete value instead of matcher to avoid Mockito issues
        `when`(fakeApiAccountService.getBalance("test-account")).thenReturn(BigDecimal("500.00"))
        `when`(fakeApiAccountService.accountExists("test-account")).thenReturn(true)

        // When
        val response = debinService.checkFundAvailability(request)

        // Then
        assertTrue(response.success) // API call is still successful
        // Message should now be "Insufficient funds" since the balance is less than the requested amount
        assertEquals("Insufficient funds", response.message)
        assertNotNull(response.data)
        // Available should now be false since the balance is less than the requested amount
        assertEquals(false, response.data!!.available)
        assertEquals(request.amount, response.data!!.amount)
        assertEquals(request.accountId, response.data!!.accountId)
        assertEquals(BigDecimal("500.00"), response.data!!.currentBalance)

        // Verify that the service methods were called
        // Use concrete value to match the mock setup
        verify(fakeApiAccountService).accountExists("test-account")
        verify(fakeApiAccountService).getBalance("test-account")
    }

    @Test
    fun `test checkFundAvailability with null accountId`() {
        // Given
        val request = FundAvailabilityRequest(
            amount = BigDecimal("100.00"),
            accountId = null,
            description = "Test fund check"
        )

        // When
        val response = debinService.checkFundAvailability(request)

        // Then
        assertEquals(false, response.success)
        assertEquals("Account ID is required", response.message)
        assertEquals(null, response.data)
        assertNotNull(response.transactionId)

        // Verify that no service methods were called
        verifyNoInteractions(fakeApiAccountService)
    }

    @Test
    fun `test checkFundAvailability with non-existent account`() {
        // Given
        val request = FundAvailabilityRequest(
            amount = BigDecimal("100.00"),
            accountId = "non-existent-account",
            description = "Test fund check"
        )

        // Mock the account to not exist
        `when`(fakeApiAccountService.accountExists("non-existent-account")).thenReturn(false)

        // When
        val response = debinService.checkFundAvailability(request)

        // Then
        assertTrue(response.success) // API call is still successful
        assertEquals("Account not found", response.message)
        assertNotNull(response.data)
        assertEquals(false, response.data!!.available)
        assertEquals(request.amount, response.data!!.amount)
        assertEquals(request.accountId, response.data!!.accountId)
        assertEquals(BigDecimal.ZERO, response.data!!.currentBalance)

        // Verify that the service methods were called
        verify(fakeApiAccountService).accountExists("non-existent-account")
        // getBalance should not be called since the account doesn't exist
        verify(fakeApiAccountService, never()).getBalance("non-existent-account")
    }

    // Test for removeFunds removed since this method doesn't exist in the current implementation

    @Test
    fun `test depositToMainApi success`() {
        // Given
        val request = DepositRequest(
            email = "test@example.com",
            amount = BigDecimal("100.00"),
            description = "Test deposit",
            accountId = "test-account"
        )
        `when`(fakeApiAccountService.accountExists("test-account")).thenReturn(true)
        `when`(fakeApiAccountService.getBalance("test-account")).thenReturn(BigDecimal("500.00"))

        // Mock the deposit call to return a successful response
        val depositResponseEntity = ResponseEntity<Map<*, *>>(mapOf("success" to true), HttpStatus.OK)

        `when`(restTemplate.exchange(
            contains("/api/deposit"),
            eq(HttpMethod.POST),
            any(HttpEntity::class.java),
            eq(Map::class.java)
        )).thenReturn(depositResponseEntity)

        // When
        val response = debinService.depositToMainApi(request)

        // Then
        assertTrue(response.success)
        assertEquals("Funds deposited to main API successfully", response.message)
        assertNotNull(response.data)
        assertEquals(request.amount, response.data!!.amount)
        assertEquals("COMPLETED", response.data!!.status)
        assertNotNull(response.data!!.transactionId)
        assertNotNull(response.data!!.accountIdentifier)

        // Verify that the service methods were called
        verify(fakeApiAccountService).accountExists("test-account")
        verify(fakeApiAccountService).getBalance("test-account")
    }

}
