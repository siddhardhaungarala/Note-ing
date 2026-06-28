package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "finance_transactions")
data class FinanceTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val noteId: Int, // Refers to the main finance account note
    val date: String, // "yyyy-MM-dd"
    val amount: Double, // Negative for expense, positive for income
    val description: String,
    val payeeContact: String? = null, // Store name starting with @
    val isLended: Boolean = false, // True if we lent money, False if borrowed
    val interestRate: Double = 0.0, // interest rate (e.g., 5.0)
    val interestPeriod: String = "MONTHLY", // "DAILY", "WEEKLY", "MONTHLY", "CUSTOM"
    val interestStartDate: String? = null,
    val isSettled: Boolean = false
)

@Entity(tableName = "finance_dashboard")
data class FinanceDashboardState(
    @PrimaryKey val id: Int = 1, // Only 1 global state
    val bankName: String = "",
    val bankBalance: Double = 0.0,
    val cashBalance: Double = 0.0,
    val creditCardLoans: Double = 0.0,
    val otherLoans: Double = 0.0,
    val monthlyIncome: Double = 0.0
)

@Entity(tableName = "bank_balances")
data class BankBalanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bankName: String,
    val balance: Double,
    val dateUpdated: String // "dd/MM/yyyy"
)

@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "BORROW" or "LEND"
    val source: String, // Bank name or Person name
    val amount: Double,
    val date: String, // "dd/MM/yyyy"
    val isSettled: Boolean = false
)
