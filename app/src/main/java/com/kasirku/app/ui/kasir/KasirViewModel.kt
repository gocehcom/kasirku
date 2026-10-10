package com.kasirku.app.ui.kasir

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasirku.app.data.CheckoutResult
import com.kasirku.app.data.PosRepository
import com.kasirku.app.data.SessionManager
import com.kasirku.app.data.local.ProductEntity
import com.kasirku.app.data.local.TransactionEntity
import com.kasirku.app.domain.CartLine
import com.kasirku.app.domain.buildReceipt
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class KasirViewModel(
    private val repo: PosRepository,
    private val session: SessionManager,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    @OptIn(ExperimentalCoroutinesApi::class)
    val products: StateFlow<List<ProductEntity>> =
        _query.flatMapLatest { repo.products(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _cart = MutableStateFlow<Map<Long, CartLine>>(emptyMap())
    val cart: StateFlow<Map<Long, CartLine>> = _cart

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _lastCheckout = MutableStateFlow<CheckoutResult?>(null)
    val lastCheckout: StateFlow<CheckoutResult?> = _lastCheckout

    /** Struk siap tampil/bagikan setelah checkout sukses. */
    data class ReceiptReady(val trx: TransactionEntity, val text: String)
    private val _lastReceipt = MutableStateFlow<ReceiptReady?>(null)
    val lastReceipt: StateFlow<ReceiptReady?> = _lastReceipt

    var showCheckout = MutableStateFlow(false)
        private set

    fun setQuery(q: String) { _query.value = q }

    fun addProduct(p: ProductEntity) {
        val cur = _cart.value[p.id]
        val qty = (cur?.qty ?: 0) + 1
        if (qty > p.stock) {
            _message.value = "Stok ${p.name} tidak cukup (sisa ${p.stock})"
            return
        }
        _cart.value = _cart.value + (p.id to CartLine(p, qty))
    }

    fun setQty(id: Long, qty: Int) {
        val line = _cart.value[id] ?: return
        if (qty <= 0) {
            _cart.value = _cart.value - id
            return
        }
        if (qty > line.product.stock) {
            _message.value = "Stok ${line.product.name} tidak cukup (sisa ${line.product.stock})"
            return
        }
        _cart.value = _cart.value + (id to line.copy(qty = qty))
    }

    fun clearCart() { _cart.value = emptyMap() }

    fun addByBarcode(code: String) {
        viewModelScope.launch {
            val p = repo.findByBarcode(code)
            if (p == null) _message.value = "Barcode tidak dikenal: $code"
            else addProduct(p)
        }
    }

    fun openCheckout() { showCheckout.value = _cart.value.isNotEmpty() }
    fun closeCheckout() { showCheckout.value = false }

    fun checkout(payMethod: String, paid: Long, discount: Long = 0, customerName: String = "") {
        viewModelScope.launch {
            try {
                val res = repo.checkout(
                    lines = _cart.value.values.toList(),
                    payMethod = payMethod,
                    paid = paid,
                    discount = discount,
                    cashierName = session.current()?.name ?: "Kasir",
                    customerName = customerName,
                )
                _cart.value = emptyMap()
                showCheckout.value = false
                _lastCheckout.value = res
                // Siapkan struk
                repo.transactionById(res.transactionId)?.let { trx ->
                    _lastReceipt.value = ReceiptReady(trx, buildReceipt("", trx))
                }
            } catch (e: Exception) {
                _message.value = e.message ?: "Gagal menyimpan transaksi"
            }
        }
    }

    fun consumeReceipt() { _lastReceipt.value = null }

    fun consumeMessage() { _message.value = null }
    fun consumeCheckout() { _lastCheckout.value = null }
}
