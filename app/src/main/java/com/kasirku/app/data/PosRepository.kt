package com.kasirku.app.data

import com.kasirku.app.data.local.ProductEntity
import com.kasirku.app.data.local.TransactionEntity
import com.kasirku.app.data.local.UserEntity
import com.kasirku.app.domain.CartLine
import kotlinx.coroutines.flow.Flow

data class CheckoutResult(
    val transactionId: Long,
    val number: String,
    val total: Long,
    val change: Long,
)

data class ReportSummary(
    val omzet: Long,
    val laba: Long,
    val jumlahTransaksi: Int,
    val perMetode: Map<String, Long>,
)

interface PosRepository {
    // Produk
    fun products(query: String): Flow<List<ProductEntity>>
    fun lowStock(): Flow<List<ProductEntity>>
    suspend fun findByBarcode(code: String): ProductEntity?
    suspend fun findProduct(id: Long): ProductEntity?
    suspend fun saveProduct(p: ProductEntity): Long
    suspend fun deleteProduct(p: ProductEntity)

    // Transaksi
    suspend fun transactionById(id: Long): TransactionEntity?
    fun recentTransactions(limit: Int, offset: Int): Flow<List<TransactionEntity>>
    suspend fun checkout(
        lines: List<CartLine>,
        payMethod: String,
        paid: Long,
        discount: Long = 0,
        cashierName: String = "Kasir",
        customerName: String = "", // wajib diisi bila payMethod = "piutang"
    ): CheckoutResult

    suspend fun report(start: Long, end: Long): ReportSummary
    suspend fun transactionsBetween(start: Long, end: Long): List<TransactionEntity>
    suspend fun unsynced(): List<TransactionEntity>
    suspend fun markSynced(ids: List<Long>)

    // Pengguna (multi-kasir + PIN)
    fun users(): Flow<List<UserEntity>>
    suspend fun userCount(): Int
    suspend fun findUser(id: Long): UserEntity?
    suspend fun saveUser(u: UserEntity): Long

    // Piutang / pelanggan
    fun debts(): Flow<List<DebtEntity>>
    fun totalOutstanding(): Flow<Long>
    suspend fun debtById(id: Long): DebtEntity?
    suspend fun debtPayments(debtId: Long): List<DebtPaymentEntity>
    suspend fun recordDebtPayment(debtId: Long, amount: Long, cashierName: String)
    fun customers(): Flow<List<CustomerEntity>>
    suspend fun saveCustomer(c: CustomerEntity): Long

    // Supplier & stok masuk
    fun suppliers(): Flow<List<SupplierEntity>>
    suspend fun saveSupplier(s: SupplierEntity): Long
    suspend fun deleteSupplier(id: Long)
    fun stockIntakes(): Flow<List<StockIntakeEntity>>
    /**
     * Catat barang masuk: stok bertambah, harga beli produk diupdate ke harga
     * beli terakhir. items: Triple(productId, qty, hargaBeli).
     */
    suspend fun recordIntake(
        supplierName: String,
        items: List<Triple<Long, Int, Long>>,
        note: String = "",
    )
}
