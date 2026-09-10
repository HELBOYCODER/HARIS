package com.openminis.app.haris.soul.memory
import com.openminis.app.haris.soul.HermesPaths
import java.io.File
// Android-private: /data/data/com.openminis.app/files/hermes/memory/
object HermesMemory{
 private fun dir():File = HermesPaths.memoryDir()
 fun readMemory():String = File(dir(),"MEMORY.md").let{if(it.exists()) it.readText() else ""}
 fun readUser():String = File(dir(),"USER.md").let{if(it.exists()) it.readText() else ""}
 fun writeMemory(md:String){ File(dir(),"MEMORY.md").writeText(md.take(2200)) }
 fun writeUser(md:String){ File(dir(),"USER.md").writeText(md.take(1375)) }
 fun ledgerAppend(entry:String){
  val d = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT).format(java.util.Date())
  File(dir(),"ledger.md").appendText("\n- $d $entry")
 }
 fun decisionRecall(keywords:String):List<String>{
  val f=File(dir(),"ledger.md"); if(!f.exists()) return emptyList()
  val ks=keywords.lowercase().split(" ").filter{it.isNotBlank()}
  return f.readLines().filter{ l-> ks.all{ l.lowercase().contains(it)} }.takeLast(20)
 }
 fun syncPreview(newMemory:String,newUser:String):String = buildString{
  appendLine("### MEMORY.md (${readMemory().length} -> ${newMemory.take(2200).length})")
  appendLine(newMemory.take(400)); appendLine("### USER.md (${readUser().length} -> ${newUser.take(1375).length})"); appendLine(newUser.take(300))
 }
}
