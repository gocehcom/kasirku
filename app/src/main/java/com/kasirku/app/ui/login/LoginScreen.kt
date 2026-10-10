package com.kasirku.app.ui.login

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasirku.app.data.PosRepository
import com.kasirku.app.data.SessionManager
import com.kasirku.app.data.SessionUser
import com.kasirku.app.data.local.UserEntity
import com.kasirku.app.domain.hashPin
import com.kasirku.app.domain.verifyPin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LoginViewModel(
    private val repo: PosRepository,
    private val session: SessionManager,
) : ViewModel() {
    val users: StateFlow<List<com.kasirku.app.data.local.UserEntity>> =
        repo.users()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _needsSetup = MutableStateFlow<Boolean?>(null)
    val needsSetup: StateFlow<Boolean?> = _needsSetup

    private val _loggedIn = MutableStateFlow(false)
    val loggedIn: StateFlow<Boolean> = _loggedIn

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        viewModelScope.launch {
            _needsSetup.value = repo.userCount() == 0
            session.current()?.let { _loggedIn.value = true }
        }
    }

    fun setupOwner(nama: String, pin: String) {
        viewModelScope.launch {
            if (nama.isBlank() || pin.length < 4) {
                _error.value = "Nama wajib diisi dan PIN minimal 4 digit"
                return@launch
            }
            val id = repo.saveUser(
                UserEntity(name = nama.trim(), role = "owner", pinHash = hashPin(pin))
            )
            session.save(SessionUser(id, nama.trim(), "owner"))
            _loggedIn.value = true
        }
    }

    fun login(user: UserEntity, pin: String) {
        viewModelScope.launch {
            val fresh = repo.findUser(user.id)
            if (fresh != null && fresh.isActive && verifyPin(pin, fresh.pinHash)) {
                session.save(SessionUser(fresh.id, fresh.name, fresh.role))
                _loggedIn.value = true
            } else {
                _error.value = "PIN salah"
            }
        }
    }

    fun addKasir(nama: String, pin: String, onDone: () -> Unit) {
        viewModelScope.launch {
            if (nama.isBlank() || pin.length < 4) {
                _error.value = "Nama wajib diisi dan PIN minimal 4 digit"
                return@launch
            }
            repo.saveUser(UserEntity(name = nama.trim(), role = "kasir", pinHash = hashPin(pin)))
            onDone()
        }
    }

    fun consumeError() { _error.value = null }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(vm: LoginViewModel, onLoggedIn: () -> Unit) {
    val needsSetup by vm.needsSetup.collectAsState()
    val loggedIn by vm.loggedIn.collectAsState()
    val error by vm.error.collectAsState()

    LaunchedEffect(loggedIn) { if (loggedIn) onLoggedIn() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("KasirKu", fontWeight = FontWeight.Bold) }) }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when (needsSetup) {
                null -> Text("Memuat…")
                true -> OwnerSetup(vm)
                false -> UserLogin(vm)
            }
            error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
                LaunchedEffect(it) { vm.consumeError() }
            }
        }
    }
}

@Composable
private fun OwnerSetup(vm: LoginViewModel) {
    var nama by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Selamat datang!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Buat akun pemilik toko dulu.", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = nama, onValueChange = { nama = it },
            label = { Text("Nama pemilik") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = pin, onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pin = it },
            label = { Text("PIN (4–6 digit)") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        Button(onClick = { vm.setupOwner(nama, pin) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Buat Akun Pemilik")
        }
    }
}

@Composable
private fun UserLogin(vm: LoginViewModel) {
    val users by vm.users.collectAsState()
    var selected by remember { mutableStateOf<UserEntity?>(null) }
    var pin by remember { mutableStateOf("") }

    if (selected == null) {
        Text("Siapa yang memakai kasir?", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        users.forEach { u ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    .clickable { selected = u; pin = "" },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(u.name, fontWeight = FontWeight.Medium)
                        Text(if (u.role == "owner") "Pemilik" else "Kasir",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    } else {
        Text("Halo, ${selected!!.name}!", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text("Masukkan PIN", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        // Indikator PIN
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(6) { i ->
                Text(
                    if (i < pin.length) "●" else "○",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(0.85f).height(320.dp),
        ) {
            items(listOf("1","2","3","4","5","6","7","8","9","","0","⌫")) { key ->
                FilledTonalButton(
                    onClick = {
                        when (key) {
                            "⌫" -> pin = pin.dropLast(1)
                            "" -> Unit
                            else -> if (pin.length < 6) {
                                pin += key
                                if (pin.length >= 4) {
                                    // Coba login otomatis saat 4+ digit? Tidak — tunggu tombol.
                                }
                            }
                        }
                    },
                    modifier = Modifier.size(72.dp),
                ) { Text(key, style = MaterialTheme.typography.titleLarge) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { selected = null; pin = "" }) { Text("Ganti") }
            Button(
                onClick = { vm.login(selected!!, pin); pin = "" },
                enabled = pin.length >= 4,
            ) { Text("Masuk") }
        }
    }
}
