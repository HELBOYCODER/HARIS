package com.openminis.app.haris.soul.research
import com.openminis.app.haris.soul.board.HandoffManager
import kotlinx.coroutines.*
// ponytail: parallel fan-out via coroutines, no extra dep. Upgrade: real sub-agents via minis-model-use.
object ResearchDepartment{
 data class Brief(val query:String,val sources:List<String> = emptyList(), val summary:String="")
 suspend fun dispatch(goal:String, tracks:List<String>):Map<String,Brief> = coroutineScope{
  tracks.associateWith { track ->
   async(Dispatchers.Default){
    val id="rs-${track.hashCode().toString().take(6)}"
    HandoffManager.delegate(id, track, "researcher", goal)
    // placeholder: real impl calls Exa/Tavily via 9Router MCP
    val b=Brief("$goal — $track", emptyList(), "pending research for $track")
    HandoffManager.complete(id, b.summary)
    b
   }
  }.mapValues{ it.value.await() }
 }
}
