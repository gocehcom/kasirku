package com.kasirku.app.ui.produk

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kasirku.app.data.local.ProductEntity
import com.kasirku.app.domain.formatRupiah
import com.kasirku.app.domain.parseTiers

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProdukScreen(
    vm: ProdukViewModel,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onSupplier: () -> Unit,
    onIntake: () -> Unit,
) {
    val products by vm.products.collectAsState()
    val lowStock by vm.lowStock.collectAsState()
    val query by vm.query.collectAsState()
    var confirmDelete by remember { mutableStateOf<ProductEntity?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Produk", style = MaterialTheme.typography.titleLarge) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = "Tambah produk")
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (lowStock.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${lowStock.size} produk stok menipis: " +
                                lowStock.take(3).joinToString(", ") { it.name } +
                                if (lowStock.size > 3) "…" else "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }
            OutlinedTextField(
                value = query,
                onValueChange = vm::setQuery,
                placeholder = { Text("Cari produk…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(onClick = onSupplier, modifier = Modifier.weight(1f)) {
                    Text("Supplier")
                }
                FilledTonalButton(onClick = onIntake, modifier = Modifier.weight(1f)) {
                    Text("Barang Masuk")
                }
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(products, key = { it.id }) { p ->
                    ListItem(
                        headlineContent = { Text(p.name, fontWeight = FontWeight.Medium) },
                        supportingContent = {
                            val tiers = parseTiers(p.tiersJson)
                            Text(
                                "${formatRupiah(p.sellPrice)} • Stok ${p.stock} ${p.unit}" +
                                    (if (p.barcode.isNotBlank()) "\n${p.barcode}" else "") +
                                    (if (tiers.isNotEmpty()) "\n" + tiers.joinToString(" • ") {
                                        "${it.nama} ≥${it.minQty}: ${formatRupiah(it.harga)}"
                                    } else ""),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { onEdit(p.id) }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Ubah")
                                }
                                IconButton(onClick = { confirmDelete = p }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Hapus",
                                        tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        },
                        tonalElevation = if (p.stock <= p.minStock) 2.dp else 0.dp,
                    )
                }
            }
        }
    }

    confirmDelete?.let { p ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Hapus produk?") },
            text = { Text("${p.name} akan dinonaktifkan (riwayat transaksi tetap aman).") },
            confirmButton = {
                TextButton(onClick = { vm.delete(p); confirmDelete = null }) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Batal") } },
        )
    }
}
