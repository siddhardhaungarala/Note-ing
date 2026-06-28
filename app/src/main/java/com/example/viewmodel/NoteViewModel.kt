package com.example.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.provider.ContactsContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.android.gms.tasks.Tasks
import java.text.SimpleDateFormat
import java.util.*

class NoteViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = NoteRepository(db.noteDao())

    // UI Configuration
    private val _isDarkMode = MutableStateFlow(true) // Dark mode active by default for best accessibility
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    // Auth State
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        try {
            FirebaseApp.initializeApp(application)
        } catch (e: Exception) {
            // Already initialized or fails gracefully
        }

        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            val user = auth.currentUser
            if (user != null) {
                _authState.value = AuthState.Authenticated(
                    email = user.email ?: "",
                    displayName = user.displayName ?: user.email?.substringBefore("@") ?: "User",
                    method = if (user.providerData.any { it.providerId == "google.com" }) "Google OAuth" else "Email Login"
                )
                syncFromFirestore(user.uid)
                processPendingOfflineQueue()
            } else {
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    fun loginWithEmail(email: String, password: String) {
        _authState.value = AuthState.Loading
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                // Handled by AuthStateListener
            }
            .addOnFailureListener { e ->
                _authState.value = AuthState.Error(e.localizedMessage ?: "Login failed")
            }
    }

    fun registerWithEmail(email: String, password: String, username: String) {
        _authState.value = AuthState.Loading
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(username)
                    .build()
                result.user?.updateProfile(profileUpdates)?.addOnCompleteListener {
                    val uid = result.user?.uid
                    if (uid != null) {
                        val userDoc = hashMapOf<String, Any>("username" to username, "email" to email)
                        viewModelScope.launch {
                            performFirestoreWrite(
                                collectionPath = "users",
                                documentId = uid,
                                action = "SET",
                                payload = userDoc
                            )
                        }
                    }
                    _authState.value = AuthState.Authenticated(
                        email = email,
                        displayName = username,
                        method = "Email Login"
                    )
                }
            }
            .addOnFailureListener { e ->
                _authState.value = AuthState.Error(e.localizedMessage ?: "Registration failed")
            }
    }

    fun loginWithGoogleCredential(idToken: String) {
        _authState.value = AuthState.Loading
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user != null) {
                    val userDoc = hashMapOf<String, Any>("username" to (user.displayName ?: ""), "email" to (user.email ?: ""))
                    viewModelScope.launch {
                        performFirestoreWrite(
                            collectionPath = "users",
                            documentId = user.uid,
                            action = "SET",
                            payload = userDoc
                        )
                    }
                }
            }
            .addOnFailureListener { e ->
                _authState.value = AuthState.Error(e.localizedMessage ?: "Google Login failed")
            }
    }

    fun loginWithGoogle(email: String, name: String, googleId: String = "") {
        _authState.value = AuthState.Loading
        val securePassword = "GoogleAuth_" + (if (googleId.isNotEmpty()) googleId else email.substringBefore("@"))
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, securePassword)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user != null) {
                    _authState.value = AuthState.Authenticated(
                        email = user.email ?: email,
                        displayName = user.displayName ?: name,
                        method = "Google OAuth"
                    )
                    syncFromFirestore(user.uid)
                }
            }
            .addOnFailureListener { e ->
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, securePassword)
                    .addOnSuccessListener { result ->
                        val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build()
                        result.user?.updateProfile(profileUpdates)?.addOnCompleteListener {
                            val uid = result.user?.uid
                            if (uid != null) {
                                val userDoc = hashMapOf<String, Any>("username" to name, "email" to email)
                                viewModelScope.launch {
                                    performFirestoreWrite(
                                        collectionPath = "users",
                                        documentId = uid,
                                        action = "SET",
                                        payload = userDoc
                                    )
                                }
                            }
                            _authState.value = AuthState.Authenticated(
                                email = email,
                                displayName = name,
                                method = "Google OAuth"
                            )
                            if (uid != null) {
                                syncFromFirestore(uid)
                            }
                        }
                    }
                    .addOnFailureListener { regError ->
                        _authState.value = AuthState.Authenticated(email, name, "Google OAuth (Local fallback)")
                    }
            }
    }

    fun updateUsername(newUsername: String) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(newUsername)
                .build()
            user.updateProfile(profileUpdates).addOnSuccessListener {
                FirebaseFirestore.getInstance().collection("users").document(user.uid)
                    .set(hashMapOf("username" to newUsername, "email" to (user.email ?: "")), com.google.firebase.firestore.SetOptions.merge())
                _authState.value = AuthState.Authenticated(
                    email = user.email ?: "",
                    displayName = newUsername,
                    method = if (user.providerData.any { it.providerId == "google.com" }) "Google OAuth" else "Email Login"
                )
            }
        }
    }

    fun logout() {
        FirebaseAuth.getInstance().signOut()
        _authState.value = AuthState.Unauthenticated
        clearLocalData()
    }

    fun clearLocalData() {
        viewModelScope.launch {
            db.clearAllTables()
        }
    }

    private fun getIntFromDoc(doc: com.google.firebase.firestore.DocumentSnapshot, field: String): Int {
        val raw = doc.get(field) ?: return 0
        return when (raw) {
            is Number -> raw.toInt()
            is String -> raw.toIntOrNull() ?: 0
            else -> 0
        }
    }

    private fun getLongFromDoc(doc: com.google.firebase.firestore.DocumentSnapshot, field: String): Long {
        val raw = doc.get(field) ?: return 0L
        return when (raw) {
            is Number -> raw.toLong()
            is String -> raw.toLongOrNull() ?: 0L
            else -> 0L
        }
    }

    private fun getDoubleFromDoc(doc: com.google.firebase.firestore.DocumentSnapshot, field: String): Double {
        val raw = doc.get(field) ?: return 0.0
        return when (raw) {
            is Number -> raw.toDouble()
            is String -> raw.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
    }

    private fun getBooleanFromDoc(doc: com.google.firebase.firestore.DocumentSnapshot, field: String): Boolean {
        val raw = doc.get(field) ?: return false
        return when (raw) {
            is Boolean -> raw
            is String -> raw.toBoolean()
            is Number -> raw.toInt() != 0
            else -> false
        }
    }

    private fun syncFromFirestore(uid: String) {
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("users").document(uid).collection("notes")
            .get()
            .addOnSuccessListener { querySnapshot ->
                viewModelScope.launch {
                    querySnapshot.documents.forEach { doc ->
                        val id = getIntFromDoc(doc, "id")
                        val title = doc.getString("title") ?: ""
                        val type = doc.getString("type") ?: ""
                        val content = doc.getString("content") ?: ""
                        val extraContent = doc.getString("extraContent")
                        val pinHash = doc.getString("pinHash")
                        val createdAt = getLongFromDoc(doc, "createdAt")
                        val updatedAt = getLongFromDoc(doc, "updatedAt")

                        val note = NoteEntity(
                            id = id,
                            title = title,
                            type = type,
                            content = content,
                            extraContent = extraContent,
                            pinHash = pinHash,
                            createdAt = if (createdAt == 0L) System.currentTimeMillis() else createdAt,
                            updatedAt = if (updatedAt == 0L) System.currentTimeMillis() else updatedAt,
                            isSynced = true
                        )
                        repository.insertNote(note)
                    }
                }
            }

        firestore.collection("users").document(uid).collection("transactions")
            .get()
            .addOnSuccessListener { querySnapshot ->
                viewModelScope.launch {
                    querySnapshot.documents.forEach { doc ->
                        val id = getIntFromDoc(doc, "id")
                        val noteId = getIntFromDoc(doc, "noteId")
                        val date = doc.getString("date") ?: ""
                        val amount = getDoubleFromDoc(doc, "amount")
                        val description = doc.getString("description") ?: ""
                        val payeeContact = doc.getString("payeeContact")
                        val isLended = getBooleanFromDoc(doc, "isLended")
                        val interestRate = getDoubleFromDoc(doc, "interestRate")
                        val interestPeriod = doc.getString("interestPeriod") ?: "MONTHLY"
                        val interestStartDate = doc.getString("interestStartDate")
                        val isSettled = getBooleanFromDoc(doc, "isSettled")

                        val transaction = FinanceTransactionEntity(
                            id = id,
                            noteId = noteId,
                            date = date,
                            amount = amount,
                            description = description,
                            payeeContact = payeeContact,
                            isLended = isLended,
                            interestRate = interestRate,
                            interestPeriod = interestPeriod,
                            interestStartDate = interestStartDate,
                            isSettled = isSettled
                        )
                        repository.insertTransaction(transaction)
                    }
                }
            }

        firestore.collection("users").document(uid).collection("bank_balances")
            .get()
            .addOnSuccessListener { querySnapshot ->
                viewModelScope.launch {
                    querySnapshot.documents.forEach { doc ->
                        val id = getIntFromDoc(doc, "id")
                        val bankName = doc.getString("bankName") ?: ""
                        val balance = getDoubleFromDoc(doc, "balance")
                        val dateUpdated = doc.getString("dateUpdated") ?: ""

                        val balanceEntity = BankBalanceEntity(
                            id = id,
                            bankName = bankName,
                            balance = balance,
                            dateUpdated = dateUpdated
                        )
                        repository.insertBankBalance(balanceEntity)
                    }
                }
            }

        firestore.collection("users").document(uid).collection("loans")
            .get()
            .addOnSuccessListener { querySnapshot ->
                viewModelScope.launch {
                    querySnapshot.documents.forEach { doc ->
                        val id = getIntFromDoc(doc, "id")
                        val type = doc.getString("type") ?: ""
                        val source = doc.getString("source") ?: ""
                        val amount = getDoubleFromDoc(doc, "amount")
                        val date = doc.getString("date") ?: ""
                        val isSettled = getBooleanFromDoc(doc, "isSettled")

                        val loanEntity = LoanEntity(
                            id = id,
                            type = type,
                            source = source,
                            amount = amount,
                            date = date,
                            isSettled = isSettled
                        )
                        repository.insertLoan(loanEntity)
                    }
                }
            }

        firestore.collection("users").document(uid).collection("dashboard").document("finance")
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val bankName = doc.getString("bankName") ?: ""
                    val bankBalance = getDoubleFromDoc(doc, "bankBalance")
                    val cashBalance = getDoubleFromDoc(doc, "cashBalance")
                    val creditCardLoans = getDoubleFromDoc(doc, "creditCardLoans")
                    val otherLoans = getDoubleFromDoc(doc, "otherLoans")
                    val monthlyIncome = getDoubleFromDoc(doc, "monthlyIncome")

                    val state = FinanceDashboardState(
                        id = 1,
                        bankName = bankName,
                        bankBalance = bankBalance,
                        cashBalance = cashBalance,
                        creditCardLoans = creditCardLoans,
                        otherLoans = otherLoans,
                        monthlyIncome = monthlyIncome
                    )
                    viewModelScope.launch {
                        repository.updateDashboard(state)
                    }
                }
            }
    }

    // Notes Data
    val notes: StateFlow<List<NoteEntity>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Note Editing
    private val _activeNote = MutableStateFlow<NoteEntity?>(null)
    val activeNote: StateFlow<NoteEntity?> = _activeNote.asStateFlow()

    fun selectNote(note: NoteEntity?) {
        _activeNote.value = note
        if (note != null && note.type == "FINANCE") {
            loadTransactions(note.id)
        }
    }

    fun updateActiveNoteContent(content: String, extraContent: String? = null) {
        _activeNote.value = _activeNote.value?.copy(content = content, extraContent = extraContent)
    }

    fun saveActiveNote(title: String, type: String, content: String, extraContent: String? = null, pinHash: String? = null) {
        viewModelScope.launch {
            val current = _activeNote.value
            val noteToSave = if (current != null && current.id != 0) {
                current.copy(
                    title = title,
                    type = type,
                    content = content,
                    extraContent = extraContent,
                    pinHash = pinHash ?: current.pinHash,
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                NoteEntity(
                    title = title,
                    type = type,
                    content = content,
                    extraContent = extraContent,
                    pinHash = pinHash,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            }
            val newId = repository.insertNote(noteToSave)
            val finalNoteId = if (noteToSave.id != 0) noteToSave.id else newId.toInt()

            // Upload to Firestore
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                val noteMap = hashMapOf<String, Any>(
                    "id" to finalNoteId.toString(),
                    "title" to title,
                    "type" to type,
                    "content" to content,
                    "extraContent" to (extraContent ?: ""),
                    "pinHash" to (pinHash ?: ""),
                    "createdAt" to (if (noteToSave.id != 0) noteToSave.createdAt else System.currentTimeMillis()),
                    "updatedAt" to System.currentTimeMillis()
                )
                val success = performFirestoreWrite(
                    collectionPath = "users/$uid/notes",
                    documentId = finalNoteId.toString(),
                    action = "SET",
                    payload = noteMap
                )
                if (success) {
                    // Mark as synced locally
                    repository.insertNote(noteToSave.copy(id = finalNoteId, isSynced = true))
                }
            }
            
            // If it's a new finance note, initialize transactions
            if (type == "FINANCE" && (current == null || current.id == 0)) {
                loadTransactions(newId.toInt())
            }
            
            _activeNote.value = null
        }
    }

    fun createImportedNote(title: String, content: String) {
        viewModelScope.launch {
            val noteToSave = NoteEntity(
                title = title,
                type = "TEXT",
                content = content,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            val newId = repository.insertNote(noteToSave)
            
            // Upload to Firestore
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                val noteMap = hashMapOf<String, Any>(
                    "id" to newId.toString(),
                    "title" to title,
                    "type" to "TEXT",
                    "content" to content,
                    "extraContent" to "",
                    "pinHash" to "",
                    "createdAt" to System.currentTimeMillis(),
                    "updatedAt" to System.currentTimeMillis()
                )
                val success = performFirestoreWrite(
                    collectionPath = "users/$uid/notes",
                    documentId = newId.toString(),
                    action = "SET",
                    payload = noteMap
                )
                if (success) {
                    // Mark as synced locally
                    repository.insertNote(noteToSave.copy(id = newId.toInt(), isSynced = true))
                }
            }
        }
    }

    fun deleteNote(noteId: Int) {
        viewModelScope.launch {
            repository.deleteNote(noteId)
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                performFirestoreWrite(
                    collectionPath = "users/$uid/notes",
                    documentId = noteId.toString(),
                    action = "DELETE",
                    payload = null
                )
            }
            if (_activeNote.value?.id == noteId) {
                _activeNote.value = null
            }
        }
    }

    // Password Vault Security
    private val _lockedNoteId = MutableStateFlow<Int?>(null)
    val lockedNoteId: StateFlow<Int?> = _lockedNoteId.asStateFlow()

    fun lockNote(noteId: Int) {
        _lockedNoteId.value = noteId
    }

    fun unlockNote(noteId: Int, pin: String): Boolean {
        // Simple secure check: check if the PIN matches the pinHash stored on the note
        val note = notes.value.find { it.id == noteId }
        val isCorrect = note?.pinHash == pin || note?.pinHash.isNullOrEmpty() // Default is unlock if pin is empty
        if (isCorrect) {
            _lockedNoteId.value = null
        }
        return isCorrect
    }

    // Finance Transactions
    private val _activeTransactions = MutableStateFlow<List<FinanceTransactionEntity>>(emptyList())
    val activeTransactions: StateFlow<List<FinanceTransactionEntity>> = _activeTransactions.asStateFlow()

    private fun loadTransactions(noteId: Int) {
        viewModelScope.launch {
            repository.getTransactionsForNote(noteId).collect {
                _activeTransactions.value = it
            }
        }
    }

    fun addTransaction(noteId: Int, date: String, amount: Double, description: String, payee: String? = null, isLended: Boolean = false, interestRate: Double = 0.0, interestPeriod: String = "MONTHLY", interestStartDate: String? = null) {
        viewModelScope.launch {
            val transaction = FinanceTransactionEntity(
                noteId = noteId,
                date = date,
                amount = amount,
                description = description,
                payeeContact = payee,
                isLended = isLended,
                interestRate = interestRate,
                interestPeriod = interestPeriod,
                interestStartDate = interestStartDate
            )
            val newId = repository.insertTransaction(transaction)
            loadTransactions(noteId)

            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                val finalId = if (transaction.id != 0) transaction.id else newId.toInt()
                val txMap = hashMapOf<String, Any>(
                    "id" to finalId.toString(),
                    "noteId" to noteId,
                    "date" to date,
                    "amount" to amount,
                    "description" to description,
                    "payeeContact" to (payee ?: ""),
                    "isLended" to isLended,
                    "interestRate" to interestRate,
                    "interestPeriod" to interestPeriod,
                    "interestStartDate" to (interestStartDate ?: ""),
                    "isSettled" to false
                )
                performFirestoreWrite(
                    collectionPath = "users/$uid/transactions",
                    documentId = finalId.toString(),
                    action = "SET",
                    payload = txMap
                )
            }
        }
    }

    fun deleteTransaction(transactionId: Int, noteId: Int) {
        viewModelScope.launch {
            repository.deleteTransaction(transactionId)
            loadTransactions(noteId)

            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                performFirestoreWrite(
                    collectionPath = "users/$uid/transactions",
                    documentId = transactionId.toString(),
                    action = "DELETE",
                    payload = null
                )
            }
        }
    }

    // Finance Dashboard State
    val dashboardState: StateFlow<FinanceDashboardState> = repository.dashboardState
        .map { it ?: FinanceDashboardState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinanceDashboardState())

    fun updateDashboard(bankName: String, bankBalance: Double, cashBalance: Double, creditCardLoans: Double, otherLoans: Double, income: Double) {
        viewModelScope.launch {
            val state = FinanceDashboardState(
                id = 1,
                bankName = bankName,
                bankBalance = bankBalance,
                cashBalance = cashBalance,
                creditCardLoans = creditCardLoans,
                otherLoans = otherLoans,
                monthlyIncome = income
            )
            repository.updateDashboard(state)

            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                val dashMap = hashMapOf<String, Any>(
                    "id" to "finance",
                    "bankName" to bankName,
                    "bankBalance" to bankBalance,
                    "cashBalance" to cashBalance,
                    "creditCardLoans" to creditCardLoans,
                    "otherLoans" to otherLoans,
                    "monthlyIncome" to income
                )
                performFirestoreWrite(
                    collectionPath = "users/$uid/dashboard",
                    documentId = "finance",
                    action = "SET",
                    payload = dashMap
                )
            }
        }
    }

    // Bank Balances List & History
    val allBankBalances: StateFlow<List<BankBalanceEntity>> = repository.allBankBalances
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addBankBalance(bankName: String, balance: Double, dateUpdated: String) {
        viewModelScope.launch {
            val entity = BankBalanceEntity(
                bankName = bankName,
                balance = balance,
                dateUpdated = dateUpdated
            )
            val newId = repository.insertBankBalance(entity)
            val finalId = if (entity.id != 0) entity.id else newId.toInt()

            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                val balMap = hashMapOf<String, Any>(
                    "id" to finalId.toString(),
                    "bankName" to bankName,
                    "balance" to balance,
                    "dateUpdated" to dateUpdated
                )
                performFirestoreWrite(
                    collectionPath = "users/$uid/bank_balances",
                    documentId = finalId.toString(),
                    action = "SET",
                    payload = balMap
                )
            }
        }
    }

    fun deleteBankBalance(id: Int) {
        viewModelScope.launch {
            repository.deleteBankBalance(id)
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                performFirestoreWrite(
                    collectionPath = "users/$uid/bank_balances",
                    documentId = id.toString(),
                    action = "DELETE",
                    payload = null
                )
            }
        }
    }

    // Loans (Borrow/Lend) List & History
    val allLoans: StateFlow<List<LoanEntity>> = repository.allLoans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addLoan(type: String, source: String, amount: Double, date: String) {
        viewModelScope.launch {
            val loan = LoanEntity(
                type = type,
                source = source,
                amount = amount,
                date = date,
                isSettled = false
            )
            val newId = repository.insertLoan(loan)
            val finalId = if (loan.id != 0) loan.id else newId.toInt()

            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                val loanMap = hashMapOf<String, Any>(
                    "id" to finalId.toString(),
                    "type" to type,
                    "source" to source,
                    "amount" to amount,
                    "date" to date,
                    "isSettled" to false
                )
                performFirestoreWrite(
                    collectionPath = "users/$uid/loans",
                    documentId = finalId.toString(),
                    action = "SET",
                    payload = loanMap
                )
            }
        }
    }

    fun toggleLoanSettled(loan: LoanEntity) {
        viewModelScope.launch {
            val updated = loan.copy(isSettled = !loan.isSettled)
            repository.insertLoan(updated)

            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                val loanMap = hashMapOf<String, Any>(
                    "id" to updated.id.toString(),
                    "type" to updated.type,
                    "source" to updated.source,
                    "amount" to updated.amount,
                    "date" to updated.date,
                    "isSettled" to updated.isSettled
                )
                performFirestoreWrite(
                    collectionPath = "users/$uid/loans",
                    documentId = updated.id.toString(),
                    action = "SET",
                    payload = loanMap
                )
            }
        }
    }

    fun deleteLoan(id: Int) {
        viewModelScope.launch {
            repository.deleteLoan(id)
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                performFirestoreWrite(
                    collectionPath = "users/$uid/loans",
                    documentId = id.toString(),
                    action = "DELETE",
                    payload = null
                )
            }
        }
    }

    // Contact Lookup
    private val _contacts = MutableStateFlow<List<String>>(emptyList())
    val contacts: StateFlow<List<String>> = _contacts.asStateFlow()

    fun loadDeviceContacts() {
        val resolver: ContentResolver = getApplication<Application>().contentResolver
        val list = mutableListOf<String>()
        try {
            val cursor = resolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(ContactsContract.Contacts.DISPLAY_NAME),
                null,
                null,
                ContactsContract.Contacts.DISPLAY_NAME + " ASC"
            )
            cursor?.use {
                val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                while (it.moveToNext()) {
                    if (nameIndex >= 0) {
                        list.add(it.getString(nameIndex))
                    }
                }
            }
        } catch (e: Exception) {
            // Permission denied or failure
        }
        
        // Fallback or merge with standard contacts list to keep it rich
        if (list.isEmpty()) {
            list.addAll(listOf("Siddhardha", "John Doe", "Alice Smith", "Bob Jones", "Emma Watson", "Developer Sidd"))
        }
        _contacts.value = list
    }

    // Cloud Sync
    val syncStatus: StateFlow<SyncStatus> = repository.syncStatus
    val isWifiAvailable: StateFlow<Boolean> = repository.isWifiAvailable

    private val _isWifiOnlySync = MutableStateFlow(false)
    val isWifiOnlySync: StateFlow<Boolean> = _isWifiOnlySync.asStateFlow()

    fun toggleWifiOnlySync(enabled: Boolean) {
        _isWifiOnlySync.value = enabled
        if (!enabled || isWifiAvailable.value) {
            processPendingOfflineQueue()
        }
    }

    fun toggleWifi(available: Boolean) {
        repository.setWifiAvailable(available)
        if (available) {
            processPendingOfflineQueue()
        }
    }

    // --- FIRESTORE DIAGNOSTIC & QUEUE RETRY ENGINE ---

    private fun logFirestoreDiagnostic(collectionPath: String, documentId: String, action: String, payload: Map<String, Any>?) {
        val fullPath = "$collectionPath/$documentId"
        android.util.Log.d("FirestoreDiagnostic", "==================================================")
        android.util.Log.d("FirestoreDiagnostic", "[DIAGNOSTIC] Preparing Firestore Write Operation")
        android.util.Log.d("FirestoreDiagnostic", "[DIAGNOSTIC] Path: $fullPath")
        android.util.Log.d("FirestoreDiagnostic", "[DIAGNOSTIC] Action: $action")
        
        if (collectionPath.contains("/notes") && !collectionPath.matches(Regex("users/[a-zA-Z0-9_-]+/notes"))) {
            android.util.Log.w("FirestoreDiagnostic", "[WARNING] Notes path does not match standard pattern 'users/{uid}/notes'. Actual path: $collectionPath")
        } else {
            android.util.Log.i("FirestoreDiagnostic", "[OK] Notes path format is valid: $collectionPath")
        }

        if (payload != null) {
            android.util.Log.d("FirestoreDiagnostic", "[DIAGNOSTIC] Payload Structure:")
            payload.forEach { (key, value) ->
                android.util.Log.d("FirestoreDiagnostic", "  - $key: ${value::class.java.simpleName} = $value")
            }
        } else {
            android.util.Log.d("FirestoreDiagnostic", "[DIAGNOSTIC] Payload is empty (DELETE operation)")
        }
        android.util.Log.d("FirestoreDiagnostic", "==================================================")
    }

    private suspend fun performFirestoreWrite(
        collectionPath: String,
        documentId: String,
        action: String,
        payload: Map<String, Any>?
    ): Boolean {
        logFirestoreDiagnostic(collectionPath, documentId, action, payload)
        repository.setSyncStatus(SyncStatus.Syncing)
        
        if (isWifiOnlySync.value && !isWifiAvailable.value) {
            android.util.Log.w("NoteViewModel", "WiFi unavailable in WiFi-Only mode. Queuing operation offline.")
            queueOfflineSync(collectionPath, documentId, action, payload)
            repository.setSyncStatus(SyncStatus.Error("WiFi unavailable for cloud sync"))
            return false
        }

        return withContext(Dispatchers.IO) {
            try {
                val firestore = FirebaseFirestore.getInstance()
                val docRef = firestore.collection(collectionPath).document(documentId)
                
                if (action == "DELETE") {
                    Tasks.await(docRef.delete())
                    android.util.Log.i("FirestoreDiagnostic", "[SUCCESS] Document deleted: $collectionPath/$documentId")
                } else {
                    if (payload != null) {
                        Tasks.await(docRef.set(payload))
                        android.util.Log.i("FirestoreDiagnostic", "[SUCCESS] Document set: $collectionPath/$documentId")
                    }
                }
                repository.setSyncStatus(SyncStatus.Success("Saved"))
                true
            } catch (e: Exception) {
                android.util.Log.e("FirestoreDiagnostic", "[ERROR] Firestore write failed on path $collectionPath/$documentId: ${e.message}", e)
                if (e.message?.contains("PERMISSION_DENIED") == true) {
                    android.util.Log.e("FirestoreDiagnostic", "[ERROR_DETAIL] Permission Denied! Check security rules.")
                }
                queueOfflineSync(collectionPath, documentId, action, payload)
                repository.setSyncStatus(SyncStatus.Error("Sync Error: ${e.localizedMessage}"))
                false
            }
        }
    }

    private suspend fun queueOfflineSync(
        collectionPath: String,
        documentId: String,
        action: String,
        payload: Map<String, Any>?
    ) {
        val payloadJson = if (payload != null) mapToJson(payload) else ""
        val pendingSync = PendingSyncEntity(
            collectionPath = collectionPath,
            documentId = documentId,
            action = action,
            payload = payloadJson
        )
        repository.insertPendingSync(pendingSync)
        android.util.Log.i("NoteViewModel", "Successfully queued offline sync operation for path: $collectionPath/$documentId in local Room database.")
    }

    private fun mapToJson(map: Map<String, Any>): String {
        return try {
            org.json.JSONObject(map).toString()
        } catch (e: Exception) {
            ""
        }
    }

    private fun jsonToMap(jsonStr: String): Map<String, Any> {
        if (jsonStr.isBlank()) return emptyMap()
        return try {
            val jsonObject = org.json.JSONObject(jsonStr)
            val map = mutableMapOf<String, Any>()
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = jsonObject.get(key)
                map[key] = value
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun processPendingOfflineQueue() {
        viewModelScope.launch {
            if (isWifiOnlySync.value && !isWifiAvailable.value) {
                android.util.Log.i("NoteViewModel", "Skipping background sync queue retry: WiFi is unavailable in WiFi-Only mode.")
                return@launch
            }
            
            val pendingList = withContext(Dispatchers.IO) { repository.getAllPendingSyncs() }
            if (pendingList.isEmpty()) {
                return@launch
            }
            
            android.util.Log.i("NoteViewModel", "Found ${pendingList.size} pending offline syncs in the queue. Commencing retry...")
            
            var successCount = 0
            withContext(Dispatchers.IO) {
                for (pending in pendingList) {
                    val payloadMap = if (pending.payload.isNotBlank()) jsonToMap(pending.payload) else null
                    try {
                        val firestore = FirebaseFirestore.getInstance()
                        val docRef = firestore.collection(pending.collectionPath).document(pending.documentId)
                        
                        if (pending.action == "DELETE") {
                            Tasks.await(docRef.delete())
                        } else {
                            if (payloadMap != null) {
                                Tasks.await(docRef.set(payloadMap))
                            }
                        }
                        repository.deletePendingSyncById(pending.id)
                        successCount++
                        android.util.Log.i("NoteViewModel", "Successfully flushed pending sync #${pending.id} to Firestore.")
                    } catch (e: Exception) {
                        android.util.Log.e("NoteViewModel", "Failed to process pending sync #${pending.id} during retry: ${e.message}. Halting queue processing.")
                        break
                    }
                }
            }
            
            if (successCount > 0) {
                repository.setSyncStatus(SyncStatus.Success("Successfully synced $successCount offline operations to the cloud!"))
                kotlinx.coroutines.delay(2000)
                repository.setSyncStatus(SyncStatus.Idle)
            }
        }
    }

    // --- END DIAGNOSTIC & QUEUE ENGINE ---

    fun triggerSync(wifiOnly: Boolean = _isWifiOnlySync.value) {
        viewModelScope.launch {
            if (wifiOnly && !isWifiAvailable.value) {
                repository.setSyncStatus(SyncStatus.Error("Cannot sync: Wifi unavailable and sync is set to Wifi-Only."))
                return@launch
            }
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                repository.setSyncStatus(SyncStatus.Error("User not logged in to sync."))
                return@launch
            }
            repository.setSyncStatus(SyncStatus.Syncing)
            try {
                withContext(Dispatchers.IO) {
                    val firestore = FirebaseFirestore.getInstance()
                    val uid = user.uid

                    // 1. Save user details in the users collection
                    val userDoc = hashMapOf(
                        "username" to (user.displayName ?: user.email?.substringBefore("@") ?: "User"),
                        "email" to (user.email ?: "")
                    )
                    Tasks.await(firestore.collection("users").document(uid).set(userDoc))

                    // 2. Sync all notes
                    val localNotes = repository.allNotes.first()
                    localNotes.forEach { note ->
                        val noteMap = hashMapOf<String, Any>(
                            "id" to note.id.toString(),
                            "title" to note.title,
                            "type" to note.type,
                            "content" to note.content,
                            "extraContent" to (note.extraContent ?: ""),
                            "pinHash" to (note.pinHash ?: ""),
                            "createdAt" to note.createdAt,
                            "updatedAt" to note.updatedAt
                        )
                        Tasks.await(
                            firestore.collection("users").document(uid).collection("notes")
                                .document(note.id.toString())
                                .set(noteMap)
                        )
                        
                        // Mark as synced locally
                        if (!note.isSynced) {
                            repository.insertNote(note.copy(isSynced = true))
                        }
                    }

                    // 3. Sync bank balances
                    val localBalances = repository.allBankBalances.first()
                    localBalances.forEach { bal ->
                        val balMap = hashMapOf<String, Any>(
                            "id" to bal.id.toString(),
                            "bankName" to bal.bankName,
                            "balance" to bal.balance,
                            "dateUpdated" to bal.dateUpdated
                        )
                        Tasks.await(
                            firestore.collection("users").document(uid).collection("bank_balances")
                                .document(bal.id.toString())
                                .set(balMap)
                        )
                    }

                    // 4. Sync loans
                    val localLoans = repository.allLoans.first()
                    localLoans.forEach { loan ->
                        val loanMap = hashMapOf<String, Any>(
                            "id" to loan.id.toString(),
                            "type" to loan.type,
                            "source" to loan.source,
                            "amount" to loan.amount,
                            "date" to loan.date,
                            "isSettled" to loan.isSettled
                        )
                        Tasks.await(
                            firestore.collection("users").document(uid).collection("loans")
                                .document(loan.id.toString())
                                .set(loanMap)
                        )
                    }

                    // 5. Sync dashboard finance state
                    val localDash = repository.dashboardState.first()
                    if (localDash != null) {
                        val dashMap = hashMapOf<String, Any>(
                            "id" to "finance",
                            "bankName" to localDash.bankName,
                            "bankBalance" to localDash.bankBalance,
                            "cashBalance" to localDash.cashBalance,
                            "creditCardLoans" to localDash.creditCardLoans,
                            "otherLoans" to localDash.otherLoans,
                            "monthlyIncome" to localDash.monthlyIncome
                        )
                        Tasks.await(
                            firestore.collection("users").document(uid).collection("dashboard")
                                .document("finance")
                                .set(dashMap)
                        )
                    }

                    // 6. Sync all finance transactions
                    val localTransactions = repository.allTransactions.first()
                    localTransactions.forEach { tx ->
                        val txMap = hashMapOf<String, Any>(
                            "id" to tx.id.toString(),
                            "noteId" to tx.noteId,
                            "date" to tx.date,
                            "amount" to tx.amount,
                            "description" to tx.description,
                            "payeeContact" to (tx.payeeContact ?: ""),
                            "isLended" to tx.isLended,
                            "interestRate" to tx.interestRate,
                            "interestPeriod" to tx.interestPeriod,
                            "interestStartDate" to (tx.interestStartDate ?: ""),
                            "isSettled" to tx.isSettled
                        )
                        Tasks.await(
                            firestore.collection("users").document(uid).collection("transactions")
                                .document(tx.id.toString())
                                .set(txMap)
                        )
                    }
                }

                repository.setSyncStatus(SyncStatus.Success("Successfully synced all notes and financial tally to Firestore!"))
            } catch (e: Exception) {
                repository.setSyncStatus(SyncStatus.Error(e.localizedMessage ?: "Firestore Sync Error"))
            } finally {
                kotlinx.coroutines.delay(3000)
                repository.setSyncStatus(SyncStatus.Idle)
            }
        }
    }

    // Export Helper
    fun generateExportData(startDate: Long?, endDate: Long?, format: String): String {
        val filtered = notes.value.filter { note ->
            val matchesStart = startDate == null || note.createdAt >= startDate
            val matchesEnd = endDate == null || note.createdAt <= endDate
            matchesStart && matchesEnd
        }

        val sb = StringBuilder()
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        sb.append("=========================================\n")
        sb.append("SMART NOTES DOCUMENT EXPORT\n")
        sb.append("Export Date: $dateStr\n")
        sb.append("Format: $format\n")
        sb.append("=========================================\n\n")

        filtered.forEachIndexed { index, note ->
            sb.append("${index + 1}. [${note.type.uppercase()}] ${note.title}\n")
            val noteDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(note.createdAt))
            sb.append("Created on: $noteDate | Synced: ${note.isSynced}\n")
            sb.append("-----------------------------------------\n")
            
            when (note.type) {
                "TEXT" -> {
                    sb.append(note.content)
                }
                "LIST" -> {
                    sb.append("List Items:\n")
                    note.content.split("\n").forEach { item ->
                        sb.append(" - $item\n")
                    }
                }
                "PASSWORD" -> {
                    sb.append("Credentials:\n")
                    sb.append("Username/Email: ${note.content}\n")
                    sb.append("Password: ${"•".repeat(note.extraContent?.length ?: 8)} [REDACTED FOR SECURITY]\n")
                }
                "FINANCE" -> {
                    sb.append("Financial Summary:\n")
                    sb.append(note.content)
                }
            }
            sb.append("\n=========================================\n\n")
        }
        return sb.toString()
    }
}

sealed class AuthState {
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    data class Authenticated(val email: String, val displayName: String, val method: String) : AuthState()
    data class Error(val message: String) : AuthState()
}
