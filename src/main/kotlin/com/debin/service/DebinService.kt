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

@Service
class DebinService(
    @Autowired private val restTemplate: RestTemplate,
    @Value("\${main-api.base-url}") private val mainApiBaseUrl: String,
    @Value("\${main-api.endpoints.deposit}") private val depositEndpoint: String
) {

    private val logger = LoggerFactory.getLogger(DebinService::class.java)

    fun receiveMoney(request: ReceiveMoneyRequest): ApiResponse<TransferResponse> {
        logger.info("Processing money reception for account: ${request.accountIdentifier}")

        return try {
            Thread.sleep(1000)

            val isSuccessful = Random().nextDouble() > 0.1

            if (isSuccessful) {
                val transactionId = generateTransactionId()
                val transferResponse = TransferResponse(
                    transactionId = transactionId,
                    amount = request.amount,
                    status = "COMPLETED",
                    accountIdentifier = request.accountIdentifier
                )

                logger.info("Money reception successful. Transaction ID: $transactionId")

                ApiResponse(
                    success = true,
                    message = "Money received successfully",
                    data = transferResponse,
                    transactionId = transactionId
                )
            } else {
                logger.warn("Money reception failed for account: ${request.accountIdentifier}")

                ApiResponse(
                    success = false,
                    message = "Failed to process money reception. Please try again later.",
                    data = null
                )
            }
        } catch (e: Exception) {
            logger.error("Error processing money reception", e)

            ApiResponse(
                success = false,
                message = "Internal server error: ${e.message}",
                data = null
            )
        }
    }

    fun requestMoney(request: EmailTransactionRequest): ApiResponse<Any> {
        logger.info("Processing money request for account: ${request.email}")

        return try {
            val url = "$mainApiBaseUrl$depositEndpoint"

            // Convert to the format expected by the deposit endpoint
            val emailTransactionRequest = EmailTransactionRequest(
                email = request.email,
                amount = request.amount,
                description = request.description
            )

            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                // Forward any cookies or auth headers from the original request
                set("Cookie", "user=${request.email}")
            }

            val httpEntity = HttpEntity(emailTransactionRequest, headers)

            logger.info("Calling main API deposit endpoint at: $url")

            val response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                httpEntity,
                Map::class.java
            )

            logger.info("Main API deposit response status: ${response.statusCode}")

            if (response.statusCode.is2xxSuccessful) {
                ApiResponse(
                    success = true,
                    message = "Money request processed successfully",
                    data = response.body,
                    transactionId = generateTransactionId()
                )
            } else {
                ApiResponse(
                    success = false,
                    message = "Failed to process money request. Status: ${response.statusCode}",
                    data = response.body
                )
            }

        } catch (e: Exception) {
            logger.error("Error calling main API deposit endpoint", e)

            ApiResponse(
                success = false,
                message = "Failed to communicate with main API: ${e.message}",
                data = null
            )
        }
    }

    private fun generateTransactionId(): String {
        return "DEB-${System.currentTimeMillis()}-${Random().nextInt(1000, 9999)}"
    }
}
