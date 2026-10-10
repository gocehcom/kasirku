package com.kasirku.app.data

import android.content.Context
import androidx.core.content.edit

/** Sesi login sederhana: siapa yang sedang memakai aplikasi. */
data class SessionUser(val id: Long, val name: String, val role: String)

class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("kasirku_session", Context.MODE_PRIVATE)

    fun save(user: SessionUser) {
        prefs.edit {
            putLong("uid", user.id)
            putString("uname", user.name)
            putString("urole", user.role)
        }
    }

    fun current(): SessionUser? {
        val id = prefs.getLong("uid", -1L)
        if (id < 0) return null
        return SessionUser(
            id = id,
            name = prefs.getString("uname", "Kasir") ?: "Kasir",
            role = prefs.getString("urole", "kasir") ?: "kasir",
        )
    }

    fun clear() {
        prefs.edit { clear() }
    }

    val isOwner: Boolean get() = current()?.role == "owner"
}
