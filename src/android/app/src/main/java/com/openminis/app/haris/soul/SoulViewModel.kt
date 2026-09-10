package com.openminis.app.haris.soul
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.openminis.app.haris.soul.db.BoardTaskEntity
import com.openminis.app.haris.soul.db.HermesSkillEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SoulViewModel(app: Application): AndroidViewModel(app) {
    private val repo = SoulRepository.get(app)
    val board: StateFlow<List<BoardTaskEntity>> = repo.observeBoard().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val skills: StateFlow<List<HermesSkillEntity>> = repo.observeSkills().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast
    fun move(id: String, status: String) { viewModelScope.launch { repo.boardMove(id, status); _toast.value = "$id -> $status" }; clearToastSoon() }
    fun delegate(id: String, title: String, role: String, brief: String) { viewModelScope.launch { repo.boardUpsert(id, title, role, "todo", brief.take(400)); repo.memoryWrite("LEDGER","handoff $id -> $role: $title") } }
    private fun clearToastSoon() { viewModelScope.launch { kotlinx.coroutines.delay(2000); _toast.value = null } }
}
