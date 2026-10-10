package com.kasirku.app.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.kasirku.app.KasirKuApp
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Sinkronisasi malam hari: mengunggah transaksi yang belum tersinkron ke Firestore.
 *
 * TODO (saat Firebase sudah dikonfigurasi):
 *  1. Tambahkan google-services.json + plugin google-services.
 *  2. Ganti blok TODO di doWork() dengan Firestore batch write ke
 *     stores/{storeId}/transaksi, lalu repo.markSynced(ids).
 */
class SyncWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as KasirKuApp
        val pending = app.repository.unsynced()
        if (pending.isEmpty()) return Result.success()

        return try {
            // TODO: upload ke Firestore:
            // val storeId = ... (dari Auth custom claims / DataStore)
            // val batch = FirebaseFirestore.getInstance().batch()
            // pending.forEach { t ->
            //     val ref = db.collection("stores").document(storeId)
            //         .collection("transaksi").document(t.id.toString())
            //     batch.set(ref, t.toFirestoreMap())
            // }
            // batch.commit().await()
            // app.repository.markSynced(pending.map { it.id })

            // Untuk sekarang: anggap antrean aman di lokal (offline-first).
            // Jangan tandai synced agar tidak kehilangan data sebelum cloud aktif.
            Result.retry()
        } catch (e: Exception) {
            if (runAttemptCount >= 3) Result.failure() else Result.retry()
        }
    }

    companion object {
        private const val NAME = "sync_malam"

        /** Dijadwalkan tiap hari ~00:00, hanya saat ada koneksi. */
        fun scheduleNightly(context: Context) {
            val now = Calendar.getInstance()
            val nextMidnight = (now.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 5)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val delayMin = (nextMidnight.timeInMillis - now.timeInMillis) / 60_000L
            val req = PeriodicWorkRequestBuilder<SyncWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delayMin.coerceAtLeast(0), TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                NAME, ExistingPeriodicWorkPolicy.KEEP, req
            )
        }
    }
}
