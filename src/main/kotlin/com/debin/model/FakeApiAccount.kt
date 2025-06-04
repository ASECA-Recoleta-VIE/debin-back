package com.debin.model

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class FakeApiAccount(
    val id: String = UUID.randomUUID().toString(),
    var balance: BigDecimal = BigDecimal.ZERO,
    val transactions: MutableList<FakeApiTransaction> = mutableListOf()
) {
    fun deposit(amount: BigDecimal, description: String): Boolean {
        if (amount <= BigDecimal.ZERO) {
            return false
        }
        
        balance = balance.add(amount)
        transactions.add(
            FakeApiTransaction(
                amount = amount,
                type = TransactionType.DEPOSIT,
                description = description
            )
        )
        return true
    }

}

data class FakeApiTransaction(
    val id: String = UUID.randomUUID().toString(),
    val amount: BigDecimal,
    val type: TransactionType,
    val description: String,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

enum class TransactionType {
    DEPOSIT
}