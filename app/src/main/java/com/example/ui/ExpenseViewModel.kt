package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.entity.*
import com.example.data.repository.ExpenseRepository
import com.example.data.repository.UserPreferences
import com.example.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExpenseViewModel(
    private val repository: ExpenseRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    init {
        viewModelScope.launch {
            repository.prepopulateIfEmpty()
        }
    }

    val preferences: StateFlow<UserPreferences> = preferencesRepository.preferences

    fun updateUserName(name: String) {
        preferencesRepository.updateUserName(name)
    }

    fun updateCurrencySymbol(symbol: String) {
        preferencesRepository.updateCurrencySymbol(symbol)
    }

    fun updateThemeMode(mode: String) {
        preferencesRepository.updateThemeMode(mode)
    }

    fun updateAccentColor(color: String) {
        preferencesRepository.updateAccentColor(color)
    }

    val accounts: StateFlow<List<Account>> = repository.accounts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val budgets: StateFlow<List<Budget>> = repository.budgets
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recurringEvents: StateFlow<List<RecurringEvent>> = repository.recurringEvents
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val transactions: StateFlow<List<Transaction>> = repository.transactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val goals: StateFlow<List<Goal>> = repository.goals
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- Actions ---

    fun addAccount(name: String, type: String, balance: Double, iconName: String) {
        viewModelScope.launch {
            repository.insertAccount(Account(name = name, type = type, balance = balance, iconName = iconName))
        }
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch {
            repository.deleteAccount(account)
        }
    }

    fun addBudget(category: String, limitAmount: Double) {
        viewModelScope.launch {
            repository.insertBudget(Budget(category = category, limitAmount = limitAmount, spentAmount = 0.0))
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            repository.deleteBudget(budget)
        }
    }

    fun addRecurringEvent(
        title: String,
        amount: Double,
        isExpense: Boolean,
        category: String,
        frequency: String,
        nextDateMillis: Long,
        accountId: Int
    ) {
        viewModelScope.launch {
            repository.insertRecurringEvent(
                RecurringEvent(
                    title = title,
                    amount = amount,
                    isExpense = isExpense,
                    category = category,
                    frequency = frequency,
                    nextDateMillis = nextDateMillis,
                    accountId = accountId
                )
            )
        }
    }

    fun deleteRecurringEvent(event: RecurringEvent) {
        viewModelScope.launch {
            repository.deleteRecurringEvent(event)
        }
    }

    fun executeRecurringEvent(event: RecurringEvent) {
        viewModelScope.launch {
            repository.executeRecurringEvent(event)
        }
    }

    fun addTransaction(
        accountId: Int,
        category: String,
        amount: Double,
        isExpense: Boolean,
        note: String,
        timestampMillis: Long
    ) {
        viewModelScope.launch {
            repository.insertTransaction(
                Transaction(
                    accountId = accountId,
                    category = category,
                    amount = amount,
                    isExpense = isExpense,
                    note = note,
                    timestampMillis = timestampMillis
                )
            )
        }
    }

    fun transferFunds(fromAccountId: Int, toAccountId: Int, amount: Double, note: String) {
        viewModelScope.launch {
            repository.transferFunds(fromAccountId, toAccountId, amount, note)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun addGoal(title: String, targetAmount: Double, currentAmount: Double, deadlineMillis: Long) {
        viewModelScope.launch {
            repository.insertGoal(
                Goal(
                    title = title,
                    targetAmount = targetAmount,
                    currentAmount = currentAmount,
                    deadlineMillis = deadlineMillis
                )
            )
        }
    }

    fun updateGoalProgress(goal: Goal, newAmount: Double) {
        viewModelScope.launch {
            repository.updateGoal(goal.copy(currentAmount = newAmount))
        }
    }

    fun deleteGoal(goal: Goal) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
        }
    }
}

class ExpenseViewModelFactory(
    private val repository: ExpenseRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExpenseViewModel(repository, preferencesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

