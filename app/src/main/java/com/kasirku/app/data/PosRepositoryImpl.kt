package com.kasirku.app.data

import androidx.room.withTransaction
import com.kasirku.app.data.local.AppDatabase
import com.kasirku.app.data.local.ProductEntity
import com.kasirku.app.data.local.TransactionEntity
import com.kasirku.app.domain.CartLine
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PosRepositoryImpl(private val db: AppDatabase) : PosRepository {

    private val products = db.productDao()
    private val trx = db.transactionDao()

    override fun products(query: String): Flow<List<ProductEntity>> =
        if (query.isBlank()) products.allActive() else products.search(query.trim())

    override fun lowStock(): Flow<List<ProductEntity>> = products.lowStock()

    override suspend fun findByBarcode(code: String): ProductEntity? =
        products.findByBarcode(code)

    override suspend fun findProduct(id: Long): ProductEntity? =
        products.findById(id)

    override suspend fun saveProduct(p: ProductEntity): Long = products.upsert(p)

    override suspend fun deleteProduct(p: ProductEntity) {
        // Soft delete agar riwayat transaksi tetap valid
        products.update(p.copy(isActive = false))
    }

    override suspend fun transactionById(id: Long): TransactionEntity? = trx.findById(id)

    override fun recentTransactions(limit: Int, offset: Int): Flow<List<TransactionEntity>> =
        trx.recent(limit, offset)

    override suspend fun checkout(
        lines: List<CartLine>,
        payMethod: String,
        paid: Long,
        discount: Long,
        cashierName: String,
        customerName: String,
    ): CheckoutResult {
        require(lines.isNotEmpty()) { "Keranjang kosong" }
        val isDebt = payMethod == "piutang"
        if (isDebt) require(customerName.isNotBlank()) { "Nama pelanggan wajib diisi untuk bon" }
        val total = lines.sumOf { it.subtotal } - discount
        require(total >= 0) { "Total tidak valid" }
        if (payMethod == "tunai") require(paid >= total) { "Uang kurang" }

        val items = JSONArray()
        lines.forEach { l ->
            items.put(JSONObject().apply {
                put("productId", l.product.id)
                put("nama", l.product.name)
                put("qty", l.qty)
                put("hargaSatuan", l.unitPrice)
                put("tier", l.tierName)
                put("subtotal", l.subtotal)
                put("laba", l.profit)
            })
        }
        val profit = lines.sumOf { it.profit }
        val number = "TRX-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val change = if (payMethod == "tunai") paid - total else 0L

        var trxId = -1L
        db.withTransaction {
            // Kurangi stok dulu — gagal bila stok tidak cukup
            lines.forEach { l ->
                val cur = products.findById(l.product.id)
                    ?: throw IllegalStateException("Produk hilang: ${l.product.name}")
                if (cur.stock < l.qty) throw IllegalStateException("Stok kurang: ${cur.name}")
                products.update(cur.copy(stock = cur.stock - l.qty))
            }
            trxId = trx.insert(
                TransactionEntity(
                    number = number,
                    cashierName = cashierName,
                    itemsJson = items.toString(),
                    total = total,
                    discount = discount,
                    profit = profit,
                    paid = if (payMethod == "tunai") paid else total,
                    change = change,
                    payMethod = payMethod,
                )
            )
            if (isDebt) {
                db.debtDao().insert(
                    DebtEntity(
                        customerName = customerName.trim(),
                        transactionNumber = number,
                        total = total,
                        paid = 0,
                    )
                )
            }
        }
        return CheckoutResult(trxId, number, total, change)
    }

    override suspend fun report(start: Long, end: Long): ReportSummary = ReportSummary(
        omzet = trx.sumTotal(start, end),
        laba = trx.sumProfit(start, end),
        jumlahTransaksi = trx.countBetween(start, end),
        perMetode = trx.sumByMethod(start, end).associate { it.payMethod to it.total },
    )

    override suspend fun transactionsBetween(start: Long, end: Long): List<TransactionEntity> =
        trx.between(start, end)

    override suspend fun unsynced(): List<TransactionEntity> = trx.unsynced()

    override suspend fun markSynced(ids: List<Long>) = trx.markSynced(ids)

    override fun users(): Flow<List<com.kasirku.app.data.local.UserEntity>> =
        db.userDao().allActive()

    override suspend fun userCount(): Int = db.userDao().count()

    override suspend fun findUser(id: Long) = db.userDao().findById(id)

    override suspend fun saveUser(u: com.kasirku.app.data.local.UserEntity): Long =
        db.userDao().upsert(u)

    // ---- Piutang ----
    override fun debts(): Flow<List<DebtEntity>> = db.debtDao().all()

    override fun totalOutstanding(): Flow<Long> = db.debtDao().totalOutstanding()

    override suspend fun debtById(id: Long): DebtEntity? = db.debtDao().findById(id)

    override suspend fun debtPayments(debtId: Long): List<DebtPaymentEntity> =
        db.debtDao().paymentsOf(debtId)

    override suspend fun recordDebtPayment(debtId: Long, amount: Long, cashierName: String) {
        val dao = db.debtDao()
        val debt = dao.findById(debtId) ?: return
        val pay = amount.coerceAtMost(debt.remaining).coerceAtLeast(0)
        if (pay <= 0) return
        db.withTransaction {
            dao.insertPayment(DebtPaymentEntity(debtId = debtId, amount = pay, cashierName = cashierName))
            dao.update(debt.copy(paid = debt.paid + pay))
        }
    }

    override fun customers(): Flow<List<CustomerEntity>> = db.customerDao().all()

    override suspend fun saveCustomer(c: CustomerEntity): Long = db.customerDao().insert(c)

    // ---- Supplier & stok masuk ----
    override fun suppliers(): Flow<List<SupplierEntity>> = db.supplierDao().all()

    override suspend fun saveSupplier(s: SupplierEntity): Long = db.supplierDao().upsert(s)

    override suspend fun deleteSupplier(id: Long) = db.supplierDao().deleteById(id)

    override fun stockIntakes(): Flow<List<StockIntakeEntity>> = db.stockIntakeDao().recent()

    override suspend fun recordIntake(
        supplierName: String,
        items: List<Triple<Long, Int, Long>>,
        note: String,
    ) {
        require(items.isNotEmpty()) { "Belum ada barang" }
        val arr = JSONArray()
        var totalCost = 0L
        db.withTransaction {
            items.forEach { (productId, qty, cost) ->
                require(qty > 0) { "Qty harus > 0" }
                val p = prod.findById(productId) ?: return@forEach
                prod.update(p.copy(stock = p.stock + qty, buyPrice = cost))
                arr.put(JSONObject().apply {
                    put("productId", productId)
                    put("nama", p.name)
                    put("qty", qty)
                    put("hargaBeli", cost)
                })
                totalCost += qty * cost
            }
            db.stockIntakeDao().insert(
                StockIntakeEntity(
                    supplierName = supplierName,
                    itemsJson = arr.toString(),
                    totalCost = totalCost,
                    note = note,
                )
            )
        }
    }
}
