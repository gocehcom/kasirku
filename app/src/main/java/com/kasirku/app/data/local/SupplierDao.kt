package com.kasirku.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers ORDER BY name COLLATE NOCASE")
    fun all(): Flow<List<SupplierEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(s: SupplierEntity): Long

    @Query("DELETE FROM suppliers WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface StockIntakeDao {
    @Query("SELECT * FROM stock_intakes ORDER BY createdAt DESC LIMIT 100")
    fun recent(): Flow<List<StockIntakeEntity>>

    @Insert
    suspend fun insert(i: StockIntakeEntity): Long
}
