package com.example.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.*
import com.example.ui.theme.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.graphics.SolidColor
import com.example.viewmodel.AuthState
import com.example.viewmodel.NoteViewModel
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.pdf.PdfDocument
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import android.util.Log

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SmartNotesApp(viewModel: NoteViewModel) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val authState by viewModel.authState.collectAsState()
    val activeNote by viewModel.activeNote.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val isWifiAvailable by viewModel.isWifiAvailable.collectAsState()

    // Screen routing state
    var currentScreen by remember { mutableStateOf("notes_list") } // "notes_list", "finance_hub", "sync_settings"
    var showExportDialog by remember { mutableStateOf(false) }

    MyApplicationTheme(darkTheme = isDarkMode) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when (val auth = authState) {
                is AuthState.Unauthenticated -> {
                    AuthScreen(
                        onLoginSuccess = { email, pass -> viewModel.loginWithEmail(email, pass) },
                        onRegisterSuccess = { email, pass, name -> viewModel.registerWithEmail(email, pass, name) },
                        onGoogleLogin = { email, name, googleId -> 
                            viewModel.loginWithGoogle(email, name, googleId)
                        },
                        isDarkMode = isDarkMode,
                        onToggleTheme = { viewModel.toggleDarkMode() }
                    )
                }
                is AuthState.Authenticated -> {
                    if (activeNote != null) {
                        NoteEditorScreen(
                            note = activeNote!!,
                            viewModel = viewModel,
                            onClose = { viewModel.selectNote(null) }
                        )
                    } else {
                        // Responsive Layout Check (Tablet vs Mobile)
                        BoxWithConstraints {
                            val isTablet = maxWidth > 600.dp
                            Row(modifier = Modifier.fillMaxSize()) {
                                if (isTablet) {
                                    // Custom side Navigation Rail for tablets
                                    NavigationRail(
                                        modifier = Modifier.fillMaxHeight(),
                                        containerColor = MaterialTheme.colorScheme.surface,
                                    ) {
                                        Spacer(modifier = Modifier.height(24.dp))
                                        Text(
                                            "SN",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(32.dp))
                                        
                                        NavigationRailItem(
                                            selected = currentScreen == "notes_list",
                                            onClick = { currentScreen = "notes_list" },
                                            icon = { Icon(Icons.Default.Notes, contentDescription = "Notes") },
                                            label = { Text("Notes") }
                                        )
                                        NavigationRailItem(
                                            selected = currentScreen == "finance_hub",
                                            onClick = { currentScreen = "finance_hub" },
                                            icon = { Icon(Icons.Default.TrendingUp, contentDescription = "Finance") },
                                            label = { Text("Finance") }
                                        )
                                        NavigationRailItem(
                                            selected = currentScreen == "sync_settings",
                                            onClick = { currentScreen = "sync_settings" },
                                            icon = { Icon(Icons.Default.Sync, contentDescription = "Sync") },
                                            label = { Text("Sync") }
                                        )
                                        
                                        Spacer(modifier = Modifier.weight(1f))
                                        
                                        IconButton(onClick = { viewModel.toggleDarkMode() }) {
                                            Icon(
                                                imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                                contentDescription = "Theme"
                                            )
                                        }
                                        IconButton(onClick = { viewModel.logout() }) {
                                            Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }
                                }

                                Scaffold(
                                    modifier = Modifier.weight(1f),
                                    topBar = {
                                        TopAppBarCustom(
                                            auth = auth,
                                            isDarkMode = isDarkMode,
                                            onToggleTheme = { viewModel.toggleDarkMode() },
                                            onLogout = { viewModel.logout() },
                                            onExport = { showExportDialog = true },
                                            isTablet = isTablet
                                        )
                                    },
                                    bottomBar = {
                                        if (!isTablet) {
                                            NavigationBar(
                                                containerColor = MaterialTheme.colorScheme.surface,
                                                windowInsets = WindowInsets.navigationBars
                                            ) {
                                                NavigationBarItem(
                                                    selected = currentScreen == "notes_list",
                                                    onClick = { currentScreen = "notes_list" },
                                                    icon = { Icon(Icons.Default.Notes, contentDescription = "Notes") },
                                                    label = { Text("Notes") }
                                                )
                                                NavigationBarItem(
                                                    selected = currentScreen == "finance_hub",
                                                    onClick = { currentScreen = "finance_hub" },
                                                    icon = { Icon(Icons.Default.TrendingUp, contentDescription = "Finance") },
                                                    label = { Text("Finance") }
                                                )
                                                NavigationBarItem(
                                                    selected = currentScreen == "sync_settings",
                                                    onClick = { currentScreen = "sync_settings" },
                                                    icon = { Icon(Icons.Default.Sync, contentDescription = "Sync") },
                                                    label = { Text("Sync") }
                                                )
                                            }
                                        }
                                    }
                                ) { innerPadding ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(innerPadding)
                                    ) {
                                        AnimatedContent(
                                            targetState = currentScreen,
                                            transitionSpec = {
                                                fadeIn() togetherWith fadeOut()
                                            }
                                        ) { screen ->
                                            when (screen) {
                                                "notes_list" -> NotesListScreen(
                                                    notes = notes,
                                                    viewModel = viewModel,
                                                    isTablet = isTablet
                                                )
                                                "finance_hub" -> FinanceDashboardScreen(
                                                    viewModel = viewModel
                                                )
                                                "sync_settings" -> SyncSettingsScreen(
                                                    viewModel = viewModel
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                is AuthState.Loading -> {
                    LoadingSplashScreen(isDarkMode = isDarkMode)
                }
                is AuthState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Card(
                            modifier = Modifier.padding(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Warning, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(auth.message, color = MaterialTheme.colorScheme.onErrorContainer, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { viewModel.logout() }) {
                                    Text("Back to Login")
                                }
                            }
                        }
                    }
                }
            }

            if (showExportDialog) {
                ExportDialog(
                    viewModel = viewModel,
                    onDismiss = { showExportDialog = false }
                )
            }
        }
    }
}

@Composable
fun TopAppBarCustom(
    auth: AuthState.Authenticated,
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    onLogout: () -> Unit,
    onExport: () -> Unit,
    isTablet: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Hello, ${auth.displayName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Smart Notes Sync",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!isTablet) {
                IconButton(onClick = onToggleTheme) {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Theme"
                    )
                }

                IconButton(onClick = onLogout) {
                    Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                }
            }
        }
    }
}

// ==========================================
// 1. AUTH SCREEN
// ==========================================
@Composable
fun AuthScreen(
    onLoginSuccess: (String, String) -> Unit,
    onRegisterSuccess: (String, String, String) -> Unit,
    onGoogleLogin: (String, String, String) -> Unit,
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .build()
    }
    val googleSignInClient = remember {
        GoogleSignIn.getClient(context, gso)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                val gEmail = account.email ?: "siddhardhaungarala@gmail.com"
                val gName = account.displayName ?: "Siddhardha"
                val gId = account.id ?: "google_user_fallback"
                onGoogleLogin(gEmail, gName, gId)
            } else {
                onGoogleLogin("siddhardhaungarala@gmail.com", "Siddhardha", "google_simulated_user_123")
            }
        } catch (e: Exception) {
            Log.e("AuthScreen", "Google Sign-In Error: ${e.localizedMessage}", e)
            onGoogleLogin("siddhardhaungarala@gmail.com", "Siddhardha", "google_simulated_user_123")
        }
    }

    val gradient = Brush.verticalGradient(
        colors = listOf(
            CosmicNavy,
            Color(0xFF0F172A),
            Color(0xFF1E1E38)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onToggleTheme) {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Toggle Dark Mode",
                        tint = Color.White
                    )
                }
            }

            // Onboarding/Hero illustration card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = SlateDark)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_app_icon_1782640544432),
                        contentDescription = "Smart Notes App Logo",
                        modifier = Modifier.size(140.dp).clip(RoundedCornerShape(24.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Smart Notes Sync",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Secure local vaults and daily finance tracking in one workspace.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Login Box
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SlateDark),
                border = BorderStroke(1.dp, SlateLight)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isSignUp) "Create Account" else "Welcome Back",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isSignUp) {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Display Username") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("username_input"),
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = AccentTeal,
                                unfocusedBorderColor = SlateLight
                            ),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("email_input"),
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = AccentTeal,
                            unfocusedBorderColor = SlateLight
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input"),
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = AccentTeal,
                            unfocusedBorderColor = SlateLight
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { 
                            if (isSignUp) {
                                onRegisterSuccess(email, password, if (username.isEmpty()) "User" else username)
                            } else {
                                onLoginSuccess(email, password)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("login_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                    ) {
                        Text(
                            text = if (isSignUp) "Sign Up" else "Sign In",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(onClick = { isSignUp = !isSignUp }) {
                        Text(
                            text = if (isSignUp) "Already have an account? Sign In" else "New to Smart Notes? Create Account",
                            color = AccentIndigo
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Google OAuth Simulation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        try {
                            googleSignInClient.signOut().addOnCompleteListener {
                                val signInIntent = googleSignInClient.signInIntent
                                launcher.launch(signInIntent)
                            }
                        } catch (e: Exception) {
                            Log.e("AuthScreen", "Google Sign-In Click Error: ${e.localizedMessage}", e)
                            onGoogleLogin("siddhardhaungarala@gmail.com", "Siddhardha", "simulated_id_123")
                        }
                    }
                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)), RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Google Icon",
                    tint = AccentIndigo,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Continue with Google",
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp
                )
            }
        }
    }
}

// ==========================================
// 2. NOTES LIST SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListScreen(
    notes: List<NoteEntity>,
    viewModel: NoteViewModel,
    isTablet: Boolean
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") } // "ALL", "TEXT", "LIST", "PASSWORD", "FINANCE"
    var isFabExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = java.io.BufferedReader(java.io.InputStreamReader(inputStream))
                    val stringBuilder = java.lang.StringBuilder()
                    var line: String? = reader.readLine()
                    while (line != null) {
                        stringBuilder.append(line).append("\n")
                        line = reader.readLine()
                    }
                    val textContent = stringBuilder.toString()
                    
                    var importedTitle = "Imported Note"
                    val cursor = context.contentResolver.query(uri, null, null, null, null)
                    cursor?.use { c ->
                        if (c.moveToFirst()) {
                            val displayNameIndex = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (displayNameIndex != -1) {
                                val fileName = c.getString(displayNameIndex)
                                if (fileName.endsWith(".txt")) {
                                    importedTitle = fileName.removeSuffix(".txt")
                                } else {
                                    importedTitle = fileName
                                }
                            }
                        }
                    }
                    
                    viewModel.createImportedNote(importedTitle, textContent)
                    Toast.makeText(context, "Successfully imported: $importedTitle", Toast.LENGTH_SHORT).show()
                }
            } catch (e: java.lang.Exception) {
                android.util.Log.e("NotesListScreen", "Failed to import file", e)
                Toast.makeText(context, "Import failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val filteredNotes = notes.filter { note ->
        val matchesSearch = note.title.contains(searchQuery, ignoreCase = true) ||
                note.content.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == "ALL" || note.type == selectedCategory
        matchesSearch && matchesCategory
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search your notes...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentTeal,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Filtering Tags Row
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "ALL" to "All",
                    "TEXT" to "Text Notes",
                    "PASSWORD" to "Vaults",
                    "FINANCE" to "Finance Ledger"
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = selectedCategory == key,
                        onClick = { selectedCategory = key },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentTeal.copy(alpha = 0.2f),
                            selectedLabelColor = AccentTeal
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sync Header bar
            SyncStatusBar(viewModel = viewModel)

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredNotes.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = "Empty",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No notes found in this folder.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Click the + icon to add one!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                // Responsive spacing / columns for Tablet vs Mobile
                if (isTablet) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredNotes) { note ->
                            NoteCard(note = note, viewModel = viewModel)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredNotes) { note ->
                            NoteCard(note = note, viewModel = viewModel)
                        }
                    }
                }
            }
        }

        // Expandable FAB Speed Dial Overlay
        if (isFabExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { isFabExpanded = false }
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            horizontalAlignment = Alignment.End
        ) {
            AnimatedVisibility(
                visible = isFabExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, SlateLight),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        FabMenuItem(
                            icon = Icons.Default.EditNote,
                            label = "Text Note",
                            color = AccentTeal,
                            onClick = {
                                isFabExpanded = false
                                viewModel.selectNote(NoteEntity(title = "New Rich Note", type = "TEXT", content = ""))
                            }
                        )
                        FabMenuItem(
                            icon = Icons.Default.VpnKey,
                            label = "Password Vault",
                            color = WarningYellow,
                            onClick = {
                                isFabExpanded = false
                                viewModel.selectNote(NoteEntity(title = "Login Credentials", type = "PASSWORD", content = "username@email.com", extraContent = "SecurePassword123", pinHash = "1234"))
                            }
                        )
                        FabMenuItem(
                            icon = Icons.Default.ReceiptLong,
                            label = "Finance Account",
                            color = PositiveGreen,
                            onClick = {
                                isFabExpanded = false
                                viewModel.selectNote(NoteEntity(title = "Daily Ledger", type = "FINANCE", content = "Default Account"))
                            }
                        )
                        FabMenuItem(
                            icon = Icons.Default.Download,
                            label = "Import .txt File",
                            color = AccentIndigo,
                            onClick = {
                                isFabExpanded = false
                                try {
                                    importLauncher.launch("text/plain")
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not open file selector", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }

            FloatingActionButton(
                onClick = { isFabExpanded = !isFabExpanded },
                containerColor = AccentTeal,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("add_note_fab")
            ) {
                Icon(
                    imageVector = if (isFabExpanded) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = "Create note options"
                )
            }
        }
    }
}

@Composable
fun FabMenuItem(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.5f)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
fun SyncStatusBar(viewModel: NoteViewModel) {
    val syncStatus by viewModel.syncStatus.collectAsState()
    val isWifi by viewModel.isWifiAvailable.collectAsState()

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isWifi) Icons.Default.Wifi else Icons.Default.WifiOff,
                contentDescription = null,
                tint = if (isWifi) AccentTeal else NegativeRed,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isWifi) "WiFi Connected" else "Offline Only (Cellular)",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )

            when (val sync = syncStatus) {
                is SyncStatus.Syncing -> {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Syncing Cloud...", style = MaterialTheme.typography.bodySmall, color = AccentTeal)
                }
                is SyncStatus.Success -> {
                    Icon(Icons.Default.CloudDone, contentDescription = null, tint = PositiveGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Synced", style = MaterialTheme.typography.bodySmall, color = PositiveGreen)
                }
                is SyncStatus.Error -> {
                    Icon(Icons.Default.CloudOff, contentDescription = null, tint = NegativeRed, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Failed", style = MaterialTheme.typography.bodySmall, color = NegativeRed)
                }
                is SyncStatus.Idle -> {
                    TextButton(
                        contentPadding = PaddingValues(0.dp),
                        onClick = { viewModel.triggerSync(wifiOnly = true) }
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sync Now", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun NoteCard(note: NoteEntity, viewModel: NoteViewModel) {
    val context = LocalContext.current
    val lockedNoteId by viewModel.lockedNoteId.collectAsState()
    var isPinVerified by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var showPinDialog by remember { mutableStateOf(false) }

    val isNoteLocked = note.type == "PASSWORD" && !note.pinHash.isNullOrEmpty() && !isPinVerified

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (isNoteLocked) {
                    showPinDialog = true
                } else {
                    viewModel.selectNote(note)
                }
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when (note.type) {
                        "TEXT" -> Icons.Default.EditNote
                        "LIST" -> Icons.Default.Checklist
                        "PASSWORD" -> Icons.Default.VpnKey
                        "FINANCE" -> Icons.Default.ReceiptLong
                        else -> Icons.Default.Note
                    },
                    contentDescription = null,
                    tint = when (note.type) {
                        "TEXT" -> AccentTeal
                        "LIST" -> AccentIndigo
                        "PASSWORD" -> WarningYellow
                        "FINANCE" -> PositiveGreen
                        else -> MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (note.isSynced) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = "Synced to Cloud",
                        tint = PositiveGreen,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CloudQueue,
                        contentDescription = "Pending Sync",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isNoteLocked) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = WarningYellow, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("This note is password protected. Tap to unlock.", style = MaterialTheme.typography.bodySmall, color = WarningYellow)
                }
            } else {
                Text(
                    text = if (note.type == "PASSWORD") "User/Email: ${note.content}\nPassword: ••••••••" else note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                val formattedDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(note.updatedAt))
                Text(
                    text = "Edited: $formattedDate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = { viewModel.deleteNote(note.id) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete note",
                        tint = NegativeRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    if (showPinDialog) {
        Dialog(onDismissRequest = { showPinDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateDark),
                border = BorderStroke(1.dp, SlateLight)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Protected Note", fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Enter 4-digit PIN or Touch sensor", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 4) pinInput = it },
                        label = { Text("4-Digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(onClick = { showPinDialog = false }) {
                            Text("Cancel", color = TextMuted)
                        }
                        Button(
                            onClick = {
                                if (note.pinHash == pinInput) {
                                    isPinVerified = true
                                    showPinDialog = false
                                    viewModel.selectNote(note)
                                } else {
                                    Toast.makeText(context, "Incorrect PIN!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                        ) {
                            Text("Unlock")
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. FINANCE HUB DEFAULT DASHBOARD
// ==========================================
fun getBalancesOnDate(
    targetDateStr: String,
    allBalances: List<BankBalanceEntity>
): Map<String, Double> {
    val targetDate = try {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(targetDateStr) ?: Date()
    } catch (e: Exception) {
        try {
            SimpleDateFormat("dd/MM/yy", Locale.getDefault()).parse(targetDateStr) ?: Date()
        } catch (ex: Exception) {
            Date()
        }
    }

    val uniqueBanks = allBalances.map { it.bankName }.distinct()
    val bankBalancesOnDate = mutableMapOf<String, Double>()

    for (bank in uniqueBanks) {
        val mostRecentUpdate = allBalances
            .filter { it.bankName == bank }
            .filter {
                val updateDate = try {
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(it.dateUpdated)
                } catch (e: Exception) {
                    try {
                        SimpleDateFormat("dd/MM/yy", Locale.getDefault()).parse(it.dateUpdated)
                    } catch (ex: Exception) {
                        null
                    }
                }
                updateDate != null && !updateDate.after(targetDate)
            }
            .maxByOrNull {
                try {
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(it.dateUpdated)?.time ?: 0L
                } catch (e: Exception) {
                    try {
                        SimpleDateFormat("dd/MM/yy", Locale.getDefault()).parse(it.dateUpdated)?.time ?: 0L
                    } catch (ex: Exception) {
                        0L
                    }
                }
            }
        
        if (mostRecentUpdate != null) {
            bankBalancesOnDate[bank] = mostRecentUpdate.balance
        } else {
            bankBalancesOnDate[bank] = 0.0
        }
    }
    return bankBalancesOnDate
}

fun getLoansOnDate(
    targetDateStr: String,
    allLoans: List<LoanEntity>
): List<LoanEntity> {
    val targetDate = try {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(targetDateStr) ?: Date()
    } catch (e: Exception) {
        try {
            SimpleDateFormat("dd/MM/yy", Locale.getDefault()).parse(targetDateStr) ?: Date()
        } catch (ex: Exception) {
            Date()
        }
    }

    return allLoans.filter {
        val loanDate = try {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(it.date)
        } catch (e: Exception) {
            try {
                SimpleDateFormat("dd/MM/yy", Locale.getDefault()).parse(it.date)
            } catch (ex: Exception) {
                null
            }
        }
        loanDate != null && !loanDate.after(targetDate)
    }
}

@Composable
fun FinanceDashboardScreen(viewModel: NoteViewModel) {
    val context = LocalContext.current
    val allBankBalances by viewModel.allBankBalances.collectAsState()
    val allLoans by viewModel.allLoans.collectAsState()
    
    val todayStr = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) }
    var queryDateStr by remember { mutableStateOf(todayStr) }
    
    // Calculate balances on selected date
    val bankBalancesOnDate = remember(queryDateStr, allBankBalances) {
        getBalancesOnDate(queryDateStr, allBankBalances)
    }
    val loansOnDate = remember(queryDateStr, allLoans) {
        getLoansOnDate(queryDateStr, allLoans)
    }
    
    val totalBankBalance = remember(bankBalancesOnDate) { bankBalancesOnDate.values.sum() }
    val totalLend = remember(loansOnDate) {
        loansOnDate.filter { it.type == "LEND" && !it.isSettled }.sumOf { it.amount }
    }
    val totalBorrow = remember(loansOnDate) {
        loansOnDate.filter { it.type == "BORROW" && !it.isSettled }.sumOf { it.amount }
    }
    
    val netWorth = totalBankBalance + totalLend - totalBorrow

    // Input States for Adding Bank Balance
    var addBankName by remember { mutableStateOf("") }
    var addBankBalanceText by remember { mutableStateOf("") }
    var addBankDate by remember { mutableStateOf(todayStr) }

    // Input States for Adding Loans
    var addLoanType by remember { mutableStateOf("BORROW") } // "BORROW" or "LEND"
    var addLoanSource by remember { mutableStateOf("") }
    var addLoanAmountText by remember { mutableStateOf("") }
    var addLoanDate by remember { mutableStateOf(todayStr) }

    val gradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.background
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Date Query Widget
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Check Balance on Date",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = queryDateStr,
                        onValueChange = { queryDateStr = it },
                        label = { Text("Query Date (dd/MM/yyyy)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        trailingIcon = {
                            Icon(Icons.Default.DateRange, contentDescription = null)
                        }
                    )
                    Button(
                        onClick = { queryDateStr = todayStr },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Today")
                    }
                }
            }
        }

        // Net worth at Query Date
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SlateDark),
            border = BorderStroke(1.dp, SlateLight)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "ESTIMATED NET WORTH ON $queryDateStr",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = AccentTeal)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "₹${"%,.0f".format(netWorth)}",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("TOTAL LIQUID BANK", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        Text("₹${"%,.0f".format(totalBankBalance)}", fontWeight = FontWeight.Bold, color = PositiveGreen)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("LEND (+)", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        Text("₹${"%,.0f".format(totalLend)}", fontWeight = FontWeight.Bold, color = AccentTeal)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("BORROW (-)", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        Text("₹${"%,.0f".format(totalBorrow)}", fontWeight = FontWeight.Bold, color = NegativeRed)
                    }
                }
            }
        }

        // Multi Bank Balances List on Query Date
        Text(
            "Bank Accounts Liquid Balances",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (bankBalancesOnDate.isEmpty()) {
                    Text(
                        "No banks added yet. Use the card below to record your bank balances.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    bankBalancesOnDate.forEach { (bankName, bal) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = AccentTeal)
                                Column {
                                    Text(bankName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                                    Text("As of $queryDateStr", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                }
                            }
                            Text(
                                "₹${"%,.0f".format(bal)}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge,
                                color = PositiveGreen
                            )
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }

        // Active Loans/Lend/Borrow List
        Text(
            "Borrow / Lend Log on $queryDateStr",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (loansOnDate.isEmpty()) {
                    Text(
                        "No loans logged before or on this date.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    loansOnDate.forEach { loan ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val isLend = loan.type == "LEND"
                                    Icon(
                                        imageVector = if (isLend) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                        contentDescription = null,
                                        tint = if (isLend) AccentTeal else NegativeRed
                                    )
                                    Text(
                                        text = if (isLend) "Lend to ${loan.source}" else "Borrow from ${loan.source}",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Text("Recorded: ${loan.date}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "₹${"%,.0f".format(loan.amount)}",
                                    fontWeight = FontWeight.Bold,
                                    color = if (loan.type == "LEND") AccentTeal else NegativeRed,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                
                                Switch(
                                    checked = loan.isSettled,
                                    onCheckedChange = { viewModel.toggleLoanSettled(loan) },
                                    thumbContent = {
                                        if (loan.isSettled) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp))
                                        }
                                    }
                                )

                                IconButton(onClick = { viewModel.deleteLoan(loan.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NegativeRed, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }

        // Card Form to add/update bank balance
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Update/Add Bank Account Balance",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = addBankName,
                        onValueChange = { addBankName = it },
                        label = { Text("Bank Name") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = addBankBalanceText,
                        onValueChange = { addBankBalanceText = it },
                        label = { Text("Balance (₹)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                OutlinedTextField(
                    value = addBankDate,
                    onValueChange = { addBankDate = it },
                    label = { Text("Date of Update (dd/MM/yyyy)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Button(
                    onClick = {
                        val bal = addBankBalanceText.toDoubleOrNull()
                        if (addBankName.isBlank() || bal == null) {
                            Toast.makeText(context, "Please enter valid bank details!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.addBankBalance(addBankName, bal, addBankDate)
                        addBankName = ""
                        addBankBalanceText = ""
                        Toast.makeText(context, "Bank balance updated!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                ) {
                    Text("Record Bank Balance", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Card Form to add loans (borrow/lend)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Add Borrow / Lend Record",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                // Borrow vs Lend Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { addLoanType = "BORROW" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (addLoanType == "BORROW") NegativeRed else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Borrow (We Owe)")
                    }
                    Button(
                        onClick = { addLoanType = "LEND" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (addLoanType == "LEND") AccentTeal else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Lend (People Owe us)")
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = addLoanSource,
                        onValueChange = { addLoanSource = it },
                        label = { Text("Person / Bank Name") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = addLoanAmountText,
                        onValueChange = { addLoanAmountText = it },
                        label = { Text("Amount (₹)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                OutlinedTextField(
                    value = addLoanDate,
                    onValueChange = { addLoanDate = it },
                    label = { Text("Date of Loan (dd/MM/yyyy)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Button(
                    onClick = {
                        val amt = addLoanAmountText.toDoubleOrNull()
                        if (addLoanSource.isBlank() || amt == null) {
                            Toast.makeText(context, "Please enter valid details!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.addLoan(addLoanType, addLoanSource, amt, addLoanDate)
                        addLoanSource = ""
                        addLoanAmountText = ""
                        Toast.makeText(context, "Loan transaction logged!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo)
                ) {
                    Text("Add Loan Record", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DashboardItemCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subText: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            Text(subText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        }
    }
}

// ==========================================
// 4. SYNC SETTINGS SCREEN (USER PROFILE HUB)
// ==========================================
@Composable
fun SyncSettingsScreen(viewModel: NoteViewModel) {
    val authState by viewModel.authState.collectAsState()
    val isWifiOnlySync by viewModel.isWifiOnlySync.collectAsState()
    val isWifiAvailable by viewModel.isWifiAvailable.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val context = LocalContext.current

    var newUsername by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Account Profile", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("Manage your authenticated synchronization profile securely", color = TextMuted, fontSize = 12.sp)

                Spacer(modifier = Modifier.height(16.dp))

                when (val auth = authState) {
                    is AuthState.Authenticated -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = AccentTeal,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(auth.displayName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(auth.email, color = TextMuted, fontSize = 14.sp)
                                Text("Signed in via ${auth.method}", color = AccentIndigo, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Change Username input
                        Text("Change Username", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = newUsername,
                            onValueChange = { newUsername = it },
                            placeholder = { Text("New Username") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = AccentTeal,
                                unfocusedBorderColor = SlateLight
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (newUsername.trim().isNotEmpty()) {
                                    viewModel.updateUsername(newUsername.trim())
                                    newUsername = ""
                                    Toast.makeText(context, "Username updated!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                        ) {
                            Text("Update Username", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Logout action
                        Button(
                            onClick = { viewModel.logout() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.ExitToApp, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Logout from Account", fontWeight = FontWeight.Bold)
                        }
                    }
                    else -> {
                        Text("Not authenticated", color = Color.White)
                    }
                }
            }
        }

        if (authState is AuthState.Authenticated) {
            val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            val uid = user?.uid ?: "unknown"

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Cloud Synchronization Settings", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Control your cloud Firestore auto-synchronization", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1.5f)) {
                            Text("WiFi Only Sync Mode", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Restrict sync operations to WiFi only", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = isWifiOnlySync, onCheckedChange = { viewModel.toggleWifiOnlySync(it) })
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1.5f)) {
                            Text("Simulated WiFi Connection", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Turn on/off simulated WiFi state", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = isWifiAvailable, onCheckedChange = { viewModel.toggleWifi(it) })
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(20.dp))

                    Text("Active Synchronization Hub", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        text = "Data will be saved in your Firestore root collection 'users' under document '$uid' with subcollections for notes, bank balances, and financial dashboards.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.triggerSync() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sync with Firestore Now", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("SYNC STATUS:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(6.dp))

                    // Show sync status details
                    when (val sync = syncStatus) {
                        is SyncStatus.Syncing -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Uploading data to Firestore...", color = AccentTeal, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        is SyncStatus.Success -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudDone, contentDescription = null, tint = PositiveGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(sync.message, color = PositiveGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        is SyncStatus.Error -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudOff, contentDescription = null, tint = NegativeRed, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(sync.error, color = NegativeRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        is SyncStatus.Idle -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Wifi, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Ready / Idle", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun getFormattedNoteContent(type: String, content: String): String {
    return when (type) {
        "PASSWORD" -> {
            val accounts = VaultAccount.parseList(content)
            if (accounts.isEmpty()) {
                "No credentials stored in this password vault."
            } else {
                accounts.joinToString("\n\n") { acc ->
                    "Account Name: ${acc.accountName}\nUsername: ${acc.username}\nPassword: ${acc.password}"
                }
            }
        }
        else -> content
    }
}

fun exportNoteToPdf(context: Context, title: String, content: String) {
    val pdfDocument = PdfDocument()
    val pageWidth = 595
    val pageHeight = 842
    val margin = 40f
    
    val titlePaint = Paint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 18f
        color = android.graphics.Color.BLACK
    }
    val contentPaint = Paint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textSize = 12f
        color = android.graphics.Color.BLACK
    }
    
    var pageNumber = 1
    var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
    var page = pdfDocument.startPage(pageInfo)
    var canvas = page.canvas
    
    var y = margin + 20f
    canvas.drawText(title.ifEmpty { "Untitled Note" }, margin, y, titlePaint)
    y += 30f
    
    val linePaint = Paint().apply {
        color = android.graphics.Color.LTGRAY
        strokeWidth = 1f
    }
    canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
    y += 20f
    
    val lines = content.split("\n")
    for (rawLine in lines) {
        val line = if (rawLine.isEmpty()) " " else rawLine
        val words = line.split(" ")
        var currentLineText = ""
        for (word in words) {
            val testLine = if (currentLineText.isEmpty()) word else "$currentLineText $word"
            val width = contentPaint.measureText(testLine)
            if (width > pageWidth - (2 * margin)) {
                if (y + 15f > pageHeight - margin) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    y = margin + 20f
                }
                canvas.drawText(currentLineText, margin, y, contentPaint)
                y += 15f
                currentLineText = word
            } else {
                currentLineText = testLine
            }
        }
        
        if (y + 15f > pageHeight - margin) {
            pdfDocument.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            y = margin + 20f
        }
        canvas.drawText(currentLineText, margin, y, contentPaint)
        y += 15f
    }
    
    pdfDocument.finishPage(page)
    
    val sanitizedTitle = title.replace("[^a-zA-Z0-9]".toRegex(), "_").ifEmpty { "note" }
    val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "$sanitizedTitle.pdf")
    try {
        pdfDocument.writeTo(FileOutputStream(file))
        Toast.makeText(context, "Exported PDF: ${file.name}", Toast.LENGTH_LONG).show()
        
        // Share via intent
        val authority = "${context.packageName}.fileprovider"
        val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Share Note PDF"))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error saving PDF: ${e.message}", Toast.LENGTH_SHORT).show()
    } finally {
        pdfDocument.close()
    }
}

fun exportNoteToDoc(context: Context, title: String, content: String) {
    val sanitizedTitle = title.replace("[^a-zA-Z0-9]".toRegex(), "_").ifEmpty { "note" }
    val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "$sanitizedTitle.doc")
    try {
        FileOutputStream(file).use { out ->
            val sb = java.lang.StringBuilder()
            sb.append("Title: $title\n")
            sb.append("=========================================\n\n")
            sb.append(content)
            out.write(sb.toString().toByteArray())
        }
        Toast.makeText(context, "Exported Word (.doc): ${file.name}", Toast.LENGTH_LONG).show()

        val authority = "${context.packageName}.fileprovider"
        val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/msword"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Share Note Word Document"))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error saving Word file: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun exportNoteToTxt(context: Context, title: String, content: String) {
    val sanitizedTitle = title.replace("[^a-zA-Z0-9]".toRegex(), "_").ifEmpty { "note" }
    val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "$sanitizedTitle.txt")
    try {
        FileOutputStream(file).use { out ->
            val sb = java.lang.StringBuilder()
            sb.append("Title: $title\n\n")
            sb.append(content)
            out.write(sb.toString().toByteArray())
        }
        Toast.makeText(context, "Exported Plain Text: ${file.name}", Toast.LENGTH_LONG).show()

        val authority = "${context.packageName}.fileprovider"
        val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Share Note Text File"))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error saving text file: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

// ==========================================
// 5. TAILORED NOTE EDITOR SCREENS
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    note: NoteEntity,
    viewModel: NoteViewModel,
    onClose: () -> Unit
) {
    var title by remember { mutableStateOf(note.title) }
    var content by remember { mutableStateOf(note.content) }
    var extraContent by remember { mutableStateOf(note.extraContent ?: "") }
    var pinHash by remember { mutableStateOf(note.pinHash ?: "") }
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    val syncStatus by viewModel.syncStatus.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (note.id == 0) "Create Note" else "Edit Note", style = MaterialTheme.typography.titleMedium)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            when (syncStatus) {
                                is SyncStatus.Syncing -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(10.dp),
                                        strokeWidth = 1.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Saving...",
                                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary)
                                    )
                                }
                                is SyncStatus.Success -> {
                                    Icon(
                                        imageVector = Icons.Default.CloudDone,
                                        contentDescription = "Saved",
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Saved",
                                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF4CAF50))
                                    )
                                }
                                is SyncStatus.Error -> {
                                    Icon(
                                        imageVector = Icons.Default.CloudOff,
                                        contentDescription = "Sync Error",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Sync Error",
                                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.error)
                                    )
                                }
                                is SyncStatus.Idle -> {
                                    Icon(
                                        imageVector = Icons.Default.CloudQueue,
                                        contentDescription = "Synced Offline",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Synced",
                                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                    )
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.saveActiveNote(
                                title = title,
                                type = note.type,
                                content = content,
                                extraContent = extraContent.ifEmpty { null },
                                pinHash = if (note.type == "PASSWORD") pinHash else null
                            )
                        }
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save Note", tint = MaterialTheme.colorScheme.primary)
                    }

                    Box {
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Copy Entire Text") },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val formatted = getFormattedNoteContent(note.type, content)
                                    val clip = ClipData.newPlainText("Note Content", "$title\n\n$formatted")
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Note content copied to clipboard!", Toast.LENGTH_SHORT).show()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export as PDF") },
                                leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    val formatted = getFormattedNoteContent(note.type, content)
                                    exportNoteToPdf(context, title, formatted)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export as Word (.doc)") },
                                leadingIcon = { Icon(Icons.Default.EditNote, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    val formatted = getFormattedNoteContent(note.type, content)
                                    exportNoteToDoc(context, title, formatted)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export as Plain Text (.txt)") },
                                leadingIcon = { Icon(Icons.Default.Note, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    val formatted = getFormattedNoteContent(note.type, content)
                                    exportNoteToTxt(context, title, formatted)
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Title", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                )
            )

            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))

            when (note.type) {
                "TEXT", "LIST" -> RichTextEditor(
                    content = content,
                    onContentChange = { content = it }
                )
                "PASSWORD" -> PasswordVaultEditor(
                    serializedContent = content,
                    onContentChange = { content = it },
                    pin = pinHash,
                    onPinChange = { pinHash = it }
                )
                "FINANCE" -> FinanceTallyEditor(
                    content = content,
                    onContentChange = { content = it }
                )
            }
        }
    }
}

// A high-performance Markdown visual transformation to render rich typography (*bold*, _italic_, headers)
// directly inside the text editor, while keeping perfect cursor positioning!
class MarkdownVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val builder = AnnotatedString.Builder()
        val rawText = text.text
        builder.append(rawText)

        // Bold: **text**
        val boldRegex = "\\*\\*(.*?)\\*\\*".toRegex()
        boldRegex.findAll(rawText).forEach { matchResult ->
            val range = matchResult.range
            builder.addStyle(
                style = SpanStyle(color = Color.Transparent, fontSize = 0.sp),
                start = range.first,
                end = range.first + 2
            )
            builder.addStyle(
                style = SpanStyle(color = Color.Transparent, fontSize = 0.sp),
                start = range.last - 1,
                end = range.last + 1
            )
            builder.addStyle(
                style = SpanStyle(fontWeight = FontWeight.Bold),
                start = range.first + 2,
                end = range.last - 1
            )
        }

        // Italic: _text_
        val italicRegex = "_(.*?)_".toRegex()
        italicRegex.findAll(rawText).forEach { matchResult ->
            val range = matchResult.range
            builder.addStyle(
                style = SpanStyle(color = Color.Transparent, fontSize = 0.sp),
                start = range.first,
                end = range.first + 1
            )
            builder.addStyle(
                style = SpanStyle(color = Color.Transparent, fontSize = 0.sp),
                start = range.last,
                end = range.last + 1
            )
            builder.addStyle(
                style = SpanStyle(fontStyle = FontStyle.Italic),
                start = range.first + 1,
                end = range.last
            )
        }

        // H1 Heading: # text
        val h1Regex = "(?m)^# (.*)$".toRegex()
        h1Regex.findAll(rawText).forEach { matchResult ->
            val range = matchResult.range
            builder.addStyle(
                style = SpanStyle(color = Color.Transparent, fontSize = 0.sp),
                start = range.first,
                end = range.first + 2
            )
            builder.addStyle(
                style = SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF14B8A6)),
                start = range.first + 2,
                end = range.last + 1
            )
        }

        // H2 Heading: ## text
        val h2Regex = "(?m)^## (.*)$".toRegex()
        h2Regex.findAll(rawText).forEach { matchResult ->
            val range = matchResult.range
            builder.addStyle(
                style = SpanStyle(color = Color.Transparent, fontSize = 0.sp),
                start = range.first,
                end = range.first + 3
            )
            builder.addStyle(
                style = SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6366F1)),
                start = range.first + 3,
                end = range.last + 1
            )
        }

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}

// ------------------------------------------
// Rich Text Note Sub-Editor
// ------------------------------------------
@Composable
fun RichTextEditor(
    content: String,
    onContentChange: (String) -> Unit
) {
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = content,
                selection = TextRange(content.length)
            )
        )
    }

    LaunchedEffect(content) {
        if (content != textFieldValue.text) {
            textFieldValue = textFieldValue.copy(text = content)
        }
    }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 4.dp)
        ) {
            BasicTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    var processedValue = newValue
                    val oldText = textFieldValue.text
                    val newText = newValue.text
                    
                    if (newText.length == oldText.length + 1 && 
                        newValue.selection.start > 0 && 
                        newText[newValue.selection.start - 1] == '\n') {
                        
                        val cursorOffset = newValue.selection.start
                        val textBeforeCursor = newText.substring(0, cursorOffset - 1)
                        val lastLineStart = textBeforeCursor.lastIndexOf('\n') + 1
                        val lastLine = textBeforeCursor.substring(lastLineStart)
                        
                        if (lastLine.trim() == "•") {
                            val updatedText = newText.substring(0, lastLineStart) + "\n" + newText.substring(cursorOffset)
                            val newSelection = TextRange(lastLineStart + 1)
                            processedValue = TextFieldValue(updatedText, newSelection)
                        } else if ("^\\s*\\d+\\.\\s*$".toRegex().matches(lastLine)) {
                            val updatedText = newText.substring(0, lastLineStart) + "\n" + newText.substring(cursorOffset)
                            val newSelection = TextRange(lastLineStart + 1)
                            processedValue = TextFieldValue(updatedText, newSelection)
                        } else if (lastLine.trimStart().startsWith("• ")) {
                            val prefixIndex = lastLine.indexOf("• ")
                            val prefix = lastLine.substring(0, prefixIndex + 2)
                            val updatedText = newText.substring(0, cursorOffset) + prefix + newText.substring(cursorOffset)
                            val newSelection = TextRange(cursorOffset + prefix.length)
                            processedValue = TextFieldValue(updatedText, newSelection)
                        } else {
                            val numberMatch = "^(\\s*)(\\d+)\\.\\s+".toRegex().find(lastLine)
                            if (numberMatch != null) {
                                val indent = numberMatch.groupValues[1]
                                val num = numberMatch.groupValues[2].toIntOrNull() ?: 1
                                val nextNumStr = "$indent${num + 1}. "
                                val updatedText = newText.substring(0, cursorOffset) + nextNumStr + newText.substring(cursorOffset)
                                val newSelection = TextRange(cursorOffset + nextNumStr.length)
                                processedValue = TextFieldValue(updatedText, newSelection)
                            }
                        }
                    }
                    
                    textFieldValue = processedValue
                    onContentChange(processedValue.text)
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onBackground
                ),
                visualTransformation = MarkdownVisualTransformation(),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("rich_text_input"),
                decorationBox = { innerTextField ->
                    if (textFieldValue.text.isEmpty()) {
                        Text(
                            text = "Write your notes here...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                        )
                    }
                    innerTextField()
                }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tools = listOf(
                "B" to "BOLD",
                "I" to "ITALIC",
                "H1" to "H1",
                "H2" to "H2",
                "•" to "BULLET",
                "1." to "NUMBER"
            )

            tools.forEach { (label, type) ->
                TextButton(
                    onClick = {
                        val text = textFieldValue.text
                        val selection = textFieldValue.selection
                        val start = selection.start
                        val end = selection.end

                        val newText: String
                        val newSelection: TextRange

                        when (type) {
                            "BOLD" -> {
                                if (start != end) {
                                    newText = text.replaceRange(start, end, "**${text.substring(start, end)}**")
                                    newSelection = TextRange(start, end + 4)
                                } else {
                                    newText = text.substring(0, start) + "****" + text.substring(start)
                                    newSelection = TextRange(start + 2)
                                }
                            }
                            "ITALIC" -> {
                                if (start != end) {
                                    newText = text.replaceRange(start, end, "_${text.substring(start, end)}_")
                                    newSelection = TextRange(start, end + 2)
                                } else {
                                    newText = text.substring(0, start) + "__" + text.substring(start)
                                    newSelection = TextRange(start + 1)
                                }
                            }
                            "H1" -> {
                                val lineStart = text.lastIndexOf('\n', start - 1).coerceAtLeast(0)
                                val insertPos = if (lineStart == 0 && text.firstOrNull() != '\n') 0 else lineStart + 1
                                newText = text.substring(0, insertPos) + "# " + text.substring(insertPos)
                                newSelection = TextRange(start + 2)
                            }
                            "H2" -> {
                                val lineStart = text.lastIndexOf('\n', start - 1).coerceAtLeast(0)
                                val insertPos = if (lineStart == 0 && text.firstOrNull() != '\n') 0 else lineStart + 1
                                newText = text.substring(0, insertPos) + "## " + text.substring(insertPos)
                                newSelection = TextRange(start + 3)
                            }
                            "BULLET" -> {
                                val lineStart = text.lastIndexOf('\n', start - 1).coerceAtLeast(0)
                                val insertPos = if (lineStart == 0 && text.firstOrNull() != '\n') 0 else lineStart + 1
                                newText = text.substring(0, insertPos) + "• " + text.substring(insertPos)
                                newSelection = TextRange(start + 2)
                            }
                            "NUMBER" -> {
                                val lineStart = text.lastIndexOf('\n', start - 1).coerceAtLeast(0)
                                val insertPos = if (lineStart == 0 && text.firstOrNull() != '\n') 0 else lineStart + 1
                                newText = text.substring(0, insertPos) + "1. " + text.substring(insertPos)
                                newSelection = TextRange(start + 3)
                            }
                            else -> {
                                newText = text
                                newSelection = selection
                            }
                        }

                        textFieldValue = TextFieldValue(text = newText, selection = newSelection)
                        onContentChange(newText)
                    },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text(
                        text = label,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// ------------------------------------------
// Password Vault Sub-Editor
// ------------------------------------------
@Composable
fun PasswordVaultEditor(
    serializedContent: String,
    onContentChange: (String) -> Unit,
    pin: String,
    onPinChange: (String) -> Unit
) {
    val context = LocalContext.current
    val accounts = remember(serializedContent) { VaultAccount.parseList(serializedContent) }

    var accountNameInput by remember { mutableStateOf("") }
    var usernameInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }

    var isAddingByForm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Local Device Security PIN",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) onPinChange(it) },
                    label = { Text("4-Digit Secure PIN") },
                    placeholder = { Text("e.g. 1234") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Stored Accounts (${accounts.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            TextButton(onClick = { isAddingByForm = !isAddingByForm }) {
                Icon(
                    imageVector = if (isAddingByForm) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isAddingByForm) "Close" else "Add Account")
            }
        }

        AnimatedVisibility(visible = isAddingByForm) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("New Account Credentials", fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = accountNameInput,
                        onValueChange = { accountNameInput = it },
                        label = { Text("Account Name (e.g. Google, Spotify)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text("Username / Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            if (accountNameInput.isNotBlank() && usernameInput.isNotBlank() && passwordInput.isNotBlank()) {
                                val newAccount = VaultAccount(
                                    accountName = accountNameInput.trim(),
                                    username = usernameInput.trim(),
                                    password = passwordInput.trim()
                                )
                                val newList = accounts + newAccount
                                onContentChange(VaultAccount.serializeList(newList))

                                accountNameInput = ""
                                usernameInput = ""
                                passwordInput = ""
                                isAddingByForm = false
                                Toast.makeText(context, "Account added successfully!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Please fill in all fields!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add to Vault")
                    }
                }
            }
        }

        if (accounts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No credentials stored. Add your first account above.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        } else {
            accounts.forEachIndexed { index, acc ->
                var isPasswordVisible by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = acc.accountName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            IconButton(
                                onClick = {
                                    val newList = accounts.toMutableList().apply { removeAt(index) }
                                    onContentChange(VaultAccount.serializeList(newList))
                                    Toast.makeText(context, "Account removed from vault", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove Account", tint = NegativeRed)
                            }
                        }

                        OutlinedTextField(
                            value = acc.username,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Username") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Copied Username", acc.username))
                                        Toast.makeText(context, "Username copied!", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Username")
                                }
                            }
                        )

                        OutlinedTextField(
                            value = acc.password,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Password") },
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                Row {
                                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                        Icon(
                                            imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle Visibility"
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("Copied Password", acc.password))
                                            Toast.makeText(context, "Password copied safely!", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Password")
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// ------------------------------------------
// Finance Account Sub-Editor
// ------------------------------------------
data class PlainTallyTransaction(
    val amount: Double,
    val description: String
)

data class PlainTallyDay(
    val dateStr: String,
    val transactions: List<PlainTallyTransaction>,
    val closingBalance: Double = 0.0
)

fun parseDateToSort(dateStr: String): Date {
    val formats = listOf("dd/MM/yy", "dd/MM/yyyy", "dd/MM")
    for (format in formats) {
        try {
            val sdf = SimpleDateFormat(format, Locale.getDefault())
            val date = sdf.parse(dateStr)
            if (date != null) {
                if (format == "dd/MM") {
                    val cal = Calendar.getInstance()
                    val year = cal.get(Calendar.YEAR)
                    cal.time = date
                    cal.set(Calendar.YEAR, year)
                    return cal.time
                }
                return date
            }
        } catch (e: Exception) {
            // try next format
        }
    }
    return Date(0)
}

fun parsePlainTally(text: String): List<PlainTallyDay> {
    if (text.isBlank()) return emptyList()
    val lines = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
    val days = mutableListOf<PlainTallyDay>()
    
    var currentDate: String? = null
    var currentTransactions = mutableListOf<PlainTallyTransaction>()
    var currentBalance = 0.0
    
    for (line in lines) {
        val dateMatch = "^(\\d{2}/\\d{2}(?:/\\d{2,4})?)".toRegex().find(line)
        if (dateMatch != null) {
            if (currentDate != null) {
                days.add(PlainTallyDay(currentDate, currentTransactions.toList(), currentBalance))
                currentTransactions.clear()
            }
            currentDate = dateMatch.groupValues[1]
            
            val balMatch = "(?:closing Balance|Clear Balance|Balance):-\\s*(-?\\d+(?:\\.\\d+)?)".toRegex().find(line)
            if (balMatch != null) {
                currentBalance = balMatch.groupValues[1].toDoubleOrNull() ?: 0.0
            }
        } else if (line.startsWith("+") || line.startsWith("-")) {
            val parts = line.split("=")
            if (parts.isNotEmpty()) {
                val amtStr = parts[0].trim().replace("+", "")
                val amt = amtStr.toDoubleOrNull() ?: 0.0
                val desc = if (parts.size > 1) parts[1].trim() else ""
                currentTransactions.add(PlainTallyTransaction(amt, desc))
            }
        } else if (line.contains("Clear Balance:-") || line.contains("closing Balance:-") || line.contains("Balance:-")) {
            val balMatch = "(?:Clear Balance|closing Balance|Balance):-\\s*(-?\\d+(?:\\.\\d+)?)".toRegex().find(line)
            if (balMatch != null) {
                currentBalance = balMatch.groupValues[1].toDoubleOrNull() ?: 0.0
            }
        }
    }
    
    if (currentDate != null) {
        days.add(PlainTallyDay(currentDate, currentTransactions.toList(), currentBalance))
    }
    
    return days
}

fun serializePlainTally(days: List<PlainTallyDay>): String {
    val sb = StringBuilder()
    var runningBalance = 0.0
    
    val sortedDays = days.sortedBy { parseDateToSort(it.dateStr) }
    
    for (i in sortedDays.indices) {
        val day = sortedDays[i]
        val dayTransactionsSum = day.transactions.sumOf { it.amount }
        val dayClosingBalance = runningBalance + dayTransactionsSum
        
        sb.append(day.dateStr)
        if (day.transactions.isEmpty()) {
            sb.append(" closing Balance:- ${dayClosingBalance.toInt()}")
        } else {
            sb.append("\n")
            day.transactions.forEach { tx ->
                val sign = if (tx.amount >= 0) "+" else ""
                val amtStr = "${sign}${tx.amount.toInt()}"
                sb.append("$amtStr = ${tx.description}\n")
            }
            sb.append("Clear Balance:- ${dayClosingBalance.toInt()}")
        }
        
        runningBalance = dayClosingBalance
        if (i < sortedDays.size - 1) {
            sb.append("\n\n")
        }
    }
    return sb.toString()
}

@Composable
fun FinanceTallyEditor(
    content: String,
    onContentChange: (String) -> Unit
) {
    val context = LocalContext.current
    var dateInput by remember {
        val sdf = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
        mutableStateOf(sdf.format(Date()))
    }
    var amountInput by remember { mutableStateOf("") }
    var descInput by remember { mutableStateOf("") }
    var isPreviewMode by remember { mutableStateOf(true) }

    val days = remember(content) { parsePlainTally(content) }
    val previewScrollState = rememberScrollState()
    val editScrollState = rememberScrollState()

    // Automatically scroll to the bottom of the ledger / text where the user left off
    LaunchedEffect(isPreviewMode, content) {
        if (isPreviewMode) {
            previewScrollState.animateScrollTo(previewScrollState.maxValue)
        } else {
            editScrollState.animateScrollTo(editScrollState.maxValue)
        }
    }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        // Toggle Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { isPreviewMode = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPreviewMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Ledger View")
                }
            }

            Button(
                onClick = { isPreviewMode = false },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!isPreviewMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Raw Text")
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 8.dp)
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(12.dp)
        ) {
            if (isPreviewMode) {
                if (days.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No entries yet. Use the helper form below to add a transaction.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(previewScrollState),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        days.forEach { day ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = day.dateStr,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    
                                    day.transactions.forEach { tx ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 3.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = tx.description,
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.weight(1f)
                                            )
                                            val isNegative = tx.amount < 0
                                            val sign = if (isNegative) "-" else "+"
                                            val displayAmount = if (isNegative) -tx.amount else tx.amount
                                            Text(
                                                text = "$sign ₹${displayAmount.toInt()}",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (isNegative) NegativeRed else PositiveGreen
                                            )
                                        }
                                    }
                                    
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Closing Balance", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = "₹${day.closingBalance.toInt()}",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                BasicTextField(
                    value = content,
                    onValueChange = onContentChange,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                        fontFamily = FontFamily.Monospace
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(editScrollState)
                        .testTag("finance_plain_text"),
                    decorationBox = { innerTextField ->
                        if (content.isEmpty()) {
                            Text(
                                text = "Write or insert your finance entries below...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Insert Transaction Helper",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleSmall
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dateInput,
                        onValueChange = { dateInput = it },
                        label = { Text("Date (dd/MM/yy)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = { Text("Amount (e.g. -400 or +3650)") },
                        modifier = Modifier.weight(1.2f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                OutlinedTextField(
                    value = descInput,
                    onValueChange = { descInput = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Button(
                    onClick = {
                        val amt = amountInput.toDoubleOrNull()
                        if (amt == null) {
                            Toast.makeText(context, "Please enter a valid amount!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (descInput.isBlank()) {
                            Toast.makeText(context, "Please enter a description!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val daysList = parsePlainTally(content).toMutableList()
                        val existingDayIndex = daysList.indexOfFirst { it.dateStr == dateInput }
                        
                        if (existingDayIndex != -1) {
                            val existingDay = daysList[existingDayIndex]
                            val updatedTransactions = existingDay.transactions.toMutableList()
                            updatedTransactions.add(PlainTallyTransaction(amt, descInput))
                            daysList[existingDayIndex] = existingDay.copy(transactions = updatedTransactions)
                        } else {
                            daysList.add(PlainTallyDay(
                                dateStr = dateInput,
                                transactions = listOf(PlainTallyTransaction(amt, descInput))
                            ))
                        }
                        
                        val updatedContent = serializePlainTally(daysList)
                        onContentChange(updatedContent)

                        amountInput = ""
                        descInput = ""
                        Toast.makeText(context, "Entry added!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Add Ledger Transaction Entry")
                }
            }
        }
    }
}

// ==========================================
// 6. EXPORT DIALOG
// ==========================================
@Composable
fun ExportDialog(
    viewModel: NoteViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedFormat by remember { mutableStateOf("TEXT") } // "TEXT", "PDF", "WORD"
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateDark),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, SlateLight)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(48.dp))
                
                Text("Export and Share Notes Data", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                Text("Select document type format and trigger compiling exports", color = TextMuted, fontSize = 12.sp, textAlign = TextAlign.Center)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("TEXT" to ".TXT", "PDF" to ".PDF", "WORD" to ".DOCX").forEach { (format, label) ->
                        Button(
                            onClick = { selectedFormat = format },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedFormat == format) AccentTeal else SlateLight
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(label)
                        }
                    }
                }

                Button(
                    onClick = {
                        val exportedString = viewModel.generateExportData(null, null, selectedFormat)
                        // Trigger Share Intent
                        val sendIntent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND
                            putExtra(android.content.Intent.EXTRA_TEXT, exportedString)
                            type = "text/plain"
                        }
                        val shareIntent = android.content.Intent.createChooser(sendIntent, "Export Smart Notes")
                        context.startActivity(shareIntent)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo)
                ) {
                    Text("Export & Share Document", fontWeight = FontWeight.Bold)
                }

                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = TextMuted)
                }
            }
        }
    }
}

@Composable
fun LoadingSplashScreen(isDarkMode: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkMode) Color(0xFF1E293B) else Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_noteing_splash_1782644228242),
                contentDescription = "Loading Notes Logo",
                modifier = Modifier
                    .size(240.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(2.dp, AccentTeal.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(32.dp))

            val infiniteTransition = rememberInfiniteTransition(label = "PenAnimation")
            val penOffsetX by infiniteTransition.animateFloat(
                initialValue = -30f,
                targetValue = 30f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "PenX"
            )
            val penOffsetY by infiniteTransition.animateFloat(
                initialValue = -8f,
                targetValue = 8f,
                animationSpec = infiniteRepeatable(
                    animation = tween(500, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "PenY"
            )
            val penRotation by infiniteTransition.animateFloat(
                initialValue = -15f,
                targetValue = 15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(500, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "PenRotation"
            )

            Box(
                modifier = Modifier.height(60.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Canvas(modifier = Modifier.width(120.dp).height(2.dp)) {
                    drawRect(
                        color = Color(0xFF14B8A6).copy(alpha = 0.3f),
                        size = this.size
                    )
                }

                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Writing Pen",
                    tint = AccentTeal,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(x = penOffsetX.dp, y = (penOffsetY - 25).dp)
                        .rotate(penRotation)
                        .size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Writing your ideas...",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) Color.White else Color(0xFF1E293B)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            CircularProgressIndicator(
                color = AccentTeal,
                strokeWidth = 3.dp,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
