package com.openminis.app.haris.soul
import android.content.Context
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
object HermesToolHandlers {
 fun handle(ctx: Context?, name:String, args:Map<String, JsonElement>): String {
  fun s(k:String)= args[k]?.toString()?.trim('"')?.trim() ?: ""
  if(ctx==null) return """{"error":"no_context"}"""
  val repo=SoulRepository.get(ctx)
  return when(name){
   "delegate_to_subagent" -> {
    val id=s("task_id").ifBlank{"sub-${System.currentTimeMillis()}"}
    runBlocking{ repo.boardUpsert(id, s("title"), s("role").ifBlank{"operator"},"todo", s("brief").take(400)) }
    """{"ok":true,"task_id":"$id"}"""
   }
   "hermes_board" -> when(s("action")){
    "list" -> { val list=runBlocking{ repo.boardList() }; """{"tasks":${list.size}} """ + list.take(20).joinToString(prefix="[",postfix="]"){"""{"id":"${it.id}","status":"${it.status}"}"""} }
    "move" -> { runBlocking{ repo.boardMove(s("task_id"), s("status")) }; """{"ok":true}""" }
    else -> """{"error":"unknown"}"""
   }
   "hermes_memory" -> when(s("action")){
    "write_memory" -> { runBlocking{ repo.memoryWrite("MEMORY", s("text")) }; """{"ok":true}""" }
    "write_user" -> { runBlocking{ repo.memoryWrite("USER", s("text")) }; """{"ok":true}""" }
    "recall" -> { val r=runBlocking{ repo.ledgerRecall(s("keywords").ifBlank{s("text")}) }; """{"hits":${r.size}} """ + r.joinToString("\n") }
    "read" -> { val m=runBlocking{ repo.memoryRead("MEMORY") }; """{"memory":"${m.take(400).replace("\"","'")}"}""" }
    else -> """{"error":"unknown"}"""
   }
   "hermes_plan" -> {
    val steps=listOf("research","design","build","verify"); steps.forEach{ runBlocking{ repo.boardUpsert(it, it, if(it=="research")"researcher" else "operator") } }
    """{"goal":"${s("goal").take(120)}","steps":${steps.size}}"""
   }
   else -> """{"error":"not_hermes_tool"}"""
  }
 }
 fun handle(name:String, args:Map<String, JsonElement>):String = handle(null,name,args)
}
