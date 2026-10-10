package com.kasirku.app.domain

import java.security.MessageDigest

/**
 * Hash PIN untuk penyimpanan lokal. Catatan keamanan: SHA-256 cukup untuk
 * gembok kasir di perangkat sendiri; untuk autentikasi server gunakan
 * bcrypt/argon2 di backend, jangan pernah kirim PIN mentah.
 */
fun hashPin(pin: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

fun verifyPin(pin: String, hash: String): Boolean = hashPin(pin) == hash
