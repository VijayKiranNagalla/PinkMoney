package com.example.pinkmoney.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "merchant_bucket_map")
data class MerchantBucketMap(
    @PrimaryKey
    val merchant: String,
    val bucket: String
)