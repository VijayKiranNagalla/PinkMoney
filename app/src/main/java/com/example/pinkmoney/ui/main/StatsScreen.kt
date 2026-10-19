package com.example.pinkmoney.ui.main

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import java.util.Locale

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
    val spentByCategory = remember(transactions, buckets) {
        val bucketNames = buckets.map { it.name }

        val groupedTransactions = transactions
            .groupBy { transaction ->
                effectiveCategory(transaction, bucketNames)
            }
            .mapValues { (_, categoryTransactions) ->
                categoryTransactions
                    .filter(::isExpenseTransaction)
                    .sumOf { it.amount } to categoryTransactions.size
            }

        val orderedCategories = buildList {
            addAll(bucketNames)
            addAll(groupedTransactions.keys.filterNot { groupedCategory ->
                bucketNames.any { bucketName ->
                    sameCategory(bucketName, groupedCategory)
                }
            })
        }

        orderedCategories.map { category ->
            val spend = groupedTransactions[category] ?: (0.0 to 0)
            val amount = spend.first
            val count = spend.second

            CategorySpend(
                name = category,
                amount = amount,
                transactionCount = count
            )
        }.sortedWith(
            compareByDescending<CategorySpend> { it.amount }
                .thenBy { it.name.lowercase(Locale.ROOT) }
        )
    }
    val totalSpent = spentByCategory.sumOf { it.amount }

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

        Spacer(Modifier.height(18.dp))

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
            }
        }

        Spacer(Modifier.height(14.dp))

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
                        maxAmount = spentByCategory.first().amount
                    )
                }
            }
        }
    }
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
    maxAmount: Double
) {
    val progress = if (maxAmount <= 0.0) {
        0f
    } else {
        (category.amount / maxAmount).toFloat().coerceIn(0f, 1f)
    }
    val accentColor = bucketAccent(category.name)

    Card(
        modifier = Modifier.fillMaxWidth(),
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
private fun EmptyStatsState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No spending data yet.",
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
