package com.example.pinkmoney.ui.main

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.pinkmoney.data.db.PinkMoneyDatabase
import com.example.pinkmoney.data.entity.MerchantBucketMap
import com.example.pinkmoney.data.entity.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sqrt

@Composable
fun BucketsScreen(
    incomingTransaction: TransactionEntity?,
    onDone: () -> Unit // 🔥 IMPORTANT
) {

    val context = LocalContext.current

    val buckets = listOf(
        "Food", "Groceries", "Shopping", "Bills",
        "Transport", "Electronics", "Entertainment", "Transfers"
    )

    var cardPosition by remember { mutableStateOf(Offset(200f, 1400f)) }
    val bucketPositions = remember { mutableStateMapOf<String, Offset>() }
    var hoveredBucket by remember { mutableStateOf<String?>(null) }

    val selectedBucket = incomingTransaction?.category

    var showConfirmDialog by remember { mutableStateOf(false) }
    var pendingBucket by remember { mutableStateOf<String?>(null) }
    var pendingTxn by remember { mutableStateOf<TransactionEntity?>(null) }

    var showDialog by remember { mutableStateOf(false) }
    var newBucketName by remember { mutableStateOf("") }

    // reset position when new txn comes
    LaunchedEffect(incomingTransaction) {
        cardPosition = Offset(200f, 1400f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {

        // 🪣 BUCKET GRID
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(buckets) { bucket ->

                val isSelected = bucket == selectedBucket
                val isHovered = bucket == hoveredBucket

                Card(
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxWidth()
                        .height(120.dp)
                        .onGloballyPositioned { coords ->
                            bucketPositions[bucket] =
                                coords.localToRoot(Offset.Zero)
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            isHovered -> Color(0xFFBB86FC)
                            isSelected -> Color(0xFF03DAC5)
                            else -> Color(0xFF1E1F25)
                        }
                    )
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = bucket,
                            color = when {
                                isHovered -> Color.Black
                                isSelected -> Color.Black
                                else -> Color.White
                            }
                        )
                    }
                }
            }
        }

        // 🔥 DRAG CARD (simple + reliable)
        if (incomingTransaction != null) {

            Card(
                modifier = Modifier
                    .zIndex(1f)
                    .offset {
                        IntOffset(
                            cardPosition.x.toInt(),
                            cardPosition.y.toInt()
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(

                            onDrag = { change, dragAmount ->
                                change.consume()
                                cardPosition += dragAmount

                                var minDistance = Float.MAX_VALUE
                                var closest: String? = null

                                bucketPositions.forEach { (bucket, pos) ->
                                    val dx = pos.x - cardPosition.x
                                    val dy = pos.y - cardPosition.y
                                    val dist = sqrt(dx * dx + dy * dy)

                                    if (dist < minDistance) {
                                        minDistance = dist
                                        closest = bucket
                                    }
                                }

                                hoveredBucket = closest
                            },

                            onDragEnd = {
                                val bucket = hoveredBucket
                                val txn = incomingTransaction

                                if (bucket != null && txn != null) {
                                    pendingBucket = bucket
                                    pendingTxn = txn
                                    showConfirmDialog = true
                                }

                                hoveredBucket = null
                            }
                        )
                    }
                    .padding(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFBB86FC)
                )
            ) {

                Column(modifier = Modifier.padding(16.dp)) {

                    Text(
                        text = incomingTransaction.merchant ?: "Unknown",
                        color = Color.Black
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "₹${incomingTransaction.amount}",
                        color = Color.Black
                    )
                }
            }
        }

        // ✅ CONFIRMATION
        if (showConfirmDialog && pendingTxn != null && pendingBucket != null) {

            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                confirmButton = {
                    Button(onClick = {

                        val merchant = pendingTxn!!.merchant ?: "Unknown"

                        CoroutineScope(Dispatchers.IO).launch {
                            val db = PinkMoneyDatabase.getInstance(context)

                            db.bucketDao().saveMapping(
                                MerchantBucketMap(
                                    merchant = merchant,
                                    bucket = pendingBucket!!
                                )
                            )

                            db.transactionDao().updateCategoryForMerchant(
                                merchant = merchant,
                                category = pendingBucket!!
                            )
                        }

                        Log.d("BUCKET_SAVE", "$merchant → $pendingBucket")

                        showConfirmDialog = false

                        // 🔥 THIS IS THE MAGIC FIX
                        onDone()

                    }) {
                        Text("Yes")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showConfirmDialog = false
                    }) {
                        Text("No")
                    }
                },
                title = { Text("Confirm") },
                text = {
                    Text("Add \"${pendingTxn!!.merchant}\" to ${pendingBucket}?")
                }
            )
        }

        // ➕ CREATE BUCKET
        FloatingActionButton(
            onClick = { showDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Text("+")
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                confirmButton = {
                    Button(onClick = {
                        if (newBucketName.isNotBlank()) {
                            Log.d("BUCKET_CREATE", newBucketName)
                            newBucketName = ""
                            showDialog = false
                        }
                    }) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDialog = false
                    }) {
                        Text("Cancel")
                    }
                },
                title = { Text("New Bucket") },
                text = {
                    OutlinedTextField(
                        value = newBucketName,
                        onValueChange = { newBucketName = it },
                        placeholder = { Text("Bucket name") }
                    )
                }
            )
        }
    }
}