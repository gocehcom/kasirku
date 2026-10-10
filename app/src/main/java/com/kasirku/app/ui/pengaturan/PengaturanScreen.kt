package com.kasirku.app.ui.pengaturan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.kasirku.app.sync.SyncWorker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PengaturanScreen(onLogout: () -> Unit, onLangganan: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as com.kasirku.app.KasirKuApp
    var namaToko by remember { mutableStateOf("Toko Saya") }
    var syncMsg by remember { mutableStateOf<String?>(null) }
    val trialLeft = remember { app.trialManager.daysLeft() }
    val isPremium = remember { app.billingManager.isPremium.value }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Pengaturan", style = MaterialTheme.typography.titleLarge) }) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Toko", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = namaToko, onValueChange = { namaToko = it },
                label = { Text("Nama toko") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Sinkronisasi Cloud", style = MaterialTheme.typography.titleMedium)
            Text(
                "Data tersimpan lokal dan otomatis disinkronkan ke cloud " +
                    "setiap hari pukul 00:00 saat ada koneksi internet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = {
                WorkManager.getInstance(context)
                    .enqueue(OneTimeWorkRequestBuilder<SyncWorker>().build())
                syncMsg = "Sinkronisasi dimulai di latar belakang."
            }) { Text("Sinkronkan Sekarang") }
            syncMsg?.let { Text(it, style = MaterialTheme.typography.bodySmall) }

            Text("Langganan", style = MaterialTheme.typography.titleMedium)
            Text(
                if (isPremium) "Premium aktif — terima kasih!"
                else "Masa trial tersisa $trialLeft hari.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onLangganan, modifier = Modifier.fillMaxWidth()) {
                Text(if (isPremium) "Kelola Langganan" else "Lihat Paket Langganan")
            }

            Text("Tentang", style = MaterialTheme.typography.titleMedium)
            Text("KasirKu 1.0.0 • Mode offline-first",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
            ) { Text("Keluar / Ganti Pengguna") }
        }
    }
}
