package com.example.data.dao

import androidx.room.*
import com.example.data.entity.Account
import com.example.data.entity.Budget
import com.example.data.entity.RecurringEvent
import com.example.data.entity.Transaction
import com.example.data.entity.Goal
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    // --- Accounts ---
    @Query("SELECT * FROM accounts ORDER BY id ASC")
    fun getAllAccountsFlow(): Flow<List<Account>>

    @Query("SELECT * FROM accounts")
    suspend fun getAllAccounts(): List<Account>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: Int): Account?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: Account)

    @Update
    suspend fun updateAccount(account: Account)

    @Delete
    suspend fun deleteAccount(account: Account)


    // --- Budgets ---
    @Query("SELECT * FROM budgets ORDER BY category ASC")
    fun getAllBudgetsFlow(): Flow<List<Budget>>

    @Query("SELECT * FROM budgets")
    suspend fun getAllBudgets(): List<Budget>

    @Query("SELECT * FROM budgets WHERE category = :category LIMIT 1")
    suspend fun getBudgetByCategory(category: String): Budget?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget)

    @Update
    suspend fun updateBudget(budget: Budget)

    @Delete
    suspend fun deleteBudget(budget: Budget)


    // --- Recurring Events ---
    @Query("SELECT * FROM recurring_events ORDER BY nextDateMillis ASC")
    fun getAllRecurringEventsFlow(): Flow<List<RecurringEvent>>

    @Query("SELECT * FROM recurring_events")
    suspend fun getAllRecurringEvents(): List<RecurringEvent>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringEvent(event: RecurringEvent)

    @Update
    suspend fun updateRecurringEvent(event: RecurringEvent)

    @Delete
    suspend fun deleteRecurringEvent(event: RecurringEvent)


    // --- Transactions ---
    @Query("SELECT * FROM transactions ORDER BY timestampMillis DESC")
    fun getAllTransactionsFlow(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)


    // --- Goals ---
    @Query("SELECT * FROM goals ORDER BY deadlineMillis ASC")
    fun getAllGoalsFlow(): Flow<List<Goal>>

    @Query("SELECT * FROM goals")
    suspend fun getAllGoals(): List<Goal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: Goal)

    @Update
    suspend fun updateGoal(goal: Goal)

    @Delete
    suspend fun deleteGoal(goal: Goal)
}
