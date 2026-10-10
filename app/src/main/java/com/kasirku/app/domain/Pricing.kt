package com.kasirku.app.domain

import com.kasirku.app.data.local.ProductEntity
import org.json.JSONArray
import java.text.NumberFormat
import java.util.Locale

data class PriceTier(val nama: String, val minQty: Int, val harga: Long)

private val rupiahFmt: NumberFormat =
    NumberFormat.getNumberInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }

/** 15_000 -> "Rp15.000" */
fun formatRupiah(n: Long): String = "Rp" + rupiahFmt.format(n)

fun parseTiers(json: String): List<PriceTier> {
    if (json.isBlank()) return emptyList()
    return try {
        val arr = JSONArray(json)
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            PriceTier(o.getString("nama"), o.getInt("minQty"), o.getLong("harga"))
        }.sortedByDescending { it.minQty }
    } catch (_: Exception) {
        emptyList()
    }
}

fun tiersToJson(tiers: List<PriceTier>): String {
    val arr = JSONArray()
    tiers.forEach {
        arr.put(org.json.JSONObject().apply {
            put("nama", it.nama); put("minQty", it.minQty); put("harga", it.harga)
        })
    }
    return arr.toString()
}

/**
 * Harga satuan untuk [qty] unit: tier dengan minQty terbesar yang terpenuhi menang,
 * selain itu harga ecer. Mengembalikan (hargaSatuan, namaTier).
 */
fun priceFor(p: ProductEntity, qty: Int): Pair<Long, String> {
    val tier = parseTiers(p.tiersJson).firstOrNull { qty >= it.minQty }
    return if (tier != null) tier.harga to tier.nama else p.sellPrice to "ecer"
}

/** Satu baris keranjang. */
data class CartLine(
    val product: ProductEntity,
    val qty: Int,
) {
    val unitPrice: Long get() = priceFor(product, qty).first
    val tierName: String get() = priceFor(product, qty).second
    val subtotal: Long get() = unitPrice * qty
    val profit: Long get() = (unitPrice - product.buyPrice) * qty
}
