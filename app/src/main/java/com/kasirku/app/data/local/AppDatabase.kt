package com.kasirku.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProductEntity::class, TransactionEntity::class, UserEntity::class,
        CustomerEntity::class, DebtEntity::class, DebtPaymentEntity::class,
        SupplierEntity::class, StockIntakeEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun transactionDao(): TransactionDao
    abstract fun userDao(): UserDao
    abstract fun customerDao(): CustomerDao
    abstract fun debtDao(): DebtDao
    abstract fun supplierDao(): SupplierDao
    abstract fun stockIntakeDao(): StockIntakeDao
}
