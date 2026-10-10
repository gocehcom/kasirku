package com.kasirku.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name COLLATE NOCASE")
    fun all(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE name LIKE '%' || :q || '%' ORDER BY name LIMIT 20")
    suspend fun search(q: String): List<CustomerEntity>

    @Insert
    suspend fun insert(c: CustomerEntity): Long
}

@Dao
interface DebtDao {
    // Diurutkan: yang belum lunas dulu, lalu terbaru
    @Query("SELECT * FROM debts ORDER BY (total - paid) <= 0, createdAt DESC")
    fun all(): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): DebtEntity?

    @Query("SELECT COALESCE(SUM(total - paid),0) FROM debts WHERE (total - paid) > 0")
    fun totalOutstanding(): Flow<Long>

    @Insert
    suspend fun insert(d: DebtEntity): Long

    @Update
    suspend fun update(d: DebtEntity)

    @Insert
    suspend fun insertPayment(p: DebtPaymentEntity): Long

    @Query("SELECT * FROM debt_payments WHERE debtId = :debtId ORDER BY createdAt DESC")
    suspend fun paymentsOf(debtId: Long): List<DebtPaymentEntity>
}
