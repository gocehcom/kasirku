package com.kasirku.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class MethodSum(val payMethod: String, val total: Long)

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY name COLLATE NOCASE")
    fun allActive(): Flow<List<ProductEntity>>

    @Query(
        "SELECT * FROM products WHERE isActive = 1 AND " +
            "(name LIKE '%' || :q || '%' OR barcode = :q) " +
            "ORDER BY name COLLATE NOCASE LIMIT 100"
    )
    fun search(q: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE barcode = :code AND isActive = 1 LIMIT 1")
    suspend fun findByBarcode(code: String): ProductEntity?

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE isActive = 1 AND stock <= minStock ORDER BY stock ASC")
    fun lowStock(): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(p: ProductEntity): Long

    @Update
    suspend fun update(p: ProductEntity)

    @Delete
    suspend fun delete(p: ProductEntity)
}

@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(t: TransactionEntity): Long

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    fun recent(limit: Int, offset: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE createdAt BETWEEN :start AND :end ORDER BY createdAt DESC")
    suspend fun between(start: Long, end: Long): List<TransactionEntity>

    @Query("SELECT COALESCE(SUM(total),0) FROM transactions WHERE createdAt BETWEEN :start AND :end")
    suspend fun sumTotal(start: Long, end: Long): Long

    @Query("SELECT COALESCE(SUM(profit),0) FROM transactions WHERE createdAt BETWEEN :start AND :end")
    suspend fun sumProfit(start: Long, end: Long): Long

    @Query("SELECT COUNT(*) FROM transactions WHERE createdAt BETWEEN :start AND :end")
    suspend fun countBetween(start: Long, end: Long): Int

    @Query(
        "SELECT payMethod, COALESCE(SUM(total),0) AS total FROM transactions " +
            "WHERE createdAt BETWEEN :start AND :end GROUP BY payMethod"
    )
    suspend fun sumByMethod(start: Long, end: Long): List<MethodSum>

    @Query("SELECT * FROM transactions WHERE synced = 0 ORDER BY createdAt ASC LIMIT 200")
    suspend fun unsynced(): List<TransactionEntity>

    @Query("UPDATE transactions SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)
}
