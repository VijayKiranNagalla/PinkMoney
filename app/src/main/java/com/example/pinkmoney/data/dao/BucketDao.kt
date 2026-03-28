package com.example.pinkmoney.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pinkmoney.data.entity.BucketEntity
import com.example.pinkmoney.data.entity.MerchantBucketMap

@Dao
interface BucketDao {

    @Insert
    suspend fun insertBucket(bucket: BucketEntity)

    @Query("SELECT * FROM buckets")
    suspend fun getBuckets(): List<BucketEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMapping(map: MerchantBucketMap)

    @Query("SELECT bucket FROM merchant_bucket_map WHERE merchant = :merchant LIMIT 1")
    suspend fun getBucketForMerchant(merchant: String?): String?
}