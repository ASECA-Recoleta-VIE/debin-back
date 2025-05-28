package com.debin.config

import org.mockito.Mockito
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate

@TestConfiguration
class TestConfig {

    @Bean
    @Primary
    fun mockRestTemplate(): RestTemplate {
        // Create a mock RestTemplate that will be used in tests
        val mockRestTemplate = Mockito.mock(RestTemplate::class.java)
        
        // Configure the mock to return a successful response for any exchange call
        Mockito.`when`(
            mockRestTemplate.exchange(
                Mockito.anyString(),
                Mockito.any(),
                Mockito.any(),
                Mockito.eq(Map::class.java)
            )
        ).thenReturn(
            ResponseEntity(
                mapOf(
                    "status" to "PENDING",
                    "transactionId" to "TEST-123456",
                    "message" to "Test response from mock API"
                ),
                HttpStatus.OK
            ) as ResponseEntity<Map<*, *>>
        )
        
        return mockRestTemplate
    }
}