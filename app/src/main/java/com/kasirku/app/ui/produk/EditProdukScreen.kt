package com.kasirku.app.ui.produk

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kasirku.app.domain.PriceTier
import com.kasirku.app.domain.parseTiers

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProdukScreen(
    vm: EditProdukViewModel,
    productId: Long?,
    scannedBarcode: String?,
    onBarcodeConsumed: () -> Unit,
    onScan: () -> Unit,
    onDone: () -> Unit,
) {
    var barcode by remember { mutableStateOf("") }
    var nama by remember { mutableStateOf("") }
    var kategori by remember { mutableStateOf("") }
    var satuan by remember { mutableStateOf("pcs") }
    var hargaBeli by remember { mutableStateOf("") }
    var hargaEcer by remember { mutableStateOf("") }
    var stok by remember { mutableStateOf("") }
    var minStok by remember { mutableStateOf("5") }
    val tiers = remember { mutableStateListOf<PriceTier>() }
    var loaded by remember { mutableStateOf(productId == null) }
    val saved by vm.saved.collectAsState()

    LaunchedEffect(productId) {
        if (productId != null && !loaded) {
            vm.load(productId) { p ->
                barcode = p.barcode; nama = p.name; kategori = p.category
                satuan = p.unit; hargaBeli = p.buyPrice.toString()
                hargaEcer = p.sellPrice.toString(); stok = p.stock.toString()
                minStok = p.minStock.toString()
                tiers.clear(); tiers.addAll(parseTiers(p.tiersJson))
                loaded = true
            }
        }
    }
    LaunchedEffect(scannedBarcode) {
        if (!scannedBarcode.isNullOrBlank()) {
            barcode = scannedBarcode
            onBarcodeConsumed()
        }
    }
    LaunchedEffect(saved) { if (saved) { vm.resetSaved(); onDone() } }

    val valid = nama.isNotBlank() && (hargaEcer.toLongOrNull() ?: 0L) > 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (productId == null) "Tambah Produk" else "Ubah Produk") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = barcode, onValueChange = { barcode = it },
                label = { Text("Barcode") }, singleLine = true,
                trailingIcon = {
                    IconButton(onClick = onScan) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scan barcode")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(value = nama, onValueChange = { nama = it },
                label = { Text("Nama produk *") }, singleLine = true,
                modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = kategori, onValueChange = { kategori = it },
                    label = { Text("Kategori") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = satuan, onValueChange = { satuan = it },
                    label = { Text("Satuan") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NumField(value = hargaBeli, onValue = { hargaBeli = it },
                    label = "Harga beli (Rp)", modifier = Modifier.weight(1f))
                NumField(value = hargaEcer, onValue = { hargaEcer = it },
                    label = "Harga ecer (Rp) *", modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NumField(value = stok, onValue = { stok = it },
                    label = "Stok", modifier = Modifier.weight(1f))
                NumField(value = minStok, onValue = { minStok = it },
                    label = "Stok minimum", modifier = Modifier.weight(1f))
            }

            Text("Harga grosir (opsional)", style = MaterialTheme.typography.titleMedium)
            tiers.forEachIndexed { i, t ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(value = t.nama,
                        onValueChange = { tiers[i] = t.copy(nama = it) },
                        label = { Text("Nama") }, singleLine = true,
                        modifier = Modifier.weight(1.2f))
                    OutlinedTextField(value = t.minQty.toString(),
                        onValueChange = { v -> tiers[i] = t.copy(minQty = v.toIntOrNull() ?: 0) },
                        label = { Text("Min.") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = t.harga.toString(),
                        onValueChange = { v -> tiers[i] = t.copy(harga = v.toLongOrNull() ?: 0L) },
                        label = { Text("Harga") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true, modifier = Modifier.weight(1.2f))
                    IconButton(onClick = { tiers.removeAt(i) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Hapus tier",
                            tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Button(onClick = { tiers.add(PriceTier("Grosir ${tiers.size + 1}", 12, 0)) }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Tambah tier grosir")
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    vm.save(
                        id = productId, barcode = barcode, nama = nama,
                        kategori = kategori, satuan = satuan,
                        hargaBeli = hargaBeli.toLongOrNull() ?: 0L,
                        hargaEcer = hargaEcer.toLongOrNull() ?: 0L,
                        tiers = tiers.filter { it.harga > 0 && it.minQty > 0 },
                        stok = stok.toIntOrNull() ?: 0,
                        minStok = minStok.toIntOrNull() ?: 0,
                    )
                },
                enabled = valid && loaded,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Simpan") }
        }
    }
}

@Composable
private fun NumField(
    value: String,
    onValue: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValue(it.filter { c -> c.isDigit() }) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier,
    )
}
