package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.entity.Account
import com.example.data.entity.Goal
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LinkedGoalsAccountsTest {

    private lateinit var db: AppDatabase
    private lateinit var context: Context

    @Before
    fun createDb() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testInsertGoalAndCreateAccount() = runBlocking {
        val dao = db.expenseDao()

        // 1. Create a goal with new dedicated account (linkedAccountId = -1)
        dao.insertGoalAndCreateAccount(
            title = "Hawaii Vacation",
            targetAmount = 3500.0,
            currentAmount = 100.0,
            deadlineMillis = System.currentTimeMillis(),
            linkedAccountId = -1,
            excludeFromTotal = true
        )

        val accounts = dao.getAllAccounts()
        val goals = dao.getAllGoals()

        assertEquals(1, goals.size)
        val goal = goals[0]
        assertEquals("Hawaii Vacation", goal.title)
        assertEquals(3500.0, goal.targetAmount, 0.001)
        assertEquals(100.0, goal.currentAmount, 0.001)

        // Verify dedicated account was created
        assertEquals(1, accounts.size)
        val dedicatedAccount = accounts[0]
        assertEquals("Hawaii Vacation Savings", dedicatedAccount.name)
        assertEquals("Goal Savings", dedicatedAccount.type)
        assertEquals(100.0, dedicatedAccount.balance, 0.001)
        assertTrue(dedicatedAccount.excludeFromTotal)

        // Verify linkedAccountId points to the created account id
        assertEquals(dedicatedAccount.id, goal.linkedAccountId)
    }

    @Test
    fun testContributeToGoalAndSync() = runBlocking {
        val dao = db.expenseDao()

        // Create source checking account with $1000 balance
        val sourceAccount = Account(
            name = "Chase Checking",
            type = "Checking",
            balance = 1000.0,
            iconName = "AccountBalance",
            excludeFromTotal = false
        )
        dao.insertAccount(sourceAccount)
        val sourceId = dao.getAllAccounts()[0].id

        // Create goal with dedicated account and $100 saved
        dao.insertGoalAndCreateAccount(
            title = "Car Fund",
            targetAmount = 5000.0,
            currentAmount = 100.0,
            deadlineMillis = System.currentTimeMillis(),
            linkedAccountId = -1,
            excludeFromTotal = true
        )

        val initialGoal = dao.getAllGoals()[0]
        val initialAccounts = dao.getAllAccounts()
        val destAccount = initialAccounts.find { it.type == "Goal Savings" }!!

        // 2. Contribute $200 from Chase Checking to Car Fund goal
        dao.contributeToGoalAndSync(
            goalId = initialGoal.id,
            sourceAccountId = sourceId,
            amount = 200.0
        )

        val updatedGoal = dao.getAllGoals()[0]
        val updatedAccounts = dao.getAllAccounts()

        // Goal currentAmount should increase by $200
        assertEquals(300.0, updatedGoal.currentAmount, 0.001)

        // Source account balance should decrease by $200
        val updatedSource = updatedAccounts.find { it.id == sourceId }!!
        assertEquals(800.0, updatedSource.balance, 0.001)

        // Destination account balance should increase by $200
        val updatedDest = updatedAccounts.find { it.id == destAccount.id }!!
        assertEquals(300.0, updatedDest.balance, 0.001)
    }

    @Test
    fun testDeleteGoalAndSync() = runBlocking {
        val dao = db.expenseDao()

        // Create goal with dedicated account
        dao.insertGoalAndCreateAccount(
            title = "Hawaii Vacation",
            targetAmount = 3500.0,
            currentAmount = 100.0,
            deadlineMillis = System.currentTimeMillis(),
            linkedAccountId = -1,
            excludeFromTotal = true
        )

        val goal = dao.getAllGoals()[0]
        val accountsBeforeDelete = dao.getAllAccounts()
        assertEquals(1, accountsBeforeDelete.size)

        // Delete goal
        dao.deleteGoalAndSync(goal)

        val goalsAfter = dao.getAllGoals()
        val accountsAfter = dao.getAllAccounts()

        // Goal and its dedicated savings account should be deleted
        assertEquals(0, goalsAfter.size)
        assertEquals(0, accountsAfter.size)
    }

    @Test
    fun testNetWorthCalculationExclusion() = runBlocking {
        // Create included account ($1000)
        val chase = Account(name = "Chase Checking", type = "Checking", balance = 1000.0, iconName = "AccountBalance", excludeFromTotal = false)
        // Create excluded account ($500)
        val savings = Account(name = "Goal Savings", type = "Goal Savings", balance = 500.0, iconName = "Savings", excludeFromTotal = true)

        val accounts = listOf(chase, savings)

        // Compute net worth using our logic
        val netWorth = accounts.filter { !it.excludeFromTotal }.sumOf { it.balance }

        assertEquals(1000.0, netWorth, 0.001)
    }
}
