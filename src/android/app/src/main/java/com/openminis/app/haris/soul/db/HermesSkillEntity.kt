package com.openminis.app.haris.soul.db
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "hermes_skills")
data class HermesSkillEntity(
    @PrimaryKey val name: String,
    val present: Boolean,
    val sizeKb: Long,
    val lastSeen: Long,
    val health: String,
)
