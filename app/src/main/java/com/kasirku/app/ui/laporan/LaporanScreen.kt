package com.kasirku.app.ui.laporan

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasirku.app.data.PosRepository
import com.kasirku.app.data.ReportSummary
import com.kasirku.app.data.local.TransactionEntity
import com.kasirku.app.domain.formatRupiah
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class LaporanViewModel(private val repo: PosRepository) : ViewModel() {
    private val _day = MutableStateFlow(todayStart())
    val day: StateFlow<Long> = _day

    private val _summary = MutableStateFlow(ReportSummary(0, 0, 0, emptyMap()))
    val summary: StateFlow<ReportSummary> = _summary

    private val _list = MutableStateFlow<List<TransactionEntity>>(emptyList())
    val list: StateFlow<List<TransactionEntity>> = _list

    init { refresh() }

    fun setDay(millis: Long) {
        _day.value = dayStart(millis)
        refresh()
    }

    fun shiftDay(delta: Int) {
        val c = Calendar.getInstance().apply { timeInMillis = _day.value; add(Calendar.DAY_OF_YEAR, delta) }
        setDay(c.timeInMillis)
    }

    private fun refresh() {
        viewModelScope.launch {
            val start = _day.value
            val end = start + 24 * 60 * 60 * 1000L - 1
            _summary.value = repo.report(start, end)
            _list.value = repo.transactionsBetween(start, end)
        }
    }

    companion object {
        fun dayStart(millis: Long): Long {
            val c = Calendar.getInstance().apply {
                timeInMillis = millis
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            return c.timeInMillis
        }
        fun todayStart() = dayStart(System.currentTimeMillis())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaporanScreen(vm: LaporanViewModel) {
    val summary by vm.summary.collectAsState()
    val list by vm.list.collectAsState()
    val day by vm.day.collectAsState()
    var showPicker by remember { mutableStateOf(false) }
    val dayLabel = SimpleDateFormat("EEEE, d MMM yyyy", Locale("id", "ID")).format(Date(day))

    Scaffold(
        topBar = { TopAppBar(title = { Text("Laporan", style = MaterialTheme.typography.titleLarge) }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(onClick = { vm.shiftDay(-1) }) { Text("‹") }
                    OutlinedButton(
                        onClick = { showPicker = true },
                        modifier = Modifier.weight(1f),
                    ) { Text(dayLabel) }
                    OutlinedButton(onClick = { vm.shiftDay(1) }) { Text("›") }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Omzet", formatRupiah(summary.omzet), Modifier.weight(1f))
                    StatCard("Laba", formatRupiah(summary.laba), Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Transaksi", "${summary.jumlahTransaksi}", Modifier.weight(1f))
                    val avg = if (summary.jumlahTransaksi > 0)
                        summary.omzet / summary.jumlahTransaksi else 0L
                    StatCard("Rata-rata", formatRupiah(avg), Modifier.weight(1f))
                }
            }
            if (summary.perMetode.isNotEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Per metode bayar", style = MaterialTheme.typography.titleSmall)
                            summary.perMetode.forEach { (m, v) ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(m.replaceFirstChar { it.uppercase() })
                                    Text(formatRupiah(v), fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }
            item {
                Text("Transaksi (${list.size})", style = MaterialTheme.typography.titleMedium)
            }
            items(list, key = { it.id }) { t ->
                val time = SimpleDateFormat("HH:mm", Locale("id", "ID")).format(Date(t.createdAt))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(t.number, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text("$time • ${t.payMethod.uppercase()} • ${t.cashierName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(formatRupiah(t.total), fontWeight = FontWeight.SemiBold)
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (showPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = day)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let(vm::setDay)
                    showPicker = false
                }) { Text("Pilih") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Batal") } },
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(value, style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}
