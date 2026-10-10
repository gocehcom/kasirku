package com.kasirku.app.ui.langganan

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kasirku.app.billing.BillingManager
import com.kasirku.app.data.TrialManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    billing: BillingManager,
    trial: TrialManager,
    onBack: () -> Unit,
) {
    val isPremium by billing.isPremium.collectAsState()
    val products by billing.products.collectAsState()
    val context = LocalContext.current

    DisposableEffect(Unit) {
        billing.connect()
        onDispose { }
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Langganan") }, navigationIcon = {
            FilledTonalButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp)) { Text("‹") }
        })
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isPremium) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (isPremium) "Premium Aktif ✓"
                        else "Masa trial: ${trial.daysLeft()} hari lagi",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (isPremium)
                            "Terima kasih! Semua fitur KasirKu terbuka."
                        else
                            "Setelah trial habis, aktifkan langganan agar kasir tetap bisa dipakai.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            if (!isPremium) {
                if (products.isEmpty()) {
                    Text(
                        "Paket langganan belum tersedia. " +
                            "Buat produk subscription di Play Console dulu " +
                            "(lihat TODO di BillingManager.kt).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    products.forEach { p ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text(p.name, fontWeight = FontWeight.Bold)
                                Text(p.description,
                                    style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(8.dp))
                                val price = p.subscriptionOfferDetails
                                    ?.firstOrNull()?.pricingPhases
                                    ?.pricingPhaseList?.firstOrNull()
                                    ?.formattedPrice ?: ""
                                Button(onClick = {
                                    (context as? Activity)?.let {
                                        billing.launchPurchase(it, p)
                                    }
                                }, modifier = Modifier.fillMaxWidth()) {
                                    Text("Berlangganan • $price")
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Text("Satu langganan berlaku untuk satu toko.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Layar kunci saat trial habis dan belum berlangganan. */
@Composable
fun PaywallScreen(onSubscribe: () -> Unit, onLogout: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Masa Trial Habis",
            style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(
            "Terima kasih sudah mencoba KasirKu. Aktifkan langganan untuk terus memakai kasir.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onSubscribe, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Aktifkan Langganan")
        }
        Spacer(Modifier.height(8.dp))
        FilledTonalButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
            Text("Keluar")
        }
    }
}
