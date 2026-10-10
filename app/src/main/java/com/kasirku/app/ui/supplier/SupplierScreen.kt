package com.kasirku.app.ui.supplier

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasirku.app.data.PosRepository
import com.kasirku.app.data.local.ProductEntity
import com.kasirku.app.data.local.SupplierEntity
import com.kasirku.app.domain.formatRupiah
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SupplierViewModel(private val repo: PosRepository) : ViewModel() {
    val suppliers = repo.suppliers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val products = repo.products()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveSupplier(name: String, phone: String, address: String, onDone: () -> Unit) {
        viewModelScope.launch {
            repo.saveSupplier(SupplierEntity(name = name.trim(), phone = phone.trim(), address = address.trim()))
            onDone()
        }
    }

    fun deleteSupplier(id: Long) {
        viewModelScope.launch { repo.deleteSupplier(id) }
    }

    fun recordIntake(supplier: String, items: List<Triple<Long, Int, Long>>, note: String, onDone: () -> Unit) {
        viewModelScope.launch {
            repo.recordIntake(supplier.trim(), items, note.trim())
            onDone()
        }
    }
}

// ---------- Daftar supplier ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierScreen(vm: SupplierViewModel, onBack: () -> Unit, onIntake: () -> Unit) {
    val suppliers by vm.suppliers.collectAsState()
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Supplier") }, navigationIcon = {
            FilledTonalButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp)) { Text("‹") }
        }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Tambah supplier")
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Button(
                onClick = onIntake,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) { Text("Catat Barang Masuk") }
            if (suppliers.isEmpty()) {
                Text("Belum ada supplier. Tambah dulu pakai tombol +.",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn {
                    items(suppliers, key = { it.id }) { s ->
                        ListItem(
                            headlineContent = { Text(s.name, fontWeight = FontWeight.Medium) },
                            supportingContent = {
                                Text(listOf(s.phone, s.address).filter { it.isNotBlank() }
                                    .joinToString(" • "))
                            },
                            trailingContent = {
                                IconButton(onClick = { vm.deleteSupplier(s.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Hapus",
                                        tint = MaterialTheme.colorScheme.error)
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    if (showAdd) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Supplier Baru") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Nama supplier*") },
                        singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(phone, { phone = it.filter { c -> c.isDigit() } },
                        label = { Text("No. HP/WA") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(address, { address = it }, label = { Text("Alamat") },
                        modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = { vm.saveSupplier(name, phone, address) { showAdd = false } },
                    enabled = name.isNotBlank()) { Text("Simpan") }
            },
            dismissButton = { FilledTonalButton(onClick = { showAdd = false }) { Text("Batal") } },
        )
    }
}

// ---------- Stok masuk ----------

data class IntakeItem(val product: ProductEntity, val qty: Int, val cost: Long)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntakeScreen(vm: SupplierViewModel, onBack: () -> Unit, onDone: () -> Unit) {
    val suppliers by vm.suppliers.collectAsState()
    val products by vm.products.collectAsState()
    var supplier by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var items by remember { mutableStateOf(listOf<IntakeItem>()) }
    var showAddItem by remember { mutableStateOf(false) }
    val totalCost = items.sumOf { it.qty * it.cost }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Barang Masuk") }, navigationIcon = {
            FilledTonalButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp)) { Text("‹") }
        }) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(supplier, { supplier = it }, label = { Text("Nama supplier") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            if (suppliers.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    suppliers.take(3).forEach { s ->
                        FilledTonalButton(onClick = { supplier = s.name }) { Text(s.name) }
                    }
                }
            }
            Card(colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Barang (${items.size})", fontWeight = FontWeight.Medium)
                        FilledTonalButton(onClick = { showAddItem = true }) { Text("+ Tambah") }
                    }
                    items.forEach { it ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(it.product.name, fontWeight = FontWeight.Medium)
                                Text("${it.qty} × ${formatRupiah(it.cost)}",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                            Text(formatRupiah(it.qty * it.cost))
                        }
                    }
                    if (items.isEmpty()) Text("Belum ada barang.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            OutlinedTextField(note, { note = it }, label = { Text("Catatan (opsional)") },
                modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.weight(1f))
            Text("Total modal: ${formatRupiah(totalCost)}",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Button(
                onClick = {
                    vm.recordIntake(supplier, items.map {
                        Triple(it.product.id, it.qty, it.cost)
                    }, note) { onDone() }
                },
                enabled = items.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Simpan — Stok Bertambah") }
        }
    }

    if (showAddItem) {
        var query by remember { mutableStateOf("") }
        var selected by remember { mutableStateOf<ProductEntity?>(null) }
        var qtyStr by remember { mutableStateOf("") }
        var costStr by remember { mutableStateOf("") }
        val filtered = remember(query, products) {
            if (query.isBlank()) products.take(20)
            else products.filter {
                it.name.contains(query, ignoreCase = true) ||
                    (it.barcode?.contains(query) == true)
            }.take(20)
        }
        AlertDialog(
            onDismissRequest = { showAddItem = false },
            title = { Text("Tambah Barang") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (selected == null) {
                        OutlinedTextField(query, { query = it }, label = { Text("Cari produk") },
                            singleLine = true, modifier = Modifier.fillMaxWidth())
                        LazyColumn(modifier = Modifier.height(180.dp)) {
                            items(filtered, key = { it.id }) { p ->
                                Text(p.name, modifier = Modifier.fillMaxWidth()
                                    .clickable { selected = p; costStr = p.buyPrice.toString() }
                                    .padding(vertical = 8.dp))
                            }
                        }
                    } else {
                        Text(selected!!.name, fontWeight = FontWeight.Bold)
                        OutlinedTextField(qtyStr, { qtyStr = it.filter { c -> c.isDigit() } },
                            label = { Text("Jumlah") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(costStr, { costStr = it.filter { c -> c.isDigit() } },
                            label = { Text("Harga beli /pcs") }, prefix = { Text("Rp") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {
                if (selected == null) {
                    FilledTonalButton(onClick = { showAddItem = false }) { Text("Tutup") }
                } else {
                    val qty = qtyStr.toIntOrNull() ?: 0
                    val cost = costStr.toLongOrNull() ?: 0
                    Button(
                        onClick = {
                            items = items + IntakeItem(selected!!, qty, cost)
                            showAddItem = false
                        },
                        enabled = qty > 0 && cost > 0,
                    ) { Text("Tambah") }
                }
            },
        )
    }
}
