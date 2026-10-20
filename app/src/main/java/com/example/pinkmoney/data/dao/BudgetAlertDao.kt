package com.example.pinkmoney.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.pinkmoney.data.entity.BudgetAlertEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetAlertDao {

    @Insert
    suspend fun insertAlert(alert: BudgetAlertEntity): Long

    @Query("SELECT * FROM budget_alerts ORDER BY timestamp DESC")
    fun getAllAlerts(): Flow<List<BudgetAlertEntity>>

    @Query("SELECT COUNT(*) FROM budget_alerts WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>

    @Query("UPDATE budget_alerts SET isRead = 1 WHERE isRead = 0")
    suspend fun markAllAsRead()

    @Query("DELETE FROM budget_alerts")
    suspend fun clearAll()
}
