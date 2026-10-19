package com.example.pinkmoney.ui.main

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.pinkmoney.data.db.PinkMoneyDatabase
import com.example.pinkmoney.data.entity.TransactionEntity
import com.example.pinkmoney.utils.MerchantCategoryClassifier
import com.example.pinkmoney.utils.TransactionDeduplicator
import com.example.pinkmoney.utils.TransactionTypeDetector
import kotlinx.coroutines.flow.Flow
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class DateRangeFilter(val label: String) {
    ALL_TIME("All Time"),
    LAST_7_DAYS("Last 7 Days"),
    LAST_30_DAYS("Last 30 Days"),
    LAST_YEAR("Last Year"),
    CUSTOM("Custom")
}

enum class SortOption(val label: String) {
    AMOUNT_DESC("Amount: High to Low"),
    AMOUNT_ASC("Amount: Low to High"),
    COUNT_DESC("Transactions: High to Low"),
    COUNT_ASC("Transactions: Low to High"),
    NAME_ASC("Name: A to Z")
}

@Composable
fun StatsScreen(
    transactionsFlow: Flow<List<TransactionEntity>>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val db = remember { PinkMoneyDatabase.getInstance(context) }
    val rawTransactions by transactionsFlow.collectAsState(initial = emptyList())
    val buckets by db.bucketDao()
        .getBuckets()
        .collectAsState(initial = emptyList())
    val transactions = remember(rawTransactions) {
        TransactionDeduplicator.dedupe(rawTransactions)
    }

    var selectedCategoryName by remember { mutableStateOf<String?>(null) }
    var selectedDateFilter by remember { mutableStateOf(DateRangeFilter.ALL_TIME) }
    var selectedSortOption by remember { mutableStateOf(SortOption.AMOUNT_DESC) }

    var customStartDate by remember { mutableStateOf<Long?>(null) }
    var customEndDate by remember { mutableStateOf<Long?>(null) }
    var showCustomDateRangeDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    val filteredTransactions = remember(transactions, selectedDateFilter, customStartDate, customEndDate) {
        val now = System.currentTimeMillis()
        val (minTimestamp, maxTimestamp) = when (selectedDateFilter) {
            DateRangeFilter.ALL_TIME -> null to null
            DateRangeFilter.LAST_7_DAYS -> {
                val start = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -6)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                start to now
            }
            DateRangeFilter.LAST_30_DAYS -> {
                val start = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -29)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                start to now
            }
            DateRangeFilter.LAST_YEAR -> {
                val start = Calendar.getInstance().apply {
                    add(Calendar.YEAR, -1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                start to now
            }
            DateRangeFilter.CUSTOM -> {
                val start = customStartDate?.let { getStartOfDay(it) }
                val end = customEndDate?.let { getEndOfDay(it) }
                start to end
            }
        }

        transactions.filter { txn ->
            val afterStart = minTimestamp == null || txn.timestamp >= minTimestamp
            val beforeEnd = maxTimestamp == null || txn.timestamp <= maxTimestamp
            afterStart && beforeEnd
        }
    }

    val spentByCategory = remember(filteredTransactions, buckets, selectedSortOption, selectedDateFilter) {
        val bucketNames = buckets.map { it.name }

        val groupedTransactions = filteredTransactions
            .groupBy { transaction ->
                effectiveCategory(transaction, bucketNames)
            }
            .mapValues { (_, categoryTransactions) ->
                val expenseTransactions = categoryTransactions.filter(::isExpenseTransaction)
                expenseTransactions.sumOf { it.amount } to expenseTransactions.size
            }

        val orderedCategories = buildList {
            addAll(bucketNames)
            addAll(groupedTransactions.keys.filterNot { groupedCategory ->
                bucketNames.any { bucketName ->
                    sameCategory(bucketName, groupedCategory)
                }
            })
        }

        val rawCategorySpends = orderedCategories.map { category ->
            val spend = groupedTransactions[category] ?: (0.0 to 0)
            CategorySpend(
                name = category,
                amount = spend.first,
                transactionCount = spend.second
            )
        }

        val activeSpends = if (selectedDateFilter == DateRangeFilter.ALL_TIME) {
            rawCategorySpends
        } else {
            rawCategorySpends.filter { it.amount > 0 || it.transactionCount > 0 }
        }

        when (selectedSortOption) {
            SortOption.AMOUNT_DESC -> activeSpends.sortedWith(
                compareByDescending<CategorySpend> { it.amount }
                    .thenByDescending { it.transactionCount }
                    .thenBy { it.name.lowercase(Locale.ROOT) }
            )
            SortOption.AMOUNT_ASC -> activeSpends.sortedWith(
                compareBy<CategorySpend> { it.amount }
                    .thenBy { it.name.lowercase(Locale.ROOT) }
            )
            SortOption.COUNT_DESC -> activeSpends.sortedWith(
                compareByDescending<CategorySpend> { it.transactionCount }
                    .thenByDescending { it.amount }
                    .thenBy { it.name.lowercase(Locale.ROOT) }
            )
            SortOption.COUNT_ASC -> activeSpends.sortedWith(
                compareBy<CategorySpend> { it.transactionCount }
                    .thenBy { it.name.lowercase(Locale.ROOT) }
            )
            SortOption.NAME_ASC -> activeSpends.sortedBy { it.name.lowercase(Locale.ROOT) }
        }
    }

    val totalSpent = spentByCategory.sumOf { it.amount }
    val maxCategoryAmount = remember(spentByCategory) { spentByCategory.maxOfOrNull { it.amount } ?: 0.0 }
    val totalExpenseCount = remember(filteredTransactions) { filteredTransactions.count(::isExpenseTransaction) }

    val periodLabel = remember(selectedDateFilter, customStartDate, customEndDate) {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        when (selectedDateFilter) {
            DateRangeFilter.ALL_TIME -> "All time"
            DateRangeFilter.LAST_7_DAYS -> "Last 7 days"
            DateRangeFilter.LAST_30_DAYS -> "Last 30 days"
            DateRangeFilter.LAST_YEAR -> "Last year"
            DateRangeFilter.CUSTOM -> {
                if (customStartDate != null && customEndDate != null) {
                    "${dateFormat.format(Date(customStartDate!!))} - ${dateFormat.format(Date(customEndDate!!))}"
                } else {
                    "Custom period"
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Text(
            text = "Stats",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Spending by category",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(14.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Total spent",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatRupees(totalSpent),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "$periodLabel • $totalExpenseCount expense transactions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Date Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DateRangeFilter.entries.forEach { filter ->
                DateFilterChip(
                    label = filter.label,
                    isSelected = selectedDateFilter == filter,
                    onClick = {
                        if (filter == DateRangeFilter.CUSTOM) {
                            showCustomDateRangeDialog = true
                        } else {
                            selectedDateFilter = filter
                        }
                    }
                )
            }
        }

        if (selectedDateFilter == DateRangeFilter.CUSTOM && customStartDate != null && customEndDate != null) {
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.clickable { showCustomDateRangeDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "$periodLabel (Tap to change)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Sorting & Summary Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${spentByCategory.size} categories",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Box {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.clickable { showSortMenu = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sort",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = selectedSortOption.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    SortOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option.label,
                                    fontWeight = if (option == selectedSortOption) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                selectedSortOption = option
                                showSortMenu = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        if (spentByCategory.isEmpty()) {
            EmptyStatsState(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = spentByCategory,
                    key = { category -> category.name }
                ) { category ->
                    CategorySpendRow(
                        category = category,
                        maxAmount = maxCategoryAmount,
                        onClick = { selectedCategoryName = category.name }
                    )
                }
            }
        }
    }

    selectedCategoryName?.let { categoryName ->
        val bucketNames = remember(buckets) { buckets.map { it.name } }
        val categoryTransactions = remember(filteredTransactions, categoryName, bucketNames) {
            filteredTransactions.filter { transaction ->
                effectiveCategory(transaction, bucketNames).equals(categoryName, ignoreCase = true)
            }.sortedByDescending { it.timestamp }
        }
        CategoryTransactionsDialog(
            categoryName = categoryName,
            transactions = categoryTransactions,
            onDismiss = { selectedCategoryName = null }
        )
    }

    if (showCustomDateRangeDialog) {
        CustomDateRangeDialog(
            initialStartDate = customStartDate,
            initialEndDate = customEndDate,
            onDismiss = { showCustomDateRangeDialog = false },
            onApply = { start, end ->
                customStartDate = start
                customEndDate = end
                selectedDateFilter = DateRangeFilter.CUSTOM
                showCustomDateRangeDialog = false
            }
        )
    }
}

@Composable
private fun DateFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun CustomDateRangeDialog(
    initialStartDate: Long?,
    initialEndDate: Long?,
    onDismiss: () -> Unit,
    onApply: (Long, Long) -> Unit
) {
    val context = LocalContext.current
    val now = System.currentTimeMillis()
    val defaultStart = remember { now - 7 * 24 * 60 * 60 * 1000L }

    var tempStart by remember { mutableLongStateOf(initialStartDate ?: defaultStart) }
    var tempEnd by remember { mutableLongStateOf(initialEndDate ?: now) }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val start = minOf(tempStart, tempEnd)
                    val end = maxOf(tempStart, tempEnd)
                    onApply(start, end)
                }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("Select Custom Date Range") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Choose start and end dates to filter your expenses.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "From:",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    OutlinedButton(
                        onClick = {
                            showDatePicker(context, tempStart) { selected ->
                                tempStart = selected
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(dateFormat.format(Date(tempStart)))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "To:",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    OutlinedButton(
                        onClick = {
                            showDatePicker(context, tempEnd) { selected ->
                                tempEnd = selected
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(dateFormat.format(Date(tempEnd)))
                    }
                }
            }
        }
    )
}

private fun showDatePicker(
    context: android.content.Context,
    initialMillis: Long,
    onDateSelected: (Long) -> Unit
) {
    val cal = Calendar.getInstance().apply { timeInMillis = initialMillis }
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selectedCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
            }
            onDateSelected(selectedCal.timeInMillis)
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    ).show()
}

private fun getStartOfDay(timestamp: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun getEndOfDay(timestamp: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis
}

private fun isExpenseTransaction(transaction: TransactionEntity): Boolean {
    return when {
        transaction.transactionType.equals("DEBIT", ignoreCase = true) -> true
        transaction.transactionType.equals("CREDIT", ignoreCase = true) -> false
        else -> {
            TransactionTypeDetector.detect(transaction.rawText) ==
                TransactionTypeDetector.TransactionType.DEBIT
        }
    }
}

private fun effectiveCategory(
    transaction: TransactionEntity,
    bucketNames: List<String>
): String {
    val storedCategory = transaction.category.trim()
    if (storedCategory.isNotEmpty() && !storedCategory.equals("Others", ignoreCase = true)) {
        return canonicalCategoryName(storedCategory, bucketNames)
    }

    val inferredCategory = MerchantCategoryClassifier.classify(transaction.merchant)
    val fallbackCategory = inferredCategory.ifBlank { "Others" }
    return canonicalCategoryName(fallbackCategory, bucketNames)
}

private fun canonicalCategoryName(
    category: String,
    bucketNames: List<String>
): String {
    val trimmedCategory = category.trim()
    if (trimmedCategory.isEmpty()) return "Others"

    bucketNames.firstOrNull { bucketName ->
        sameCategory(bucketName, trimmedCategory)
    }?.let { return it }

    return trimmedCategory
}

private fun sameCategory(first: String, second: String): Boolean {
    val normalizedFirst = normalizeCategoryKey(first)
    val normalizedSecond = normalizeCategoryKey(second)

    if (normalizedFirst == normalizedSecond) return true

    return setOf(normalizedFirst, normalizedSecond) == setOf("transfer", "transfers")
}

private fun normalizeCategoryKey(category: String): String {
    return category
        .trim()
        .lowercase(Locale.ROOT)
        .replace("&", " and ")
        .replace(Regex("[^a-z0-9]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

@Composable
private fun CategorySpendRow(
    category: CategorySpend,
    maxAmount: Double,
    onClick: () -> Unit
) {
    val progress = if (maxAmount <= 0.0) {
        0f
    } else {
        (category.amount / maxAmount).toFloat().coerceIn(0f, 1f)
    }
    val accentColor = bucketAccent(category.name)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .width(10.dp)
                        .height(34.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor
                ) {}

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${category.transactionCount} transactions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = formatRupees(category.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = accentColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun CategoryTransactionsDialog(
    categoryName: String,
    transactions: List<TransactionEntity>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        title = { Text(categoryName) },
        text = {
            if (transactions.isEmpty()) {
                Text(
                    text = "No transactions in this category yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = transactions,
                        key = { transaction ->
                            transaction.transactionHash.ifBlank { transaction.id.toString() }
                        }
                    ) { transaction ->
                        CategoryTransactionRow(transaction = transaction)
                    }
                }
            }
        }
    )
}

@Composable
private fun CategoryTransactionRow(
    transaction: TransactionEntity
) {
    val date = remember(transaction.timestamp) {
        SimpleDateFormat("dd MMM", Locale.getDefault())
            .format(Date(transaction.timestamp))
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.merchant ?: "Unknown",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$date • ${transaction.transactionType}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "₹${transaction.amount}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun EmptyStatsState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No spending data for this period.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private data class CategorySpend(
    val name: String,
    val amount: Double,
    val transactionCount: Int
)

private fun formatRupees(amount: Double): String {
    return NumberFormat
        .getCurrencyInstance(
            Locale.Builder()
                .setLanguage("en")
                .setRegion("IN")
                .build()
        )
        .format(amount)
}

private fun bucketAccent(name: String): Color {
    val palette = listOf(
        Color(0xFFE85D75),
        Color(0xFF2D9CDB),
        Color(0xFF27AE60),
        Color(0xFFF2C94C),
        Color(0xFFF2994A),
        Color(0xFF9B51E0),
        Color(0xFF56CCF2),
        Color(0xFFEB5757)
    )
    val index = name.lowercase().fold(0) { total, char -> total + char.code }
    return palette[index % palette.size]
}
