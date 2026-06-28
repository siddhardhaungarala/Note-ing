package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        NoteEntity::class,
        FinanceTransactionEntity::class,
        FinanceDashboardState::class,
        BankBalanceEntity::class,
        LoanEntity::class,
        PendingSyncEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_notes_database"
                )
                .fallbackToDestructiveMigration() // Simple option for development
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
