package com.debin.service

import com.debin.dto.*
import jakarta.websocket.Endpoint
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.math.BigDecimal
import java.util.*
import java.util.Base64

@Service
class DebinService(
    @Autowired private val restTemplate: RestTemplate,
    @Autowired private val fakeApiAccountService: FakeApiAccountService,
    @Value("\${main-api.base-url}") private val mainApiBaseUrl: String,
    @Value("\${main-api.auth.endpoint}") private val authEndpoint: String
) {

    private val logger = LoggerFactory.getLogger(DebinService::class.java)

    @Value("\${main-api.endpoints.deposit}")
    private lateinit var depositEndpoint: String

    private fun generateTransactionId(): String {
        return "DEB-${System.currentTimeMillis()}-${Random().nextInt(1000, 9999)}"
    }

    fun authenticate(authRequest: AuthRequest): ApiResponse<AuthResponse> {
        logger.info("Authenticating with main API for user: ${authRequest.email}")

        return try {
            val url = "$mainApiBaseUrl$authEndpoint"

            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }

            val httpEntity = HttpEntity(authRequest, headers)

            logger.info("Calling main API authentication endpoint at: $url")

            val response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                httpEntity,
                String::class.java
            )

            logger.info("Main API authentication response status: ${response.statusCode}")

            if (response.statusCode.is2xxSuccessful) {
                try {
                    val cookies = response.headers.getFirst("Set-Cookie")
                    if (cookies != null && cookies.contains("token=")) {
                        val tokenValue = cookies.substringAfter("token=").substringBefore(";")

                        val authResponse = AuthResponse(
                            token = tokenValue,
                            expiresIn = 7200,
                            tokenType = "Bearer"
                        )

                        return ApiResponse(
                            success = true,
                            message = "Authentication successful (cookie)",
                            data = authResponse
                        )
                    }

                    val responseBody = response.body
                    if (responseBody != null && responseBody.contains("token")) {
                        val tokenValue = responseBody.substringAfter("\"token\":\"").substringBefore("\"")

                        if (tokenValue.isNotEmpty()) {
                            val authResponse = AuthResponse(
                                token = tokenValue,
                                expiresIn = 7200,
                                tokenType = "Bearer"
                            )

                            return ApiResponse(
                                success = true,
                                message = "Authentication successful (body)",
                                data = authResponse
                            )
                        }
                    }

                    logger.warn("No se encontró token en respuesta, usando credenciales como token")
                    val tokenValue = "${authRequest.email}:${authRequest.password}"

                    val authResponse = AuthResponse(
                        token = tokenValue,
                        expiresIn = 7200,
                        tokenType = "Basic"
                    )

                    return ApiResponse(
                        success = true,
                        message = "Authentication successful (basic)",
                        data = authResponse
                    )
                } catch (e: Exception) {
                    logger.error("Error parsing authentication response", e)
                    return ApiResponse(
                        success = false,
                        message = "Error parsing authentication response: ${e.message}",
                        data = null
                    )
                }
            } else {
                ApiResponse(
                    success = false,
                    message = "Authentication failed. Status: ${response.statusCode}",
                    data = null
                )
            }

        } catch (e: Exception) {
            logger.error("Error authenticating with main API", e)

            ApiResponse(
                success = false,
                message = "Failed to authenticate with main API: ${e.message}",
                data = null
            )
        }
    }

    fun checkFundAvailability(request: FundAvailabilityRequest): ApiResponse<FundAvailabilityResponse> {
        logger.info("Checking fund availability for amount: ${request.amount}")

        // Check if accountId is provided
        if (request.accountId == null) {
            return ApiResponse(
                success = false,
                message = "Account ID is required",
                data = null,
                transactionId = generateTransactionId()
            )
        }

        val accountId = request.accountId

        // Check if the account exists
        val accountExists = fakeApiAccountService.accountExists(accountId)

        // If the account doesn't exist, return false
        if (!accountExists) {
            val response = FundAvailabilityResponse(
                available = false,
                amount = request.amount,
                accountId = accountId,
                currentBalance = BigDecimal.ZERO
            )

            return ApiResponse(
                success = true, // The API call is successful even if the account doesn't exist
                message = "Account not found",
                data = response,
                transactionId = generateTransactionId()
            )
        }

        val currentBalance = fakeApiAccountService.getBalance(accountId)

        // Check if the account has sufficient balance for the requested amount
        // We'll consider funds available if the balance is at least the requested amount
        val available = currentBalance >= request.amount

        val response = FundAvailabilityResponse(
            available = available,
            amount = request.amount,
            accountId = accountId,
            currentBalance = currentBalance
        )

        val message = if (available) "Funds are available" else "Insufficient funds"

        return ApiResponse(
            success = true, // The API call is successful even if funds are not available
            message = message,
            data = response,
            transactionId = generateTransactionId()
        )
    }

    fun depositToMainApi(request: DepositRequest): ApiResponse<TransferResponse> {
        logger.info("Depositing funds to main API for user: ${request.email}, amount: ${request.amount}")

        // Use the account ID from the request
        val accountId = request.accountId
        val fundAvailabilityRequest = FundAvailabilityRequest(
            amount = request.amount,
            accountId = accountId,
            description = "Check funds for deposit to main API"
        )

        val fundAvailabilityResponse = checkFundAvailability(fundAvailabilityRequest)

        if (fundAvailabilityResponse.message == "Insufficient funds" || 
            fundAvailabilityResponse.message == "Account not found") {
            logger.error("Insufficient funds or account not found: ${fundAvailabilityResponse.message}")
            return ApiResponse(
                success = false,
                message = fundAvailabilityResponse.message,
                data = null
            )
        }

        return try {
            val url = "$mainApiBaseUrl$depositEndpoint"

            val depositRequest = EmailTransactionRequest(
                email = request.email,
                amount = request.amount,
                description = request.description ?: "Deposit from Debin"
            )

            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }

            val httpEntity = HttpEntity(depositRequest, headers)

            logger.info("Calling main API deposit endpoint at: $url")

            val response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                httpEntity,
                Map::class.java
            )

            logger.info("Main API deposit response status: ${response.statusCode}")

            if (response.statusCode.is2xxSuccessful && response.body != null) {
                // Process the deposit transaction
                val description = request.description ?: "Deposit to main API"

                // Generate transaction ID and create response
                val transactionId = generateTransactionId()
                val transferResponse = TransferResponse(
                    transactionId = transactionId,
                    amount = request.amount,
                    status = "COMPLETED",
                    accountIdentifier = accountId
                )

                logger.info("Successfully deposited funds to main API. Transaction ID: $transactionId")

                ApiResponse(
                    success = true,
                    message = "Funds deposited to main API successfully",
                    data = transferResponse,
                    transactionId = transactionId
                )
            } else {
                logger.error("Deposit to main API failed. Status: ${response.statusCode}")

                ApiResponse(
                    success = false,
                    message = "Deposit to main API failed. Status: ${response.statusCode}",
                    data = null
                )
            }

        } catch (e: Exception) {
            logger.error("Error depositing to main API", e)

            ApiResponse(
                success = false,
                message = "Failed to deposit to main API: ${e.message}",
                data = null
            )
        }
    }
}
