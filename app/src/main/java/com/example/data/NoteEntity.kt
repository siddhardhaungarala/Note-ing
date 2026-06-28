package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val type: String, // "TEXT", "LIST", "PASSWORD", "FINANCE"
    val content: String, // Main text content (or serialized items for LIST)
    val extraContent: String? = null, // For storing passwords or other metadata
    val pinHash: String? = null, // Security PIN for Password notes
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)

data class VaultAccount(
    val accountName: String,
    val username: String,
    val password: String
) {
    companion object {
        fun parseList(serialized: String): List<VaultAccount> {
            if (serialized.isBlank()) return emptyList()
            return serialized.split("|||").mapNotNull { block ->
                val parts = block.split(":::")
                if (parts.size >= 3) {
                    VaultAccount(parts[0], parts[1], parts[2])
                } else {
                    null
                }
            }
        }

        fun serializeList(list: List<VaultAccount>): String {
            return list.joinToString("|||") { "${it.accountName}:::${it.username}:::${it.password}" }
        }
    }
}
