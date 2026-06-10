package com.example

import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.database.AppDatabase
import com.example.data.entity.*
import com.example.data.repository.ExpenseRepository
import com.example.data.repository.UserPreferences
import com.example.data.repository.UserPreferencesRepository
import com.example.ui.ExpenseViewModel
import com.example.ui.ExpenseViewModelFactory
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.MintGreen
import com.example.ui.theme.DeepSlate
import com.example.ui.theme.CoolGray
import com.example.ui.theme.CoralRed
import com.example.ui.theme.SunnyYellow
import com.example.ui.theme.SkyBlue
import com.example.ui.theme.LavenderPurple
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val database = remember { AppDatabase.getDatabase(context) }
            val repository = remember { ExpenseRepository(database.expenseDao()) }
            val prefsRepository = remember { UserPreferencesRepository(context.applicationContext) }
            val viewModel: ExpenseViewModel = viewModel(
                factory = ExpenseViewModelFactory(repository, prefsRepository)
            )
            val preferences by viewModel.preferences.collectAsStateWithLifecycle()

            MyApplicationTheme(
                themeMode = preferences.themeMode,
                accentColorName = preferences.accentColor
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ExpenseTrackerApp(viewModel)
                }
            }
        }
    }
}

// Helper formats
fun formatAmount(amount: Double, symbol: String = "$"): String {
    return if (amount >= 0) {
        String.format("%s%,.2f", symbol, amount)
    } else {
        String.format("-%s%,.2f", symbol, kotlin.math.abs(amount))
    }
}

fun formatDate(millis: Long): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return formatter.format(Date(millis))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseTrackerApp(viewModel: ExpenseViewModel) {
    // Collect reactive state flows
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()
    val recurringEvents by viewModel.recurringEvents.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()

    var showConfetti by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    // Dialog trigger states
    var showQuickAddTransaction by remember { mutableStateOf(false) }
    var showAddAccount by remember { mutableStateOf(false) }
    var showAddBudget by remember { mutableStateOf(false) }
    var showAddRecurring by remember { mutableStateOf(false) }
    var showAddGoal by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    // Active bottom-nav tab state
    var activeTab by remember { mutableStateOf("dashboard") } // dashboard, accounts, budgets, goals

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = "App Logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Expense Tracker",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    IconButton(
                        onClick = { showSettings = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Personalization & Settings",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = {
                            // Quick simulation button to execute any due events
                            val due = recurringEvents.filter { it.nextDateMillis <= System.currentTimeMillis() }
                            due.forEach { viewModel.executeRecurringEvent(it) }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync recurring events",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                windowInsets = WindowInsets.navigationBars
            ) {
                NavigationBarItem(
                    selected = activeTab == "dashboard",
                    onClick = { activeTab = "dashboard" },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = activeTab == "accounts",
                    onClick = { activeTab = "accounts" },
                    icon = { Icon(Icons.Default.Wallet, contentDescription = "Accounts") },
                    label = { Text("Accounts") }
                )
                NavigationBarItem(
                    selected = activeTab == "budgets",
                    onClick = { activeTab = "budgets" },
                    icon = { Icon(Icons.Default.PieChart, contentDescription = "Budgets") },
                    label = { Text("Budgets") }
                )
                NavigationBarItem(
                    selected = activeTab == "goals",
                    onClick = { activeTab = "goals" },
                    icon = { Icon(Icons.Default.EmojiEvents, contentDescription = "Goals") },
                    label = { Text("Goals") }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showQuickAddTransaction = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp, end = 8.dp)
                    .testTag("quick_add_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Quick Add Transaction",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                },
                label = "TabTransition"
            ) { targetTab ->
                when (targetTab) {
                    "dashboard" -> DashboardScreen(
                        accounts = accounts,
                        budgets = budgets,
                        recurringEvents = recurringEvents,
                        transactions = transactions,
                        goals = goals,
                        viewModel = viewModel,
                        currencySymbol = preferences.currencySymbol,
                        userName = preferences.userName,
                        onAddAccountClick = { showAddAccount = true },
                        onAddBudgetClick = { showAddBudget = true },
                        onAddRecurringClick = { showAddRecurring = true },
                        onAddGoalClick = { showAddGoal = true },
                        onAddContribution = { goal, accountId, amount ->
                            val newAmount = (goal.currentAmount + amount).coerceAtMost(goal.targetAmount)
                            if (goal.currentAmount < goal.targetAmount && newAmount >= goal.targetAmount) {
                                showConfetti = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            } else {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            viewModel.contributeToGoal(goal, accountId, amount)
                        }
                    )
                    "accounts" -> AccountsTabScreen(
                        accounts = accounts,
                        viewModel = viewModel,
                        currencySymbol = preferences.currencySymbol,
                        onAddAccountClick = { showAddAccount = true }
                    )
                    "budgets" -> BudgetsTabScreen(
                        budgets = budgets,
                        viewModel = viewModel,
                        currencySymbol = preferences.currencySymbol,
                        onAddBudgetClick = { showAddBudget = true }
                    )
                    "goals" -> GoalsTabScreen(
                        goals = goals,
                        accounts = accounts,
                        currencySymbol = preferences.currencySymbol,
                        onAddGoalClick = { showAddGoal = true },
                        onAddContribution = { goal, accountId, amount ->
                            val newAmount = (goal.currentAmount + amount).coerceAtMost(goal.targetAmount)
                            if (goal.currentAmount < goal.targetAmount && newAmount >= goal.targetAmount) {
                                showConfetti = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            } else {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            viewModel.contributeToGoal(goal, accountId, amount)
                        },
                        onDeleteGoal = { goal -> viewModel.deleteGoal(goal) }
                    )
                }
            }
        }
    }

    // Modal Forms as clean Dialog components
    if (showQuickAddTransaction) {
        QuickAddTransactionDialog(
            accounts = accounts,
            budgets = budgets,
            currencySymbol = preferences.currencySymbol,
            onDismiss = { showQuickAddTransaction = false },
            onConfirm = { accountId, title, category, amount, isExpense, note ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.addTransaction(accountId, title, category, amount, isExpense, note, System.currentTimeMillis())
                showQuickAddTransaction = false
            },
            onTransferConfirm = { fromAccountId, toAccountId, amount, note ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.transferFunds(fromAccountId, toAccountId, amount, note)
                showQuickAddTransaction = false
            }
        )
    }

    if (showAddAccount) {
        AddAccountDialog(
            currencySymbol = preferences.currencySymbol,
            onDismiss = { showAddAccount = false },
            onConfirm = { name, type, balance, iconName, excludeFromTotal ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.addAccount(name, type, balance, iconName, excludeFromTotal)
                showAddAccount = false
            }
        )
    }

    if (showAddBudget) {
        AddBudgetDialog(
            existingCategories = budgets.map { it.category },
            currencySymbol = preferences.currencySymbol,
            onDismiss = { showAddBudget = false },
            onConfirm = { category, limit ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.addBudget(category, limit)
                showAddBudget = false
            }
        )
    }

    if (showAddRecurring) {
        AddRecurringEventDialog(
            accounts = accounts,
            currencySymbol = preferences.currencySymbol,
            onDismiss = { showAddRecurring = false },
            onConfirm = { title, amount, isExpense, category, freq, date, accId ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.addRecurringEvent(title, amount, isExpense, category, freq, date, accId)
                showAddRecurring = false
            }
        )
    }

    if (showAddGoal) {
        AddGoalDialog(
            accounts = accounts,
            currencySymbol = preferences.currencySymbol,
            onDismiss = { showAddGoal = false },
            onConfirm = { title, target, current, deadline, linkedAccountId, excludeFromTotal ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.addGoal(title, target, current, deadline, linkedAccountId, excludeFromTotal)
                showAddGoal = false
            }
        )
    }

    if (showSettings) {
        SettingsDialog(
            preferences = preferences,
            onDismiss = { showSettings = false },
            onUpdateName = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.updateUserName(it)
            },
            onUpdateCurrency = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.updateCurrencySymbol(it)
            },
            onUpdateTheme = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.updateThemeMode(it)
            },
            onUpdateAccentColor = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.updateAccentColor(it)
            }
        )
    }

    if (showConfetti) {
        ConfettiOverlay(trigger = showConfetti, onFinished = { showConfetti = false })
    }
}

// ----------------------------------------------------------------------------------
// SCREEN: HOME DASHBOARD (Widgets list)
// ----------------------------------------------------------------------------------
@Composable
fun DashboardScreen(
    accounts: List<Account>,
    budgets: List<Budget>,
    recurringEvents: List<RecurringEvent>,
    transactions: List<Transaction>,
    goals: List<Goal>,
    viewModel: ExpenseViewModel,
    currencySymbol: String,
    userName: String,
    onAddAccountClick: () -> Unit,
    onAddBudgetClick: () -> Unit,
    onAddRecurringClick: () -> Unit,
    onAddGoalClick: () -> Unit,
    onAddContribution: (Goal, Int, Double) -> Unit
) {
    // Compute quick dashboard summaries
    val netWorth = accounts.filter { !it.excludeFromTotal }.sumOf { it.balance }
    val totalExpenseThisMonth = transactions.filter { it.isExpense && it.category != "Transfer" }.sumOf { it.amount }
    val totalIncomeThisMonth = transactions.filter { !it.isExpense && it.category != "Transfer" }.sumOf { it.amount }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp)
    ) {
        // 1. Header Hero Widget
        item {
            NetWorthHeroWidget(netWorth, totalIncomeThisMonth, totalExpenseThisMonth, currencySymbol, userName)
        }

        // 2. Analytics Donut Widget
        item {
            AnalyticsDonutWidget(transactions, currencySymbol)
        }

        // 3. Accounts Widget (Horizontal carousel with scrolling indicator)
        item {
            AccountsOverviewWidget(accounts, currencySymbol, onAddAccountClick)
        }

        // 4. Budgets Status Widget
        item {
            BudgetsWidget(budgets, currencySymbol, onAddBudgetClick)
        }

        // 5. Recurring Schedules Widget
        item {
            RecurringEventsWidget(
                events = recurringEvents,
                currencySymbol = currencySymbol,
                onAddClick = onAddRecurringClick,
                onExecuteClick = { event -> viewModel.executeRecurringEvent(event) },
                onDeleteClick = { event -> viewModel.deleteRecurringEvent(event) }
            )
        }

        // 6. Savings Goals tracking Widget
        item {
            GoalsWidget(
                goals = goals,
                accounts = accounts,
                currencySymbol = currencySymbol,
                onAddClick = onAddGoalClick,
                onAddContribution = onAddContribution,
                onDeleteClick = { goal -> viewModel.deleteGoal(goal) }
            )
        }

        // 7. Recent Transactions List
        item {
            Text(
                text = "Recent History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (transactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No transactions tracked yet.\nTap the + button to log an entry!",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(transactions.take(6)) { transaction ->
                TransactionListItem(
                    transaction = transaction,
                    accounts = accounts,
                    currencySymbol = currencySymbol,
                    onDelete = { viewModel.deleteTransaction(transaction) }
                )
            }
        }
    }
}

// ----------------------------------------------------------------------------------
// WIDGETS: HOMEPAGE VISUAL BLOCKS
// ----------------------------------------------------------------------------------

// 1. Hero Summary Card
@Composable
fun NetWorthHeroWidget(netWorth: Double, income: Double, expense: Double, currencySymbol: String, userName: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Hello, $userName!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Total Balanced Assets",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatAmount(netWorth, currencySymbol),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Dual indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Income Column
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MintGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = "Income Flow",
                            tint = MintGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Income total",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatAmount(income, currencySymbol),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MintGreen
                        )
                    }
                }

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))
                )

                // Expense Column
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CoralRed.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingDown,
                            contentDescription = "Expense Flow",
                            tint = CoralRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Expense total",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatAmount(expense, currencySymbol),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = CoralRed
                        )
                    }
                }
            }
        }
    }
}

// 2. ANALYTICS: Dynamic Category Donut Chart
@Composable
fun AnalyticsDonutWidget(transactions: List<Transaction>, currencySymbol: String) {
    val expenses = transactions.filter { it.isExpense && it.category != "Transfer" }
    val totalExpense = expenses.sumOf { it.amount }

    // Aggregate category values
    val categoryTotals = remember(expenses) {
        expenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    // Color mapper for unique visual identities
    val categoryColors = mapOf(
        "Food & Groceries" to MintGreen,
        "Shopping" to SkyBlue,
        "Entertainment" to LavenderPurple,
        "Transport" to SunnyYellow,
        "Bills & Utilities" to CoralRed,
        "Health" to Color(0xFFE040FB),
        "Salary" to MintGreen,
        "Other" to CoolGray
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = "Expense Distribution (Analytics)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (totalExpense <= 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Not enough expense entries yet to generate analytics breakout.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Custom Donut Canvas Drawing
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            var currentAngle = -90f
                            categoryTotals.forEach { (category, amount) ->
                                val pct = (amount / totalExpense).toFloat()
                                val sweepAngle = pct * 360f
                                val color = categoryColors[category] ?: CoolGray
                                drawArc(
                                    color = color,
                                    startAngle = currentAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    style = Stroke(width = 24f, cap = StrokeCap.Round)
                                )
                                currentAngle += sweepAngle
                            }
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Total Out",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format("%s%,.0f", currencySymbol, totalExpense),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    // Segmented Legends list
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Max 4 items in legend, group 'Other' if more
                        val displayLegends = categoryTotals.take(4)
                        displayLegends.forEach { (cat, amt) ->
                            val color = categoryColors[cat] ?: CoolGray
                            val percentage = (amt / totalExpense) * 100
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = cat,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = String.format("%.0f%%", percentage),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (categoryTotals.size > 4) {
                            val leftoverCount = categoryTotals.size - 4
                            Text(
                                text = "+ $leftoverCount secondary categories",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// 3. ACCOUNTS OVERVIEW: Horizontal Swipe carousel
@Composable
fun AccountsOverviewWidget(accounts: List<Account>, currencySymbol: String, onAddAccountClick: () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Accounts Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onAddAccountClick) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add", style = MaterialTheme.typography.labelMedium)
            }
        }

        if (accounts.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .clickable { onAddAccountClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No accounts configured yet.\nTap here to setup your first Account!",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(accounts) { account ->
                    AccountCardItem(account, currencySymbol)
                }
            }
        }
    }
}

@Composable
fun AccountCardItem(account: Account, currencySymbol: String) {
    val iconsMap = mapOf(
        "AccountBalance" to Icons.Default.AccountBalance,
        "Wallet" to Icons.Default.Wallet,
        "CreditCard" to Icons.Default.CreditCard,
        "Savings" to Icons.Default.Savings
    )
    val cardIcon = iconsMap[account.iconName] ?: Icons.Default.Help

    val containerColor = when (account.type) {
        "Savings" -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
        "Credit Card" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier
            .width(185.dp)
            .testTag("account_card_${account.id}"),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = account.type,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = cardIcon,
                    contentDescription = null,
                    tint = if (account.balance >= 0) MintGreen else CoralRed,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = account.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = formatAmount(account.balance, currencySymbol),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = if (account.balance >= 0) MintGreen else CoralRed
            )
        }
    }
}

// 4. BUDGETS WIDGET: Simple overview with warning colors
@Composable
fun BudgetsWidget(budgets: List<Budget>, currencySymbol: String, onAddBudgetClick: () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Monthly Budgets",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onAddBudgetClick) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Set Budget", style = MaterialTheme.typography.labelMedium)
            }
        }

        if (budgets.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .clickable { onAddBudgetClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No budgets defined.\nTap here to setup an expense limit!",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    budgets.take(3).forEach { budget ->
                        BudgetWidgetRow(budget, currencySymbol)
                    }
                    if (budgets.size > 3) {
                        val remaining = budgets.size - 3
                        Text(
                            text = "and $remaining other budgets configured...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BudgetWidgetRow(budget: Budget, currencySymbol: String) {
    val fraction = if (budget.limitAmount > 0) (budget.spentAmount / budget.limitAmount).toFloat() else 0f
    val progressColor = when {
        fraction < 0.75f -> MintGreen
        fraction < 1.0f -> SunnyYellow
        else -> CoralRed
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = budget.category,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${formatAmount(budget.spentAmount, currencySymbol)} / ${formatAmount(budget.limitAmount, currencySymbol)}",
                style = MaterialTheme.typography.bodySmall,
                color = if (budget.spentAmount > budget.limitAmount) CoralRed else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = progressColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

// 5. RECURRING EVENTS WIDGET: Upcoming items with execute triggers
@Composable
fun RecurringEventsWidget(
    events: List<RecurringEvent>,
    currencySymbol: String,
    onAddClick: () -> Unit,
    onExecuteClick: (RecurringEvent) -> Unit,
    onDeleteClick: (RecurringEvent) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Upcoming Recurring Events",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Schedule", style = MaterialTheme.typography.labelMedium)
            }
        }

        if (events.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .clickable { onAddClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No schedules. Tap to add subscriptions, utility transfers or salary schedules!",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Show chronological upcoming events
                    events.take(3).forEach { event ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (event.isExpense) CoralRed.copy(alpha = 0.12f)
                                        else MintGreen.copy(alpha = 0.12f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (event.isExpense) Icons.Default.EventBusy else Icons.Default.Update,
                                    contentDescription = null,
                                    tint = if (event.isExpense) CoralRed else MintGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = event.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Next: ${formatDate(event.nextDateMillis)} • ${event.frequency}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(
                                horizontalAlignment = Alignment.End,
                                modifier = Modifier.padding(end = 6.dp)
                            ) {
                                Text(
                                    text = (if (event.isExpense) "-" else "+") + formatAmount(event.amount, currencySymbol),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (event.isExpense) CoralRed else MintGreen
                                )
                            }
                            // Execute CTA
                            IconButton(
                                onClick = { onExecuteClick(event) },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    .testTag("execute_recurring_${event.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Post transaction",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    if (events.size > 3) {
                        Text(
                            text = "+ ${events.size - 3} other events scheduled",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        }
    }
}

// 6. GOALS WIDGET: Tracking goals progress with contributions
@Composable
fun GoalsWidget(
    goals: List<Goal>,
    accounts: List<Account>,
    currencySymbol: String,
    onAddClick: () -> Unit,
    onAddContribution: (Goal, Int, Double) -> Unit,
    onDeleteClick: (Goal) -> Unit
) {
    var showContributeGoal: Goal? by remember { mutableStateOf(null) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Savings Goals",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Goal", style = MaterialTheme.typography.labelMedium)
            }
        }

        if (goals.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .clickable { onAddClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No financial savings goals defined yet.\nTap here to setup a target goal!",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(goals) { goal ->
                    GoalCardItem(
                        goal = goal,
                        accounts = accounts,
                        currencySymbol = currencySymbol,
                        onContributeClick = { showContributeGoal = goal }
                    )
                }
            }
        }
    }

    if (showContributeGoal != null) {
        val targetGoal = showContributeGoal!!
        ContributeGoalDialog(
            goalTitle = targetGoal.title,
            accounts = accounts.filter { !it.excludeFromTotal },
            currencySymbol = currencySymbol,
            onDismiss = { showContributeGoal = null },
            onConfirm = { accountId, amount ->
                onAddContribution(targetGoal, accountId, amount)
                showContributeGoal = null
            }
        )
    }
}

@Composable
fun GoalCardItem(goal: Goal, accounts: List<Account>, currencySymbol: String, onContributeClick: () -> Unit) {
    val pctFraction = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat() else 0f
    val percentage = (pctFraction * 100).toInt()

    Card(
        modifier = Modifier
            .width(220.dp)
            .clickable { onContributeClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = goal.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "$percentage%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Saved ${formatAmount(goal.currentAmount, currencySymbol)} of ${formatAmount(goal.targetAmount, currencySymbol)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { pctFraction.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = SkyBlue,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            val linkedAccount = accounts.find { it.id == goal.linkedAccountId }
            if (linkedAccount != null) {
                Text(
                    text = "Linked: ${linkedAccount.name}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "By: ${formatDate(goal.deadlineMillis)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = onContributeClick,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(Icons.Default.Savings, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------------
// COMPONENT: TRANSACTION LIST ITEM
// ----------------------------------------------------------------------------------
@Composable
fun TransactionListItem(transaction: Transaction, accounts: List<Account>, currencySymbol: String, onDelete: () -> Unit) {
    val accountName = accounts.find { it.id == transaction.accountId }?.name ?: "Unknown"

    val iconsMap = mapOf(
        "Food & Groceries" to Icons.Default.Restaurant,
        "Shopping" to Icons.Default.LocalMall,
        "Entertainment" to Icons.Default.Movie,
        "Transport" to Icons.Default.DirectionsCar,
        "Bills & Utilities" to Icons.Default.Lightbulb,
        "Health" to Icons.Default.LocalHospital,
        "Salary" to Icons.Default.AttachMoney,
        "Transfer" to Icons.Default.SwapHoriz,
        "Other" to Icons.Default.Category
    )
    val indicatorIcon = iconsMap[transaction.category] ?: Icons.Default.Help

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (transaction.category == "Transfer") SkyBlue.copy(alpha = 0.15f)
                    else if (transaction.isExpense) CoralRed.copy(alpha = 0.1f)
                    else MintGreen.copy(alpha = 0.1f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = indicatorIcon,
                contentDescription = null,
                tint = if (transaction.category == "Transfer") SkyBlue
                       else if (transaction.isExpense) CoralRed else MintGreen,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (transaction.title.isNotBlank()) transaction.title else transaction.category,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$accountName • ${transaction.category} • ${formatDate(transaction.timestampMillis)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = (if (transaction.isExpense) "-" else "+") + formatAmount(transaction.amount, currencySymbol),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Black,
                    color = if (transaction.category == "Transfer") SkyBlue
                            else if (transaction.isExpense) CoralRed else MintGreen
                )
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------------
// TABS: ACCOUNTS, BUDGETS, GOALS COMPILATIONS
// ----------------------------------------------------------------------------------

@Composable
fun AccountsTabScreen(
    accounts: List<Account>,
    viewModel: ExpenseViewModel,
    currencySymbol: String,
    onAddAccountClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "My Financial Accounts",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Manage your active wallets and credit balances",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onAddAccountClick,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("add_account_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add", style = MaterialTheme.typography.labelMedium)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (accounts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No accounts configured.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(accounts) { account ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            val iconsMap = mapOf(
                                "AccountBalance" to Icons.Default.AccountBalance,
                                "Wallet" to Icons.Default.Wallet,
                                "CreditCard" to Icons.Default.CreditCard,
                                "Savings" to Icons.Default.Savings
                            )
                            Icon(
                                imageVector = iconsMap[account.iconName] ?: Icons.Default.Help,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = account.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Type: ${account.type}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (account.excludeFromTotal) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Excluded",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                        Text(
                            text = formatAmount(account.balance, currencySymbol),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (account.balance >= 0) MintGreen else CoralRed,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        IconButton(onClick = { viewModel.deleteAccount(account) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Account", tint = CoralRed)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BudgetsTabScreen(
    budgets: List<Budget>,
    viewModel: ExpenseViewModel,
    currencySymbol: String,
    onAddBudgetClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Spend Limits & Budgets",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Monthly expenditure caps by category",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onAddBudgetClick,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("add_budget_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Set", style = MaterialTheme.typography.labelMedium)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (budgets.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No active budgets. Set limit to trace and control your spends!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(budgets) { budget ->
                    val fraction = if (budget.limitAmount > 0) (budget.spentAmount / budget.limitAmount).toFloat() else 0f
                    val progressColor = when {
                        fraction < 0.75f -> MintGreen
                        fraction < 1.0f -> SunnyYellow
                        else -> CoralRed
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = budget.category,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${formatAmount(budget.spentAmount, currencySymbol)} spent of ${formatAmount(budget.limitAmount, currencySymbol)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (fraction >= 1f) CoralRed else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { fraction.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = progressColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        IconButton(onClick = { viewModel.deleteBudget(budget) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Budget", tint = CoralRed)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GoalsTabScreen(
    goals: List<Goal>,
    accounts: List<Account>,
    currencySymbol: String,
    onAddGoalClick: () -> Unit,
    onAddContribution: (Goal, Int, Double) -> Unit,
    onDeleteGoal: (Goal) -> Unit
) {
    var showContributeGoal: Goal? by remember { mutableStateOf(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Savings & Visual Goals",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Setup target goals to save periodically",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onAddGoalClick,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("add_goal_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Goal", style = MaterialTheme.typography.labelMedium)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (goals.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No active savings goals configured.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(goals) { goal ->
                    val fraction = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat() else 0f
                    val percentage = (fraction * 100).toInt()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { showContributeGoal = goal }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = goal.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${formatAmount(goal.currentAmount, currencySymbol)} / ${formatAmount(goal.targetAmount, currencySymbol)} ($percentage%)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { fraction.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = SkyBlue,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Deadline: ${formatDate(goal.deadlineMillis)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val linkedAccount = accounts.find { it.id == goal.linkedAccountId }
                                if (linkedAccount != null) {
                                    Text(
                                        text = "Linked: ${linkedAccount.name}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        IconButton(onClick = { onDeleteGoal(goal) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Goal", tint = CoralRed)
                        }
                    }
                }
            }
        }
    }

    if (showContributeGoal != null) {
        val targetGoal = showContributeGoal!!
        ContributeGoalDialog(
            goalTitle = targetGoal.title,
            accounts = accounts.filter { !it.excludeFromTotal },
            currencySymbol = currencySymbol,
            onDismiss = { showContributeGoal = null },
            onConfirm = { accountId, amount ->
                onAddContribution(targetGoal, accountId, amount)
                showContributeGoal = null
            }
        )
    }
}

// ----------------------------------------------------------------------------------
// FORMS / FORUM DIALOG CONSTRUCTORS
// ----------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddTransactionDialog(
    accounts: List<Account>,
    budgets: List<Budget>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (accountId: Int, title: String, category: String, amount: Double, isExpense: Boolean, note: String) -> Unit,
    onTransferConfirm: (fromAccountId: Int, toAccountId: Int, amount: Double, note: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var transactionType by remember { mutableStateOf("expense") } // expense, income, transfer

    // Account choice selection
    var selectedAccountIndex by remember { mutableStateOf(0) }
    var accountExpanded by remember { mutableStateOf(false) }

    // Source and destination accounts for transfers
    var selectedSourceIndex by remember { mutableStateOf(0) }
    var selectedDestIndex by remember { mutableStateOf(if (accounts.size > 1) 1 else 0) }
    var sourceExpanded by remember { mutableStateOf(false) }
    var destExpanded by remember { mutableStateOf(false) }

    // Category selection mapping
    val categories = if (transactionType == "expense") {
        listOf("Food & Groceries", "Shopping", "Entertainment", "Transport", "Bills & Utilities", "Health", "Other")
    } else {
        listOf("Salary", "Investment", "Gift", "Other")
    }
    var selectedCategoryIndex by remember { mutableStateOf(0) }
    var categoryExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Quick Tracker Entry",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                // Expense / Income / Transfer toggle
                TabRow(
                    selectedTabIndex = when (transactionType) {
                        "expense" -> 0
                        "income" -> 1
                        else -> 2
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    indicator = {}
                ) {
                    Tab(
                        selected = transactionType == "expense",
                        onClick = {
                            transactionType = "expense"
                            selectedCategoryIndex = 0
                        },
                        text = { Text("Expense", fontWeight = FontWeight.Bold) },
                        selectedContentColor = CoralRed,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Tab(
                        selected = transactionType == "income",
                        onClick = {
                            transactionType = "income"
                            selectedCategoryIndex = 0
                        },
                        text = { Text("Income", fontWeight = FontWeight.Bold) },
                        selectedContentColor = MintGreen,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Tab(
                        selected = transactionType == "transfer",
                        onClick = {
                            transactionType = "transfer"
                        },
                        text = { Text("Transfer", fontWeight = FontWeight.Bold) },
                        selectedContentColor = SkyBlue,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Transaction Name (Title) input
                if (transactionType != "transfer") {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Transaction Name") },
                        singleLine = true,
                        placeholder = { Text("e.g. Weekly Groceries, Gas") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Numeric input amount
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { value ->
                        if (value.isEmpty() || value.toDoubleOrNull() != null || value == ".") {
                            amountStr = value
                        }
                    },
                    label = { Text("Amount ($currencySymbol)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                if (accounts.isEmpty()) {
                    Text(
                        text = "⚠️ You must add an Account first is required!",
                        color = CoralRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    if (transactionType == "transfer") {
                        if (accounts.size < 2) {
                            Text(
                                text = "⚠️ Transferee requires at least 2 accounts!",
                                color = SunnyYellow,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        // Source selection dropdown
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = accounts[selectedSourceIndex].name,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("From Account (Source)") },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { sourceExpanded = true }
                            )
                            DropdownMenu(
                                expanded = sourceExpanded,
                                onDismissRequest = { sourceExpanded = false }
                            ) {
                                accounts.forEachIndexed { idx, acc ->
                                    DropdownMenuItem(
                                        text = { Text("${acc.name} (${formatAmount(acc.balance, currencySymbol)})") },
                                        onClick = {
                                            selectedSourceIndex = idx
                                            sourceExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Target selection dropdown
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = accounts[selectedDestIndex].name,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("To Account (Destination)") },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { destExpanded = true }
                            )
                            DropdownMenu(
                                expanded = destExpanded,
                                onDismissRequest = { destExpanded = false }
                            ) {
                                accounts.forEachIndexed { idx, acc ->
                                    DropdownMenuItem(
                                        text = { Text("${acc.name} (${formatAmount(acc.balance, currencySymbol)})") },
                                        onClick = {
                                            selectedDestIndex = idx
                                            destExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        // Account select dropdown launcher
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = accounts[selectedAccountIndex].name,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(if (transactionType == "expense") "Account Source" else "Account Destination") },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { accountExpanded = true }
                            )
                            DropdownMenu(
                                expanded = accountExpanded,
                                onDismissRequest = { accountExpanded = false }
                            ) {
                                accounts.forEachIndexed { idx, acc ->
                                    DropdownMenuItem(
                                        text = { Text("${acc.name} (${formatAmount(acc.balance, currencySymbol)})") },
                                        onClick = {
                                            selectedAccountIndex = idx
                                            accountExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Category dropdown picker
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = categories[selectedCategoryIndex],
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Category") },
                                trailingIcon = { Icon(Icons.Default.Category, null) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { categoryExpanded = true }
                            )
                            DropdownMenu(
                                expanded = categoryExpanded,
                                onDismissRequest = { categoryExpanded = false }
                            ) {
                                categories.forEachIndexed { index, cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat) },
                                        onClick = {
                                            selectedCategoryIndex = index
                                            categoryExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Optional text note descriptor
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Transaction Note / Description") },
                    singleLine = true,
                    placeholder = { Text("e.g. Pocket transfer, Rent") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Action controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val amtValue = amountStr.toDoubleOrNull() ?: 0.0
                            if (amtValue > 0 && accounts.isNotEmpty()) {
                                if (transactionType == "transfer") {
                                    val fromAccId = accounts[selectedSourceIndex].id
                                    val toAccId = accounts[selectedDestIndex].id
                                    if (fromAccId != toAccId) {
                                        onTransferConfirm(fromAccId, toAccId, amtValue, note)
                                    }
                                } else {
                                    val finalTitle = title.ifBlank { categories[selectedCategoryIndex] }
                                    onConfirm(
                                        accounts[selectedAccountIndex].id,
                                        finalTitle,
                                        categories[selectedCategoryIndex],
                                        amtValue,
                                        transactionType == "expense",
                                        note
                                    )
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (transactionType) {
                                "expense" -> CoralRed
                                "income" -> MintGreen
                                else -> SkyBlue
                            },
                            contentColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.testTag("add_transaction_submit")
                    ) {
                        Text(if (transactionType == "transfer") "Transfer Funds" else "Track Entry", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Create Account Dialog
@Composable
fun AddAccountDialog(
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: String, balance: Double, iconName: String, excludeFromTotal: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var balanceStr by remember { mutableStateOf("") }

    val types = listOf("Checking", "Savings", "Cash", "Credit Card")
    var selectedTypeIndex by remember { mutableStateOf(0) }
    var typeExpanded by remember { mutableStateOf(false) }

    val iconChoices = listOf(
        "AccountBalance" to "Bank institution",
        "Wallet" to "Pocket Wallet",
        "CreditCard" to "Credit Cards",
        "Savings" to "Savings Vault"
    )
    var selectedIconIndex by remember { mutableStateOf(0) }
    var iconExpanded by remember { mutableStateOf(false) }

    var excludeFromTotal by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Create Asset Account",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account Name") },
                    singleLine = true,
                    placeholder = { Text("e.g. CapitalOne Checking") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = balanceStr,
                    onValueChange = { balanceStr = it },
                    label = { Text("Initial Balance ($currencySymbol)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // Select Account Type
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = types[selectedTypeIndex],
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Account Type") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { typeExpanded = true }
                    )
                    DropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        types.forEachIndexed { index, item ->
                            DropdownMenuItem(
                                text = { Text(item) },
                                onClick = {
                                    selectedTypeIndex = index
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }

                // Select Visual Icon representing account style
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = iconChoices[selectedIconIndex].second,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Visual Icon Theme") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { iconExpanded = true }
                    )
                    DropdownMenu(
                        expanded = iconExpanded,
                        onDismissRequest = { iconExpanded = false }
                    ) {
                        iconChoices.forEachIndexed { idx, pair ->
                            DropdownMenuItem(
                                text = { Text(pair.second) },
                                onClick = {
                                    selectedIconIndex = idx
                                    iconExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Exclude from Total Balance",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = excludeFromTotal,
                        onCheckedChange = { excludeFromTotal = it }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val bal = balanceStr.toDoubleOrNull() ?: 0.0
                            if (name.isNotBlank()) {
                                onConfirm(
                                    name,
                                    types[selectedTypeIndex],
                                    bal,
                                    iconChoices[selectedIconIndex].first,
                                    excludeFromTotal
                                )
                            }
                        }
                    ) {
                        Text("Add Account")
                    }
                }
            }
        }
    }
}

// Create Budget limit parameters
@Composable
fun AddBudgetDialog(
    existingCategories: List<String>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (category: String, limitAmount: Double) -> Unit
) {
    var limitStr by remember { mutableStateOf("") }

    val categories = listOf("Food & Groceries", "Shopping", "Entertainment", "Transport", "Bills & Utilities", "Health", "Other")
        .filter { !existingCategories.contains(it) }
    
    var selectedCatIndex by remember { mutableStateOf(0) }
    var categoryExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Establish Monthly Budget",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (categories.isEmpty()) {
                    Text(
                        text = "All primary categories already have configured budgets defined!",
                        color = CoralRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = categories[selectedCatIndex],
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Budget Category") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { categoryExpanded = true }
                        )
                        DropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            categories.forEachIndexed { itemIdx, item ->
                                DropdownMenuItem(
                                    text = { Text(item) },
                                    onClick = {
                                        selectedCatIndex = itemIdx
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = limitStr,
                    onValueChange = { limitStr = it },
                    label = { Text("Monthly Target Limit ($currencySymbol)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val lim = limitStr.toDoubleOrNull() ?: 0.0
                            if (lim > 0 && categories.isNotEmpty()) {
                                onConfirm(categories[selectedCatIndex], lim)
                            }
                        },
                        enabled = categories.isNotEmpty()
                    ) {
                        Text("Save Limit")
                    }
                }
            }
        }
    }
}

// Create Recurring scheduled Events
@Composable
fun AddRecurringEventDialog(
    accounts: List<Account>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (title: String, amount: Double, isExpense: Boolean, category: String, frequency: String, dateMillis: Long, accountId: Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var isExpense by remember { mutableStateOf(true) }

    val frequencies = listOf("Daily", "Weekly", "Monthly", "Yearly")
    var selectedFreqIndex by remember { mutableStateOf(2) } // Monthly default
    var freqExpanded by remember { mutableStateOf(false) }

    val categories = listOf("Food & Groceries", "Shopping", "Entertainment", "Transport", "Bills & Utilities", "Health", "Salary", "Other")
    var selectedCatIndex by remember { mutableStateOf(4) } // Bills default
    var catExpanded by remember { mutableStateOf(false) }

    var selectedAccountIndex by remember { mutableStateOf(0) }
    var accExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Add Recurring Transfer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Schedules Label (e.g. Rent, Gym)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Transaction Amount ($currencySymbol)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // Expense / Income toggle
                TabRow(
                    selectedTabIndex = if (isExpense) 0 else 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp)),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    indicator = {}
                ) {
                    Tab(
                        selected = isExpense,
                        onClick = { isExpense = true },
                        text = { Text("Expense Charge") },
                        selectedContentColor = CoralRed
                    )
                    Tab(
                        selected = !isExpense,
                        onClick = { isExpense = false },
                        text = { Text("Income Release") },
                        selectedContentColor = MintGreen
                    )
                }

                // Frequency Select
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = frequencies[selectedFreqIndex],
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Repeat Frequency") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { freqExpanded = true }
                    )
                    DropdownMenu(
                        expanded = freqExpanded,
                        onDismissRequest = { freqExpanded = false }
                    ) {
                        frequencies.forEachIndexed { itemIdx, item ->
                            DropdownMenuItem(
                                text = { Text(item) },
                                onClick = {
                                    selectedFreqIndex = itemIdx
                                    freqExpanded = false
                                }
                            )
                        }
                    }
                }

                // Category selection dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = categories[selectedCatIndex],
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { Icon(Icons.Default.Category, null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { catExpanded = true }
                    )
                    DropdownMenu(
                        expanded = catExpanded,
                        onDismissRequest = { catExpanded = false }
                    ) {
                        categories.forEachIndexed { idx, item ->
                            DropdownMenuItem(
                                text = { Text(item) },
                                onClick = {
                                    selectedCatIndex = idx
                                    catExpanded = false
                                }
                            )
                        }
                    }
                }

                if (accounts.isEmpty()) {
                    Text("⚠️ Add an account source first!", color = CoralRed, style = MaterialTheme.typography.bodySmall)
                } else {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = accounts[selectedAccountIndex].name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Debit/Credit Account Link") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { accExpanded = true }
                        )
                        DropdownMenu(
                            expanded = accExpanded,
                            onDismissRequest = { accExpanded = false }
                        ) {
                            accounts.forEachIndexed { itemIdx, acc ->
                                DropdownMenuItem(
                                    text = { Text(acc.name) },
                                    onClick = {
                                        selectedAccountIndex = itemIdx
                                        accExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amt = amountStr.toDoubleOrNull() ?: 0.0
                            if (title.isNotBlank() && amt > 0 && accounts.isNotEmpty()) {
                                // Default next execution is tomorrow
                                val cal = Calendar.getInstance()
                                cal.add(Calendar.DAY_OF_YEAR, 1)
                                onConfirm(
                                    title,
                                    amt,
                                    isExpense,
                                    categories[selectedCatIndex],
                                    frequencies[selectedFreqIndex],
                                    cal.timeInMillis,
                                    accounts[selectedAccountIndex].id
                                )
                            }
                        },
                        enabled = accounts.isNotEmpty()
                    ) {
                        Text("Schedule Recurring")
                    }
                }
            }
        }
    }
}

// Add Savings Goal tracking
@Composable
fun AddGoalDialog(
    accounts: List<Account>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (title: String, targetAmount: Double, currentAmount: Double, deadlineMillis: Long, linkedAccountId: Int, excludeFromTotal: Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var targetStr by remember { mutableStateOf("") }
    var currentStr by remember { mutableStateOf("") }

    val daysChoices = listOf(30, 90, 180, 365)
    val daysLabels = listOf("1 Month", "3 Months (Short Term)", "6 Months", "1 Year (Long Term)")
    var selectedDaysIndex by remember { mutableStateOf(1) }
    var daysExpanded by remember { mutableStateOf(false) }

    var linkedAccountId by remember { mutableStateOf(-1) }
    var excludeFromTotal by remember { mutableStateOf(true) }
    var accountExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Establish Savings Goal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Savings target (e.g. Trip fund, Car downpayment)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = targetStr,
                    onValueChange = { targetStr = it },
                    label = { Text("Target Amount ($currencySymbol)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = currentStr,
                    onValueChange = { currentStr = it },
                    label = { Text("Already Saved Amount ($currencySymbol)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // Select deadline period
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = daysLabels[selectedDaysIndex],
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Savings Period Target") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { daysExpanded = true }
                    )
                    DropdownMenu(
                        expanded = daysExpanded,
                        onDismissRequest = { daysExpanded = false }
                    ) {
                        daysLabels.forEachIndexed { idx, item ->
                            DropdownMenuItem(
                                text = { Text(item) },
                                onClick = {
                                    selectedDaysIndex = idx
                                    daysExpanded = false
                                }
                            )
                        }
                    }
                }

                // Select Linked Account
                Box(modifier = Modifier.fillMaxWidth()) {
                    val selectedAccountName = if (linkedAccountId == -1) "Create New Savings Account" else accounts.find { it.id == linkedAccountId }?.name ?: "Create New Savings Account"
                    OutlinedTextField(
                        value = selectedAccountName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Link to Account") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { accountExpanded = true }
                    )
                    DropdownMenu(
                        expanded = accountExpanded,
                        onDismissRequest = { accountExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Create New Savings Account") },
                            onClick = {
                                linkedAccountId = -1
                                accountExpanded = false
                            }
                        )
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text("${acc.name} (${acc.type})") },
                                onClick = {
                                    linkedAccountId = acc.id
                                    accountExpanded = false
                                }
                            )
                        }
                    }
                }

                if (linkedAccountId == -1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Exclude Dedicated Account from Total Balance",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Switch(
                            checked = excludeFromTotal,
                            onCheckedChange = { excludeFromTotal = it }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val target = targetStr.toDoubleOrNull() ?: 0.0
                            val cur = currentStr.toDoubleOrNull() ?: 0.0
                            if (title.isNotBlank() && target > 0) {
                                val cal = Calendar.getInstance()
                                cal.add(Calendar.DAY_OF_YEAR, daysChoices[selectedDaysIndex])
                                onConfirm(
                                    title,
                                    target,
                                    cur,
                                    cal.timeInMillis,
                                    linkedAccountId,
                                    excludeFromTotal
                                )
                            }
                        }
                    ) {
                        Text("Add Target Goal")
                    }
                }
            }
        }
    }
}

// Contribute to Savings Goal Dialog
@Composable
fun ContributeGoalDialog(
    goalTitle: String,
    accounts: List<Account>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (accountId: Int, amount: Double) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var selectedAccountIndex by remember { mutableStateOf(0) }
    var accountExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Deposit to Savings Goal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = goalTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                // Source Account Selection Dropdown
                if (accounts.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = accounts[selectedAccountIndex].name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Source Account") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { accountExpanded = true }
                        )
                        DropdownMenu(
                            expanded = accountExpanded,
                            onDismissRequest = { accountExpanded = false }
                        ) {
                            accounts.forEachIndexed { idx, acc ->
                                DropdownMenuItem(
                                    text = { Text("${acc.name} (${formatAmount(acc.balance, currencySymbol)})") },
                                    onClick = {
                                        selectedAccountIndex = idx
                                        accountExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "No active source accounts available (non-excluded). Please create a standard asset account first.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Transfer Deposit Amount ($currencySymbol)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = accounts.isNotEmpty()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Dismiss") }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val amt = amountStr.toDoubleOrNull() ?: 0.0
                            if (amt > 0 && accounts.isNotEmpty()) {
                                onConfirm(accounts[selectedAccountIndex].id, amt)
                            }
                        },
                        enabled = accounts.isNotEmpty()
                    ) {
                        Text("Record Savings")
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------------
// PERSONALIZATION: SETTINGS DIALOG
// ----------------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    preferences: UserPreferences,
    onDismiss: () -> Unit,
    onUpdateName: (String) -> Unit,
    onUpdateCurrency: (String) -> Unit,
    onUpdateTheme: (String) -> Unit,
    onUpdateAccentColor: (String) -> Unit
) {
    var name by remember { mutableStateOf(preferences.userName) }

    val currencies = listOf("$", "€", "£", "₹", "¥")
    var currencyExpanded by remember { mutableStateOf(false) }

    val themes = listOf("System", "Light", "Dark", "OLED")
    var themeExpanded by remember { mutableStateOf(false) }

    val accentColors = remember {
        buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add("Dynamic" to "Dynamic (Material You)")
            }
            add("MintGreen" to "Mint Green")
            add("SkyBlue" to "Sky Blue")
            add("LavenderPurple" to "Lavender Purple")
            add("CoralRed" to "Coral Red")
            add("SunnyYellow" to "Sunny Yellow")
        }
    }
    var accentExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Personalize Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                // Name Input
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Profile Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Currency Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = preferences.currencySymbol,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Preferred Currency") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { currencyExpanded = true }
                    )
                    DropdownMenu(
                        expanded = currencyExpanded,
                        onDismissRequest = { currencyExpanded = false }
                    ) {
                        currencies.forEach { cur ->
                            DropdownMenuItem(
                                text = { Text(cur) },
                                onClick = {
                                    onUpdateCurrency(cur)
                                    currencyExpanded = false
                                }
                            )
                        }
                    }
                }

                // Theme Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = preferences.themeMode,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Theme Mode") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { themeExpanded = true }
                    )
                    DropdownMenu(
                        expanded = themeExpanded,
                        onDismissRequest = { themeExpanded = false }
                    ) {
                        themes.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t) },
                                onClick = {
                                    onUpdateTheme(t)
                                    themeExpanded = false
                                }
                            )
                        }
                    }
                }

                // Accent Color Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    val currentLabel = accentColors.find { it.first == preferences.accentColor }?.second ?: "Mint Green"
                    OutlinedTextField(
                        value = currentLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Primary Accent Color") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { accentExpanded = true }
                    )
                    DropdownMenu(
                        expanded = accentExpanded,
                        onDismissRequest = { accentExpanded = false }
                    ) {
                        accentColors.forEach { (colorKey, colorLabel) ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Visual Indicator box
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    when (colorKey) {
                                                        "SkyBlue" -> SkyBlue
                                                        "LavenderPurple" -> LavenderPurple
                                                        "CoralRed" -> CoralRed
                                                        "SunnyYellow" -> SunnyYellow
                                                        "Dynamic" -> MaterialTheme.colorScheme.primary
                                                        else -> MintGreen
                                                    }
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(colorLabel)
                                    }
                                },
                                onClick = {
                                    onUpdateAccentColor(colorKey)
                                    accentExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onUpdateName(name)
                            }
                            onDismiss()
                        }
                    ) {
                        Text("Save & Close")
                    }
                }
            }
        }
    }
}

data class ConfettiParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Color,
    val size: Float,
    var rotation: Float,
    val rotationSpeed: Float,
    var alpha: Float = 1f
)

@Composable
fun ConfettiOverlay(
    trigger: Boolean,
    onFinished: () -> Unit
) {
    if (!trigger) return

    val particleList = remember { mutableStateListOf<ConfettiParticle>() }
    val colors = listOf(
        Color(0xFF386B20), // Mint Green
        Color(0xFF00668B), // Sky Blue
        Color(0xFF7F4D9C), // Lavender Purple
        Color(0xFFBA1A1A), // Coral Red
        Color(0xFFE2C000)  // Sunny Yellow
    )
    var screenWidth by remember { mutableStateOf(1080f) }
    var frameTick by remember { mutableStateOf(0) }

    LaunchedEffect(trigger) {
        if (trigger) {
            particleList.clear()
            repeat(150) {
                particleList.add(
                    ConfettiParticle(
                        x = Random.nextFloat() * screenWidth,
                        y = -50f - Random.nextFloat() * 200f, // stagger initial drops
                        vx = (Random.nextFloat() - 0.5f) * 12f,
                        vy = Random.nextFloat() * 12f + 8f,
                        color = colors.random(),
                        size = Random.nextFloat() * 16f + 14f,
                        rotation = Random.nextFloat() * 360f,
                        rotationSpeed = (Random.nextFloat() - 0.5f) * 15f
                    )
                )
            }

            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < 3500) { // Animate for 3.5 seconds
                withFrameMillis { frameTime ->
                    val elapsed = System.currentTimeMillis() - startTime
                    for (particle in particleList) {
                        particle.x += particle.vx
                        particle.y += particle.vy
                        particle.vy += 0.4f // gravity simulation
                        particle.rotation += particle.rotationSpeed
                        
                        // Slowly fade out after 2.5 seconds
                        if (elapsed > 2500) {
                            particle.alpha = ((3500f - elapsed) / 1000f).coerceIn(0f, 1f)
                        }
                    }
                    frameTick++
                }
            }
            onFinished()
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                screenWidth = coordinates.size.width.toFloat()
            }
    ) {
        val tick = frameTick
        particleList.forEach { particle ->
            if (particle.alpha > 0f) {
                rotate(degrees = particle.rotation, pivot = Offset(particle.x, particle.y)) {
                    drawRect(
                        color = particle.color.copy(alpha = particle.alpha),
                        topLeft = Offset(particle.x - particle.size / 2, particle.y - particle.size / 2),
                        size = Size(particle.size, particle.size * 0.5f)
                    )
                }
            }
        }
    }
}
