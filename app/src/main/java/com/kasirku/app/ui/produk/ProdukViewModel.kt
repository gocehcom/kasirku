package com.kasirku.app.ui.produk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasirku.app.data.PosRepository
import com.kasirku.app.data.local.ProductEntity
import com.kasirku.app.domain.PriceTier
import com.kasirku.app.domain.parseTiers
import com.kasirku.app.domain.tiersToJson
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProdukViewModel(private val repo: PosRepository) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    @OptIn(ExperimentalCoroutinesApi::class)
    val products: StateFlow<List<ProductEntity>> =
        _query.flatMapLatest { repo.products(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val lowStock = repo.lowStock()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(q: String) { _query.value = q }

    fun delete(p: ProductEntity) {
        viewModelScope.launch { repo.deleteProduct(p) }
    }
}

class EditProdukViewModel(private val repo: PosRepository) : ViewModel() {
    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved

    fun load(id: Long, onLoaded: (ProductEntity) -> Unit) {
        viewModelScope.launch {
            repo.findProduct(id)?.let(onLoaded)
        }
    }

    fun save(
        id: Long?,
        barcode: String,
        nama: String,
        kategori: String,
        satuan: String,
        hargaBeli: Long,
        hargaEcer: Long,
        tiers: List<PriceTier>,
        stok: Int,
        minStok: Int,
    ) {
        viewModelScope.launch {
            val existing = id?.let { repo.findProduct(it) }
            repo.saveProduct(
                (existing ?: ProductEntity(name = nama)).copy(
                    barcode = barcode.trim(),
                    name = nama.trim(),
                    category = kategori.trim(),
                    unit = satuan.trim().ifBlank { "pcs" },
                    buyPrice = hargaBeli,
                    sellPrice = hargaEcer,
                    tiersJson = tiersToJson(tiers),
                    stock = stok,
                    minStock = minStok,
                    isActive = true,
                )
            )
            _saved.value = true
        }
    }

    fun resetSaved() { _saved.value = false }
}
