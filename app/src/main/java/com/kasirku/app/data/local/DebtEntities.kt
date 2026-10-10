package com.kasirku.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
)

/** Bon/piutang pelanggan. sisa = total - terbayar (dihitung saat baca). */
@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerName: String,
    val transactionNumber: String = "",
    val total: Long,
    val paid: Long = 0,
    val dueDate: Long? = null,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
) {
    val remaining: Long get() = total - paid
    val isPaidOff: Boolean get() = remaining <= 0
}

@Entity(tableName = "debt_payments")
data class DebtPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val debtId: Long,
    val amount: Long,
    val cashierName: String = "Kasir",
    val createdAt: Long = System.currentTimeMillis(),
)
