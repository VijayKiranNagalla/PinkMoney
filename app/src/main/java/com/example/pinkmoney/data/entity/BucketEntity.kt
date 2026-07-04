package com.example.pinkmoney.data.entity
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "buckets",
    indices = [Index(value = ["name"], unique = true)]
)
data class BucketEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String
)