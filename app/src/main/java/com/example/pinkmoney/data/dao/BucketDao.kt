package com.example.pinkmoney.data.dao


import androidx.room.*
import com.example.pinkmoney.data.entity.BucketEntity
import com.example.pinkmoney.data.entity.MerchantBucketMap
import kotlinx.coroutines.flow.Flow

@Dao
interface BucketDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBucket(bucket: BucketEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBuckets(buckets: List<BucketEntity>)

    @Query("SELECT COUNT(*) FROM buckets")
    suspend fun bucketCount(): Int

    @Query("SELECT * FROM buckets ORDER BY name")
    fun getBuckets(): Flow<List<BucketEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMapping(map: MerchantBucketMap)

    @Query("""
        SELECT bucket
        FROM merchant_bucket_map
        WHERE merchant = :merchant
        LIMIT 1
    """)
    suspend fun getBucketForMerchant(
        merchant: String?
    ): String?

    @Query("""
        UPDATE transactions
        SET category = :category
        WHERE merchant = :merchant
    """)
    suspend fun updateCategoryForMerchant(
        merchant: String,
        category: String
    )
}
