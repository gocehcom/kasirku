package com.kasirku.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Pengguna aplikasi: owner (pemilik) atau kasir. PIN disimpan sebagai hash SHA-256. */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val role: String = "kasir", // owner|kasir
    val pinHash: String,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)
