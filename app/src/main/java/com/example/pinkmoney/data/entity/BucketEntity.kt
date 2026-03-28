package com.example.pinkmoney.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "buckets")
data class BucketEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String
)