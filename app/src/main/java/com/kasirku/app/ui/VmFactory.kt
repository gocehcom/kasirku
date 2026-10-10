package com.kasirku.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kasirku.app.data.PosRepository
import com.kasirku.app.data.SessionManager
import com.kasirku.app.ui.kasir.KasirViewModel
import com.kasirku.app.ui.laporan.LaporanViewModel
import com.kasirku.app.ui.login.LoginViewModel
import com.kasirku.app.ui.piutang.PiutangViewModel
import com.kasirku.app.ui.supplier.SupplierViewModel
import com.kasirku.app.ui.produk.EditProdukViewModel
import com.kasirku.app.ui.produk.ProdukViewModel

/** Factory sederhana — tanpa DI framework untuk MVP. */
class VmFactory(
    private val repo: PosRepository,
    private val session: SessionManager,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(KasirViewModel::class.java) ->
            KasirViewModel(repo, session) as T
        modelClass.isAssignableFrom(ProdukViewModel::class.java) ->
            ProdukViewModel(repo) as T
        modelClass.isAssignableFrom(EditProdukViewModel::class.java) ->
            EditProdukViewModel(repo) as T
        modelClass.isAssignableFrom(LaporanViewModel::class.java) ->
            LaporanViewModel(repo) as T
        modelClass.isAssignableFrom(LoginViewModel::class.java) ->
            LoginViewModel(repo, session) as T
        modelClass.isAssignableFrom(PiutangViewModel::class.java) ->
            PiutangViewModel(repo, session) as T
        modelClass.isAssignableFrom(SupplierViewModel::class.java) ->
            SupplierViewModel(repo) as T
        else -> throw IllegalArgumentException("Unknown VM: $modelClass")
    }
}
