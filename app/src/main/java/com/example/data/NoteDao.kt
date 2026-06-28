package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    // Note Queries
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Int): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Int)

    // Finance Transactions
    @Query("SELECT * FROM finance_transactions WHERE noteId = :noteId ORDER BY date DESC")
    fun getTransactionsForNote(noteId: Int): Flow<List<FinanceTransactionEntity>>

    @Query("SELECT * FROM finance_transactions")
    fun getAllTransactions(): Flow<List<FinanceTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: FinanceTransactionEntity): Long

    @Query("DELETE FROM finance_transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Int)

    @Query("DELETE FROM finance_transactions WHERE noteId = :noteId")
    suspend fun deleteTransactionsByNoteId(noteId: Int)

    // Finance Dashboard State
    @Query("SELECT * FROM finance_dashboard WHERE id = 1")
    fun getDashboardState(): Flow<FinanceDashboardState?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDashboardState(state: FinanceDashboardState)

    // Bank Balances
    @Query("SELECT * FROM bank_balances ORDER BY dateUpdated DESC")
    fun getAllBankBalances(): Flow<List<BankBalanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBankBalance(bankBalance: BankBalanceEntity): Long

    @Query("DELETE FROM bank_balances WHERE id = :id")
    suspend fun deleteBankBalanceById(id: Int)

    // Loans (Borrow/Lend)
    @Query("SELECT * FROM loans ORDER BY date DESC")
    fun getAllLoans(): Flow<List<LoanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: LoanEntity): Long

    @Query("DELETE FROM loans WHERE id = :id")
    suspend fun deleteLoanById(id: Int)

    // Pending Syncs Queue
    @Query("SELECT * FROM pending_syncs ORDER BY timestamp ASC")
    fun getAllPendingSyncsFlow(): Flow<List<PendingSyncEntity>>

    @Query("SELECT * FROM pending_syncs ORDER BY timestamp ASC")
    suspend fun getAllPendingSyncs(): List<PendingSyncEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingSync(pendingSync: PendingSyncEntity): Long

    @Query("DELETE FROM pending_syncs WHERE id = :id")
    suspend fun deletePendingSyncById(id: Int)

    @Query("DELETE FROM pending_syncs")
    suspend fun clearAllPendingSyncs()
}
