package com.kasirku.app.domain

import com.kasirku.app.data.local.TransactionEntity
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ReceiptItem(val name: String, val qty: Int, val unitPrice: Long)

fun parseReceiptItems(itemsJson: String): List<ReceiptItem> {
    val out = mutableListOf<ReceiptItem>()
    runCatching {
        val arr = JSONArray(itemsJson)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out += ReceiptItem(
                name = o.optString("nama", "-"),
                qty = o.optInt("qty", 1),
                unitPrice = o.optLong("hargaSatuan", 0),
            )
        }
    }
    return out
}

/** Struk teks monospace 32 kolom — cocok untuk printer thermal & WhatsApp. */
fun buildReceipt(storeName: String, trx: TransactionEntity): String {
    val w = 32
    fun line() = "-".repeat(w)
    fun center(s: String) = s.take(w).padStart((w + s.take(w).length) / 2).padEnd(w)
    fun pair(left: String, right: String): String {
        val l = left.take(w - right.length - 1)
        return (l + " ".repeat(w - l.length - right.length) + right).take(w)
    }
    val date = SimpleDateFormat("d MMM yyyy HH:mm", Locale("in", "ID"))
        .format(Date(trx.createdAt))
    val sb = StringBuilder()
    sb.appendLine(center(storeName.ifBlank { "TOKO KAMI" }))
    sb.appendLine(center("Terima kasih sudah berbelanja"))
    sb.appendLine(line())
    sb.appendLine(pair("No: ${trx.number.take(18)}", ""))
    sb.appendLine(pair(date, trx.cashierName.take(12)))
    sb.appendLine(line())
    parseReceiptItems(trx.itemsJson).forEach { it ->
        sb.appendLine(it.name.take(w))
        sb.appendLine(pair("${it.qty} x ${formatRupiah(it.unitPrice)}",
            formatRupiah(it.qty * it.unitPrice)))
    }
    sb.appendLine(line())
    if (trx.discount > 0) sb.appendLine(pair("Diskon", "-" + formatRupiah(trx.discount)))
    sb.appendLine(pair("TOTAL", formatRupiah(trx.total)))
    val methodLabel = when (trx.payMethod) {
        "tunai" -> "Tunai"
        "qris" -> "QRIS"
        "transfer" -> "Transfer"
        "ewallet" -> "E-Wallet"
        "piutang" -> "Bon/Piutang"
        else -> trx.payMethod
    }
    sb.appendLine(pair("Bayar ($methodLabel)", formatRupiah(trx.paid)))
    if (trx.payMethod == "tunai") sb.appendLine(pair("Kembali", formatRupiah(trx.change)))
    sb.appendLine(line())
    sb.appendLine(center("Barang yang dibeli tidak dapat"))
    sb.appendLine(center("ditukar/dikembalikan"))
    return sb.toString()
}
