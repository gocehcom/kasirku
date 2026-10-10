package com.kasirku.app.ui.piutang

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import com.kasirku.app.data.SessionManager
import com.kasirku.app.data.local.DebtEntity
import com.kasirku.app.data.local.DebtPaymentEntity
import com.kasirku.app.domain.formatRupiah
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PiutangViewModel(
    private val repo: PosRepository,
    private val session: SessionManager,
) : ViewModel() {
    val debts = repo.debts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val totalOutstanding = repo.totalOutstanding()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    private var detailId by mutableStateOf<Long?>(null)
    val showingDetail: Long? get() = detailId

    private val _payments = mutableStateOf<List<DebtPaymentEntity>>(emptyList())
    val payments: List<DebtPaymentEntity> get() = _payments.value

    fun openDetail(id: Long) {
        detailId = id
        viewModelScope.launch { _payments.value = repo.debtPayments(id) }
    }
    fun closeDetail() { detailId = null }

    fun bayar(debtId: Long, amount: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            repo.recordDebtPayment(debtId, amount, session.current()?.name ?: "Kasir")
            _payments.value = repo.debtPayments(debtId)
            onDone()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PiutangScreen(vm: PiutangViewModel) {
    val debts by vm.debts.collectAsState()
    val outstanding by vm.totalOutstanding.collectAsState()
    val detailId = vm.showingDetail

    Scaffold(topBar = { TopAppBar(title = { Text("Piutang") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Total piutang belum lunas",
                        style = MaterialTheme.typography.bodyMedium)
                    Text(formatRupiah(outstanding),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
            if (debts.isEmpty()) {
                Text("Belum ada bon/piutang.", modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn {
                    items(debts, key = { it.id }) { d ->
                        DebtRow(d, onClick = { vm.openDetail(d.id) })
                    }
                }
            }
        }
    }

    detailId?.let { id ->
        val debt = debts.firstOrNull { it.id == id }
        if (debt != null) {
            DebtDetailDialog(debt = debt, payments = vm.payments,
                onDismiss = vm::closeDetail,
                onBayar = { amount -> vm.bayar(id, amount) {} })
        }
    }
}

@Composable
private fun DebtRow(d: DebtEntity, onClick: () -> Unit) {
    val date = remember(d.createdAt) {
        SimpleDateFormat("d MMM yyyy", Locale("in", "ID")).format(Date(d.createdAt))
    }
    ListItem(
        headlineContent = { Text(d.customerName, fontWeight = FontWeight.Medium) },
        supportingContent = { Text("${d.transactionNumber} • $date") },
        trailingContent = {
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                Text(formatRupiah(d.remaining), fontWeight = FontWeight.Bold,
                    color = if (d.isPaidOff) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error)
                AssistChip(
                    onClick = {},
                    label = { Text(if (d.isPaidOff) "Lunas" else "Belum lunas") },
                )
            }
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun DebtDetailDialog(
    debt: DebtEntity,
    payments: List<DebtPaymentEntity>,
    onDismiss: () -> Unit,
    onBayar: (Long) -> Unit,
) {
    var amountStr by remember { mutableStateOf("") }
    val amount = amountStr.filter { it.isDigit() }.toLongOrNull() ?: 0L
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(debt.customerName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total bon"); Text(formatRupiah(debt.total), fontWeight = FontWeight.Medium)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Sudah dibayar"); Text(formatRupiah(debt.paid))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Sisa", fontWeight = FontWeight.Bold)
                    Text(formatRupiah(debt.remaining), fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error)
                }
                if (!debt.isPaidOff) {
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Bayar cicilan") },
                        prefix = { Text("Rp") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (payments.isNotEmpty()) {
                    Text("Riwayat pembayaran:", style = MaterialTheme.typography.labelLarge)
                    payments.forEach { p ->
                        val pd = remember(p.createdAt) {
                            SimpleDateFormat("d MMM yyyy HH:mm", Locale("in", "ID"))
                                .format(Date(p.createdAt))
                        }
                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(pd, style = MaterialTheme.typography.bodySmall)
                            Text(formatRupiah(p.amount),
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!debt.isPaidOff) {
                Button(
                    onClick = { onBayar(amount); amountStr = "" },
                    enabled = amount in 1..debt.remaining,
                ) { Text("Catat") }
            }
        },
        dismissButton = {
            FilledTonalButton(onClick = onDismiss) { Text("Tutup") }
        },
    )
}
