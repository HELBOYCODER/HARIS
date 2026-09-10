package com.openminis.app.haris.soul
import android.content.Context
import com.openminis.app.haris.soul.db.BoardTaskEntity
import com.openminis.app.data.db.AppDatabase
import com.openminis.app.haris.soul.db.HermesSkillEntity
import com.openminis.app.haris.soul.db.MemoryEntryEntity
import kotlinx.coroutines.flow.Flow

class SoulRepository(ctx: Context){
 private val dao=AppDatabase.getInstance(ctx).soulDao()
 fun observeBoard():Flow<List<BoardTaskEntity>> = dao.observeBoard()
 suspend fun boardList()=dao.listBoard()
 suspend fun boardUpsert(id:String,title:String,role:String,status:String="todo",blocker:String?=null,parentId:String?=null)=dao.upsertBoard(BoardTaskEntity(id,title,role,status,blocker,System.currentTimeMillis(),parentId))
 suspend fun boardMove(id:String,status:String,blocker:String?=null)=dao.updateBoard(id,status,blocker)
 suspend fun boardDelete(id:String)=dao.deleteTask(id)
 suspend fun memoryWrite(kind:String, content:String){
  val capped = when(kind){ "MEMORY"->content.take(2200); "USER"->content.take(1375); else->content.take(4000)}
  dao.insertMemory(MemoryEntryEntity(kind=kind, content=capped))
 }
 suspend fun memoryRead(kind:String, limit:Int=1):String = dao.listMemory(kind,limit).firstOrNull()?.content ?: ""
 suspend fun ledgerRecall(keywords:String):List<String>{
  val rows=dao.listMemory("LEDGER",100)
  val ks=keywords.lowercase().split(" ").filter{it.isNotBlank()}
  return rows.map{it.content}.filter{ l-> ks.all{ l.lowercase().contains(it)} }.takeLast(20)
 }
 fun observeSkills():Flow<List<HermesSkillEntity>> = dao.observeSkills()
 suspend fun seedSkills(names:List<String>){ names.forEach{ dao.upsertSkill(HermesSkillEntity(it,true,0,System.currentTimeMillis(),"ok")) } }
 suspend fun skillsList()=dao.listSkills()
 companion object{
  @Volatile private var I:SoulRepository?=null
  fun get(ctx:Context):SoulRepository = I ?: synchronized(this){ I ?: SoulRepository(ctx).also{I=it} }
 }
}
