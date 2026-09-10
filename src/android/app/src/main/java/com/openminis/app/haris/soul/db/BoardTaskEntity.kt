package com.openminis.app.haris.soul.db
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "hermes_board", indices = [androidx.room.Index("updatedAt")])
data class BoardTaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val role: String,
    val status: String,
    val blocker: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val parentId: String? = null,
)
