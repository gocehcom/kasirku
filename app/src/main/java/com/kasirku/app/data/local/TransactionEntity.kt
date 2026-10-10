package com.kasirku.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Transaksi penjualan. itemsJson menyimpan SNAPSHOT item saat transaksi terjadi
 * (nama & harga disalin, bukan referensi) agar laporan historis tetap akurat
 * walau harga produk berubah kemudian.
 * Contoh itemsJson:
 * [{"productId":3,"nama":"Indomie Goreng","qty":5,"hargaSatuan":3500,
 *   "tier":"ecer","subtotal":17500,"laba":2500}]
 */
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String, // TRX-20261006-153045
    val cashierName: String = "Kasir",
    val itemsJson: String,
    val total: Long,
    val discount: Long = 0,
    val profit: Long = 0, // total laba kotor transaksi ini
    val paid: Long,
    val change: Long,
    val payMethod: String = "tunai", // tunai|qris|transfer|ewallet
    val createdAt: Long = System.currentTimeMillis(),
    val synced: Boolean = false, // sudah terkirim ke cloud?
)
