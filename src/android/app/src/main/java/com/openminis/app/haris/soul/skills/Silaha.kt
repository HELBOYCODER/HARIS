package com.openminis.app.haris.soul.skills
import com.openminis.app.haris.soul.HermesPaths
import java.io.File

data class SkillHealth(val name: String, val present: Boolean, val sizeKb: Long, val stale: Boolean)

object SkillManager {
    fun snapshot(): String = try {
        HermesPaths.skillsSnapshotFile().readText().take(600)
    } catch (e: Exception) {
        "no_snapshot"
    }

    fun list(): List<String> = try {
        val f = HermesPaths.skillsSnapshotFile()
        if (!f.exists()) {
            emptyList()
        } else {
            val t = f.readText().trim()
            if (t.startsWith("[")) {
                t.removeSurrounding("[", "]").split(",").map { it.trim().trim('"') }.filter { it.isNotBlank() }
            } else {
                emptyList()
            }
        }
    } catch (e: Exception) {
        emptyList()
    }

    fun health(): List<SkillHealth> = list().map { SkillHealth(it, true, 0, false) }
    fun harnessInventory(): Map<String, Any> = mapOf(
        "skills" to list().size,
        "board" to HermesPaths.boardFile().exists(),
        "memory" to HermesPaths.memoryDir().exists()
    )

    fun seedIfMissing(names: List<String>) {
        val f = HermesPaths.skillsSnapshotFile()
        if (!f.exists()) {
            f.writeText(names.joinToString(prefix = "[\"", separator = "\",\"", postfix = "\"]"))
        }
    }
}
