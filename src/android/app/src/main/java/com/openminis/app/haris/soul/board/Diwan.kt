package com.openminis.app.haris.soul.board
import com.openminis.app.haris.soul.HermesPaths
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
@Serializable data class BoardTask(val id:String,val title:String,val role:String,val status:String,val blocker:String?=null,val updatedAt:Long=System.currentTimeMillis())
@Serializable data class BoardState(val tasks:List<BoardTask> = emptyList(), val heartbeats:Map<String,Long> = emptyMap())
object AgentBoard {
    private val json=Json{ignoreUnknownKeys=true;prettyPrint=true}
    fun load():BoardState = try{ json.decodeFromString<BoardState>(HermesPaths.boardFile().readText()) }catch(e: Exception){ BoardState() }
    private fun save(s:BoardState){ HermesPaths.boardFile().writeText(json.encodeToString(BoardState.serializer(), s)) }
    fun upsert(t:BoardTask){ val s=load(); save(s.copy(tasks=s.tasks.filterNot{it.id==t.id}+t)) }
    fun move(id:String,status:String,blocker:String?=null){ val s=load(); save(s.copy(tasks=s.tasks.map{if(it.id==id) it.copy(status=status,blocker=blocker,updatedAt=System.currentTimeMillis()) else it})) }
    fun heartbeat(agent:String){ val s=load(); save(s.copy(heartbeats=s.heartbeats+(agent to System.currentTimeMillis()))) }
    fun stale(thresholdMs:Long=90_000):List<String> = load().heartbeats.filter{System.currentTimeMillis()-it.value>thresholdMs}.keys.toList()
}
