package com.kasirku.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kasirku.app.KasirKuApp
import com.kasirku.app.ui.langganan.PaywallScreen
import com.kasirku.app.ui.langganan.SubscriptionScreen
import com.kasirku.app.ui.kasir.KasirScreen
import com.kasirku.app.ui.kasir.KasirViewModel
import com.kasirku.app.ui.laporan.LaporanScreen
import com.kasirku.app.ui.laporan.LaporanViewModel
import com.kasirku.app.ui.login.LoginScreen
import com.kasirku.app.ui.login.LoginViewModel
import com.kasirku.app.ui.piutang.PiutangScreen
import com.kasirku.app.ui.piutang.PiutangViewModel
import com.kasirku.app.ui.pengaturan.PengaturanScreen
import com.kasirku.app.ui.produk.EditProdukScreen
import com.kasirku.app.ui.produk.EditProdukViewModel
import com.kasirku.app.ui.produk.ProdukScreen
import com.kasirku.app.ui.produk.ProdukViewModel
import com.kasirku.app.ui.scan.ScanScreen
import com.kasirku.app.ui.supplier.IntakeScreen
import com.kasirku.app.ui.supplier.SupplierScreen
import com.kasirku.app.ui.supplier.SupplierViewModel

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Kasir : Screen("kasir", "Kasir", Icons.Filled.PointOfSale)
    data object Produk : Screen("produk", "Produk", Icons.Filled.Inventory2)
    data object Piutang : Screen("piutang", "Piutang", Icons.Filled.ReceiptLong)
    data object Laporan : Screen("laporan", "Laporan", Icons.Filled.Assessment)
    data object Pengaturan : Screen("pengaturan", "Pengaturan", Icons.Filled.Settings)
}

private val bottomTabs = listOf(Screen.Kasir, Screen.Produk, Screen.Piutang, Screen.Laporan, Screen.Pengaturan)

@Composable
fun KasirKuNav() {
    val app = LocalContext.current.applicationContext as KasirKuApp
    var loggedIn by remember { mutableStateOf(app.sessionManager.current() != null) }

    if (!loggedIn) {
        val vm: LoginViewModel = viewModel(factory = app.vmFactory)
        LoginScreen(vm = vm, onLoggedIn = { loggedIn = true })
    } else {
        MainNav(onLogout = { app.sessionManager.clear(); loggedIn = false })
    }
}

@Composable
private fun MainNav(onLogout: () -> Unit) {
    val app = LocalContext.current.applicationContext as KasirKuApp
    val nav = rememberNavController()
    val isPremium by app.billingManager.isPremium.collectAsState()

    DisposableEffect(Unit) {
        app.billingManager.connect()
        onDispose { }
    }
    val hasAccess = app.trialManager.isTrialActive() || isPremium

    if (!hasAccess) {
        // Trial habis & belum premium: kunci aplikasi kecuali layar langganan
        NavHost(navController = nav, startDestination = "paywall") {
            composable("paywall") {
                PaywallScreen(
                    onSubscribe = { nav.navigate("langganan") },
                    onLogout = onLogout,
                )
            }
            composable("langganan") {
                SubscriptionScreen(
                    billing = app.billingManager,
                    trial = app.trialManager,
                    onBack = { nav.popBackStack() },
                )
            }
        }
        return
    }

    MainScaffold(nav = nav, onLogout = onLogout)
}

@Composable
private fun MainScaffold(nav: NavHostController, onLogout: () -> Unit) {
    val factory = (LocalContext.current.applicationContext as KasirKuApp).vmFactory
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            // Hanya tampil di tab utama, bukan di layar edit/scan
            if (currentRoute in bottomTabs.map { it.route }) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Screen.Kasir.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Screen.Kasir.route) {
                val vm: KasirViewModel = viewModel(factory = factory)
                KasirScreen(vm = vm, nav = nav, onScan = { nav.navigate("scan") })
            }
            composable(Screen.Produk.route) {
                val vm: ProdukViewModel = viewModel(factory = factory)
                ProdukScreen(
                    vm = vm,
                    onAdd = { nav.navigate("produkEdit") },
                    onEdit = { id -> nav.navigate("produkEdit?id=$id") },
                    onSupplier = { nav.navigate("supplier") },
                    onIntake = { nav.navigate("stok_masuk") },
                )
            }
            composable("supplier") {
                val vm: SupplierViewModel = viewModel(factory = factory)
                SupplierScreen(
                    vm = vm,
                    onBack = { nav.popBackStack() },
                    onIntake = { nav.navigate("stok_masuk") },
                )
            }
            composable("stok_masuk") {
                val vm: SupplierViewModel = viewModel(factory = factory)
                IntakeScreen(
                    vm = vm,
                    onBack = { nav.popBackStack() },
                    onDone = { nav.popBackStack() },
                )
            }
            composable(
                route = "produkEdit?id={id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L }),
            ) { entry ->
                val vm: EditProdukViewModel = viewModel(factory = factory)
                val scanned by entry.savedStateHandle
                    .getStateFlow("barcode_hasil", "")
                    .collectAsState()
                EditProdukScreen(
                    vm = vm,
                    productId = entry.arguments?.getLong("id")?.takeIf { it >= 0 },
                    scannedBarcode = scanned.ifBlank { null },
                    onBarcodeConsumed = { entry.savedStateHandle["barcode_hasil"] = "" },
                    onScan = { nav.navigate("scan") },
                    onDone = { nav.popBackStack() },
                )
            }
            composable("scan") {
                ScanScreen(
                    onBarcode = { code ->
                        nav.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("barcode_hasil", code)
                        nav.popBackStack()
                    },
                    onClose = { nav.popBackStack() },
                )
            }
            composable(Screen.Piutang.route) {
                val vm: PiutangViewModel = viewModel(factory = factory)
                PiutangScreen(vm)
            }
            composable(Screen.Laporan.route) {
                val vm: LaporanViewModel = viewModel(factory = factory)
                LaporanScreen(vm = vm)
            }
            composable(Screen.Pengaturan.route) {
                PengaturanScreen(
                    onLogout = onLogout,
                    onLangganan = { nav.navigate("langganan") },
                )
            }
            composable("langganan") {
                val app = LocalContext.current.applicationContext as KasirKuApp
                SubscriptionScreen(
                    billing = app.billingManager,
                    trial = app.trialManager,
                    onBack = { nav.popBackStack() },
                )
            }
        }
    }
}
