package com.example.data.dao

import androidx.room.*
import com.example.data.entity.Account
import com.example.data.entity.Budget
import com.example.data.entity.RecurringEvent
import com.example.data.entity.Transaction
import com.example.data.entity.Goal
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

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

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Int): Transaction?

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

    // --- Transactional Sync Flows ---
    @androidx.room.Transaction
    suspend fun insertTransactionAndSync(transaction: Transaction) {
        if (transaction.id != 0) {
            val oldTransaction = getTransactionById(transaction.id)
            if (oldTransaction != null) {
                val oldAccount = getAccountById(oldTransaction.accountId)
                if (oldAccount != null) {
                    val oldDelta = if (oldTransaction.isExpense) oldTransaction.amount else -oldTransaction.amount
                    updateAccount(oldAccount.copy(balance = oldAccount.balance + oldDelta))
                }
                if (oldTransaction.isExpense) {
                    val oldBudget = getBudgetByCategory(oldTransaction.category)
                    if (oldBudget != null) {
                        val newSpent = (oldBudget.spentAmount - oldTransaction.amount).coerceAtLeast(0.0)
                        updateBudget(oldBudget.copy(spentAmount = newSpent))
                    }
                }
            }
        }

        insertTransaction(transaction)

        val account = getAccountById(transaction.accountId)
        if (account != null) {
            val delta = if (transaction.isExpense) -transaction.amount else transaction.amount
            updateAccount(account.copy(balance = account.balance + delta))
        }

        if (transaction.isExpense) {
            val budget = getBudgetByCategory(transaction.category)
            if (budget != null) {
                updateBudget(budget.copy(spentAmount = budget.spentAmount + transaction.amount))
            }
        }
    }

    @androidx.room.Transaction
    suspend fun deleteTransactionAndSync(transaction: Transaction) {
        deleteTransaction(transaction)

        val account = getAccountById(transaction.accountId)
        if (account != null) {
            val delta = if (transaction.isExpense) transaction.amount else -transaction.amount
            updateAccount(account.copy(balance = account.balance + delta))
        }

        if (transaction.isExpense) {
            val budget = getBudgetByCategory(transaction.category)
            if (budget != null) {
                val newSpent = (budget.spentAmount - transaction.amount).coerceAtLeast(0.0)
                updateBudget(budget.copy(spentAmount = newSpent))
            }
        }
    }

    @androidx.room.Transaction
    suspend fun transferFundsAndSync(fromAccountId: Int, toAccountId: Int, amount: Double, note: String) {
        val now = System.currentTimeMillis()
        val fromAccount = getAccountById(fromAccountId)
        val toAccount = getAccountById(toAccountId)
        if (fromAccount != null && toAccount != null) {
            val noteText = note.ifBlank { "Transfer" }
            
            val outTx = Transaction(
                accountId = fromAccountId,
                category = "Transfer",
                amount = amount,
                isExpense = true,
                note = "$noteText to ${toAccount.name}",
                timestampMillis = now
            )
            val inTx = Transaction(
                accountId = toAccountId,
                category = "Transfer",
                amount = amount,
                isExpense = false,
                note = "$noteText from ${fromAccount.name}",
                timestampMillis = now
            )
            
            insertTransactionAndSync(outTx)
            insertTransactionAndSync(inTx)
        }
    }

    @androidx.room.Transaction
    suspend fun executeRecurringEventAndSync(event: RecurringEvent) {
        val now = System.currentTimeMillis()
        var currentNextDate = event.nextDateMillis
        var lastExec = event.lastExecutedMillis
        
        val cal = Calendar.getInstance()
        
        while (currentNextDate <= now) {
            val transaction = Transaction(
                accountId = event.accountId,
                category = event.category,
                amount = event.amount,
                isExpense = event.isExpense,
                note = "[Recurring] ${event.title}",
                timestampMillis = currentNextDate
            )
            insertTransactionAndSync(transaction)
            
            lastExec = now
            cal.timeInMillis = currentNextDate
            when (event.frequency) {
                "Daily" -> cal.add(Calendar.DAY_OF_YEAR, 1)
                "Weekly" -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                "Monthly" -> cal.add(Calendar.MONTH, 1)
                "Yearly" -> cal.add(Calendar.YEAR, 1)
                else -> cal.add(Calendar.MONTH, 1)
            }
            currentNextDate = cal.timeInMillis
        }
        
        val updatedEvent = event.copy(
            lastExecutedMillis = lastExec,
            nextDateMillis = currentNextDate
        )
        updateRecurringEvent(updatedEvent)
    }
}
