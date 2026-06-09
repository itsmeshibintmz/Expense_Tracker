package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // Checking, Savings, Cash, Credit Card
    val balance: Double,
    val iconName: String
)

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String, // Food, Shopping, Transport, Utilities, Entertainment, Health, Other
    val limitAmount: Double,
    val spentAmount: Double = 0.0
)

@Entity(tableName = "recurring_events")
data class RecurringEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val isExpense: Boolean,
    val category: String,
    val frequency: String, // Daily, Weekly, Monthly, Yearly
    val nextDateMillis: Long,
    val lastExecutedMillis: Long = 0L,
    val accountId: Int
)

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val accountId: Int,
    val category: String,
    val amount: Double,
    val isExpense: Boolean,
    val note: String,
    val timestampMillis: Long
)

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val deadlineMillis: Long
)
