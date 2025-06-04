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
    
    init {
        createAccount(defaultAccountId, BigDecimal("1000.00"))
        logger.info("Initialized default account with ID: $defaultAccountId and balance: 1000.00")
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
}