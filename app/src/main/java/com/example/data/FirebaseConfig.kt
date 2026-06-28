package com.example.data

data class FirebaseConfig(
    val apiKey: String = "",
    val projectId: String = "",
    val appId: String = "",
    val messagingSenderId: String = "",
    val storageBucket: String = "",
    val isWifiOnly: Boolean = true,
    val autoSyncEnabled: Boolean = true
)
