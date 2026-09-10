package com.openminis.app.haris.soul.skills
import com.openminis.app.haris.soul.HermesPaths
import java.io.File
data class SkillHealth(val name:String,val present:Boolean,val sizeKb:Long,val stale:Boolean)
object SkillManager{
 // On Android, skills live in app assets + Hermes snapshot. No /var/minis.
 fun snapshot():String = try{ HermesPaths.skillsSnapshotFile().readText().take(600) }catch(_:Exception){"no_snapshot"}
 fun list():List<String> = try{
  HermesPaths.skillsSnapshotFile().let{
   if(!it.exists()) return emptyList()
   // snapshot is JSON array of names; fallback to empty
   it.readText().let{ t-> if(t.trim().startsWith("[")) t.trim().removeSurrounding("[","]").split(",").map{ s-> s.trim().trim('"')} .filter{ s-> s.isNotBlank()} else emptyList()}
  }
 }catch(_:Exception){ emptyList() }
 fun health():List<SkillHealth> = list().map{ SkillHealth(it,true,0,false) }
 fun harnessInventory():Map<String,Any> = mapOf("skills" to list().size, "board" to HermesPaths.boardFile().exists(), "memory" to HermesPaths.memoryDir().exists())
 // Called once at MinisApp init to seed snapshot from packaged skills index if missing
 fun seedIfMissing(names:List<String>){ val f=HermesPaths.skillsSnapshotFile(); if(!f.exists()){ f.writeText(names.joinToString(prefix="[\"",separator="\",\"",postfix="\"]")) } }
}
