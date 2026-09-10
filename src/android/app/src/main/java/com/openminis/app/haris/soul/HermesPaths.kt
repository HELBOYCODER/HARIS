package com.openminis.app.haris.soul
import java.io.File
// ponytail: minimal paths for HARIS soul — uses app files dir or tmp fallback when no Context.
// Diwan/SoulMemory/Silaha all route through here; Room tables are primary, files are fallback.
object HermesPaths {
    private var baseDir: File? = null
    fun init(dir: File) { baseDir = dir }
    private fun root(): File = baseDir ?: File(System.getProperty("java.io.tmpdir") ?: "/tmp", "haris-hermes")
    fun boardFile(): File = File(root(), "board.json").also { it.parentFile?.mkdirs() }
    fun memoryDir(): File = File(root(), "memory").also { it.mkdirs() }
    fun skillsSnapshotFile(): File = File(root(), "skills.json").also { it.parentFile?.mkdirs() }
}
