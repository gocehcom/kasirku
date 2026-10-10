package com.kasirku.app

import android.app.Application
import androidx.room.Room
import androidx.work.Configuration
import com.kasirku.app.billing.BillingManager
import com.kasirku.app.data.PosRepository
import com.kasirku.app.data.PosRepositoryImpl
import com.kasirku.app.data.SessionManager
import com.kasirku.app.data.TrialManager
import com.kasirku.app.data.local.AppDatabase
import com.kasirku.app.sync.SyncWorker
import com.kasirku.app.ui.VmFactory

class KasirKuApp : Application() {

    val db: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "kasirku.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    val repository: PosRepository by lazy { PosRepositoryImpl(db) }

    val sessionManager: SessionManager by lazy { SessionManager(this) }

    val trialManager: TrialManager by lazy { TrialManager(this) }

    val billingManager: BillingManager by lazy { BillingManager(this) }

    val vmFactory: VmFactory by lazy { VmFactory(repository, sessionManager) }

    override fun onCreate() {
        super.onCreate()
        SyncWorker.scheduleNightly(this)
    }
}
