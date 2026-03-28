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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.pinkmoney.data.entity.TransactionEntity
import kotlin.math.sqrt

@Composable
fun BucketsScreen(incomingTransaction: TransactionEntity?) {

    val buckets = listOf(
        "Food", "Groceries", "Shopping", "Bills",
        "Transport", "Electronics", "Entertainment", "Transfers"
    )

    // ✅ START VISIBLE (fix your main bug)
    var cardPosition by remember {
        mutableStateOf(Offset(300f, 1400f))
    }

    val bucketPositions = remember { mutableStateMapOf<String, Offset>() }
    var hoveredBucket by remember { mutableStateOf<String?>(null) }

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
                        containerColor =
                            if (bucket == hoveredBucket)
                                Color(0xFFBB86FC)
                            else
                                Color(0xFF1E1F25)
                    )
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = bucket,
                            color = if (bucket == hoveredBucket)
                                Color.Black else Color.White
                        )
                    }
                }
            }
        }

        // 🔥 DRAG CARD
        if (incomingTransaction != null) {

            Card(
                modifier = Modifier
                    .zIndex(1f) // ✅ ensures it's touchable
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

                                // 🔥 detect closest bucket
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
                                if (hoveredBucket != null) {
                                    Log.d(
                                        "BUCKET_DROP",
                                        "Dropped on $hoveredBucket"
                                    )
                                }

                                hoveredBucket = null

                                // ✅ reset to bottom
                                cardPosition = Offset(300f, 1400f)
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
    }
}