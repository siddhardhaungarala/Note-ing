package com.example.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

class NoteRepository(private val noteDao: NoteDao) {

    val allNotes: Flow<List<NoteEntity>> = noteDao.getAllNotes()
    val allTransactions: Flow<List<FinanceTransactionEntity>> = noteDao.getAllTransactions()
    val dashboardState: Flow<FinanceDashboardState?> = noteDao.getDashboardState()

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _isWifiAvailable = MutableStateFlow(true) // Simulated Network State
    val isWifiAvailable: StateFlow<Boolean> = _isWifiAvailable.asStateFlow()

    suspend fun getNoteById(id: Int): NoteEntity? = noteDao.getNoteById(id)

    suspend fun insertNote(note: NoteEntity): Long {
        val updatedNote = note.copy(updatedAt = System.currentTimeMillis(), isSynced = false)
        return noteDao.insertNote(updatedNote)
    }

    suspend fun deleteNote(id: Int) {
        noteDao.deleteNoteById(id)
        noteDao.deleteTransactionsByNoteId(id)
    }

    val allPendingSyncsFlow: Flow<List<PendingSyncEntity>> = noteDao.getAllPendingSyncsFlow()

    suspend fun getAllPendingSyncs(): List<PendingSyncEntity> = noteDao.getAllPendingSyncs()

    suspend fun insertPendingSync(pendingSync: PendingSyncEntity): Long = noteDao.insertPendingSync(pendingSync)

    suspend fun deletePendingSyncById(id: Int) = noteDao.deletePendingSyncById(id)

    suspend fun clearAllPendingSyncs() = noteDao.clearAllPendingSyncs()

    fun getTransactionsForNote(noteId: Int): Flow<List<FinanceTransactionEntity>> {
        return noteDao.getTransactionsForNote(noteId)
    }

    suspend fun insertTransaction(transaction: FinanceTransactionEntity): Long {
        return noteDao.insertTransaction(transaction)
    }

    suspend fun deleteTransaction(id: Int) {
        noteDao.deleteTransactionById(id)
    }

    suspend fun updateDashboard(state: FinanceDashboardState) {
        noteDao.insertDashboardState(state)
    }

    // Bank Balances
    val allBankBalances: Flow<List<BankBalanceEntity>> = noteDao.getAllBankBalances()

    suspend fun insertBankBalance(bankBalance: BankBalanceEntity): Long {
        return noteDao.insertBankBalance(bankBalance)
    }

    suspend fun deleteBankBalance(id: Int) {
        noteDao.deleteBankBalanceById(id)
    }

    // Loans
    val allLoans: Flow<List<LoanEntity>> = noteDao.getAllLoans()

    suspend fun insertLoan(loan: LoanEntity): Long {
        return noteDao.insertLoan(loan)
    }

    suspend fun deleteLoan(id: Int) {
        noteDao.deleteLoanById(id)
    }

    fun setWifiAvailable(available: Boolean) {
        _isWifiAvailable.value = available
    }

    fun setSyncStatus(status: SyncStatus) {
        _syncStatus.value = status
    }

    suspend fun performSync(wifiOnly: Boolean) {
        if (wifiOnly && !_isWifiAvailable.value) {
            _syncStatus.value = SyncStatus.Error("Cannot sync: Wifi unavailable and sync is set to Wifi-Only.")
            return
        }

        _syncStatus.value = SyncStatus.Syncing
        try {
            // Simulate API roundtrip for notes and transactions
            delay(1500)
            
            // Mark all notes as synced
            val notes = noteDao.getAllNotes().first()
            notes.forEach { note ->
                if (!note.isSynced) {
                    noteDao.insertNote(note.copy(isSynced = true))
                }
            }
            
            _syncStatus.value = SyncStatus.Success("Successfully synced all notes and financial tally offline data to the cloud.")
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.Error(e.message ?: "Unknown sync error")
        } finally {
            delay(2000)
            _syncStatus.value = SyncStatus.Idle
        }
    }
}

sealed class SyncStatus {
    object Idle : SyncStatus()
    object Syncing : SyncStatus()
    data class Success(val message: String) : SyncStatus()
    data class Error(val error: String) : SyncStatus()
}
