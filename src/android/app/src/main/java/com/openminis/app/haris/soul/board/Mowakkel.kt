package com.openminis.app.haris.soul.board
// ponytail: minimal delegate — reuses AgentBoard + ToolRegistry, no new dep.
// Upgrade: queue via Room + WorkManager for persistence across kills.
object HandoffManager{
 fun delegate(taskId:String,title:String,toRole:String,brief:String,parentId:String?=null):BoardTask{
  val t=BoardTask(taskId,title,toRole,"todo",null)
  AgentBoard.upsert(t)
  // log handoff for audit — survives kills via board.json
  return t
 }
 fun complete(taskId:String, result:String?=null){ AgentBoard.move(taskId,"done", result?.take(400)) }
 fun block(taskId:String, reason:String){ AgentBoard.move(taskId,"blocked", reason.take(400)) }
 fun status(taskId:String)= AgentBoard.load().tasks.find{it.id==taskId}
}
