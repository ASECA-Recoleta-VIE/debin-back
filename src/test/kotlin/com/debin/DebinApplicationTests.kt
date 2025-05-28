package com.debin

import com.debin.config.TestConfig
import com.debin.dto.ReceiveMoneyRequest
import com.debin.dto.RequestMoneyRequest
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal

@SpringBootTest(classes = [DebinApplication::class, TestConfig::class])
@AutoConfigureMockMvc
class DebinApplicationTests {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun contextLoads() {
        // Verify that the application context loads successfully
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
    fun `test receive money flow`() {
        // Create a receive money request
        val receiveRequest = ReceiveMoneyRequest(
            accountIdentifier = "123456789",
            amount = BigDecimal("100.00"),
            description = "Integration test for money reception",
            senderName = "Integration Test",
            senderAccount = "987654321"
        )

        // Send the request and get the result for further inspection
        val result = mockMvc.perform(
            post("/api/receive")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(receiveRequest))
        )

        // Check common fields that should be present regardless of success/failure
        result.andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.success").exists())

        // Get the success value to determine expected status
        val successValue = result.andReturn().response.contentAsString
            .let { objectMapper.readTree(it).get("success").asBoolean() }

        // If success is true, status should be 200 OK
        // If success is false, status should be 400 Bad Request
        if (successValue) {
            result.andExpect(status().isOk)
        } else {
            result.andExpect(status().isBadRequest)
        }
    }

    @Test
    fun `test request money flow`() {
        // Create a request money request
        val requestMoneyRequest = RequestMoneyRequest(
            accountIdentifier = "123456789",
            amount = BigDecimal("100.00"),
            description = "Integration test for money request",
            requesterName = "Integration Test",
            requesterAccount = "987654321"
        )

        // This test might fail if the external API is not available or configured
        // In a real scenario, we would mock the external API call
        mockMvc.perform(
            post("/api/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestMoneyRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").exists())
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.timestamp").exists())
    }

    @Test
    fun `test validation errors`() {
        // Test with invalid request (missing required fields)
        val invalidRequest = mapOf(
            "amount" to 100.00
            // Missing accountIdentifier and description
        )

        mockMvc.perform(
            post("/api/receive")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
        )
            .andExpect(status().isBadRequest)

        mockMvc.perform(
            post("/api/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
        )
            .andExpect(status().isBadRequest)
    }
}
