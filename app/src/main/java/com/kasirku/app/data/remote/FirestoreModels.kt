package com.kasirku.app.data.remote

import com.kasirku.app.data.local.ProductEntity
import com.kasirku.app.data.local.TransactionEntity
import org.json.JSONArray

/**
 * Pemetaan model lokal -> dokumen Firestore, mengikuti skema:
 *   stores/{storeId}/produk/{id}, stores/{storeId}/transaksi/{id}, dst.
 *
 * Sengaja TIDAK mengimpor API Firebase di sini agar proyek tetap bisa
 * dikompilasi sebelum google-services.json ditambahkan. toMap() menghasilkan
 * Map<String, Any> yang siap dilempar ke Firestore set().
 */
data class FirestoreProduct(
    val barcode: String,
    val nama: String,
    val kategori: String,
    val satuan: String,
    val hargaBeli: Long,
    val hargaTiers: List<Map<String, Any>>,
    val stok: Int,
    val stokMinimum: Int,
    val aktif: Boolean,
    val diperbaruiPada: Long,
) {
    fun toMap(): Map<String, Any> = mapOf(
        "barcode" to barcode,
        "nama" to nama,
        "kategori" to kategori,
        "satuan" to satuan,
        "hargaBeli" to hargaBeli,
        "hargaTiers" to hargaTiers,
        "stok" to stok,
        "stokMinimum" to stokMinimum,
        "aktif" to aktif,
        "diperbaruiPada" to diperbaruiPada,
    )

    companion object {
        fun fromLocal(p: ProductEntity, tiers: List<Map<String, Any>>): FirestoreProduct =
            FirestoreProduct(
                barcode = p.barcode, nama = p.name, kategori = p.category,
                satuan = p.unit, hargaBeli = p.buyPrice, hargaTiers = tiers,
                stok = p.stock, stokMinimum = p.minStock, aktif = p.isActive,
                diperbaruiPada = p.updatedAt,
            )
    }
}

data class FirestoreTransaction(
    val nomor: String,
    val kasirNama: String,
    val items: List<Map<String, Any>>,
    val total: Long,
    val diskon: Long,
    val laba: Long,
    val bayar: Long,
    val kembalian: Long,
    val metodeBayar: String,
    val dibuatPada: Long,
) {
    fun toMap(): Map<String, Any> = mapOf(
        "nomor" to nomor,
        "kasirNama" to kasirNama,
        "items" to items,
        "total" to total,
        "diskon" to diskon,
        "laba" to laba,
        "bayar" to bayar,
        "kembalian" to kembalian,
        "metodeBayar" to metodeBayar,
        "dibuatPada" to dibuatPada,
    )

    companion object {
        fun fromLocal(t: TransactionEntity): FirestoreTransaction {
            val items = mutableListOf<Map<String, Any>>()
            val arr = JSONArray(t.itemsJson)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                items += mapOf(
                    "productId" to o.getLong("productId"),
                    "nama" to o.getString("nama"),
                    "qty" to o.getInt("qty"),
                    "hargaSatuan" to o.getLong("hargaSatuan"),
                    "tier" to o.getString("tier"),
                    "subtotal" to o.getLong("subtotal"),
                    "laba" to o.getLong("laba"),
                )
            }
            return FirestoreTransaction(
                nomor = t.number, kasirNama = t.cashierName, items = items,
                total = t.total, diskon = t.discount, laba = t.profit,
                bayar = t.paid, kembalian = t.change,
                metodeBayar = t.payMethod, dibuatPada = t.createdAt,
            )
        }
    }
}
