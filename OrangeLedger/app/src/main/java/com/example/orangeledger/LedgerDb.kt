package com.example.orangeledger

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Entity(tableName = "expenses")
data class Expense(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val amount: Double, val spentAt: Long, val note: String = "")

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY spentAt DESC") suspend fun all(): List<Expense>
    @Query("SELECT * FROM expenses WHERE spentAt BETWEEN :from AND :to ORDER BY spentAt DESC") suspend fun range(from: Long, to: Long): List<Expense>
    @Insert suspend fun insert(item: Expense)
    @Query("DELETE FROM expenses") suspend fun clear()
    @Insert suspend fun insertAll(items: List<Expense>)
}

@Entity(tableName = "settings")
data class LedgerSettings(@PrimaryKey val id: Int = 1, val allowance: Double = 0.0, val savings: Double = 0.0, val currency: String = "SAR")

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id=1") suspend fun get(): LedgerSettings?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun save(s: LedgerSettings)
}

@Database(entities = [Expense::class, LedgerSettings::class], version = 1, exportSchema = false)
abstract class LedgerDb : RoomDatabase() {
    abstract fun expenses(): ExpenseDao
    abstract fun settings(): SettingsDao
    companion object {
        @Volatile private var INSTANCE: LedgerDb? = null
        fun get(context: Context): LedgerDb = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, LedgerDb::class.java, "orange-ledger.db").build().also { INSTANCE = it }
        }
    }
}
