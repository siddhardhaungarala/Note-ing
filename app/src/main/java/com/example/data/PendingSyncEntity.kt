package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_syncs")
data class PendingSyncEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val collectionPath: String, // e.g. "users/uid/notes" or "users/uid/transactions" etc.
    val documentId: String,     // ID of the document
    val action: String,         // "SET" or "DELETE"
    val payload: String,        // JSON serialized map of properties or empty
    val timestamp: Long = System.currentTimeMillis()
)
