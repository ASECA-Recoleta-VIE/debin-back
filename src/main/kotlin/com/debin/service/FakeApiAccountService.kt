package com.debin.service

import com.debin.model.FakeApiAccount
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.concurrent.ConcurrentHashMap

@Service
class FakeApiAccountService {
    private val logger = LoggerFactory.getLogger(FakeApiAccountService::class.java)

    private val accounts = ConcurrentHashMap<String, FakeApiAccount>()

    private val defaultAccountId = "default-account"

    // Immutable list of accounts with varied balances
    val predefinedAccounts = listOf(
        FakeApiAccount(id = "account-1", balance = BigDecimal("1000.00")),
        FakeApiAccount(id = "account-2", balance = BigDecimal("500.00")),
        FakeApiAccount(id = "account-3", balance = BigDecimal("2000.00")),
        FakeApiAccount(id = "account-4", balance = BigDecimal("50.00")),
        FakeApiAccount(id = "account-5", balance = BigDecimal("10.00")),
        FakeApiAccount(id = "account-6", balance = BigDecimal("5000.00")),
        FakeApiAccount(id = "account-7", balance = BigDecimal("750.00")),
        FakeApiAccount(id = "account-8", balance = BigDecimal("1500.00")),
        FakeApiAccount(id = "account-9", balance = BigDecimal("25.00")),
        FakeApiAccount(id = "account-10", balance = BigDecimal("3000.00"))
    )

    init {
        createAccount(defaultAccountId, BigDecimal("1000.00"))
        logger.info("Initialized default account with ID: $defaultAccountId and balance: 1000.00")

        // Add predefined accounts to the accounts map
        predefinedAccounts.forEach { account ->
            accounts[account.id] = account
            logger.info("Added predefined account with ID: ${account.id} and balance: ${account.balance}")
        }
    }

    fun createAccount(accountId: String, initialBalance: BigDecimal = BigDecimal.ZERO): FakeApiAccount {
        val account = FakeApiAccount(id = accountId, balance = initialBalance)
        accounts[accountId] = account
        logger.info("Created account with ID: $accountId and initial balance: $initialBalance")
        return account
    }

    fun getOrCreateAccount(accountId: String = defaultAccountId): FakeApiAccount {
        return accounts[accountId] ?: createAccount(accountId)
    }

    fun deposit(amount: BigDecimal, description: String, accountId: String = defaultAccountId): Boolean {
        val account = getOrCreateAccount(accountId)
        val success = account.deposit(amount, description)

        if (success) {
            logger.info("Deposited $amount to account $accountId. New balance: ${account.balance}")
        } else {
            logger.warn("Failed to deposit $amount to account $accountId")
        }

        return success
    }

    fun getBalance(accountId: String = defaultAccountId): BigDecimal {
        return getOrCreateAccount(accountId).balance
    }

    fun accountExists(accountId: String): Boolean {
        return accounts.containsKey(accountId)
    }
}
