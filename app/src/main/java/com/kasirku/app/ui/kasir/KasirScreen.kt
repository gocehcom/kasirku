package com.kasirku.app.ui.kasir

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.kasirku.app.data.local.ProductEntity
import com.kasirku.app.domain.CartLine
import com.kasirku.app.domain.formatRupiah
import com.kasirku.app.domain.parseTiers
import com.kasirku.app.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KasirScreen(vm: KasirViewModel, nav: NavController, onScan: () -> Unit) {
    // Ambil hasil scan barcode dari layar Scan, lalu langsung masukkan ke keranjang
    val entry = remember(nav) { nav.getBackStackEntry(Screen.Kasir.route) }
    val barcode by entry.savedStateHandle.getStateFlow("barcode_hasil", "").collectAsState()
    LaunchedEffect(barcode) {
        if (barcode.isNotEmpty()) {
            vm.addByBarcode(barcode)
            entry.savedStateHandle["barcode_hasil"] = ""
        }
    }

    KasirBody(vm, onScan)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KasirBody(vm: KasirViewModel, onScan: () -> Unit) {
    val products by vm.products.collectAsState()
    val cart by vm.cart.collectAsState()
    val query by vm.query.collectAsState()
    val message by vm.message.collectAsState()
    val lastReceipt by vm.lastReceipt.collectAsState()
    val showCheckout by vm.showCheckout.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var showCart by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); vm.consumeMessage() }
    }

    lastReceipt?.let { receipt ->
        ReceiptDialog(
            text = receipt.text,
            onShare = { shareReceipt(context, receipt.text) },
            onDismiss = vm::consumeReceipt,
        )
    }

    val total = cart.values.sumOf { it.subtotal }
    val itemCount = cart.values.sumOf { it.qty }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { TopAppBar(title = { Text("Kasir", style = MaterialTheme.typography.titleLarge) }) },
        bottomBar = {
            if (cart.isNotEmpty()) {
                Button(
                    onClick = { showCart = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp),
                ) {
                    Icon(Icons.Filled.ShoppingCart, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("$itemCount item • ${formatRupiah(total)}")
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = vm::setQuery,
                placeholder = { Text("Cari nama / barcode…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = onScan) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scan barcode")
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(products, key = { it.id }) { p ->
                    ProductRow(p = p, onAdd = { vm.addProduct(p) })
                }
            }
        }
    }

    if (showCart) {
        ModalBottomSheet(
            onDismissRequest = { showCart = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            dragHandle = { BottomSheetDefaults.DragHandle() },
        ) {
            CartSheet(
                cart = cart,
                onQty = vm::setQty,
                onCheckout = { showCart = false; vm.openCheckout() },
                onClear = { vm.clearCart(); showCart = false },
            )
        }
    }

    if (showCheckout) {
        CheckoutDialog(
            total = total,
            onDismiss = vm::closeCheckout,
            onPay = { method, paid, customer -> vm.checkout(method, paid, customerName = customer) },
        )
    }
}

@Composable
private fun ProductRow(p: ProductEntity, onAdd: () -> Unit) {
    val tiers = parseTiers(p.tiersJson)
    ListItem(
        headlineContent = { Text(p.name, style = MaterialTheme.typography.titleMedium) },
        supportingContent = {
            Column {
                Text(formatRupiah(p.sellPrice), style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                if (tiers.isNotEmpty()) {
                    Text(
                        tiers.joinToString(" • ") { "${it.nama} ≥${it.minQty}: ${formatRupiah(it.harga)}" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                Text(
                    if (p.stock <= 0) "Stok habis" else "Stok: ${p.stock} ${p.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (p.stock <= p.minStock) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        trailingContent = {
            FilledTonalButton(onClick = onAdd, enabled = p.stock > 0) {
                Icon(Icons.Filled.Add, contentDescription = "Tambah", modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Tambah")
            }
        },
    )
}

@Composable
private fun CartSheet(
    cart: Map<Long, CartLine>,
    onQty: (Long, Int) -> Unit,
    onCheckout: () -> Unit,
    onClear: () -> Unit,
) {
    val total = cart.values.sumOf { it.subtotal }
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text("Keranjang", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        cart.values.forEach { line ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(line.product.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${formatRupiah(line.unitPrice)}${if (line.tierName != "ecer") " (${line.tierName})" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                QtyStepper(qty = line.qty, onQty = { onQty(line.product.id, it) })
                Spacer(Modifier.width(8.dp))
                Text(formatRupiah(line.subtotal), fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.width(96.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Total", style = MaterialTheme.typography.titleMedium)
            Text(formatRupiah(total), style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = onClear, modifier = Modifier.weight(1f)) { Text("Kosongkan") }
            Button(onClick = onCheckout, modifier = Modifier.weight(2f)) { Text("Bayar") }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun QtyStepper(qty: Int, onQty: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onQty(qty - 1) }, modifier = Modifier.size(40.dp)) {
            Text("−", style = MaterialTheme.typography.titleLarge)
        }
        Text("$qty", style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(32.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        IconButton(onClick = { onQty(qty + 1) }, modifier = Modifier.size(40.dp)) {
            Text("+", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CheckoutDialog(
    total: Long,
    onDismiss: () -> Unit,
    onPay: (method: String, paid: Long, customer: String) -> Unit,
) {
    var method by remember { mutableStateOf("tunai") }
    var paidStr by remember { mutableStateOf("") }
    var customer by remember { mutableStateOf("") }
    val paid = paidStr.filter { it.isDigit() }.toLongOrNull() ?: 0L
    val change = if (method == "tunai") paid - total else 0L
    val isDebt = method == "piutang"
    val canPay = (method != "tunai" || paid >= total) && (!isDebt || customer.isNotBlank())

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pembayaran") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Total: ${formatRupiah(total)}", style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("tunai" to "Tunai", "qris" to "QRIS",
                        "transfer" to "Transfer", "ewallet" to "E-Wallet",
                        "piutang" to "Bon").forEach { (id, label) ->
                        androidx.compose.material3.FilterChip(
                            selected = method == id,
                            onClick = { method = id },
                            label = { Text(label) },
                        )
                    }
                }
                if (isDebt) {
                    OutlinedTextField(
                        value = customer,
                        onValueChange = { customer = it },
                        label = { Text("Nama pelanggan") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("Bon dicatat sebagai piutang pelanggan.",
                        style = MaterialTheme.typography.bodySmall)
                }
                if (method == "tunai") {
                    OutlinedTextField(
                        value = paidStr,
                        onValueChange = { paidStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Uang diterima") },
                        prefix = { Text("Rp") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(total, 50_000L, 100_000L).distinct().forEach { quick ->
                            FilledTonalButton(onClick = { paidStr = quick.toString() }) {
                                Text(formatRupiah(quick).removePrefix("Rp"))
                            }
                        }
                    }
                    Text(
                        if (paid >= total) "Kembalian: ${formatRupiah(change)}" else "Kurang: ${formatRupiah(total - paid)}",
                        color = if (paid >= total) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                    )
                } else if (!isDebt) {
                    Text("Pastikan pembayaran ${method.uppercase()} sudah diterima sebelum menekan Bayar.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onPay(method, if (method == "tunai") paid else total, customer) },
                enabled = canPay,
            ) {
                Text(if (isDebt) "Catat Bon ${formatRupiah(total)}" else "Bayar ${formatRupiah(total)}")
            }
        },
        dismissButton = {
            FilledTonalButton(onClick = onDismiss) { Text("Batal") }
        },
    )
}

/** Dialog struk setelah pembayaran sukses: pratinjau + bagikan (WhatsApp/dll). */
@Composable
private fun ReceiptDialog(
    text: String,
    onShare: () -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Transaksi Berhasil") },
        text = {
            Column {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .verticalScroll(androidx.compose.foundation.rememberScrollState())
                        .padding(8.dp)
                ) {
                    Text(
                        text,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onShare) { Text("Bagikan") }
        },
        dismissButton = {
            FilledTonalButton(onClick = onDismiss) { Text("Tutup") }
        },
    )
}

/** Bagikan struk teks: coba WhatsApp dulu, fallback ke chooser sistem. */
private fun shareReceipt(context: android.content.Context, text: String) {
    val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_TEXT, text)
    }
    val wa = android.content.Intent(send).setPackage("com.whatsapp")
    try {
        // Cek WhatsApp terpasang
        context.packageManager.getPackageInfo("com.whatsapp", 0)
        context.startActivity(wa)
    } catch (e: Exception) {
        context.startActivity(android.content.Intent.createChooser(send, "Bagikan struk"))
    }
}
