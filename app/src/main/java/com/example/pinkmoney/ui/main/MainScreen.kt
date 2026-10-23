package com.example.pinkmoney.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.pinkmoney.data.db.PinkMoneyDatabase
import com.example.pinkmoney.data.entity.TransactionEntity
import com.example.pinkmoney.data.model.SpendingLimitManager
import com.example.pinkmoney.ui.alerts.AlertsDialog
import com.example.pinkmoney.ui.settings.SettingsDialog
import com.example.pinkmoney.ui.transactions.TransactionListScreen
import com.example.pinkmoney.ui.transactions.TransactionViewModel
import com.example.pinkmoney.utils.TransactionTypeDetector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: TransactionViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val db = remember { PinkMoneyDatabase.getInstance(context) }
    val unreadAlertsCount by db.budgetAlertDao().getUnreadCount().collectAsState(initial = 0)

    var selectedTab by remember { mutableStateOf(0) }
    var selectedTxn by remember { mutableStateOf<TransactionEntity?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showAlertDialog by remember { mutableStateOf(false) }
    var refreshLimitsCounter by remember { mutableIntStateOf(0) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "PinkMoney",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                actions = {
                    IconButton(onClick = { showAlertDialog = true }) {
                        if (unreadAlertsCount > 0) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ) {
                                        Text("$unreadAlertsCount")
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = "Alerts",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Alerts",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    IconButton(onClick = {
                        requestNotificationPermissionIfNeeded()
                        showSettingsDialog = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, null) },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.AccountTree, null) },
                    label = { Text("Buckets") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.BarChart, null) },
                    label = { Text("Stats") }
                )
            }
        }
    ) { padding ->

        Column(modifier = Modifier.padding(padding)) {
            SpendingLimitBanner(
                viewModel = viewModel,
                refreshKey = refreshLimitsCounter,
                onOpenSettings = {
                    requestNotificationPermissionIfNeeded()
                    showSettingsDialog = true
                }
            )

            when (selectedTab) {
                0 -> TransactionListScreen(
                    transactionsFlow = viewModel.transactions,
                    onCategorizeClick = { txn ->
                        selectedTxn = txn
                        selectedTab = 1
                    },
                    modifier = Modifier.weight(1f)
                )

                1 -> BucketsScreen(
                    incomingTransaction = selectedTxn,
                    onDone = {
                        selectedTxn = null
                    },
                    modifier = Modifier.weight(1f)
                )

                2 -> StatsScreen(
                    transactionsFlow = viewModel.transactions,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (showSettingsDialog) {
        SettingsDialog(
            onDismiss = { showSettingsDialog = false },
            onSettingsSaved = {
                refreshLimitsCounter++
            }
        )
    }

    if (showAlertDialog) {
        AlertsDialog(
            onDismiss = { showAlertDialog = false }
        )
    }
}

@Composable
private fun SpendingLimitBanner(
    viewModel: TransactionViewModel,
    refreshKey: Int,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val limitManager = remember(refreshKey) { SpendingLimitManager(context) }
    val settings = remember(refreshKey) { limitManager.getSettings() }

    if (!settings.isEnabled || !settings.overallLimitEnabled || settings.overallLimitAmount <= 0.0) {
        return
    }

    val transactions by viewModel.transactions.collectAsState(initial = emptyList())
    val periodStartTimestamp = remember(settings.period) {
        limitManager.getPeriodStartTimestamp(settings.period)
    }

    val spentInPeriod = remember(transactions, periodStartTimestamp) {
        transactions
            .filter { it.timestamp >= periodStartTimestamp }
            .filter { txn ->
                when {
                    txn.transactionType.equals("DEBIT", ignoreCase = true) -> true
                    txn.transactionType.equals("CREDIT", ignoreCase = true) -> false
                    else -> TransactionTypeDetector.detect(txn.rawText) == TransactionTypeDetector.TransactionType.DEBIT
                }
            }
            .sumOf { it.amount }
    }

    val limit = settings.overallLimitAmount
    val ratio = (spentInPeriod / limit).toFloat()
    val progress = ratio.coerceIn(0f, 1f)
    val percentage = (ratio * 100).toInt()
    val isExceeded = spentInPeriod > limit
    val isWarning = ratio >= 0.8f

    val bannerColor = when {
        isExceeded -> MaterialTheme.colorScheme.errorContainer
        isWarning -> Color(0xFFFFF3CD)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = when {
        isExceeded -> MaterialTheme.colorScheme.onErrorContainer
        isWarning -> Color(0xFF856404)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val progressColor = when {
        isExceeded -> MaterialTheme.colorScheme.error
        isWarning -> Color(0xFFFF9800)
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onOpenSettings),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = bannerColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isWarning || isExceeded) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = progressColor,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                    Text(
                        text = "${settings.period.label} Budget: ₹${spentInPeriod.toInt()} / ₹${limit.toInt()}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = progressColor
                )
            }

            Spacer(Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            if (isExceeded) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "⚠️ Limit exceeded by ₹${(spentInPeriod - limit).toInt()}!",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
