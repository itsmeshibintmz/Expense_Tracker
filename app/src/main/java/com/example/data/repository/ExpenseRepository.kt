package com.example.data.repository

import com.example.data.dao.ExpenseDao
import com.example.data.entity.*
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class ExpenseRepository(private val expenseDao: ExpenseDao) {

    val accounts: Flow<List<Account>> = expenseDao.getAllAccountsFlow()
    val budgets: Flow<List<Budget>> = expenseDao.getAllBudgetsFlow()
    val recurringEvents: Flow<List<RecurringEvent>> = expenseDao.getAllRecurringEventsFlow()
    val transactions: Flow<List<Transaction>> = expenseDao.getAllTransactionsFlow()
    val goals: Flow<List<Goal>> = expenseDao.getAllGoalsFlow()

    // --- Accounts ---
    suspend fun insertAccount(account: Account) {
        expenseDao.insertAccount(account)
    }

    suspend fun updateAccount(account: Account) {
        expenseDao.updateAccount(account)
    }

    suspend fun deleteAccount(account: Account) {
        expenseDao.deleteAccount(account)
    }

    // --- Budgets ---
    suspend fun insertBudget(budget: Budget) {
        expenseDao.insertBudget(budget)
    }

    suspend fun updateBudget(budget: Budget) {
        expenseDao.updateBudget(budget)
    }

    suspend fun deleteBudget(budget: Budget) {
        expenseDao.deleteBudget(budget)
    }

    // --- Recurring Events ---
    suspend fun insertRecurringEvent(event: RecurringEvent) {
        expenseDao.insertRecurringEvent(event)
    }

    suspend fun updateRecurringEvent(event: RecurringEvent) {
        expenseDao.updateRecurringEvent(event)
    }

    suspend fun deleteRecurringEvent(event: RecurringEvent) {
        expenseDao.deleteRecurringEvent(event)
    }

    // --- Goals ---
    suspend fun insertGoal(goal: Goal) {
        expenseDao.insertGoal(goal)
    }

    suspend fun updateGoal(goal: Goal) {
        expenseDao.updateGoal(goal)
    }

    suspend fun deleteGoal(goal: Goal) {
        expenseDao.deleteGoal(goal)
    }

    // --- Transactions & Smart Financial Logic ---
    suspend fun transferFunds(fromAccountId: Int, toAccountId: Int, amount: Double, note: String) {
        expenseDao.transferFundsAndSync(fromAccountId, toAccountId, amount, note)
    }

    suspend fun insertTransaction(transaction: Transaction) {
        expenseDao.insertTransactionAndSync(transaction)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        expenseDao.deleteTransactionAndSync(transaction)
    }

    // Execute recurring event: Adds matching transaction and moves date forward
    suspend fun executeRecurringEvent(event: RecurringEvent) {
        expenseDao.executeRecurringEventAndSync(event)
    }

    // Populate mock data if database is brand new to showcase excellent UX
    suspend fun prepopulateIfEmpty() {
        val currentAccounts = expenseDao.getAllAccounts()
        if (currentAccounts.isEmpty()) {
            expenseDao.insertAccount(Account(name = "Chase Checking", type = "Checking", balance = 2450.00, iconName = "AccountBalance"))
            expenseDao.insertAccount(Account(name = "Pocket Cash", type = "Cash", balance = 120.00, iconName = "Wallet"))
            expenseDao.insertAccount(Account(name = "Amex Credit Card", type = "Credit Card", balance = -450.00, iconName = "CreditCard"))
            expenseDao.insertAccount(Account(name = "Ally Savings", type = "Savings", balance = 15000.00, iconName = "Savings"))

            expenseDao.insertBudget(Budget(category = "Food & Groceries", limitAmount = 600.0, spentAmount = 210.50))
            expenseDao.insertBudget(Budget(category = "Shopping", limitAmount = 300.0, spentAmount = 145.20))
            expenseDao.insertBudget(Budget(category = "Entertainment", limitAmount = 150.0, spentAmount = 90.00))
            expenseDao.insertBudget(Budget(category = "Transport", limitAmount = 200.0, spentAmount = 65.00))
            expenseDao.insertBudget(Budget(category = "Bills & Utilities", limitAmount = 500.0, spentAmount = 350.00))

            val calendar = Calendar.getInstance()
            calendar.add(Calendar.MONTH, 3)
            expenseDao.insertGoal(Goal(title = "Hawaii Vacation", targetAmount = 3500.0, currentAmount = 1200.0, deadlineMillis = calendar.timeInMillis))
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.MONTH, 12)
            expenseDao.insertGoal(Goal(title = "Emergency Fund", targetAmount = 10000.0, currentAmount = 6500.0, deadlineMillis = calendar.timeInMillis))

            val now = System.currentTimeMillis()
            expenseDao.insertTransaction(Transaction(accountId = 1, title = "Weekly Grocery", category = "Food & Groceries", amount = 45.50, isExpense = true, note = "Grocery shopping at market", timestampMillis = now - 2 * 3600 * 1000))
            expenseDao.insertTransaction(Transaction(accountId = 1, title = "Utility Bills", category = "Bills & Utilities", amount = 120.00, isExpense = true, note = "Wifi & Electricity", timestampMillis = now - 24 * 3600 * 1000))
            expenseDao.insertTransaction(Transaction(accountId = 2, title = "Starbucks", category = "Food & Groceries", amount = 12.00, isExpense = true, note = "Mocha Latte coffee shop", timestampMillis = now - 2 * 24 * 3600 * 1000))
            expenseDao.insertTransaction(Transaction(accountId = 3, title = "Winter Coat", category = "Shopping", amount = 85.00, isExpense = true, note = "Winter Jacket purchase", timestampMillis = now - 3 * 24 * 3600 * 1000))
            expenseDao.insertTransaction(Transaction(accountId = 1, title = "Gas Station", category = "Transport", amount = 35.00, isExpense = true, note = "Gasoline fill up", timestampMillis = now - 4 * 24 * 3600 * 1000))
            expenseDao.insertTransaction(Transaction(accountId = 1, title = "Netflix Rental", category = "Entertainment", amount = 15.00, isExpense = true, note = "Movie rental online", timestampMillis = now - 5 * 24 * 3600 * 1000))
            
            // Income
            expenseDao.insertTransaction(Transaction(accountId = 1, title = "Work Paycheck", category = "Salary", amount = 3200.00, isExpense = false, note = "Bi-weekly Salary deposit", timestampMillis = now - 8 * 24 * 3600 * 1000))

            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.DAY_OF_YEAR, 3)
            expenseDao.insertRecurringEvent(RecurringEvent(
                title = "Netflix Premium",
                amount = 22.99,
                isExpense = true,
                category = "Entertainment",
                frequency = "Monthly",
                nextDateMillis = calendar.timeInMillis,
                accountId = 3
            ))
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.DAY_OF_YEAR, 5)
            expenseDao.insertRecurringEvent(RecurringEvent(
                title = "LA Fitness Gym",
                amount = 34.99,
                isExpense = true,
                category = "Health",
                frequency = "Monthly",
                nextDateMillis = calendar.timeInMillis,
                accountId = 1
            ))
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.DAY_OF_YEAR, 14)
            expenseDao.insertRecurringEvent(RecurringEvent(
                title = "Monthly Paycheck",
                amount = 3200.00,
                isExpense = false,
                category = "Salary",
                frequency = "Monthly",
                nextDateMillis = calendar.timeInMillis,
                accountId = 1
            ))
        }
    }
}
