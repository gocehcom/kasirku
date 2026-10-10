package com.kasirku.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Produk. Harga dalam Rupiah (Long, tanpa desimal).
 * tiersJson: daftar tier harga grosir, contoh:
 * [{"nama":"Grosir 1","minQty":12,"harga":9000},{"nama":"Grosir 2","minQty":50,"harga":8500}]
 */
@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val barcode: String = "",
    val name: String,
    val category: String = "",
    val unit: String = "pcs",
    val buyPrice: Long = 0,
    val sellPrice: Long = 0, // harga ecer
    val tiersJson: String = "[]",
    val stock: Int = 0,
    val minStock: Int = 0,
    val isActive: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis(),
)
