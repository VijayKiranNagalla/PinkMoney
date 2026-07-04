package com.example.pinkmoney.ui.main

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.pinkmoney.data.db.PinkMoneyDatabase
import com.example.pinkmoney.data.entity.BucketEntity
import com.example.pinkmoney.data.entity.MerchantBucketMap
import com.example.pinkmoney.data.entity.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun BucketsScreen(
    incomingTransaction: TransactionEntity?,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { PinkMoneyDatabase.getInstance(context) }
    val buckets by db.bucketDao()
        .getBuckets()
        .collectAsState(initial = emptyList())

    val density = LocalDensity.current
    val bucketBounds = remember { mutableStateMapOf<String, Rect>() }
    var cardPosition by remember { mutableStateOf(Offset.Zero) }
    var hoveredBucket by remember { mutableStateOf<String?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var pendingBucket by remember { mutableStateOf<String?>(null) }
    var pendingTxn by remember { mutableStateOf<TransactionEntity?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var newBucketName by remember { mutableStateOf("") }

    fun requestAssignment(bucketName: String, txn: TransactionEntity?) {
        if (txn == null) return
        pendingBucket = bucketName
        pendingTxn = txn
        showConfirmDialog = true
    }

    fun createBucket() {
        val trimmedName = newBucketName.trim()
        if (trimmedName.isEmpty()) return

        scope.launch(Dispatchers.IO) {
            db.bucketDao().insertBucket(BucketEntity(name = trimmedName))
        }
        newBucketName = ""
        showDialog = false
    }

    fun saveAssignment() {
        val txn = pendingTxn ?: return
        val bucket = pendingBucket ?: return
        val merchant = txn.merchant ?: "Unknown"

        scope.launch(Dispatchers.IO) {
            db.bucketDao().saveMapping(
                MerchantBucketMap(
                    merchant = merchant,
                    bucket = bucket
                )
            )
            db.transactionDao().updateCategoryForMerchant(
                merchant = merchant,
                category = bucket
            )
        }

        Log.d("BUCKET_SAVE", "$merchant -> $bucket")
        showConfirmDialog = false
        pendingTxn = null
        pendingBucket = null
        onDone()
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val cardWidthPx = with(density) { 280.dp.toPx() }
        val cardHeightPx = with(density) { 104.dp.toPx() }
        val horizontalInsetPx = with(density) { 20.dp.toPx() }
        val bottomInsetPx = with(density) { 132.dp.toPx() }
        val containerWidthPx = with(density) { maxWidth.toPx() }
        val containerHeightPx = with(density) { maxHeight.toPx() }

        LaunchedEffect(incomingTransaction, containerWidthPx, containerHeightPx) {
            if (incomingTransaction != null) {
                cardPosition = Offset(
                    x = horizontalInsetPx.coerceAtMost((containerWidthPx - cardWidthPx).coerceAtLeast(0f)),
                    y = (containerHeightPx - bottomInsetPx).coerceAtLeast(horizontalInsetPx)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Buckets",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (incomingTransaction == null) {
                            "${buckets.size} saved categories"
                        } else {
                            "Categorizing ${incomingTransaction.merchant ?: "Unknown"}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { showDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Bucket")
                }
            }

            Spacer(Modifier.height(18.dp))

            if (buckets.isEmpty()) {
                EmptyBucketsState(
                    onCreateClick = { showDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(
                        items = buckets,
                        key = { bucket -> bucket.name }
                    ) { bucket ->
                        val bucketName = bucket.name
                        BucketCard(
                            name = bucketName,
                            selected = bucketName == incomingTransaction?.category,
                            hovered = bucketName == hoveredBucket,
                            enabled = incomingTransaction != null,
                            modifier = Modifier
                                .height(118.dp)
                                .onGloballyPositioned { coords ->
                                    bucketBounds[bucketName] = coords.boundsInRoot()
                                }
                                .clickable(enabled = incomingTransaction != null) {
                                    requestAssignment(bucketName, incomingTransaction)
                                }
                        )
                    }
                }
            }
        }

        if (incomingTransaction != null) {
            val maxX = (containerWidthPx - cardWidthPx).coerceAtLeast(0f)
            val maxY = (containerHeightPx - cardHeightPx).coerceAtLeast(0f)

            Card(
                modifier = Modifier
                    .zIndex(1f)
                    .width(280.dp)
                    .offset {
                        IntOffset(
                            cardPosition.x.roundToInt(),
                            cardPosition.y.roundToInt()
                        )
                    }
                    .pointerInput(bucketBounds) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                cardPosition = Offset(
                                    x = (cardPosition.x + dragAmount.x).coerceIn(0f, maxX),
                                    y = (cardPosition.y + dragAmount.y).coerceIn(0f, maxY)
                                )

                                val cardCenter = Offset(
                                    x = cardPosition.x + cardWidthPx / 2f,
                                    y = cardPosition.y + cardHeightPx / 2f
                                )
                                hoveredBucket = bucketBounds.entries
                                    .firstOrNull { (_, bounds) -> bounds.contains(cardCenter) }
                                    ?.key
                            },
                            onDragEnd = {
                                val bucket = hoveredBucket
                                if (bucket != null) {
                                    requestAssignment(bucket, incomingTransaction)
                                }
                                hoveredBucket = null
                            },
                            onDragCancel = {
                                hoveredBucket = null
                            }
                        )
                    },
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = incomingTransaction.merchant ?: "Unknown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Rs ${incomingTransaction.amount}",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { showDialog = true },
            icon = {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null
                )
            },
            text = { Text("New bucket") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        )

        if (showConfirmDialog && pendingTxn != null && pendingBucket != null) {
            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                confirmButton = {
                    Button(
                        onClick = { saveAssignment() },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showConfirmDialog = false }
                    ) {
                        Text("Cancel")
                    }
                },
                title = { Text("Save category") },
                text = {
                    Text("Assign \"${pendingTxn?.merchant ?: "Unknown"}\" to ${pendingBucket}?")
                }
            )
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                confirmButton = {
                    Button(
                        onClick = { createBucket() },
                        enabled = newBucketName.isNotBlank(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDialog = false }
                    ) {
                        Text("Cancel")
                    }
                },
                title = { Text("New bucket") },
                text = {
                    OutlinedTextField(
                        value = newBucketName,
                        onValueChange = { newBucketName = it },
                        singleLine = true,
                        label = { Text("Bucket name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            )
        }
    }
}

@Composable
private fun BucketCard(
    name: String,
    selected: Boolean,
    hovered: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val accentColor = bucketAccent(name)
    val containerColor = when {
        hovered -> MaterialTheme.colorScheme.primaryContainer
        selected -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val borderColor = when {
        hovered -> MaterialTheme.colorScheme.primary
        selected -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (hovered) 8.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                color = accentColor.copy(alpha = if (enabled) 0.18f else 0.12f),
                contentColor = accentColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = name.firstOrNull()?.uppercase() ?: "#",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (selected) "Current category" else "Category",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun EmptyBucketsState(
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 360.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "No buckets yet",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Create spending categories to keep transactions organized.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = onCreateClick,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Create bucket")
                }
            }
        }
    }
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
