package com.openminis.app.haris.soul.db
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "hermes_memory")
data class MemoryEntryEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val kind: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
)
