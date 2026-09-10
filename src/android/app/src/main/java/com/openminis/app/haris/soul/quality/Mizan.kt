package com.openminis.app.haris.soul.quality
import com.openminis.app.haris.soul.memory.HermesMemory
data class GateResult(val pass:Boolean,val notes:List<String>)
object VerificationGate{
 fun check(filesChanged:List<String>, hasTests:Boolean, hasDocs:Boolean):GateResult{
  val notes=mutableListOf<String>()
  if(!hasTests) notes += "missing_tests"
  if(filesChanged.any{it.endsWith(".kt")} && !hasDocs) notes += "missing_docs"
  return GateResult(notes.isEmpty(), notes)
 }
}
object BuildFailureTriage{
 fun triage(log:String):String = when{
  log.contains("rclone",true) -> "missing_aar: add submodules or stub RcloneBridge"
  log.contains("Unresolved reference",true) -> "compile_error: check imports / generated sources"
  log.contains("OutOfMemory",true) -> "oom: raise gradle heap"
  else -> "unknown: see log tail"
 }
}
object WorkflowLearning{
 fun record(success:Boolean, lesson:String){ HermesMemory.ledgerAppend("[${if(success)"ok" else "fail"}] $lesson") }
}
object InstinctLedger{
 fun distill():List<String>{
  val lines=HermesMemory.decisionRecall("")
  val fails=lines.filter{it.contains("[fail]")}.takeLast(5)
  return fails.map{"rule: avoid -> ${it.take(120)}"}
 }
}
