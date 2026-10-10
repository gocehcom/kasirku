package com.kasirku.app.data

import android.content.Context
import androidx.core.content.edit

/**
 * Masa trial lokal 30 hari sejak instalasi. Setelah Firebase aktif, status
 * langganan resmi dibaca dari dokumen toko (field `langganan`) — lihat
 * SyncWorker/FirestoreSync. TrialManager hanya untuk masa perkenalan offline.
 */
class TrialManager(context: Context) {
    private val prefs = context.getSharedPreferences("kasirku_trial", Context.MODE_PRIVATE)

    companion object {
        const val TRIAL_DAYS = 30L
    }

    init {
        if (!prefs.contains("install_at")) {
            prefs.edit { putLong("install_at", System.currentTimeMillis()) }
        }
    }

    private val installAt: Long get() = prefs.getLong("install_at", System.currentTimeMillis())

    fun daysUsed(): Long =
        ((System.currentTimeMillis() - installAt) / 86_400_000L).coerceAtLeast(0)

    fun daysLeft(): Long = (TRIAL_DAYS - daysUsed()).coerceAtLeast(0)

    fun isTrialActive(): Boolean = daysUsed() < TRIAL_DAYS
}
