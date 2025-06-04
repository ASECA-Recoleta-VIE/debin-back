package com.debin.service

import com.debin.dto.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.util.*
import java.util.Base64

@Service
class DebinService(
    @Autowired private val restTemplate: RestTemplate,
    @Autowired private val fakeApiAccountService: FakeApiAccountService,
    @Value("\${main-api.base-url}") private val mainApiBaseUrl: String,
    @Value("\${main-api.endpoints.withdraw}") private val withdrawEndpoint: String,
    @Value("\${main-api.auth.endpoint}") private val authEndpoint: String
) {

    private val logger = LoggerFactory.getLogger(DebinService::class.java)

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

        val accountId = request.accountId ?: fakeApiAccountService.getOrCreateAccount().id
        val available = true
        val currentBalance = fakeApiAccountService.getBalance(accountId)

        val response = FundAvailabilityResponse(
            available = available,
            amount = request.amount,
            accountId = accountId,
            currentBalance = currentBalance
        )

        return ApiResponse(
            success = true,
            message = "Funds are available",
            data = response,
            transactionId = generateTransactionId()
        )
    }

    fun withdrawFromMainApi(request: WithdrawRequest): ApiResponse<TransferResponse> {
        logger.info("Withdrawing funds from main API for user: ${request.email}, amount: ${request.amount}")

        val authRequest = AuthRequest(
            email = request.email,
            password = request.password
        )

        val authResponse = authenticate(authRequest)

        if (!authResponse.success || authResponse.data == null) {
            logger.error("Authentication failed for user: ${request.email}")
            return ApiResponse(
                success = false,
                message = "Authentication failed: ${authResponse.message}",
                data = null
            )
        }

        return try {
            val url = "$mainApiBaseUrl$withdrawEndpoint"

            val withdrawRequest = mapOf(
                "email" to request.email,
                "amount" to request.amount,
                "description" to (request.description ?: "Withdrawal from Debin"),
                "password" to request.password
            )

            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON

                if (authResponse.data.tokenType == "Basic") {
                    val encodedAuth = Base64.getEncoder().encodeToString(authResponse.data.token.toByteArray())
                    set("Authorization", "Basic $encodedAuth")
                } else {
                    set("Authorization", "${authResponse.data.tokenType} ${authResponse.data.token}")
                }

                set("Cookie", "token=${authResponse.data.token}")
            }

            val httpEntity = HttpEntity(withdrawRequest, headers)

            logger.info("Calling main API withdraw endpoint at: $url")

            val response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                httpEntity,
                Map::class.java
            )

            logger.info("Main API withdraw response status: ${response.statusCode}")

            if (response.statusCode.is2xxSuccessful && response.body != null) {
                val accountId = fakeApiAccountService.getOrCreateAccount().id
                val description = request.description ?: "Withdrawal from main API"

                val depositSuccess = fakeApiAccountService.deposit(
                    amount = request.amount,
                    description = description,
                    accountId = accountId
                )

                if (!depositSuccess) {
                    logger.error("Failed to deposit funds to fake API account: $accountId")
                    return ApiResponse(
                        success = false,
                        message = "Failed to deposit funds to fake API",
                        data = null
                    )
                }

                val transactionId = generateTransactionId()
                val transferResponse = TransferResponse(
                    transactionId = transactionId,
                    amount = request.amount,
                    status = "COMPLETED",
                    accountIdentifier = accountId
                )

                logger.info("Successfully withdrew funds from main API and deposited to fake API. Transaction ID: $transactionId")

                ApiResponse(
                    success = true,
                    message = "Funds withdrawn from main API and deposited to fake API successfully",
                    data = transferResponse,
                    transactionId = transactionId
                )
            } else {
                logger.error("Withdrawal from main API failed. Status: ${response.statusCode}")

                ApiResponse(
                    success = false,
                    message = "Withdrawal from main API failed. Status: ${response.statusCode}",
                    data = null
                )
            }

        } catch (e: Exception) {
            logger.error("Error withdrawing from main API", e)

            ApiResponse(
                success = false,
                message = "Failed to withdraw from main API: ${e.message}",
                data = null
            )
        }
    }
}
