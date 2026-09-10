package com.openminis.app.haris.soul.db
import androidx.room.*
import kotlinx.coroutines.flow.Flow
@Dao
interface HermesDao {
    // BOARD
    @Query("SELECT * FROM hermes_board ORDER BY updatedAt DESC")
    fun observeBoard(): Flow<List<BoardTaskEntity>>
    @Query("SELECT * FROM hermes_board ORDER BY updatedAt DESC")
    suspend fun listBoard(): List<BoardTaskEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBoard(e: BoardTaskEntity)
    @Query("UPDATE hermes_board SET status=:status, blocker=:blocker, updatedAt=:now WHERE id=:id")
    suspend fun updateBoard(id: String, status: String, blocker: String?, now: Long = System.currentTimeMillis())
    @Query("DELETE FROM hermes_board WHERE id=:id")
    suspend fun deleteTask(id: String)
    @Query("SELECT * FROM hermes_board WHERE id=:id LIMIT 1")
    suspend fun getTask(id: String): BoardTaskEntity?
    // MEMORY
    @Query("SELECT content FROM hermes_memory WHERE kind=:kind ORDER BY createdAt DESC LIMIT 1")
    suspend fun latest(kind: String): String?
    @Insert
    suspend fun insertMemory(e: MemoryEntryEntity)
    @Query("SELECT * FROM hermes_memory ORDER BY createdAt DESC LIMIT 50")
    suspend fun ledger(): List<MemoryEntryEntity>
    @Query("SELECT * FROM hermes_memory WHERE kind=:kind ORDER BY createdAt DESC LIMIT :limit")
    suspend fun listMemory(kind: String, limit: Int): List<MemoryEntryEntity>
    // SKILLS
    @Query("SELECT * FROM hermes_skills ORDER BY name")
    fun observeSkills(): Flow<List<HermesSkillEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSkill(e: HermesSkillEntity)
    @Query("SELECT * FROM hermes_skills ORDER BY name")
    suspend fun listSkills(): List<HermesSkillEntity>
    @Query("SELECT COUNT(*) FROM hermes_skills")
    suspend fun skillCount(): Int
}
