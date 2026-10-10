package com.kasirku.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val address: String = "",
)

/** Catatan barang masuk dari supplier. itemsJson: [{"productId":1,"nama":"..","qty":10,"hargaBeli":2500}] */
@Entity(tableName = "stock_intakes")
data class StockIntakeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val supplierName: String = "",
    val itemsJson: String,
    val totalCost: Long = 0,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)
