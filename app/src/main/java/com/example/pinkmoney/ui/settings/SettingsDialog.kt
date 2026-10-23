package com.example.pinkmoney.ui.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.pinkmoney.data.db.PinkMoneyDatabase
import com.example.pinkmoney.data.model.LimitPeriod
import com.example.pinkmoney.data.model.SpendingLimitManager
import com.example.pinkmoney.data.model.SpendingLimitSettings

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    onSettingsSaved: () -> Unit
) {
    val context = LocalContext.current
    val limitManager = remember { SpendingLimitManager(context) }
    val db = remember { PinkMoneyDatabase.getInstance(context) }
    val buckets by db.bucketDao().getBuckets().collectAsState(initial = emptyList())

    val initialSettings = remember { limitManager.getSettings() }

    var isEnabled by remember { mutableStateOf(initialSettings.isEnabled) }
    var selectedPeriod by remember { mutableStateOf(initialSettings.period) }
    var overallLimitEnabled by remember { mutableStateOf(initialSettings.overallLimitEnabled) }
    var overallLimitText by remember {
        mutableStateOf(if (initialSettings.overallLimitAmount > 0) initialSettings.overallLimitAmount.toInt().toString() else "")
    }

    val bucketLimitsState = remember(buckets, initialSettings) {
        val map = mutableStateMapOf<String, Pair<Boolean, String>>()
        buckets.forEach { bucket ->
            val limit = initialSettings.bucketLimits[bucket.name] ?: 0.0
            map[bucket.name] = Pair(limit > 0, if (limit > 0) limit.toInt().toString() else "")
        }
        map
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val overallAmount = overallLimitText.toDoubleOrNull() ?: 0.0
                    val bucketLimitsMap = mutableMapOf<String, Double>()
                    bucketLimitsState.forEach { (bucketName, state) ->
                        val (enabled, text) = state
                        if (enabled) {
                            bucketLimitsMap[bucketName] = text.toDoubleOrNull() ?: 0.0
                        }
                    }

                    val newSettings = SpendingLimitSettings(
                        isEnabled = isEnabled,
                        period = selectedPeriod,
                        overallLimitEnabled = overallLimitEnabled,
                        overallLimitAmount = overallAmount,
                        bucketLimits = bucketLimitsMap
                    )
                    limitManager.saveSettings(newSettings)
                    onSettingsSaved()
                    onDismiss()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("Spending Limits Settings") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Enable Spending Limits",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Track and get warnings when spending exceeds limits",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isEnabled,
                                onCheckedChange = { isEnabled = it }
                            )
                        }
                    }
                }

                if (isEnabled) {
                    item {
                        Text(
                            text = "Limit Period",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            LimitPeriod.entries.forEach { period ->
                                FilterChip(
                                    selected = selectedPeriod == period,
                                    onClick = { selectedPeriod = period },
                                    label = {
                                        Text(
                                            text = period.label,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                )
                            }
                        }
                    }

                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Overall Spending Limit",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Switch(
                                        checked = overallLimitEnabled,
                                        onCheckedChange = { overallLimitEnabled = it }
                                    )
                                }

                                if (overallLimitEnabled) {
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = overallLimitText,
                                        onValueChange = { overallLimitText = it },
                                        label = { Text("Limit Amount (₹)") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Bucket Limits",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Set limits for individual expense categories",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (buckets.isEmpty()) {
                        item {
                            Text(
                                text = "No buckets created yet. Create buckets in the Buckets tab.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(
                            items = buckets,
                            key = { bucket -> bucket.id }
                        ) { bucket ->
                            val current = bucketLimitsState[bucket.name] ?: Pair(false, "")
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = bucket.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Switch(
                                            checked = current.first,
                                            onCheckedChange = { checked ->
                                                bucketLimitsState[bucket.name] = Pair(checked, current.second)
                                            }
                                        )
                                    }

                                    if (current.first) {
                                        Spacer(Modifier.height(6.dp))
                                        OutlinedTextField(
                                            value = current.second,
                                            onValueChange = { text ->
                                                bucketLimitsState[bucket.name] = Pair(true, text)
                                            },
                                            label = { Text("${bucket.name} Limit (₹)") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}
