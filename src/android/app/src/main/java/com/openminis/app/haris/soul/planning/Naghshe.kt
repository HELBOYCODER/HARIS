package com.openminis.app.haris.soul.planning
import com.openminis.app.haris.soul.board.HandoffManager
data class PlanStep(val id:String,val title:String,val role:String,val dependsOn:List<String> = emptyList())
data class Plan(val goal:String,val steps:List<PlanStep>)
object PlanEngine{
 fun draft(goal:String, hints:List<String> = emptyList()):Plan{
  val steps = listOf(
   PlanStep("research","Research — ${hints.firstOrNull() ?: goal.take(40)}","researcher"),
   PlanStep("design","Design & codegraph","planner", listOf("research")),
   PlanStep("build","Build","operator", listOf("design")),
   PlanStep("verify","Verify + gate","reviewer", listOf("build")),
  )
  steps.forEach{ HandoffManager.delegate(it.id, it.title, it.role, goal) }
  return Plan(goal, steps)
 }
 fun markDone(stepId:String){ HandoffManager.complete(stepId) }
}
